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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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
    
  public static boolean IS_RUNNING = false;
  private ExecutorService mThreadPool;
  private Handler mHandler;
  
  private int mNotifyID = 1;
  private NotificationCompat.Builder mBuilder;
  private int mCurrentDownloadCount;
  private int mUnsuccessfulDownloads;
  private int mDaysToLoad;
  
  private Hashtable<String, Hashtable<Byte, CurrentDataHolder>> mCurrentData;
  
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
        
        Logging.openLogForDataUpdate(getApplicationContext());
        
        if(intent.getIntExtra(TYPE, TV_DATA_TYPE) == TV_DATA_TYPE) {
          mDaysToLoad = intent.getIntExtra(getResources().getString(R.string.DAYS_TO_DOWNLOAD), Integer.parseInt(getResources().getString(R.string.days_to_download_default)));
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
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      
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
              
              where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " IS " + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=\'" + idParts[2] + "\' ) ";
              
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
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (OperationApplicationException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    
    mSyncFavorites = null;
  }
  
  private void updateChannels() {
    if(!IS_RUNNING) {
      IS_RUNNING = true;
    
      NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
      mBuilder.setProgress(100, 0, true);
      mBuilder.setContentTitle(getResources().getText(R.string.channel_notification_title));
      mBuilder.setContentText(getResources().getText(R.string.channel_notification_text));
      notification.notify(mNotifyID, mBuilder.build());
      
      File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
      
      if(!parent.isDirectory()) {
        parent = getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
      }
      
      final File path = new File(parent,"tvbrowserdata");
      File nomedia = new File(path,".nomedia");
      
      if(!path.isDirectory()) {
        path.mkdirs();
      }
      
      if(!nomedia.isFile()) {
        try {
          nomedia.createNewFile();
        } catch (IOException e) {}
      }
      
      new Thread() {
        public void run() {
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
              
              stopSelf();
            }
          }
        }
      }.start();
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
      
      if(isConnectedToServer(test, 5000)) {
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
  
  private void updateGroups(File groups, final File path) {
    if(groups.isFile()) {
      final ChangeableFinalBoolean success = new ChangeableFinalBoolean(false);
      
      final ArrayList<GroupInfo> channelMirrors = new ArrayList<GroupInfo>();
      
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(groups)));
        
        ContentResolver cr = getContentResolver();
  
        String line = null;
        doLog("Read groups from: " + groups.getName());
        
        while((line = in.readLine()) != null) {
          doLog("GROUP LINE: " + line);
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
          doLog("Mirrors for group '" + parts[0] + "': " + builder.toString());
          values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, builder.toString());
          
          if(query == null || query.getCount() == 0) {
            doLog("Insert group '" + parts[0] + "' into database.");
            // The group is not already known, so insert it
            Uri insert = cr.insert(TvBrowserContentProvider.CONTENT_URI_GROUPS, values);
            
            Cursor groupTest = cr.query(insert, null, null, null, null);
            
            GroupInfo test = loadChannelForGroup(groupTest);
            
            if(!groupTest.isClosed()) {
              groupTest.close();
            }
            
            if(test != null) {
              doLog("Load channels for group '" + parts[0] + "' to " + test.getFileName());
              channelMirrors.add(test);
            }
          }
          else {
            doLog("Update group '" + parts[0] + "' in database.");
            cr.update(TvBrowserContentProvider.CONTENT_URI_GROUPS, values, w, null);
            
            Cursor groupTest = cr.query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, w, null, null);
            
            GroupInfo test = loadChannelForGroup(groupTest);
            
            if(test != null) {
              doLog("Load channels for group '" + parts[0] + "' to " + test.getFileName());
              channelMirrors.add(test);
            }
            
            groupTest.close();
            
          }
          
          query.close();
        }
        
        in.close();
      } catch (IOException e) {}
      
      if(!channelMirrors.isEmpty()) {
        success.setBoolean(true);
        
        mThreadPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
                
        for(final GroupInfo info : channelMirrors) {
          mThreadPool.execute(new Thread() {
            public void run() {
              File group = new File(path,info.getFileName());
              
              boolean groupSucces = false;
              
              for(String url : info.getUrls()) {
                try {
                  IOUtils.saveUrl(group.getAbsolutePath(), url + info.getFileName());
                  groupSucces = addChannels(group,info);
                  
                  if(groupSucces) {
                    doLog("Load channels for group '" + info.getUniqueGroupID() + "' successfull from: " + url);
                    break;
                  }
                  else {
                    doLog("Not successfull load channels for group '" + info.getUniqueGroupID() + "' from: " + url);
                  }
                } catch (Exception e) {}
              }
              
              success.andUpdateBoolean(groupSucces);
            }
          });
        }
        
        mThreadPool.shutdown();
        
        try {
          mThreadPool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {}
        
        NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notification.cancel(mNotifyID);

        Intent updateDone = new Intent(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE);
        updateDone.putExtra(SettingConstants.CHANNEL_DOWNLOAD_SUCCESSFULLY, success.getBoolean());
        
        LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(updateDone);
      }
      
      if(!groups.delete()) {
        groups.deleteOnExit();
      }
    }
    else {    
      Intent updateDone = new Intent(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE);
      updateDone.putExtra(SettingConstants.CHANNEL_DOWNLOAD_SUCCESSFULLY, false);
      
      LocalBroadcastManager.getInstance(TvDataUpdateService.this).sendBroadcast(updateDone);
    }
    
    IS_RUNNING = false;
    stopSelf();
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
      
      ArrayList<String> mirrorList = new ArrayList<String>();
      
      for(String mirror : mirrors) {
        if(isConnectedToServer(mirror,5000)) {
          if(!mirror.endsWith("/")) {
            mirror += "/";
          }
          
          mirrorList.add(mirror);
        }
      }
      
      if(!mirrorList.isEmpty()) {
        return new GroupInfo(mirrorList.toArray(new String[mirrorList.size()]),groupId+"_channellist.gz",keyID);
      }
    }
    
    cursor.close();
    
    return null;
  }
  
  private class GroupInfo {
    private String[] mUrlArr;
    private int mUniqueGroupID;
    private String mFileName;
    
    public GroupInfo(String[] urls, String fileName, int uniqueGroupID) {
      mUrlArr = urls;
      mUniqueGroupID = uniqueGroupID;
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
  }
  
  // Cursor contains the channel group
  public boolean addChannels(File group, GroupInfo info) {
    boolean returnValue = false;
    
    if(group.isFile()) {
      try {
        BufferedReader read = new BufferedReader(new InputStreamReader(IOUtils.decompressStream(new FileInputStream(group)),"ISO-8859-1"));
        
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
          
          if(logoUrl != null && logoUrl.length() > 0) {
            try {
              byte[] blob = IOUtils.loadUrl(logoUrl);
              
              values.put(TvBrowserContentProvider.CHANNEL_KEY_LOGO, blob);
            }catch(Exception e1) {}
          }
          
          if(query == null || query.getCount() == 0) {
            // add channel
            if(cr.insert(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values) != null) {
              returnValue = true;
            }
          }
          else {
            // update channel
            if(cr.update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, where, null) > 0) {
              returnValue = true;
            }
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
    
    return returnValue;
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
  
  private byte[] getXmlBytes(boolean syncFav, boolean syncMarkings, boolean syncCalendar) {
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
    if(syncCalendar) {
      if(where.length() > 0) {
        where.append(" OR ");
      }
      
      where.append(" ( ").append(TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR).append(" ) ");
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
          
          dat.append(startTime).append(";").append(info.mDataServiceID).append(":").append(info.mGroupID).append(":").append(baseCountry).append(":").append(channelID).append("\n");
        }
      }
    }
    
    programs.close();
          
    return IOUtils.getCompressedData(dat.toString().getBytes());
  }
  
  public void backSyncPrograms() {
    final String CrLf = "\r\n";
    
    SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
    
    String car = pref.getString(SettingConstants.USER_NAME, null);
    String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
    
    boolean syncFav = PrefUtils.getBooleanValue(R.string.PREF_SYNC_FAV_TO_DESKTOP, R.bool.pref_sync_fav_to_desktop_default);
    boolean syncMarkings = PrefUtils.getBooleanValue(R.string.PREF_SYNC_MARKED_TO_DESKTOP, R.bool.pref_sync_marked_to_desktop_default);
    boolean syncCalendar = PrefUtils.getBooleanValue(R.string.PREF_SYNC_CALENDAR_TO_DESKTOP, R.bool.pref_sync_calendar_to_desktop_default);
    
    if((syncFav || syncMarkings || syncCalendar) && car != null && bicycle != null && car.trim().length() > 0 && bicycle.trim().length() > 0) {
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
          Log.d("info8", basicAuth);
          String postData = "";
          
          byte[] xmlData = getXmlBytes(syncFav, syncMarkings, syncCalendar);
          
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
  private void calculateMissingEnds(NotificationManager notification) {
    try {
      mBuilder.setProgress(100, 0, true);
      mBuilder.setContentText(getResources().getText(R.string.update_notification_calculate));
      notification.notify(mNotifyID, mBuilder.build());
      
      // Only ID, channel ID, start and end time are needed for update, so use only these columns
      String[] projection = {
          TvBrowserContentProvider.KEY_ID,
          TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
          TvBrowserContentProvider.DATA_KEY_STARTTIME,
          TvBrowserContentProvider.DATA_KEY_ENDTIME,
          TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME
      };
      
      String[] channelProjection = {
          TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE
      };
      
      Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, projection, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      
      // only if there are data update it
      if(c.getCount() > 0) {
        c.moveToFirst();
        
        int nettoColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME);
        
        TimeZone timeZone = null;
        
        int keyIDColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
        int channelKeyColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
        int startTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        int endTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
        
        do {
          long progID = c.getLong(keyIDColumn);
          int channelKey = c.getInt(channelKeyColumn);
          long meStart = c.getLong(startTimeColumn);
          long end = c.getLong(endTimeColumn);
          long nettoPlayTime = 0;
          
          if(timeZone == null) {
            Cursor channel = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelProjection, null, null, null);
            
            if(channel.moveToFirst()) {
              timeZone = TimeZone.getTimeZone(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE)));
            }
            
            channel.close();
          }
          
          if(c.isNull(nettoColumn)) {
            nettoPlayTime = c.getLong(nettoColumn) * 60000;
          }
          
          c.moveToNext();
          
          long nextStart = c.getLong(startTimeColumn);
          
          if(c.getInt(channelKeyColumn) == channelKey) {
            boolean lastProgram = false;
            
            if(timeZone != null) {
              Calendar meTest = Calendar.getInstance(timeZone);
              meTest.setTimeInMillis(meStart);
              
              Calendar nextTest = Calendar.getInstance(timeZone);
              nextTest.setTimeInMillis(nextStart);
              
              lastProgram = meTest.get(Calendar.DAY_OF_YEAR) + 1 == nextTest.get(Calendar.DAY_OF_YEAR);
            }
            
            // if end not set or netto play time larger than next start or next time not end time
            if(end == 0 || (nettoPlayTime > (nextStart - meStart)) || (lastProgram && end != nextStart && ((nextStart - meStart) < (3 * 60 * 60000)))) {
              if(nettoPlayTime > (nextStart - meStart)) {
                nextStart = meStart + nettoPlayTime;
              }
              else if((nextStart - meStart) >= (12 * 60 * 60000)) {
                nextStart = meStart + (long)(2.5 * 60 * 60000);
              }
              
              ContentValues values = new ContentValues();
              values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, nextStart);
              
              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, progID));
              opBuilder.withValues(values);
              
              updateValuesList.add(opBuilder.build());
            }
          }
          else {
            timeZone = null;
          }
        }while(!c.isLast());
      }
      
      c.close();
      
      if(!updateValuesList.isEmpty()) {
        getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
      }
    }catch(Throwable t) {}
    
    finishUpdate(notification);
  }
    
  private void finishUpdate(NotificationManager notification) {
    TvBrowserContentProvider.INFORM_FOR_CHANGES = true;
    getApplicationContext().getContentResolver().notifyChange(TvBrowserContentProvider.CONTENT_URI_DATA, null);
  
    updateFavorites();
    syncFavorites();
    
    Intent inform = new Intent(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(inform);
    
    mDontWantToSeeValues = null;
    
    mBuilder.setProgress(0, 0, false);
    notification.cancel(mNotifyID);
    
    // Data update complete inform user
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getApplicationContext(), R.string.update_complete, Toast.LENGTH_LONG).show();
      }
    });
    
    Log.d("info1", "Unsuccessful downloads: " + String.valueOf(mUnsuccessfulDownloads));
    
    Logging.closeLogForDataUpdate();
    
    Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
    edit.putLong(getString(R.string.LAST_DATA_UPDATE), System.currentTimeMillis());
    edit.commit();
    
    IS_RUNNING = false;
    stopSelf();
  }
  
  private void updateFavorites() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    
    Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
        
    for(String favorite : favoritesSet) {
      String[] values = favorite.split(";;");
      
      boolean remind = false;
      
      if(values.length > 3) {
        remind = Boolean.valueOf(values[3]);
      }
      
      Favorite fav = new Favorite(values[0], values[1], Boolean.valueOf(values[2]), remind);
      
      Favorite.updateFavoriteMarking(getApplicationContext(), getContentResolver(), fav);
    }
    
    backSyncPrograms();
  }
  
  public void doLog(String value) {
    Logging.log(null, value, Logging.DATA_UPDATE_TYPE, getApplicationContext());
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
      
      if(!groups.delete()) {
        groups.deleteOnExit();
      }
    }
    
    return mirrorLine;
  }
  
  private void updateTvData() {
    if(!IS_RUNNING) {
      mUnsuccessfulDownloads = 0;
      IS_RUNNING = true;
      
      File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
      
      if(!parent.isDirectory()) {
        parent = getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
      }
      
      final File path = new File(parent,"tvbrowserdata");
      
      File nomedia = new File(path,".nomedia");
      
      if(!path.isDirectory()) {
        path.mkdirs();
      }
      
      if(!nomedia.isFile()) {
        try {
          nomedia.createNewFile();
        } catch (IOException e) {}
      }
      
      File[] oldDataFiles = path.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().toLowerCase().endsWith(".gz");
        }
      });
      
      for(File oldFile : oldDataFiles) {
        if(!oldFile.delete()) {
          oldFile.deleteOnExit();
        }
      }
      
      int[] levels = null;
      
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
      
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
      
      final NotificationManager notification = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
      notification.notify(mNotifyID, mBuilder.build());

      loadAccessAndFavoriteSync();
      
      TvBrowserContentProvider.INFORM_FOR_CHANGES = false;
      
      ContentResolver cr = getContentResolver();
      
      StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
      where.append(" = 1");
      
      final ArrayList<ChannelUpdate> baseList = new ArrayList<ChannelUpdate>();
      final ArrayList<ChannelUpdate> moreList = new ArrayList<ChannelUpdate>();
      final ArrayList<ChannelUpdate> pictureList = new ArrayList<ChannelUpdate>();
      
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
          doLog("Load info for channel '" + channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
          
          if(lastGroup != groupKey) {
            summary = null;
            mirror = null;
            groupId = null;
            
            Cursor group = cr.query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupKey), null, null, null, null);
            doLog("Cursor size for groupKey '" + group.getCount());
            if(group.getCount() > 0) {
              group.moveToFirst();
              
              groupId = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
              String mirrorURL = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS));

              doLog("Available mirrorURLs for group '" + groupId + "': " + mirrorURL);
              doLog("Group info for '" + groupId + "'  groupKey: " + groupKey + " group name: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME)) + " group provider: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER)) + " group description: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION)));
              
              if(!mirrorURL.contains("http://") && !mirrorURL.contains("https://")) {
                doLog("RELOAD MIRRORS FOR '" + groupId);
                mirrorURL = reloadMirrors(groupId, path);
                
                doLog("Available mirrorURLs for group '" + groupId + "': " + mirrorURL);
                doLog("Group info for '" + groupId + "'  groupKey: " + groupKey + " group name: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME)) + " group provider: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER)) + " group description: " + group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION)));
              }
              
              Mirror[] mirrors = Mirror.getMirrorsFor(mirrorURL);
              
              mirror = Mirror.getMirrorToUseForGroup(mirrors, groupId, this);                
              doLog("Choosen mirror for group '" + groupId + "': " + mirror);
              if(mirror != null) {
                doLog("Donwload summary from: " + mirror.getUrl() + groupId + "_summary.gz");
                summary = readSummary(new File(path,groupId + "_summary.gz"), mirror.getUrl() + groupId + "_summary.gz");
                doLog("To download: " + mirror.getUrl() + groupId + "_mirrorlist.gz");
                downloadMirrorList.add(mirror.getUrl() + groupId + "_mirrorlist.gz");
              }
            }
            
            group.close();
          }
          
          doLog("Summary downloaded: " + (summary != null));
          
          if(summary != null && mirror != null) {
            String channelID = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
            doLog("Load summary info for: " + channelID);
            ChannelFrame frame = summary.getChannelFrame(channelID);
            doLog("Summary frame with ID '" + channelID + "' read: " + (frame != null));
            if(frame != null) {
              Calendar startDate = summary.getStartDate();
              
              Calendar testDate = Calendar.getInstance();
              testDate.setTimeInMillis(startDate.getTimeInMillis());
              testDate.set(Calendar.HOUR_OF_DAY, 0);
              
              doLog("Start date of data for frame with ID '" + channelID + "': " + startDate.getTime());
              Calendar now = Calendar.getInstance();
              now.add(Calendar.DAY_OF_MONTH, -2);

              Calendar to = Calendar.getInstance();
              to.add(Calendar.DAY_OF_MONTH, mDaysToLoad);
              doLog("End date of data to download for frame with ID '" + channelID + "': " + to.getTime());
              
              for(int i = 0; i < frame.getDayCount(); i++) {
                startDate.add(Calendar.DAY_OF_YEAR, 1);
                testDate.add(Calendar.DAY_OF_YEAR, 1);
                
                if(testDate.compareTo(now) >= 0 && testDate.compareTo(to) <= 0) {
                  int[] version = frame.getVersionForDay(i);
                  doLog("Version found for frame with ID '" + channelID + "': " + (version != null));
                  if(version != null) {
                    long daysSince1970 = startDate.getTimeInMillis() / 24 / 60 / 60000;
                    
                    String versionWhere = TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + " = " + daysSince1970 + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channelKey;
                    
                    Cursor versions = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, null, versionWhere, null, null);
                    
                    if(versions.getCount() > 0) {
                      versions.moveToFirst();
                    }
                    
                    for(int level : levels) {
                      int testVersion = 0;
                      
                      if(versions.getCount() > 0) {
                        testVersion = versions.getInt(versions.getColumnIndex(versionColumns[level]));
                      }
                      doLog("Currently known version for '" + channelID + "' and days since 1970 '" + daysSince1970 + "': " + testVersion);
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
                        dateFile.append(SettingConstants.LEVEL_NAMES[level]);
                        dateFile.append("_full.prog.gz");
                        
                        doLog("Download data for '" + channelID + "' from " + dateFile.toString() + " for level: " + level);
                        
                        if(level == 0) {
                          baseList.add(new ChannelUpdate(dateFile.toString(), channelKey, timeZone, startDate.getTimeInMillis()));
                        }
                        else if(level == 1 || level == 2) {
                          moreList.add(new ChannelUpdate(dateFile.toString(), channelKey, timeZone, startDate.getTimeInMillis()));
                        }
                        else if(level == 3 || level == 4) {
                          pictureList.add(new ChannelUpdate(dateFile.toString(), channelKey, timeZone, startDate.getTimeInMillis()));
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
      
      
      
      final int downloadCount = downloadMirrorList.size() + baseList.size() + moreList.size() + pictureList.size();
      doLog("Data files to load " + downloadCount);
      mThreadPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
      
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
            } catch (Exception e) {
              mUnsuccessfulDownloads++;
            }
          }
        });
      }
      
      readCurrentData();
      
      Log.d("info5", "updateCount " + baseList.size() + " moreCount " + moreList.size() + " pictureCount " + pictureList.size());
      
      for(final ChannelUpdate update : baseList) {
        mThreadPool.execute(new Thread() {
          public void run() {
            File updateFile = new File(path,update.getUrl().substring(update.getUrl().lastIndexOf("/")+1));
            doLog("Download from: " + update.getUrl() + " to: " +  updateFile.getAbsolutePath());
            try {
              IOUtils.saveUrl(updateFile.getAbsolutePath(), update.getUrl());
              doLog("Data file saved from: " + update.getUrl() + " to: " +  updateFile.getAbsolutePath() + ": " + updateFile.isFile());
              updateData(updateFile, update, true);
              mCurrentDownloadCount++;
              mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
              notification.notify(mNotifyID, mBuilder.build());
            } catch (Exception e) {
              mUnsuccessfulDownloads++;
            }
          }
        });
      }
      
      mThreadPool.shutdown();
      
      try {
        mThreadPool.awaitTermination(baseList.size(), TimeUnit.MINUTES);
      } catch (InterruptedException e) {}
      
      mThreadPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
      readCurrentData();
      
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
            } catch (Exception e) {
              mUnsuccessfulDownloads++;
            }
          }
        });
      }
      
      for(final ChannelUpdate update : pictureList) {
        mThreadPool.execute(new Thread() {
          public void run() {
            File updateFile = new File(path,update.getUrl().substring(update.getUrl().lastIndexOf("/")+1));
            
            try {
              IOUtils.saveUrl(updateFile.getAbsolutePath(), update.getUrl());
              updateData(updateFile, update, false);
              mCurrentDownloadCount++;
              mBuilder.setProgress(downloadCount, mCurrentDownloadCount, false);
              notification.notify(mNotifyID, mBuilder.build());
            } catch (Exception e) {
              mUnsuccessfulDownloads++;
            }
          }
        });
      }
      
      mThreadPool.shutdown();
      
      try {
        mThreadPool.awaitTermination(moreList.size() + pictureList.size(), TimeUnit.MINUTES);
      } catch (InterruptedException e) {}
      
      mBuilder.setProgress(100, 0, true);
      notification.notify(mNotifyID, mBuilder.build());
      
      if(baseList.size() > 0) {
        calculateMissingEnds(notification);
      }
      else {
        finishUpdate(notification);
      }
    }
  }
  
  private static final class CurrentDataHolder {
    long mProgramID;
    boolean mDontWantToSee;
    String mTitle;
  }
  
  private void readCurrentData() {
    if(mCurrentData != null) {
      mCurrentData.clear();
    }
    else {
      mCurrentData = new Hashtable<String, Hashtable<Byte,CurrentDataHolder>>();
    }
    
    String[] projection = {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, TvBrowserContentProvider.DATA_KEY_UNIX_DATE, TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID, TvBrowserContentProvider.DATA_KEY_TITLE, TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE};
    
    Cursor data = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + ", " + TvBrowserContentProvider.DATA_KEY_UNIX_DATE + ", " + TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID);
    
    Hashtable<Byte, CurrentDataHolder> current = null;
    String currentKey = null;
    
    int keyColumn = data.getColumnIndex(TvBrowserContentProvider.KEY_ID);
    int frameIDColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID);
    int channelColumn = data.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    int unixDateColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_UNIX_DATE);
    int titleColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
    int dontWantToSeeColumn = data.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE);
    
    if(data.getCount() > 0) {
      try {
        while(!data.isClosed() && data.moveToNext()) {
          long programKey = data.getInt(keyColumn);
          byte frameID = (byte)data.getInt(frameIDColumn);
          
          int channelID = data.getInt(channelColumn);
          long unixDate = data.getLong(unixDateColumn);
          
          String testKey = channelID + "_" + unixDate;
          
          if(currentKey == null || !currentKey.equals(testKey)) {
            currentKey = testKey;
            current = mCurrentData.get(testKey);
            
            if(current == null) {
              current = new Hashtable<Byte, CurrentDataHolder>();
              
              mCurrentData.put(currentKey, current);
            }
          }
          
          CurrentDataHolder holder = new CurrentDataHolder();
          
          holder.mProgramID = programKey;
          holder.mTitle = data.getString(titleColumn);
          holder.mDontWantToSee = data.getInt(dontWantToSeeColumn) == 1;
          
          current.put(Byte.valueOf(frameID), holder);
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
  
  private Summary readSummary(final File path, final String summaryurl) {
    final Summary summary = new Summary();
    
    try {
      IOUtils.saveUrl(path.getAbsolutePath(), summaryurl);
      
      if(path.isFile()) {
        BufferedInputStream in = new BufferedInputStream(IOUtils.decompressStream(new FileInputStream(path)));
        
        int version = in.read();
        
        summary.setVersion(version);
        
        long daysSince1970 = ((in.read() & 0xFF) << 16 ) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        
        summary.setStartDaySince1970(daysSince1970);
        
        summary.setLevels(in.read());
        
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
            int[] values = new int[summary.getLevels()];
            
            for(int j = 0; j < values.length; j++) {
              values[j] = in.read();
            }
            
            frame.add(day, values);
          }
          
          summary.addChannelFrame(frame);
        }
        
        if(!path.delete()) {
          path.deleteOnExit();
        }
      }
    } catch (Exception e) {}
    
    return summary;
  }
    
  private byte readValuesFromDataFile(ContentValues values, BufferedInputStream in, ChannelUpdate update) throws IOException {
    // ID of this program frame
    byte frameId = (byte)in.read();
    // number of program fields
    byte fieldCount = (byte)in.read();
    
    if(!values.containsKey(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID)) {
      values.put(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID, frameId);
      values.put(TvBrowserContentProvider.DATA_KEY_UNIX_DATE, update.getDate());
      values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
    }
    
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
    
    return frameId;
  }
  
  private void updateData(File dataFile, ChannelUpdate update, boolean baseLevel) {
    ArrayList<ContentValues> insertValueList = new ArrayList<ContentValues>();
    ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
    
    if(dataFile.isFile()) {
      doLog("Read data from file: " +dataFile.getAbsolutePath());
      try {
        BufferedInputStream in = null;
        
        try {
          in = new BufferedInputStream(IOUtils.decompressStream(new FileInputStream(dataFile)));
        }catch(IOException ee) {
         
        }
        
        byte fileVersion = (byte)in.read();
        byte dataVersion = (byte)in.read();
        
        int frameCount = in.read();
        Log.d("info5","Frame count of data file: '" +dataFile.getName() + "': " + frameCount + " " + new Date(update.getDate()));
        doLog("Frame count of data file: '" +dataFile.getName() + "': " + frameCount);
        ArrayList<Byte> missingFrameIDs = null;
                
        String key = update.getChannelID() + "_" + update.getDate();
        
        Hashtable<Byte, CurrentDataHolder> current = mCurrentData.get(key);
        
        if(current != null && baseLevel) {
          Set<Byte> keySet = current.keySet();
          
          missingFrameIDs = new ArrayList<Byte>(keySet.size());
          
          for(Byte frameID : keySet) {
            missingFrameIDs.add(frameID);
          }
        }
        
        for(int i = 0; i < frameCount; i++) {
          ContentValues values = new ContentValues();
          
          byte frameID = readValuesFromDataFile(values, in, update);
          
          long programID = -1;
          CurrentDataHolder value = null;
          
          if(current != null) {
            value = current.get(Byte.valueOf(frameID));
            
            if(value != null) {
              programID = value.mProgramID;
            }
          }
          
          if(values.size() > 0) {
            if(missingFrameIDs != null) {
              missingFrameIDs.remove(Byte.valueOf(frameID));
            }
            
            if(programID >= 0) {
              
              
              if(baseLevel && mDontWantToSeeValues != null) {
                String title = values.containsKey(TvBrowserContentProvider.DATA_KEY_TITLE) ? values.getAsString(TvBrowserContentProvider.DATA_KEY_TITLE) : null;
                
                if(title != null) {
                  if(title.equals(value.mTitle)) {
                    values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, value.mDontWantToSee ? 1 : 0);
                  }
                  else if(UiUtils.filter(getApplicationContext(), title, mDontWantToSeeValues)) {
                    values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, 1);
                  }
                }
              }
              
              // program known update it
              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, programID));
              opBuilder.withValues(values);
              
              updateValuesList.add(opBuilder.build());
            }
            else if(values.containsKey(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
              // program unknown insert it
              if(baseLevel && mDontWantToSeeValues != null) {
                String title = values.containsKey(TvBrowserContentProvider.DATA_KEY_TITLE) ? values.getAsString(TvBrowserContentProvider.DATA_KEY_TITLE) : null;
                
                if(title != null && UiUtils.filter(getApplicationContext(), title, mDontWantToSeeValues)) {
                  values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, 1);
                }
              }
              
              insertValueList.add(values);
            }
          }
        }
        
        Log.d("info5", "Size of insertValueList for '" + dataFile.getName() + "' " + insertValueList.size());
        
        if(baseLevel && !insertValueList.isEmpty()) {
          Collections.sort(insertValueList, new Comparator<ContentValues>() {
            @Override
            public int compare(ContentValues lhs, ContentValues rhs) {
              long lStart = lhs.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
              long rStart = rhs.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
              
              if(lStart < rStart) {
                return -1;
              }
              else if(lStart > rStart) {
                return 1;
              }
              
              return 0;
            }
          });
          
          ContentValues toAdd = insertValueList.get(0);
          
          for(int i = 1; i < insertValueList.size()-1; i++) {
            if(toAdd.getAsLong(TvBrowserContentProvider.DATA_KEY_ENDTIME) == 0) {
              long meStart = toAdd.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
              int j = i + 0;
              
              while(meStart == insertValueList.get(j).getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
                j++;
              }
              
              if(j < insertValueList.size()) {
                long nextStart = insertValueList.get(j).getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
                
                if((nextStart - meStart) >= (12 * 60 * 60000)) {
                  nextStart = meStart + (long)(2.5 * 60 * 60000);
                }
                
                toAdd.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, nextStart);
              }
            }
            
            toAdd = insertValueList.get(i);
          }
          
          getContentResolver().bulkInsert(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, insertValueList.toArray(new ContentValues[insertValueList.size()]));
        }
        
        if(!updateValuesList.isEmpty()) {
          getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
        }
        
        insertValueList.clear();
        insertValueList = null;
        
        updateVersionTable(update,dataVersion);
        
        if(baseLevel && missingFrameIDs != null) {
          StringBuilder where = new StringBuilder();
          
          for(Byte id : missingFrameIDs) {
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
        
          if(where.toString().trim().length() > 0) {
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
            Log.d("info5", " DELETE WHERE " + where);
            getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, where.toString(), null);
          }
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
      
      if(!dataFile.delete()) {
        dataFile.deleteOnExit();
      }
    }
    else {
      Log.d("info5", "file not available " + dataFile.getPath());
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
    else if(update.getUrl().toLowerCase().contains(SettingConstants.LEVEL_NAMES[3])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE0016_VERSION,dataVersion);
    }
    else if(update.getUrl().toLowerCase().contains(SettingConstants.LEVEL_NAMES[4])) {
      values.put(TvBrowserContentProvider.VERSION_KEY_PICTURE1600_VERSION,dataVersion);
    }
    
    String where = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + update.getChannelID() + " AND " + TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + " = " + daysSince1970;
        
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
