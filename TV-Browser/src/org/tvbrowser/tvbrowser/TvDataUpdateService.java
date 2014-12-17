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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

/**
 * The update service for the data of TV-Browser.
 * 
 * @author René Mach
 */
public class TvDataUpdateService extends Service {
  public static final String TAG = "TV_DATA_UPDATE_SERVICE";
  
  public static final String TYPE = "TYPE";
  public static final int TV_DATA_TYPE = 1;
  public static final int CHANNEL_TYPE = 2;
  public static final int REMINDER_DOWN_TYPE = 3;
  public static final int SYNCHRONIZE_UP_TYPE = 4;
  
  private static final int BASE_LEVEL = 0;
  private static final int MORE_LEVEL = 1;
  private static final int PICTURE_LEVEL = 2;
    
  public static boolean IS_RUNNING = false;
  private ExecutorService mThreadPool;
  private ExecutorService mDataUpdatePool;
  private Handler mHandler;
  
  private static final int NOTIFY_ID = 511;
  private NotificationCompat.Builder mBuilder;
  private int mCurrentDownloadCount;
  private int mUnsuccessfulDownloads;
  private int mDaysToLoad;
  
  private Hashtable<String, Hashtable<String, CurrentDataHolder>> mCurrentData;
  private Hashtable<String, int[]> mCurrentVersionIDs;

  private static final int TABLE_OPERATION_MIN_SIZE = Math.max(100, (int)(Runtime.getRuntime().maxMemory()/1000000));
  
  private ArrayList<ContentValues> mDataInsertList;
  private ArrayList<ContentProviderOperation> mDataUpdateList;

  private ArrayList<ContentValues> mVersionInsertList;
  private ArrayList<ContentProviderOperation> mVersionUpdateList;
  
  private ArrayList<String> mSyncFavorites;
    
  private DontWantToSeeExclusion[] mDontWantToSeeValues;
    
  private static final String GROUP_FILE = "groups.txt";
  
  private static final String DEFAULT_GROUPS_URL = "http://www.tvbrowser.org/listings/";
  
  private static final String[] DEFAULT_GROUPS_URL_MIRRORS = {
      "http://tvbrowser.dyndns.tv/",
      "http://www.gamers-fusion.de/projects/tvbrowser.org/",
      "http://tvbrowser1.sam-schwedler.de/",
      "http://tvbrowser.nicht-langweilig.de/data/"
  };
  
  private static final String[] BASE_LEVEL_FIELDS = {
    TvBrowserContentProvider.DATA_KEY_STARTTIME,
    TvBrowserContentProvider.DATA_KEY_ENDTIME,
    TvBrowserContentProvider.DATA_KEY_TITLE,
    TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
    TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_REGIE,
    TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO,
    TvBrowserContentProvider.DATA_KEY_CATEGORIES,
    TvBrowserContentProvider.DATA_KEY_AGE_LIMIT,
    TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK,
    TvBrowserContentProvider.DATA_KEY_GENRE,
    TvBrowserContentProvider.DATA_KEY_ORIGIN,
    TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME,
    TvBrowserContentProvider.DATA_KEY_VPS,
    TvBrowserContentProvider.DATA_KEY_SCRIPT,
    TvBrowserContentProvider.DATA_KEY_REPETITION_FROM,
    TvBrowserContentProvider.DATA_KEY_MUSIC,
    TvBrowserContentProvider.DATA_KEY_MODERATION,
    TvBrowserContentProvider.DATA_KEY_YEAR,
    TvBrowserContentProvider.DATA_KEY_REPETITION_ON,
    TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER,
    TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT,
    TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER,
    TvBrowserContentProvider.DATA_KEY_PRODUCER,
    TvBrowserContentProvider.DATA_KEY_CAMERA,
    TvBrowserContentProvider.DATA_KEY_CUT,
    TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS,
    TvBrowserContentProvider.DATA_KEY_RATING,
    TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM,
    TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING,
    TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR,
    TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO,
    TvBrowserContentProvider.DATA_KEY_SERIES,
    TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT,
    TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT,
    TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES,
    TvBrowserContentProvider.DATA_KEY_INFO_BLACK_AND_WHITE,
    TvBrowserContentProvider.DATA_KEY_INFO_4_TO_3,
    TvBrowserContentProvider.DATA_KEY_INFO_16_TO_9,
    TvBrowserContentProvider.DATA_KEY_INFO_MONO,
    TvBrowserContentProvider.DATA_KEY_INFO_STEREO,
    TvBrowserContentProvider.DATA_KEY_INFO_DOLBY_SOURROUND,
    TvBrowserContentProvider.DATA_KEY_INFO_DOLBY_DIGITAL_5_1,
    TvBrowserContentProvider.DATA_KEY_INFO_SECOND_AUDIO_PROGRAM,
    TvBrowserContentProvider.DATA_KEY_INFO_CLOSED_CAPTION,
    TvBrowserContentProvider.DATA_KEY_INFO_LIVE,
    TvBrowserContentProvider.DATA_KEY_INFO_OMU,
    TvBrowserContentProvider.DATA_KEY_INFO_FILM,
    TvBrowserContentProvider.DATA_KEY_INFO_SERIES,
    TvBrowserContentProvider.DATA_KEY_INFO_NEW,
    TvBrowserContentProvider.DATA_KEY_INFO_AUDIO_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_INFO_NEWS,
    TvBrowserContentProvider.DATA_KEY_INFO_SHOW,
    TvBrowserContentProvider.DATA_KEY_INFO_MAGAZIN,
    TvBrowserContentProvider.DATA_KEY_INFO_HD,
    TvBrowserContentProvider.DATA_KEY_INFO_DOCUMENTATION,
    TvBrowserContentProvider.DATA_KEY_INFO_ART,
    TvBrowserContentProvider.DATA_KEY_INFO_SPORT,
    TvBrowserContentProvider.DATA_KEY_INFO_CHILDREN,
    TvBrowserContentProvider.DATA_KEY_INFO_OTHER,
    TvBrowserContentProvider.DATA_KEY_INFO_SIGN_LANGUAGE
  };
  
  private static final String[] MORE_LEVEL_FIELDS = {
    TvBrowserContentProvider.DATA_KEY_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_ACTORS
  };
  
  private static final String[] PICTURE_LEVEL_FIELDS = {
    TvBrowserContentProvider.DATA_KEY_PICTURE,
    TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
    TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION
  };
  
  private boolean mShowNotification;
  
