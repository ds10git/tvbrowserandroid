/*
 * TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.devplugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Logging;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;

/**
 * A class that handles TV-Browser Plugins.
 * 
 * @author René Mach
 */
public final class PluginHandler {
  public static final String PLUGIN_ACTION = "org.tvbrowser.intent.action.PLUGIN";
  private static ArrayList<PluginServiceConnection> PLUGIN_LIST;
  
  private static PluginManager PLUGIN_MANAGER;
  
  //public static final long PROGRAM_IDS_ALREADY_HANDLED_ID = -2;
  //private static long PROGRAM_ID_FIRST = PROGRAM_IDS_ALREADY_HANDLED_ID;
  //private static long PROGRAM_ID_LAST = PROGRAM_IDS_ALREADY_HANDLED_ID;
  
  private static final AtomicInteger BLOG_COUNT = new AtomicInteger(0);
  
  public static final boolean pluginsAvailable() {
    return PLUGIN_LIST != null && !PLUGIN_LIST.isEmpty();
  }
  
  private static PluginManager createPluginManager(final Context context) {
    return new PluginManager.Stub() {
      @Override
      public List<Channel> getSubscribedChannels() throws RemoteException {
        return IOUtils.getChannelList(context);
      }
      
      @Override
      public Program getProgramWithId(long programId) throws RemoteException {
        Program result = null;
        
        final long token = Binder.clearCallingIdentity();
        Cursor programs = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL,programId), ProgramUtils.DATA_CHANNEL_PROJECTION, null, null, null);
        
        try {
          result = ProgramUtils.createProgramFromDataCursor(context, programs);
        }finally {
          IOUtils.closeCursor(programs);
          Binder.restoreCallingIdentity(token);
        }
        
        return result;
      }
      
      @Override
      public Program getProgramForChannelAndTime(int channelId, long startTimeInUTC) throws RemoteException {
        Program result = null;
        
        String where = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + "=" + channelId + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "=" + startTimeInUTC;
        
        final long token = Binder.clearCallingIdentity();
        Cursor programs = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, ProgramUtils.DATA_CHANNEL_PROJECTION, where, null, null);
        
        try {
          result = ProgramUtils.createProgramFromDataCursor(context, programs);
        }finally {
          IOUtils.closeCursor(programs);
          Binder.restoreCallingIdentity(token);
        }
        
