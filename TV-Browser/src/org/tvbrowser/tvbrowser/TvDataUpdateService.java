package org.tvbrowser.tvbrowser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

public class TvDataUpdateService extends Service {
  public static final String TAG = "TV_DATA_UPDATE_SERVICE";
  public static final String DAYS_TO_LOAD = "DAYS_TO_LOAD";
  
  public static final String TYPE = "TYPE";
  public static final int TV_DATA_TYPE = 1;
  public static final int CHANNEL_TYPE = 2;
    
  private boolean updateRunning;
  private ExecutorService mThreadPool;
  private Handler mHandler;
  private int mDayStart;
  private int mDayEnd;
  
  private int mNotifyID = 1;
  private NotificationCompat.Builder mBuilder;
  private int mCurrentDownloadCount;
  private int mDaysToLoad;
    
  private static final Integer[] FRAME_ID_ARR;
  
  private ArrayList<String> mSyncFavorites;
  
  static {
    FRAME_ID_ARR = new Integer[253];
    
    for(int i = 0; i < FRAME_ID_ARR.length; i++) {
      FRAME_ID_ARR[i] = Integer.valueOf(i+2);
    }
  }
    
  @Override
  public void onCreate() {
    super.onCreate();
    
    mDaysToLoad = 2;
    
    mBuilder = new NotificationCompat.Builder(this);
    mBuilder.setSmallIcon(R.drawable.ic_launcher);
    mBuilder.setOngoing(true);
    
    mHandler = new Handler();
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    // TODO Auto-generated method stub
    
    
    new Thread() {
      public void run() {
        setPriority(MIN_PRIORITY);
        
        if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == TV_DATA_TYPE) {
          mDaysToLoad = intent.getIntExtra(DAYS_TO_LOAD, 2);
          
          Calendar cal = Calendar.getInstance();
          cal.set(2014, Calendar.JANUARY, 5);
          
          if(cal.getTimeInMillis() > System.currentTimeMillis()) {
            mDayStart = 0;
            mDayEnd = 24;
          }
          else {
            mDayStart = 19;
            mDayEnd = 23;
          }
          updateTvData();
        }
        else if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == CHANNEL_TYPE) {
          updateChannels();
        }
      }
    }.start();
        
    return Service.START_NOT_STICKY;
  }
  
  private void syncFavorites() {
    if(mSyncFavorites != null) {
      for(String fav : mSyncFavorites) {
        if(fav != null && fav.contains(";") && fav.contains(":")) {
          String[] parts = fav.split(";");
          
          long time = Long.parseLong(parts[0]) * 60000;
          String[] idParts = parts[1].split(":");
        
          if(idParts[0].equals("1")) {
            String dataService = "EPG_FREE";
            
            String where = " ( " +TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = \"" + dataService + "\" ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = \"" + idParts[1] + "\" ) ";
            
            Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
            
            if(group.moveToFirst()) {
              int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
              
              where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = " + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=\'" + idParts[2] + "\' ) ";
              Log.d("date", where);
              
              Cursor channel = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where, null, null);
              
              if(channel.moveToFirst()) {
                int channelId = channel.getInt(channel.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                Log.d("date", "channelid " + channelId + " " + channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
                
                where = " ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channelId + " ) AND ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " = " + time + " ) ";
                
                Log.d("date", where);
      //          where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " = " + time;
                Cursor program = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, where, null, null);
                Log.d("date", String.valueOf(program.getCount()));
                if(program.moveToFirst()) {
                  String current = program.getString(program.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
                  Log.d("date", String.valueOf(current));
                  boolean update = false;
                  
                  if(current == null || current.trim().length() == 0) {
                    current = SettingConstants.MARK_VALUE_SYNC_FAVORITE;
                    update = true;
                  }
                  else if(!current.contains(SettingConstants.MARK_VALUE_SYNC_FAVORITE)) {
                    current += ";" + SettingConstants.MARK_VALUE_SYNC_FAVORITE;
                    update = true;
                  }
                  
                  if(update) {
                    ContentValues values = new ContentValues();
                    values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
                    
                    long programID = program.getLong(program.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                    
                    
                    getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA,programID), values, null, null);
                    
                    Intent indent = new Intent(SettingConstants.MARKINGS_CHANGED);
                    indent.putExtra(SettingConstants.MARKINGS_ID, programID);
      
                    LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(indent);
                    
                    Log.d("date", program.getString(program.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
                  }
                }
                
                program.close();
              }
              channel.close();
            }
            
            group.close();
            Log.d("date", String.valueOf(time));
            Log.d("date", parts[1]);
          }
        }
      }
    }
    
    mSyncFavorites = null;
  }
  
  private void updateChannels() {
    NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    mBuilder.setProgress(100, 0, true);
    mBuilder.setContentTitle(getResources().getText(R.string.channel_notification_title));
    mBuilder.setContentText(getResources().getText(R.string.channel_notification_text));
    notification.notify(mNotifyID, mBuilder.build());
    
    final File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"tvbrowserdata");
    
    if(!path.isDirectory()) {
      path.mkdirs();
    }
    
    new Thread() {
      public void run() {
        File groups = new File(path,"groups.txt");
        
        try {
          IOUtils.saveUrl(groups.getAbsolutePath(), "http://www.tvbrowser.org/listings/groups.txt");
          updateGroups(groups, path);
        } catch (Exception e) {}
      }
    }.start();
  }
  
  private void updateGroups(File groups, final File path) {
    if(groups.isFile()) {
      final ArrayList<GroupInfo> channelMirrors = new ArrayList<GroupInfo>();
      
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(groups)));
        
        ContentResolver cr = getContentResolver();
  
        String line = null;
        
        while((line = in.readLine()) != null) {
          Log.d("infoa", line);
          String[] parts = line.split(";");
          
          // Construct a where clause to make sure we don't already have ths group in the provider.
          String w = TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = '" + SettingConstants.EPG_FREE_KEY + "' AND " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = '" + parts[0] + "'";
          
          // If the group is new, insert it into the provider.
          Cursor query = cr.query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, w, null, null);
          
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID, SettingConstants.EPG_FREE_KEY);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_ID, parts[0]);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME, parts[1]);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER, parts[2]);
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION, parts[3]);
          
          StringBuilder builder = new StringBuilder(parts[4]);
          
          for(int i = 5; i < parts.length; i++) {
            builder.append(";");
            builder.append(parts[i]);
          }
          
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, builder.toString());
          
          
          
          if(query == null || query.getCount() == 0) {
            // The group is not already known, so insert it
            Uri insert = cr.insert(TvBrowserContentProvider.CONTENT_URI_GROUPS, values);
            
            GroupInfo test = loadChannelForGroup(cr.query(insert, null, null, null, null));
            Log.d(TAG, " GROUPINFO " + String.valueOf(test));
            if(test != null) {
              channelMirrors.add(test);
            }
          }
          else {
            cr.update(TvBrowserContentProvider.CONTENT_URI_GROUPS, values, w, null);
            
            Cursor groupTest = cr.query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, w, null, null);
            
            GroupInfo test = loadChannelForGroup(groupTest);
            Log.d(TAG, " GROUPINFO " + String.valueOf(test));
            if(test != null) {
              channelMirrors.add(test);
            }
            
            groupTest.close();
            
          }
          
          query.close();
        }
        
        in.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        Log.d(TAG, "EXCEPTION", e);
      }
      
      Log.d(TAG, String.valueOf(channelMirrors.isEmpty()));
      if(!channelMirrors.isEmpty()) {
        mThreadPool =  Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
                
        for(final GroupInfo info : channelMirrors) {
          mThreadPool.execute(new Thread() {
            public void run() {
              File group = new File(path,info.getUrl().substring(info.getUrl().lastIndexOf("/")+1));
              Log.d("infoa", group.getAbsolutePath());
              try {
                IOUtils.saveUrl(group.getAbsolutePath(), info.getUrl());
                addChannels(group,info);//requestId,keyID,groupId);
              } catch (Exception e) {}
            }
          });
        }
        
        mThreadPool.shutdown();
        try {
          Log.d("info", "await termination for channels");
          mThreadPool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notification.cancel(mNotifyID);

        LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(new Intent(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE));
        stopSelf();
      }
      
      if(!groups.delete()) {
        groups.deleteOnExit();
      }
    }
  }
  
  private synchronized GroupInfo loadChannelForGroup(final Cursor cursor) { 
    int index = cursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS);
    
    if(index >= 0) {
      cursor.moveToFirst();
      
      String temp = cursor.getString(index);
      
      index = cursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      final String groupId = cursor.getString(index);
      
      String[] mirrors = null;
      
      if(temp.contains(";")) {
        mirrors = temp.split(";");
      }
      else {
        mirrors = new String[1];
        mirrors[0] = temp;
      }
      
      int idIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      final int keyID = cursor.getInt(idIndex);
      
      for(String mirror : mirrors) {
        
        if(isConnectedToServer(mirror,5000)) {
          if(!mirror.endsWith("/")) {
            mirror += "/";
          }
          
          return new GroupInfo(mirror+groupId+"_channellist.gz",keyID,groupId);
        }
      }
    }
    
    cursor.close();
    
    return null;
  }
  
  private class GroupInfo {
    private String mUrl;
    private int mUniqueGroupID;
    private String mGroupID;
    
    public GroupInfo(String url, int uniqueGroupID, String groupID) {
      mUrl = url;
      mUniqueGroupID = uniqueGroupID;
      mGroupID = groupID;
    }
    
    public String getUrl() {
      return mUrl;
    }
    
    public int getUniqueGroupID() {
      return mUniqueGroupID;
    }
    
    public String getGroupID() {
      return mGroupID;
    }
  }
  
  // Cursor contains the channel group
  public void addChannels(File group, GroupInfo info) {
    if(group.isFile()) {
      try {
        BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(group)),"ISO-8859-1"));
        
        String line = null;
        
        while((line = read.readLine()) != null) {
          String[] parts = line.split(";");
          
          String baseCountry = parts[0];
          String timeZone = parts[1];
          String channelId = parts[2];
          String name = parts[3];
          String copyright = parts[4];
          String website = parts[5];
          String logoUrl = parts[6];
          int category = Integer.parseInt(parts[7]);
          
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
          
          String allCountries = baseCountry;
          String joinedChannel = "";
          
          if(parts.length > i) {
            allCountries = parts[i++];
          }
          
          if(parts.length > i) {
            joinedChannel = parts[i];
          }
          
          String where = TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = " + info.mUniqueGroupID + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = '" + channelId + "'";
          
          ContentResolver cr = getContentResolver();
          
          Cursor query = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where, null, null);
          
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
          
          if(query == null || query.getCount() == 0) {
            // add channel
            cr.insert(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values);
          }
          else {
            // update channel
            cr.update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, where, null);
          }
          
          query.close();
        }
        read.close();
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      if(!group.delete()) {
        group.deleteOnExit();
      }
    }
  }

  private boolean isConnectedToServer(String url, int timeout) {
    try {
      URL myUrl = new URL(url);
      
      URLConnection connection;
      connection = myUrl.openConnection();
      connection.setConnectTimeout(timeout);
      
      HttpURLConnection httpConnection = (HttpURLConnection)connection;
      
      if(httpConnection != null) {
        int responseCode = httpConnection.getResponseCode();
      
        return responseCode == HttpURLConnection.HTTP_OK;
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return false;
  }
  
  private void loadAccessAndFavoriteSync() {
    try {      
      URL documentUrl = new URL("http://android.tvbrowser.org/hurtzAndroidTvb2.php");
      //URL documentUrl = new URL("http://android.tvbrowser.org/webtest/android-tvb/data/scripts/hurtzAndroidTvb.php");
      URLConnection connection = documentUrl.openConnection();
      
      SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
      
      String car = pref.getString(SettingConstants.USER_NAME, null);
      String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
      
      if(car != null && bicycle != null) {
        String userpass = car.trim() + ":" + bicycle.trim();
        String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
        
        connection.setRequestProperty ("Authorization", basicAuth);
        
        BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),"UTF-8"));
        
        String dateValue = read.readLine();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date sponsoringDate = dateFormat.parse(dateValue.trim());
        Log.d("dateinfo", String.valueOf(sponsoringDate));
        if(sponsoringDate.getTime() < System.currentTimeMillis()) {
          mDayStart = 19;
          mDayEnd = 23;
        }
        else {
          mDayStart = 0;
          mDayEnd = 24;
          
          mSyncFavorites = new ArrayList<String>();
          
          String line = null;
          
          while((line = read.readLine()) != null) {
            mSyncFavorites.add(line);
          }
        }
        
        read.close();
      }
    }catch(Throwable t) {Log.d("dateinfo", "",t);}
  }
  /**
   * Calculate the end times of programs that are missing end time in the data.
   */
  private void calculateMissingEnds() {
    NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    mBuilder.setProgress(100, 0, true);
    mBuilder.setContentText(getResources().getText(R.string.update_notification_calculate));
    notification.notify(mNotifyID, mBuilder.build());
    
    // Only ID, channel ID, start and end time are needed for update, so use only these columns
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
    };
    
    Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, projection, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    Log.d(TAG, "DATA-COUNT " + c.getCount());
    
    // only if there are data update it
    if(c.getCount() > 0) {
      c.moveToFirst();
            
      do {
        long progID = c.getLong(c.getColumnIndex(TvBrowserContentProvider.KEY_ID));
        int channelKey = c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
        long meStart = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
        long end = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
        
        c.moveToNext();
        
        // only if end is not set update it (maybe all should be updated except programs that contains a length value)
        if(end == 0) {
          long nextStart = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          if(c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) == channelKey) {
            if(((nextStart - meStart) >= (12 * 60 * 60000))) {
              nextStart = meStart + (long)(2.5 * 60 * 60000);
            }
            
            ContentValues values = new ContentValues();
            values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, nextStart);
            
            getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, progID), values, null, null);
          }
        }
      }while(!c.isLast());
    }
    
    c.close();
    
    updateRunning = false;
    
    TvBrowserContentProvider.INFORM_FOR_CHANGES = true;
    getApplicationContext().getContentResolver().notifyChange(TvBrowserContentProvider.CONTENT_URI_DATA, null);
    
    updateFavorites();
    syncFavorites();
    
    Intent inform = new Intent(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(inform);
    
    mBuilder.setProgress(0, 0, false);
    notification.cancel(mNotifyID);
    
    // Data update complete inform user
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getApplicationContext(), R.string.update_complete, Toast.LENGTH_LONG).show();
      }
    });
    
    stopSelf();
  }
  
  private void updateFavorites() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    
    Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
        
    for(String favorite : favoritesSet) {
      String[] values = favorite.split(";;");
      
      Favorite fav = new Favorite(values[0], values[1], Boolean.valueOf(values[2]));
      
      Favorite.updateFavoriteMarking(getApplicationContext(), getContentResolver(), fav);
    }
  }
  
  private int getDayStart(String name) {
    if(mDayStart != 0) {
      if(name.contains("_ard_") || name.contains("_zdf_") || name.contains("_bfs_") || name.contains("_hr_") || name.contains("_mdr-sn_") 
      || name.contains("_mdr_") || name.contains("_mdr-th_") || name.contains("_ndr-hh_") || name.contains("_ndr-mv_") || name.contains("_ndr_")
      || name.contains("_ndr_") || name.contains("_ndr-sh_") || name.contains("_rbbberlin_") || name.contains("_rbbbrandenburg_") || name.contains("_swr_")
      || name.contains("_swrrp_") || name.contains("_swrsr_") || name.contains("_wdr_") || name.contains("_orf1_") || name.contains("_sfdrs1_")) {
        return 0;
      }
    }
    return mDayStart;
  }
  
  private int getDayEnd(String name) {
    if(mDayEnd != 24) {
      if(name.contains("_ard_") || name.contains("_zdf_") || name.contains("_bfs_") || name.contains("_hr_") || name.contains("_mdr-sn_") 
      || name.contains("_mdr_") || name.contains("_mdr-th_") || name.contains("_ndr-hh_") || name.contains("_ndr-mv_") || name.contains("_ndr_")
      || name.contains("_ndr_") || name.contains("_ndr-sh_") || name.contains("_rbbberlin_") || name.contains("_rbbbrandenburg_") || name.contains("_swr_")
      || name.contains("_swrrp_") || name.contains("_swrsr_") || name.contains("_wdr_") || name.contains("_orf1_") || name.contains("_sfdrs1_")) {
        return 24;
      }
    }
    return mDayEnd;
  }
  
  private void updateTvData() {
    if(!updateRunning) {
      final File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"tvbrowserdata");
      
      if(!path.isDirectory()) {
        path.mkdirs();
      }
      
      mCurrentDownloadCount = 0;
      mBuilder.setContentTitle(getResources().getText(R.string.update_notification_title));
      mBuilder.setContentText(getResources().getText(R.string.update_notification_text));
      
      final NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
      notification.notify(mNotifyID, mBuilder.build());

      loadAccessAndFavoriteSync();
      
      TvBrowserContentProvider.INFORM_FOR_CHANGES = false;
      updateRunning = true;
      
      ContentResolver cr = getContentResolver();
      
      StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
      where.append(" = 1");
      
      final ArrayList<ChannelUpdate> baseList = new ArrayList<ChannelUpdate>();
      final ArrayList<ChannelUpdate> moreList = new ArrayList<ChannelUpdate>();
      
      ArrayList<String> downloadMirrorList = new ArrayList<String>();
      
      Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where.toString(), null, TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      
      String[] versionColumns = {TvBrowserContentProvider.VERSION_KEY_BASE_VERSION, TvBrowserContentProvider.VERSION_KEY_MORE0016_VERSION, TvBrowserContentProvider.VERSION_KEY_MORE1600_VERSION, TvBrowserContentProvider.VERSION_KEY_PICTURE0016_VERSION, TvBrowserContentProvider.VERSION_KEY_PICTURE1600_VERSION};
      
      if(channelCursor.getCount() > 0) {
        channelCursor.moveToFirst();
        
        int lastGroup = -1;
        Mirror mirror = null;
        String groupId = null;
        Summary summary = null;
        
        do {
          int groupKey = channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
          int channelKey = channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          String timeZone = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE));
          
          if(lastGroup != groupKey) {
            Cursor group = cr.query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupKey), null, null, null, null);
            Log.d("MIRR", " XXX " + String.valueOf(groupKey) + " " + String.valueOf(group.getCount()));
            if(group.getCount() > 0) {
              group.moveToFirst();
              
              groupId = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
              String mirrorURL = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS));
              
              Mirror[] mirrors = Mirror.getMirrorsFor(mirrorURL);
              
              mirror = Mirror.getMirrorToUseForGroup(mirrors, groupId);                
              
              Log.d(TAG, mirrorURL);
              Log.d(TAG, " MIRROR " + mirror + " " + groupId + "_summary.gz");
              
              if(mirror != null) {
                summary = readSummary(mirror.getUrl() + groupId + "_summary.gz");
                downloadMirrorList.add(mirror.getUrl() + groupId + "_mirrorlist.gz");
              }
            }
            
            group.close();
          }
          
          if(summary != null) {
            ChannelFrame frame = summary.getChannelFrame(channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)));
            Log.d(TAG, " CHANNEL FRAME " + String.valueOf(frame) + " " + String.valueOf(channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))));
            if(frame != null) {
              Calendar startDate = summary.getStartDate();
              
              Calendar now = Calendar.getInstance();
              now.add(Calendar.DAY_OF_MONTH, -2);

              Calendar to = Calendar.getInstance();
              to.add(Calendar.DAY_OF_MONTH, mDaysToLoad);

              
              for(int i = 0; i < frame.getDayCount(); i++) {
                startDate.add(Calendar.DAY_OF_YEAR, 1);
                
                if(startDate.compareTo(now) >= 0 && startDate.compareTo(to) <= 0) {
                  int[] version = frame.getVersionForDay(i);
                  // load only base files
                  
                  Log.d(TAG, " VERSION " + version); 
                  if(version != null) {
                    long daysSince1970 = startDate.getTimeInMillis() / 24 / 60 / 60000;
                    
                    String versionWhere = TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + " = " + daysSince1970 + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channelKey;
                    
                    Cursor versions = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, null, versionWhere, null, null);
                    
                    if(versions.getCount() > 0) {
                      versions.moveToFirst();
                    }
                    
                    int maxlevel = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingConstants.LOAD_FULL_DATA, false) ? 2 : 0;
                    
                    for(int level = 0; level <= maxlevel; level++) {
                      int testVersion = 0;
                      
                      if(versions.getCount() > 0) {
                        testVersion = versions.getInt(versions.getColumnIndex(versionColumns[level]));
                      }
                      
                      Log.d("MIRR", testVersion +  " level version " + version[level] + " " + frame.getChannelID() + " " + startDate.getTime() + " " + daysSince1970);
                      
                      if(version[level] > testVersion) {
                        String month = String.valueOf(startDate.get(Calendar.MONTH)+1);
                        String day = String.valueOf(startDate.get(Calendar.DAY_OF_MONTH));
                        
                        if(month.length() == 1) {
                          month = "0" + month;
                        }
                        
                        if(day.length() == 1) {
                          day = "0" + day;
                        }
                        
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
                        dateFile.append(SettingConstants.LEVEL_NAMES[level]);
                        dateFile.append("_full.prog.gz");
                        
                        if(level == 0) {
                          baseList.add(new ChannelUpdate(dateFile.toString(), channelKey, timeZone, startDate.getTimeInMillis()));
                        }
                        else if(level == 1 || level == 2) {
                          moreList.add(new ChannelUpdate(dateFile.toString(), channelKey, timeZone, startDate.getTimeInMillis()));
                        }
                     //   Log.d(TAG, " DOWNLOADS " + dateFile.toString());
                      }
                    }
                    
                    versions.close();
                  }
                }
              }
            }
          }
          
          lastGroup = groupKey;
        }while(channelCursor.moveToNext());
        
      }
      
      channelCursor.close();
      
      final int downloadCount = downloadMirrorList.size() + baseList.size() + moreList.size();
      
      mThreadPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
      Log.d("info", " length " + String.valueOf(baseList.size()));
      
      mBuilder.setProgress(downloadCount, 0, false);
      notification.notify(mNotifyID, mBuilder.build());
      
      for(final String mirror : downloadMirrorList) {
        final File mirrorFile = new File(path,mirror.substring(mirror.lastIndexOf("/")+1));
        
        mThreadPool.execute(new Thread() {
          public void run() {
            try {
              IOUtils.saveUrl(mirrorFile.getAbsolutePath(), mirror);
              updateMirror(mirrorFile);
              mCurrentDownloadCount++;
              mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
              notification.notify(mNotifyID, mBuilder.build());
            } catch (Exception e) {}
          }
        });
      }
      
      for(final ChannelUpdate update : baseList) {
        mThreadPool.execute(new Thread() {
          public void run() {
            File updateFile = new File(path,update.getUrl().substring(update.getUrl().lastIndexOf("/")+1));
            
            try {
              Log.d("testdata", updateFile.getAbsolutePath() + " " + update.getUrl());
              
              IOUtils.saveUrl(updateFile.getAbsolutePath(), update.getUrl());
              updateData(updateFile, update, true);
              mCurrentDownloadCount++;
              mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
              notification.notify(mNotifyID, mBuilder.build());
            } catch (Exception e) {
              Log.d("testdata","",e);
            }
          }
        });
      }
      
      for(final ChannelUpdate update : moreList) {
        mThreadPool.execute(new Thread() {
          public void run() {
            File updateFile = new File(path,update.getUrl().substring(update.getUrl().lastIndexOf("/")+1));
            
            try {
              IOUtils.saveUrl(updateFile.getAbsolutePath(), update.getUrl());
              updateData(updateFile, update, false);
              mCurrentDownloadCount++;
              mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
              notification.notify(mNotifyID, mBuilder.build());
            } catch (Exception e) {}
          }
        });
      }
      
      mThreadPool.shutdown();
      
      try {
        Log.d("info", "await termination");
        int waitTime = mDaysToLoad * 4;
        mThreadPool.awaitTermination(moreList.isEmpty() ? waitTime : 2 * waitTime, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        Log.d("info", " term ", e);
      }
      
      Log.d("info", "calculate missing length");
      mBuilder.setProgress(100, 0, true);
      notification.notify(mNotifyID, mBuilder.build());
      calculateMissingEnds();
    }
  }
  
  private void updateMirror(File mirrorFile) {
    if(mirrorFile.isFile()) {
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(mirrorFile))));
        
        StringBuilder mirrors = new StringBuilder();
        
        String line = null;
        
        while((line = in.readLine()) != null) {
          line = line.replace(";", "#");
          mirrors.append(line);
          mirrors.append(";");
        }
        
        if(mirrors.length() > 0) {
          mirrors.deleteCharAt(mirrors.length()-1);
        }
        
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, mirrors.toString());
        Log.d("MIRR", mirrors.toString() + " " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = " + mirrorFile.getName().substring(0, mirrorFile.getName().lastIndexOf("_")));
        getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_GROUPS, values, TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = \"" + mirrorFile.getName().substring(0, mirrorFile.getName().lastIndexOf("_"))+"\"", null);
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      if(!mirrorFile.delete()) {
        mirrorFile.deleteOnExit();
      }
    }
  }
  
  private Summary readSummary(final String summaryurl) {
    final Summary summary = new Summary();
    Log.d("MIRR", "READ SUMMARY");

    URL url;
    try {
      url = new URL(summaryurl);
      Log.d("MIRR", " HIER " + summaryurl);
      URLConnection connection;
      
      connection = url.openConnection();
      connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
      
      HttpURLConnection httpConnection = (HttpURLConnection)connection;
      if(httpConnection != null) {
      int responseCode = httpConnection.getResponseCode();
      Log.d("MIRR", String.valueOf(responseCode) + " " + String.valueOf(HttpURLConnection.HTTP_OK));
      if(responseCode == HttpURLConnection.HTTP_OK) {
        InputStream in = httpConnection.getInputStream();
        
        Map<String,List<String>>  map = httpConnection.getHeaderFields();
        
        for(String key : map.keySet()) {
          Log.d("MIRR", key + " " + map.get(key));
        }
        
       // if("gzip".equalsIgnoreCase(httpConnection.getHeaderField("Content-Encoding")) || "application/x-gzip".equalsIgnoreCase(httpConnection.getHeaderField("Content-Type"))) {
          try {
            in = new GZIPInputStream(in);
          }catch(IOException e2) {
            Log.d("MIRR", "GZIP", e2);
            // Guess it's not compressed if here
          }
       // }
        
        in = new BufferedInputStream(in);
        
        int version = in.read();
        Log.d("MIRR", "VERSION " + String.valueOf(version));
      //  in.
        
        //read file version
        summary.setVersion(version);
        
        long daysSince1970 = ((in.read() & 0xFF) << 16 ) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        
        summary.setStartDaySince1970(daysSince1970);
        
        summary.setLevels(in.read());
        
        int frameCount = (in.read() & 0xFF << 8) | (in.read() & 0xFF);
        Log.d("MIRR", "daysSince1970 " + String.valueOf(daysSince1970) + " frameCount " + String.valueOf(frameCount));
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
            int[] values = new int[summary.getLevels()];
            
            for(int j = 0; j < values.length; j++) {
              values[j] = in.read();
            }
            
            frame.add(day, values);
          }
          
          summary.addChannelFrame(frame);
        }
        
      }
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      Log.d("MIRR", "SUMMARY", e);
    }
    
    return summary;
  }
  
  private void updateData(File dataFile, ChannelUpdate update, boolean baseLevel) {
    if(dataFile.isFile()) {
      try {
        BufferedInputStream in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(dataFile)));
        
        byte fileVersion = (byte)in.read();
        byte dataVersion = (byte)in.read();
        
        int frameCount = in.read();
        
        ArrayList<Integer> missingFrameIDs = new ArrayList<Integer>(Arrays.asList(FRAME_ID_ARR));
        int maxFrameID = 0;
        
        for(int i = 0; i < frameCount; i++) {
          // ID of this program frame
          byte frameId = (byte)in.read();
          // number of program fields
          byte fieldCount = (byte)in.read();
          
          missingFrameIDs.remove(Integer.valueOf(frameId));
          
          maxFrameID = Math.max(maxFrameID, frameId);
          
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID, frameId);
          values.put(TvBrowserContentProvider.DATA_KEY_UNIX_DATE, update.getDate());
          values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
          
          String where = TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID + " = " + frameId +
              " AND " + TvBrowserContentProvider.DATA_KEY_UNIX_DATE + " = " + update.getDate() +
              " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + update.getChannelID();
          
          for(byte field = 0; field < fieldCount; field++) {
            byte fieldType = (byte)in.read();
            
            int dataCount = ((in.read() & 0xFF) << 16) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            
            byte[] data = new byte[dataCount];
            
            in.read(data);
                      
            switch(fieldType) {
              case 1: {
                              int startTime = IOUtils.getIntForBytes(data);
                              Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                              utc.setTimeInMillis(update.getDate());
                              
                              Calendar cal = Calendar.getInstance(update.getTimeZone());
                              cal.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
                              cal.set(Calendar.MONTH, utc.get(Calendar.MONTH));
                              cal.set(Calendar.YEAR, utc.get(Calendar.YEAR));
                              
                              cal.set(Calendar.HOUR_OF_DAY, startTime / 60);
                              cal.set(Calendar.MINUTE, startTime % 60);
                              cal.set(Calendar.SECOND, 30);
                              
                              long time = (((long)(cal.getTimeInMillis() / 60000)) * 60000);
                              
                              values.put(TvBrowserContentProvider.DATA_KEY_STARTTIME, time);
                           }break;
              case 2: {
                int endTime = IOUtils.getIntForBytes(data);
                
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(update.getDate());
                
                Calendar cal = Calendar.getInstance(update.getTimeZone());
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
                
                values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, time);
             }break;
              case 3: values.put(TvBrowserContentProvider.DATA_KEY_TITLE, new String(data));break;
              case 4: values.put(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL, new String(data));break;
              case 5: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE, new String(data));break;
              case 6: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL, new String(data));break;
              case 7: values.put(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION, new String(data));break;
              case 8: values.put(TvBrowserContentProvider.DATA_KEY_DESCRIPTION, new String(data));break;
              case 0xA: values.put(TvBrowserContentProvider.DATA_KEY_ACTORS, new String(data));break;
              case 0xB: values.put(TvBrowserContentProvider.DATA_KEY_REGIE, new String(data));break;
              case 0xC: values.put(TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO, new String(data));break;
              case 0xD: values.put(TvBrowserContentProvider.DATA_KEY_CATEGORIES, IOUtils.getIntForBytes(data));break;
              case 0xE: values.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT, IOUtils.getIntForBytes(data));break;
              case 0xF: values.put(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK, new String(data));break;
              case 0x10: values.put(TvBrowserContentProvider.DATA_KEY_GENRE, new String(data));break;
              case 0x11: values.put(TvBrowserContentProvider.DATA_KEY_ORIGIN, new String(data));break;
              case 0x12: values.put(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME, IOUtils.getIntForBytes(data));break;
              case 0x13: values.put(TvBrowserContentProvider.DATA_KEY_VPS, IOUtils.getIntForBytes(data));break;
              case 0x14: values.put(TvBrowserContentProvider.DATA_KEY_SCRIPT, new String(data));break;
              case 0x15: values.put(TvBrowserContentProvider.DATA_KEY_REPETITION_FROM, new String(data));break;
              case 0x16: values.put(TvBrowserContentProvider.DATA_KEY_MUSIC, new String(data));break;
              case 0x17: values.put(TvBrowserContentProvider.DATA_KEY_MODERATION, new String(data));break;
              case 0x18: values.put(TvBrowserContentProvider.DATA_KEY_YEAR, IOUtils.getIntForBytes(data));break;
              case 0x19: values.put(TvBrowserContentProvider.DATA_KEY_REPETITION_ON, new String(data));break;
              case 0x1A: values.put(TvBrowserContentProvider.DATA_KEY_PICTURE, data);break;
              case 0x1B: values.put(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT, new String(data));break;
              case 0x1C: values.put(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION, new String(data));break;
              case 0x1D: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER, IOUtils.getIntForBytes(data));break;
              case 0x1E: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT, IOUtils.getIntForBytes(data));break;
              case 0x1F: values.put(TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER, IOUtils.getIntForBytes(data));break;
              case 0x20: values.put(TvBrowserContentProvider.DATA_KEY_PRODUCER, new String(data));break;
              case 0x21: values.put(TvBrowserContentProvider.DATA_KEY_CAMERA, new String(data));break;
              case 0x22: values.put(TvBrowserContentProvider.DATA_KEY_CUT, new String(data));break;
              case 0x23: values.put(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS, new String(data));break;
              case 0x24: values.put(TvBrowserContentProvider.DATA_KEY_RATING, IOUtils.getIntForBytes(data));break;
              case 0x25: values.put(TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM, new String(data));break;
              case 0x26: values.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING, new String(data));break;
              case 0x27: values.put(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, IOUtils.getIntForBytes(data));break;
              case 0x28: values.put(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, new String(data));break;
              case 0x29: values.put(TvBrowserContentProvider.DATA_KEY_SERIES, new String(data));break;
            }
            
            data = null;
          }
          
          if(values.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME) && !values.containsKey(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
            values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, 0);
          }
          
          Cursor test = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, null, where, null, null);
          
          if(test.getCount() > 0) {
            // program known update it
            getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, values, where, null);
          }
          else if(values.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
            long startTime = values.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startTime);
            
            if(cal.get(Calendar.HOUR_OF_DAY) >= getDayStart(dataFile.getName()) && (cal.get(Calendar.HOUR_OF_DAY) < getDayEnd(dataFile.getName()) || (cal.get(Calendar.HOUR_OF_DAY) == getDayEnd(dataFile.getName()) && cal.get(Calendar.MINUTE) == 0))) {
              getContentResolver().insert(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, values);            
            }
          }
          
          values.clear();
          values = null;
          
          test.close();
        }
        
        updateVersionTable(update,dataVersion);
        
        if(baseLevel) {
          StringBuilder where = new StringBuilder();
          
          for(Integer id : missingFrameIDs) {
            if(id.intValue() > maxFrameID) {
              break;
            }
            else {
              if(where.length() > 0) {
                where.append(" OR ");
              }
              else {
                where.append(" ( ");
              }
              
              where.append(" ( ");
              where.append(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID);
              where.append(" = ");
              where.append(id);
              where.append(" ) ");
            }
          }
          
          if(where.length() > 0) {
            where.append(" ) AND ");
            where.append(" ( ");
            where.append(TvBrowserContentProvider.DATA_KEY_UNIX_DATE);
            where.append(" = ");
            where.append(update.getDate());
            where.append(" ) AND ( ");
            where.append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
            where.append(" = ");
            where.append(update.getChannelID());
            where.append(" ) ");
                        
            int count = getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, where.toString(), null);
            Log.d(TAG, " Number of deleted programs " + count + " " + update.getUrl());
          }
        }
        
        in.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        Log.d("info", "UPDATE_DATA", e);
      }
      
      if(!dataFile.delete()) {
        dataFile.deleteOnExit();
      }
    }
  }
  
  private void updateVersionTable(ChannelUpdate update, int dataVersion) {
    long daysSince1970 = update.getDate() / 24 / 60 / 60000;
    
    ContentValues values = new ContentValues();
    
    values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
    values.put(TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970, daysSince1970);
    
    if(update.getUrl().toLowerCase().contains(SettingConstants.LEVEL_NAMES[0])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_BASE_VERSION,dataVersion);
    }
    else if(update.getUrl().toLowerCase().contains(SettingConstants.LEVEL_NAMES[1])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_MORE0016_VERSION,dataVersion);
    }
    else if(update.getUrl().toLowerCase().contains(SettingConstants.LEVEL_NAMES[2])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_MORE1600_VERSION,dataVersion);
    }    
    
    String where = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + update.getChannelID() + " AND " + TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + " = " + daysSince1970;
    
    Log.d("DOWN", daysSince1970 + " " + update.getUrl() + " " + dataVersion);
    Log.d(TAG, " Version update where: "+where);
    Log.d(TAG, " Version update content count "+values.size());
    
    Cursor test = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, null, where, null, null);
    
    // update current value
    if(test.getCount() > 0) {
      test.moveToFirst();
      long id = test.getLong(test.getColumnIndex(TvBrowserContentProvider.KEY_ID));
      
      int count = getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, id), values, null, null);
      Log.d(TAG, " Number of updated versions " + count);
    }
    else {
      Uri inserted = getContentResolver().insert(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, values);
      Log.d(TAG, " Inserted version " + inserted);
    }
    
    test.close();
  }
  
  /**
   * Helper class for data update.
   * Stores url, channel ID, timezone and date of a channel.
   * 
   * @author René Mach
   */
  private class ChannelUpdate {
    private String mUrl;
    private long mChannelID;
    private String mTimeZone;
    private long mDate;
    
    public ChannelUpdate(String url, long channelID, String timezone, long date) {
      mUrl = url;
      mChannelID = channelID;
      mTimeZone = timezone;
      mDate = date;
    }
    
    public String getUrl() {
      return mUrl;
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
  
  /**
   * Helper class that stores informations about the available data
   * on an update server.
   * 
   * @author René Mach
   */
  private static class Summary {
    private int mVersion;
    private long mStartDaySince1970;
    private int mLevels;

    /**
     * List with available ChannelFrames for the server.
     */
    private ArrayList<ChannelFrame> mFrameList;
    
    public Summary() {
      mFrameList = new ArrayList<ChannelFrame>();
    }
    
    public void setVersion(int version) {
      mVersion = version;
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
    
    public ChannelFrame[] getChannelFrames() {
      return mFrameList.toArray(new ChannelFrame[mFrameList.size()]);
    }
    
    public int getLevels() {
      return mLevels;
    }
    
    public long getStartDaySince1970() {
      return mStartDaySince1970;
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
     // Log.d(TAG, "CHANNELID " + channelID + " " +mFrameList.size());
      for(ChannelFrame frame : mFrameList) {
      //  Log.d(TAG, " FRAME ID " + frame.mChannelID);
        if(frame.mChannelID.equals(channelID)) {
          return frame;
        }
      }
      
      return null;
    }
  }
}
