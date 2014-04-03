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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
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
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.TvbPreferencesActivity;
import org.tvbrowser.settings.SettingConstants;

import billing.util.IabHelper;
import billing.util.IabHelper.QueryInventoryFinishedListener;
import billing.util.IabResult;
import billing.util.Inventory;
import billing.util.Purchase;
import billing.util.SkuDetails;

import android.app.ActionBar;
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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
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
  private MenuItem mSendDataUpdateLogItem;
  private MenuItem mDeleteDataUpdateLogItem;
  private MenuItem mSendReminderLogItem;
  private MenuItem mDeleteReminderLogItem;
  private MenuItem mScrollTimeItem;
  
  private MenuItem mDebugMenuItem;
  
  private MenuItem mPauseReminder;
  private MenuItem mContinueReminder;
  
  private static final Calendar mRundate;
  private static final int[] SCROLL_IDS = new int[] {-1,-2,-3,-4,-5,-6};
  private static int[] SCROLL_TIMES = new int[6];
  
  private boolean mSelectionNumberChanged;
  
  private boolean mIsActive;
  private boolean mProgramsListWasShow;
  private int mLastSelectedTab;
  
  private static String ALL_VALUE;
  
  private Menu mOptionsMenu;
  
  private Stack<ProgramsListState> mProgamListStateStack;
  private BroadcastReceiver mUpdateDoneBroadcastReceiver;
  
  private long mResumeTime;
  private IabHelper mHelper;
  
  static {
    mRundate = Calendar.getInstance();
    mRundate.set(Calendar.YEAR, 2015);
    mRundate.set(Calendar.MONTH, Calendar.JANUARY);
    mRundate.set(Calendar.DAY_OF_MONTH, 1);
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putBoolean(SettingConstants.UPDATE_RUNNING_KEY, updateRunning);
    outState.putBoolean(SettingConstants.SELECTION_CHANNELS_KEY, selectingChannels);
    outState.putBoolean(SettingConstants.REMINDER_STATE_KEY, SettingConstants.IS_REMINDER_PAUSED);

    super.onSaveInstanceState(outState);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    PrefUtils.initialize(TvBrowser.this);
    
    if(PrefUtils.getBooleanValue(R.string.DARK_STYLE, R.bool.dark_style_default)) {
      setTheme(android.R.style.Theme_Holo);
      
      SettingConstants.IS_DARK_THEME = true;
    }
    else {
      SettingConstants.IS_DARK_THEME = false;
    }
    
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      
      int oldVersion = PrefUtils.getIntValueWithDefaultKey(R.string.OLD_VERSION, R.integer.old_version_default);
      
      if(oldVersion < 167) {
        Set<String> favoritesSet = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
        
        HashSet<String> newFavorites = new HashSet<String>();
        
        for(String favorite : favoritesSet) {
          String[] values = favorite.split(";;");
          
          boolean remind = false;
          
          if(values.length > 3) {
            remind = Boolean.valueOf(values[3]);
          }
          
          Favorite fav = new Favorite(values[0], values[1], Boolean.valueOf(values[2]), remind);
          
          if(fav.isValid()) {
            newFavorites.add(favorite);
          }
          else {
            Favorite.removeFavoriteMarking(TvBrowser.this, getContentResolver(), fav);
          }
        }
        
        Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
        edit.putStringSet(SettingConstants.FAVORITE_LIST, newFavorites);
        edit.commit();
      }
      if(oldVersion !=  pInfo.versionCode) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
        edit.putInt(getString(R.string.OLD_VERSION), pInfo.versionCode);
        edit.commit();
      }
    } catch (NameNotFoundException e) {}
    
    super.onCreate(savedInstanceState);
    
    SettingConstants.updateLogoMap(TvBrowser.this);
    
    setContentView(R.layout.activity_tv_browser);
    
    handler = new Handler();
    
    mProgamListStateStack = new Stack<ProgramsListState>();
    
    ALL_VALUE = getResources().getString(R.string.filter_channel_all);
    
    if(savedInstanceState != null) {
      updateRunning = savedInstanceState.getBoolean(SettingConstants.UPDATE_RUNNING_KEY, false);
      selectingChannels = savedInstanceState.getBoolean(SettingConstants.SELECTION_CHANNELS_KEY, false);
      SettingConstants.IS_REMINDER_PAUSED = savedInstanceState.getBoolean(SettingConstants.REMINDER_STATE_KEY, false);
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
              ((ProgramTableFragment)fragment).scrollToTime(0, mScrollTimeItem);
            }
            
            mProgramsListWasShow = false;
            
            if(position != 1) {
              mProgamListStateStack.clear();
            }
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
    
    int startTab = Integer.parseInt(PrefUtils.getStringValue(R.string.TAB_TO_SHOW_AT_START, R.string.tab_to_show_at_start_default));
    
    if(mSectionsPagerAdapter.getCount() > startTab) {
      mViewPager.setCurrentItem(startTab);
    }
    
    IOUtils.handleDataUpdatePreferences(TvBrowser.this);
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    
    long timeDiff = System.currentTimeMillis() - mResumeTime + PrefUtils.getLongValueWithDefaultKey(R.string.PREF_RUNNING_TIME, R.integer.pref_running_time_default);
    
    Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
    edit.putLong(getString(R.string.PREF_RUNNING_TIME), timeDiff);
    edit.commit();
    
    mIsActive = false;
    
    if(mTimer != null) {
      mTimer.cancel();
    }
    
    if(mUpdateDoneBroadcastReceiver != null) {
      LocalBroadcastManager.getInstance(TvBrowser.this).unregisterReceiver(mUpdateDoneBroadcastReceiver);
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
    
    mResumeTime = System.currentTimeMillis();
    
    mIsActive = true;
    showTerms();
    
    mUpdateDoneBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateProgressIcon(false);
        
        boolean fromRemider = PrefUtils.getBooleanValue(R.string.PREF_SYNC_REMINDERS_FROM_DESKTOP, R.bool.pref_sync_reminders_from_desktop_default);
        boolean toRemider = PrefUtils.getBooleanValue(R.string.PREF_SYNC_REMINDERS_TO_DESKTOP, R.bool.pref_sync_reminders_to_desktop_default);
        
        if(fromRemider) {
          synchronizeRemindersDown(false);
        }
        if(toRemider) {
          synchronizeUp(false,null,"http://android.tvbrowser.org/data/scripts/syncUp.php?type=reminderFromApp");
        }
      }
    };
    
    IntentFilter filter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mUpdateDoneBroadcastReceiver, filter);
  }
  
  private void handleResume() {
    new Thread() {
      @Override
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
      
      Calendar test = (Calendar)mRundate.clone();
      test.add(Calendar.DAY_OF_YEAR, 7);
      
      final int diff = (int)((test.getTimeInMillis() - System.currentTimeMillis()) / (24 * 60 * 60000));
      
      String expiredMessage = diff == 0 ? getString(R.string.versionExpiredMsgLast) : getString(R.string.versionExpiredMsg);
      
      expiredMessage = expiredMessage.replace("{0}", String.valueOf(diff));
      
      String updateText = SettingConstants.GOOGLE_PLAY ? getString(R.string.update_google_play) : getString(R.string.update_website);
      
      builder.setMessage(expiredMessage);
      builder.setPositiveButton(updateText, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if(SettingConstants.GOOGLE_PLAY) {
            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
            }
          }
          else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.tvbrowser.org/index.php?id=download")));
          }
          
          System.exit(0);
        }
      });
      builder.setNegativeButton(R.string.update_not_now, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if(diff < 0) {
            System.exit(0);
          }
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
    
    IOUtils.closeSafely(channels);
    
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
          new Thread() {
            @Override
			public void run() {
              try {
                sleep(60000);
              } catch (InterruptedException e1) {}
              
              Calendar cal2 = Calendar.getInstance();
              cal2.add(Calendar.DAY_OF_YEAR, -2);
              
              try {
                getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + cal2.getTimeInMillis(), null);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SettingConstants.DATA_UPDATE_DONE));
              }catch(IllegalArgumentException e) {}
            }
          }.start();
          
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
    
    IOUtils.closeSafely(test);
    
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
      @Override
	public void run() {
        boolean somethingSynchonized = false;
        
        if(replace) {
          ContentValues values = new ContentValues();
          values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 0);
          values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, 0);
          
          getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.KEY_ID + " >= 0 " , null);
          getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, TvBrowserContentProvider.KEY_ID + " >= 0", null);
              
          if(getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null) > 0) {
            somethingSynchonized = true;
          }
        }
        
        URL documentUrl;
        try {
          documentUrl = new URL("http://android.tvbrowser.org/data/scripts/syncDown.php?type=channelsFromDesktop");
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
                    
                    IOUtils.closeSafely(group);
                    
                    sort++;
                  }
                }
              }
            }
            
            if(somethingSynchonized) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  SettingConstants.updateLogoMap(TvBrowser.this);
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
  
  private byte[] getXmlBytes() {
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.CHANNEL_TABLE + "." + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.GROUP_KEY_GROUP_ID,
        TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY
    };
    
    StringBuilder where = new StringBuilder();
    
    where.append(" ( ").append(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER).append(" OR ").append(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER).append(" ) ") ;
    
    Cursor programs = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where.toString(), null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    StringBuilder dat = new StringBuilder();
    
    SparseArray<SimpleGroupInfo> groupInfo = new SparseArray<SimpleGroupInfo>();
    
    if(programs.getCount() > 0) {
      int startTimeColumnIndex = programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      int groupKeyColumnIndex = programs.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      int channelKeyBaseCountryColumnIndex = programs.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY);
      
      String[] groupProjection = {
          TvBrowserContentProvider.KEY_ID,
          TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID,
          TvBrowserContentProvider.GROUP_KEY_GROUP_ID
      };
      
      Cursor groups = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupProjection, null, null, null);
      
      if(groups.getCount() > 0) {
        while(groups.moveToNext()) {
          int groupKey = groups.getInt(0);
          String dataServiceID = groups.getString(1);
          String groupID = groups.getString(2);
          
          if(dataServiceID.equals(SettingConstants.EPG_FREE_KEY)) {
            dataServiceID = "1";
          }
          
          groupInfo.put(groupKey, new SimpleGroupInfo(dataServiceID, groupID));
        }
      }
      
      IOUtils.closeSafely(groups);
      
      if(groupInfo.size() > 0) {
        while(programs.moveToNext()) {
          int groupID = programs.getInt(groupKeyColumnIndex);
          long startTime = programs.getLong(startTimeColumnIndex) / 60000;
          String channelID = programs.getString(1);
          String baseCountry = programs.getString(channelKeyBaseCountryColumnIndex);
          
          SimpleGroupInfo info = groupInfo.get(groupID);
          
          dat.append(startTime).append(";").append(info.mDataServiceID).append(":").append(info.mGroupID).append(":").append(baseCountry).append(":").append(channelID).append("\n");
        }
      }
    }
    
    IOUtils.closeSafely(programs);
    
    return IOUtils.getCompressedData(dat.toString().getBytes());
  }

  private static final String CrLf = "\r\n";

  private void synchronizeUp(boolean info, final String value, final String address) {
    new Thread() {
      @Override
      public void run() {

        SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
        
        String car = pref.getString(SettingConstants.USER_NAME, null);
        String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
        
        if(car != null && car.trim().length() > 0 && bicycle != null && bicycle.trim().length() > 0) {
          String userpass = car.trim() + ":" + bicycle.trim();
          String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
          
          URLConnection conn = null;
          OutputStream os = null;
          InputStream is = null;
      
          try {
              URL url = new URL(address);
              
              conn = url.openConnection();
              conn.setRequestProperty ("Authorization", basicAuth);
              conn.setDoOutput(true);
              
              byte[] xmlData = value == null ? getXmlBytes() : IOUtils.getCompressedData(value.getBytes("UTF-8"));
              
              String message1 = "";
              message1 += "-----------------------------4664151417711" + CrLf;
              message1 += "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\""+car+".gz\""
                      + CrLf;
              message1 += "Content-Type: text/plain" + CrLf;
              message1 += CrLf;
      
              // the image is sent between the messages in the multipart message.
      
              String message2 = "";
              message2 += CrLf + "-----------------------------4664151417711--"
                      + CrLf;
      
              conn.setRequestProperty("Content-Type",
                      "multipart/form-data; boundary=---------------------------4664151417711");
              // might not need to specify the content-length when sending chunked
              // data.
              conn.setRequestProperty("Content-Length", String.valueOf((message1
                      .length() + message2.length() + xmlData.length)));
      
              Log.d("info8","open os");
              os = conn.getOutputStream();
      
              Log.d("info8",message1);
              os.write(message1.getBytes());
              
              // SEND THE IMAGE
              int index = 0;
              int size = 1024;
              do {
                Log.d("info8","write:" + index);
                  if ((index + size) > xmlData.length) {
                      size = xmlData.length - index;
                  }
                  os.write(xmlData, index, size);
                  index += size;
              } while (index < xmlData.length);
              
              Log.d("info8","written:" + index);
      
              Log.d("info8",message2);
              os.write(message2.getBytes());
              os.flush();
      
              Log.d("info8","open is");
              is = conn.getInputStream();
      
              char buff = 512;
              int len;
              byte[] data = new byte[buff];
              do {
                Log.d("info8","READ");
                  len = is.read(data);
      
                  if (len > 0) {
                    Log.d("info8",new String(data, 0, len));
                  }
              } while (len > 0);
      
              Log.d("info8","DONE");
          } catch (Exception e) {
            /*int response = 0;
            
            if(conn != null) {
              try {
                response = ((HttpURLConnection)conn).getResponseCode();
              } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              }
            }*/
            
              Log.d("info8", "" ,e);
          } finally {
            Log.d("info8","Close connection");
            IOUtils.closeSafely(os);
            IOUtils.closeSafely(is);
          }
        }
      }
    }.start();
  }
  
  private void synchronizeRemindersDown(boolean info) {
    new Thread() {
      @Override
	public void run() {
        if(!SettingConstants.UPDATING_REMINDERS) {
          SettingConstants.UPDATING_REMINDERS = true;
          
          URL documentUrl;
          
          try {
            documentUrl = new URL("http://android.tvbrowser.org/data/scripts/syncDown.php?type=reminderFromDesktop");
            URLConnection connection = documentUrl.openConnection();
            
            SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
            
            String car = pref.getString(SettingConstants.USER_NAME, null);
            String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
            
            if(car != null && bicycle != null && car.trim().length() > 0 && bicycle.trim().length() > 0) {
              updateProgressIcon(true);
              
              String userpass = car + ":" + bicycle;
              String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
              
              connection.setRequestProperty ("Authorization", basicAuth);
              
              BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),"UTF-8"));
              
              String reminder = null;
              
              ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
              ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
              
              while((reminder = read.readLine()) != null) {
                if(reminder != null && reminder.contains(";") && reminder.contains(":")) {
                  String[] parts = reminder.split(";");
                  
                  long time = Long.parseLong(parts[0]) * 60000;
                  String[] idParts = parts[1].split(":");
                
                  if(idParts[0].equals("1")) {
                    String dataService = "EPG_FREE";
                    
                    String where = " ( " +TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = \"" + dataService + "\" ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = \"" + idParts[1] + "\" ) ";
                    
                    Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
                    
                    if(group.moveToFirst()) {
                      int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                      
                      where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " IS " + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=\'" + idParts[2] + "\' ) ";
                      
                      Cursor channel = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where, null, null);
                      
                      if(channel.moveToFirst()) {
                        int channelId = channel.getInt(channel.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                        
                        where = " ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channelId + " ) AND ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " = " + time + " ) " + " AND ( NOT " + TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER + " ) ";
                        
                        Cursor program = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, where, null, null);
                        
                        if(program.moveToFirst()) {
                          boolean marked = program.getInt(program.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER)) == 1;
                                                    
                          if(!marked) {
                            ContentValues values = new ContentValues();
                            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, true);
                            
                            long programID = program.getLong(program.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                            
                            ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID));
                            opBuilder.withValues(values);
                            
                            updateValuesList.add(opBuilder.build());
                            
                            Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
                            intent.putExtra(SettingConstants.MARKINGS_ID, programID);
                            
                            markingIntentList.add(intent);
                                                        
                            UiUtils.addReminder(TvBrowser.this, programID, time, TvBrowser.class);
                          }
                        }
                        
                        IOUtils.closeSafely(program);
                      }
                      
                      IOUtils.closeSafely(channel);
                    }
                    
                    IOUtils.closeSafely(group);
                  }
                }
              }
              
              if(!updateValuesList.isEmpty()) {
                try {
                  getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                  
                  LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(TvBrowser.this);
                  
                  for(Intent markUpdate : markingIntentList) {
                    localBroadcast.sendBroadcast(markUpdate);
                  }
                } catch (RemoteException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                } catch (OperationApplicationException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              }
            }
          }catch(Exception e) {
            Log.d("info", "", e);
            
          }
  
          updateProgressIcon(false);
          
          SettingConstants.UPDATING_REMINDERS = false;
        }
      }
    }.start();
  }
  
  private void synchronizeDontWantToSee(final boolean replace) {
    new Thread() {
      @Override
	public void run() {
        if(!SettingConstants.UPDATING_FILTER) {
          SettingConstants.UPDATING_FILTER = true;
          
          NotificationCompat.Builder builder;
          
          builder = new NotificationCompat.Builder(TvBrowser.this);
          builder.setSmallIcon(R.drawable.ic_stat_notification);
          builder.setOngoing(true);
          builder.setContentTitle(getResources().getText(R.string.action_dont_want_to_see));
          builder.setContentText(getResources().getText(R.string.dont_want_to_see_notification_text));
          
          int notifyID = 2;
          
          NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
          notification.notify(notifyID, builder.build());
          
          updateProgressIcon(true);
                  
          URL documentUrl;
          
          try {
            documentUrl = new URL("http://android.tvbrowser.org/data/scripts/syncDown.php?type=dontWantToSee");
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
              
              StringBuilder exclusionBuilder = new StringBuilder();
              HashSet<String> exclusions = new HashSet<String>();
              ArrayList<DontWantToSeeExclusion> exclusionList = new ArrayList<DontWantToSeeExclusion>();
              
              while((line = read.readLine()) != null) {
                if(line.contains(";;") && line.trim().length() > 0) {
                  exclusions.add(line);
                  exclusionList.add(new DontWantToSeeExclusion(line));
                  exclusionBuilder.append(line).append("\n");
                }
              }

              String key = getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES);
              SharedPreferences pref1 = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
              
              Set<String> oldValues = pref1.getStringSet(key, null);
              
              if(exclusions.size() > 0) {
                if(!replace && oldValues != null) {
                  for(String old : oldValues) {
                    if(!exclusions.contains(old)) {
                      exclusions.add(old);
                      exclusionList.add(new DontWantToSeeExclusion(old));
                      exclusionBuilder.append(old).append("\n");
                    }
                  }
                }
                
                Editor edit = pref1.edit();
                
                edit.putStringSet(key, exclusions);
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
                
                IOUtils.closeSafely(c);
                
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
                
                if(!replace && exclusionBuilder.length() > 0) {
                  synchronizeUp(false, exclusionBuilder.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=dontWantToSee");
                }
              }
              else {
                if(!replace && oldValues != null && !oldValues.isEmpty()) {
                  for(String old : oldValues) {
                    exclusionBuilder.append(old).append("\n");
                  }
                  
                  synchronizeUp(false, exclusionBuilder.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=dontWantToSee");
                }
                
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
          
          notification.cancel(notifyID);
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
      return value == null || mCountry.toLowerCase(Locale.getDefault()).contains(value.toLowerCase(Locale.getDefault()));
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
    
    @Override
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
	private static final long serialVersionUID = 4735217720951379320L;
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
    
    @Override
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
      
      if(logo != null && logo.length > 0) {
        channelLogo = BitmapFactory.decodeByteArray(logo, 0, logo.length);
        
        if(channelLogo != null) {
          BitmapDrawable l = new BitmapDrawable(getResources(), channelLogo);
          
          ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
          background.setBounds(0, 0, channelLogo.getWidth()+2,channelLogo.getHeight()+2);
          
          LayerDrawable logoDrawable = new LayerDrawable(new Drawable[] {background,l});
          logoDrawable.setBounds(background.getBounds());
          
          l.setBounds(2, 2, channelLogo.getWidth(), channelLogo.getHeight());
          
          channelLogo = UiUtils.drawableToBitmap(logoDrawable);
        }
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
    
    IOUtils.closeSafely(channels);
    
    // create filter for filtering of category and country
    final ChannelFilter filter = new ChannelFilter(SettingConstants.TV_CATEGORY, null);
    
    channelSelectionList.setFilter(filter);

    // Custom array adapter for channel selection
    final ArrayAdapter<ChannelSelection> channelSelectionAdapter = new ArrayAdapter<ChannelSelection>(TvBrowser.this, R.layout.channel_row, channelSelectionList) {
      @Override
	public View getView(int position, View convertView, ViewGroup parent) {
        ChannelSelection value = getItem(position);
        ViewHolder holder = null;
        
        if (convertView == null) {
          LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          
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
          // default logo
          holder.mLogo.setImageResource( R.drawable.ic_launcher);
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
            SettingConstants.updateLogoMap(TvBrowser.this);
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
    
    @Override
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
          
          boolean success = intent.getBooleanExtra(SettingConstants.CHANNEL_DOWNLOAD_SUCCESSFULLY, false);
          
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
    
    IOUtils.closeSafely(channels);
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
            
            BitmapDrawable l = new BitmapDrawable(getResources(), channelLogo);
            
            ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
            background.setBounds(0, 0, channelLogo.getWidth()+2,channelLogo.getHeight()+2);
            
            LayerDrawable logoDrawable = new LayerDrawable(new Drawable[] {background,l});
            logoDrawable.setBounds(background.getBounds());
            
            l.setBounds(2, 2, channelLogo.getWidth(), channelLogo.getHeight());
            
            channelLogo = UiUtils.drawableToBitmap(logoDrawable);
          }
                    
          channelSource.add(new ChannelSort(key, name, order, channelLogo));
        }while(channels.moveToNext());
      }
      
      IOUtils.closeSafely(channels);
      
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

      final ArrayAdapter<ChannelSort> aa = new ArrayAdapter<TvBrowser.ChannelSort>(TvBrowser.this, R.layout.channel_sort_row, channelSource) {
        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
          ChannelSort value = getItem(position);
          ViewHolder holder = null;
          
          if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
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
            // default logo
            holder.mLogo.setImageResource(R.drawable.ic_launcher);
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
          number.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
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
    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    
    if(netInfo != null && netInfo.isConnectedOrConnecting()) {
      return true;
    }
    
    return false;
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
        
        String daysToDownload = PrefUtils.getStringValue(R.string.DAYS_TO_DOWNLOAD, R.string.days_to_download_default);
        
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
        
        IOUtils.closeSafely(test2);
        
        selectChannels(loadAgain);
      }
      
      IOUtils.closeSafely(test);
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
    
    updateSynchroMenu();
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
        @Override
		public void run() {
          URL documentUrl;
          try {
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
      Set<String> currentExclusions = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);
      
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
          builder.setSmallIcon(R.drawable.ic_stat_notification);
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
            @Override
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
              
              IOUtils.closeSafely(programs);
              
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
 // Pass on the activity result to the helper for handling
    if(mHelper != null) {
      if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
          // not handled, so handle it ourselves (here's where you'd
          // perform any handling of activity results not related to in-app
          // billing...
          super.onActivityResult(requestCode, resultCode, data);
      }
      else {
          Log.d("info", "onActivityResult handled by IABUtil.");
      }
    }
    else {
      super.onActivityResult(requestCode, resultCode, data);
    }
    
    if(requestCode == SHOW_PREFERENCES) {
      updateFromPreferences();
    }
  }
  
  private void updateFromPreferences() {
    Fragment test1 = mSectionsPagerAdapter.getRegisteredFragment(1);
    
    if(test1 instanceof DummySectionFragment) {
      ((DummySectionFragment)test1).updateChannels();
    }

    Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(2);
    
    if(fragment instanceof FavoritesFragment) {
      ((FavoritesFragment)fragment).updateSynchroButton(null);
      ((FavoritesFragment)fragment).updateProgramsList();
    }
    
    boolean programTableActivated = PrefUtils.getBooleanValue(R.string.PROG_TABLE_ACTIVATED, R.bool.prog_table_activated_default);
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
        ((ProgramTableFragment)test).updateMarkings();
      }
    }
    
    if(mDebugMenuItem != null) {
      boolean dataUpdateLogEnabled = PrefUtils.getBooleanValue(R.string.WRITE_DATA_UPDATE_LOG, R.bool.write_data_update_log_default);
      boolean reminderLogEnabled = PrefUtils.getBooleanValue(R.string.WRITE_REMINDER_LOG, R.bool.write_reminder_log_default);
      
      mDebugMenuItem.setVisible(dataUpdateLogEnabled || reminderLogEnabled);
      
      mSendDataUpdateLogItem.setEnabled(dataUpdateLogEnabled);
      mDeleteDataUpdateLogItem.setEnabled(dataUpdateLogEnabled);
      mSendReminderLogItem.setEnabled(reminderLogEnabled);
      mDeleteReminderLogItem.setEnabled(reminderLogEnabled);
    }
    
    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(SettingConstants.UPDATE_TIME_BUTTONS));
    updateScrollMenu();
    
    new UpdateAlarmValue().onReceive(TvBrowser.this, null);
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
    
    TextView lastUpdate = (TextView)about.findViewById(R.id.data_update);
    lastUpdate.setText(DateFormat.getLongDateFormat(TvBrowser.this).format(new Date(PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE, 0))));
    
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
  
  private void synchronizeDontWantToSee() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.synchronize_replace_add_title);
    builder.setMessage(R.string.synchronize_replace_exclusion_add_text);
    
    builder.setPositiveButton(R.string.synchronize_add_exclusion, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        synchronizeDontWantToSee(false);
      }
    });
    
    builder.setNegativeButton(R.string.synchronize_replace_exclusion, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        synchronizeDontWantToSee(true);
      }
    });
    
    builder.show();
  }
  
  private void pauseReminder() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.action_pause_reminder);
    builder.setMessage(R.string.action_pause_reminder_text);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        SettingConstants.IS_REMINDER_PAUSED = true;
        
        mPauseReminder.setVisible(false);
        mContinueReminder.setVisible(true);
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
      }
    });
    
    builder.show();
  }
  
  private void sendLogMail(String file, String type) {
    File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    
    if(!parent.isDirectory()) {
      parent = getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
    }
    
    final File path = new File(parent,"tvbrowserdata");
    
    File logFile = new File(path,file);
    
    Log.d("Reminder", "" + logFile.getAbsolutePath() + " " + logFile.isFile());

    if(logFile.isFile()) {
      Intent sendMail = new Intent(Intent.ACTION_SEND);
      
      String subject = getString(R.string.log_send_mail_subject).replace("{0}", type);
      String text =  getString(R.string.log_send_mail_content).replace("{0}", type);
      
      sendMail.putExtra(Intent.EXTRA_EMAIL, new String[]{"android@tvbrowser.org"});
      sendMail.putExtra(Intent.EXTRA_SUBJECT, subject);
      sendMail.putExtra(Intent.EXTRA_TEXT,text + " " + new Date().toString());
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
  
  private void deleteLog(String type) {
    File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    
    if(!parent.isDirectory()) {
      parent = getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
    }
    
    final File path = new File(parent,"tvbrowserdata");
    
    File logFile = new File(path,type);
             
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
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_username_password:
      {  
        showUserSetting(false);
    }
      break;
      case R.id.action_donation: showDonationInfo(); break;
      case R.id.action_pause_reminder: pauseReminder(); break;
      case R.id.action_continue_reminder: SettingConstants.IS_REMINDER_PAUSED = false; mPauseReminder.setVisible(true); mContinueReminder.setVisible(false); break;
      case R.id.action_synchronize_reminders_down:
        if(isOnline()) {
          synchronizeRemindersDown(true);
        }
        else {
          showNoInternetConnection(null);
        }
        break;
      case R.id.action_synchronize_reminders_up:
        if(isOnline()) {
          synchronizeUp(true,null,"http://android.tvbrowser.org/data/scripts/syncUp.php?type=reminderFromApp");
        }
        else {
          showNoInternetConnection(null);
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
      case R.id.action_delete_data_update_log: deleteLog("data-update-log.txt");break;
      case R.id.action_delete_reminder_log: deleteLog("reminder-log.txt");break;
      case R.id.action_send_data_update_log:sendLogMail("data-update-log.txt",getString(R.string.log_send_data_update));break;
      case R.id.action_send_reminder_log:sendLogMail("reminder-log.txt",getString(R.string.log_send_reminder));break;
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
      case R.id.action_scroll_now:scrollToTime(0);break;
    }
    
    for(int i = 0; i < SCROLL_IDS.length; i++) {
      if(item.getItemId() == SCROLL_IDS[i]) {
        scrollToTime(SCROLL_TIMES[i]+1);
        break;
      }
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  private void scrollToTime(int time) {
    if(mViewPager.getCurrentItem() == 1) {
      Intent scroll = new Intent(SettingConstants.SCROLL_TO_TIME_INTENT);
      scroll.putExtra(SettingConstants.START_TIME_EXTRA, (long)time);
      
      LocalBroadcastManager.getInstance(TvBrowser.this).sendBroadcast(scroll);
    }
    else if(mViewPager.getCurrentItem() == 3) {
      Fragment test = mSectionsPagerAdapter.getRegisteredFragment(3);
      
      if(test instanceof ProgramTableFragment) {
        ((ProgramTableFragment)test).scrollToTime(time, mScrollTimeItem);
      }
    }
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
    }
    
    mDebugMenuItem = menu.findItem(R.id.action_debug);
    mSendDataUpdateLogItem = menu.findItem(R.id.action_send_data_update_log);
    mDeleteDataUpdateLogItem = menu.findItem(R.id.action_delete_data_update_log);
    mSendReminderLogItem = menu.findItem(R.id.action_send_reminder_log);
    mDeleteReminderLogItem = menu.findItem(R.id.action_delete_reminder_log);
    mScrollTimeItem = menu.findItem(R.id.action_scroll);
    
    mPauseReminder = menu.findItem(R.id.action_pause_reminder);
    mContinueReminder = menu.findItem(R.id.action_continue_reminder);
    
    mPauseReminder.setVisible(!SettingConstants.IS_REMINDER_PAUSED);
    mContinueReminder.setVisible(SettingConstants.IS_REMINDER_PAUSED);
    
    mScrollTimeItem.setVisible(mViewPager.getCurrentItem() == 1 || mViewPager.getCurrentItem() == 3);

    boolean dataUpdateLogEnabled = PrefUtils.getBooleanValue(R.string.WRITE_DATA_UPDATE_LOG, R.bool.write_data_update_log_default);
    boolean reminderLogEnabled = PrefUtils.getBooleanValue(R.string.WRITE_REMINDER_LOG, R.bool.write_reminder_log_default);
    
    mDebugMenuItem.setVisible(dataUpdateLogEnabled || reminderLogEnabled);
    
    mSendDataUpdateLogItem.setEnabled(dataUpdateLogEnabled);
    mDeleteDataUpdateLogItem.setEnabled(dataUpdateLogEnabled);
    mSendReminderLogItem.setEnabled(reminderLogEnabled);
    mDeleteReminderLogItem.setEnabled(reminderLogEnabled);
    
    updateScrollMenu();
    
    mOptionsMenu = menu;
    
    updateSynchroMenu();
    
    return true;
  }
  
  private void updateSynchroMenu() {
    SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
    
    String car = pref.getString(SettingConstants.USER_NAME, null);
    String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
    
    boolean isAccount = (car != null && car.trim().length() > 0 && bicycle != null && bicycle.trim().length() > 0);
    
    if(mOptionsMenu != null) {
      mOptionsMenu.findItem(R.id.action_synchronize_channels).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_dont_want_to_see).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_favorites).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_reminders_up).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_reminders_down).setEnabled(isAccount);
    }
  }
  
  private void updateScrollMenu() {
    if(mScrollTimeItem != null) {
      SubMenu subMenu = mScrollTimeItem.getSubMenu();
      
      for(int i = 0; i < SCROLL_IDS.length; i++) {
        subMenu.removeItem(SCROLL_IDS[i]);
      }
      
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
      
      ArrayList<Integer> values = new ArrayList<Integer>();
      
      int[] defaultValues = getResources().getIntArray(R.array.time_button_defaults);
      
      for(int i = 1; i <= 6; i++) {
        try {
          Class<?> string = R.string.class;
          
          Field setting = string.getDeclaredField("TIME_BUTTON_" + i);
          
          Integer value = Integer.valueOf(pref.getInt(getResources().getString((Integer)setting.get(string)), defaultValues[i-1]));
          
          if(value >= -1 && !values.contains(value)) {
            values.add(value);
          }
        } catch (Exception e) {}
      }
      
      if(PrefUtils.getBooleanValue(R.string.SORT_RUNNING_TIMES, R.bool.sort_running_times_default)) {
        Collections.sort(values);
      }
            
      for(int i = 0; i < values.size(); i++) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, values.get(i) / 60);
        cal.set(Calendar.MINUTE, values.get(i) % 60);
        
        SCROLL_TIMES[i] = values.get(i).intValue();
        
        subMenu.add(100, SCROLL_IDS[i], i+1, DateFormat.getTimeFormat(TvBrowser.this).format(cal.getTime()));
      }
    }
  }
  
  @Override
  public void onTabSelected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in
    // the ViewPager.
    mViewPager.setCurrentItem(tab.getPosition());
    
    if(mScrollTimeItem != null) {
      switch(tab.getPosition()) {
        case 1:
        case 3:mScrollTimeItem.setVisible(true);break;
        
        default:mScrollTimeItem.setVisible(false);break;
      }
    }
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
      if(PrefUtils.getBooleanValue(R.string.PROG_TABLE_ACTIVATED, R.bool.prog_table_activated_default)) {
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
    if(mViewPager.getCurrentItem() != 1) {
      mLastSelectedTab = mViewPager.getCurrentItem();
      mViewPager.setCurrentItem(1,true);
      mProgramsListWasShow = true;
    }
  }
  
  private void setRatingAndDonationInfoShown() {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
    edit.putBoolean(getString(R.string.PREF_RATING_DONATION_INFO_SHOWN), true);
    edit.commit();
  }
  
  private void showInAppDonations(final Inventory inv, boolean showIfAlreadyDonated) {
    mUpdateItem.setActionView(null);
    AlertDialog.Builder alert = new AlertDialog.Builder(TvBrowser.this);
    
    alert.setTitle(R.string.donation);
    
    View view = getLayoutInflater().inflate(R.layout.in_app_donations, null);
    LinearLayout layout = (LinearLayout)view.findViewById(R.id.donation_in_app_layout);
    
    alert.setView(view);
    
    alert.setNegativeButton(getString(R.string.not_now).replace("{0}", ""), new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    
    alert.setPositiveButton(R.string.donation_info_website, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.tvbrowser.org/index.php?id=donations")));
      }
    });
    
    final AlertDialog d = alert.create();
    
    View.OnClickListener onDonationClick = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        d.dismiss();
        
        openDonation((SkuDetails)v.getTag());
      }
    };
    
    Purchase donated = null;
    SkuDetails donatedDetails = null;
    
    for(String sku : SettingConstants.SKU_LIST) {
      SkuDetails details = inv.getSkuDetails(sku);
      Purchase donatedTest = inv.getPurchase(sku);
      Log.d("info"," donated " + donated);
      if(donatedTest != null && details != null) {
        donated = donatedTest;
        donatedDetails = details;
      }
      if(details != null) {
        String title = details.getTitle().substring(0,details.getTitle().indexOf("(")-1);
        
        Button donation = new Button(this);
        donation.setTextSize(UiUtils.convertDpToPixel(12, getResources()));
        donation.setText(title + ": " + details.getPrice());
        donation.setTag(details);
        donation.setOnClickListener(onDonationClick);
      
        layout.addView(donation);
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)donation.getLayoutParams();
        params.setMargins(5, 0, 5, 5); //left, top, right, bottom
        donation.setLayoutParams(params);
      }
    }
    
    if(donated == null || showIfAlreadyDonated) {
      d.show();
    }
    else if(donated != null) {
      AlertDialog.Builder alert2 = new AlertDialog.Builder(TvBrowser.this);
      
      alert2.setTitle(R.string.donation);
      
      String message = getString(R.string.already_donated).replace("{1}", DateFormat.getLongDateFormat(this).format(new Date(donated.getPurchaseTime()))).replace("{0}", donatedDetails.getPrice());
      
      alert2.setMessage(message);
      
      final Purchase toConsume = donated;
      
      alert2.setPositiveButton(R.string.donate_again, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          mUpdateItem.setActionView(R.layout.progressbar);
          
          mHelper.consumeAsync(toConsume,new IabHelper.OnConsumeFinishedListener() {
            @Override
            public void onConsumeFinished(Purchase purchase, IabResult result) {
              mUpdateItem.setActionView(null);
              
              if(result.isSuccess()) {
                d.show();
              }
              else {
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    Toast.makeText(TvBrowser.this, "", Toast.LENGTH_LONG).show();
                  }
                });
              }
            }
          });
        }
      });
      
      alert2.setNegativeButton(R.string.stop_donation, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {}
      });
      
      alert2.show();
    }
  }
  
  @Override
  public void onDestroy() {
     super.onDestroy();
     
     if (mHelper != null) {
       mHelper.dispose();
     }
     
     mHelper = null;
  }
  
  private void showInAppError(String error) {
    mUpdateItem.setActionView(null);
    
    AlertDialog.Builder alert = new AlertDialog.Builder(TvBrowser.this);
    
    alert.setTitle(R.string.donation);
    alert.setMessage(R.string.in_app_error);
    
    alert.setNegativeButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {}
    });
    
    alert.setPositiveButton(R.string.donation_open_website, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.tvbrowser.org/index.php?id=donations")));
      }
    });
    
    alert.show();
  }
  
  private void listPurchaseItems() {
    mHelper.queryInventoryAsync(true, SettingConstants.SKU_LIST, new QueryInventoryFinishedListener() {
      @Override
      public void onQueryInventoryFinished(IabResult result, final Inventory inv) {
        if(result.isFailure()) {
          showInAppError("InApp Billing listing failed");
        }
        else {
          handler.post(new Runnable() {
            @Override
            public void run() {
              showInAppDonations(inv,false);
            }
          });
        }
      }
    });
  }
  
  private void prepareInAppPayment() {
    mUpdateItem.setActionView(R.layout.progressbar);
    
    if(mHelper == null) {
      String a2b = "2XQh0oOHnnZ2p3Ja8Xj6SlLFmI1Z/QIDAQAB";
      String a2a = "8AMIIBCgKCAQEAqfmi767AEH+MBv+";
      String ag2 = "Zh6iBFrN3zYpj1ikPu9jdtp+H47F8JvCzKt55xgIrzBpID58VfO";
      String u6c = "+K1ZDlHw1rO+qN7GW177mzEO0yk+bVs0hwE/5QF2RamM+hOcCeyB7";
      String ab2 = "6r2nGP94ai9Rgip1NLwZ1VYzFOPFC2/";
      String hm5 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ";
      String ot8 = "tWvkedDMJd+4l912GuKiUa6FNw/sZLa9UIWB2ojgr2";
      String bt4 = "TPd7Q6T9xOhHS01Ydws58YaK1NSCuIrFLG1I";
      String ddx = "x3bLB5fJKPrWJc33MMqybm6KWIc+HVt2+HT";
      String iz4 = "dePazzkaD5s84IG9FDe/cO3tvL/EZmSUiphDGXWl+beL2TW7D";
      String hrq = hm5 + a2a + ab2 + bt4 + ddx + iz4 + u6c + ag2 + ot8 + a2b;
      
      mHelper = new IabHelper(TvBrowser.this, hrq);
      mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
        
        @Override
        public void onIabSetupFinished(IabResult result) {
          if (!result.isSuccess()) {
            showInAppError("InApp Billing Setup failed");
          }
          else {
            handler.post(new Runnable() {
              @Override
              public void run() {
                listPurchaseItems();
              }
            });
          }
        }
      });
    }
    else {
      listPurchaseItems();
    }
  }
    
  private void openDonation(final SkuDetails skuDetails) {
    if(skuDetails != null && mHelper != null) {
      AlertDialog.Builder alert = new AlertDialog.Builder(TvBrowser.this);
      
      alert.setTitle(R.string.donation);
      
      View view = getLayoutInflater().inflate(R.layout.open_donation, null);
      
      alert.setView(view);
      
      ((TextView)view.findViewById(R.id.donation_open_info)).setText(getString(R.string.make_donation_info).replace("{0}", skuDetails.getPrice()));
      
      alert.setNegativeButton(R.string.stop_donation, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {}
      });
      
      alert.setPositiveButton(R.string.make_donation, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              mHelper.launchPurchaseFlow(TvBrowser.this, skuDetails.getSku(), 500012, new IabHelper.OnIabPurchaseFinishedListener() {           
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                  Log.d("info1","" +result + " " + info);
                  if(result.isSuccess()) {
                    AlertDialog.Builder alert2 = new AlertDialog.Builder(TvBrowser.this);
                    
                    alert2.setTitle(R.string.donation);
                    alert2.setMessage(R.string.thanks_for_donation);
                    
                    alert2.setPositiveButton(android.R.string.ok, new OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {}
                    });
                    
                    alert2.show();
                  }
                }
              }, Long.toHexString(Double.doubleToLongBits(Math.random())));
            }
          });
        }
      });
      
      alert.show();
    }
  }
  
  private void showDonationInfo() {
    AlertDialog.Builder alert = new AlertDialog.Builder(TvBrowser.this);
    
    alert.setTitle(R.string.donation);
    
    View view = getLayoutInflater().inflate(R.layout.donations, null);
    
    alert.setView(view);
    
    TextView inAppInfo = (TextView)view.findViewById(R.id.donation_in_app_text);
    Button inAppDonation = (Button)view.findViewById(R.id.donation_in_app_button);
    Button openWeb = (Button)view.findViewById(R.id.donation_website_button);
    
    if(!SettingConstants.GOOGLE_PLAY) {
      inAppInfo.setVisibility(View.GONE);
      inAppDonation.setVisibility(View.GONE);
    }
    
    alert.setNegativeButton(getString(R.string.not_now).replace("{0}", ""), new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    
    final AlertDialog d = alert.create();
    
    inAppDonation.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        d.dismiss();
        
        prepareInAppPayment();
      }
    });
    
    openWeb.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        d.dismiss();
        
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.tvbrowser.org/index.php?id=donations")));
      }
    });
    
    d.show();
  }
  
  private void showRatingAndDonationInfo() {
    AlertDialog.Builder alert = new AlertDialog.Builder(TvBrowser.this);
    
    alert.setTitle(R.string.you_like_it);
    
    View view = getLayoutInflater().inflate(R.layout.rating_and_donation, null);
    
    TextView ratingInfo = (TextView)view.findViewById(R.id.rating_info);
    Button rate = (Button)view.findViewById(R.id.rating_button);
    Button donate = (Button)view.findViewById(R.id.donation_button);
    
    if(!SettingConstants.GOOGLE_PLAY) {
      ratingInfo.setVisibility(View.GONE);
      rate.setVisibility(View.GONE);
    }
    
    ratingInfo.setText(Html.fromHtml(getString(R.string.rating_text)));
    ((TextView)view.findViewById(R.id.donation_info)).setText(Html.fromHtml(getString(R.string.donate_text)));
    
    final Button cancel = (Button)view.findViewById(R.id.rating_donation_cancel);
    cancel.setEnabled(false);

    
    alert.setView(view);
    alert.setCancelable(false);
    
    final AlertDialog d = alert.create();
    
    cancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        d.dismiss();
        
        setRatingAndDonationInfoShown();
        finish();
      }
    });
    
    donate.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        d.dismiss();
        setRatingAndDonationInfoShown();
        
        showDonationInfo();
      }
    });
    
    rate.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        d.dismiss();
        
        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
        
        setRatingAndDonationInfoShown();
        finish();
      }
    });
    
    
    d.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialog) {
        new Thread("Cancel wait thread") {
          @Override
          public void run() {
            int count = 10;
            
            while(--count >= 0) {
              final int countValue = count+1;
              
              handler.post(new Runnable() {
                @Override
                public void run() {
                  cancel.setText(getString(R.string.not_now).replace("{0}", " (" + countValue + ")"));
                }
              });
              
              try {
                sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            
            handler.post(new Runnable() {
              @Override
              public void run() {
                cancel.setText(getString(R.string.not_now).replace("{0}", ""));
                cancel.setEnabled(true);
              }
            });
          }
        }.start();
      }
    });
    
    d.show();
  }
  
  @Override
  public void onBackPressed() {
    if(mProgramsListWasShow) {
      mProgramsListWasShow = false;
      mViewPager.setCurrentItem(mLastSelectedTab,true);
    }
    else if(!mProgamListStateStack.isEmpty()) {
      mProgramsListWasShow = false;
      
      ProgramsListState state = mProgamListStateStack.pop();
      
      Intent showChannel = new Intent(SettingConstants.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT);
      showChannel.putExtra(SettingConstants.CHANNEL_ID_EXTRA, state.mChannelID);         
      showChannel.putExtra(SettingConstants.SCROLL_POSITION_EXTRA, state.mScrollPos);
      showChannel.putExtra(SettingConstants.DAY_POSITION_EXTRA, state.mDayPos);
      showChannel.putExtra(SettingConstants.FILTER_POSITION_EXTRA, state.mFilterPos);
      
      LocalBroadcastManager.getInstance(TvBrowser.this).sendBroadcastSync(showChannel);
    }
    else {
      if(isTaskRoot() && !PrefUtils.getBooleanValue(R.string.PREF_RATING_DONATION_INFO_SHOWN, R.bool.pref_rating_donation_info_shown_default) && PrefUtils.getLongValueWithDefaultKey(R.string.PREF_RUNNING_TIME, R.integer.pref_running_time_default) > 2 * 60 * 60000) {
        showRatingAndDonationInfo();
      }
      else {
        super.onBackPressed();
      }
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    
    SettingConstants.ORIENTATION = newConfig.orientation;
  }
  
  public void addProgramListState(int dayPos, int channelID, int filterPos, int scrollPos) {
    mProgramsListWasShow = false;
    mProgamListStateStack.push(new ProgramsListState(dayPos, channelID, filterPos, scrollPos));
  }
  
  private static final class ProgramsListState {
    public int mDayPos;
    public int mChannelID;
    public int mFilterPos;
    public int mScrollPos;
    
    public ProgramsListState(int dayPos, int channelID, int filterPos, int scrollPos) {
      mDayPos = dayPos;
      mChannelID = channelID;
      mFilterPos = filterPos;
      mScrollPos = scrollPos;
    }
  }
}