        return result;
      }

      @Override
      public TvBrowserSettings getTvBrowserSettings() throws RemoteException {
        String version = "Unknown";
        int versionCode = -1;

        try {
          PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
          version = pInfo.versionName;
          versionCode = pInfo.versionCode;
        } catch (NameNotFoundException e) {}
        
        return new TvBrowserSettings(SettingConstants.IS_DARK_THEME, version, versionCode, PrefUtils.getLongValueWithDefaultKey(R.string.META_DATA_ID_FIRST_KNOWN, R.integer.meta_data_id_default), PrefUtils.getLongValueWithDefaultKey(R.string.META_DATA_ID_LAST_KNOWN, R.integer.meta_data_id_default), PrefUtils.getLongValue(R.string.PREF_LAST_KNOWN_DATA_DATE, SettingConstants.DATA_LAST_DATE_NO_DATA));
      }

      @Override
      public boolean markProgram(Program program) throws RemoteException {
        return program != null ? markProgramWithIcon(program, null) : false;
      }

      @Override
      public boolean unmarkProgram(Program program) throws RemoteException {
        return program != null ? unmarkProgramWithIcon(program, null) : false;
      }

      @Override
      public boolean markProgramWithIcon(Program program, String pluginCanonicalClassName) throws RemoteException {
        return program != null ? ProgramUtils.markProgram(context, program, pluginCanonicalClassName) : false;
      }

      @Override
      public boolean unmarkProgramWithIcon(Program program, String pluginCanonicalClassName) throws RemoteException {
        return program != null ? ProgramUtils.unmarkProgram(context, program, pluginCanonicalClassName) : false;
      }

      @Override
      public Program[] getProgramsForChannelInRange(int channelId, long startTimeInUTC, long endTimeInUTC) throws RemoteException {
        Program[] result = null;
        StringBuilder where = new StringBuilder();
        
        where.append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID).append(" IS ").append(channelId);
        where.append(" AND ");
        where.append(TvBrowserContentProvider.DATA_KEY_STARTTIME).append(">=").append(startTimeInUTC);
        where.append(" AND ");
        where.append(TvBrowserContentProvider.DATA_KEY_ENDTIME).append("<=").append(endTimeInUTC);
        
        final long token = Binder.clearCallingIdentity();
        
        Cursor programs = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, ProgramUtils.DATA_CHANNEL_PROJECTION, null, null, null);
        
        try {
          result = ProgramUtils.createProgramsFromDataCursor(context, programs);
        }finally {
          IOUtils.closeCursor(programs);
          Binder.restoreCallingIdentity(token);
        }
        
        return result;
      }

      @Override
      public void setRatingForProgram(Program program, int rating) throws RemoteException {
        // TODO Create rating function.
      }
    };
  }
  
  public static PluginManager getPluginManagerCreateIfNecessary(Context context) {
    if(PLUGIN_MANAGER == null) {
      return createPluginManager(context);
    }
    
    return PLUGIN_MANAGER;
  }
  /*
  public static final void loadFirstAndLastProgramId(Context context) {
    PrefUtils.initialize(context);
    long lastInfo = PrefUtils.getLongValue(org.tvbrowser.tvbrowser.R.string.PLUGIN_LAST_ID_INFO_DATE, 0);
    
    Calendar test = Calendar.getInstance();
    test.add(Calendar.DAY_OF_YEAR, -1);
    test.set(Calendar.HOUR_OF_DAY, 0);
    test.set(Calendar.MINUTE, 0);
    test.set(Calendar.SECOND, 0);
    test.set(Calendar.MILLISECOND, 0);
    
    PROGRAM_ID_LAST = PROGRAM_ID_FIRST = PROGRAM_IDS_ALREADY_HANDLED_ID;
    
    if(lastInfo != test.getTimeInMillis()) {
      Cursor firstProgram = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_TITLE}, TvBrowserContentProvider.DATA_KEY_STARTTIME +">="+test.getTimeInMillis(), null, TvBrowserContentProvider.KEY_ID + " LIMIT 1");
      long firstStart = 0;
      String firstTitle = "";
      try {
        if(firstProgram.moveToFirst()) {
          PROGRAM_ID_FIRST = firstProgram.getLong(firstProgram.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          firstStart = firstProgram.getLong(firstProgram.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          firstTitle = firstProgram.getString(firstProgram.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
        }
        else {
          PROGRAM_ID_FIRST = -1;
        }
      }finally {
        IOUtils.closeCursor(firstProgram);
      }
      
      if(PROGRAM_ID_FIRST != -1 && PROGRAM_ID_FIRST != PROGRAM_IDS_ALREADY_HANDLED_ID) {
        Cursor lastProgram = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME}, TvBrowserContentProvider.DATA_KEY_STARTTIME +">="+test.getTimeInMillis(), null, TvBrowserContentProvider.KEY_ID + " DESC LIMIT 1");
        
        try {
          if(lastProgram.moveToFirst()) {
            PROGRAM_ID_LAST = lastProgram.getLong(lastProgram.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          }
          else {
            PROGRAM_ID_LAST = -1;
          }
        }finally {
          IOUtils.closeCursor(lastProgram);
        }
      }
      
      Log.d("info2", "FIRST KNOWN ID: " + PROGRAM_ID_FIRST + new Date(firstStart) + " " + firstTitle + " LAST KNOWN ID " + " " + PROGRAM_ID_LAST);
      
      Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
      edit.putLong(context.getString(R.string.PLUGIN_LAST_ID_INFO_DATE), test.getTimeInMillis());
      edit.commit();
    }
  }*/
  
  private static void doLog(Context context, String message) {
    Logging.log(null, message, Logging.TYPE_PLUGIN, context);
  }
  
  public static final void loadPlugins(Context context1, Handler handler) {
    try {
      doLog(context1, "loadPlugins");
      
      final Context context = context1.getApplicationContext();
      
      PLUGIN_MANAGER = createPluginManager(context);
      
      if(PLUGIN_LIST == null) {
        PLUGIN_LIST = new ArrayList<PluginServiceConnection>();
        
       // loadFirstAndLastProgramId(context);
        ProgramUtils.handleFirstAndLastKnownProgramId(context, PrefUtils.getLongValueWithDefaultKey(R.string.META_DATA_ID_FIRST_KNOWN, R.integer.meta_data_id_default), PrefUtils.getLongValueWithDefaultKey(R.string.META_DATA_ID_LAST_KNOWN, R.integer.meta_data_id_default));
        
        PackageManager packageManager = context.getPackageManager();
        Intent baseIntent = new Intent( PluginHandler.PLUGIN_ACTION );
        baseIntent.setFlags( Intent.FLAG_DEBUG_LOG_RESOLUTION );
        List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER );
        
        for( int i = 0 ; i < list.size() ; ++i ) {
          ResolveInfo info = list.get( i );
          ServiceInfo sinfo = info.serviceInfo;
          
          if(sinfo != null) {
            PluginServiceConnection plugin = new PluginServiceConnection(sinfo.packageName, sinfo.name, context);
            
            if(plugin.bindPlugin(context, null)) {
              PLUGIN_LIST.add(plugin);
            }
          }
        }
        
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            if(PLUGIN_LIST != null) {
              Collections.sort(PLUGIN_LIST);
              PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putInt(context.getString(R.string.PLUGIN_LAST_ID_INFO_DATE), Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).commit();
            }
          }
        }, 2000);
      }
      
      incrementBlogCount();
      doLog(context1, "Plugin reference count " + BLOG_COUNT.get());
    }catch(Throwable t) {
      
    }
  }
  
  public static PluginServiceConnection[] onlyLoadAndGetPlugins(Context context, Handler handler) {
    final ArrayList<PluginServiceConnection> pluginList = new ArrayList<PluginServiceConnection>();
    
    PackageManager packageManager = context.getPackageManager();
    Intent baseIntent = new Intent( PluginHandler.PLUGIN_ACTION );
    baseIntent.setFlags( Intent.FLAG_DEBUG_LOG_RESOLUTION );
    List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER );
    
    for( int i = 0 ; i < list.size() ; ++i ) {
      ResolveInfo info = list.get( i );
      ServiceInfo sinfo = info.serviceInfo;
      
      if(sinfo != null) {
        PluginServiceConnection plugin = new PluginServiceConnection(sinfo.packageName, sinfo.name, context);
        
        if(plugin.bindPlugin(context, null)) {
          pluginList.add(plugin);
        }
      }
    }
    
    handler.postDelayed(new Runnable() {
      
      @Override
      public void run() {
        if(pluginList != null) {
          Collections.sort(pluginList);
        }        
      }
    }, 2000);
    
    return pluginList.toArray(new PluginServiceConnection[pluginList.size()]);
  }
  
  public static final void shutdownPlugins(Context context) {
    doLog(context, "shutdownPlugins: reference count " + BLOG_COUNT.get());
    if(BLOG_COUNT.get() == 1) {
      doLog(context, "do Plugin shutdown");
      if(PLUGIN_LIST != null) {
        context = context.getApplicationContext();
        
        for(PluginServiceConnection plugin : PLUGIN_LIST) {
          if(plugin.isActivated()) {
            plugin.callOnDeactivation();
          }
          
          plugin.unbindPlugin(context);
        }
      
        PLUGIN_LIST.clear();
        PLUGIN_LIST = null;
      }
      
      PLUGIN_MANAGER = null;
      
      PluginHandler.PLUGIN_LIST = null;
      
      decrementBlogCount();
    }
  }
  
  public static PluginManager getPluginManager() {
    return PLUGIN_MANAGER;
  }
  
  public static boolean isMarkedByPlugins(long programId) {
    boolean result = false;
    
    if(PLUGIN_LIST != null) {
      for(PluginServiceConnection connection : PLUGIN_LIST) {
        try {
          if(connection.isActivated() && connection.getPlugin().isMarked(programId)) {
            result = true;
            break;
          }
        } catch (RemoteException e) {}
      }
    }
    
    return result;
  }
  
  public static boolean firstAndLastProgramIdAlreadyHandled() {
    return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == PrefUtils.getIntValue(R.string.PLUGIN_LAST_ID_INFO_DATE, 0);
  }
  
  public static PluginServiceConnection getConnectionForId(String id) {
    PluginServiceConnection result = null;
    
    if(PLUGIN_LIST != null) {
      for(PluginServiceConnection connection : PLUGIN_LIST) {
        if(connection.getId().equals(id)) {
          result = connection;
          break;
        }
      }
    }
    
    return result;
  }
  
  public static boolean hasPlugins() {
    return PLUGIN_LIST != null && !PLUGIN_LIST.isEmpty();
  }
  
  public static PluginServiceConnection[] getAvailablePlugins() {
    PluginServiceConnection[] result = null;
    
    if(hasPlugins()) {
      result = PLUGIN_LIST.toArray(new PluginServiceConnection[PLUGIN_LIST.size()]);
    }
    
    return result;
  }
  
  static void removePluginServiceConnection(PluginServiceConnection connection) {
    if(PluginHandler.PLUGIN_LIST != null) {
      PluginHandler.PLUGIN_LIST.remove(connection);
    }
  }
  
  public static void incrementBlogCount() {
    BLOG_COUNT.incrementAndGet();
  }
  
  public static void decrementBlogCount() {
    BLOG_COUNT.decrementAndGet();
  }
}
