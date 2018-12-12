package org.tvbrowser.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.ChannelOrdered;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.ProgramOrdered;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Favorite;
import org.tvbrowser.tvbrowser.MarkingsUpdateListener;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.WhereClause;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Binder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.collection.SparseArrayCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;

public class ProgramUtils {
  public static final String[] DATA_CHANNEL_PROJECTION = {
      TvBrowserContentProvider.KEY_ID,
      TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
      TvBrowserContentProvider.CHANNEL_KEY_NAME,
      TvBrowserContentProvider.CHANNEL_KEY_LOGO,
      TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
      TvBrowserContentProvider.DATA_KEY_STARTTIME,
      TvBrowserContentProvider.DATA_KEY_ENDTIME,
      TvBrowserContentProvider.DATA_KEY_TITLE,
      TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
      TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE
  };

  public static Program createProgramFromDataCursor(Context context, Cursor cursor) {
    Program result = null;

    final ProgramOrdered test = createProgramOrderedFromDataCursor(context, cursor);

    if(test != null) {
      result = test.getProgram();
    }

    return result;
  }
  
  public static ProgramOrdered createProgramOrderedFromDataCursor(Context context, Cursor cursor) {
    ProgramOrdered result = null;
    
    if(IOUtils.prepareAccessFirst(cursor) && cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME) != -1) {
      ChannelOrdered channel = createChannelOrderedFromCursor(context, cursor);
      
      if(channel != null) {
        final long startTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
        
        Calendar test = Calendar.getInstance();
        test.add(Calendar.DAY_OF_YEAR, -1);
        test.set(Calendar.HOUR_OF_DAY, 0);
        test.set(Calendar.MINUTE, 0);
        test.set(Calendar.SECOND, 0);
        test.set(Calendar.MILLISECOND, 0);
        
        if(test.getTimeInMillis() <= startTime) {
          final long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          final String title = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
          final String shortDescription = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
          final String description = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));
          final String episodeTitle = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
          
          result = new ProgramOrdered(new Program(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)), startTime, endTime, title, shortDescription, description, episodeTitle, channel.getChannel()), channel);
        }
      }
    }
    
    return result;
  }
  
  public static Program[] createProgramsFromDataCursor(Context context, Cursor cursor) {
    ArrayList<Program> programsList = new ArrayList<>();
    SparseArrayCompat<Channel> channelMap = new SparseArrayCompat<>();
    
    if(cursor != null && !cursor.isClosed() && cursor.moveToFirst() && cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME) != -1) {
      Calendar test = Calendar.getInstance();
      test.add(Calendar.DAY_OF_YEAR, -1);
      test.set(Calendar.HOUR_OF_DAY, 0);
      test.set(Calendar.MINUTE, 0);
      test.set(Calendar.SECOND, 0);
      test.set(Calendar.MILLISECOND, 0);
      
      int idColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int startTimeColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      int endTimeColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
      int titleColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
      int shortDescriptionColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION);
      int descriptionColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION);
      int episodeColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      int channelIdColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      
      do {
        int channelId = cursor.getInt(channelIdColumnIndex);
        
        Channel channel = channelMap.get(channelId);
      
        if(channel == null) {
          channel = createChannelOrderedFromCursor(context, cursor).getChannel();
          
          if(channel != null) {
            channelMap.put(channelId, channel);
          }
        }
        
        if(channel != null) {
          final long startTime = cursor.getLong(startTimeColumnIndex);
          
          if(test.getTimeInMillis() <= startTime) {
            final long endTime = cursor.getLong(endTimeColumnIndex);
            final String title = cursor.getString(titleColumnIndex);
            final String shortDescription = cursor.getString(shortDescriptionColumnIndex);
            final String description = cursor.getString(descriptionColumnIndex);
            final String episodeTitle = cursor.getString(episodeColumnIndex);
            
            programsList.add(new Program(cursor.getLong(idColumnIndex), startTime, endTime, title, shortDescription, description, episodeTitle, channel));
          }
        }
      }while(cursor.moveToNext());
    }
    
    return programsList.isEmpty() ? null : programsList.toArray(new Program[0]);
  }
