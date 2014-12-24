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
import java.io.IOException;
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
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.PluginDefinition;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.settings.PluginPreferencesActivity;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.settings.TvbPreferencesActivity;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
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
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.text.style.URLSpan;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import billing.util.IabHelper;
import billing.util.IabHelper.QueryInventoryFinishedListener;
import billing.util.IabResult;
import billing.util.Inventory;
import billing.util.Purchase;
import billing.util.SkuDetails;

import com.example.android.listviewdragginganimation.DynamicListView;
import com.example.android.listviewdragginganimation.StableArrayAdapter;

public class TvBrowser extends ActionBarActivity implements
    ActionBar.TabListener {
  private static final boolean TEST_VERSION = false;
  
  private static final int SHOW_PREFERENCES = 1;
  private static final int OPEN_FILTER_EDIT = 2;
  private static final int INSTALL_PLUGIN = 3;
  private static final int SHOW_PLUGIN_PREFERENCES = 4;
  
  private ChannelFilterValues mCurrentChannelFilter;
  private String mCurrentChannelFilterId;
    
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
  private boolean mSearchExpanded;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
    
  private Handler handler;
  
  private Timer mTimer;
  
  private MenuItem mFilterItem;
  private MenuItem mCreateFavorite;
  
  private MenuItem mUpdateItem;
  private MenuItem mSendDataUpdateLogItem;
  private MenuItem mDeleteDataUpdateLogItem;
  private MenuItem mSendReminderLogItem;
  private MenuItem mDeleteReminderLogItem;
  private MenuItem mScrollTimeItem;
  private MenuItem mPluginPreferencesMenuItem;
  
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
    
    super.onSaveInstanceState(outState);
  }
  
  private final boolean addUserColor(SharedPreferences pref, Editor edit, int defaultColorKey, int colorKey, int userColorKey) {
    int defaultColor = getResources().getColor(defaultColorKey);
    int color = pref.getInt(getString(colorKey), defaultColor);
    
    edit.putInt(getString(userColorKey), color);
    
    return defaultColor != color;
  }
  
  @Override
  protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
    PrefUtils.initialize(TvBrowser.this);
    
    if(PrefUtils.getIntValueWithDefaultKey(R.string.OLD_VERSION, R.integer.old_version_default) < 283) {
      Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
      edit.putBoolean(getString(R.string.DARK_STYLE), true);
      edit.commit();
    }
    
    if(PrefUtils.getBooleanValue(R.string.DARK_STYLE, R.bool.dark_style_default)) {
      SettingConstants.IS_DARK_THEME = true;
    }
    else {
      SettingConstants.IS_DARK_THEME = false;
    }
    
    resid = UiUtils.getThemeResourceId();
    
    super.onApplyThemeResource(theme, resid, first);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    handler = new Handler();
    PrefUtils.initialize(TvBrowser.this);
        
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
      if(oldVersion < 218) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
        Editor edit = pref.edit();
        
        boolean userDefined = addUserColor(pref,edit,R.color.pref_color_on_air_background_tvb_style_default,R.string.PREF_COLOR_ON_AIR_BACKGROUND,R.string.PREF_COLOR_ON_AIR_BACKGROUND_USER_DEFINED);
        userDefined = addUserColor(pref,edit,R.color.pref_color_on_air_progress_tvb_style_default,R.string.PREF_COLOR_ON_AIR_PROGRESS,R.string.PREF_COLOR_ON_AIR_PROGRESS_USER_DEFINED) || userDefined;
        userDefined = addUserColor(pref,edit,R.color.pref_color_mark_tvb_style_default,R.string.PREF_COLOR_MARKED,R.string.PREF_COLOR_MARKED_USER_DEFINED) || userDefined;
        userDefined = addUserColor(pref,edit,R.color.pref_color_mark_favorite_tvb_style_default,R.string.PREF_COLOR_FAVORITE,R.string.PREF_COLOR_FAVORITE) || userDefined;
        userDefined = addUserColor(pref,edit,R.color.pref_color_mark_reminder_tvb_style_default,R.string.PREF_COLOR_REMINDER,R.string.PREF_COLOR_REMINDER_USER_DEFINED) || userDefined;
        userDefined = addUserColor(pref,edit,R.color.pref_color_mark_sync_tvb_style_favorite_default,R.string.PREF_COLOR_SYNC,R.string.PREF_COLOR_SYNC_USER_DEFINED) || userDefined;
        
        if(userDefined) {
          edit.putString(getString(R.string.PREF_COLOR_STYLE), "0");
        }
        
        edit.commit();
      }
      if(oldVersion < 242) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
        Editor edit = pref.edit();
        
        if(pref.contains("PREF_WIDGET_BACKGROUND_TRANSPARENCY") && !pref.getBoolean("PREF_WIDGET_BACKGROUND_TRANSPARENCY", true)) {
          edit.remove("PREF_WIDGET_BACKGROUND_TRANSPARENCY");
          edit.putString(getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_HEADER), "0");
          edit.putString(getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_LIST), "0");
          edit.putBoolean(getString(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS), false);
        }
        
        if(pref.contains("SELECTED_TV_CHANNELS_LIST")) {
          edit.remove("SELECTED_TV_CHANNELS_LIST");
        }
        if(pref.contains("SELECTED_RADIO_CHANNELS_LIST")) {
          edit.remove("SELECTED_RADIO_CHANNELS_LIST");
        }
        if(pref.contains("SELECTED_CINEMA_CHANNELS_LIST")) {
          edit.remove("SELECTED_CINEMA_CHANNELS_LIST");
        }
        
        edit.commit();
      }
      if(oldVersion < 284 && PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.pref_program_lists_divider_size_default).equals(getString(R.string.divider_small))) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
        
        Editor edit = pref.edit();
        edit.remove(getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE));
        edit.commit();
      }
      if(oldVersion < 287 && PrefUtils.getBooleanValue(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS, true)) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
        edit.remove(getString(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS));
        edit.commit();
        
        UiUtils.updateImportantProgramsWidget(getApplicationContext());
        UiUtils.updateRunningProgramsWidget(getApplicationContext());
      }
      
      if(oldVersion > getResources().getInteger(R.integer.old_version_default) && oldVersion < pInfo.versionCode) {
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
            
            builder.setTitle(R.string.info_version);
            builder.setMessage(Html.fromHtml(getString(R.string.info_version_new)));
            builder.setPositiveButton(android.R.string.ok, null);
            
            builder.show();
          }
        }, 5000);
        
      }
      else if(oldVersion != getResources().getInteger(R.integer.old_version_default)) {      
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            showNews();
          }
        }, 5000);
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
    
    mProgamListStateStack = new Stack<ProgramsListState>();
    
    ALL_VALUE = getResources().getString(R.string.filter_channel_all);
        
    if(savedInstanceState != null) {
      updateRunning = savedInstanceState.getBoolean(SettingConstants.UPDATE_RUNNING_KEY, false);
      selectingChannels = savedInstanceState.getBoolean(SettingConstants.SELECTION_CHANNELS_KEY, false);
    }
        
    // Set up the action bar.
    actionBar = getSupportActionBar();
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
              mFilterItem.setVisible(!(fragment instanceof FragmentFavorites) && !mSearchExpanded);
            }
            if(mCreateFavorite != null) {
              mCreateFavorite.setVisible(fragment instanceof FragmentFavorites && !mSearchExpanded);
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
    
    PluginHandler.loadPlugins(getApplicationContext(),handler);
    
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
      
      ScrollView layout = (ScrollView)getLayoutInflater().inflate(R.layout.terms_layout, (ViewGroup)getCurrentFocus(), false);
      
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
      updateProgressIcon(false);
    }
    
   /* if(TEST_VERSION) {
      Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
      edit.putLong(getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), System.currentTimeMillis() + 60000);
      edit.commit();
      
      IOUtils.handleDataUpdatePreferences(TvBrowser.this);
      //R.string.AUTO_UPDATE_CURRENT_START_TIME
    }*/
    
    SettingConstants.ORIENTATION = getResources().getConfiguration().orientation;
    
    mUpdateDoneBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        PluginHandler.loadFirstProgramId(TvBrowser.this.getApplicationContext());
        
        updateProgressIcon(false);
        showNews();
      }
    };
    
    IntentFilter filter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mUpdateDoneBroadcastReceiver, filter);
    
    mCurrentChannelFilterId = PrefUtils.getStringValue(R.string.CURRENT_FILTER_ID, SettingConstants.ALL_FILTER_ID);
    
    if(!mCurrentChannelFilterId.equals(SettingConstants.ALL_FILTER_ID)) {
      SharedPreferences pref = getSharedPreferences(SettingConstants.FILTER_PREFERENCES, Context.MODE_PRIVATE);
      
      String values = pref.getString(mCurrentChannelFilterId, null);
      
      if(mCurrentChannelFilterId != null && values != null) {
        mCurrentChannelFilter = new ChannelFilterValues(mCurrentChannelFilterId,values);
      }
      else {
        mCurrentChannelFilter = new ChannelFilterValues(SettingConstants.ALL_FILTER_ID, getString(R.string.activity_edit_filter_list_text_all), "");
        mCurrentChannelFilterId = mCurrentChannelFilter.getId();
      }
    }
    
    if(mCurrentChannelFilterId.equals(SettingConstants.ALL_FILTER_ID)) {
      mCurrentChannelFilter = new ChannelFilterValues(SettingConstants.ALL_FILTER_ID, getString(R.string.activity_edit_filter_list_text_all), "");
    }
    
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        showEpgDonateInfo();
      }
    }, 7000);
  }
  
  private static boolean SHOWING_DONATION_INFO = false;
  
  private void showEpgDonateInfo() {
    if(!SHOWING_DONATION_INFO && hasEpgDonateChannelsSubscribed()) {
      final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
      
      final String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
      int month = Calendar.getInstance().get(Calendar.MONTH);
      
      final long now = System.currentTimeMillis();
      long firstDownload = pref.getLong(getString(R.string.EPG_DONATE_FIRST_DATA_DOWNLOAD), now);
      long lastDownload = pref.getLong(getString(R.string.EPG_DONATE_LAST_DATA_DOWNLOAD), now);
      long lastShown = pref.getLong(getString(R.string.EPG_DONATE_LAST_DONATION_INFO_SHOWN), (now - (60 * 24 * 60 * 60000L)));
      
      Calendar monthTest = Calendar.getInstance();
      monthTest.setTimeInMillis(lastShown);
      int shownMonth = monthTest.get(Calendar.MONTH);
      
      boolean firstTimeoutReached = (firstDownload + (14 * 24 * 60 * 60000L)) < now;
      boolean lastTimoutReached = lastDownload > (now - ((42 * 24 * 60 * 60000L)));
      boolean alreadyShowTimeoutReached = (lastShown + (14 * 24 * 60 * 60000L) < now);
      boolean alreadyShownThisMonth = shownMonth == month;
      boolean dontShowAgainThisYear = year.equals(pref.getString(getString(R.string.EPG_DONATE_DONT_SHOW_AGAIN_YEAR), "0"));
      boolean radomShow = Math.random() > 0.33;
      
      boolean show = firstTimeoutReached && lastTimoutReached && alreadyShowTimeoutReached && !alreadyShownThisMonth && !dontShowAgainThisYear && radomShow;
      
      //Log.d("info21", "firstTimeoutReached (" + ((now - firstDownload)/(24 * 60 * 60000L)) + "): " + firstTimeoutReached + " lastTimoutReached: " + lastTimoutReached + " alreadyShowTimeoutReached: " + alreadyShowTimeoutReached + " alreadyShownThisMonth: " + alreadyShownThisMonth + " dontShowAgainThisYear: " + dontShowAgainThisYear + " randomShow: " + radomShow + " SHOW: " + show);
      
      if(show) {
        AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
        
        String info = getString(R.string.epg_donate_info);
        String amount = getString(R.string.epg_donate_amount);
        String percentInfo = getString(R.string.epg_donate_percent_info);
        
        String amountValue = pref.getString(getString(R.string.EPG_DONATE_CURRENT_DONATION_AMOUNT_PREFIX)+"_"+year, getString(R.string.epg_donate_current_donation_amount_default));
        int percentValue = Integer.parseInt(pref.getString(getString(R.string.EPG_DONATE_CURRENT_DONATION_PERCENT), "-1"));
        
        amount = amount.replace("{0}", year).replace("{1}", amountValue);
        
        info = info.replace("{0}", "<h2>"+amount+"</h2>");
        
        builder.setTitle(R.string.epg_donate_name);
        builder.setCancelable(false);
        
        View view = getLayoutInflater().inflate(R.layout.dialog_epg_donate_info, (ViewGroup)getCurrentFocus(), false);
        
        TextView message = (TextView)view.findViewById(R.id.dialog_epg_donate_message);
        message.setText(Html.fromHtml(info));
        message.setMovementMethod(LinkMovementMethod.getInstance());
        
        TextView percentInfoView = (TextView)view.findViewById(R.id.dialog_epg_donate_percent_info);
        percentInfoView.setText(Html.fromHtml(percentInfo, null, new NewsTagHandler()));
        
        SeekBar percent = (SeekBar)view.findViewById(R.id.dialog_epg_donate_percent);
        percent.setEnabled(false);
        
        if(percentValue >= 0) {
          percent.setProgress(percentValue);
        }
        else {
          percentInfoView.setVisibility(View.GONE);
          percent.setVisibility(View.GONE);
        }
        
        final Spinner reason = (Spinner)view.findViewById(R.id.dialog_epg_donate_reason_selection);
        reason.setEnabled(false);
        
        final CheckBox dontShowAgain = (CheckBox)view.findViewById(R.id.dialog_epg_donate_dont_show_again);
        dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            reason.setEnabled(isChecked);
          }
        });
        
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            SHOWING_DONATION_INFO = false;
            Editor edit = pref.edit();
            edit.putLong(getString(R.string.EPG_DONATE_LAST_DONATION_INFO_SHOWN), now);
            
            if(dontShowAgain.isChecked()) {
              edit.putString(getString(R.string.EPG_DONATE_DONT_SHOW_AGAIN_YEAR), year);
            }
            
            edit.commit();
          }
        });
        
        builder.show();
        SHOWING_DONATION_INFO = true;
      }
    }
  }
  
  private boolean hasChannels() {
    boolean result = false;
    
    Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID}, null, null, null);
    
    result = c != null && c.getCount() > 0;
    
    IOUtils.closeCursor(c);
    
    return result;
  }
  
  private boolean hasEpgDonateChannelsSubscribed() {
    final String[] projection = new String[] {TvBrowserContentProvider.KEY_ID};
    boolean result = false;
    int epgDonateKey = -1;
        
    Cursor groups = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, projection, TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + "=\"" + SettingConstants.EPG_DONATE_KEY +"\"", null, null);
    
    try {
      if(groups.moveToFirst()) {
        epgDonateKey = groups.getInt(groups.getColumnIndex(TvBrowserContentProvider.KEY_ID));
      }
    } finally {
      IOUtils.closeCursor(groups);
    }
    
    if(epgDonateKey != -1) {
      Cursor epgDonateSubscribedChannels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + "=" + epgDonateKey + " ) AND " + TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, null);
      
      try {
        result = epgDonateSubscribedChannels.getCount() > 0;
      } finally {
        IOUtils.closeCursor(epgDonateSubscribedChannels);
      }
    }
    
    return result;
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
    else {
      Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null, null);
      
      try {
        if(!selectingChannels && (channels == null || channels.getCount() == 0)) {
          askChannelDownload(R.string.select_channels);
        }
        else if(PluginHandler.getFirstProgramId() == -1) {
          if(isOnline()) {
            checkTermsAccepted();
          }
        }
      }
      finally {
        IOUtils.closeCursor(channels);
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
  }
  
  private void askChannelDownload(int positiveButton) {
    selectingChannels = true;
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    builder.setMessage(R.string.no_channels);
    builder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
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
  
  private void showChannelSelection() {
    if(PrefUtils.getStringValue(R.string.PREF_AUTO_CHANNEL_UPDATE_CHANNELS_INSERTED, null) != null ||
     PrefUtils.getStringValue(R.string.PREF_AUTO_CHANNEL_UPDATE_CHANNELS_UPDATED, null) != null) {
      showChannelUpdateInfo();
    }
    else {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.synchronize_title);
      builder.setMessage(R.string.synchronize_text);
      builder.setCancelable(false);
      
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
                  
                  String dataService = null;
                  String groupKey = null;
                  String channelId = null;
                  String sortNumber = null;
                  
                  if(parts[0].equals(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_FREE_KEY))) {
                    dataService = SettingConstants.EPG_FREE_KEY;
                    groupKey = parts[1];
                    channelId = parts[2];
                    
                    if(parts.length > 3) {
                      sortNumber = parts[3];
                    }
                  }
                  else if(parts[0].equals(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_DONATE_KEY))) {
                    dataService = SettingConstants.EPG_DONATE_KEY;
                    groupKey = SettingConstants.EPG_DONATE_GROUP_KEY;
                    channelId = parts[1];
                    
                    if(parts.length > 2) {
                      sortNumber = parts[2];
                    }
                  }
                    
                  if(dataService != null) {  
                    String where = " ( " + TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = '" + dataService + "' ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + "='" + groupKey + "' ) ";
                    
                    Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
                    
                    if(group.moveToFirst()) {
                      int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                      
                      where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + "=" + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "='" + channelId + "' ) ";
                      
                      ContentValues values = new ContentValues();
                      
                      if(sortNumber != null) {
                        try {
                          sort = Integer.parseInt(sortNumber);
                        }catch(NumberFormatException e) {}
                      }
                      
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
                    e.printStackTrace();
                  } catch (OperationApplicationException e) {
                    e.printStackTrace();
                  }
                }
                
                if(!replace && exclusionBuilder.length() > 0) {
                  startSynchronizeUp(false, exclusionBuilder.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=dontWantToSee", null, null);
                }
              }
              else {
                if(!replace && oldValues != null && !oldValues.isEmpty()) {
                  for(String old : oldValues) {
                    exclusionBuilder.append(old).append("\n");
                  }
                  
                  startSynchronizeUp(false, exclusionBuilder.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=dontWantToSee", null, null);
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
  
  private void backupPreferences() {
    if(isOnline()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.action_backup_preferences_save);
      builder.setMessage(R.string.backup_preferences_save_text);
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
          Map<String,?> preferences = pref.getAll();
          
          StringBuilder backup = new StringBuilder();
          
          Set<String> keys = preferences.keySet();
          
          for(String key : keys) {
            Object value = preferences.get(key);
            
            if(value instanceof Boolean) {
              backup.append("boolean:").append(key).append("=").append(value).append("\n");
            }
            else if(value instanceof Integer) {
              backup.append("int:").append(key).append("=").append(value).append("\n");
            }
            else if(value instanceof Float) {
              backup.append("float:").append(key).append("=").append(value).append("\n");
            }
            else if(value instanceof Long) {
              backup.append("long:").append(key).append("=").append(value).append("\n");
            }
            else if(value instanceof String && value != null && ((String)value).trim().length() > 0) {
              backup.append("string:").append(key).append("=").append(value).append("\n");
            }
            else if(value instanceof Set<?>){
              if(!key.equals(getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES))) {
                Set<String> valueSet = (Set<String>)value;
                
                if(key.equals(SettingConstants.FAVORITE_LIST)) {
                  HashSet<String> favoriteList = new HashSet<String>();
                  
                  for(String setValue : valueSet) {
                    Favorite temp = new Favorite(setValue);
                    temp.loadChannelRestrictionIdsFromUniqueChannelRestriction(getApplicationContext());
                    favoriteList.add(temp.getSaveString(getApplicationContext()));
                  }
                  
                  valueSet = favoriteList;
                }
                
                backup.append("set:").append(key).append("=");
                
                backup.append(TextUtils.join("#,#", valueSet));
                
                backup.append("\n");
              }
            }
          }
          
          startSynchronizeUp(true, backup.toString(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=preferencesBackup", SettingConstants.SYNCHRONIZE_UP_DONE, getString(R.string.backup_preferences_success));
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel,null);
      
      builder.show();
    }
    else {
      showNoInternetConnection(getString(R.string.no_network_info_data_pref_backup), null);
    }
  }
  
  private void restorePreferencesInternal() {
    new Thread("RESTORE PREFERENCES") {
      @Override
      public void run() {
        updateProgressIcon(true);
        
        URL documentUrl;
        
        BufferedReader read = null;
        boolean restored = false;
        
        try {
          documentUrl = new URL("http://android.tvbrowser.org/data/scripts/syncDown.php?type=preferencesBackup");
          URLConnection connection = documentUrl.openConnection();
          
          SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
          
          String car = pref.getString(SettingConstants.USER_NAME, null);
          String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
          
          if(car != null && bicycle != null) {
            String userpass = car + ":" + bicycle;
            String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
            
            connection.setRequestProperty ("Authorization", basicAuth);
            
            read = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),"UTF-8"));
            
            String line = null;
            Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
            
            while((line = read.readLine()) != null) {
              int index = line.indexOf(":");
              
              if(index > 0) {
                restored = true;
                String type = line.substring(0,index);
                String[] parts = line.substring(index+1).split("=");
                
                if(parts != null && parts.length > 1) {
                  if(type.equals("boolean")) {
                    edit.putBoolean(parts[0], Boolean.valueOf(parts[1].trim()));
                  }
                  else if(type.equals("int")) {
                    edit.putInt(parts[0], Integer.valueOf(parts[1].trim()));
                  }
                  else if(type.equals("float")) {
                    edit.putFloat(parts[0], Float.valueOf(parts[1].trim()));
                  }
                  else if(type.equals("long")) {
                    edit.putLong(parts[0], Long.valueOf(parts[1].trim()));
                  }
                  else if(type.equals("string")) {
                    edit.putString(parts[0], parts[1].trim());
                  }
                  else if(type.equals("set")) {
                    HashSet<String> set = new HashSet<String>();
                    
                    String[] setParts = parts[1].split("#,#");
                    
                    if(setParts != null && setParts.length > 0) {
                      for(String setPart : setParts) {
                        if(parts[0].equals(SettingConstants.FAVORITE_LIST)) {
                          Favorite temp = new Favorite(setPart);
                          temp.loadChannelRestrictionIdsFromUniqueChannelRestriction(getApplicationContext());
                          set.add(temp.getSaveString());
                        }
                        else {
                          set.add(setPart);
                        }
                      }
                      
                      edit.putStringSet(parts[0], set);
                    }
                  }
                }
              }
            }
            
            if(restored) {
              edit.commit();
              handler.post(new Runnable() {
                @Override
                public void run() {
                  updateFromPreferences();
                  TvBrowser.this.finish();
                }
              });
              
              IOUtils.handleDataUpdatePreferences(getApplicationContext());
            }
          }
        }catch(Exception e) {
          restored = false;
        }
        finally {
          if(read != null) {
            try {
              read.close();
            } catch (IOException e) {}
          }
        }
        
        if(restored) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              Toast.makeText(TvBrowser.this, getString(R.string.backup_preferences_restore_success), Toast.LENGTH_LONG).show();
            }
          });
        }
        else {
          handler.post(new Runnable() {
            @Override
            public void run() {
              Toast.makeText(TvBrowser.this, getString(R.string.backup_preferences_restore_failure), Toast.LENGTH_LONG).show();
            }
          });
        }
        
        updateProgressIcon(false);
      }
    }.start();
  }
  
  private void restorePreferences() {
    if(isOnline()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.action_backup_preferences_restore);
      builder.setMessage(R.string.backup_preferences_restore_text);
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          restorePreferencesInternal();
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel,null);
      
      builder.show();
    }
    else {
      showNoInternetConnection(getString(R.string.no_network_info_data_pref_restore), null);
    }
  }
  
  private void uploadChannels() {
    String[] projection = {
        TvBrowserContentProvider.GROUP_KEY_GROUP_ID,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER
    };
    
    Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    SparseArray<String> groupKeys = new SparseArray<String>();
    
    StringBuilder uploadChannels = new StringBuilder();
    
    try {
      channels.moveToPosition(-1);
      
      int groupKeyColumn = channels.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      int channelKeyColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      int channelKeyOrderNumberColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      
      while(channels.moveToNext()) {
        int groupKey = channels.getInt(groupKeyColumn);
        String channelId = channels.getString(channelKeyColumn);
        int orderNumber = channels.getInt(channelKeyOrderNumberColumn);
        
        String groupId = groupKeys.get(groupKey);
        
        if(groupId == null) {
          String[] groupProjection = {
              TvBrowserContentProvider.GROUP_KEY_GROUP_ID,
              TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID
          };
          
          Cursor groups = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupProjection, TvBrowserContentProvider.KEY_ID + "=" + groupKey, null, null);
          
          try {
            if(groups.moveToFirst()) {
              String dataServiceId = groups.getString(groups.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID));
              String goupIdValue = groups.getString(groups.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
              
              String dataServiceIdNumber = SettingConstants.getNumberForDataServiceKey(dataServiceId);
              
              if(dataServiceIdNumber != null) {
                if(dataServiceId.equals(SettingConstants.EPG_FREE_KEY)) {
                  groupId = dataServiceIdNumber + ":"+goupIdValue+":";
                }
                else if(dataServiceId.equals(SettingConstants.EPG_DONATE_KEY)) {
                  groupId = dataServiceIdNumber + ":";
                }
                groupKeys.put(groupKey, groupId);
              }
            }
          }finally {
            groups.close();
          }
        }
        
        uploadChannels.append(groupId).append(channelId);
        
        if(orderNumber > 0) {
          uploadChannels.append(":").append(orderNumber);
        }
        
        uploadChannels.append("\n");
      }
    }finally {
      channels.close();
    }
    
    if(uploadChannels.toString().trim().length() > 0) {
      startSynchronizeUp(true, uploadChannels.toString().trim(), "http://android.tvbrowser.org/data/scripts/syncUp.php?type=channelsFromDesktop", SettingConstants.SYNCHRONIZE_UP_DONE, getString(R.string.backup_channels_success));
    }
  }
  
  private void startSynchronizeUp(boolean info, String value, String address, String receiveDone, final String userInfo) {
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
          
          if(userInfo != null) {
            handler.post(new Runnable() {
              @Override
              public void run() {
                Toast.makeText(TvBrowser.this, userInfo, Toast.LENGTH_LONG).show();
              }
            });
          }
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
    showChannelSelectionInternal(null,null,null);
  }
  
  private void showChannelSelectionInternal(final String selection, final String title, final String help) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_CATEGORY,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ALL_COUNTRIES
        };
    
    ContentResolver cr = getContentResolver();
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, selection, null, null);
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
      
      channelSelectionList.add(new ChannelSelection(channelID, name, category, countries, channelLogo, isSelected));
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
          
          convertView = mInflater.inflate(R.layout.channel_row, (ViewGroup)getCurrentFocus(), false);
          
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
    View channelSelectionView = getLayoutInflater().inflate(R.layout.dialog_channel_selection_list, (ViewGroup)getCurrentFocus(), false);
    channelSelectionView.findViewById(R.id.channel_selection_selection_buttons).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_selection_input_id_name).setVisibility(View.GONE);
    
    TextView infoView = (TextView)channelSelectionView.findViewById(R.id.channel_selection_label_id_name);
    
    if(help != null) {
      infoView.setText(help);
      infoView.setTextSize(getResources().getDimension(R.dimen.epg_donate_info_font_size));
    }
    else {
      infoView.setVisibility(View.GONE);
    }
    
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
      
      if(title == null) {
        builder.setTitle(R.string.select_channels);
      }
      else {
        builder.setTitle(title);
      }
      
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
          
          boolean success = intent.getBooleanExtra(SettingConstants.EXTRA_CHANNEL_DOWNLOAD_SUCCESSFULLY, false);
          
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
    
    try {
      if(loadAgain || channels.getCount() < 1) {
        if(isOnline()) {
          runChannelDownload();
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_channel_download), new Runnable() {
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
    }finally {
      IOUtils.closeCursor(channels);
    }
  }
  
  private void sortChannels() {
    ContentResolver cr = getContentResolver();
    
    StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where.append("=1");
    
    LinearLayout main = (LinearLayout)getLayoutInflater().inflate(R.layout.channel_sort_list, (ViewGroup)getCurrentFocus(), false);
    
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
            
            convertView = mInflater.inflate(R.layout.channel_sort_row, (ViewGroup)getCurrentFocus(), false);
            
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
          
          LinearLayout numberSelection = (LinearLayout)getLayoutInflater().inflate(R.layout.sort_number_selection, (ViewGroup)getCurrentFocus(), false);
          
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
          
          builder.setNegativeButton(android.R.string.cancel, null);
          
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
        
        RelativeLayout dataDownload = (RelativeLayout)getLayoutInflater().inflate(R.layout.dialog_data_update_selection, (ViewGroup)getCurrentFocus(), false);
        
        final Spinner days = (Spinner)dataDownload.findViewById(R.id.dialog_data_update_selection_download_days);
        final CheckBox pictures = (CheckBox)dataDownload.findViewById(R.id.dialog_data_update_selection_download_picture);
        
        final Spinner autoUpdate = (Spinner)dataDownload.findViewById(R.id.dialog_data_update_preferences_auto_update_selection_type);
        final Spinner frequency = (Spinner)dataDownload.findViewById(R.id.dialog_data_update_preferences_auto_update_selection_frequency);
        final CheckBox onlyWiFi = (CheckBox)dataDownload.findViewById(R.id.dialog_data_update_preferences_auto_update_selection_type_connection);
        final TextView timeLabel = (TextView)dataDownload.findViewById(R.id.dialog_data_update_preferences_auto_update_selection_time_label);
        final TextView time = (TextView)dataDownload.findViewById(R.id.dialog_data_update_preferences_auto_update_selection_time);
        time.setTextColor(onlyWiFi.getTextColors());
        
        String currentDownloadDays = PrefUtils.getStringValue(R.string.DAYS_TO_DOWNLOAD, R.string.days_to_download_default);
        
        final String[] possibleDownloadDays = getResources().getStringArray(R.array.download_days);
        
        for(int i = 0; i < possibleDownloadDays.length; i++) {
          if(currentDownloadDays.equals(possibleDownloadDays[i])) {
            days.setSelection(i);
            break;
          }
        }
        
        pictures.setChecked(PrefUtils.getBooleanValue(R.string.LOAD_PICTURE_DATA, R.bool.load_picture_data_default));
                
        String currentAutoUpdateValue = PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default);
        String currentAutoUpdateFrequency = PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default);
        
        if(currentAutoUpdateValue.equals("0")) {
          frequency.setEnabled(false);
          onlyWiFi.setEnabled(false);
          timeLabel.setEnabled(false);
          time.setEnabled(false);
          frequency.setVisibility(View.GONE);
          onlyWiFi.setVisibility(View.GONE);
          timeLabel.setVisibility(View.GONE);
          time.setVisibility(View.GONE);
        }
        else if(currentAutoUpdateValue.equals("1")) {
          autoUpdate.setSelection(1);
          timeLabel.setEnabled(false);
          time.setEnabled(false);
          timeLabel.setVisibility(View.GONE);
          time.setVisibility(View.GONE);
        }
        else if(currentAutoUpdateValue.equals("2")) {
          autoUpdate.setSelection(2);
        }
        
        final String[] autoFrequencyPossibleValues = getResources().getStringArray(R.array.pref_auto_update_frequency_values);
        
        for(int i = 0; i < autoFrequencyPossibleValues.length; i++) {
          if(autoFrequencyPossibleValues[i].equals(currentAutoUpdateFrequency)) {
            frequency.setSelection(i);
            break;
          }
        }
        
        onlyWiFi.setChecked(PrefUtils.getBooleanValue(R.string.PREF_AUTO_UPDATE_ONLY_WIFI, R.bool.pref_auto_update_only_wifi_default));
        
        final AtomicInteger currentAutoUpdateTime = new AtomicInteger(PrefUtils.getIntValue(R.string.PREF_AUTO_UPDATE_START_TIME, R.integer.pref_auto_update_start_time_default));
        
        Calendar now = Calendar.getInstance();
        
        now.set(Calendar.HOUR_OF_DAY, currentAutoUpdateTime.get()/60);
        now.set(Calendar.MINUTE, currentAutoUpdateTime.get()%60);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        
        time.setText(DateFormat.getTimeFormat(TvBrowser.this).format(now.getTime()));
        
        autoUpdate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            frequency.setEnabled(position != 0);
            onlyWiFi.setEnabled(position != 0);
            
            if(position != 0) {
              frequency.setVisibility(View.VISIBLE);
              onlyWiFi.setVisibility(View.VISIBLE);
            }
            else {
              frequency.setVisibility(View.GONE);
              onlyWiFi.setVisibility(View.GONE);
            }
            
            timeLabel.setEnabled(position == 2);
            time.setEnabled(position == 2);
            
            if(position == 2) {
              timeLabel.setVisibility(View.VISIBLE);
              time.setVisibility(View.VISIBLE);
            }
            else {
              timeLabel.setVisibility(View.GONE);
              time.setVisibility(View.GONE);
            }
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            
          }
        });
        
        View.OnClickListener onClickListener = new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            AlertDialog.Builder b2 = new AlertDialog.Builder(TvBrowser.this);
            
            LinearLayout timeSelection = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_data_update_selection_auto_update_time, (ViewGroup)getCurrentFocus(), false);
            
            final TimePicker timePick = (TimePicker)timeSelection.findViewById(R.id.dialog_data_update_selection_auto_update_selection_time);
            timePick.setIs24HourView(DateFormat.is24HourFormat(TvBrowser.this));
            timePick.setCurrentHour(currentAutoUpdateTime.get()/60);
            timePick.setCurrentMinute(currentAutoUpdateTime.get()%60);
            
            b2.setView(timeSelection);
            
            b2.setPositiveButton(android.R.string.ok, new OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                currentAutoUpdateTime.set(timePick.getCurrentHour() * 60 + timePick.getCurrentMinute());
                
                Calendar now = Calendar.getInstance();
                
                now.set(Calendar.HOUR_OF_DAY, currentAutoUpdateTime.get()/60);
                now.set(Calendar.MINUTE, currentAutoUpdateTime.get()%60);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);
                
                time.setText(DateFormat.getTimeFormat(TvBrowser.this).format(now.getTime()));
              }
            });
            b2.setNegativeButton(android.R.string.cancel, null);
            
            b2.show();
          }
        };
        
        time.setOnClickListener(onClickListener);
        timeLabel.setOnClickListener(onClickListener);
        
        builder.setTitle(R.string.download_data);
        builder.setView(dataDownload);
        
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            String value = possibleDownloadDays[days.getSelectedItemPosition()];
            
            Editor settings = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
            
            if(PrefUtils.getStringValueAsInt(R.string.PREF_AUTO_UPDATE_RANGE, R.string.pref_auto_update_range_default) < Integer.parseInt(value)) {
              settings.putString(getString(R.string.PREF_AUTO_UPDATE_RANGE), value);
            }
            
            settings.putString(getString(R.string.DAYS_TO_DOWNLOAD), value);
            settings.putBoolean(getString(R.string.LOAD_PICTURE_DATA), pictures.isChecked());
            settings.putString(getString(R.string.PREF_AUTO_UPDATE_TYPE), String.valueOf(autoUpdate.getSelectedItemPosition()));
            
            if(autoUpdate.getSelectedItemPosition() == 1 || autoUpdate.getSelectedItemPosition() == 2) {
              settings.putString(getString(R.string.PREF_AUTO_UPDATE_FREQUENCY), autoFrequencyPossibleValues[frequency.getSelectedItemPosition()]);
              settings.putBoolean(getString(R.string.PREF_AUTO_UPDATE_ONLY_WIFI), onlyWiFi.isChecked());
              
              if(autoUpdate.getSelectedItemPosition() == 2) {
                settings.putInt(getString(R.string.PREF_AUTO_UPDATE_START_TIME), currentAutoUpdateTime.get());
              }
            }
            
            settings.commit();
            
            IOUtils.handleDataUpdatePreferences(TvBrowser.this);
            
            Intent startDownload = new Intent(TvBrowser.this, TvDataUpdateService.class);
            startDownload.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.TV_DATA_TYPE);
            startDownload.putExtra(getResources().getString(R.string.DAYS_TO_DOWNLOAD), Integer.parseInt(value));
            
            startService(startDownload);
            
            updateProgressIcon(true);
          }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
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
    
    if(fragment instanceof FragmentFavorites) {
      ((FragmentFavorites)fragment).updateSynchroButton(null);
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
    builder.setCancelable(false);
    
    RelativeLayout username_password_setup = (RelativeLayout)getLayoutInflater().inflate(R.layout.username_password_setup, (ViewGroup)getCurrentFocus(), false);
            
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
  
  private void showNoInternetConnection(String type, final Runnable callback) {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.no_network);
    builder.setMessage(getString(R.string.no_network_info).replace("{0}", type));
    
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
      
      View view = getLayoutInflater().inflate(R.layout.dont_want_to_see_exclusion_edit_list, (ViewGroup)getCurrentFocus(), false);
      
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
          
          View editView = getLayoutInflater().inflate(R.layout.dont_want_to_see_edit, (ViewGroup)getCurrentFocus(), false);
          
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
                  e.printStackTrace();
                } catch (OperationApplicationException e) {
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
    else if(requestCode == OPEN_FILTER_EDIT) {
      updateFromFilterEdit();
      
      sendChannelFilterUpdate();
    }
    else if(requestCode == INSTALL_PLUGIN && mCurrentDownloadPlugin != null && mCurrentDownloadPlugin.isFile()) {
      if(!mCurrentDownloadPlugin.delete()) {
        mCurrentDownloadPlugin.deleteOnExit();
      }
    }
    else if(requestCode == SHOW_PLUGIN_PREFERENCES) {
      PluginPreferencesActivity.clearPlugins();
    }
  }
  
  private void updateFromPreferences() {
    Fragment test1 = mSectionsPagerAdapter.getRegisteredFragment(1);
    
    if(test1 instanceof FragmentProgramsList) {
      ((FragmentProgramsList)test1).updateChannels();
    }
    
    Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(2);
    
    if(fragment instanceof FragmentFavorites) {
      ((FragmentFavorites)fragment).updateSynchroButton(null);
      ((FragmentFavorites)fragment).updateProgramsList();
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
    
    if(mUpdateItem != null && !TvDataUpdateService.IS_RUNNING) {
      if(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("0")) {
        mUpdateItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
      else {
        mUpdateItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        invalidateOptionsMenu();
      }
    }
    
    new UpdateAlarmValue().onReceive(TvBrowser.this, null);
  }
  
  private void showAbout() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    RelativeLayout about = (RelativeLayout)getLayoutInflater().inflate(R.layout.about, (ViewGroup)getCurrentFocus(), false);
    
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      TextView version = (TextView)about.findViewById(R.id.version);
      version.setText(pInfo.versionName);
    } catch (NameNotFoundException e) {
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
    final File path = IOUtils.getDownloadDirectory(getApplicationContext());
    
    File logFile = new File(path,file);
    
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
    final File path = IOUtils.getDownloadDirectory(getApplicationContext());
    
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
  
  private void openFilterEdit() {
    Intent startFilterEdit = new Intent(this, ActivityFilterListEdit.class);
    startActivityForResult(startFilterEdit, OPEN_FILTER_EDIT);
  }
  
  private void sendChannelFilterUpdate() {
    Intent refresh = new Intent(SettingConstants.DATA_UPDATE_DONE);
    LocalBroadcastManager.getInstance(TvBrowser.this).sendBroadcast(refresh);
    UiUtils.updateRunningProgramsWidget(TvBrowser.this);
    
    updateProgramListChannelBar();
  }
  
  private void updateChannelFilter(ChannelFilterValues filter, int iconRes) {
    mCurrentChannelFilterId = filter.getId();
    mCurrentChannelFilter = filter;
    setCurrentFilterPreference(mCurrentChannelFilterId);
    mFilterItem.setIcon(iconRes);

    sendChannelFilterUpdate();
  }
  
  private static final class SpecialSpan extends ReplacementSpan {
    final static int RIGHT_TYPE = 0;
    final static int LINE_TYPE = 1;
    
    private int mWidth;
    private int mSpanType;
    
    public SpecialSpan(int type) {
      mSpanType = type;
    }
    
    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
      if(mSpanType == LINE_TYPE) {
        canvas.drawLine(canvas.getClipBounds().left, top + (bottom-top)/2, canvas.getClipBounds().right, top + (bottom-top)/2, paint);
      }
      else if(mSpanType == RIGHT_TYPE) {
        canvas.drawText(text.toString().substring(start, end), canvas.getClipBounds().right-mWidth, y, paint);
      }
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
      mWidth = (int)paint.measureText(text,start,end);
      return mWidth;
    }
  }
  
  private static final class NewsTagHandler implements TagHandler {
    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
      if(tag.equals("right")) {
        doSpecial(opening,output,SpecialSpan.RIGHT_TYPE);
      }
      else if(tag.equals("line")) {
        doSpecial(opening,output,SpecialSpan.LINE_TYPE);
      }
    }
    
    private void doSpecial(boolean opening, Editable output, int type) {
      int len = output.length();
      
      ReplacementSpan span = new SpecialSpan(type);
      
      if(opening) {
          output.setSpan(span, len, len, Spannable.SPAN_MARK_MARK);
      } else {
          Object obj = getLast(output, SpecialSpan.class);
          int where = output.getSpanStart(obj);

          output.removeSpan(obj);

          if (where != len) {
              output.setSpan(span, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
      }
    }
    
    private Object getLast(Editable text, Class<?> kind) {
      Object[] objs = text.getSpans(0, text.length(), kind);

      if (objs.length == 0) {
          return null;
      } else {
          for(int i = objs.length;i>0;i--) {
              if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
                  return objs[i-1];
              }
          }
          return null;
      }
    }
  }
  
  private void showNews() {
    if(!selectingChannels) {
      final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this);
      
      if(pref.getBoolean(getString(R.string.PREF_NEWS_SHOW), getResources().getBoolean(R.bool.pref_news_show_default))) {
        long lastShown = pref.getLong(getString(R.string.NEWS_DATE_LAST_SHOWN), 0);
        long lastKnown = pref.getLong(getString(R.string.NEWS_DATE_LAST_KNOWN), 0);
        
        final String news = pref.getString(getString(R.string.NEWS_TEXT), "");
        
        if(lastShown < lastKnown && news.trim().length() > 0) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              final AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
              
              builder.setTitle(R.string.title_news);
              builder.setCancelable(false);
              builder.setMessage(Html.fromHtml(news,null,new NewsTagHandler()));
              
              builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  Editor edit = pref.edit();
                  edit.putLong(getString(R.string.NEWS_DATE_LAST_SHOWN), System.currentTimeMillis());
                  edit.commit();
                  
                  showPluginInfo();
                }
              });
              
              AlertDialog d = builder.create();
              d.show();
              
              ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }
          });
        }
        else {
          showPluginInfo();
        }
      }
      else {
        showPluginInfo();
      }
    }
  }
  
  private void showPluginInfo() {
    if(!PrefUtils.getBooleanValue(R.string.PLUGIN_INFO_SHOWN, false)) {
      final AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.plugin_info_title);
      builder.setCancelable(false);
      builder.setMessage(R.string.plugin_info_message);
      
      builder.setPositiveButton(R.string.plugin_info_load, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          savePluginInfoShown();
          
          if(isOnline()) {
            searchPlugins(true);
          }
          else {
            showNoInternetConnection(getString(R.string.no_network_info_data_search_plugins),new Runnable() {
              @Override
              public void run() {
                searchPlugins(true);
              }
            });
          }
        }
      });
      
      builder.setNegativeButton(getString(R.string.not_now).replace("{0}", ""), new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          savePluginInfoShown();
          showChannelUpdateInfo();
        }
      });
      
      builder.show();
    }
    else {
      showChannelUpdateInfo();
    }
  }
  
  private void showChannelUpdateInfo() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        
    StringBuilder selection = new StringBuilder();
    
    String insertedChannels = PrefUtils.getStringValue(R.string.PREF_AUTO_CHANNEL_UPDATE_CHANNELS_INSERTED, null);
    String updateChannels = PrefUtils.getStringValue(R.string.PREF_AUTO_CHANNEL_UPDATE_CHANNELS_UPDATED, null);
    
    Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
    
    if(insertedChannels != null) {
      selection.append(insertedChannels);
      edit.remove(getString(R.string.PREF_AUTO_CHANNEL_UPDATE_CHANNELS_INSERTED));
    }
    if(updateChannels != null) {
      if(selection.length() > 0) {
        selection.append(",");
      }
      
      selection.append(updateChannels);
      edit.remove(getString(R.string.PREF_AUTO_CHANNEL_UPDATE_CHANNELS_UPDATED));
    }
    
    if(selection.toString().trim().length() > 0) {
      edit.commit();
      selection.insert(0, TvBrowserContentProvider.KEY_ID + " IN ( ");
      selection.append(" ) ");
      
      showChannelSelectionInternal(selection.toString(), getString(R.string.dialog_select_channels_update_title), getString(R.string.dialog_select_channels_update_help));
    }
      }
    });

  }
  
  private void savePluginInfoShown() {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
    edit.putBoolean(getString(R.string.PLUGIN_INFO_SHOWN), true);
    edit.commit();
  }
  
  private int FILTER_MAX_ID = 0; 
  
  private synchronized void updateFromFilterEdit() {
    final SubMenu filters = mFilterItem.getSubMenu();

    for(int i = 0; i < FILTER_MAX_ID; i++) {
      filters.removeItem(i);
    }
    
    ArrayList<ChannelFilterValues> channelFilterList = new ArrayList<ChannelFilterValues>();
    SharedPreferences filterPreferences = getSharedPreferences(SettingConstants.FILTER_PREFERENCES, Context.MODE_PRIVATE);
    Map<String,?> filterValues = filterPreferences.getAll();
    
    for(String key : filterValues.keySet()) {
      Object values = filterValues.get(key);
      
      if(key.startsWith("filter.") && values instanceof String && values != null) {        
        channelFilterList.add(new ChannelFilterValues(key, (String)values));
      }
    }
    
    Collections.sort(channelFilterList, ChannelFilterValues.CHANNEL_FILTER_VALUES_COMPARATOR);
    
    int groupId = 3;
    int id = 1;
    
    final ChannelFilterValues allFilter = new ChannelFilterValues(SettingConstants.ALL_FILTER_ID, getString(R.string.activity_edit_filter_list_text_all), "");
    
    MenuItem all = filters.add(groupId, id++, groupId, allFilter.toString());
    all.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        updateChannelFilter(allFilter,R.drawable.ic_filter_default);
        item.setChecked(true);
        return true;
      }
    });
           
    if(mCurrentChannelFilterId == null || allFilter.getId().endsWith(mCurrentChannelFilterId)) {
      all.setChecked(true);
    }
    
    for(final ChannelFilterValues filter : channelFilterList) {
      MenuItem item = filters.add(groupId, id++, groupId, filter.toString());
      
      if(mCurrentChannelFilterId != null && filter.getId().endsWith(mCurrentChannelFilterId)) {
        mFilterItem.setIcon(R.drawable.ic_filter_on);
        item.setChecked(true);
      }
      
      item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
          updateChannelFilter(filter,R.drawable.ic_filter_on);
          item.setChecked(true);
          
          return true;
        }
      });
    }
    
    FILTER_MAX_ID = id;
    
    filters.setGroupCheckable(groupId, true, true);
  }
  
  private void setCurrentFilterPreference(String id) {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(TvBrowser.this).edit();
    edit.putString(getString(R.string.CURRENT_FILTER_ID), id);
    edit.commit();
  }
  
  private void searchPlugins(final boolean showChannelUpdateInfo) {
    if(isOnline()) {
      new Thread("SEARCH FOR PLUGINS THREAD") {
        @Override
        public void run() {
          updateProgressIcon(true);
          PluginDefinition[] availablePlugins = PluginDefinition.loadAvailablePluginDefinitions();
          
          final ArrayList<PluginDefinition> newPlugins = new ArrayList<PluginDefinition>();
          final PluginServiceConnection[] connections = PluginHandler.getAvailablePlugins();
          
          for(PluginDefinition def : availablePlugins) {
            if(Build.VERSION.SDK_INT >= def.getMinApiVersion()) {
              String packageName = def.getPackageName();
              String[] services = def.getServices();
              
              for(String service : services) {
                if(service.startsWith(".")) {
                  service = packageName + service;
                }
                
                String[] parts = service.split(":");
                
                boolean wasAdded = false;
                boolean wasFound = false;
                                
                if(connections != null && connections.length > 0) {
                  for(PluginServiceConnection connection : connections) {
                    if(connection.getId().equals(parts[0])) {
                      wasFound = true;
                      
                      String currentVersion = connection.getPluginVersion();
                      
                      if(currentVersion != null && !currentVersion.equals(parts[1])) {
                        newPlugins.add(def);
                        def.setIsUpdate();
                        wasAdded = true;
                        break;
                      }
                    }
                  }
                }
                
                if(wasAdded) {
                  break;
                }
                else if(!wasFound) {
                  newPlugins.add(def);
                }
              }
            }
          }
          
          StringBuilder pluginsText = new StringBuilder();
          
          Collections.sort(newPlugins);
          
          for(PluginDefinition news : newPlugins) {
            if(pluginsText.length() > 0) {
              pluginsText.append("<line>LINE</line>");
            }
            
            pluginsText.append("<h3>");
            pluginsText.append(news.getName());
            
            if(news.isUpdate()) {
              pluginsText.append(" <i>(Update)</i>");
            }
            
            pluginsText.append("</h3>");
            
            pluginsText.append(news.getDescription());
            
            pluginsText.append("<p><i>");
            pluginsText.append(getString(R.string.author)).append(" ");
            pluginsText.append(news.getAuthor());
            pluginsText.append(" <right>").append(getString(R.string.version)).append(" ");
            pluginsText.append(news.getVersion());
            pluginsText.append("</i></right></p>");
            
            if(news.isOnGooglePlay()) {
              pluginsText.append("<p><a href=\"http://play.google.com/store/apps/details?id=");
              pluginsText.append(news.getPackageName());
              pluginsText.append("\">").append(getString(R.string.plugin_open_google_play)).append("</a></p>");
            }
            
            if(news.getDownloadLink() != null && news.getDownloadLink().trim().length() > 0) {
              pluginsText.append("<p><a href=\"");
              pluginsText.append(news.getDownloadLink().replace("http://", "plugin://").replace("https://", "plugins://"));
              pluginsText.append("\">").append(getString(R.string.plugin_download_manually)).append("</a></p>");
            }
          }
          
          String title = getString(R.string.plugin_available_title);
          
          if(newPlugins.isEmpty()) {
            title = getString(R.string.plugin_available_not_title);
            pluginsText.append(getString(R.string.plugin_available_not_message));
          }
          
          final AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
          builder.setTitle(title);
          builder.setCancelable(false);
          
          builder.setMessage(getClickableText(Html.fromHtml(pluginsText.toString(),null,new NewsTagHandler())));
          
          builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              if(!newPlugins.isEmpty()) {
                PluginHandler.shutdownPlugins(TvBrowser.this);
                
                handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                    PluginHandler.loadPlugins(TvBrowser.this, handler);
                    
                    if(mPluginPreferencesMenuItem != null) {
                      mPluginPreferencesMenuItem.setEnabled(PluginHandler.hasPlugins());
                    }
                  }
                }, 2000);
              }
              
              if(showChannelUpdateInfo) {
                showChannelUpdateInfo();
              }
            }
          });
          
          handler.post(new Runnable() {
            @Override
            public void run() {
              AlertDialog d = builder.create();
              d.show();
              
              ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }
          });
          
          updateProgressIcon(false);
        }
      }.start();
    }
  }
  
  private boolean mLoadingPlugin = false;
  private File mCurrentDownloadPlugin;
  
  private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
  {    
      int start = strBuilder.getSpanStart(span);
      int end = strBuilder.getSpanEnd(span);
      int flags = strBuilder.getSpanFlags(span);
      ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
              if(!mLoadingPlugin) {
                mLoadingPlugin = true;
                String url = span.getURL();
                
                if(url.startsWith("http://play.google.com/store/apps/details?id=")) {
                  try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("http://play.google.com/store/apps", "market:/"))));
                  } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                  }
                  
                  mLoadingPlugin = false;
                }
                else if(url.startsWith("plugin://") || url.startsWith("plugins://")) {
                  final File path = IOUtils.getDownloadDirectory(getApplicationContext());
                  
                  if(!path.isDirectory()) {
                    path.mkdirs();
                  }
                  
                  if(url.startsWith("plugin://")) {
                    url = url.replace("plugin://", "http://");
                  }
                  else if(url.startsWith("plugins://")) {
                    url = url.replace("plugins://", "https://");
                  }
                  
                  String name = url.substring(url.lastIndexOf("/")+1);
                  
                  mCurrentDownloadPlugin = new File(path, name);
                  
                  if(mCurrentDownloadPlugin.isFile()) {
                    mCurrentDownloadPlugin.delete();
                  }
                  
                  final String downloadUrl = url;
                  
                  handler.post(new Runnable() {
                    @Override
                    public void run() {
                      AsyncTask<String, Void, Boolean> async = new AsyncTask<String, Void, Boolean>() {
                        private ProgressDialog mProgress;
                        private File mPluginFile;
                        
                        protected void onPreExecute() {
                          mProgress = new ProgressDialog(TvBrowser.this);
                          mProgress.setMessage(getString(R.string.plugin_info_donwload).replace("{0}", mCurrentDownloadPlugin.getName()));
                          mProgress.show();
                        };
                        
                        @Override
                        protected Boolean doInBackground(String... params) {
                          mPluginFile = new File(params[0]);
                          return IOUtils.saveUrl(params[0], params[1], 15000);
                        }
                        
                        protected void onPostExecute(Boolean result) {
                          mProgress.hide();
                          
                          if(result) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(mPluginFile),"application/vnd.android.package-archive");
                            TvBrowser.this.startActivityForResult(intent, INSTALL_PLUGIN);
                          }
                          
                          mLoadingPlugin = false;
                        };
                      };
                      
                      async.execute(mCurrentDownloadPlugin.toString(), downloadUrl);
                    }
                  });
                }
                else {
                  mLoadingPlugin = false;
                }
              }
            }
      };
      strBuilder.setSpan(clickable, start, end, flags);
      strBuilder.removeSpan(span);
  }

  private SpannableStringBuilder getClickableText(CharSequence sequence)
  {
          SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
          URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);   
          for(URLSpan span : urls) {
              makeLinkClickable(strBuilder, span);
          }
      return strBuilder;    
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_username_password:
      {  
        showUserSetting(false);
      }
      break;
      case R.id.menu_tvbrowser_action_create_favorite: UiUtils.editFavorite(null, TvBrowser.this, null);break;
      case R.id.action_donation: showDonationInfo(); break;
      case R.id.action_search_plugins: 
        if(isOnline()) {
          searchPlugins(false);
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_search_plugins),new Runnable() {
            @Override
            public void run() {
              searchPlugins(false);
            }
          });
        }
        break;
      case R.id.action_pause_reminder: pauseReminder(); break;
      case R.id.action_continue_reminder: SettingConstants.setReminderPaused(TvBrowser.this, false); mPauseReminder.setVisible(true); mContinueReminder.setVisible(false); break;
      case R.id.action_synchronize_reminders_down:
        if(isOnline()) {
          startSynchronizeRemindersDown();
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_sync_reminder), null);
        }
        break;
      case R.id.action_synchronize_reminders_up:
        if(isOnline()) {
          startSynchronizeUp(true, null, "http://android.tvbrowser.org/data/scripts/syncUp.php?type=reminderFromApp", SettingConstants.SYNCHRONIZE_UP_DONE, null);
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_sync_reminder),null);
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
          showNoInternetConnection(getString(R.string.no_network_info_data_sync_dont_want_to_see), null);
        }
        break;
      case R.id.action_synchronize_channels:
        if(isOnline()) {
          if(!hasChannels()) {
            askChannelDownload(R.string.channel_notification_title);
          }
          else {
            syncronizeChannels();
          }
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_sync_channels),null);
        }
        break;
      case R.id.action_synchronize_channels_up:
        if(isOnline()) {
          uploadChannels();
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_sync_channels),null);
        }
        break;
      case R.id.action_delete_data_update_log: deleteLog("data-update-log.txt");break;
      case R.id.action_delete_reminder_log: deleteLog("reminder-log.txt");break;
      case R.id.action_send_data_update_log:sendLogMail("data-update-log.txt",getString(R.string.log_send_data_update));break;
      case R.id.action_send_reminder_log:sendLogMail("reminder-log.txt",getString(R.string.log_send_reminder));break;
      case R.id.menu_tvbrowser_action_settings_basic:
        Intent startPref = new Intent(this, TvbPreferencesActivity.class);
        startActivityForResult(startPref, SHOW_PREFERENCES);
        break;
      case R.id.menu_tvbrowser_action_settings_plugins:
        Intent startPluginPref = new Intent(this, PluginPreferencesActivity.class);
        startActivityForResult(startPluginPref, SHOW_PLUGIN_PREFERENCES);
        break;
      case R.id.menu_tvbrowser_action_update_data:
        if(isOnline()) {
          checkTermsAccepted();
        }
        else {
          showNoInternetConnection(getString(R.string.no_network_info_data_update), null);
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
      case R.id.action_activity_filter_list_edit_open:openFilterEdit();break;
     // case R.id.action_filter_channels:filterChannels();break;
      case R.id.action_reset: {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        edit.putLong(getString(R.string.LAST_DATA_UPDATE), 0);
        edit.commit();
        
        break;
      }
      case R.id.action_backup_preferences_save: backupPreferences();break;
      case R.id.action_backup_preferences_restore: restorePreferences();break;
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
    if(mViewPager.getCurrentItem() == 0) {
      Fragment test = mSectionsPagerAdapter.getRegisteredFragment(0);
      
      if(test instanceof FragmentProgramsListRunning && time >= 0) {
        ((FragmentProgramsListRunning)test).selectTime(time);
      }
    }
    else if(mViewPager.getCurrentItem() == 1) {
      Fragment test = mSectionsPagerAdapter.getRegisteredFragment(1);
      
      if(test instanceof FragmentProgramsList && time >= 0) {
        ((FragmentProgramsList)test).setScrollTime(time);
        ((FragmentProgramsList)test).scrollToTime();
      }
    }
    else if(mViewPager.getCurrentItem() == 3) {
      Fragment test = mSectionsPagerAdapter.getRegisteredFragment(3);
      
      if(test instanceof ProgramTableFragment) {
        ((ProgramTableFragment)test).scrollToTime(time, mScrollTimeItem);
      }
    }
  }
  
  private Menu mMainMenu;
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.tv_browser, menu);
    
    mMainMenu = menu;
    
    //  Associate searchable configuration with the SearchView
    SearchManager searchManager =
           (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView =
            (SearchView) menu.findItem(R.id.search).getActionView();
    searchView.setSearchableInfo(
            searchManager.getSearchableInfo(getComponentName()));
    
    mUpdateItem = menu.findItem(R.id.menu_tvbrowser_action_update_data);
    
    if(!PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("0")) {
      mUpdateItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }
    
    mFilterItem = menu.findItem(R.id.action_filter_channels);
    mCreateFavorite = menu.findItem(R.id.menu_tvbrowser_action_create_favorite);
    
    Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
    
    mFilterItem.setVisible(!(fragment instanceof FragmentFavorites));
    mCreateFavorite.setVisible(fragment instanceof FragmentFavorites);
    mScrollTimeItem = menu.findItem(R.id.action_scroll);
    
    updateFromFilterEdit();
    
    mPluginPreferencesMenuItem = menu.findItem(R.id.menu_tvbrowser_action_settings_plugins);
    
    mPluginPreferencesMenuItem.setEnabled(PluginHandler.pluginsAvailable());
    
    menu.findItem(R.id.action_reset).setVisible(TEST_VERSION);
    
    mSearchExpanded = false;
    
   // if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      addOnActionExpandListener(menu.findItem(R.id.search));
   // }
    
   // menu.findItem(R.id.action_synchronize_dont_want_to_see).setVisible(false);
    menu.findItem(R.id.action_synchronize_favorites).setVisible(false);
    
    if(mUpdateItem != null && TvDataUpdateService.IS_RUNNING) {
      updateProgressIcon(true);
    }
    
    mDebugMenuItem = menu.findItem(R.id.action_debug);
    mSendDataUpdateLogItem = menu.findItem(R.id.action_send_data_update_log);
    mDeleteDataUpdateLogItem = menu.findItem(R.id.action_delete_data_update_log);
    mSendReminderLogItem = menu.findItem(R.id.action_send_reminder_log);
    mDeleteReminderLogItem = menu.findItem(R.id.action_delete_reminder_log);
        
    mPauseReminder = menu.findItem(R.id.action_pause_reminder);
    mContinueReminder = menu.findItem(R.id.action_continue_reminder);
    
    mPauseReminder.setVisible(!SettingConstants.isReminderPaused(TvBrowser.this));
    mContinueReminder.setVisible(SettingConstants.isReminderPaused(TvBrowser.this));
    
    mScrollTimeItem.setVisible(mViewPager.getCurrentItem() != 2 && !mSearchExpanded);
        
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
    
    applyStupidWorkaroundForWrongMenuPopupColor();
    
    return true;
  }

  @SuppressLint("NewApi")
  private void addOnActionExpandListener(MenuItem search) {
    if(search != null) {
      MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
          mSearchExpanded = true;
          
          if(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("0")) {
            mUpdateItem.setVisible(false);
          }
          
          mScrollTimeItem.setVisible(false);
          mCreateFavorite.setVisible(false);
          
          mFilterItem.setVisible(false);
          
          return true;
        }
        
        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
          mSearchExpanded = false;
          
          Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
          
          if(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("0")) {
            mUpdateItem.setVisible(true);
          }
          if(!(fragment instanceof FragmentFavorites)) {
            mScrollTimeItem.setVisible(true);
          }
          if((fragment instanceof FragmentFavorites)) {
            mCreateFavorite.setVisible(true);
          }
          
          if(!(fragment instanceof FragmentFavorites)) {
            mFilterItem.setVisible(true);
          }
          
          return true;
        }
      });
    }
  }
  
  final int[] workaroundMenuIds = new int[] {
      R.id.action_synchronize_channels,
      R.id.action_synchronize_channels_up,
      R.id.action_synchronize_dont_want_to_see,
      R.id.action_synchronize_favorites,
      R.id.action_synchronize_reminders_up,
      R.id.action_synchronize_reminders_down,
      R.id.action_backup_preferences_save,
      R.id.action_backup_preferences_restore,
      R.id.action_username_password,
      R.id.menu_tvbrowser_action_settings_basic,
      R.id.menu_tvbrowser_action_settings_plugins,
      R.id.action_preferences_channels,
      R.id.action_load_channels_again,
      R.id.action_select_channels,
      R.id.action_sort_channels,
      R.id.action_delete_all_data,
      R.id.action_dont_want_to_see_edit,
      R.id.action_delete_data_update_log,
      R.id.action_send_data_update_log,
      R.id.action_delete_reminder_log,
      R.id.action_send_reminder_log
  };
  
  private void makeItemForegroundVisible(MenuItem item) {
    SpannableStringBuilder test = new SpannableStringBuilder(item.getTitle());
    test.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.abc_primary_text_material_light)), 0, item.getTitle().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    
    item.setTitle(test);
  }
  
  private void applyStupidWorkaroundForWrongMenuPopupColor() {
    if(!SettingConstants.IS_DARK_THEME && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      for(int i = 0; i < workaroundMenuIds.length; i++) {
        MenuItem item = mMainMenu.findItem(workaroundMenuIds[i]);
        
        if(item != null) {
          makeItemForegroundVisible(item);
        }
      }
    }
  }
  
  private void updateSynchroMenu() {
    SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
    
    String car = pref.getString(SettingConstants.USER_NAME, null);
    String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
    
    boolean isAccount = (car != null && car.trim().length() > 0 && bicycle != null && bicycle.trim().length() > 0);
    
    if(mOptionsMenu != null) {
      mOptionsMenu.findItem(R.id.action_synchronize_channels).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_channels_up).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_dont_want_to_see).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_favorites).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_reminders_up).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_synchronize_reminders_down).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_backup_preferences_save).setEnabled(isAccount);
      mOptionsMenu.findItem(R.id.action_backup_preferences_restore).setEnabled(isAccount);
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
        
        default:mScrollTimeItem.setVisible(true && !mSearchExpanded);break;
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
        fragment = new FragmentProgramsListRunning();
      }
      else if(position == 1) {
        fragment = new FragmentProgramsList();
      }
      else if(position == 2) {
        fragment = new FragmentFavorites();
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
          mUpdateItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        else {
          if(!PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("0")) {
            mUpdateItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            invalidateOptionsMenu();
          }
          
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
    updateProgressIcon(false);
    
    AlertDialog.Builder alert = new AlertDialog.Builder(TvBrowser.this);
    
    alert.setTitle(R.string.donation);
    
    View view = getLayoutInflater().inflate(R.layout.in_app_donations, (ViewGroup)getCurrentFocus(), false);
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
      
      if(donatedTest != null && details != null) {
        donated = donatedTest;
        donatedDetails = details;
      }
      if(details != null) {
        String title = details.getTitle().substring(0,details.getTitle().indexOf("(")-1);
        
        Button donation = new Button(this);
        donation.setTextSize(UiUtils.convertDpToPixel(16, getResources()));
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
          updateProgressIcon(true);
          
          mHelper.consumeAsync(toConsume,new IabHelper.OnConsumeFinishedListener() {
            @Override
            public void onConsumeFinished(Purchase purchase, IabResult result) {
              updateProgressIcon(false);
              
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
     
     PluginHandler.shutdownPlugins(getApplicationContext());
     
     if (mHelper != null) {
       mHelper.dispose();
     }
     
     mHelper = null;
  }
  
  private void showInAppError(String error) {
    updateProgressIcon(false);
    
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
    try {
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
    }catch(IllegalStateException e) {
      showInAppError("InApp Billing listing failed");
    }
  }
  
  private void prepareInAppPayment() {
    updateProgressIcon(true);
    
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
      
      View view = getLayoutInflater().inflate(R.layout.open_donation, (ViewGroup)getCurrentFocus(), false);
      
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
    
    View view = getLayoutInflater().inflate(R.layout.donations, (ViewGroup)getCurrentFocus(), false);
    
    alert.setView(view);
    
    Button inAppDonation = (Button)view.findViewById(R.id.donation_in_app_button);
    
    TextView webInfo = (TextView)view.findViewById(R.id.donation_show_ways);
    Button openWeb = (Button)view.findViewById(R.id.donation_website_button);
    
    Calendar timeout = Calendar.getInstance();
    timeout.set(2015, Calendar.JANUARY,1,0,0,0);
    timeout.set(Calendar.MILLISECOND, 0);
    
    if(Locale.getDefault().getCountry().equals("DE") && timeout.compareTo(Calendar.getInstance()) < 0) {
      webInfo.setVisibility(View.GONE);
      openWeb.setVisibility(View.GONE);
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
    
    View view = getLayoutInflater().inflate(R.layout.rating_and_donation, (ViewGroup)getCurrentFocus(), false);
    
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
      PrefUtils.initialize(TvBrowser.this);
      
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
    if(mCurrentChannelFilter != null) {
      return mCurrentChannelFilter.getWhereClause();
    }
    
    return "";
  }
}
