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
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.settings.TvbPreferencesActivity;

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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
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
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
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
import billing.util.IabHelper;
import billing.util.IabHelper.QueryInventoryFinishedListener;
import billing.util.IabResult;
import billing.util.Inventory;
import billing.util.Purchase;
import billing.util.SkuDetails;

import com.example.android.listviewdragginganimation.DynamicListView;
import com.example.android.listviewdragginganimation.StableArrayAdapter;

public class TvBrowser extends FragmentActivity implements
    ActionBar.TabListener {
  private static final boolean TEST_VERSION = false;
  
  private static final int SHOW_PREFERENCES = 1;
  
  private static final int ALL_FILTER = 0;
  private static final int TV_FILTER = 1;
  private static final int RADIO_FILTER = 2;
  private static final int CINEMA_FILTER = 3;
  
  private int mCurrentFilter;
  private String mCurrentFilterSelection;
  private int mCurrentSelection;
  
  private static final String CURRENT_FILTER_EXTRA = "CURRENT_FILTER_EXTRA";
  private static final String CURRENT_FILTER_SELECTION_EXTRA = "CURRENT_FILTER_SELECTION_EXTRA";
  
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
  private boolean mFilterItemWasVisible;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
    
  private Handler handler;
  
  private Timer mTimer;
  
  private MenuItem mFilterItem;
  
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
  private static int[] SCROLL_IDS = new int[0];
  private static int[] SCROLL_TIMES;
  
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
    mRundate.set(Calendar.MONTH, Calendar.AUGUST);
    mRundate.set(Calendar.DAY_OF_MONTH, 1);
  }
  
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putBoolean(SettingConstants.UPDATE_RUNNING_KEY, updateRunning);
    outState.putBoolean(SettingConstants.SELECTION_CHANNELS_KEY, selectingChannels);
    outState.putInt(CURRENT_FILTER_EXTRA, mCurrentFilter);
    outState.putString(CURRENT_FILTER_SELECTION_EXTRA, mCurrentFilterSelection);
    
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
          Favorite fav = new Favorite(favorite);
          
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
      if(oldVersion < 181) {
        updateSelectedChannelsLists();
      }
      if(oldVersion < 204) {
        int firstTime = PrefUtils.getStringValueAsInt(R.string.PREF_REMINDER_TIME, R.string.pref_reminder_time_default);
        boolean remindAgain = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).getBoolean("PREF_REMIND_AGAIN_AT_START", false);
        Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
        edit.remove("PREF_REMIND_AGAIN_AT_START");
        edit.commit();
        
        if(remindAgain && firstTime > 0) {
          edit.putString(getString(R.string.PREF_REMINDER_TIME_SECOND), getString(R.string.pref_reminder_time_default));
          edit.commit();
          
          Intent updateAlarmValues = new Intent(UpdateAlarmValue.class.getCanonicalName());
          sendBroadcast(updateAlarmValues);
        }
      }
      if(oldVersion !=  pInfo.versionCode) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
        edit.putInt(getString(R.string.OLD_VERSION), pInfo.versionCode);
        edit.commit();
      }
    } catch (NameNotFoundException e) {}
    
    super.onCreate(savedInstanceState);
    
    SettingConstants.initializeLogoMap(TvBrowser.this,false);
    
    setContentView(R.layout.activity_tv_browser);
    
    handler = new Handler();
    
    mProgamListStateStack = new Stack<ProgramsListState>();
    
    ALL_VALUE = getResources().getString(R.string.filter_channel_all);
        
    if(savedInstanceState != null) {
      updateRunning = savedInstanceState.getBoolean(SettingConstants.UPDATE_RUNNING_KEY, false);
      selectingChannels = savedInstanceState.getBoolean(SettingConstants.SELECTION_CHANNELS_KEY, false);
      mCurrentFilter = savedInstanceState.getInt(CURRENT_FILTER_EXTRA);
      mCurrentFilterSelection = savedInstanceState.getString(CURRENT_FILTER_SELECTION_EXTRA);
    }
    else {
      mCurrentFilter = ALL_FILTER;
      mCurrentFilterSelection = "";
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
              ((ProgramTableFragment)fragment).firstLoad(getLayoutInflater());
              ((ProgramTableFragment)fragment).scrollToTime(0, mScrollTimeItem);
            }
            
            if(mFilterItem != null) {
              mFilterItem.setVisible(mFilterItemWasVisible && !(fragment instanceof FavoritesFragment));
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
  
  private void updateSelectedChannelsLists() {
    HashSet<String> tvChannelSet = new HashSet<String>();
    HashSet<String> radioChannelSet = new HashSet<String>();
    HashSet<String> cinemaChannelSet = new HashSet<String>();
    
    Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_CATEGORY}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);
    
    if(channels != null && channels.getCount() > 0) {
      channels.moveToPosition(-1);
      
      int idColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int categoryColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CATEGORY);
      
      while(channels.moveToNext()) {
        int channelID = channels.getInt(idColumn);
        int category = channels.getInt(categoryColumn);
        
        if((category & SettingConstants.TV_CATEGORY) == SettingConstants.TV_CATEGORY) {
          tvChannelSet.add(String.valueOf(channelID));
        }
        else if((category & SettingConstants.RADIO_CATEGORY) == SettingConstants.RADIO_CATEGORY) {
          radioChannelSet.add(String.valueOf(channelID));
        }
        else if((category & SettingConstants.CINEMA_CATEGORY) == SettingConstants.CINEMA_CATEGORY) {
          cinemaChannelSet.add(String.valueOf(channelID));
        }
      }
      
      Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
      edit.putStringSet(SettingConstants.SELECTED_TV_CHANNELS_LIST, tvChannelSet);
      edit.putStringSet(SettingConstants.SELECTED_RADIO_CHANNELS_LIST, radioChannelSet);
      edit.putStringSet(SettingConstants.SELECTED_CINEMA_CHANNELS_LIST, cinemaChannelSet);
      edit.commit();
    }
    
    mCurrentFilter = ALL_FILTER;
    mCurrentFilterSelection = "";
    
    mFilterItemWasVisible = !(radioChannelSet.isEmpty() && cinemaChannelSet.isEmpty()) || (tvChannelSet.isEmpty() && radioChannelSet.isEmpty()) || (tvChannelSet.isEmpty() && cinemaChannelSet.isEmpty());
    
    if(mFilterItem != null) {
      mFilterItem.setVisible(mFilterItemWasVisible);
    }
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
    
    if(mUpdateItem != null && !TvDataUpdateService.IS_RUNNING) {
      mUpdateItem.setActionView(null);
    }
    
    if(TEST_VERSION) {
      Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
      edit.putLong(getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), System.currentTimeMillis() + 60000);
      edit.commit();
      
      IOUtils.handleDataUpdatePreferences(TvBrowser.this);
      //R.string.AUTO_UPDATE_CURRENT_START_TIME
    }
    
    SettingConstants.ORIENTATION = getResources().getConfiguration().orientation;
    
    mUpdateDoneBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateProgressIcon(false);
      }
    };
    
    IntentFilter filter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mUpdateDoneBroadcastReceiver, filter);
    
    Log.d("info7", "INTENT " + getIntent() + " " + getIntent().hasExtra(SettingConstants.CHANNEL_ID_EXTRA));
    
    /*if(getIntent().hasExtra(SettingConstants.CHANNEL_ID_EXTRA) && getIntent().hasExtra(SettingConstants.START_TIME_EXTRA)) {
      
    }*/
  }
  
  private void handleResume() {
    new Thread() {
      public void run() {
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.DAY_OF_YEAR, -2);
        
        long daysSince1970 = cal2.getTimeInMillis() / 24 / 60 / 60000;
        
        try {
          getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + cal2.getTimeInMillis(), null);
        }catch(IllegalArgumentException e) {}
        
        try {
          getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + "<" + daysSince1970, null);
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
    
    if(channels != null && !channels.isClosed()) {
      channels.close();
    }
    
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
    updateProgramListChannelBar(true);
  }
  
  private void updateProgramListChannelBar(boolean updateChannelLists) {
    if(updateChannelLists) {
      updateSelectedChannelsLists();
    }
    
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
                  SettingConstants.initializeLogoMap(TvBrowser.this, true);
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
    
  private void synchronizeDontWantToSee(final boolean replace) {
    new Thread() {
      public void run() {
        if(!SettingConstants.UPDATING_FILTER) {
          SettingConstants.UPDATING_FILTER = true;
          
          Context applicationContext = getApplicationContext();
          
          NotificationCompat.Builder builder;
          
          builder = new NotificationCompat.Builder(TvBrowser.this);
          builder.setSmallIcon(R.drawable.ic_stat_notify);
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
            Log.d("info", car + " "+ bicycle);
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
                c.moveToPosition(-1);
                
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
                
                c.close();
                
                if(!updateValuesList.isEmpty()) {
                  try {
                    getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                    UiUtils.sendDontWantToSeeChangedBroadcast(applicationContext,true);
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
                  startSynchronizeUp(false, exclusionBuilder.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=dontWantToSee", null);
                }
              }
              else {
                if(!replace && oldValues != null && !oldValues.isEmpty()) {
                  for(String old : oldValues) {
                    exclusionBuilder.append(old).append("\n");
                  }
                  
                  startSynchronizeUp(false, exclusionBuilder.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=dontWantToSee", null);
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
  
  private void startSynchronizeUp(boolean info, String value, String address, String receiveDone) {
    Intent synchronizeUp = new Intent(TvBrowser.this, TvDataUpdateService.class);
    synchronizeUp.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.SYNCHRONIZE_UP_TYPE);
    synchronizeUp.putExtra(SettingConstants.SYNCHRONIZE_SHOW_INFO_EXTRA, info);
    synchronizeUp.putExtra(SettingConstants.SYNCHRONIZE_UP_URL_EXTRA, address);
    
    if(value != null) {
      synchronizeUp.putExtra(SettingConstants.SYNCHRONIZE_UP_VALUE_EXTRA, value);
    }
    
    if(receiveDone != null) {
      updateProgressIcon(true);
      IntentFilter filter = new IntentFilter(receiveDone);
      
      BroadcastReceiver refreshIcon = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          updateProgressIcon(false);
          LocalBroadcastManager.getInstance(TvBrowser.this).unregisterReceiver(this);
        }
      };
      
      LocalBroadcastManager.getInstance(TvBrowser.this).registerReceiver(refreshIcon, filter);
    }
    
    startService(synchronizeUp);
  }
  
  private void startSynchronizeRemindersDown() {
    updateProgressIcon(true);
    
    Intent synchronizeRemindersDown = new Intent(TvBrowser.this, TvDataUpdateService.class);
    synchronizeRemindersDown.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.REMINDER_DOWN_TYPE);
    synchronizeRemindersDown.putExtra(SettingConstants.SYNCHRONIZE_SHOW_INFO_EXTRA, true);
    
    IntentFilter remindersDownFilter = new IntentFilter(SettingConstants.REMINDER_DOWN_DONE);
    
    BroadcastReceiver remindersDownDone = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateProgressIcon(false);
        LocalBroadcastManager.getInstance(TvBrowser.this).unregisterReceiver(this);
      }
    };
    
    LocalBroadcastManager.getInstance(TvBrowser.this).registerReceiver(remindersDownDone, remindersDownFilter);
    
    startService(synchronizeRemindersDown);
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
    channels.moveToPosition(-1);
    
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
      
      Bitmap channelLogo = UiUtils.createBitmapFromByteArray(logo);
      
      if(channelLogo != null) {
        BitmapDrawable l = new BitmapDrawable(getResources(), channelLogo);
        
        ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
        background.setBounds(0, 0, channelLogo.getWidth()+2,channelLogo.getHeight()+2);
        
        LayerDrawable logoDrawable = new LayerDrawable(new Drawable[] {background,l});
        logoDrawable.setBounds(background.getBounds());
        
        l.setBounds(2, 2, channelLogo.getWidth(), channelLogo.getHeight());
        
        channelLogo = UiUtils.drawableToBitmap(logoDrawable);
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
    channelSelectionView.findViewById(R.id.channel_selection_selection_buttons).setVisibility(View.GONE);
    
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
            SettingConstants.initializeLogoMap(TvBrowser.this, true);
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
  
  private static class ChannelSort implements SortInterface {
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
    
    @Override
    public void setSortNumber(int value) {
      mSortNumber = value;
    }
    
    @Override
    public int getSortNumber() {
      return mSortNumber;
    }
    
    public String toString() {
      return (mSortNumber == 0 ? "-" : mSortNumber) + ". " + mName;
    }
    
    @Override
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
    
    channels.close();
  }
  
  private void sortChannels() {
    ContentResolver cr = getContentResolver();
    
    StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where.append("=1");
    
    LinearLayout main = (LinearLayout)getLayoutInflater().inflate(R.layout.channel_sort_list, null);
    
    Button sortAlphabetically = (Button)main.findViewById(R.id.channel_sort_alpabetically);
    
    final DynamicListView channelSort = (DynamicListView)main.findViewById(R.id.channel_sort);
    
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO
        };
    
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    final ArrayList<SortInterface> channelSource = new ArrayList<SortInterface>();
      
    if(channels.moveToFirst()) {
      do {
        int key = channels.getInt(0);
        String name = channels.getString(1);
        
        int order = 0;
        
        if(!channels.isNull(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER))) {
          order = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));
        }
        
        Bitmap channelLogo = UiUtils.createBitmapFromByteArray(channels.getBlob(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));
        
        if(channelLogo != null) {
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
      
      channels.close();
      
      final Comparator<SortInterface> sortComparator = new Comparator<SortInterface>() {
        @Override
        public int compare(SortInterface lhs, SortInterface rhs) {
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
      
      final StableArrayAdapter<SortInterface> aa = new StableArrayAdapter<SortInterface>(TvBrowser.this, R.layout.channel_sort_row, channelSource) {
        public View getView(int position, View convertView, ViewGroup parent) {
          ChannelSort value = (ChannelSort)getItem(position);
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
      channelSort.setArrayList(channelSource);
      channelSort.setSortDropListener(new SortDropListener() {
        @Override
        public void dropped(int originalPosition, int position) {
          int startIndex = originalPosition;
          int endIndex = position;
          
          int droppedPos = position;
          
          if(originalPosition > position) {
            startIndex = position;
            endIndex = originalPosition;
          }
          
          int previousNumber = 0;
          
          if(startIndex > 0) {
            previousNumber = aa.getItem(startIndex-1).getSortNumber();
          }
          
          int firstVisible = channelSort.getFirstVisiblePosition();
          
          for(int i = startIndex; i <= endIndex; i++) {
            if(i == droppedPos || aa.getItem(i).getSortNumber() != 0) {
              aa.getItem(i).setSortNumber(++previousNumber);
              
              if(i >= firstVisible) {
                View line = channelSort.getChildAt(i-firstVisible);
                
                if(line != null) {
                  ((TextView)line.findViewById(R.id.row_of_channel_sort_number)).setText(String.valueOf(previousNumber)+".");
                }
              }
            }
          }
        
        }
      });
      
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
          
          final ChannelSort selection = (ChannelSort)channelSource.get(position);
          
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
          Collections.sort(channelSource, new Comparator<SortInterface>() {
            @Override
            public int compare(SortInterface lhs, SortInterface rhs) {
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
          
          for(SortInterface selection : channelSource) {
            if(((ChannelSort)selection).wasChanged()) {
              somethingChanged = true;
              
              ContentValues values = new ContentValues();
              values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, selection.getSortNumber());
              
              getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, ((ChannelSort)selection).getKey()), values, null, null);
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
          builder.setSmallIcon(R.drawable.ic_stat_notify);
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
              programs.moveToPosition(-1);
              
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
    
    if(test1 instanceof ProgramsListFragment) {
      ((ProgramsListFragment)test1).updateChannels();
    }
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    
    Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(2);
    
    if(fragment instanceof FavoritesFragment) {
      ((FavoritesFragment)fragment).updateSynchroButton(null);
      ((FavoritesFragment)fragment).updateProgramsList();
    }
    
    boolean programTableActivated = PrefUtils.getBooleanValue(R.string.PROG_TABLE_ACTIVATED, R.bool.prog_table_activated_default);
    Fragment test = mSectionsPagerAdapter.getRegisteredFragment(3);
    
    if(!programTableActivated && test instanceof ProgramTableFragment) {
      ((ProgramTableFragment)test).removed();
      mSectionsPagerAdapter.destroyItem(mViewPager, 3, mSectionsPagerAdapter.getRegisteredFragment(3));
      mSectionsPagerAdapter.notifyDataSetChanged();
      actionBar.removeTabAt(3);
    }
    else if(!(test instanceof ProgramTableFragment) && programTableActivated) {
      actionBar.addTab(actionBar.newTab()
          .setText(mSectionsPagerAdapter.getPageTitle(3)).setTabListener(this));
      mSectionsPagerAdapter.notifyDataSetChanged();
      mSectionsPagerAdapter.instantiateItem(mViewPager, 3);
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
    
    UiUtils.updateImportantProgramsWidget(getApplicationContext());
    UiUtils.updateRunningProgramsWidget(getApplicationContext());
    
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
    
    TextView nextUpdate = (TextView)about.findViewById(R.id.next_data_update);
    
    switch(Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default))) {
      case 0: nextUpdate.setText(R.string.next_data_update_manually);break;
      case 1: nextUpdate.setText(R.string.next_data_update_connection);break;
      case 2: {
        Date date = new Date(PrefUtils.getLongValue(R.string.AUTO_UPDATE_CURRENT_START_TIME, 0)); 
        nextUpdate.setText(DateFormat.getMediumDateFormat(TvBrowser.this).format(date) + " " + DateFormat.getTimeFormat(TvBrowser.this).format(date));
      } break;
    }
    
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
        SettingConstants.setReminderPaused(TvBrowser.this, true);
        
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
      case R.id.action_continue_reminder: SettingConstants.setReminderPaused(TvBrowser.this, false); mPauseReminder.setVisible(true); mContinueReminder.setVisible(false); break;
      case R.id.action_synchronize_reminders_down:
        if(isOnline()) {
          startSynchronizeRemindersDown();
        }
        else {
          showNoInternetConnection(null);
        }
        break;
      case R.id.action_synchronize_reminders_up:
        if(isOnline()) {
          startSynchronizeUp(true, null, "http://android.tvbrowser.org/data/scripts/syncUp.php?type=reminderFromApp", SettingConstants.SYNCHRONIZE_UP_DONE);
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
      case R.id.action_filter_channels:filterChannels();break;
      case R.id.action_reset: {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        edit.putLong(getString(R.string.LAST_DATA_UPDATE), 0);
        edit.commit();
        
        break;
      }
    }
    
    for(int i = 0; i < SCROLL_IDS.length; i++) {
      if(item.getItemId() == SCROLL_IDS[i]) {
        scrollToTime(SCROLL_TIMES[i]+1);
        break;
      }
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  private void filterChannels() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.action_filter_channels);
    
    final ArrayList<String> availableFilter = new ArrayList<String>();
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
    
    final Set<String> tvList = pref.getStringSet(SettingConstants.SELECTED_TV_CHANNELS_LIST, new HashSet<String>(0));
    final Set<String> radioList = pref.getStringSet(SettingConstants.SELECTED_RADIO_CHANNELS_LIST, new HashSet<String>(0));
    final Set<String> cinemaList = pref.getStringSet(SettingConstants.SELECTED_CINEMA_CHANNELS_LIST, new HashSet<String>(0));
    
    mCurrentSelection = 0;
    
    availableFilter.add(getString(R.string.channel_filter_all));
    
    if(!tvList.isEmpty()) {
      availableFilter.add(getString(R.string.channel_filter_tv));
      
      if(mCurrentFilter == TV_FILTER) {
        mCurrentSelection = availableFilter.size()-1;
      }
    }
    
    if(!radioList.isEmpty()) {
      availableFilter.add(getString(R.string.channel_filter_radio));
      
      if(mCurrentFilter == RADIO_FILTER) {
        mCurrentSelection = availableFilter.size()-1;
      }
    }
    
    if(!cinemaList.isEmpty()) {
      availableFilter.add(getString(R.string.channel_filter_cinema));
      
      if(mCurrentFilter == CINEMA_FILTER) {
        mCurrentSelection = availableFilter.size()-1;
      }
    }
    
    if(availableFilter.size() == 2) {
      availableFilter.remove(availableFilter.size()-1);
      mCurrentSelection = 0;
    }
    
    builder.setSingleChoiceItems(availableFilter.toArray(new String[availableFilter.size()]), mCurrentSelection, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        mCurrentSelection = which;
      }
    });
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        String selection = availableFilter.get(mCurrentSelection);
        
        int oldFilter = mCurrentFilter;
        
        if(selection.equals(getString(R.string.channel_filter_all))) {
          mFilterItem.setIcon(getResources().getDrawable(R.drawable.ic_filter_default));
          mCurrentFilter = ALL_FILTER;
          mCurrentFilterSelection = "";
        }
        else if(selection.equals(getString(R.string.channel_filter_tv))) {
          mFilterItem.setIcon(getResources().getDrawable(R.drawable.ic_filter_tv));
          mCurrentFilter = TV_FILTER;
          mCurrentFilterSelection = " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IN (" + TextUtils.join(", ", tvList) + ") ";
        }
        else if(selection.equals(getString(R.string.channel_filter_radio))) {
          mFilterItem.setIcon(getResources().getDrawable(R.drawable.ic_filter_radio));
          mCurrentFilter = RADIO_FILTER;
          mCurrentFilterSelection = " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IN (" + TextUtils.join(", ", radioList) + ") ";
        }
        else if(selection.equals(getString(R.string.channel_filter_cinema))) {
          if(Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage())) {
            mFilterItem.setIcon(getResources().getDrawable(R.drawable.ic_filter_kino));
          }
          else {
            mFilterItem.setIcon(getResources().getDrawable(R.drawable.ic_filter_cinema));
          }
          
          mCurrentFilter = CINEMA_FILTER;
          mCurrentFilterSelection = " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IN (" + TextUtils.join(", ", cinemaList) + ") ";
        }
        
        if(oldFilter != mCurrentFilter) {
          updateProgramListChannelBar(false);
          
          Intent refresh = new Intent(SettingConstants.DATA_UPDATE_DONE);
          LocalBroadcastManager.getInstance(TvBrowser.this).sendBroadcast(refresh);
        }
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
  }
  
  private void scrollToTime(int time) {
    if(mViewPager.getCurrentItem() == 0) {
      Fragment test = mSectionsPagerAdapter.getRegisteredFragment(0);
      
      if(test instanceof RunningProgramsListFragment && time >= 0) {
        ((RunningProgramsListFragment)test).selectTime(time);
      }
    }
    else if(mViewPager.getCurrentItem() == 1) {
      Fragment test = mSectionsPagerAdapter.getRegisteredFragment(1);
      
      if(test instanceof ProgramsListFragment && time >= 0) {
        ((ProgramsListFragment)test).setScrollTime(time);
        ((ProgramsListFragment)test).scrollToTime();
      }
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
    mFilterItem = menu.findItem(R.id.action_filter_channels);
    
    menu.findItem(R.id.action_reset).setVisible(TEST_VERSION);
    
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
    
    mPauseReminder.setVisible(!SettingConstants.isReminderPaused(TvBrowser.this));
    mContinueReminder.setVisible(SettingConstants.isReminderPaused(TvBrowser.this));
    
    mScrollTimeItem.setVisible(mViewPager.getCurrentItem() != 2);
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
    
    Set<String> tvChannelSet = pref.getStringSet(SettingConstants.SELECTED_TV_CHANNELS_LIST, new HashSet<String>(0));
    Set<String> radioChannelSet = pref.getStringSet(SettingConstants.SELECTED_RADIO_CHANNELS_LIST, new HashSet<String>(0));
    Set<String> cinemaChannelSet = pref.getStringSet(SettingConstants.SELECTED_CINEMA_CHANNELS_LIST, new HashSet<String>(0));
    
    mFilterItemWasVisible = !(radioChannelSet.isEmpty() && cinemaChannelSet.isEmpty()) || (tvChannelSet.isEmpty() && radioChannelSet.isEmpty()) || (tvChannelSet.isEmpty() && cinemaChannelSet.isEmpty());
    
    mFilterItem.setVisible(mFilterItemWasVisible);
    
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
      
      int timeButtonCount = pref.getInt(getString(R.string.TIME_BUTTON_COUNT),getResources().getInteger(R.integer.time_button_count_default));
      
      for(int i = 1; i <= Math.min(timeButtonCount, getResources().getInteger(R.integer.time_button_count_default)); i++) {
        try {
          Class<?> string = R.string.class;
          
          Field setting = string.getDeclaredField("TIME_BUTTON_" + i);
          
          Integer value = Integer.valueOf(pref.getInt(getResources().getString((Integer)setting.get(string)), defaultValues[i-1]));
          
          if(value >= -1 && !values.contains(value)) {
            values.add(value);
          }
        } catch (Exception e) {}
      }
      
      for(int i = 7; i <= timeButtonCount; i++) {
          Integer value = Integer.valueOf(pref.getInt("TIME_BUTTON_" + i, 0));
          
          if(value >= -1 && !values.contains(value)) {
            values.add(value);
          }
      }
      
      if(PrefUtils.getBooleanValue(R.string.SORT_RUNNING_TIMES, R.bool.sort_running_times_default)) {
        Collections.sort(values);
      }
      
      SCROLL_TIMES = new int[values.size()];
      SCROLL_IDS = new int[values.size()];
      
      for(int i = 0; i < values.size(); i++) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, values.get(i) / 60);
        cal.set(Calendar.MINUTE, values.get(i) % 60);
        
        SCROLL_TIMES[i] = values.get(i).intValue();
        SCROLL_IDS[i] = -(i+1);
        
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
        case 2:mScrollTimeItem.setVisible(false);break;
        
        default:mScrollTimeItem.setVisible(true);break;
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
      
      if(position == 0) {
        fragment = new RunningProgramsListFragment();
      }
      else if(position == 1) {
        fragment = new ProgramsListFragment();
      }
      else if(position == 2) {
        fragment = new FavoritesFragment();
      }
      else if(position == 3) {
        fragment = new ProgramTableFragment();
      }
      
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
  
  public void showProgramsListTab(boolean remember) {
    if(mViewPager.getCurrentItem() != 1) {
      mLastSelectedTab = mViewPager.getCurrentItem();
      mViewPager.setCurrentItem(1,true);
      
      if(remember) {
        mProgramsListWasShow = true;
      }
      else {
        mProgramsListWasShow = false;
      }
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
  
  public String getChannelFilterSelection() {
    return mCurrentFilterSelection;
  }
}
