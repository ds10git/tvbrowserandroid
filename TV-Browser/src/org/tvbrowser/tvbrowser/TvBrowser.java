/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software free of charge without modifications and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.TvbPreferencesActivity;
import org.tvbrowser.settings.SettingConstants;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TvBrowser extends FragmentActivity implements
    ActionBar.TabListener {
  private static final int SHOW_PREFERENCES = 1;
  
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
   * keep every loaded fragment in memory. If this becomes too memory intensive,
   * it may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;
  
  private boolean updateRunning;
  private boolean selectingChannels;
  private ActionBar actionBar;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
    
  private Handler handler;
  
  private Timer mTimer;
  
  private MenuItem mUpdateItem;
  private MenuItem mSendLogItem;
  private MenuItem mDeleteLogItem;
  
  private static final Calendar mRundate;
  
  private boolean mSelectionNumberChanged;
  
  private boolean mIsActive;
  private boolean mProgramsListWasShow;
  
  private static String ALL_VALUE;
  
  static {
    mRundate = Calendar.getInstance();
    mRundate.set(Calendar.YEAR, 2014);
    mRundate.set(Calendar.MONTH, Calendar.JANUARY);
    mRundate.set(Calendar.DAY_OF_MONTH, 15);
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putBoolean("updateRunning", updateRunning);
    outState.putBoolean("selectionChannels", selectingChannels);

    super.onSaveInstanceState(outState);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if(PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).getBoolean(getString(R.string.DARK_STYLE), false)) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_tv_browser);
    
    handler = new Handler();
    
    ALL_VALUE = getResources().getString(R.string.filter_channel_all);
    
    if(savedInstanceState != null) {
      updateRunning = savedInstanceState.getBoolean("updateRunning", false);
      selectingChannels = savedInstanceState.getBoolean("selectionChannels", false);
    }
    
    // Set up the action bar.
    actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(
        getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.setOffscreenPageLimit(3);

    // When swiping between different sections, select the corresponding
    // tab. We can also use ActionBar.Tab#select() to do this if we have
    // a reference to the Tab.
    mViewPager
        .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
            
            Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(position);
            
            if(fragment instanceof ProgramTableFragment) {
              ((ProgramTableFragment)fragment).scrollToNow();
            }
            
            mProgramsListWasShow = false;
          }
        });

    // For each of the sections in the app, add a tab to the action bar.
    for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
      // Create a tab with text corresponding to the page title defined by
      // the adapter. Also specify this Activity object, which implements
      // the TabListener interface, as the callback (listener) for when
      // this tab is selected.
      actionBar.addTab(actionBar.newTab()
          .setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
    }
    
    int startTab = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getResources().getString(R.string.TAB_TO_SHOW_AT_START), "0"));
    
    if(mSectionsPagerAdapter.getCount() > startTab) {
      mViewPager.setCurrentItem(startTab);
    }
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    
    mIsActive = false;
    
    if(mTimer != null) {
      mTimer.cancel();
    }
  }
  
  private void showTerms() {
    if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingConstants.EULA_ACCEPTED, false)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setTitle(R.string.terms_of_use);
      
      ScrollView layout = (ScrollView)getLayoutInflater().inflate(R.layout.terms_layout, null);
      
      ((TextView)layout.findViewById(R.id.terms_license)).setText(Html.fromHtml(getResources().getString(R.string.license)));
      
      builder.setView(layout);
      builder.setPositiveButton(R.string.terms_of_use_accept, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
          edit.putBoolean(SettingConstants.EULA_ACCEPTED, true);
          edit.commit();
          
          handler.post(new Runnable() {
            @Override
            public void run() {
              handleResume();
            }
          });
        }
      });
      builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          System.exit(0);
        }
      });
      builder.setCancelable(false);
      
      
      AlertDialog dialog = builder.create();
      dialog.show();
    }
    else {
      handleResume();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    mIsActive = true;
    showTerms();
  }
  
  private void handleResume() {
    new Thread() {
      public void run() {
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.DAY_OF_YEAR, -2);
        
        try {
          getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + cal2.getTimeInMillis(), null);
        }catch(IllegalArgumentException e) {}
      }
    }.start();
    
    // Don't allow use of version after date
    if(mRundate.getTimeInMillis() < System.currentTimeMillis()) {    
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setTitle(R.string.versionExpired);
      builder.setMessage(R.string.versionExpiredMsg);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          System.exit(0);
        }
      });
      builder.setCancelable(false);
      
      AlertDialog dialog = builder.create();
      dialog.show();
    }
    
    Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null, null);
    
    if(!selectingChannels && (channels == null || channels.getCount() == 0)) {
      selectingChannels = true;
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setMessage(R.string.no_channels);
      builder.setPositiveButton(R.string.select_channels, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          selectChannels(false);
        }
      });
      
      builder.setNegativeButton(R.string.dont_select_channels, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      AlertDialog dialog = builder.create();
      dialog.show();
    }
    
    channels.close();
    
    Calendar now = Calendar.getInstance();
    
    now.add(Calendar.MINUTE, 1);
    now.set(Calendar.SECOND, 5);
    
    mTimer = new Timer();
    mTimer.schedule(new TimerTask() {
      private int mCurrentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
      @Override
      public void run() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        
        if(mCurrentDay != day) {
          LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SettingConstants.DATA_UPDATE_DONE));
          mCurrentDay = day;
        }
        
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SettingConstants.REFRESH_VIEWS));
      }
    }, now.getTime(), 60000);
  }
  
  private void showChannelSelection() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.synchronize_title);
    builder.setMessage(R.string.synchronize_text);
    
    builder.setPositiveButton(R.string.synchronize_ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        final SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
        
        if(pref.getString(SettingConstants.USER_NAME, "").trim().length() == 0 || pref.getString(SettingConstants.USER_PASSWORD, "").trim().length() == 0) {
          showUserSetting(true);
        }
        else {
          syncronizeChannels();
        }
      }
    });
    builder.setNegativeButton(R.string.synchronize_cancel, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            showChannelSelectionInternal();
          }
        });
      }
    });
    
    AlertDialog d = builder.create();
    
    d.show();

    ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
  }
  
  private void updateProgramListChannelBar() {
    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(TvBrowser.this);
    
    localBroadcastManager.sendBroadcast(new Intent(SettingConstants.CHANNEL_UPDATE_DONE));
  }
  
  private void syncronizeChannels()  {
    Cursor test = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.CHANNEL_KEY_SELECTION}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1 ", null, null);
    
    int count = test.getCount();
    
    test.close();
    
    if(count > 0) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.synchronize_replace_add_title);
      builder.setMessage(R.string.synchronize_replace_add_text);
      
      builder.setPositiveButton(R.string.synchronize_add, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          syncronizeChannels(false);
        }
      });
      builder.setNegativeButton(R.string.synchronize_replace, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          syncronizeChannels(true);
        }
      });
      
      builder.show();
    }
    else {
      syncronizeChannels(false);
    }
  }
  
  private void syncronizeChannels(final boolean replace) {
    new Thread() {
      public void run() {
        boolean somethingSynchonized = false;
        
        if(replace) {
          ContentValues values = new ContentValues();
          values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 0);
          values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, 0);
          
          if(getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null) > 0) {
            somethingSynchonized = true;
          }
        }
        
        URL documentUrl;
        try {
          //documentUrl = new URL("http://android.tvbrowser.org/hurtzAndroidTvbChannels2.php");
          documentUrl = new URL("http://android.tvbrowser.org/data/scripts/hurtzAndroidTvbChannels.php");
          URLConnection connection = documentUrl.openConnection();
          
          SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
          
          String car = pref.getString(SettingConstants.USER_NAME, null);
          String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
          
          if(car != null && bicycle != null) {
            String userpass = car + ":" + bicycle;
            String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
            
            connection.setRequestProperty ("Authorization", basicAuth);
            
            BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),"UTF-8"));
            
            String line = null;
            
            int sort = 1;
            
            while((line = read.readLine()) != null) {
              if(line.trim().length() > 0) {
                if(line.contains(":")) {
                  String[] parts = line.split(":");
                  
                  if(parts[0].equals("1")) {
                    String dataService = "EPG_FREE";
                    
                    String where = " ( " + TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = '" + dataService + "' ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + "='" + parts[1] + "' ) ";
                    
                    Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
                    
                    if(group.moveToFirst()) {
                      int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                      
                      where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + "=" + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "='" + parts[2] + "' ) ";
                      
                      ContentValues values = new ContentValues();
                      
                      values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 1);
                      values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, sort);
                      
                      int changed = getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, where, null);
                      
                      if(changed > 0) {
                        somethingSynchonized = true;
                      }
                    }
                    
                    group.close();
                    
                    sort++;
                  }
                }
              }
            }
            
            if(somethingSynchonized) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  updateProgramListChannelBar();
                  Toast.makeText(getApplicationContext(), R.string.synchronize_done, Toast.LENGTH_LONG).show();
                  checkTermsAccepted();
                }
              });
            }
            else {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(getApplicationContext(), R.string.synchronize_error, Toast.LENGTH_LONG).show();
                  showChannelSelectionInternal();
                }
              });
            }
          }
          else {
            handler.post(new Runnable() {
              @Override
              public void run() {
                showChannelSelectionInternal();
              }
            });
          }
        } catch (Exception e) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              showChannelSelectionInternal();
            }
          });
          Log.d("dateinfo", "",e);
        }
        
        selectingChannels = false;
      }
    }.start();
    
  }
  
  private void synchronizeDontWantToSee() {
    new Thread() {
      public void run() {
        if(!SettingConstants.UPDATING_FILTER) {
          SettingConstants.UPDATING_FILTER = true;
          
          NotificationCompat.Builder builder;
          
          builder = new NotificationCompat.Builder(TvBrowser.this);
          builder.setSmallIcon(R.drawable.ic_launcher);
          builder.setOngoing(true);
          builder.setContentTitle(getResources().getText(R.string.action_dont_want_to_see));
          builder.setContentText(getResources().getText(R.string.dont_want_to_see_notification_text));
          
          int notifyID = 2;
          
          NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
          notification.notify(notifyID, builder.build());
          
          updateProgressIcon(true);
                  
          URL documentUrl;
          
          try {
            //documentUrl = new URL("http://android.tvbrowser.org/hurtzAndroidTvbChannels2.php");
            documentUrl = new URL("http://android.tvbrowser.org/data/scripts/hurtzAndroidDontWantToSee.php");
            URLConnection connection = documentUrl.openConnection();
            
            SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
            
            String car = pref.getString(SettingConstants.USER_NAME, null);
            String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
            Log.d("dateinfo", car + " " + bicycle);
            if(car != null && bicycle != null) {
              String userpass = car + ":" + bicycle;
              String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
              
              connection.setRequestProperty ("Authorization", basicAuth);
              
              BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),"UTF-8"));
              
              String line = null;
              
              HashSet<String> exclusions = new HashSet<String>();
              ArrayList<DontWantToSeeExclusion> exclusionList = new ArrayList<DontWantToSeeExclusion>();
              
              while((line = read.readLine()) != null) {
                if(line.contains(";;") && line.trim().length() > 0) {
                  exclusions.add(line);
                  exclusionList.add(new DontWantToSeeExclusion(line));
                }
              }
              
              if(exclusions.size() > 0) {
                Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
                
                edit.putStringSet(getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), exclusions);
                edit.commit();
                
                DontWantToSeeExclusion[] exclusionArr = exclusionList.toArray(new DontWantToSeeExclusion[exclusionList.size()]);
                
                Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE}, null, null, TvBrowserContentProvider.KEY_ID);
                
                builder.setProgress(c.getCount(), 0, true);
                notification.notify(notifyID, builder.build());
                
                ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
                
                int keyColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                int titleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
                
                while(c.moveToNext()) {
                  builder.setProgress(c.getCount(), c.getPosition(), false);
                  notification.notify(notifyID, builder.build());
                  
                  String title = c.getString(titleColumn);
                  
                  boolean filter = UiUtils.filter(getApplicationContext(), title, exclusionArr);
                  long progID = c.getLong(keyColumn);
                  
                  ContentValues values = new ContentValues();
                  values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, filter ? 1 : 0);
                  
                  ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, progID));
                  opBuilder.withValues(values);
                  
                  updateValuesList.add(opBuilder.build());
                }
                
                notification.cancel(notifyID);
                
                c.close();
                
                if(!updateValuesList.isEmpty()) {
                  try {
                    getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                    UiUtils.sendDontWantToSeeChangedBroadcast(getApplicationContext(),true);
                    handler.post(new Runnable() {
                      @Override
                      public void run() {
                        Toast.makeText(getApplicationContext(), R.string.dont_want_to_see_sync_success, Toast.LENGTH_LONG).show();
                      }
                    });
                  } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  } catch (OperationApplicationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                }
              }
              else {
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    Toast.makeText(getApplicationContext(), R.string.no_dont_want_to_see_sync, Toast.LENGTH_LONG).show();
                  }
                });
              }
            }
          }catch(Throwable t) {
            Log.d("info1", "" , t);
            handler.post(new Runnable() {
              @Override
              public void run() {
                Toast.makeText(getApplicationContext(), R.string.no_dont_want_to_see_sync, Toast.LENGTH_LONG).show();
              }
            });
          }
  
          updateProgressIcon(false);
          
          SettingConstants.UPDATING_FILTER = false;
        }
      }
    }.start();
  }
  
  private static final class ChannelSelection {
    private int mChannelID;
    private int mCategory;
    private Bitmap mChannelLogo;
    private String mCountry;
    private String mName;
    private boolean mIsSelected;
    private boolean mWasSelected;
    
    public ChannelSelection(int channelID, String name, int category, String country, Bitmap channelLogo, boolean isSelected) {
      mChannelID = channelID;
      mCategory = category;
      mCountry = country;
      mChannelLogo = channelLogo;
      mName = name;
      mWasSelected = mIsSelected = isSelected;
    }
    
    public boolean isCategory(int category) {
      return category == 0 || (mCategory & category) == category;
    }
    
    public boolean isCountry(String value) {
      return value == null || mCountry.toLowerCase().contains(value.toLowerCase());
    }
    
    public boolean isSelected() {
      return mIsSelected;
    }
    
    public boolean wasSelected() {
      return mWasSelected;
    }
    
    public void setSelected(boolean value) {
      mIsSelected = value;
    }
    
    public Bitmap getLogo() {
      return mChannelLogo;
    }
    
    public String toString() {
      return mName;
    }
    
    public int getChannelID() {
      return mChannelID;
    }
  }
  
  /**
   * View holder for custom array adapter of channel selection.
   * 
   * @author René Mach
   */
  private static final class ViewHolder {
    CheckBox mCheckBox;
    TextView mTextView;
    TextView mSortNumber;
    ImageView mLogo;
  }
  
  /**
   * Class for wrapping array list to support
   * filtering channel selection for country and
   * category.
   * 
   * @author René Mach
   */
  private static class ArrayListWrapper extends ArrayList<ChannelSelection> {
    Integer[] mValueMap = null;
    
    @Override
    public ChannelSelection get(int index) {
      if(mValueMap != null) {
        if(mValueMap.length >= 0 && index < mValueMap.length) {
          return super.get(mValueMap[index]);
        }
        
        throw new ArrayIndexOutOfBoundsException(index);
      }
      
      return super.get(index);
    }
    
    public void setFilter(ChannelFilter filter) {
      ArrayList<Integer> map = new ArrayList<Integer>();
      
      for(int i = 0; i < super.size(); i++) {
        ChannelSelection selection = super.get(i);
        
        if(selection.isCategory(filter.mCategory) && selection.isCountry(filter.mCountry)) {
          map.add(Integer.valueOf(i));
        }
      }
      
      mValueMap = map.toArray(new Integer[map.size()]);
    }
    
    @Override
    public int size() {
      if(mValueMap != null) {
        return mValueMap.length;
      }
      
      return super.size();
    }
    
    private ChannelSelection superGet(int index) {
      return super.get(index);
    }
    
    public Iterator<ChannelSelection> superIterator() {
      return super.iterator();
    }
    
    @Override
    public Iterator<ChannelSelection> iterator() {
      if(mValueMap != null) {
        Iterator<ChannelSelection> it = new Iterator<TvBrowser.ChannelSelection>() {
          private int mCurrentIndex = 0;
          private ChannelSelection mCurrent = null;
          
          @Override
          public boolean hasNext() {
            return mCurrentIndex < mValueMap.length-1;
          }

          @Override
          public ChannelSelection next() {
            mCurrent = superGet(mValueMap[++mCurrentIndex]);
            return mCurrent;
          }

          @Override
          public void remove() {
            mCurrent = null;
          }
        };
        
        return it;
      }
      
      return super.iterator();
    }
  };
  
  /**
   * Class for filtering of country and category for channel selection.
   * 
   * @author René Mach
   */
  private final static class ChannelFilter {
    public int mCategory;
    public String mCountry;
    
    public ChannelFilter(int category, String country) {
      mCategory = category;
      mCountry = country;
    }
  }
  
  /**
   * Class for country filtering of channel selecton.
   * 
   * @author René Mach
   */
  private final static class Country {    
    public Locale mLocale;
    
    public Country(Locale locale) {
      mLocale = locale;
    }
    
    public String toString() {
      if(mLocale == null) {
        return ALL_VALUE;
      }
      
      return mLocale.getDisplayCountry();
    }
    
    public String getCountry() {
      if(mLocale == null) {
        return null;
      }
      
      return mLocale.getCountry();
    }
    
    @Override
    public boolean equals(Object o) {
      if(o instanceof Country) {
        return mLocale.getCountry().equals(((Country)o).mLocale.getCountry());
      }
      
      return super.equals(o);
    }
  }
  
  private void showChannelSelectionInternal() {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_CATEGORY,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ALL_COUNTRIES
        };
    
    ContentResolver cr = getContentResolver();
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, null, null, null);
    
    // populate array list with all available channels
    final ArrayListWrapper channelSelectionList = new ArrayListWrapper();
    ArrayList<Country> countryList = new ArrayList<Country>();
            
    while(channels.moveToNext()) {
      int channelID = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.KEY_ID));
      int category = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CATEGORY));
      byte[] logo = channels.getBlob(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
      String name = channels.getString(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
      String countries = channels.getString(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ALL_COUNTRIES));
      boolean isSelected = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_SELECTION)) == 1;
      
      if(countries.contains("$")) {
        String[] values = countries.split("\\$");
        
        for(String country : values) {
          Country test = new Country(new Locale(country, country));
          
          if(!countryList.contains(test) && test.mLocale.getDisplayCountry().trim().length() > 0) {
            countryList.add(test);
          }
        }
      }
      else {
        Country test = new Country(new Locale(countries, countries));
        
        if(!countryList.contains(test) && test.mLocale.getDisplayCountry().trim().length() > 0) {
          countryList.add(test);
        }
      }
      
      Bitmap channelLogo = null;
      
      if(logo != null) {
        channelLogo = BitmapFactory.decodeByteArray(logo, 0, logo.length);
      }
      
      ChannelSelection selection = new ChannelSelection(channelID, name, category, countries, channelLogo, isSelected);
      
      channelSelectionList.add(selection);
    }
    
    // sort countries for filtering
    Collections.sort(countryList, new Comparator<Country>() {
      @Override
      public int compare(Country lhs, Country rhs) {
        return lhs.toString().compareToIgnoreCase(rhs.toString());
      }
    });
    
    countryList.add(0,new Country(null));
    
    channels.close();
    
    // create filter for filtering of category and country
    final ChannelFilter filter = new ChannelFilter(SettingConstants.TV_CATEGORY, null);
    
    channelSelectionList.setFilter(filter);
    
    // create default logo for channels without logo
    final Bitmap defaultLogo = BitmapFactory.decodeResource( getResources(), R.drawable.ic_launcher);
    
    // Custom array adapter for channel selection
    final ArrayAdapter<ChannelSelection> channelSelectionAdapter = new ArrayAdapter<ChannelSelection>(TvBrowser.this, R.layout.channel_row, channelSelectionList) {
      public View getView(int position, View convertView, ViewGroup parent) {
        ChannelSelection value = getItem(position);
        ViewHolder holder = null;
        
        if (convertView == null) {
          LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
          
          holder = new ViewHolder();
          
          convertView = mInflater.inflate(R.layout.channel_row, null);
          
          holder.mTextView = (TextView)convertView.findViewById(R.id.row_of_channel_text);
          holder.mCheckBox = (CheckBox)convertView.findViewById(R.id.row_of_channel_selection);
          holder.mLogo = (ImageView)convertView.findViewById(R.id.row_of_channel_icon);
          
          convertView.setTag(holder);
          
        }
        else {
          holder = (ViewHolder)convertView.getTag();
        }
        
        holder.mTextView.setText(value.toString());
        holder.mCheckBox.setChecked(value.isSelected());
        
        Bitmap logo = value.getLogo();
        
        if(logo != null) {
          holder.mLogo.setImageBitmap(logo);
        }
        else {
          holder.mLogo.setImageBitmap(defaultLogo);
        }
        
        return convertView;
      }
    };
    
    // inflate channel selection view
    View channelSelectionView = getLayoutInflater().inflate(R.layout.channel_selection_list, null);
    
    // get spinner for country filtering and create array adapter with all available countries
    Spinner country = (Spinner)channelSelectionView.findViewById(R.id.channel_country_value);
    
    final ArrayAdapter<Country> countryListAdapter = new ArrayAdapter<Country>(this, android.R.layout.simple_spinner_item, countryList);
    countryListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    country.setAdapter(countryListAdapter);
    
    // add item selection listener to react of user setting filter for country
    country.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
        Country country = countryListAdapter.getItem(position);
        
        filter.mCountry = country.getCountry();
        channelSelectionList.setFilter(filter);
        channelSelectionAdapter.notifyDataSetChanged();
      }

      @Override
      public void onNothingSelected (AdapterView<?> parent) {}
    });
    
    // get spinner for category selection and add listener to react to user category selection
    Spinner category = (Spinner)channelSelectionView.findViewById(R.id.channel_category_value);
    category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
        switch(position) {
          case 1: filter.mCategory = SettingConstants.TV_CATEGORY;break;
          case 2: filter.mCategory = SettingConstants.RADIO_CATEGORY;break;
          case 3: filter.mCategory = SettingConstants.CINEMA_CATEGORY;break;
          
          default: filter.mCategory = SettingConstants.NO_CATEGORY;break;
        }
        
        channelSelectionList.setFilter(filter);
        channelSelectionAdapter.notifyDataSetChanged();
      }

      @Override
      public void onNothingSelected (AdapterView<?> parent) {}
    });
    
    // get the list view of the layout and add adapter with available channels
    ListView list = (ListView)channelSelectionView.findViewById(R.id.channel_selection_list);
    list.setAdapter(channelSelectionAdapter);
    
    // add listener to react to user selection of channels
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        CheckBox check = (CheckBox)view.findViewById(R.id.row_of_channel_selection);
        
        if(check != null) {
          check.setChecked(!check.isChecked());
          channelSelectionAdapter.getItem(position).setSelected(check.isChecked());
        }        
      }
    });
    
    // show dialog only if channels are available
    if(!channelSelectionList.isEmpty()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
      builder.setTitle(R.string.select_channels);
      builder.setView(channelSelectionView);
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {        
        @Override
        public void onClick(DialogInterface dialog, int which) {    
          boolean somethingSelected = false;
          boolean somethingChanged = false;
          
          Iterator<ChannelSelection> it = channelSelectionList.superIterator();
          
          while(it.hasNext()) {
            ChannelSelection sel = it.next();
            
            if(sel.isSelected() && !sel.wasSelected()) {
              somethingChanged = somethingSelected = true;
              
              ContentValues values = new ContentValues();
              
              values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 1);
              
              getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, sel.getChannelID()), values, null, null);              
            }
            else if(!sel.isSelected() && sel.wasSelected()) {
              somethingChanged = true;
              
              ContentValues values = new ContentValues();
              
              values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 0);
              
              getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, sel.getChannelID()), values, null, null);
              
              getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=" + sel.getChannelID(), null);
              getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=" + sel.getChannelID(), null);
            }
          }
          
          // if something was changed we need to update channel list bar in program list and the complete program table
          if(somethingChanged) {
            updateProgramListChannelBar();
          }
          
          // if something was selected we need to download new data
          if(somethingSelected) {
            checkTermsAccepted();
          }
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {        
        @Override
        public void onClick(DialogInterface dialog, int which) {}
      });
      
      builder.show();
    }
    
    selectingChannels = false;
  }
  
  private static class ChannelSort {
    private String mName;
    private int mKey;
    private int mSortNumber;
    private int mOldSortNumber;
    private Bitmap mChannelLogo;
    
    public ChannelSort(int key, String name, int sortNumber, Bitmap channelLogo) {
      mKey = key;
      mName = name;
      mOldSortNumber = mSortNumber = sortNumber;
      mChannelLogo = channelLogo;
    }
    
    public int getKey() {
      return mKey;
    }
    
    public void setSortNumber(int value) {
      mSortNumber = value;
    }
    
    public int getSortNumber() {
      return mSortNumber;
    }
    
    public String toString() {
      return (mSortNumber == 0 ? "-" : mSortNumber) + ". " + mName;
    }
    
    public String getName() {
      return mName;
    }
    
    public Bitmap getLogo() {
      return mChannelLogo;
    }
    
    public boolean wasChanged() {
      return mOldSortNumber != mSortNumber;
    }
  }
  
  private void runChannelDownload() {
    if(!TvDataUpdateService.IS_RUNNING) {
      Intent updateChannels = new Intent(TvBrowser.this, TvDataUpdateService.class);
      updateChannels.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.CHANNEL_TYPE);
      
      final IntentFilter filter = new IntentFilter(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE);
      
      BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          updateProgressIcon(false);
          
          LocalBroadcastManager.getInstance(TvBrowser.this).unregisterReceiver(this);
          
          boolean success = intent.getBooleanExtra(SettingConstants.CHANNEL_DOWNLOAD_SUCCESSFULLY, true);
          
          if(mIsActive) {
            if(success) {
              showChannelSelection();
            }
            else {
              AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
              
              builder.setTitle(R.string.channel_download_warning_title);
              builder.setMessage(R.string.channel_download_warning_text);
              
              builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  handler.post(new Runnable() {
                    @Override
                    public void run() {
                      showChannelSelection();
                    }
                  });
                }
              });
              
              builder.show();
            }
          }
        }
      };
      
      LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
      
      updateProgressIcon(true);
      startService(updateChannels);
    }
  }
  
  private void selectChannels(boolean loadAgain) {
    Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, null, null, null);
    
    if(loadAgain || channels.getCount() < 1) {
      if(isOnline()) {
        runChannelDownload();
      }
      else {
        showNoInternetConnection(new Runnable() {
          @Override
          public void run() {
            checkTermsAccepted();
          }
        });
      }
    }
    else {
      showChannelSelection();
    }
    
    channels.close();
  }
  
  private void sortChannels() {
    ContentResolver cr = getContentResolver();
    
    StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where.append("=1");
    
    LinearLayout main = (LinearLayout)getLayoutInflater().inflate(R.layout.channel_sort_list, null);
    
    Button sortAlphabetically = (Button)main.findViewById(R.id.channel_sort_alpabetically);
    
    ListView channelSort = (ListView)main.findViewById(R.id.channel_sort);
    
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO
        };
    
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    final ArrayList<ChannelSort> channelSource = new ArrayList<TvBrowser.ChannelSort>();
      
    if(channels.getCount() > 0) {
      if(channels.moveToFirst()) {
        do {
          int key = channels.getInt(0);
          String name = channels.getString(1);
          
          int order = 0;
          
          if(!channels.isNull(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER))) {
            order = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));
          }
          
          byte[] logo = channels.getBlob(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
          Bitmap channelLogo = null;
          
          if(logo != null) {
            channelLogo = BitmapFactory.decodeByteArray(logo, 0, logo.length);
          }
                    
          channelSource.add(new ChannelSort(key, name, order, channelLogo));
        }while(channels.moveToNext());
      }
      
      channels.close();
      
      final Comparator<ChannelSort> sortComparator = new Comparator<TvBrowser.ChannelSort>() {
        @Override
        public int compare(ChannelSort lhs, ChannelSort rhs) {
          if(lhs.getSortNumber() < rhs.getSortNumber()) {
            return -1;
          }
          else if(lhs.getSortNumber() > rhs.getSortNumber()) {
            return 1;
          }
          
          return 0;
        }
      };
      
      Collections.sort(channelSource, sortComparator);

      // create default logo for channels without logo
      final Bitmap defaultLogo = BitmapFactory.decodeResource( getResources(), R.drawable.ic_launcher);
      
      final ArrayAdapter<ChannelSort> aa = new ArrayAdapter<TvBrowser.ChannelSort>(TvBrowser.this, R.layout.channel_sort_row, channelSource) {
        public View getView(int position, View convertView, ViewGroup parent) {
          ChannelSort value = getItem(position);
          ViewHolder holder = null;
          
          if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            
            holder = new ViewHolder();
            
            convertView = mInflater.inflate(R.layout.channel_sort_row, null);
            
            holder.mTextView = (TextView)convertView.findViewById(R.id.row_of_channel_sort_text);
            holder.mSortNumber = (TextView)convertView.findViewById(R.id.row_of_channel_sort_number);
            holder.mLogo = (ImageView)convertView.findViewById(R.id.row_of_channel_sort_icon);
            
            convertView.setTag(holder);
            
          }
          else {
            holder = (ViewHolder)convertView.getTag();
          }
          
          holder.mTextView.setText(value.getName());
          
          String sortNumber = String.valueOf(value.getSortNumber());
          
          if(value.getSortNumber() == 0) {
            sortNumber = "-";
          }
          
          sortNumber += ".";
          
          holder.mSortNumber.setText(sortNumber);
          
          Bitmap logo = value.getLogo();
          
          if(logo != null) {
            holder.mLogo.setImageBitmap(logo);
          }
          else {
            holder.mLogo.setImageBitmap(defaultLogo);
          }
          
          return convertView;
        }
      };
      channelSort.setAdapter(aa);
      
      channelSort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, final int position,
            long id) {
          AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
          LinearLayout numberSelection = (LinearLayout)getLayoutInflater().inflate(R.layout.sort_number_selection, null);
          
          mSelectionNumberChanged = false;
          
          final NumberPicker number = (NumberPicker)numberSelection.findViewById(R.id.sort_picker);
          number.setMinValue(1);
          number.setMaxValue(channelSource.size());
          number.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
          number.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
              mSelectionNumberChanged = true;
            }
          });
          
          final EditText numberAlternative = (EditText)numberSelection.findViewById(R.id.sort_entered_number);
          
          builder.setView(numberSelection);
          
          final ChannelSort selection = channelSource.get(position);
          
          TextView name = (TextView)numberSelection.findViewById(R.id.sort_picker_channel_name);
          name.setText(selection.getName());
          
          if(selection.getSortNumber() > 0) {
            if(selection.getSortNumber() < channelSource.size()+1) {
              number.setValue(selection.getSortNumber());
            }
            else {
              numberAlternative.setText(String.valueOf(selection.getSortNumber()));
            }
          }
          
          builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              String test = numberAlternative.getText().toString().trim();
              
              if(test.length() == 0 || mSelectionNumberChanged) {
                selection.setSortNumber(number.getValue());
              }
              else {
                try {
                  selection.setSortNumber(Integer.parseInt(test));
                }catch(NumberFormatException e1) {}
              }
              
              Collections.sort(channelSource, sortComparator);
              aa.notifyDataSetChanged();
            }
          });
          
          builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              // TODO Auto-generated method stub
              
            }
          });
          
          builder.show();
        }
      });
      
      sortAlphabetically.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Collections.sort(channelSource, new Comparator<ChannelSort>() {
            @Override
            public int compare(ChannelSort lhs, ChannelSort rhs) {
              return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
          });
          
          for(int i = 0; i < channelSource.size(); i++) {
            channelSource.get(i).setSortNumber(i+1);
          }
          
          aa.notifyDataSetChanged();
        }
      });
      
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.action_sort_channels);
      builder.setView(main);
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          boolean somethingChanged = false;
          
          for(ChannelSort selection : channelSource) {
            if(selection.wasChanged()) {
              somethingChanged = true;
              
              ContentValues values = new ContentValues();
              values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, selection.getSortNumber());
              
              getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, selection.getKey()), values, null, null);
            }
          }
          
          if(somethingChanged) {
            updateProgramListChannelBar();
          }
        }
      });
      builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      
      builder.show();
    }
  }

  public boolean isOnline() {
    ConnectivityManager cm =
        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
        return true;
    }
    return false;
  }
  
  private void addUpdateBroadcastReceiver() {
    IntentFilter filter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateProgressIcon(false);
        
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);
      }
    }, filter);
  }

  private void updateTvData() {
    if(!TvDataUpdateService.IS_RUNNING) {
      Cursor test = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null, null);
      
      if(test.getCount() > 0) {
        AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
        
        LinearLayout dataDownload = (LinearLayout)getLayoutInflater().inflate(R.layout.download_selection, null);
        
        final Spinner days = (Spinner)dataDownload.findViewById(R.id.download_days);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.download_selections, android.R.layout.simple_spinner_item);
    
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        days.setAdapter(adapter);
        
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
        String daysToDownload = pref.getString(getResources().getString(R.string.DAYS_TO_DOWNLOAD), "2");
        
        String[] valueArr = getResources().getStringArray(R.array.download_days);
        
        for(int i = 0; i < valueArr.length; i++) {
          if(valueArr[i].equals(daysToDownload)) {
            days.setSelection(i);
            break;
          }
        }
        
        builder.setTitle(R.string.download_data);
        builder.setView(dataDownload);
        
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent startDownload = new Intent(TvBrowser.this, TvDataUpdateService.class);
            startDownload.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.TV_DATA_TYPE);
            
            String value = getResources().getStringArray(R.array.download_days)[days.getSelectedItemPosition()];
            
            startDownload.putExtra(getResources().getString(R.string.DAYS_TO_DOWNLOAD), Integer.parseInt(value));
            
            Editor settings = pref.edit();
            settings.putString(getResources().getString(R.string.DAYS_TO_DOWNLOAD), value);
            settings.commit();
            
            startService(startDownload);
            
            updateProgressIcon(true);
            
            addUpdateBroadcastReceiver();
          }
        });
        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            
          }
        });
        builder.show();
      }
      else {
        Cursor test2 = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, null, null, null);
        
        boolean loadAgain = test2.getCount() < 1;
        
        test2.close();
        
        selectChannels(loadAgain);
      }
      
      test.close();
    }
  }
  
  private void storeUserName(final String userName, final String password, final boolean syncChannels) {
    Editor edit = getSharedPreferences("transportation", Context.MODE_PRIVATE).edit();
    
    edit.putString(SettingConstants.USER_NAME, userName);
    edit.putString(SettingConstants.USER_PASSWORD, password);
    
    edit.commit();
    
    Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(2);
    
    if(fragment instanceof FavoritesFragment) {
      ((FavoritesFragment)fragment).updateSynchroButton(null);
    }
    
    if(syncChannels) {
      syncronizeChannels();
    }
  }
  
  private void showUserError(final String userName, final String password, final boolean syncChannels) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
        
        builder.setTitle(R.string.userpass_error_title);
        builder.setMessage(R.string.userpass_error);
        
        builder.setPositiveButton(getResources().getString(R.string.userpass_reenter), new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            handler.post(new Runnable() {
              @Override
              public void run() {
                showUserSetting(userName,password,syncChannels);
              }
            });
          }
        });
        
        builder.setNegativeButton(getResources().getString(R.string.userpass_save_anyway), new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            storeUserName(userName,password,syncChannels);
          }
        });
        
        builder.show();
      }
    });
  }
  
  private void setUserName(final String userName, final String password, final boolean syncChannels) {
    if(userName != null && password != null) {
      new Thread() {
        public void run() {
          URL documentUrl;
          try {
            //documentUrl = new URL("http://android.tvbrowser.org/hurtzAndroidTvbChannels2.php");
            documentUrl = new URL("http://android.tvbrowser.org/data/scripts/testMyAccount.php");
            URLConnection connection = documentUrl.openConnection();
            
            String userpass = userName + ":" + password;
            String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
            
            connection.setRequestProperty ("Authorization", basicAuth);
            
            if(((HttpURLConnection)connection).getResponseCode() != 200) {
              showUserError(userName,password,syncChannels);
            }
            else {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  storeUserName(userName,password,syncChannels);    
                }
              });
            }
          
          }catch(Throwable t) {
            showUserError(userName,password,syncChannels);
          }
        }
      }.start();
    }
    else if(syncChannels) {
      syncronizeChannels();
    }
  }
  
  private void showUserSetting(final boolean syncChannels) {
    showUserSetting(null,null,syncChannels);
  }
  
  private void showUserSetting(final String initiateUserName, final String initiatePassword, final boolean syncChannels) {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    RelativeLayout username_password_setup = (RelativeLayout)getLayoutInflater().inflate(R.layout.username_password_setup, null);
            
    final SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
    
    final EditText userName = (EditText)username_password_setup.findViewById(R.id.username_entry);
    final EditText password = (EditText)username_password_setup.findViewById(R.id.password_entry);
    
    userName.setText(pref.getString(SettingConstants.USER_NAME, initiateUserName != null ? initiateUserName : ""));
    password.setText(pref.getString(SettingConstants.USER_PASSWORD, initiatePassword != null? initiatePassword : ""));
    
    builder.setView(username_password_setup);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        setUserName(userName.getText().toString().trim(), password.getText().toString().trim(), syncChannels);
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if(syncChannels) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              showChannelSelectionInternal();
            }
          });
        }
      }
    });

    AlertDialog d = builder.create();
    
    d.show();

    ((TextView)d.findViewById(R.id.user_pw_sync_info)).setMovementMethod(LinkMovementMethod.getInstance());
  }
  
  private void showNoInternetConnection(final Runnable callback) {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.no_network);
    builder.setMessage(R.string.no_network_info);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if(callback != null && isOnline()) {
          callback.run();
        }
      }
    });
    
    builder.show();
  }
  
  private void checkTermsAccepted() {
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    
    String terms = pref.getString(SettingConstants.TERMS_ACCEPTED, "");
    
    if(terms.contains("EPG_FREE")) {
      updateTvData();
    }
    else {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.terms_of_use_data);
      builder.setMessage(R.string.terms_of_use_text);
      
      builder.setPositiveButton(R.string.terms_of_use_accept, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Editor edit = pref.edit();
          
          edit.putString(SettingConstants.TERMS_ACCEPTED, "EPG_FREE");
          
          edit.commit();
          
          updateTvData();
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      AlertDialog d = builder.create();
            
      d.show();

      ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
  }
  
  private static final class ExclusionEdit implements Comparable<ExclusionEdit> {
    String mExclusion;
    boolean mIsCaseSensitive;
    
    public ExclusionEdit(String exclusion) {
      String[] parts = exclusion.split(";;");
      
      mExclusion = parts[0];
      mIsCaseSensitive = parts[1].equals("1");
    }
    
    @Override
    public String toString() {
      return mExclusion;
    }

    @Override
    public int compareTo(ExclusionEdit another) {
      return mExclusion.replace("*", "").compareToIgnoreCase(another.mExclusion.replace("*", ""));
    }
    
    public String getExclusion() {
      return mExclusion + ";;" + (mIsCaseSensitive ? "1" : "0");
    }
  }
  
  private void editDontWantToSee() {
    if(!SettingConstants.UPDATING_FILTER) {
      Set<String> currentExclusions = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet(getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), null);
      
      final ArrayList<ExclusionEdit> mCurrentExclusionList = new ArrayList<TvBrowser.ExclusionEdit>();
      
      if(currentExclusions != null && !currentExclusions.isEmpty()) {
        for(String exclusion : currentExclusions) {
          mCurrentExclusionList.add(new ExclusionEdit(exclusion));
        }
      }
      
      Collections.sort(mCurrentExclusionList);
      
      final ArrayAdapter<ExclusionEdit> exclusionAdapter = new ArrayAdapter<TvBrowser.ExclusionEdit>(TvBrowser.this, android.R.layout.simple_list_item_1, mCurrentExclusionList);
      
      View view = getLayoutInflater().inflate(R.layout.dont_want_to_see_exclusion_edit_list, null);
      
      ListView list = (ListView)view.findViewById(R.id.dont_want_to_see_exclusion_list);
      
      list.setAdapter(exclusionAdapter);
      
      final Runnable cancel = new Runnable() {
        @Override
        public void run() {}
      };
      
      AdapterView.OnItemClickListener onClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          final ExclusionEdit edit = exclusionAdapter.getItem(position);
          
          View editView = getLayoutInflater().inflate(R.layout.dont_want_to_see_edit, null);
          
          final TextView exclusion = (TextView)editView.findViewById(R.id.dont_want_to_see_value);
          final CheckBox caseSensitive = (CheckBox)editView.findViewById(R.id.dont_want_to_see_case_sensitve);
          
          exclusion.setText(edit.mExclusion);
          caseSensitive.setSelected(edit.mIsCaseSensitive);
          
          Runnable editPositive = new Runnable() {
            @Override
            public void run() {
              if(exclusion.getText().toString().trim().length() > 0) {
                edit.mExclusion = exclusion.getText().toString();
                edit.mIsCaseSensitive = caseSensitive.isSelected();
                
                exclusionAdapter.notifyDataSetChanged();
              }
            }
          };
          
          showAlertDialog(getString(R.string.action_dont_want_to_see), null, editView, null, editPositive, null, cancel, false, false);
        }
      };
      
      list.setOnItemClickListener(onClickListener);
      list.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
          getMenuInflater().inflate(R.menu.don_want_to_see_context, menu);
          
          MenuItem item = menu.findItem(R.id.dont_want_to_see_delete);
          
          item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
              ExclusionEdit edit = exclusionAdapter.getItem(((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position);
              exclusionAdapter.remove(edit);
              exclusionAdapter.notifyDataSetChanged();
              
              return true;
            }
          });
        }
      });
      
      Thread positive = new Thread() {
        @Override
        public void run() {
          SettingConstants.UPDATING_FILTER = true;
          
          final NotificationCompat.Builder builder = new NotificationCompat.Builder(TvBrowser.this);
          builder.setSmallIcon(R.drawable.ic_launcher);
          builder.setOngoing(true);
          builder.setContentTitle(getResources().getText(R.string.action_dont_want_to_see));
          builder.setContentText(getResources().getText(R.string.dont_want_to_see_refresh_notification_text));
          
          final int notifyID = 2;
          
          final NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
          notification.notify(notifyID, builder.build());
          
          updateProgressIcon(true);
          
          HashSet<String> newExclusions = new HashSet<String>();
          final ArrayList<DontWantToSeeExclusion> exclusionList = new ArrayList<DontWantToSeeExclusion>();
          
          for(ExclusionEdit edit : mCurrentExclusionList) {
            String exclusion = edit.getExclusion();
            
            newExclusions.add(exclusion);
            exclusionList.add(new DontWantToSeeExclusion(exclusion));
          }
          
          new Thread() {
            public void run() {
              Cursor programs = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE}, null, null, TvBrowserContentProvider.KEY_ID);
              
              builder.setProgress(programs.getCount(), 0, true);
              notification.notify(notifyID, builder.build());
              
              ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
              
              int keyColumn = programs.getColumnIndex(TvBrowserContentProvider.KEY_ID);
              int titleColumn = programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
              
              DontWantToSeeExclusion[] exclusionArr = exclusionList.toArray(new DontWantToSeeExclusion[exclusionList.size()]);
              
              while(programs.moveToNext()) {
                builder.setProgress(programs.getCount(), programs.getPosition(), false);
                notification.notify(notifyID, builder.build());
                
                String title = programs.getString(titleColumn);
                
                boolean filter = UiUtils.filter(getApplicationContext(), title, exclusionArr);
                long progID = programs.getLong(keyColumn);
                
                ContentValues values = new ContentValues();
                values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, filter ? 1 : 0);
                
                ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, progID));
                opBuilder.withValues(values);
                
                updateValuesList.add(opBuilder.build());
              }
              
              notification.cancel(notifyID);
              
              programs.close();
              
              if(!updateValuesList.isEmpty()) {
                try {
                  getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                  UiUtils.sendDontWantToSeeChangedBroadcast(getApplicationContext(),true);
                  handler.post(new Runnable() {
                    @Override
                    public void run() {
                      Toast.makeText(getApplicationContext(), R.string.dont_want_to_see_sync_success, Toast.LENGTH_LONG).show();
                    }
                  });
                } catch (RemoteException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                } catch (OperationApplicationException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              }
              
              updateProgressIcon(false);
              SettingConstants.UPDATING_FILTER = false;
            }
          }.start();
          
          Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
          edit.putStringSet(getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), newExclusions);
          edit.commit();
        }
      };
      
      showAlertDialog(getString(R.string.action_dont_want_to_see_edit), null, view, null, positive, null, cancel, false, true);
    }
  }
  
  public void showAlertDialog(String title, String message, View view, String positiveText, final Runnable positive, String negativeText, final Runnable negative, boolean link, boolean notCancelable) {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    builder.setCancelable(!notCancelable);
    
    if(title != null) {
      builder.setTitle(title);
    }
    if(message != null) {
      builder.setMessage(message);
    }
    else if(view != null) {
      builder.setView(view);
    }
    
    if(positive != null) {
      if(positiveText == null) {
        positiveText = getString(android.R.string.ok);
      }
      
      builder.setPositiveButton(positiveText, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if(positive != null) {
            positive.run();
          }
        }
      });
    }
    
    if(negative != null) {
      if(negativeText == null) {
        negativeText = getString(android.R.string.cancel);
      }
      
      builder.setNegativeButton(negativeText, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if(negative != null) {
            negative.run();
          }
        }
      });
    }
    
    AlertDialog d = builder.create();
    
    d.show();
    
    if(link) {
      TextView test = (TextView)d.findViewById(android.R.id.message);

      if(test != null) {
        test.setMovementMethod(LinkMovementMethod.getInstance());
      }
    }
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if(requestCode == SHOW_PREFERENCES) {
      updateFromPreferences();
    }
  }
  
  private void updateFromPreferences() {
    Fragment test1 = mSectionsPagerAdapter.getRegisteredFragment(1);
    
    if(test1 instanceof DummySectionFragment) {
      ((DummySectionFragment)test1).updateChannels();
    }
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    
    Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(2);
    
    if(fragment instanceof FavoritesFragment) {
      ((FavoritesFragment)fragment).updateSynchroButton(null);
    }
    
    boolean programTableActivated = pref.getBoolean(getResources().getString(R.string.PROG_TABLE_ACTIVATED), getResources().getBoolean(R.bool.prog_table_default));
    Fragment test = mSectionsPagerAdapter.getRegisteredFragment(3);
    
    if(!programTableActivated && test instanceof ProgramTableFragment) {
      mSectionsPagerAdapter.destroyItem(mViewPager, 3, mSectionsPagerAdapter.getRegisteredFragment(3));
      mSectionsPagerAdapter.notifyDataSetChanged();
      actionBar.removeTabAt(3);
    }
    else if(!(test instanceof ProgramTableFragment) && programTableActivated) {
      actionBar.addTab(actionBar.newTab()
          .setText(mSectionsPagerAdapter.getPageTitle(3)).setTabListener(this));
      mSectionsPagerAdapter.instantiateItem(mViewPager, 3);
      mSectionsPagerAdapter.notifyDataSetChanged();
    }
    else if(test instanceof ProgramTableFragment) {
      if(!((ProgramTableFragment)test).checkTimeBlockSize() && !((ProgramTableFragment)test).updateTable()) {
        ((ProgramTableFragment)test).updateChannelBar();
      }
    }
    
    if(mSendLogItem != null) {
      mSendLogItem.setVisible(pref.getBoolean(getResources().getString(R.string.WRITE_LOG), false));
      mDeleteLogItem.setVisible(mSendLogItem.isVisible());
    }
    
    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(SettingConstants.UPDATE_TIME_BUTTONS));
  }
  
  private void showAbout() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    RelativeLayout about = (RelativeLayout)getLayoutInflater().inflate(R.layout.about, null);
    
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      TextView version = (TextView)about.findViewById(R.id.version);
      version.setText(pInfo.versionName);
    } catch (NameNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    ((TextView)about.findViewById(R.id.license)).setText(Html.fromHtml(getResources().getString(R.string.license)));
    
    TextView androidVersion = (TextView)about.findViewById(R.id.android_version);
    androidVersion.setText(Build.VERSION.RELEASE);
    
    ((TextView)about.findViewById(R.id.rundate_value)).setText(DateFormat.getLongDateFormat(getApplicationContext()).format(mRundate.getTime()));
    
    builder.setTitle(R.string.action_about);
    builder.setView(about);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    builder.show();
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_username_password:
      {  
        showUserSetting(false);
    }
      break;
      case R.id.action_dont_want_to_see_edit:
        editDontWantToSee();
        break;
      case R.id.action_synchronize_dont_want_to_see:
        if(isOnline()) {
          synchronizeDontWantToSee();
        }
        else {
          showNoInternetConnection(null);
        }
        break;
      case R.id.action_synchronize_channels:
        if(isOnline()) {
          syncronizeChannels();
        }
        else {
          showNoInternetConnection(null);
        }
        break;
      case R.id.action_delete_log:
        {
          final File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"tvbrowserdata");
          
          File logFile = new File(path,"log.txt");
                   
          AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
          if(logFile.isFile()) {  
            if(logFile.delete()) {
              builder.setTitle(R.string.log_file_delete_title);
              builder.setMessage(R.string.log_file_delete_message);
            }
            else {
              builder.setTitle(R.string.log_file_no_delete_title);
              builder.setMessage(R.string.log_file_no_delete_message);
              logFile.deleteOnExit();
            }
          }
          else {
            builder.setTitle(R.string.no_log_file_title);
            builder.setMessage(R.string.no_log_file_delete_message);
          }
          

          
          builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {}
          });
          
          builder.show();
        }
        break;
      case R.id.action_send_log:
        {
          final File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"tvbrowserdata");
          
          File logFile = new File(path,"log.txt");

          if(logFile.isFile()) {
            Intent sendMail = new Intent(Intent.ACTION_SEND);
            
            sendMail.putExtra(Intent.EXTRA_EMAIL, new String[]{"android@tvbrowser.org"});
            sendMail.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.log_send_mail_subject));
            sendMail.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.log_send_mail_content) + " " + new Date().toString());
            sendMail.setType("text/rtf");
            sendMail.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + logFile.getAbsolutePath()));
            startActivity(Intent.createChooser(sendMail, getResources().getString(R.string.log_send_mail)));
          }
          else {
            AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
            
            builder.setTitle(R.string.no_log_file_title);
            builder.setMessage(R.string.no_log_file_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {}
            });
            
            builder.show();
          }
        }
        break;
      case R.id.action_basic_preferences:
        Intent startPref = new Intent(this, TvbPreferencesActivity.class);
        startActivityForResult(startPref, SHOW_PREFERENCES);
        break;
      case R.id.action_update:
        if(isOnline()) {
          checkTermsAccepted();
        }
        else {
          showNoInternetConnection(null);
        }
        break;
      case R.id.action_about: showAbout();break;
      case R.id.action_load_channels_again: selectChannels(true);break;
      case R.id.action_select_channels: selectChannels(false);break;
      case R.id.action_sort_channels: sortChannels();break;
      case R.id.action_delete_all_data: getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.KEY_ID + " > 0", null);
                                        getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, TvBrowserContentProvider.KEY_ID + " > 0", null);
                                        break;
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.tv_browser, menu);
    
    //  Associate searchable configuration with the SearchView
    SearchManager searchManager =
           (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView =
            (SearchView) menu.findItem(R.id.search).getActionView();
    searchView.setSearchableInfo(
            searchManager.getSearchableInfo(getComponentName()));
    
    mUpdateItem = menu.findItem(R.id.action_update);
    
   // menu.findItem(R.id.action_synchronize_dont_want_to_see).setVisible(false);
    menu.findItem(R.id.action_synchronize_favorites).setVisible(false);
    
    if(mUpdateItem != null && TvDataUpdateService.IS_RUNNING) {
      mUpdateItem.setActionView(R.layout.progressbar);
      addUpdateBroadcastReceiver();
    }
    
    mSendLogItem = menu.findItem(R.id.action_send_log);
    mDeleteLogItem = menu.findItem(R.id.action_delete_log);
    
    mSendLogItem.setVisible(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(getResources().getString(R.string.WRITE_LOG), false));
    mDeleteLogItem.setVisible(mSendLogItem.isVisible());
    
    return true;
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in
    // the ViewPager.
    mViewPager.setCurrentItem(tab.getPosition());
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one
   * of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
    
    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a DummySectionFragment (defined as a static inner class
      // below) with the page number as its lone argument.
      Fragment fragment = null;
      
      if(position < 2) {
        fragment = new DummySectionFragment();
      }
      else if(position == 2) {
        fragment = new FavoritesFragment();
      }
      else if(position == 3) {
        fragment = new ProgramTableFragment();
      }
      
      Bundle args = new Bundle();
      args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
      fragment.setArguments(args);
      
      return fragment;
    }

    @Override
    public int getCount() {
      // Show 3 total pages.
      if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getResources().getString(R.string.PROG_TABLE_ACTIVATED), getResources().getBoolean(R.bool.prog_table_default))) {
        return 4;
      }
      
      return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      Locale l = Locale.getDefault();
            
      switch (position) {
        case 0:
          return getString(R.string.title_running_programs).toUpperCase(l);
        case 1:
          return getString(R.string.title_programs_list).toUpperCase(l);
        case 2:
          return getString(R.string.title_favorites).toUpperCase(l);
        case 3:
          return getString(R.string.title_program_table).toUpperCase(l);
      }
      return null;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
  }
  
  public void updateProgressIcon(final boolean progress) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if(progress) {
          mUpdateItem.setActionView(R.layout.progressbar);
        }
        else {
          mUpdateItem.setActionView(null);
        }        
      }
    });
  }
  
  public void showProgramsListTab() {
    mViewPager.setCurrentItem(1,true);
    mProgramsListWasShow = true;
   
  }
  
  @Override
  public void onBackPressed() {
    if(mProgramsListWasShow) {
      mProgramsListWasShow = false;
      mViewPager.setCurrentItem(0,true);
    }
    else {
      super.onBackPressed();
    }
  }
}