/*
  private static ChannelOrdered createChannelOrderedFromCursor(Context context, Cursor cursor) {
    ChannelOrdered channelOrdered = null;

    final Channel channel = createChannelFromCursor(context, cursor);

    if(channel != null) {
      channelOrdered = new ChannelOrdered(channel,, channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER)))
    }

    return channelOrdered;
  }*/
  
  private static ChannelOrdered createChannelOrderedFromCursor(Context context, Cursor cursor) {
    ChannelOrdered result = null;
    
    if(IOUtils.isDatabaseAccessible(context) && IOUtils.prepareAccessFirst(cursor)) {
      int nameColumn = cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      
      if(nameColumn == -1) {
        int channelId = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
        
        if(channelId >= 0) {
          Cursor channelCursor = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, TvBrowserContentProvider.KEY_ID + "=" + channelId, null, null);
          
          try {
            if(IOUtils.prepareAccessFirst(channelCursor)) {
              result = new ChannelOrdered(new Channel(channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)), channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)), channelCursor.getBlob(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO))), channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER)));
            }
          }finally {
            IOUtils.close(channelCursor);
          }
        }
      }
      else {
        int startTimeColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        
        String keyColumnName = TvBrowserContentProvider.KEY_ID;
        
        if(startTimeColumn != -1) {
          keyColumnName = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
        }
        
        result = new ChannelOrdered(new Channel(cursor.getInt(cursor.getColumnIndex(keyColumnName)), cursor.getString(nameColumn), cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO))),-1);
      }
    }
    
    return result;
  }
  
  public static boolean markProgram(Context context, Program program, String pluginId) {
    boolean result = false;
    
    if(IOUtils.isDatabaseAccessible(context)) {
      final long token = Binder.clearCallingIdentity();
      final Cursor programs = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, program.getId()), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_MARKING}, null, null, null);
      
      try {
        if(IOUtils.prepareAccessFirst(programs)) {
          if(programs.getInt(programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING)) == 0) {
            ContentValues mark = new ContentValues();
            mark.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, true);
            
            if(pluginId == null) {
              pluginId = "#unknownID";
            }
              
            result = markProgram(context, program.getId(), pluginId);
                      
            UiUtils.sendMarkingChangedBroadcast(context, program.getId(), false);
          }
          else {
            if(pluginId != null) {
              result = markProgram(context, program.getId(), pluginId);
              
              if(result) {
                UiUtils.sendMarkingChangedBroadcast(context, program.getId(), true);
              }
            }
            else {
              result = true;
            }
          }
        }
      }finally {
        IOUtils.close(programs);
        Binder.restoreCallingIdentity(token);
      }
    }
    
    return result;
  }
  
  public static boolean unmarkProgram(Context context, Program program, String pluginId) {
    boolean result = false;
    
    if(IOUtils.isDatabaseAccessible(context)) {
      final long token = Binder.clearCallingIdentity();
      Cursor programs = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, program.getId()), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_MARKING}, null, null, null);
      
      try {
        if(IOUtils.prepareAccessFirst(programs)) {
          if(programs.getInt(programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING)) == 1 && !PluginHandler.isMarkedByPlugins(program.getId())) {
            ContentValues mark = new ContentValues();
            mark.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, false);
            
            if(pluginId == null) {
              pluginId = "#unknownID";
            }
              
            result = unmarkProgram(context, program.getId(), pluginId);
            
            UiUtils.sendMarkingChangedBroadcast(context, program.getId(), false);
          }
          else {
            if(pluginId != null) {
              result = unmarkProgram(context, program.getId(), pluginId);
              
              if(result) {
                UiUtils.sendMarkingChangedBroadcast(context, program.getId(), true);
              }
            }
            else {
              result = true;
            }
          }
        }
      }finally {
        IOUtils.close(programs);
        Binder.restoreCallingIdentity(token);
      }
    }
    
    return result;
  }
  
  public static boolean isMarkedByPluginWithIcon(Context context, long programId, String pluginId) {
    boolean marked = false;
    SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKINGS, context);
    marked = pref.contains(String.valueOf(programId));
    
    if(marked && pluginId != null) {
      marked = pref.getString(String.valueOf(programId), "").contains(pluginId);
    }
    
    if(marked && pluginId != null) {
      PluginServiceConnection[] plugins = PluginHandler.getAvailablePlugins();
      
      for(PluginServiceConnection plugin : plugins) {
        if(pluginId.equals(plugin.getId())) {
          if(!plugin.isActivated()) {
            marked = false;
          }
          
          break;
        }
      }
    }
    
    return marked;
  }
  
  public static boolean isMarkedWithIcon(Context context, long programId) {
    boolean marked = false;
    
    marked = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKINGS, context).contains(String.valueOf(programId));
    
    return marked;
  }
  
  private static synchronized boolean markProgram(Context context, long programId, String pluginId) {
    boolean result = false;
    SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKINGS, context);
    
    String value = pref.getString(String.valueOf(programId), "").trim();
    
    if(!value.contains(pluginId)) {
      if(!value.isEmpty()) {
        value += ";";
      }
      else {
        ContentValues values = new ContentValues();
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, true);
        
        TvBrowserContentProvider.INFORM_FOR_CHANGES = false;
        context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programId), values, null, null);
        TvBrowserContentProvider.INFORM_FOR_CHANGES = true;
      }
      
      value += pluginId;
      
      String[] parts = value.split(";");
      
      Arrays.sort(parts);
      
      value = TextUtils.join(";", parts);
      
      Editor edit = pref.edit();
      edit.putString(String.valueOf(programId), value);
      result = edit.commit();
    }
    else {
      result = true;
    }
    
    return result;
  }
  
  private static synchronized boolean unmarkProgram(Context context, long programId, String pluginId) {
    boolean result = false;
    SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKINGS, context);
    
    String value = pref.getString(String.valueOf(programId), null).trim();
    
    if(value != null) {
      value = value.replaceAll(pluginId+";*", "").replaceAll(";{2,}", "");
      
      Editor edit = pref.edit();
      
      if(value.trim().isEmpty() || value.equals(";")) {
        edit.remove(String.valueOf(programId));
        
        ContentValues values = new ContentValues();
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, false);
        
        TvBrowserContentProvider.INFORM_FOR_CHANGES = false;
        context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programId), values, null, null);
        TvBrowserContentProvider.INFORM_FOR_CHANGES = true;
      }
      else {
        edit.putString(String.valueOf(programId), value);
      }
      
      result = edit.commit();
    }
    else {
      result = true;
    }
    
    return result;
  }
  
  public static void handleFirstAndLastKnownProgramId(Context context, long firstProgramId, long lastProgramId) {
    handleKnownIdInternal(context, firstProgramId, lastProgramId, PrefUtils.TYPE_PREFERENCES_MARKINGS);
    handleKnownIdInternal(context, firstProgramId, lastProgramId, PrefUtils.TYPE_PREFERENCES_MARKING_REMINDERS);
    handleKnownIdInternal(context, firstProgramId, lastProgramId, PrefUtils.TYPE_PREFERENCES_MARKING_SYNC);
  }
  
  private static void handleKnownIdInternal(Context context, long firstProgramId, long lastProgramId, int prefType) {
    SharedPreferences pref = PrefUtils.getSharedPreferences(prefType, context);
    Editor edit = pref.edit();
    
    if(firstProgramId == -1) {
      edit.clear();
    }
    else {
      Map<String,?> prefMap = pref.getAll();
      Set<String> keys = prefMap.keySet();
      
      for(String key : keys) {
        Long id = Long.parseLong(key);
        
        if(id < firstProgramId || (lastProgramId > firstProgramId && id > lastProgramId)) {
          edit.remove(key);
        }
      }
    }
    
    edit.commit();
  }
  
  private static ImageSpan ICON_REMINDER;
  private static ImageSpan ICON_SYNC;
  private static final String KEY_ICON_REMINDER = "org.tvbrowser.tvbrowser.REMINDER";
  private static final String KEY_ICON_SYNC = "org.tvbrowser.tvbrowser.SYNC";
  
  public static void resetReminderAndSyncMarkIcon(boolean isDarkTheme) {
    if(!isDarkTheme) {
      if(ICON_REMINDER != null) {
        ICON_REMINDER.getDrawable().setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
      }
      if(ICON_SYNC != null) {
        ICON_SYNC.getDrawable().setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
      }
    }
    else {
      if(ICON_REMINDER != null) {
        ICON_REMINDER.getDrawable().setColorFilter(null);
      }
      if(ICON_SYNC != null) {
        ICON_SYNC.getDrawable().setColorFilter(null);
      }
    }
  }
  
  public static CharSequence getMarkIcons(Context context, long programId, String title) {
    CharSequence result = title;
    
    if(PrefUtils.getBooleanValue(R.string.PREF_MARK_ICON_SHOW, R.bool.pref_mark_icon_show_default)) {
      SpannableStringBuilder markIcons = new SpannableStringBuilder();
      
      int favoriteMarkIconType = Favorite.getFavoriteMarkIconType(context, programId);
      
      if(favoriteMarkIconType >= 1) {
        markIcons.append(" ");
        markIcons.append(Favorite.KEY_MARKING_ICON);
              
        markIcons.setSpan(Favorite.getMarkIcon(context, favoriteMarkIconType), markIcons.length()-Favorite.KEY_MARKING_ICON.length(), markIcons.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      
      if(PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKING_REMINDERS, context).contains(String.valueOf(programId))) {
        markIcons.append(" ");
        markIcons.append(KEY_ICON_REMINDER);
        
        if(ICON_REMINDER == null) {
          ICON_REMINDER = UiUtils.createImageSpan(context, R.drawable.ic_action_alarms);
        }
        
        markIcons.setSpan(ICON_REMINDER, markIcons.length()-KEY_ICON_REMINDER.length(), markIcons.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      
      if(PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKING_SYNC, context).contains(String.valueOf(programId))) {
        markIcons.append(" ");
        markIcons.append(KEY_ICON_SYNC);
        
        if(ICON_SYNC == null) {
          ICON_SYNC = UiUtils.createImageSpan(context, R.drawable.ic_action_cloud);
        }
        
        markIcons.setSpan(ICON_SYNC, markIcons.length()-KEY_ICON_SYNC.length(), markIcons.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      
      SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKINGS, context);
      
      String value = pref.getString(String.valueOf(programId), null);
      
      if(value != null && !value.trim().isEmpty()) {
        String[] plugins = value.split(";");
              
        for(String plugin : plugins) {
          PluginServiceConnection connection = PluginHandler.getConnectionForId(plugin);
          
          if(connection != null && connection.isActivated()) {
            ImageSpan icon = connection.getPluginMarkIcon();
            
            if(icon != null) {
              markIcons.append(" ");
              markIcons.append(plugin);
              
              markIcons.setSpan(icon, markIcons.length()-plugin.length(), markIcons.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
          }
        }
      }
      
      if(markIcons.length() > 0) {
        markIcons.insert(0, title);
        result = markIcons;
      }
    }
    return result;
  }
  

  public static WhereClause getPluginMarkingsSelection(Context context) {
    WhereClause result = new WhereClause(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, null);
    
    if(context != null) {
      SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_MARKINGS, context);
      
      Map<String,?> prefMap = pref.getAll();
      Set<String> keySet = prefMap.keySet();
      
      if(keySet.size() < 500) {
        StringBuilder where = new StringBuilder();
        String[] selectionArgs = new String[prefMap.size()];
        
        Iterator<String> keys = keySet.iterator();
        
        for(int i=0; i < keySet.size(); i++) {
          if(where.length() > 0) {
            where.append(", ");
          }
          
          where.append("?");
          selectionArgs[i] = keys.next();
        }
        
        if(where.length() > 0) {
          where.insert(0, TvBrowserContentProvider.KEY_ID + " IN ( ");
          where.append(" ) ");
          
          result = new WhereClause(where.toString(), selectionArgs);
        }
      }
    }
    
    return result;
  }
  
  private static BroadcastReceiver mRefreshReceiver;
  private static ArrayList<MarkingsUpdateListener> mMarkingsListener;
  
  public static void registerMarkingsListener(Context context, MarkingsUpdateListener listener) {
    if(mRefreshReceiver == null) {
      if(mMarkingsListener != null) {
        mMarkingsListener.clear();
      }
      else {
        mMarkingsListener = new ArrayList<>();
      }
      
      mRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          //if(intent.getBooleanExtra(SettingConstants.EXTRA_MARKINGS_ONLY_UPDATE,false)) {
            if(mMarkingsListener != null) {
              for(MarkingsUpdateListener listener : mMarkingsListener) {
                listener.refreshMarkings();
              }
            }
          //}
        }
      };
      
      IntentFilter filter = new IntentFilter(SettingConstants.MARKINGS_CHANGED);
      
      LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(mRefreshReceiver, filter);
      listener.refreshMarkings();
    }
    
    mMarkingsListener.add(listener);
  }
  
  public static void unregisterMarkingsListener(Context context, MarkingsUpdateListener listener) {
    if(mMarkingsListener != null && !mMarkingsListener.isEmpty()) {
      mMarkingsListener.remove(listener);
      
      if(mMarkingsListener.isEmpty()) {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(mRefreshReceiver);
        
        mRefreshReceiver = null;
        mMarkingsListener = null;
      }
    }
  }
  
  public static void addReminderId(Context context, long programId) {
    addMarkId(context, programId, PrefUtils.TYPE_PREFERENCES_MARKING_REMINDERS);
  }
  
  public static void addReminderIds(Context context, ArrayList<String> idList) {
    addMarkIds(context, idList, PrefUtils.TYPE_PREFERENCES_MARKING_REMINDERS);
  }
  
  public static void removeReminderId(Context context, long programId) {
    removeMarkId(context, programId, PrefUtils.TYPE_PREFERENCES_MARKING_REMINDERS);
  }
  
  public static void removeReminderIds(Context context, ArrayList<String> idList) {
    removeMarkIds(context, idList, PrefUtils.TYPE_PREFERENCES_MARKING_REMINDERS);
  }
    
  public static void addSyncIds(Context context, ArrayList<String> idList) {
    addMarkIds(context, idList, PrefUtils.TYPE_PREFERENCES_MARKING_SYNC);
  }
  
  public static void removeSyncId(Context context, long programId) {
    removeMarkId(context, programId, PrefUtils.TYPE_PREFERENCES_MARKING_SYNC);
  }
    
  private static void addMarkId(Context context, long programId, int type) {
    Editor edit = PrefUtils.getSharedPreferences(type, context).edit();
    
    edit.putBoolean(String.valueOf(programId), true);
    
    edit.commit();
  }
  
  private static void addMarkIds(Context context, ArrayList<String> idList, int type) {
    Editor edit = PrefUtils.getSharedPreferences(type, context).edit();
    
    for(String programId : idList) {
      edit.putBoolean(programId, true);
    }
    
    edit.commit();
  }
  
  private static void removeMarkId(Context context, long programId, int type) {
    Editor edit = PrefUtils.getSharedPreferences(type, context).edit();
    
    edit.remove(String.valueOf(programId));
    
    edit.commit();
  }
  
  private static void removeMarkIds(Context context, ArrayList<String> idList, int type) {
    Editor edit = PrefUtils.getSharedPreferences(type, context).edit();
    
    for(String programId : idList) {
      edit.remove(programId);
    }
    
    edit.commit();
  }
}