  @Override
  public void onCreate() {
    super.onCreate();
    
    mDaysToLoad = 2;
    
    mBuilder = new NotificationCompat.Builder(this);
    mBuilder.setSmallIcon(R.drawable.ic_stat_notify);
    mBuilder.setOngoing(true);
    
    mHandler = new Handler();
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
  
  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    new Thread("DATA UPDATE ON START COMMOAND THREAD") {
      public void run() {
        setPriority(NORM_PRIORITY);
        Logging.openLogForDataUpdate(TvDataUpdateService.this);
        
        doLog("Received intent: " + intent);
        
        if(intent != null) {
          doLog("Extra Type: " + intent.getIntExtra(TYPE, TV_DATA_TYPE));
        }
        
        boolean isConnected = false;
        boolean onlyWifi = false;
        
        if(intent != null && intent.hasExtra(SettingConstants.INTERNET_CONNECTION_RESTRICTED_DATA_UPDATE_EXTRA)) {
          onlyWifi = intent.getExtras().getBoolean(SettingConstants.INTERNET_CONNECTION_RESTRICTED_DATA_UPDATE_EXTRA);
        }
        
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo lan = CompatUtils.getLanNetworkIfPossible(connMgr);
        NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        
        if((wifi != null && wifi.isConnected()) || (lan != null && lan.isConnected())) {
          isConnected = true;
        }
        
        if(!isConnected && !onlyWifi && mobile != null && mobile.isConnected()) {
          isConnected = true;
        }
        
        if(isConnected && intent != null) {
          if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == TV_DATA_TYPE) {
            mDaysToLoad = intent.getIntExtra(getResources().getString(R.string.DAYS_TO_DOWNLOAD), Integer.parseInt(getResources().getString(R.string.days_to_download_default)));
            updateTvData();
          }
          else if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == CHANNEL_TYPE) {
            updateChannels();
          }
          else if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == REMINDER_DOWN_TYPE) {
            startSynchronizeRemindersDown(intent.getBooleanExtra(SettingConstants.SYNCHRONIZE_SHOW_INFO_EXTRA, true));
          }
          else if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == SYNCHRONIZE_UP_TYPE) {
            if(intent.hasExtra(SettingConstants.SYNCHRONIZE_UP_URL_EXTRA)) {
              String address = intent.getStringExtra(SettingConstants.SYNCHRONIZE_UP_URL_EXTRA);
              String value = intent.getStringExtra(SettingConstants.SYNCHRONIZE_UP_VALUE_EXTRA);
              boolean showInfo = intent.getBooleanExtra(SettingConstants.SYNCHRONIZE_SHOW_INFO_EXTRA, true);
              
              startSynchronizeUp(showInfo, value, address);
            }
          }
          else {
            stopSelfInternal();
          }
        }
        else {
          if(!IS_RUNNING) {
            IS_RUNNING = true;
            mBuilder.setContentTitle(getResources().getText(R.string.update_notification_title));
            startForeground(NOTIFY_ID, mBuilder.build());
            doLog("NO UPDATE DONE, NO INTERNET CONNECTION OR NO INTENT, PROCESS EXISTING DATA");
            handleStoredDataFromKilledUpdate();
          }
          else {
            stopSelfInternal();
          }
        }
      }
    }.start();
    
    return Service.START_STICKY;
  }
  
  private void handleStoredDataFromKilledUpdate() {
    PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TVBUPDATE_LOCK");
    mWakeLock.setReferenceCounted(false);
    mWakeLock.acquire(120*60000L);
    
    final NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    
    mShowNotification = true;
    mUnsuccessfulDownloads = 0;
    IS_RUNNING = true;
    
    doLog("Favorite.handleDataUpdateStarted()");
    Favorite.handleDataUpdateStarted();
        
    final File path = IOUtils.getDownloadDirectory(TvDataUpdateService.this.getApplicationContext());
    
    if(path.isDirectory()) {
      File[] oldDataFiles = path.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().toLowerCase().endsWith(".prog.gz");
        }
      });
      
      if(oldDataFiles != null && oldDataFiles.length > 0) {
        mBuilder.setContentText(getString(R.string.update_data_notification_reload_file));
        mBuilder.setProgress(oldDataFiles.length, 0, false);
        notification.notify(NOTIFY_ID, mBuilder.build());
        
        final HashMap<String, ChannelUpdate> updateMap = new HashMap<String, TvDataUpdateService.ChannelUpdate>();
        
        final String[] projection = new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE,TvBrowserContentProvider.GROUP_KEY_GROUP_ID};
        
        final SparseArray<String> groupMap = new SparseArray<String>();
        final String[] groupProjection = new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID};
        
        Cursor groupCursor = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupProjection, null, null, null);
        
        try {
          groupCursor.moveToPosition(-1);
          
          final int keyColumn = groupCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
          final int dataServiceColumn = groupCursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID);
          
          while(groupCursor.moveToNext()) {
            int key = groupCursor.getInt(keyColumn);
            String dataServiceId = groupCursor.getString(dataServiceColumn);
            
            groupMap.put(key, dataServiceId);
          }
        }finally {
          if(groupCursor != null && !groupCursor.isClosed()) {
            groupCursor.close();
          }
        }
        
        DataHandler epgFreeDataHandler = new EPGfreeDataHandler();
        DataHandler epgDoanteDataHandler = new EPGdonateDataHandler();
        
        for(int i = 0; i < oldDataFiles.length; i++) {
          mBuilder.setProgress(oldDataFiles.length, i+1, false);
          notification.notify(NOTIFY_ID, mBuilder.build());

          File file = oldDataFiles[i];
          
          String[] fileParts = file.getName().split("_");
          
          String key = "";
          
          for(String filePart : fileParts) {
            if(filePart.equals("base") || filePart.contains("16-00") || filePart.contains("00-16")) {
              break;
            }
            
            key += filePart + "_";
          }
          
          if(key.trim().length() > 0) {
            key = key.substring(0,key.length()-1);
            
            ChannelUpdate update = updateMap.get(key);
            
            if(update == null) {
              final int firstUnderline = key.indexOf("_");
              final int secondUnderline = key.indexOf("_",firstUnderline+1);
              
              final String country = key.substring(firstUnderline + 1, secondUnderline);
              final String channelId = key.substring(secondUnderline+1);
              
              final String where = TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY + "=\"" + country +"\" AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=\"" + channelId +"\" AND " + TvBrowserContentProvider.CHANNEL_KEY_SELECTION;
              
              final Cursor channel = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where, null, null);
              
              try {
                if(channel != null && channel.moveToFirst()) {
                  try {
                    final int firstMinus = key.indexOf("-");
                    final int secondMinus = key.indexOf("-",firstMinus+1);
                    
                    String year = key.substring(0,firstMinus);
                    String month = key.substring(firstMinus+1,secondMinus);
                    String day = key.substring(secondMinus+1,firstUnderline);
                        
                    final int channelIntId = channel.getInt(channel.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                    final String timezone = channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE));
                    final int groupKey = channel.getInt(channel.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
                    
                    Calendar date = Calendar.getInstance(TimeZone.getTimeZone(timezone));
                    date.set(Integer.parseInt(year), Integer.parseInt(month)-1, Integer.parseInt(day));
                    
                    String dataServiceId = groupMap.get(groupKey);
                    
                    DataHandler toUse = epgFreeDataHandler;
                    
                    if(dataServiceId.equals(SettingConstants.EPG_DONATE_KEY)) {
                      toUse = epgDoanteDataHandler;
                    }
                    
                    update = new ChannelUpdate(toUse, channelIntId, timezone, date.getTimeInMillis());
                    
                    updateMap.put(key, update);
                  } catch (Exception e) {
                    deleteFile(file);
                  }
                }
                else {
                  deleteFile(file);
                }
              }finally {
                if(channel != null) {
                  channel.close();
                }
              }
            }
            
            if(update != null) {
              update.addDownloadedFile(file);
            }
            else {
              deleteFile(file);
            }
          }
          else {
            deleteFile(file);
          }
        }
        
        if(!updateMap.isEmpty()) {
          final UncaughtExceptionHandler handleExc = new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
              doLog("UNCAUGHT EXCEPTION Thread: " + thread.getName() + " Throwable " + ex.toString());
            }
          };
          
          readCurrentVersionIDs();
          
          Set<String> exclusions = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);
          
          if(exclusions != null) {
            mDontWantToSeeValues = new DontWantToSeeExclusion[exclusions.size()];
            
            int i = 0;
            
            for(String exclusion : exclusions) {
              mDontWantToSeeValues[i++] = new DontWantToSeeExclusion(exclusion);
            }
          }
          
          readCurrentData();
          
          mDataUpdatePool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
          
          mDataInsertList = new ArrayList<ContentValues>();
          mDataUpdateList = new ArrayList<ContentProviderOperation>();
          
          mVersionInsertList = new ArrayList<ContentValues>();
          mVersionUpdateList = new ArrayList<ContentProviderOperation>();
          
          mCurrentDownloadCount = 0;
          
          mBuilder.setContentText(getString(R.string.update_notification_text));
          mBuilder.setProgress(updateMap.size(), 0, false);
          notification.notify(NOTIFY_ID, mBuilder.build());
          
          for(String key : updateMap.keySet()) {
            updateMap.get(key).startUpdate(notification, updateMap.size(), handleExc);
          }
          
          mDataUpdatePool.shutdown();
          
          doLog("WAIT FOR DATA UPDATE FOR: " + updateMap.size() + " MINUTES");
          
          try {
            mDataUpdatePool.awaitTermination(updateMap.size(), TimeUnit.MINUTES);
          } catch (InterruptedException e) {
            doLog("UPDATE DATE INTERRUPTED " + e.getLocalizedMessage());
          }
          
          if(!mDataUpdatePool.isTerminated()) {
            doLog("NOT HANDLED UPDATES " + mDataUpdatePool.shutdownNow().size());
          }
          
          doLog("WAIT FOR DATA UPDATE FOR DONE, DATA: " + mDataUpdatePool.isTerminated());
          
          mShowNotification = false;
          
          insert(mDataInsertList);
          insertVersion(mVersionInsertList);
          
          update(mDataUpdateList);      
          update(mVersionUpdateList);
          
          mDataInsertList = null;
          mDataUpdateList = null;
          
          mVersionInsertList = null;      
          mVersionUpdateList = null;

          mBuilder.setProgress(100, 0, true);
          notification.notify(NOTIFY_ID, mBuilder.build());
          
          mCurrentVersionIDs.clear();
          mCurrentVersionIDs = null;
          
          if(mCurrentData != null) {
            mCurrentData.clear();
            mCurrentData = null;
          }
          
          updateMap.clear();
        }
      }
    }
    
    calculateMissingEnds(notification, true);
  }
  
  private void deleteFile(File file) {
    if(file != null && !file.delete()) {
      file.deleteOnExit();
    }
  }
  
  private void startSynchronizeRemindersDown(boolean info) {
    NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    
    synchronizeRemindersDown(info,notification);
    
    Intent synchronizeRemindersUpDone = new Intent(SettingConstants.REMINDER_DOWN_DONE);
    LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(synchronizeRemindersUpDone);
    
    stopSelfInternal();
  }
  
  @Override
  public void onDestroy() {
    doLog("onDestroy() called");
    
    if(mThreadPool != null && !mThreadPool.isTerminated()) {
      int notDownloadedSize = mThreadPool.shutdownNow().size();
      doLog("onDestroy(), notDownloadedSize: " + notDownloadedSize);
    }
    if(mDataUpdatePool != null && !mDataUpdatePool.isTerminated()) {
      int notUpdatedSize = mDataUpdatePool.shutdownNow().size();
      doLog("onDestroy(), notUpdatedSize: " + notUpdatedSize);
    }
    
    stopForeground(true);
    ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFY_ID);
    
    Favorite.handleDataUpdateFinished();
    
    if(mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
    }
    
    Logging.closeLogForDataUpdate();
    
    IS_RUNNING = false;
    
    super.onDestroy();
  }
  
  private void stopSelfInternal() {
    if(mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
    }
    
    if(mCurrentChannelData != null) {
      mCurrentChannelData.clear();
      mCurrentChannelData = null;
    }
    
    Logging.closeLogForDataUpdate();
    ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFY_ID);
    stopSelf();
  }
  
  private void startSynchronizeUp(boolean info, final String value, final String address) {
    NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    
    synchronizeUp(info, value, address, notification);
    
    Intent synchronizeUpDone = new Intent(SettingConstants.SYNCHRONIZE_UP_DONE);
    LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(synchronizeUpDone);
    
    stopSelfInternal();
  }
  
  private void synchronizeUp(boolean info, final String value, final String address, final NotificationManager notification) {
    mBuilder.setProgress(100, 0, true);
    mBuilder.setContentText(getResources().getText(R.string.update_data_notification_synchronize));
    notification.notify(NOTIFY_ID, mBuilder.build());
    
    final String CrLf = "\r\n";
    
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
          
          String postData = "";
          
          byte[] xmlData = value == null ? getBytesForReminders() : IOUtils.getCompressedData(value.getBytes("UTF-8"));
          
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
          try {
              os.close();
          } catch (Exception e) {
          }
          try {
              is.close();
          } catch (Exception e) {
          }
          try {
  
          } catch (Exception e) {
          }
      }
    }
    
    notification.cancel(NOTIFY_ID);
  }
  
  private byte[] getBytesForReminders() {
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
      programs.moveToPosition(-1);
      
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
        groups.moveToPosition(-1);
        
        while(groups.moveToNext()) {
          int groupKey = groups.getInt(0);
          String dataServiceID = groups.getString(1);
          String groupID = groups.getString(2);
          
          String test = SettingConstants.getNumberForDataServiceKey(dataServiceID);
          
          if(test != null) {
            dataServiceID = test;
          }
          
          groupInfo.put(groupKey, new SimpleGroupInfo(dataServiceID, groupID));
        }
      }
      
      groups.close();
      
      if(groupInfo.size() > 0) {
        while(programs.moveToNext()) {
          int groupID = programs.getInt(groupKeyColumnIndex);
          long startTime = programs.getLong(startTimeColumnIndex) / 60000;
          String channelID = programs.getString(1);
          String baseCountry = programs.getString(channelKeyBaseCountryColumnIndex);
          
          SimpleGroupInfo info = groupInfo.get(groupID);
          
          String groupId = ":" + info.mGroupID;
          
          if(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_DONATE_KEY).equals(info.mDataServiceID)) {
            groupId = "";
          }
          
          dat.append(startTime).append(";").append(info.mDataServiceID).append(groupId).append(":").append(baseCountry).append(":").append(channelID).append("\n");
        }
      }
    }
    
    programs.close();
    
    return IOUtils.getCompressedData(dat.toString().getBytes());
  }
  
  private void synchronizeRemindersDown(boolean info, final NotificationManager notification) {
    if(!SettingConstants.UPDATING_REMINDERS) {      
      SettingConstants.UPDATING_REMINDERS = true;
      
      mBuilder.setProgress(100, 0, true);
      mBuilder.setContentText(getResources().getText(R.string.update_data_notification_synchronize_remiders));
      notification.notify(NOTIFY_ID, mBuilder.build());
      
      URL documentUrl;
      
      try {
        documentUrl = new URL("http://android.tvbrowser.org/data/scripts/syncDown.php?type=reminderFromDesktop");
        URLConnection connection = documentUrl.openConnection();
        
        SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
        
        String car = pref.getString(SettingConstants.USER_NAME, null);
        String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
        
        if(car != null && bicycle != null && car.trim().length() > 0 && bicycle.trim().length() > 0) {
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
            
              String dataService = null;
              String groupKeyId = null;
              String channelIdKey = null;
              
              if(idParts[0].equals(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_FREE_KEY))) {
                dataService = SettingConstants.EPG_FREE_KEY;
                groupKeyId = idParts[1];
                channelIdKey = idParts[2];
              }
              else if(idParts[0].equals(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_DONATE_KEY))) {
                dataService = SettingConstants.EPG_DONATE_KEY;
                groupKeyId = SettingConstants.EPG_DONATE_GROUP_KEY;
                channelIdKey = idParts[1];
              }
                
              if(dataService != null) {
                String where = " ( " +TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = \"" + dataService + "\" ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = \"" + groupKeyId + "\" ) ";
                
                Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
                
                if(group.moveToFirst()) {
                  int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                  
                  where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " IS " + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=\'" + channelIdKey + "\' ) ";
                  
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
                                                    
                        UiUtils.addReminder(TvDataUpdateService.this, programID, time, TvBrowser.class, true);
                      }
                    }
                    
                    program.close();
                  }
                  
                  channel.close();
                }
                
                group.close();
              }
            }
          }
          
          if(!updateValuesList.isEmpty()) {
            try {
              getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
              
              LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(TvDataUpdateService.this);
              
              for(Intent markUpdate : markingIntentList) {
                localBroadcast.sendBroadcast(markUpdate);
              }
              
              if(info) {
                mHandler.post(new Runnable() {
                  @Override
                  public void run() {
                    Toast.makeText(TvDataUpdateService.this, R.string.synchronize_reminder_down_done, Toast.LENGTH_SHORT).show();
                  }
                });
              }
            } catch (RemoteException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            } catch (OperationApplicationException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            
            UiUtils.updateImportantProgramsWidget(TvDataUpdateService.this);
          }
          else {
            if(info) {
              mHandler.post(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(TvDataUpdateService.this, R.string.synchronize_reminder_down_nothing, Toast.LENGTH_SHORT).show();
                }
              });
            }
          }
        }
      }catch(Exception e) {
        Log.d("info", "", e);
        
      }
      
      SettingConstants.UPDATING_REMINDERS = false;
      
      notification.cancel(NOTIFY_ID);
    }
  }
  
  private void syncFavorites(final NotificationManager notification) {
    if(mSyncFavorites != null) {
      mBuilder.setProgress(100, 0, true);
      mBuilder.setContentText(getResources().getText(R.string.update_data_notification_synchronize_favorites));
      notification.notify(NOTIFY_ID, mBuilder.build());
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      
      for(String fav : mSyncFavorites) {
        if(fav != null && fav.contains(";") && fav.contains(":")) {
          String[] parts = fav.split(";");
          
          long time = Long.parseLong(parts[0]) * 60000;
          String[] idParts = parts[1].split(":");
          
          String dataService = null;
          String groupKeyId = null;
          String channelIdKey = null;
          
          if(idParts[0].equals(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_FREE_KEY))) {
            dataService = SettingConstants.EPG_FREE_KEY;
            groupKeyId = idParts[1];
            channelIdKey = idParts[2];
          }
          else if(idParts[0].equals(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_DONATE_KEY))) {
            dataService = SettingConstants.EPG_DONATE_KEY;
            groupKeyId = SettingConstants.EPG_DONATE_GROUP_KEY;
            channelIdKey = idParts[1];
          }
            
          if(dataService != null) {
            String where = " ( " +TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = \"" + dataService + "\" ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = \"" + groupKeyId + "\" ) ";
            
            Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
            
            if(group.moveToFirst()) {
              int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
              
              where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " IS " + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=\'" + channelIdKey + "\' ) ";
              
              Cursor channel = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where, null, null);
              
              if(channel.moveToFirst()) {
                int channelId = channel.getInt(channel.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                
                where = " ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channelId + " ) AND ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " = " + time + " ) ";
                
                Cursor program = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, where, null, null);
                
                if(program.moveToFirst()) {
                  boolean markedAsFavorite = program.getInt(program.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE)) == 1;
                  
                  if(!markedAsFavorite) {
                    ContentValues values = new ContentValues();
                    values.put(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC, true);
                    
                    long programID = program.getLong(program.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                    
                    ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID));
                    opBuilder.withValues(values);
                    
                    updateValuesList.add(opBuilder.build());
                    
                    Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
                    intent.putExtra(SettingConstants.MARKINGS_ID, programID);
                    
                    markingIntentList.add(intent);
                  }
                }
                
                program.close();
              }
              
              channel.close();
            }
            
            group.close();
          }
        }
      }
      
      if(!updateValuesList.isEmpty()) {
        try {
          getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
          
          LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(TvDataUpdateService.this);
          
          for(Intent markUpdate : markingIntentList) {
            localBroadcast.sendBroadcast(markUpdate);
          }
        } catch (RemoteException e) {
          e.printStackTrace();
        } catch (OperationApplicationException e) {
          e.printStackTrace();
        }
        
        UiUtils.updateImportantProgramsWidget(TvDataUpdateService.this);
      }
      
      notification.cancel(NOTIFY_ID);
    }
    
    mSyncFavorites = null;
  }
  
  private Hashtable<String, Integer> mCurrentChannelData;
  
  private void updateChannels() {
    if(!IS_RUNNING) {
      IS_RUNNING = true;
      
      final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
      mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TVBUPDATE_LOCK");
      mWakeLock.setReferenceCounted(false);
      mWakeLock.acquire(10*60000L);
      
      mBuilder.setProgress(100, 0, true);
      mBuilder.setContentTitle(getResources().getText(R.string.channel_notification_title));
      mBuilder.setContentText(getResources().getText(R.string.channel_notification_text));
      startForeground(NOTIFY_ID, mBuilder.build());
      
      mCurrentChannelData = new Hashtable<String, Integer>();
      
      final String[] projection = new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.GROUP_KEY_GROUP_ID,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID};
      
      Cursor currentChannels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, null, null, null);

      try {
        currentChannels.moveToPosition(-1);
        
        final int idIndex = currentChannels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
        final int groupKeyIndex = currentChannels.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
        final int channelKeyIndex = currentChannels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
        
        while(currentChannels.moveToNext()) {
          String key = currentChannels.getString(groupKeyIndex).trim()+"_##_"+currentChannels.getString(channelKeyIndex).trim();
          mCurrentChannelData.put(key, Integer.valueOf(currentChannels.getInt(idIndex)));
        }
      }finally {
        if(currentChannels != null) {
          currentChannels.close();
        }
      }
      
      final File path = IOUtils.getDownloadDirectory(TvDataUpdateService.this.getApplicationContext());

      File groups = new File(path,GROUP_FILE);
      
      String mirror = getGroupFileMirror();
      doLog("LOAD GROUPS FROM '" + mirror + "' to '" + groups + "'");
      if(mirror != null) {
        try {
          IOUtils.saveUrl(groups.getAbsolutePath(), mirror);
          doLog("START GROUP UPDATE");
          updateGroups(groups, path);
        } catch (Exception e) {
          Intent updateDone = new Intent(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE);
          updateDone.putExtra(SettingConstants.CHANNEL_DOWNLOAD_SUCCESSFULLY, false);
          
          LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(updateDone);
          
          stopSelfInternal();
        }
      }
    }
  }
  
  private String getGroupFileMirror() {
    ArrayList<String> groupUrlList = new ArrayList<String>();
    
    groupUrlList.add(DEFAULT_GROUPS_URL + GROUP_FILE);
    
    for(String url : DEFAULT_GROUPS_URL_MIRRORS) {
      groupUrlList.add(url + GROUP_FILE);
    }
    
    String choosenMirror = null;
    
    while(choosenMirror == null && !groupUrlList.isEmpty()) {
      int index = (int)(Math.random() * groupUrlList.size());
      
      String test = groupUrlList.get(index);
      
      if(IOUtils.isConnectedToServer(test, 5000)) {
        choosenMirror = test;
      }
      else {
        groupUrlList.remove(index);
      }
    }
    
    return choosenMirror;
  }
  
  private final class ChangeableFinalBoolean {
    private boolean mValue;
    
    public ChangeableFinalBoolean(boolean value) {
      mValue = value;
    }
    
    public synchronized boolean getBoolean() {
      return mValue;
    }
    
    public synchronized void andUpdateBoolean(boolean value) {
      mValue = mValue && value;
    }
    
    public void setBoolean(boolean value) {
      mValue = value;
    }
  }
  
  private GroupInfo updateGroup(ContentResolver cr, Integer knownId, String dataServiceId, String groupId, String mirrorUrls, ContentValues values) {
    GroupInfo result = null;
    
    String fileName = groupId + "_channellist.gz";
    String urlFileName = fileName;
    
    if(dataServiceId.equals(SettingConstants.EPG_DONATE_KEY)) {
      fileName = groupId + "_channels.gz";
      urlFileName = "channels.gz";
    }
    
    if(knownId == null) {
      doLog("Insert group '" + groupId + "' into database.");
      // The group is not already known, so insert it
      Uri insert = cr.insert(TvBrowserContentProvider.CONTENT_URI_GROUPS, values);
      doLog("Insert group '" + groupId + "' to URI: " + insert);
      int uniqueGroupID = (int)ContentUris.parseId(insert);
      
      String[] urls = loadAvailableMirrorsForGroup(mirrorUrls);
      
      GroupInfo test = new GroupInfo(dataServiceId, urls, urlFileName, fileName, uniqueGroupID);
      
      if(urls.length > 0) {
        doLog("Load channels for group '" + groupId + "' to " + test.getFileName());
        result = test;
      }
      else {
        doLog("Update group '" + groupId + "' NO SUCCESS");
      }
    }
    else {
      doLog("Update group '" + groupId + "' in database.");
      cr.update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_GROUPS,knownId.intValue()), values, null, null);
      
      doLog("Update group '" + groupId + "' loadGroupInfoForGroup().");
      String[] urls = loadAvailableMirrorsForGroup(mirrorUrls);
      
      GroupInfo test = new GroupInfo(dataServiceId, urls, urlFileName, fileName, knownId.intValue());
      
      if(urls.length > 0) {
        doLog("Load channels for group '" + groupId + "' to " + test.getFileName());
        result = test;
      }
      else {
        doLog("Update group '" + groupId + "' NO SUCCESS");
      }
    }
    
    return result;
  }
  
  private void updateGroups(File groups, final File path) {
    final ChangeableFinalBoolean success = new ChangeableFinalBoolean(true);
    Hashtable<String, Integer> currentGroups = new Hashtable<String, Integer>();
    
    if(groups.isFile()) {
      final NotificationManager notification = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      final ArrayList<GroupInfo> channelMirrors = new ArrayList<GroupInfo>();
      
      ContentResolver cr = getContentResolver();
      
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(groups)));
          
        String line = null;
        doLog("Read groups from: " + groups.getName());
        
        final String[] projection = new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID,TvBrowserContentProvider.GROUP_KEY_GROUP_ID};
        
        final Cursor currentGroupsQuery = cr.query(TvBrowserContentProvider.CONTENT_URI_GROUPS, projection, null, null, null);
        
        try {
          currentGroupsQuery.moveToPosition(-1);
          
          int keyIndex = currentGroupsQuery.getColumnIndex(TvBrowserContentProvider.KEY_ID);
          int dataServiceIndex = currentGroupsQuery.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID);
          int groupIndex = currentGroupsQuery.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
          
          while(currentGroupsQuery.moveToNext()) {
            String key = currentGroupsQuery.getString(dataServiceIndex).trim() + "_##_" + currentGroupsQuery.getString(groupIndex).trim();
            
            currentGroups.put(key, Integer.valueOf(currentGroupsQuery.getInt(keyIndex)));
          }
        }finally {
          if(currentGroupsQuery != null) {
            currentGroupsQuery.close();
          }
        }
        
        while((line = in.readLine()) != null) {
          doLog("GROUP LINE: " + line);
          final String[] parts = line.split(";");
          
          final String key = SettingConstants.EPG_FREE_KEY + "_##_" + parts[0].trim();
          final Integer knownId = currentGroups.get(key);
           
          final ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID, SettingConstants.EPG_FREE_KEY);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_ID, parts[0]);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME, parts[1]);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER, parts[2]);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION, parts[3]);
          
          final StringBuilder builder = new StringBuilder(parts[4]);
          
          for(int i = 5; i < parts.length; i++) {
            builder.append(";");
            builder.append(parts[i]);
          }
          doLog("Mirrors for group '" + parts[0] + "': " + builder.toString());
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, builder.toString());
          
          GroupInfo test = updateGroup(cr, knownId, SettingConstants.EPG_FREE_KEY, parts[0], builder.toString(), values);
          
          if(test != null) {
            channelMirrors.add(test);
          }
          else {
            success.andUpdateBoolean(false);
          }
        }
        
        in.close();
      } catch (IOException e) {}
      
      {
        final String key = SettingConstants.EPG_DONATE_KEY + "_##_" + SettingConstants.EPG_DONATE_GROUP_KEY;
        final Integer knownId = currentGroups.get(key);
         
        final ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID, SettingConstants.EPG_DONATE_KEY);
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_ID, SettingConstants.EPG_DONATE_GROUP_KEY);
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME, "EPGdonate");
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER, "René Mach");
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION, "Channels with donate support.");
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, SettingConstants.EPG_DONATE_DEFAULT_URL);
        
        GroupInfo test = updateGroup(cr, knownId, SettingConstants.EPG_DONATE_KEY, SettingConstants.EPG_DONATE_GROUP_KEY, SettingConstants.EPG_DONATE_DEFAULT_URL, values);
        
        if(test != null) {
          channelMirrors.add(test);
        }
        else {
          success.andUpdateBoolean(false);
        }
      }
      
      if(!channelMirrors.isEmpty()) {
        mThreadPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
        mCurrentDownloadCount = 0;
        mBuilder.setProgress(channelMirrors.size(), 0, false);
        notification.notify(NOTIFY_ID, mBuilder.build());
        
        for(final GroupInfo info : channelMirrors) {
          mThreadPool.execute(new Thread("DATA UPDATE GROUP CHANNEL ADDING THREAD") {
            public void run() {
              File group = new File(path,info.getFileName());
              
              boolean groupSucces = false;
              
              for(String url : info.getUrls()) {
                try {
                  if(IOUtils.saveUrl(group.getAbsolutePath(), url + info.getUrlFileName(), 10000)) {
                    groupSucces = addChannels(group,info);
                    
                    mBuilder.setProgress(channelMirrors.size(), mCurrentDownloadCount++, false);
                    notification.notify(NOTIFY_ID, mBuilder.build());
                    
                    if(groupSucces) {
                      doLog("Load channels for group '" + info.getFileName() + "' successfull from: " + url);
                      break;
                    }
                    else {
                      doLog("Not successfull load channels for group '" + info.getFileName() + "' from: " + url);
                    }
                  }
                  else {
                    doLog("Not successfull load channels for group '" + info.getFileName() + "' from: " + url);
                  }
                } catch (Exception e) {
                  groupSucces = false;
                }
              }
              
              success.andUpdateBoolean(groupSucces);
            }
          });
        }
        
        mThreadPool.shutdown();
        
        try {
          mThreadPool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {}
        
        if(!mThreadPool.isTerminated()) {
          mThreadPool.shutdownNow();
          success.setBoolean(false);
        }
      }
      else {
        success.setBoolean(false);
      }
      
      deleteFile(groups);
    }
    else {
      success.setBoolean(false);
    }
    
    Intent updateDone = new Intent(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE);
    updateDone.putExtra(SettingConstants.CHANNEL_DOWNLOAD_SUCCESSFULLY, success.getBoolean());
    
    LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(updateDone);
    
    if(currentGroups != null) {
      currentGroups.clear();
      currentGroups = null;
    }
    
    IS_RUNNING = false;
    stopForeground(true);
    stopSelfInternal();
  }
  
  private synchronized String[] loadAvailableMirrorsForGroup(String mirrorLine) { 
    String[] mirrors = null;
    
    if(mirrorLine.contains(";")) {
      mirrors = mirrorLine.split(";");
    }
    else {
      mirrors = new String[1];
      mirrors[0] = mirrorLine;
    }
    
    ArrayList<String> mirrorList = new ArrayList<String>();
    
    for(String mirror : mirrors) {
      if(IOUtils.isConnectedToServer(mirror,5000)) {
        if(!mirror.endsWith("/")) {
          mirror += "/";
        }
        
        mirrorList.add(mirror);
      }
    }
    
    return mirrorList.toArray(new String[mirrorList.size()]);
  }
  
  private class GroupInfo {
    private String[] mUrlArr;
    private int mUniqueGroupID;
    private String mFileName;
    private String mUrlFileName;
    
    private String mDataServiceId;
    
    public GroupInfo(String dataServiceId, String[] urls, String urlFileName, String fileName, int uniqueGroupID) {
      mDataServiceId = dataServiceId;
      mUrlArr = urls;
      mUniqueGroupID = uniqueGroupID;
      mUrlFileName = urlFileName;
      mFileName = fileName;
    }
    
    public String[] getUrls() {
      return mUrlArr;
    }
    
    public int getUniqueGroupID() {
      return mUniqueGroupID;
    }
    
    public String getFileName() {
      return mFileName;
    }
    
    public String getUrlFileName() {
      return mUrlFileName;
    }
    
    public String getDataServiceId() {
      return mDataServiceId;
    }
  }
  
  // Cursor contains the channel group
  public boolean addChannels(File group, GroupInfo info) {
    boolean returnValue = false;
    
    if(group.isFile()) {
      try {
        BufferedReader read = new BufferedReader(new InputStreamReader(IOUtils.decompressStream(new FileInputStream(group)),"ISO-8859-1"));
        
        String line = null;
        
        boolean returnValueOnceSet = false;
        
        final ArrayList<ContentValues> insertValuesList = new ArrayList<ContentValues>();
        final ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
        
        final String[] projection = new String[] {TvBrowserContentProvider.KEY_ID};
        
        while((line = read.readLine()) != null) {
          String[] parts = line.split(";");
          
          if(!returnValueOnceSet) {
            returnValue = true;
            returnValueOnceSet = true;
          }
          
          String baseCountry = null;
          String timeZone = null;
          String channelId = null;
          String name = null;
          String copyright = null;
          String website = null;
          String logoUrl = null;
          String allCountries = null;
          int category = 0;
          
          if(info.getDataServiceId().equals(SettingConstants.EPG_FREE_KEY)) {
            baseCountry = parts[0];
            timeZone = parts[1];
            channelId = parts[2];
            name = parts[3];
            copyright = parts[4];
            website = parts[5];
            logoUrl = parts[6];
            category = Integer.parseInt(parts[7]);
            allCountries = baseCountry;
          }
          else if(info.getDataServiceId().equals(SettingConstants.EPG_DONATE_KEY)) {
            allCountries = baseCountry = parts[1];
            
            if(baseCountry.contains("$")) {
              String[] countries = baseCountry.split("$");
              
              baseCountry = countries[0];
            }
            
            timeZone = "UTC";
            channelId = parts[0];
            name = parts[2];
            copyright = parts[4];
            website = parts[5];
            logoUrl = parts[3];
            category = Integer.parseInt(parts[6]);
          }
                    
          if(baseCountry != null && name != null) {
            StringBuilder fullName = new StringBuilder();
            
            int i = 8;
            
            if(parts.length > i) {
                do {
                  fullName.append(parts[i]);
                  fullName.append(";");
                }while(!parts[i++].endsWith("\""));
                
                fullName.deleteCharAt(fullName.length()-1);
            }
            
            if(fullName.length() == 0) {
              fullName.append(name);
            }
            
            String joinedChannel = "";
            
            if(parts.length > i) {
              allCountries = parts[i++];
            }
            
            if(parts.length > i) {
              joinedChannel = parts[i];
            }
            
            String key = info.mUniqueGroupID + "_##_" + channelId.trim();
            
            Integer uniqueChannelId = mCurrentChannelData.get(key);
            
           // String where = TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = " + info.mUniqueGroupID + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = '" + channelId + "'";
            
            //ContentResolver cr = getContentResolver();
            
            //Cursor query = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where, null, null);
            
            ContentValues values = new ContentValues();
            
            values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_ID, info.getUniqueGroupID());
            values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, channelId);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY, baseCountry);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE, timeZone);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_NAME, name);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_COPYRIGHT, copyright);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_WEBSITE, website);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_LOGO_URL, logoUrl);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_CATEGORY, category);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_FULL_NAME, fullName.toString().replaceAll("\"", ""));
            values.put(TvBrowserContentProvider.CHANNEL_KEY_ALL_COUNTRIES, allCountries);
            values.put(TvBrowserContentProvider.CHANNEL_KEY_JOINED_CHANNEL_ID, joinedChannel);
            
            if(logoUrl != null && logoUrl.length() > 0) {
              try {
                byte[] blob = IOUtils.loadUrl(logoUrl, 10000);
                
                values.put(TvBrowserContentProvider.CHANNEL_KEY_LOGO, blob);
              }catch(Exception e1) {}
            }
            
            if(uniqueChannelId == null) {
              insertValuesList.add(values);
            }
            else {
              // update channel            
              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, uniqueChannelId.intValue()));
              opBuilder.withValues(values);
              
              updateValuesList.add(opBuilder.build());
            }
          }
          else {
            returnValue = false;
          }
        }
        
        read.close();
        
        if(!insertValuesList.isEmpty()) {
          if(insertValuesList.size() > getContentResolver().bulkInsert(TvBrowserContentProvider.CONTENT_URI_CHANNELS, insertValuesList.toArray(new ContentValues[insertValuesList.size()]))) {
            returnValue = false;
          }
        }
        if(!updateValuesList.isEmpty()) {
          try {
            getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
          } catch (Exception e) {
            returnValue = false;
          }
        }
      } catch (FileNotFoundException e) {
        returnValue = false;
        e.printStackTrace();
      } catch (IOException e) {
        returnValue = false;
        e.printStackTrace();
      }
      
      deleteFile(group);
    }
    
    return returnValue;
  }
  
  private byte[] getXmlBytes(boolean syncFav, boolean syncMarkings) {
    StringBuilder where = new StringBuilder();
    
    if(syncFav) {
      where.append(" ( ").append(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE).append(" ) ");
    }
    if(syncMarkings) {
      if(where.length() > 0) {
        where.append(" OR ");
      }
      
      where.append(" ( ").append(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING).append(" ) ");
    }
        
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.CHANNEL_TABLE + "." + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.GROUP_KEY_GROUP_ID,
        TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY
    };
    
    Cursor programs = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where.toString(), null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    StringBuilder dat = new StringBuilder();
    
    SparseArray<SimpleGroupInfo> groupInfo = new SparseArray<SimpleGroupInfo>();
    
    if(programs.getCount() > 0) {
      programs.moveToPosition(-1);
      
      String[] groupProjection = {
          TvBrowserContentProvider.KEY_ID,
          TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID,
          TvBrowserContentProvider.GROUP_KEY_GROUP_ID
      };
      
      Cursor groups = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupProjection, null, null, null);
      
      if(groups.getCount() > 0) {
        groups.moveToPosition(-1);
        
        while(groups.moveToNext()) {
          int groupKey = groups.getInt(0);
          String dataServiceID = groups.getString(1);
          String groupID = groups.getString(2);
          
          String test = SettingConstants.getNumberForDataServiceKey(dataServiceID);
          
          if(test != null) {
            dataServiceID = test;
          }
          
          groupInfo.put(groupKey, new SimpleGroupInfo(dataServiceID, groupID));
        }
      }
      
      groups.close();
      
      if(groupInfo.size() > 0) {
        int startTimeColumnIndex = programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        int groupKeyColumnIndex = programs.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
        int channelKeyBaseCountryColumnIndex = programs.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY);
        
        while(programs.moveToNext()) {
          int groupID = programs.getInt(groupKeyColumnIndex);
          long startTime = programs.getLong(startTimeColumnIndex) / 60000;
          String channelID = programs.getString(1);
          String baseCountry = programs.getString(channelKeyBaseCountryColumnIndex);
          
          SimpleGroupInfo info = groupInfo.get(groupID);
          
          String groupId = ":" + info.mGroupID;
          
          if(SettingConstants.getNumberForDataServiceKey(SettingConstants.EPG_DONATE_KEY).equals(info.mDataServiceID)) {
            groupId = "";
          }
          
          dat.append(startTime).append(";").append(info.mDataServiceID).append(groupId).append(":").append(baseCountry).append(":").append(channelID).append("\n");
        }
      }
    }
    
    programs.close();
          
    return IOUtils.getCompressedData(dat.toString().getBytes());
  }
  
  public void backSyncPrograms(final NotificationManager notification) {
    mBuilder.setProgress(100, 0, true);
    mBuilder.setContentText(getResources().getText(R.string.update_data_notification_synchronize));
    notification.notify(NOTIFY_ID, mBuilder.build());
    
    final String CrLf = "\r\n";
    
    SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
    
    String car = pref.getString(SettingConstants.USER_NAME, null);
    String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
    
    boolean syncFav = PrefUtils.getBooleanValue(R.string.PREF_SYNC_FAV_TO_DESKTOP, R.bool.pref_sync_fav_to_desktop_default);
    boolean syncMarkings = PrefUtils.getBooleanValue(R.string.PREF_SYNC_MARKED_TO_DESKTOP, R.bool.pref_sync_marked_to_desktop_default);
    
    if((syncFav || syncMarkings) && car != null && bicycle != null && car.trim().length() > 0 && bicycle.trim().length() > 0) {
      String userpass = car.trim() + ":" + bicycle.trim();
      String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
      
      URLConnection conn = null;
      OutputStream os = null;
      InputStream is = null;

      try {
          URL url = new URL("http://android.tvbrowser.org/data/scripts/syncUp.php?type=favortiesFromApp");
          
          conn = url.openConnection();
          
          conn.setRequestProperty ("Authorization", basicAuth);
          
          conn.setDoOutput(true);
          
          byte[] xmlData = getXmlBytes(syncFav, syncMarkings);
          
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
          Log.d("info8", "" ,e);
      } finally {
        Log.d("info8","Close connection");
          try {
              os.close();
          } catch (Exception e) {
          }
          try {
              is.close();
          } catch (Exception e) {
          }
          try {

          } catch (Exception e) {
          }
      }
    }
    
    notification.cancel(NOTIFY_ID);
  }
  
  private void loadAccessAndFavoriteSync() {
    try {
      URL documentUrl = new URL("http://android.tvbrowser.org/data/scripts/syncDown.php?type=favoritesFromDesktop");
      URLConnection connection = documentUrl.openConnection();
      
      SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
      
      String car = pref.getString(SettingConstants.USER_NAME, null);
      String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
      
      if(PrefUtils.getBooleanValue(R.string.PREF_SYNC_FAV_FROM_DESKTOP, R.bool.pref_sync_fav_from_desktop_default) && car != null && bicycle != null && car.trim().length() > 0 && bicycle.trim().length() > 0) {
        String userpass = car.trim() + ":" + bicycle.trim();
        String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
        
        connection.setRequestProperty ("Authorization", basicAuth);
        
        BufferedReader read = new BufferedReader(new InputStreamReader(IOUtils.decompressStream(connection.getInputStream()),"UTF-8"));
        
        String dateValue = read.readLine();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date syncDate = dateFormat.parse(dateValue.trim());
        
        if(syncDate.getTime() > System.currentTimeMillis()) {
          mSyncFavorites = new ArrayList<String>();
          
          String line = null;
          
          while((line = read.readLine()) != null) {
            mSyncFavorites.add(line);
          }
        }
        
        read.close();
      }
    }catch(Throwable t) {}
  }
  /**
   * Calculate the end times of programs that are missing end time in the data.
   */
  private void calculateMissingEnds(NotificationManager notification, boolean updateFavorites) {
    try {
      mBuilder.setProgress(100, 0, true);
      mBuilder.setContentText(getResources().getText(R.string.update_notification_calculate));
      notification.notify(NOTIFY_ID, mBuilder.build());
      
      // Only ID, channel ID, start and end time are needed for update, so use only these columns
      String[] projection = {
          TvBrowserContentProvider.KEY_ID,
          TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
          TvBrowserContentProvider.DATA_KEY_STARTTIME,
          TvBrowserContentProvider.DATA_KEY_ENDTIME,
          TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME,
          TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE
      };
            
      Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " DESC");
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      
      // only if there are data update it
      if(c.getCount() > 0) {
        int nettoColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME);
        
        int keyIDColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
        int channelKeyColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
        int startTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        int endTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
        int timeZoneColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE);
        
        long lastStartTime = -1;
        int lastChannelKey = -1;
        
        c.moveToPosition(-1);
        
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar cal = null;
        
        while(c.moveToNext()) {
          long progID = c.getLong(keyIDColumn);
          int channelKey = c.getInt(channelKeyColumn);
          long meStart = c.getLong(startTimeColumn);
          long end = c.getLong(endTimeColumn);
          long nettoPlayTime = 0;
                    
          if(c.isNull(nettoColumn)) {
            nettoPlayTime = c.getLong(nettoColumn) * 60000;
          }
          
          if(lastChannelKey == channelKey) {
            cal = Calendar.getInstance(TimeZone.getTimeZone(c.getString(timeZoneColumn)));
            
            // if end not set or netto play time larger than next start or next time not end time
            if(end == 0 || (nettoPlayTime > (lastStartTime - meStart))/* || (lastProgram && end != nextStart && ((nextStart - meStart) < (3 * 60 * 60000)))*/) {
              if(nettoPlayTime > (lastStartTime - meStart)) {
                lastStartTime = meStart + nettoPlayTime;
              }
              else if((lastStartTime - meStart) >= (12 * 60 * 60000)) {
                lastStartTime = meStart + (long)(2.5 * 60 * 60000);
              }
              
              utc.setTimeInMillis((((long)(cal.getTimeInMillis() / 60000)) * 60000));
              
              ContentValues values = new ContentValues();
              values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, lastStartTime);
              values.put(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES, (int)((lastStartTime-meStart)/60000));
              
              cal.setTimeInMillis(lastStartTime);
              
              int startHour = cal.get(Calendar.HOUR_OF_DAY);
              int startMinute = cal.get(Calendar.MILLISECOND);
              
              // Normalize start hour and minute to 2014-12-31 to have the same time base on all occasions
              utc.setTimeInMillis((((long)(IOUtils.normalizeTime(cal, startHour, startMinute, 30).getTimeInMillis() / 60000)) * 60000));              
              
              values.put(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT, utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE));
              
              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, progID));
              opBuilder.withValues(values);
              
              updateValuesList.add(opBuilder.build());
            }
          }
          
          lastChannelKey = channelKey;
          lastStartTime = meStart;
        }
      }
      
      c.close();
      
      if(!updateValuesList.isEmpty()) {
        getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
      }
    }catch(Throwable t) {
      Log.d("info13", "", t);
    }
    
    finishUpdate(notification,updateFavorites);
  }
    
  private void finishUpdate(NotificationManager notification, boolean updateFavorites) {
    doLog("FINISH DATA UPDATE");
    TvBrowserContentProvider.INFORM_FOR_CHANGES = true;
    getContentResolver().notifyChange(TvBrowserContentProvider.CONTENT_URI_DATA, null);
  
    if(updateFavorites) {
      updateFavorites(notification);
    }
    
    syncFavorites(notification);
    
    boolean fromRemider = PrefUtils.getBooleanValue(R.string.PREF_SYNC_REMINDERS_FROM_DESKTOP, R.bool.pref_sync_reminders_from_desktop_default);
    boolean toRemider = PrefUtils.getBooleanValue(R.string.PREF_SYNC_REMINDERS_TO_DESKTOP, R.bool.pref_sync_reminders_to_desktop_default);
    
    if(fromRemider) {
      synchronizeRemindersDown(false, notification);
    }
    
    if(toRemider) {
      synchronizeUp(false, null, "http://android.tvbrowser.org/data/scripts/syncUp.php?type=reminderFromApp", notification);
    }
    
    if(PrefUtils.getBooleanValue(R.string.PREF_NEWS_SHOW, R.bool.pref_news_show_default)) {
      long lastNewsUpdate = PrefUtils.getLongValue(R.string.NEWS_DATE_LAST_DOWNLOAD, 0);
      long daysSinceLastNewsUpdate = (System.currentTimeMillis() - lastNewsUpdate) / (24 * 60 * 60000L);
      
      if(daysSinceLastNewsUpdate > 3 && (Math.random() * 7 < daysSinceLastNewsUpdate)) {
        mBuilder.setProgress(100, 0, true);
        mBuilder.setContentText(getResources().getText(R.string.update_data_notification_load_news));
        notification.notify(NOTIFY_ID, mBuilder.build());
        
        NewsReader.readNews(TvDataUpdateService.this, mHandler);
      }
    }
    
    Intent inform = new Intent(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(inform);
    
    mDontWantToSeeValues = null;
    
    // Data update complete inform user
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(TvDataUpdateService.this, R.string.update_complete, Toast.LENGTH_LONG).show();
      }
    });
    
    doLog("Unsuccessful downloads: " + String.valueOf(mUnsuccessfulDownloads));
        
    Editor edit = PreferenceManager.getDefaultSharedPreferences(TvDataUpdateService.this).edit();
    edit.putLong(getString(R.string.LAST_DATA_UPDATE), System.currentTimeMillis());
    edit.commit();
    
    Favorite.handleDataUpdateFinished();
        
    stopForeground(true);
    
    PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
    
    if(pm.isScreenOn()) {
      UiUtils.updateImportantProgramsWidget(getApplicationContext());
      UiUtils.updateRunningProgramsWidget(getApplicationContext());
    }
    
    IS_RUNNING = false;
    
    stopSelfInternal();
  }
  
  private AtomicInteger mFavoriteUpdateCount;
  
  private void updateFavorites(final NotificationManager notification) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TvDataUpdateService.this);
    
    final Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
    
    mBuilder.setProgress(favoritesSet.size(), 0, false);
    mBuilder.setContentText(getResources().getText(R.string.update_data_notification_favorites));
    notification.notify(NOTIFY_ID, mBuilder.build());
    
    ExecutorService updateFavorites = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
    
    mFavoriteUpdateCount = new AtomicInteger(1);
    
    for(String favorite : favoritesSet) {
      final Favorite fav = new Favorite(favorite);
      
      if(!updateFavorites.isShutdown()) {
        updateFavorites.execute(new Thread("DATA UPDATE FAVORITE UPDATE THREAD") {
          @Override
          public void run() {
            Favorite.updateFavoriteMarking(TvDataUpdateService.this, getContentResolver(), fav);
            mBuilder.setProgress(favoritesSet.size(), mFavoriteUpdateCount.getAndIncrement(), false);
            notification.notify(NOTIFY_ID, mBuilder.build());
          }
        });
      }
    }
    
    updateFavorites.shutdown();
    
    try {
      updateFavorites.awaitTermination(favoritesSet.size(), TimeUnit.MINUTES);
    } catch (InterruptedException e) {}
    
    notification.cancel(NOTIFY_ID);
    
    backSyncPrograms(notification);
  }
  
  public void doLog(String value) {
    Logging.log("info7", value, Logging.DATA_UPDATE_TYPE, TvDataUpdateService.this);
  }
  
  private void doLogData(String msg) {
    Log.d("info5", msg);
  }
  
  private String reloadMirrors(String groupID, File path) {
    String groupTxt = getGroupFileMirror();
    String mirrorLine = "";
    
    if(groupTxt != null) {
      File groups = new File(path,GROUP_FILE);
      
      try {
        IOUtils.saveUrl(groups.getAbsolutePath(), groupTxt);
        
        if(groups.isFile()) {
          BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(groups)));
          
          String line = null;
          
          while((line = in.readLine()) != null && mirrorLine.trim().length() == 0) {
            if(line.startsWith(groupID + ";")) {
              String[] parts = line.split(";");
              
              StringBuilder builder = new StringBuilder(parts[4]);
              
              for(int i = 5; i < parts.length; i++) {
                builder.append(";");
                builder.append(parts[i]);
              }
              
              mirrorLine = builder.toString();
            }
          }
          
          in.close();
        }
        
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      deleteFile(groups);
    }
    
    return mirrorLine;
  }
  
  private WakeLock mWakeLock;
  
  private static final class MirrorDownload {
    private String mDownloadURL;
    private String mFileName;
    
    public MirrorDownload(String downloadURL, String fileName) {
      mDownloadURL = downloadURL;
      mFileName = fileName;
    }
    
    public String getDownloadURL() {
      return mDownloadURL;
    }
    
    public String getFileName() {
      return mFileName;
    }
  }
  
  private void updateTvData() {
    doLog("Running state: " + IS_RUNNING);
    if(!IS_RUNNING) {
      final UncaughtExceptionHandler handleExc = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
          doLog("UNCAUGHT EXCEPTION Thread: " + thread.getName() + " Throwable " + ex.toString());
        }
      };
      
      PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
      mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TVBUPDATE_LOCK");
      mWakeLock.setReferenceCounted(false);
      mWakeLock.acquire(120*60000L);
      
      mShowNotification = true;
      mUnsuccessfulDownloads = 0;
      IS_RUNNING = true;
      
      doLog("Favorite.handleDataUpdateStarted()");
      Favorite.handleDataUpdateStarted();
      
      final File path = IOUtils.getDownloadDirectory(TvDataUpdateService.this.getApplicationContext());
      
      File[] oldDataFiles = path.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().toLowerCase().endsWith(".gz");
        }
      });
      
      for(File oldFile : oldDataFiles) {
        deleteFile(oldFile);
      }
      
      int[] levels = null;
            
      Set<String> exclusions = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);
      
      if(exclusions != null) {
        mDontWantToSeeValues = new DontWantToSeeExclusion[exclusions.size()];
        
        int i = 0;
        
        for(String exclusion : exclusions) {
          mDontWantToSeeValues[i++] = new DontWantToSeeExclusion(exclusion);
        }
      }
      
      if(PrefUtils.getBooleanValue(R.string.LOAD_FULL_DATA, R.bool.load_full_data_default)) {
        if(PrefUtils.getBooleanValue(R.string.LOAD_PICTURE_DATA, R.bool.load_picture_data_default)) {
          levels = new int[5];
        }
        else {
          levels = new int[3];
        }
        
        for(int j = 0; j < levels.length; j++) {
          levels[j] = j;
        }
      }
      else if (PrefUtils.getBooleanValue(R.string.LOAD_PICTURE_DATA, R.bool.load_picture_data_default)) {
        levels = new int[3];
        
        levels[0] = 0;
        levels[1] = 3;
        levels[2] = 4;
      }
      else {
        levels = new int[1];
        
        levels[0] = 0;
      }
      
      mCurrentDownloadCount = 0;
      mBuilder.setContentTitle(getResources().getText(R.string.update_notification_title));
      mBuilder.setContentText(getResources().getText(R.string.update_notification_text));
      
      startForeground(NOTIFY_ID, mBuilder.build());
      final NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
      //notification.notify(NOTIFY_ID, mBuilder.build());*/
      doLog("loadAccessAndFavoriteSync()");
      loadAccessAndFavoriteSync();
      
      TvBrowserContentProvider.INFORM_FOR_CHANGES = false;
      
      ContentResolver cr = getContentResolver();
      
      StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
     // where.append(" = 1");
      
      final ArrayList<ChannelUpdate> updateList = new ArrayList<ChannelUpdate>();
      int downloadCountTemp = 0;
      doLog("readCurrentVersionIDs()");
      readCurrentVersionIDs();
      
      DataHandler epgFreeDataHandler = new EPGfreeDataHandler();
      DataHandler epgDonateDataHandler = new EPGdonateDataHandler();
      
      ArrayList<MirrorDownload> downloadMirrorList = new ArrayList<MirrorDownload>();
      
      Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where.toString(), null, TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      doLog("channelCursor: " + channelCursor);
      boolean donationInfoLoaded = false;
      
      if(channelCursor.moveToFirst()) {
        int lastGroup = -1;
        Mirror mirror = null;
        String groupId = null;
        Summary summary = null;
        String channelName = null;
        
        do {
          try {
            int groupKey = channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
            int channelKey = channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
            String timeZone = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE));
            channelName = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            doLog("Load info for channel " + channelName);
            
            if(lastGroup != groupKey) {
              summary = null;
              mirror = null;
              groupId = null;
              doLog("Content URI for data update " + ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupKey));
              Cursor group = cr.query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupKey), null, null, null, null);
              doLog("Cursor size for groupKey: " + group.getCount());
              if(group.getCount() > 0) {
                group.moveToFirst();
                
                groupId = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
                String mirrorURL = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS));
                Log.d("info21", "GROUPID " + groupId + " " + mirrorURL);
                doLog("Available mirrorURLs for group '" + groupId + "': " + mirrorURL);
                doLog("Group info for '" + groupId + "'  groupKey: " + groupKey + " group name: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME)) + " group provider: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER)) + " group description: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION)));
                
                if(!mirrorURL.contains("http://") && !mirrorURL.contains("https://")) {
                  doLog("RELOAD MIRRORS FOR '" + groupId);
                  mirrorURL = reloadMirrors(groupId, path);
                  
                  doLog("Available mirrorURLs for group '" + groupId + "': " + mirrorURL);
                  doLog("Group info for '" + groupId + "'  groupKey: " + groupKey + " group name: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME)) + " group provider: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER)) + " group description: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION)));
                }
                
                boolean checkOnlyConnection = false;
                
                if(groupId.equals(SettingConstants.EPG_DONATE_GROUP_KEY)) {
                  checkOnlyConnection = true;
                }
                
                Mirror[] mirrors = Mirror.getMirrorsFor(mirrorURL);
                Log.d("info21", "MIRRORS AVAILABLE " + mirrors);
                mirror = Mirror.getMirrorToUseForGroup(mirrors, groupId, this, checkOnlyConnection);                
                doLog("Choosen mirror for group '" + groupId + "': " + mirror);
                
                Log.d("info21", "MIRROR CHOOSEN " + mirror);
                if(mirror != null) {
                  String url = mirror.getUrl() + groupId + "_mirrorlist.gz";
                  String fileName = groupId + "_mirrorlist.gz";
                  
                  String summaryUrl = mirror.getUrl() + groupId + "_summary.gz";
                  String summaryFileName = groupId + "_summary.gz";
                  
                  if(checkOnlyConnection) {
                    if(!donationInfoLoaded) {
                      Editor edit = PreferenceManager.getDefaultSharedPreferences(TvDataUpdateService.this).edit();
                      edit.putLong(getString(R.string.EPG_DONATE_LAST_DATA_DOWNLOAD), System.currentTimeMillis());
                      Log.d("info22", "EPG DONATE FIRST DOWNLOAD " + PrefUtils.getLongValue(R.string.EPG_DONATE_FIRST_DATA_DOWNLOAD, -1));
                      if(PrefUtils.getLongValue(R.string.EPG_DONATE_FIRST_DATA_DOWNLOAD, -1) == -1) {
                        edit.putLong(getString(R.string.EPG_DONATE_FIRST_DATA_DOWNLOAD), System.currentTimeMillis());
                      }
                      
                      File donationInfo = new File(path,groupId+"_donationinfo.gz");
                      
                      IOUtils.saveUrl(donationInfo.getAbsolutePath(), mirror.getUrl() + "donationinfo.gz");
                      
                      if(donationInfo.isFile()) {
                        Properties donationProp = new Properties();
                        
                        GZIPInputStream in = null;
                        try {
                          in = new GZIPInputStream(new FileInputStream(donationInfo));
                          
                          donationProp.load(in);
                        } catch(IOException e) {
                          // ignore just load the info next time
                        } finally {
                          IOUtils.closeInputStream(in);
                        }
                        
                        edit.putString(getString(R.string.EPG_DONATE_CURRENT_DONATION_PERCENT), donationProp.getProperty(SettingConstants.EPG_DONATE_DONATION_INFO_PERCENT_KEY,"-1"));
                        
                        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
                        
                        String donationAmount = donationProp.getProperty(SettingConstants.EPG_DONATE_DONATION_INFO_AMOUNT_KEY_PREFIX+year, null);
                        
                        if(donationAmount != null) {
                          edit.putString(getString(R.string.EPG_DONATE_CURRENT_DONATION_AMOUNT_PREFIX)+"_"+year, donationAmount);
                        }
                        
                        if(!donationInfo.delete()) {
                          donationInfo.deleteOnExit();
                        }
                      }
                      
                      edit.commit();
                      donationInfoLoaded = true;
                    }
                    
                    url = mirror.getUrl() + "mirrors.gz";
                    fileName = groupId + "_mirrors.gz";
                    
                    summaryUrl = mirror.getUrl() + "summary.gz";
                    
                    Log.d("info21", "SUMMARY " + summaryUrl);
                  }
                  
                  doLog("Donwload summary from: " + summaryUrl + "_summary.gz");
                  summary = readSummary(new File(path,summaryFileName),summaryUrl, groupId);
                  
                  doLog("To download: " + url);
                  downloadMirrorList.add(new MirrorDownload(url, fileName));
                }
              }
              
              group.close();
            }
            
            doLog("Summary downloaded: " + (summary != null));
            
            if(summary != null && mirror != null) {
              String channelID = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
              
              Calendar now = Calendar.getInstance();
              now.add(Calendar.DAY_OF_MONTH, -2);

              Calendar to = Calendar.getInstance();
              to.add(Calendar.DAY_OF_MONTH, mDaysToLoad);
              doLog("End date of data to download for frame with ID '" + channelID + "': " + to.getTime());

              if(summary instanceof EPGfreeSummary) {
                doLog("Load summary info for: " + channelID);
                ChannelFrame frame = ((EPGfreeSummary)summary).getChannelFrame(channelID);
                doLog("Summary frame with ID '" + channelID + "' read: " + (frame != null));
                
                if(frame != null) {
                  Calendar startDate = ((EPGfreeSummary)summary).getStartDate();
                  
                  Calendar testDate = Calendar.getInstance();
                  testDate.setTimeInMillis(startDate.getTimeInMillis());
                  testDate.set(Calendar.HOUR_OF_DAY, 0);
                  doLog("Start date of data for frame with ID '" + channelID + "': " + startDate.getTime());
                  
                  for(int i = 0; i < frame.getDayCount(); i++) {
                    startDate.add(Calendar.DAY_OF_YEAR, 1);
                    testDate.add(Calendar.DAY_OF_YEAR, 1);
                    
                    if(testDate.compareTo(now) >= 0 && testDate.compareTo(to) <= 0) {
                      int[] version = frame.getVersionForDay(i);
                      doLog("Version found for frame with ID '" + channelID + "': " + (version != null));
                      
                      if(version != null) {
                        long daysSince1970 = startDate.getTimeInMillis() / 24 / 60 / 60000;
                        
                        String versionKey = channelKey + "_" + daysSince1970;
                                            
                        ChannelUpdate channelUpdate = new ChannelUpdate(epgFreeDataHandler, channelKey, timeZone, startDate.getTimeInMillis());
                        
                        for(int level : levels) {
                          int testVersion = 0;
                                                
                          int[] versionInfo = mCurrentVersionIDs.get(versionKey);
                          
                          if(versionInfo != null) {
                            testVersion = versionInfo[level+1];
                          }
                          
                          doLog("Currently known version for '" + channelID + "' for level '" + level + "' and days since 1970 '" + daysSince1970 + "': " + testVersion);
                          
                          if(version.length > level && version[level] > testVersion) {
                            String month = String.valueOf(startDate.get(Calendar.MONTH)+1);
                            String day = String.valueOf(startDate.get(Calendar.DAY_OF_MONTH));
                            
                            if(month.length() == 1) {
                              month = "0" + month;
                            }
                            
                            if(day.length() == 1) {
                              day = "0" + day;
                            }
                            doLog("Version for day unknown for '" + channelID + "'");
                            StringBuilder dateFile = new StringBuilder();
                            dateFile.append(mirror.getUrl());
                            dateFile.append(startDate.get(Calendar.YEAR));
                            dateFile.append("-");
                            dateFile.append(month);
                            dateFile.append("-");
                            dateFile.append(day);
                            dateFile.append("_");
                            dateFile.append(frame.getCountry());
                            dateFile.append("_");
                            dateFile.append(frame.getChannelID());
                            dateFile.append("_");
                            dateFile.append(SettingConstants.EPG_FREE_LEVEL_NAMES[level]);
                            dateFile.append("_full.prog.gz");
                            
                            doLog("Download data for '" + channelID + "' from " + dateFile.toString() + " for level: " + level);
                            
                            channelUpdate.addURL(dateFile.toString());
                          }
                        }
                        
                        doLogData(" CHANNEL UPDATE TO DOWNLOAD " + channelUpdate.getChannelID() + " " + channelUpdate.getDate() + " " + channelUpdate.toDownload());
                        
                        if(channelUpdate.toDownload()) {
                          updateList.add(channelUpdate);
                          downloadCountTemp += channelUpdate.size();
                        }
                      }
                    }
                  }
                }
              }
              else if(summary instanceof EPGdonateSummary) {
                Set<Object> keys = ((EPGdonateSummary)summary).keySet();
                
                boolean loadMoreData = levels.length >= 3;
                boolean loadPictureData = levels.length >= 5;
                
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.set(2014, 12, 31, 0, 0, 0);
                utc.set(Calendar.MILLISECOND, 0);
                
                for(Object key : keys) {
                  if(key instanceof String && ((String)key).contains(channelID) && ((String)key).contains("_")) {
                    String stringKey = (String)key;
                    //Log.d("info21", "KEY " + key);
                    Object value = ((EPGdonateSummary)summary).get(stringKey);
                    
                    if(value instanceof String) {
                      String stringValue = (String)value;
                      //Log.d("info21", "stringValue " + stringValue);
                      long startMilliseconds = Long.parseLong(stringKey.substring(0,stringKey.indexOf("_"))) * 60000L;
                      //Log.d("info21", "onTime " + ((startMilliseconds >= now.getTimeInMillis() && startMilliseconds <= to.getTimeInMillis())));
                      if(startMilliseconds >= now.getTimeInMillis() && startMilliseconds <= to.getTimeInMillis()) {
                        ChannelUpdate channelUpdate = new ChannelUpdate(epgDonateDataHandler, channelKey, timeZone, startMilliseconds);

                        long daysSince1970 = startMilliseconds / 24 / 60 / 60000L;
                        
                        String versionKey = channelKey + "_" + daysSince1970;
                        
                        int[] versionInfo = mCurrentVersionIDs.get(versionKey);
                        
                        if(versionInfo == null) {
                          versionInfo = new int[] {0,0,0,0,0,0};
                        }
                                       
                        String[] versionParts = stringValue.split(",");
                        Log.d("info21", "versionInfo " + versionInfo[1] + " " + versionInfo[3] + " " + versionInfo[5]);
                        if(versionInfo[1] < Integer.parseInt(versionParts[0])) {
                          Log.d("info21", "ADDING " + mirror.getUrl() + stringKey + SettingConstants.EPG_DONATE_LEVEL_NAMES[0] + ".gz");
                          channelUpdate.addURL(mirror.getUrl() + stringKey + SettingConstants.EPG_DONATE_LEVEL_NAMES[0] + ".gz");
                        }
                        if(loadMoreData && versionInfo[3] < Integer.parseInt(versionParts[1])) {
                          channelUpdate.addURL(mirror.getUrl() + stringKey + SettingConstants.EPG_DONATE_LEVEL_NAMES[1] + ".gz");
                        }
                        if(loadPictureData && versionInfo[5] < Integer.parseInt(versionParts[2])) {
                          channelUpdate.addURL(mirror.getUrl() + stringKey + SettingConstants.EPG_DONATE_LEVEL_NAMES[2] + ".gz");
                        }
                        
                        if(channelUpdate.toDownload()) {
                          Log.d("info21", "ADDING " + stringValue);
                          updateList.add(channelUpdate);
                          downloadCountTemp += channelUpdate.size();
                        }
                      }
                    }
                  }
                }
              }
            }
            
            lastGroup = groupKey;
          }catch(Exception e) {
            StringBuilder stackTrace = new StringBuilder("PROBLEM FOR CHANNEL: ");
            stackTrace.append(channelName);
            stackTrace.append(" ");
            stackTrace.append(e.getLocalizedMessage());
            stackTrace.append("\n");
            
            for(StackTraceElement element : e.getStackTrace()) {
              stackTrace.append(element.getLineNumber()).append(" ").append(element.getFileName()).append(" ").append(element.getClassName()).append(" ").append(element.getMethodName());
              stackTrace.append("\n");
            }
            
            doLog(stackTrace.toString());
            Log.d("info21", stackTrace.toString());
          }
        }while(channelCursor.moveToNext());
      }
      
      channelCursor.close();
      
      final int downloadCount = downloadMirrorList.size() + downloadCountTemp;
      doLog("Data files to load " + downloadCount);
      mThreadPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
      mDataUpdatePool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
      
      mBuilder.setProgress(downloadCount, 0, false);
      notification.notify(NOTIFY_ID, mBuilder.build());
      
      for(final MirrorDownload mirror : downloadMirrorList) {
        final File mirrorFile = new File(path,mirror.getFileName());
        
        if(!mThreadPool.isShutdown()) {
          mThreadPool.execute(new Thread("DATA UPDATE MIRROR UPDATE THREAD") {
            public void run() {
              setUncaughtExceptionHandler(handleExc);
              
              try {
                IOUtils.saveUrl(mirrorFile.getAbsolutePath(), mirror.getDownloadURL());
                updateMirror(mirrorFile);
                mCurrentDownloadCount++;
                
                if(mShowNotification) {
                  mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
                  notification.notify(NOTIFY_ID, mBuilder.build());
                }
              } catch (Exception e) {
                mUnsuccessfulDownloads++;
              }
            }
          });
        }
      }
      //mDontWantToSeeValues = null;
      Log.d("info5", "updateCount " + downloadCountTemp);
      
      mDataInsertList = new ArrayList<ContentValues>();
      mDataUpdateList = new ArrayList<ContentProviderOperation>();
      
      mVersionInsertList = new ArrayList<ContentValues>();
      mVersionUpdateList = new ArrayList<ContentProviderOperation>();
      
      if(downloadCountTemp > 0) {
        readCurrentData();
                
        for(final ChannelUpdate update : updateList) {
          if(!mThreadPool.isShutdown()) {
            mThreadPool.execute(new Thread("DATA UPDATE DATA DOWNLOAD THEAD") {
              public void run() {
                setUncaughtExceptionHandler(handleExc);
                update.download(path, notification, downloadCount);
              }
            });
          }
        }
      }
      
      mThreadPool.shutdown();
      
      if(!mThreadPool.isTerminated()) {
        try {
          mThreadPool.awaitTermination(updateList.size(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          doLog("DOWNLOAD WAITING INTERRUPTED " + e.getLocalizedMessage());
        }
      }
      
      mDataUpdatePool.shutdown();
      
      doLog("WAIT FOR DATA UPDATE FOR: " + updateList.size() + " MINUTES");
      
      if(!mDataUpdatePool.isTerminated()) {
        try {
          mDataUpdatePool.awaitTermination(updateList.size(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          doLog("UPDATE DATE INTERRUPTED " + e.getLocalizedMessage());
        }
      }
      
      doLog("WAIT FOR DATA UPDATE FOR DONE, DOWNLOAD: " + mThreadPool.isTerminated() + " DATA: " + mDataUpdatePool.isTerminated());
      
      mShowNotification = false;
      
      insert(mDataInsertList);
      insertVersion(mVersionInsertList);
      
      update(mDataUpdateList);      
      update(mVersionUpdateList);
      
      mDataInsertList = null;
      mDataUpdateList = null;
      
      mVersionInsertList = null;      
      mVersionUpdateList = null;

      mBuilder.setProgress(100, 0, true);
      notification.notify(NOTIFY_ID, mBuilder.build());
      
      mCurrentVersionIDs.clear();
      mCurrentVersionIDs = null;
      
      if(mCurrentData != null) {
        mCurrentData.clear();
        mCurrentData = null;
      }
      
      if(updateList.size() > 0) {
        calculateMissingEnds(notification,true);
      }
      else {
        finishUpdate(notification,false);
      }
    }
    else {
      stopSelfInternal();
    }
  }
  
  private static final class CurrentDataHolder {
    long mProgramID;
    boolean mDontWantToSee;
    String mTitle;
  }
  
  private void readCurrentVersionIDs() {
    if(mCurrentVersionIDs != null) {
      mCurrentVersionIDs.clear();
    }
    else {
      mCurrentVersionIDs = new Hashtable<String, int[]>();
    }
    
    Cursor ids = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, null, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    if(ids.getCount() > 0) {
      ids.moveToPosition(-1);
      
      int keyColumn = ids.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int channelIDColumn = ids.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      int daysSince1970Column = ids.getColumnIndex(TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970);
      int baseVersionColumn = ids.getColumnIndex(TvBrowserContentProvider.VERSION_KEY_BASE_VERSION);
      int more0016Column = ids.getColumnIndex(TvBrowserContentProvider.VERSION_KEY_MORE0016_VERSION);
      int more1600Column = ids.getColumnIndex(TvBrowserContentProvider.VERSION_KEY_MORE1600_VERSION);
      int picture0016Column = ids.getColumnIndex(TvBrowserContentProvider.VERSION_KEY_PICTURE0016_VERSION);
      int picture1600Column = ids.getColumnIndex(TvBrowserContentProvider.VERSION_KEY_PICTURE1600_VERSION);
      
      while(ids.moveToNext()) {
        int[] versionInfo = new int[6];
        
        versionInfo[0] = ids.getInt(keyColumn);
        versionInfo[1] = ids.getInt(baseVersionColumn);
        versionInfo[2] = ids.getInt(more0016Column);
        versionInfo[3] = ids.getInt(more1600Column);
        versionInfo[4] = ids.getInt(picture0016Column);
        versionInfo[5] = ids.getInt(picture1600Column);
        
        String id = ids.getInt(channelIDColumn) + "_" + ids.getInt(daysSince1970Column);
        
        mCurrentVersionIDs.put(id, versionInfo);
      }
    }
    
    ids.close();
  }
  
  private void readCurrentData() {
    if(mCurrentData != null) {
      mCurrentData.clear();
    }
    else {
      mCurrentData = new Hashtable<String, Hashtable<String,CurrentDataHolder>>();
    }
    
    String[] projection = {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, TvBrowserContentProvider.DATA_KEY_UNIX_DATE, TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID, TvBrowserContentProvider.DATA_KEY_DATE_PROG_STRING_ID, TvBrowserContentProvider.DATA_KEY_TITLE, TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE};
    
    Cursor data = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + ", " + TvBrowserContentProvider.DATA_KEY_UNIX_DATE + ", " + TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID + ", " + TvBrowserContentProvider.DATA_KEY_DATE_PROG_STRING_ID);
    
    Hashtable<String, CurrentDataHolder> current = null;
    String currentKey = null;
    
    int keyColumn = data.getColumnIndex(TvBrowserContentProvider.KEY_ID);
    int frameIDColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID);
    int frameIdStringColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DATE_PROG_STRING_ID);
    int channelColumn = data.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    int unixDateColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_UNIX_DATE);
    int titleColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
    int dontWantToSeeColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE);
    
    if(data.getCount() > 0) {
      data.moveToPosition(-1);
      
      try {
        while(!data.isClosed() && data.moveToNext()) {
          long programKey = data.getInt(keyColumn);
          String frameID = data.getString(frameIdStringColumn);
          
          if(frameID == null) {
            frameID = data.getString(frameIDColumn);
          }
          
          int channelID = data.getInt(channelColumn);
          long unixDate = data.getLong(unixDateColumn);
          
          String testKey = channelID + "_" + unixDate;
          
          if(currentKey == null || !currentKey.equals(testKey)) {
            currentKey = testKey;
            current = mCurrentData.get(testKey);
            
            if(current == null) {
              current = new Hashtable<String, CurrentDataHolder>();
              
              mCurrentData.put(currentKey, current);
            }
          }
          
          CurrentDataHolder holder = new CurrentDataHolder();
          
          holder.mProgramID = programKey;
          holder.mTitle = data.getString(titleColumn);
          holder.mDontWantToSee = data.getInt(dontWantToSeeColumn) == 1;
          
          current.put(frameID, holder);
        }
      }catch(IllegalStateException e) {}
    }
    
    data.close();
  }
  
  private void updateMirror(File mirrorFile) {
    if(mirrorFile.isFile()) {
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(IOUtils.decompressStream(new FileInputStream(mirrorFile)),"ISO-8859-1"));
        
        StringBuilder mirrors = new StringBuilder();
        
        String line = null;
        
        doLog("Update mirrors from: " + mirrorFile.getName());
        
        while((line = in.readLine()) != null) {
          line = line.replace(";", "#");
          mirrors.append(line);
          mirrors.append(";");
          doLog("Mirror line in file :'" + mirrorFile.getName() + "': " + line);
        }
        
        if(mirrors.length() > 0) {
          mirrors.deleteCharAt(mirrors.length()-1);
        }
        
        doLog("Complete mirrors for database for file '" + mirrorFile.getName() + "' " + mirrors.toString());
        
        if(mirrors.length() > 0 && (mirrors.indexOf("http://") >= 0 || mirrors.indexOf("https://") >= 0)) {
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, mirrors.toString());
          
          getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_GROUPS, values, TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = \"" + mirrorFile.getName().substring(0, mirrorFile.getName().lastIndexOf("_"))+"\"", null);
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      deleteFile(mirrorFile);
    }
  }
  
  private Summary readSummary(final File path, final String summaryurl, String groupKey) {
    Summary summary = new EPGfreeSummary();
    
    if(groupKey.equals(SettingConstants.EPG_DONATE_GROUP_KEY)) {
      summary = new EPGdonateSummary();
    }
    
    try {
      IOUtils.saveUrl(path.getAbsolutePath(), summaryurl);
      
      if(path.isFile()) {
        if(summary instanceof EPGfreeSummary) {
          BufferedInputStream in = new BufferedInputStream(IOUtils.decompressStream(new FileInputStream(path)));
          
          int version = in.read();
                  
          long daysSince1970 = ((in.read() & 0xFF) << 16 ) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
          
          ((EPGfreeSummary)summary).setStartDaySince1970(daysSince1970);
          
          ((EPGfreeSummary)summary).setLevels(in.read());
          
          int frameCount = (in.read() & 0xFF << 8) | (in.read() & 0xFF);
          
          for(int i = 0; i < frameCount; i++) {
            int byteCount = in.read();
            
            byte[] value = new byte[byteCount];
            
            in.read(value);
            
            String country = new String(value);
            
            byteCount = in.read();
            
            value = new byte[byteCount];
            
            in.read(value);
            
            String channelID = new String(value);
            
            int dayCount = in.read();
            
            ChannelFrame frame = new ChannelFrame(country, channelID, dayCount);
            
            for(int day = 0; day < dayCount; day++) {
              int[] values = new int[((EPGfreeSummary)summary).getLevels()];
              
              for(int j = 0; j < values.length; j++) {
                values[j] = in.read();
              }
              
              frame.add(day, values);
            }
            
            ((EPGfreeSummary)summary).addChannelFrame(frame);
          }
        }
        else if(summary instanceof EPGdonateSummary) {
          GZIPInputStream in = new GZIPInputStream(new FileInputStream(path));
          
          ((EPGdonateSummary)summary).load(in);
          
          in.close();
        }
        
        deleteFile(path);
      }
    } catch (Exception e) {}
    
    return summary;
  }
  
  private static final void addArrayToList(ArrayList<String> list, String[] values) {
    if(values != null && list != null) {
      for(String value : values) {
        list.add(value);
      }
    }
  }
  
  private void updateVersionTable(String fileName, int dataVersion, long channelID, long date) {
    long daysSince1970 = date / 24 / 60 / 60000;
    
    ContentValues values = new ContentValues();
    
    values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, channelID);
    values.put(TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970, daysSince1970);
    
    if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[0])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_BASE_VERSION,dataVersion);
    }
    else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[1])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_MORE0016_VERSION,dataVersion);
    }
    else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[2])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_MORE1600_VERSION,dataVersion);
    }
    else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[3])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE0016_VERSION,dataVersion);
    }
    else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[4])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE1600_VERSION,dataVersion);
    }
    
    String where = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channelID + " AND " + TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + " = " + daysSince1970;
        
    Cursor test = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, null, where, null, null);
    
    // update current value
    if(test.getCount() > 0) {
      test.moveToFirst();
      long id = test.getLong(test.getColumnIndex(TvBrowserContentProvider.KEY_ID));
      
      int count = getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, id), values, null, null);
    }
    else {
      Uri inserted = getContentResolver().insert(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, values);
    }
    
    test.close();
  }
  
  private synchronized void addInsert(ContentValues insert) {
    mDataInsertList.add(insert);
    
    if(mDataInsertList.size() > TABLE_OPERATION_MIN_SIZE) {
      insert(mDataInsertList);
    }
  }
  
  private synchronized void addUpdate(ContentProviderOperation update) {
    mDataUpdateList.add(update);
    
    if(mDataUpdateList.size() > TABLE_OPERATION_MIN_SIZE) {
      update(mDataUpdateList);
    }
  }
  
  private synchronized void insert(ArrayList<ContentValues> insertList) {
    if(!insertList.isEmpty()) {
      getContentResolver().bulkInsert(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, insertList.toArray(new ContentValues[insertList.size()]));
      insertList.clear();
    }
  }
  
  private synchronized void update(ArrayList<ContentProviderOperation> updateList) {
    if(!updateList.isEmpty()) {
      try {
        getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateList);
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (OperationApplicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      updateList.clear();
    }
  }
  
  private synchronized void addVersionInsert(ContentValues insert) {
    mVersionInsertList.add(insert);
    
    if(mVersionInsertList.size() > TABLE_OPERATION_MIN_SIZE/10) {
      insertVersion(mVersionInsertList);
    }
  }
  
  private synchronized void addVersionUpdate(ContentProviderOperation update) {
    mVersionUpdateList.add(update);
    
    if(mVersionUpdateList.size() > TABLE_OPERATION_MIN_SIZE/10) {
      update(mVersionUpdateList);
    }
  }
  
  private synchronized void insertVersion(ArrayList<ContentValues> insertList) {
    if(!insertList.isEmpty()) {
      getContentResolver().bulkInsert(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, insertList.toArray(new ContentValues[insertList.size()]));
      insertList.clear();
    }
  }
  
  private class UrlFileHolder {
    private File mDownloadFile;
    private String mDownloadURL;
    
    public UrlFileHolder(File downloadFile, String downloadURL) {
      mDownloadFile = downloadFile;
      mDownloadURL = downloadURL;
    }
    
    public File getDownloadFile() {
      return mDownloadFile;
    }
    
    public short getFrameCount(short currentCount) {
      if(currentCount == 254 && mDownloadURL != null) {
        final String url = mDownloadURL.substring(0, mDownloadURL.indexOf(".prog.gz")) +  "_additional.prog.gz";
        final String fileName = mDownloadFile.getAbsolutePath().substring(0, mDownloadFile.getAbsolutePath().indexOf(".prog.gz"))  +  "_additional.prog.gz";
        
        doLog("Download additional data file from '" + url + "'");
        
        try {
          IOUtils.saveUrl(fileName, url);
          
          doLog("Read frame count from '" + fileName + "'");
          
          BufferedInputStream in = new BufferedInputStream(IOUtils.decompressStream(new FileInputStream(fileName)));
          
          in.read(); // read version
          
          currentCount = (short)(((in.read() & 0xFF) << 8) | (in.read() & 0xFF));
          
          in.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        
        File additional = new File(fileName);
        
        deleteFile(additional);
      }
      
      return currentCount;
    }
  }
  
  private class DataInfo {
    private byte mFileVersion;
    private byte mDataVersion;
    private short mFrameCount;
    
    public DataInfo(byte fileVersion, byte dataVersion, short frameCount) {
      mFileVersion = fileVersion;
      mDataVersion = dataVersion;
      mFrameCount = frameCount;
    }
    
    public byte getFileVersion() {
      return mFileVersion;
    }
    
    public byte getDataVersion() {
      return mDataVersion;
    }
    
    public short getFrameCount() {
      return mFrameCount;
    }
  }
  
  private interface DataHandler {
    public Object[] readValuesFromDataFile(ChannelUpdate update, DataInputStream in, int level) throws IOException;
    
    public DataInfo readDataInfo(ChannelUpdate update, DataInputStream in, UrlFileHolder dataUrlFileHolder) throws IOException;
    
    public void updateVersionTableInternal(ChannelUpdate update);
  }
  
  private class EPGfreeDataHandler implements DataHandler {
    @Override
    public Object[] readValuesFromDataFile(ChannelUpdate update, DataInputStream in, int level)
        throws IOException {
      short id = (short)(in.read() & 0xFF);
      int count = (short)(in.read() & 0xFF);
            
      if(count == 0) {
        byte[] addBytes = new byte[(((in.read() & 0xFF) << 8) | (in.read() & 0xFF))];
        
        in.read(addBytes);
        
        id = (short)((in.read() & 0xFF) + IOUtils.getIntForBytes(addBytes));
        count = (short)(in.read() & 0xFF);
      }
      
      ArrayList<String> columnList = new ArrayList<String>();
      
      switch(level)  {
        case BASE_LEVEL: {
          addArrayToList(columnList,BASE_LEVEL_FIELDS);
          
          if(update.mContainsDescription) {
            addArrayToList(columnList,MORE_LEVEL_FIELDS);
          }
          if(update.mContainsPicture) {
            addArrayToList(columnList,PICTURE_LEVEL_FIELDS);
          }
        }break;
        case MORE_LEVEL: addArrayToList(columnList,MORE_LEVEL_FIELDS);break;
        case PICTURE_LEVEL: addArrayToList(columnList,PICTURE_LEVEL_FIELDS);break;
      }
      
      ContentValues values = update.mContentValueList.get(String.valueOf(id));
      
      boolean isNew = false;
      
      if(values == null) {
        values = new ContentValues();
        update.mContentValueList.put(String.valueOf(id), values);
        isNew = true;
      }
      
      if(!values.containsKey(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID)) {
        values.put(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID, id);
        values.put(TvBrowserContentProvider.DATA_KEY_UNIX_DATE, update.getDate());
        values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
      }
      
      byte[] fieldInfoBuffer = new byte[4];
      
      Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      Calendar cal = Calendar.getInstance(update.getTimeZone());
      
      for(byte field = 0; field < count; field++) {
        in.read(fieldInfoBuffer);
        byte fieldType = fieldInfoBuffer[0];
        
        int dataCount = ((fieldInfoBuffer[1] & 0xFF) << 16) | ((fieldInfoBuffer[2] & 0xFF) << 8) | (fieldInfoBuffer[3] & 0xFF);//((in.read() & 0xFF) << 16) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        
        byte[] data = new byte[dataCount];
        
        in.read(data);
        
        String columnName = null;
                  
        switch(fieldType) {
          case 1: {
                          int startTime = IOUtils.getIntForBytes(data);
                          utc.setTimeInMillis(update.getDate());
                          
                          cal.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
                          cal.set(Calendar.MONTH, utc.get(Calendar.MONTH));
                          cal.set(Calendar.YEAR, utc.get(Calendar.YEAR));
                          
                          cal.set(Calendar.HOUR_OF_DAY, startTime / 60);
                          cal.set(Calendar.MINUTE, startTime % 60);
                          cal.set(Calendar.SECOND, 30);
                          
                          long time = (((long)(cal.getTimeInMillis() / 60000)) * 60000);
                          
                          utc.setTimeInMillis(time);
                          
                          values.put(columnName = TvBrowserContentProvider.DATA_KEY_STARTTIME, time);
                          
                          // Normalize start hour and minute to 2014-12-31 to have the same time base on all occasions
                          utc.setTimeInMillis((((long)(IOUtils.normalizeTime(cal, startTime, 30).getTimeInMillis() / 60000)) * 60000));
                          
                          values.put(TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT, utc.get(Calendar.HOUR_OF_DAY)*60 + utc.get(Calendar.MINUTE));
                          
                          columnList.remove(TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT);
                       }break;
          case 2: {
            int endTime = IOUtils.getIntForBytes(data);
            
            utc.setTimeInMillis(update.getDate());
            
            cal.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.MONTH, utc.get(Calendar.MONTH));
            cal.set(Calendar.YEAR, utc.get(Calendar.YEAR));
            
            cal.set(Calendar.HOUR_OF_DAY, endTime / 60);
            cal.set(Calendar.MINUTE, endTime % 60);
            cal.set(Calendar.SECOND, 30);
            
            Long o = values.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
            
            if(o instanceof Long) {
              if(o > cal.getTimeInMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
              }
            }
            
            long time =  (((long)(cal.getTimeInMillis() / 60000)) * 60000);
            
            utc.setTimeInMillis(time);
            
            values.put(columnName = TvBrowserContentProvider.DATA_KEY_ENDTIME, time);
            
            // Normalize start hour and minute to 2014-12-31 to have the same time base on all occasions
            utc.setTimeInMillis((((long)(IOUtils.normalizeTime(cal, endTime, 30).getTimeInMillis() / 60000)) * 60000));
            
            values.put(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT, utc.get(Calendar.HOUR_OF_DAY)*60 + utc.get(Calendar.MINUTE));
            
            columnList.remove(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT);
         }break;
          case 3: values.put(columnName = TvBrowserContentProvider.DATA_KEY_TITLE, new String(data));break;
          case 4: values.put(columnName = TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL, new String(data));break;
          case 5: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE, new String(data));break;
          case 6: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL, new String(data));break;
          case 7: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION, new String(data));break;
          case 8: values.put(columnName = TvBrowserContentProvider.DATA_KEY_DESCRIPTION, new String(data));break;
          case 0xA: values.put(columnName = TvBrowserContentProvider.DATA_KEY_ACTORS, new String(data));break;
          case 0xB: values.put(columnName = TvBrowserContentProvider.DATA_KEY_REGIE, new String(data));break;
          case 0xC: values.put(columnName = TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO, new String(data));break;
          case 0xD: {
              int categories = IOUtils.getIntForBytes(data);
              
              values.put(columnName = TvBrowserContentProvider.DATA_KEY_CATEGORIES, categories);
              
              for(int i = 0; i < IOUtils.INFO_CATEGORIES_ARRAY.length; i++) {
                values.put(TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY[i], IOUtils.infoSet(categories, IOUtils.INFO_CATEGORIES_ARRAY[i]));
                columnList.remove(TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY[i]);
              }
            }break;
          case 0xE: values.put(columnName = TvBrowserContentProvider.DATA_KEY_AGE_LIMIT, IOUtils.getIntForBytes(data));break;
          case 0xF: values.put(columnName = TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK, new String(data));break;
          case 0x10: values.put(columnName = TvBrowserContentProvider.DATA_KEY_GENRE, new String(data));break;
          case 0x11: values.put(columnName = TvBrowserContentProvider.DATA_KEY_ORIGIN, new String(data));break;
          case 0x12: values.put(columnName = TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME, IOUtils.getIntForBytes(data));break;
          case 0x13: values.put(columnName = TvBrowserContentProvider.DATA_KEY_VPS, IOUtils.getIntForBytes(data));break;
          case 0x14: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SCRIPT, new String(data));break;
          case 0x15: values.put(columnName = TvBrowserContentProvider.DATA_KEY_REPETITION_FROM, new String(data));break;
          case 0x16: values.put(columnName = TvBrowserContentProvider.DATA_KEY_MUSIC, new String(data));break;
          case 0x17: values.put(columnName = TvBrowserContentProvider.DATA_KEY_MODERATION, new String(data));break;
          case 0x18: values.put(columnName = TvBrowserContentProvider.DATA_KEY_YEAR, IOUtils.getIntForBytes(data));break;
          case 0x19: values.put(columnName = TvBrowserContentProvider.DATA_KEY_REPETITION_ON, new String(data));break;
          case 0x1A: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PICTURE, data);break;
          case 0x1B: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT, new String(data));break;
          case 0x1C: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION, new String(data));break;
          case 0x1D: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER, IOUtils.getIntForBytes(data));break;
          case 0x1E: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT, IOUtils.getIntForBytes(data));break;
          case 0x1F: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER, IOUtils.getIntForBytes(data));break;
          case 0x20: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PRODUCER, new String(data));break;
          case 0x21: values.put(columnName = TvBrowserContentProvider.DATA_KEY_CAMERA, new String(data));break;
          case 0x22: values.put(columnName = TvBrowserContentProvider.DATA_KEY_CUT, new String(data));break;
          case 0x23: values.put(columnName = TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS, new String(data));break;
          case 0x24: values.put(columnName = TvBrowserContentProvider.DATA_KEY_RATING, IOUtils.getIntForBytes(data));break;
          case 0x25: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM, new String(data));break;
          case 0x26: values.put(columnName = TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING, new String(data));break;
          case 0x27: values.put(columnName = TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, IOUtils.getIntForBytes(data));break;
          case 0x28: values.put(columnName = TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, new String(data));break;
          case 0x29: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SERIES, new String(data));break;
        }
        
        if(columnName != null) {
          columnList.remove(columnName);
        }
        
        data = null;
      }
      
      if(values.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME) && !values.containsKey(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
        values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, 0);
        values.put(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT, 0);
        values.put(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES, 0);
        
        columnList.remove(TvBrowserContentProvider.DATA_KEY_ENDTIME);
        columnList.remove(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT);
        columnList.remove(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES);
      }
      
      for(String columnName : columnList) {
        if(columnName.equals(TvBrowserContentProvider.DATA_KEY_CATEGORIES) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_VPS) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_YEAR) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_RATING) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR)) {
          values.put(columnName, (Integer)null);
        }
        else if(columnName.equals(TvBrowserContentProvider.DATA_KEY_PICTURE)) {
          values.put(columnName, (byte[])null);
        }
        else if(columnNameFromInfo(columnName)) {
          values.put(columnName, 0);
        }
        else {
          values.put(columnName, (String)null);
        }
      }
      
      return new Object[] {String.valueOf(id),isNew};
    }

    @Override
    public DataInfo readDataInfo(ChannelUpdate update, DataInputStream in, UrlFileHolder dataUrlFileHolder) throws IOException{
      /* EPGfree data
       * fileInfoBuffer[0] contains file version
       * fileInfoBuffer[1] contains data version
       * fileInfoBuffer[2] contains frame count
       */
      byte[] fileInfoBuffer = new byte[3];
      
      in.read(fileInfoBuffer);
      
      return new DataInfo(fileInfoBuffer[0],fileInfoBuffer[1],dataUrlFileHolder.getFrameCount((short)(fileInfoBuffer[2] & 0xFF)));
    }

    @Override
    public void updateVersionTableInternal(ChannelUpdate update) {
      long daysSince1970 = update.getDate() / 24 / 60 / 60000;
      
      ContentValues values = new ContentValues();
      
      values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
      values.put(TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970, daysSince1970);
      
      for(String fileName : update.mVersionMap.keySet()) {
        Byte dataVersion = update.mVersionMap.get(fileName);
        
        if(dataVersion != null) {
          if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[0])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_BASE_VERSION,dataVersion);
          }
          else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[1])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_MORE0016_VERSION,dataVersion);
          }
          else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[2])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_MORE1600_VERSION,dataVersion);
          }
          else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[3])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE0016_VERSION,dataVersion);
          }
          else if(fileName.toLowerCase().contains(SettingConstants.EPG_FREE_LEVEL_NAMES[4])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE1600_VERSION,dataVersion);
          }
        }
      }
      
      int[] versionInfo = mCurrentVersionIDs.get(update.getChannelID() + "_" + daysSince1970);
      
      if(versionInfo == null) {
        addVersionInsert(values);
      }
      else {
        ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, versionInfo[0]));
        opBuilder.withValues(values);
     
        addVersionUpdate(opBuilder.build());
      }
    }
  }
  
  private class EPGdonateDataHandler implements DataHandler {

    @Override
    public Object[] readValuesFromDataFile(ChannelUpdate update,
        DataInputStream in, int level) throws IOException {
      String id = in.readUTF();
      byte count = in.readByte();
            
      ArrayList<String> columnList = new ArrayList<String>();
      
      switch(level)  {
        case BASE_LEVEL: {
          addArrayToList(columnList,BASE_LEVEL_FIELDS);
          
          if(update.mContainsDescription) {
            addArrayToList(columnList,MORE_LEVEL_FIELDS);
          }
          if(update.mContainsPicture) {
            addArrayToList(columnList,PICTURE_LEVEL_FIELDS);
          }
        }break;
        case MORE_LEVEL: addArrayToList(columnList,MORE_LEVEL_FIELDS);break;
        case PICTURE_LEVEL: addArrayToList(columnList,PICTURE_LEVEL_FIELDS);break;
      }
      
      ContentValues values = update.mContentValueList.get(id);
      
      boolean isNew = false;
      
      if(values == null) {
        values = new ContentValues();
        update.mContentValueList.put(id, values);
        isNew = true;
      }
      
      if(!values.containsKey(TvBrowserContentProvider.DATA_KEY_DATE_PROG_STRING_ID)) {
        values.put(TvBrowserContentProvider.DATA_KEY_DATE_PROG_STRING_ID, id);
        values.put(TvBrowserContentProvider.DATA_KEY_UNIX_DATE, update.getDate());
        values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
      }
      
      int startMinutes = 0;
      
      for(byte field = 0; field < count; field++) {
        byte fieldType = (byte)in.read();
        String columnName = null;
        
        switch(fieldType) {
          case 1: {
                    startMinutes = in.readShort();                          
                    long startTime = update.getDate() + (startMinutes * 60000L);
                    
                    values.put(columnName = TvBrowserContentProvider.DATA_KEY_STARTTIME, startTime);
                    values.put(TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT, startMinutes);
                          
                    columnList.remove(TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT);
                  }break;
          case 2: {
                    int endMinutes = in.readShort();
                    long endTime = update.getDate() + (endMinutes * 60000L);
                    
                    if(endMinutes <= startMinutes) {
                      endTime += (1440 * 60000L);
                    }
                    
                    if(endMinutes >= 1440) {
                      endMinutes -= 1440;
                    }
                    
                    values.put(columnName = TvBrowserContentProvider.DATA_KEY_ENDTIME, endTime);
                    
                    values.put(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT, endMinutes);
                    columnList.remove(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT);
                  }break;
          case 3: values.put(columnName = TvBrowserContentProvider.DATA_KEY_TITLE, in.readUTF());break;
          case 4: values.put(columnName = TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL, in.readUTF());break;
          case 5: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE, in.readUTF());break;
          case 6: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL, in.readUTF());break;
          case 7: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION, in.readUTF());break;
          case 8: values.put(columnName = TvBrowserContentProvider.DATA_KEY_DESCRIPTION, in.readUTF());break;
          case 0xA: values.put(columnName = TvBrowserContentProvider.DATA_KEY_ACTORS, in.readUTF());break;
          case 0xB: values.put(columnName = TvBrowserContentProvider.DATA_KEY_REGIE, in.readUTF());break;
          case 0xC: values.put(columnName = TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO, in.readUTF());break;
          case 0xD: {
              int categories = in.readInt();
              
              values.put(columnName = TvBrowserContentProvider.DATA_KEY_CATEGORIES, categories);
              
              for(int i = 0; i < IOUtils.INFO_CATEGORIES_ARRAY.length; i++) {
                values.put(TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY[i], IOUtils.infoSet(categories, IOUtils.INFO_CATEGORIES_ARRAY[i]));
                columnList.remove(TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY[i]);
              }
            }break;
          case 0xE: values.put(columnName = TvBrowserContentProvider.DATA_KEY_AGE_LIMIT, in.readByte());break;
          case 0xF: values.put(columnName = TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK, in.readUTF());break;
          case 0x10: values.put(columnName = TvBrowserContentProvider.DATA_KEY_GENRE, in.readUTF());break;
          case 0x11: values.put(columnName = TvBrowserContentProvider.DATA_KEY_ORIGIN, in.readUTF());break;
          case 0x12: values.put(columnName = TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME, in.readShort());break;
          case 0x13: values.put(columnName = TvBrowserContentProvider.DATA_KEY_VPS, in.readShort());break;
          case 0x14: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SCRIPT, in.readUTF());break;
          case 0x15: values.put(columnName = TvBrowserContentProvider.DATA_KEY_REPETITION_FROM, in.readUTF());break;
          case 0x16: values.put(columnName = TvBrowserContentProvider.DATA_KEY_MUSIC, in.readUTF());break;
          case 0x17: values.put(columnName = TvBrowserContentProvider.DATA_KEY_MODERATION, in.readUTF());break;
          case 0x18: values.put(columnName = TvBrowserContentProvider.DATA_KEY_YEAR, in.readShort());break;
          case 0x19: values.put(columnName = TvBrowserContentProvider.DATA_KEY_REPETITION_ON, in.readUTF());break;
          case 0x1A: {
                        byte[] data = new byte[in.readInt()];
                        in.read(data);
                        values.put(columnName = TvBrowserContentProvider.DATA_KEY_PICTURE, data);
                     }break;
          case 0x1B: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT, in.readUTF());break;
          case 0x1C: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION, in.readUTF());break;
          case 0x1D: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER, in.readInt());break;
          case 0x1E: values.put(columnName = TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT, in.readShort());break;
          case 0x1F: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER, in.readShort());break;
          case 0x20: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PRODUCER, in.readUTF());break;
          case 0x21: values.put(columnName = TvBrowserContentProvider.DATA_KEY_CAMERA, in.readUTF());break;
          case 0x22: values.put(columnName = TvBrowserContentProvider.DATA_KEY_CUT, in.readUTF());break;
          case 0x23: values.put(columnName = TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS, in.readUTF());break;
          case 0x24: values.put(columnName = TvBrowserContentProvider.DATA_KEY_RATING, in.readShort());break;
          case 0x25: values.put(columnName = TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM, in.readUTF());break;
          case 0x26: values.put(columnName = TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING, in.readUTF());break;
          case 0x27: values.put(columnName = TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, in.readShort());break;
          case 0x28: values.put(columnName = TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, in.readUTF());break;
          case 0x29: values.put(columnName = TvBrowserContentProvider.DATA_KEY_SERIES, in.readUTF());break;
        }
        
        if(columnName != null) {
          columnList.remove(columnName);
        }
      }
      
      if(values.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME) && !values.containsKey(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
        values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, 0);
        values.put(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT, 0);
        values.put(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES, 0);
        
        columnList.remove(TvBrowserContentProvider.DATA_KEY_ENDTIME);
        columnList.remove(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT);
        columnList.remove(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES);
      }
      
      for(String columnName : columnList) {
        if(columnName.equals(TvBrowserContentProvider.DATA_KEY_CATEGORIES) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_VPS) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_YEAR) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_RATING) ||
           columnName.equals(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR)) {
          values.put(columnName, (Integer)null);
        }
        else if(columnName.equals(TvBrowserContentProvider.DATA_KEY_PICTURE)) {
          values.put(columnName, (byte[])null);
        }
        else if(columnNameFromInfo(columnName)) {
          values.put(columnName, 0);
        }
        else {
          values.put(columnName, (String)null);
        }
      }
      
      return new Object[] {id,isNew};
    }

    @Override
    public DataInfo readDataInfo(ChannelUpdate update, DataInputStream in, UrlFileHolder urlFileHolder) throws IOException {
      byte fileVersion = in.readByte();
      byte dataVersion = in.readByte();
      
      return new DataInfo(fileVersion,dataVersion,in.readShort());
    }

    @Override
    public void updateVersionTableInternal(ChannelUpdate update) {
      long daysSince1970 = update.getDate() / 24 / 60 / 60000;
      
      ContentValues values = new ContentValues();
      
      values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
      values.put(TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970, daysSince1970);
      
      for(String fileName : update.mVersionMap.keySet()) {
        Byte dataVersion = update.mVersionMap.get(fileName);
        Log.d("info21","ADD VERSION INFO " + fileName + " " + update.getChannelID() + "_" + daysSince1970 + " " + dataVersion.byteValue());
        if(dataVersion != null) {
          if(fileName.toLowerCase().contains(SettingConstants.EPG_DONATE_LEVEL_NAMES[0])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_BASE_VERSION,dataVersion.intValue());
          }
          else if(fileName.toLowerCase().contains(SettingConstants.EPG_DONATE_LEVEL_NAMES[1])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_MORE1600_VERSION,dataVersion.intValue());
          }
          else if(fileName.toLowerCase().contains(SettingConstants.EPG_DONATE_LEVEL_NAMES[2])) {
            values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE1600_VERSION,dataVersion.intValue());
          }
        }
      }
      
      int[] versionInfo = mCurrentVersionIDs.get(update.getChannelID() + "_" + daysSince1970);
      
      Log.d("info21","currentInfo " + versionInfo + " BASE " + values.getAsByte(TvBrowserContentProvider.VERSION_KEY_BASE_VERSION));
      
      if(versionInfo == null) {
        addVersionInsert(values);
      }
      else {
        ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, versionInfo[0]));
        opBuilder.withValues(values);
     
        addVersionUpdate(opBuilder.build());
      }
    }
  }
  
  /**
   * Helper class for data update.
   * Stores url, channel ID, timezone and date of a channel.
   * 
   * @author René Mach
   */
  private class ChannelUpdate {
    private ArrayList<String> mUrlList;
    private long mChannelID;
    private String mTimeZone;
    private long mDate;
    private HashMap<String, ContentValues> mContentValueList; 
    private HashMap<String, Byte> mVersionMap;
    private ArrayList<ContentValues> mInsertValuesList;
    private HashMap<Long, ContentValues> mUpdateValueMap;
    private boolean mContainsPicture;
    private boolean mContainsDescription;
    private DataHandler mDataHandler;
    
    /**
     * 
     * @param dataHandler
     * @param channelID
     * @param timezone
     * @param date Start time in milliseconds since 1970 for UTC 0 o'clock.
     */
    public ChannelUpdate(DataHandler dataHandler, long channelID, String timezone, long date) {
      mDataHandler = dataHandler;
      mChannelID = channelID;
      
      if(timezone.startsWith("GMT+01:00")) {
        mTimeZone = "CET";
      }
      else if(timezone.equals("GMT")) {
        mTimeZone = "WET";
      }
      else {
        mTimeZone = timezone;
      }
      
      mDate = date;
      mUrlList = new ArrayList<String>();
      mContentValueList = new HashMap<String, ContentValues>(0);
      mInsertValuesList = new ArrayList<ContentValues>();
      mVersionMap = new HashMap<String, Byte>();
      mUpdateValueMap = new HashMap<Long, ContentValues>();
      mContainsDescription = false;
      mContainsPicture = false;
    }
    
    public void addURL(String url) {
      mUrlList.add(url);
      
      mContainsDescription = url.contains("_more");
      mContainsPicture = url.contains("_picture");
    }
    
    public long getChannelID() {
      return mChannelID;
    }
    
    public TimeZone getTimeZone() {
      return TimeZone.getTimeZone(mTimeZone);
    }
    
    public long getDate() {
      return mDate;
    }
    
    public boolean toDownload() {
      return !mUrlList.isEmpty();
    }
    
    public int size() {
      return mUrlList.size();
    }
    
    private ArrayList<UrlFileHolder> mDownloadList;
    
    public void addDownloadedFile(File file) {
      if(mDownloadList == null) {
        mDownloadList = new ArrayList<UrlFileHolder>();
      }
      
      mDownloadList.add(new UrlFileHolder(file, null));
    }
    
    public void startUpdate(final NotificationManager notification, final int downloadCount, final UncaughtExceptionHandler handleExc) {
      if(!mDataUpdatePool.isShutdown()) {
        mDataUpdatePool.execute(new Thread("CHANNEL UPDATE HANDLE START UPDATE") {
          @Override
          public void run() {
            setUncaughtExceptionHandler(handleExc);
            
            for(UrlFileHolder updateFile : mDownloadList) {
              handleDownload(updateFile);
            }
            
            handleData();
            
            if(mShowNotification) {
              mCurrentDownloadCount++;
              mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
              notification.notify(NOTIFY_ID, mBuilder.build());
            }
          }
        });
      }
    }
    
    public void download(File path, final NotificationManager notification, final int downloadCount) {
      final ArrayList<UrlFileHolder> downloadList = new ArrayList<UrlFileHolder>();
      
      for(String url : mUrlList) {
        File updateFile = new File(path,url.substring(url.lastIndexOf("/")+1));
        
        try {
          IOUtils.saveUrl(updateFile.getAbsolutePath(), url);
          downloadList.add(new UrlFileHolder(updateFile, url));
        } catch (Exception e) {
          mUnsuccessfulDownloads++;
        }
      }
      
      if(!mDataUpdatePool.isShutdown()) {
        mDataUpdatePool.execute(new Thread("CHANNEL UPDATE HANDLE DOWNLOAD") {
          @Override
          public void run() {
            for(UrlFileHolder updateFile : downloadList) {
              handleDownload(updateFile);
              
              if(mShowNotification) {
                mCurrentDownloadCount++;
                mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
                notification.notify(NOTIFY_ID, mBuilder.build());
              }
            }
            
            handleData();
          }
        });
      }
    }
    
    private void handleData() {
      if(!mInsertValuesList.isEmpty()) {
        Collections.sort(mInsertValuesList, new Comparator<ContentValues>() {
           @Override
           public int compare(ContentValues lhs, ContentValues rhs) {
             if(lhs.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME) && rhs.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
               long lStart = lhs.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
               long rStart = rhs.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
               
               if(lStart < rStart) {
                 return -1;
               }
               else if(lStart > rStart) {
                 return 1;
               }
             }
             
             return 0;
           }
         });
         
         ContentValues toAdd = mInsertValuesList.get(0);
         Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
         Calendar cal = Calendar.getInstance(getTimeZone());
         
         for(int i = 1; i < mInsertValuesList.size()-1; i++) {
           if(toAdd.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
             if(!toAdd.containsKey(TvBrowserContentProvider.DATA_KEY_ENDTIME) || toAdd.getAsLong(TvBrowserContentProvider.DATA_KEY_ENDTIME) == 0) {
               long meStart = toAdd.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
               int j = i + 0;
               
               while(j < mInsertValuesList.size() && meStart == mInsertValuesList.get(j).getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
                 j++;
               }
               
               if(j < mInsertValuesList.size()) {
                 long nextStart = mInsertValuesList.get(j).getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
                 
                 if((nextStart - meStart) >= (12 * 60 * 60000)) {
                   nextStart = meStart + (long)(2.5 * 60 * 60000);
                 }
                 
                 cal.setTimeInMillis(nextStart);
                 
                 int startHour = cal.get(Calendar.HOUR_OF_DAY);
                 int startMinute = cal.get(Calendar.MILLISECOND);
                 
                 // Normalize start hour and minute to 2014-12-31 to have the same time base on all occasions
                 utc.setTimeInMillis((((long)(IOUtils.normalizeTime(cal, startHour, startMinute, 30).getTimeInMillis() / 60000)) * 60000));
                 
                 toAdd.put(TvBrowserContentProvider.DATA_KEY_UTC_END_MINUTE_AFTER_MIDNIGHT, utc.get(Calendar.HOUR_OF_DAY)*60 + utc.get(Calendar.MINUTE));
                 toAdd.put(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES, (int)((nextStart - meStart)/60000));
                 toAdd.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, nextStart);
               }
             }
             else {
               long meStart = toAdd.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
               long meEnd = toAdd.getAsLong(TvBrowserContentProvider.DATA_KEY_ENDTIME);
               
               toAdd.put(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES, (int)((meEnd - meStart)/60000));
             }
           }
           
           toAdd = mInsertValuesList.get(i);
         }
         for(ContentValues values : mInsertValuesList) {
           if(values.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
             addInsert(values);
           }
         }
       }
       
       if(!mUpdateValueMap.isEmpty()) {
         for(Long programID : mUpdateValueMap.keySet()) {
           ContentValues value = mUpdateValueMap.get(programID);
           
           if(value != null) {
             ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, programID));
             opBuilder.withValues(value);
          
             addUpdate(opBuilder.build());
           }
         }
       }
       
       mDataHandler.updateVersionTableInternal(this);
         
       clear();
    }
    
    public void clear() {
      mContentValueList.clear();
      mVersionMap.clear();
      mUpdateValueMap.clear();
      mInsertValuesList.clear();
      
      if(mDownloadList != null) {
        mDownloadList.clear();
        mDownloadList = null;
      }
      
      mContentValueList = null;
      mVersionMap = null;
      mUpdateValueMap = null;
      mInsertValuesList = null;
    }
    
    private void handleDownload(UrlFileHolder dataUrlFileHolder) {
      File dataFile = dataUrlFileHolder.getDownloadFile();
      Log.d("info21", "FILE " + dataFile.getAbsolutePath());
      if(dataFile.isFile()) {
        doLog("Read data from file: " +dataFile.getAbsolutePath());
        try {
          DataInputStream in = null;
          
          try {
            in = new DataInputStream(new BufferedInputStream(IOUtils.decompressStream(new FileInputStream(dataFile))));
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[8192];
            
            int count = 0;
            int readCount = 0;
            
            while((count = in.read(buffer)) > 0) {
              temp.write(buffer, readCount, count);
            }
            
            in.close();
            
            in = new DataInputStream(new ByteArrayInputStream(temp.toByteArray()));
            temp.close();
          }catch(IOException ee) {
           
          }
          
          final DataInfo dataInfo = mDataHandler.readDataInfo(this, in, dataUrlFileHolder);
          
          doLog("Frame count of data file: '" +dataFile.getName() + "': " + dataInfo.getFrameCount() + " CURRENT DATA STATE: " + (mCurrentData != null));
          ArrayList<String> missingFrameIDs = null;
                  
          String key = getChannelID() + "_" + getDate();
          
          Hashtable<String, CurrentDataHolder> current = mCurrentData.get(key);
          
          int level = BASE_LEVEL;
          
          if(dataFile.getName().contains("_more")) {
            level = MORE_LEVEL;
          }
          else if(dataFile.getName().contains("_picture")) {
            level = PICTURE_LEVEL;
          }
          
          doLogData(" LEVEL " + level);
          
          if(current != null && level == BASE_LEVEL) {
            Set<String> keySet = current.keySet();
            
            missingFrameIDs = new ArrayList<String>(keySet.size());
            
            for(String frameID : keySet) {
              missingFrameIDs.add(frameID);
            }
          }
          
          for(int i = 0; i < dataInfo.getFrameCount(); i++) {
            Object[] info = mDataHandler.readValuesFromDataFile(this, in, level);
            
            String frameID = (String)info[0];
            boolean isNew =  (Boolean)info[1];
            
            ContentValues contentValues = mContentValueList.get(frameID);
            
            if(contentValues == null) {
              break;
            }
            
            long programID = -1;
            CurrentDataHolder value = null;
            
            if(current != null) {
              value = current.get(frameID);
              
              if(value != null) {
                programID = value.mProgramID;
              }
            }
            
            if(contentValues.size() > 0) {
              if(missingFrameIDs != null) {
                missingFrameIDs.remove(frameID);
              }
              
              if(programID >= 0) {
                if(level == BASE_LEVEL && mDontWantToSeeValues != null) {
                  String title = contentValues.getAsString(TvBrowserContentProvider.DATA_KEY_TITLE);
                  
                  if(title != null) {
                    if(title.equals(value.mTitle)) {
                      contentValues.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, value.mDontWantToSee ? 1 : 0);
                    }
                    else if(UiUtils.filter(TvDataUpdateService.this, title, mDontWantToSeeValues)) {
                      contentValues.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, 1);
                    }
                  }
                }
                
                // program known update it
                if(isNew) {
                  mUpdateValueMap.put(Long.valueOf(programID), contentValues);
                }
              }
              else if(contentValues.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME) && contentValues.get(TvBrowserContentProvider.DATA_KEY_STARTTIME) != null) {
                // program unknown insert it
                if(level == BASE_LEVEL && mDontWantToSeeValues != null) {
                  String title = contentValues.getAsString(TvBrowserContentProvider.DATA_KEY_TITLE);
                  
                  if(UiUtils.filter(TvDataUpdateService.this, title, mDontWantToSeeValues)) {
                    contentValues.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, 1);
                  }
                }
                
                if(isNew) {
                  mInsertValuesList.add(contentValues);
                }
              }
              else if(level == BASE_LEVEL) {
                // insert but no start time key or start time is null, dismiss
                mContentValueList.remove(frameID);
                mInsertValuesList.remove(contentValues);
              }
            }
          }
          Log.d("info21", "VERSION " + dataFile.getName() + " " + Byte.valueOf(dataInfo.getDataVersion()));
          mVersionMap.put(dataFile.getName(), Byte.valueOf(dataInfo.getDataVersion()));
          
          if(level == BASE_LEVEL && missingFrameIDs != null && !missingFrameIDs.isEmpty()) {
            StringBuilder where = new StringBuilder(" ( ");
            
            where.append(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID);
            where.append(" IN ( ");
            where.append(TextUtils.join(", ", missingFrameIDs));
            where.append(" ) OR ( ");
            where.append(TvBrowserContentProvider.DATA_KEY_DATE_PROG_STRING_ID);
            where.append(" IN ( ");
            where.append(TextUtils.join(", ", missingFrameIDs));            
            where.append(" ) ) ");
            where.append(" AND ");
            where.append(" ( ");
            where.append(TvBrowserContentProvider.DATA_KEY_UNIX_DATE);
            where.append(" = ");
            where.append(getDate());
            where.append(" ) AND ( ");
            where.append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
            where.append(" = ");
            where.append(getChannelID());
            where.append(" ) ");
            Log.d("info66", " DELETE WHERE " + where);
            
            getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, where.toString(), null);
          }
          
          Log.d("info5", "INSERTED");
          in.close();
        } catch (Exception e) {
          StackTraceElement[] elements = e.getStackTrace();
          
          StringBuilder message = new StringBuilder();
          
          for(StackTraceElement el : elements) {
            message.append(el.getFileName()).append(" ").append(el.getLineNumber()).append(" ").append(el.getClassName()).append(" ").append(el.getMethodName()).append("\n");
          }
          
          doLog("Error read data file: '" +dataFile.getAbsolutePath() + "': " + e.getMessage() + " " + message.toString());
          Log.d("info5", "error data update", e);
        }
        
        deleteFile(dataFile);
        
        doLog("Read data DONE from file: " +dataFile.getAbsolutePath());
      }
      else {
        Log.d("info5", "file not available " + dataFile.getPath());
      }
    }
        
    @Override
    public String toString() {
      return "ChannelID: " + mChannelID + " " + new Date(mDate) + " TimeZone: " + mTimeZone;
    }
  }
  
  private boolean columnNameFromInfo(String columnName) {
    for(String name : TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY) {
      if(name.equals(columnName)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Class that stores informations about available data for a channel on an update server.
   * <p>
   * @author René Mach
   */
  @SuppressLint("UseSparseArrays")
  private static class ChannelFrame {
    private String mCountry;
    private String mChannelID;
    private int mDayCount;
    
    private HashMap<Integer,int[]> mLevelVersions;
    
    public ChannelFrame(String country, String channelID, int dayCount) {
      mCountry = country;
      mChannelID = channelID;
      mDayCount = dayCount;
      
      mLevelVersions = new HashMap<Integer, int[]>();
    }
    
    public void add(int day, int[] levelVersions) {
      mLevelVersions.put(day, levelVersions);
    }
    
    public int[] getVersionForDay(int day) {
      return mLevelVersions.get(Integer.valueOf(day));
    }
    
    public int getDayCount() {
      return mDayCount;
    }
    
    public String getCountry() {
      return mCountry;
    }
    
    public String getChannelID() {
      return mChannelID;
    }
  }
  
  private static interface Summary {}
  
  /**
   * Helper class that stores informations about the available data
   * on an update server.
   * 
   * @author René Mach
   */
  private static class EPGfreeSummary implements Summary {
    private long mStartDaySince1970;
    private int mLevels;

    /**
     * List with available ChannelFrames for the server.
     */
    private ArrayList<ChannelFrame> mFrameList;
    
    public EPGfreeSummary() {
      mFrameList = new ArrayList<ChannelFrame>();
    }
    
    public void setStartDaySince1970(long days) {
      mStartDaySince1970 = days-1;
    }
    
    public void setLevels(int levels) {
      mLevels = levels;
    }
    
    public void addChannelFrame(ChannelFrame frame) {
      mFrameList.add(frame);
    }
    
    public int getLevels() {
      return mLevels;
    }
        
    public Calendar getStartDate() {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      
      // calculate the number of miliseconds since 1970 to get to the UNIX time  
      cal.setTimeInMillis(mStartDaySince1970 * 24 * 60 * 60000);
      
      return cal;
    }
    
    /**
     * Get the ChannelFrame for the given channel ID
     * <p>
     * @param channelID The channel ID to get the ChannelFrame for.
     * @return The requested ChannelFrame or <code>null</code> if there is no ChannelFrame for given ID.
     */
    public ChannelFrame getChannelFrame(String channelID) {
      for(ChannelFrame frame : mFrameList) {
        if(frame.mChannelID.equals(channelID)) {
          return frame;
        }
      }
      
      return null;
    }
  }
  
  private static final class EPGdonateSummary extends Properties implements Summary {}
}
