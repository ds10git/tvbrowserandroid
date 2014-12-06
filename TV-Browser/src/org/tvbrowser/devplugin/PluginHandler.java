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
import java.util.Iterator;
import java.util.List;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.IOUtils;
import org.tvbrowser.tvbrowser.ProgramUtils;
import org.tvbrowser.tvbrowser.R;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A class that handles TV-Browser Plugins.
 * 
 * @author René Mach
 */
public final class PluginHandler {
  public static final String PLUGIN_ACTION = "org.tvbrowser.intent.action.PLUGIN";
  public static ArrayList<PluginServiceConnection> PLUGIN_LIST;
  
  private static PluginManager PLUGIN_MANAGER;
  
  public static final long FIRST_PROGRAM_ALREADY_HANDLED_ID = -2;
  private static long FIRST_PROGRAM_ID = FIRST_PROGRAM_ALREADY_HANDLED_ID;
  
  public static final boolean pluginsAvailable() {
    return PLUGIN_LIST != null && !PLUGIN_LIST.isEmpty();
  }
  
  private static void createPluginManager(final Context context) {
    PLUGIN_MANAGER = new PluginManager.Stub() {
      @Override
      public List<Channel> getSubscribedChannels() throws RemoteException {
        ArrayList<Channel> channelList = new ArrayList<Channel>();
        
        final long token = Binder.clearCallingIdentity();
        Cursor channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.KEY_ID);
        
        try {
          if(channels != null) {
            channels.moveToPosition(-1);
            
            int keyColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
            int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
            int iconColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
            
            while(channels.moveToNext()) {
              channelList.add(new Channel(channels.getInt(keyColumn), channels.getString(nameColumn), channels.getBlob(iconColumn)));
            }
          }
        }finally {
          IOUtils.closeCursor(channels);
          Binder.restoreCallingIdentity(token);
        }
        
        return channelList;
      }
      
      @Override
      public Program getProgramWithId(long programId) throws RemoteException {
        Program result = null;
        Log.d("info44", "TRY TO LOAD " + programId);
        final long token = Binder.clearCallingIdentity();
        Cursor programs = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL,programId), ProgramUtils.DATA_CHANNEL_PROJECTION, null, null, null);
        Log.d("info44", "CURSOR " + programs.getCount());
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
        Log.d("info44", "CURSOR " + programs.getCount());
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

        try {
          PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
          version = pInfo.versionName;
        } catch (NameNotFoundException e) {}
        
        return new TvBrowserSettings(SettingConstants.IS_DARK_THEME, version);
      }

      @Override
      public boolean markProgram(Program program) throws RemoteException {
        Log.d("info44", "MARK " + program);
        return program != null ? ProgramUtils.markProgram(context, program) : false;
      }

      @Override
      public boolean unmarkProgram(Program program) throws RemoteException {
        Log.d("info44", "PLUGIN_HANDLER_UNMARK " + program);
        return program != null ? ProgramUtils.unmarkProgram(context, program) : false;
      }
    };
  }
  
  public static final void loadPlugins(Context context, Handler handler) {
    createPluginManager(context.getApplicationContext());
    
    if(PLUGIN_LIST == null) {
      PrefUtils.initialize(context);
      long lastInfo = PrefUtils.getLongValue(org.tvbrowser.tvbrowser.R.string.PLUGIN_LAST_ID_INFO_DATE, 0);
      
      Calendar test = Calendar.getInstance();
      test.add(Calendar.DAY_OF_YEAR, -1);
      test.set(Calendar.HOUR_OF_DAY, 0);
      test.set(Calendar.MINUTE, 0);
      test.set(Calendar.SECOND, 0);
      test.set(Calendar.MILLISECOND, 0);
      
      FIRST_PROGRAM_ID = FIRST_PROGRAM_ALREADY_HANDLED_ID;
      
      if(lastInfo != test.getTimeInMillis()) {
        Cursor firstProgram = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.DATA_KEY_STARTTIME +">="+test.getTimeInMillis(), null, TvBrowserContentProvider.KEY_ID + " LIMIT 1");
        
        try {
          if(firstProgram.moveToFirst()) {
            FIRST_PROGRAM_ID = firstProgram.getLong(firstProgram.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          }
          else {
            FIRST_PROGRAM_ID = -1;
          }
        }finally {
          IOUtils.closeCursor(firstProgram);
        }
        
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.PLUGIN_LAST_ID_INFO_DATE), test.getTimeInMillis());
        edit.commit();
      }
      
      PLUGIN_LIST = new ArrayList<PluginServiceConnection>();
      
      PackageManager packageManager = context.getPackageManager();
      Intent baseIntent = new Intent( PluginHandler.PLUGIN_ACTION );
      baseIntent.setFlags( Intent.FLAG_DEBUG_LOG_RESOLUTION );
      List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER );
      
      for( int i = 0 ; i < list.size() ; ++i ) {
        ResolveInfo info = list.get( i );
        ServiceInfo sinfo = info.serviceInfo;
        IntentFilter filter1 = info.filter;

        Log.d( "info23", "fillPluginList: i: "+i+"; sinfo: "+sinfo+";filter: "+filter1 + " " + sinfo.name);
        if(sinfo != null) {
          Log.d( "info23", "hier " + filter1.countCategories() + " " + filter1.getAction(0));
          if( filter1 != null ) {
            StringBuilder categories = new StringBuilder();
            String firstCategory = null;
            
            for( Iterator<String> categoryIterator = filter1.categoriesIterator() ;
                categoryIterator.hasNext() ; ) {
              String category = categoryIterator.next();
              if( firstCategory == null )
                firstCategory = category;
              if( categories.length() > 0 )
                categories.append( "," );
              categories.append( category );
            }
            
            if(firstCategory != null) {
              PluginServiceConnection plugin = new PluginServiceConnection(sinfo.name, context);
              
              Intent intent = new Intent( PluginHandler.PLUGIN_ACTION );
              intent.addCategory( categories.toString() );
                            
              context.bindService( intent, plugin, Context.BIND_AUTO_CREATE);
              
              PLUGIN_LIST.add(plugin);
            }
            
            Log.d( "info23", "categories: " + categories.toString());
          }
        }
      }
      
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          Collections.sort(PLUGIN_LIST);
        }
      }, 5000);
    }
  }
  
  public static final void shutdownPlugins(Context context) {
    if(PLUGIN_LIST != null) {
      for(PluginServiceConnection plugin : PLUGIN_LIST) {
        if(plugin.isActivated()) {
          plugin.callOnDeactivation();
        }
        
        try {
          if(plugin.isConnected()) {
            context.unbindService(plugin);
          }
        }catch(RuntimeException e) {}
      }
    
      PLUGIN_LIST.clear();
      PLUGIN_LIST = null;
    }
    
    PLUGIN_MANAGER = null;
    
    PluginHandler.PLUGIN_LIST = null;
  }
  
  public static PluginManager getPluginManager() {
    return PLUGIN_MANAGER;
  }
  
  public static boolean isMarkedByPlugins(long programId) {
    boolean result = false;
    
    if(PLUGIN_LIST != null) {
      for(PluginServiceConnection connection : PLUGIN_LIST) {
        Log.d("info45", connection.getId());
        try {
          Log.d("info45", "" + connection.getPlugin().isMarked(programId));
          if(connection.isActivated() && connection.getPlugin().isMarked(programId)) {
            result = true;
            break;
          }
        } catch (RemoteException e) {}
      }
    }
    
    return result;
  }
  
  public static long getFirstProgramId() {
    return FIRST_PROGRAM_ID;
  }
}
