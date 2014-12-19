package org.tvbrowser.tvbrowser;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.Program;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Binder;

public class ProgramUtils {
  public static final String[] DATA_CHANNEL_PROJECTION = {
      TvBrowserContentProvider.KEY_ID,
      TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
      TvBrowserContentProvider.CHANNEL_KEY_NAME,
      TvBrowserContentProvider.CHANNEL_KEY_LOGO,
      TvBrowserContentProvider.DATA_KEY_STARTTIME,
      TvBrowserContentProvider.DATA_KEY_ENDTIME,
      TvBrowserContentProvider.DATA_KEY_TITLE,
      TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
      TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE
  };
  
  public static final String[] DATA_PROJECTION = {
    TvBrowserContentProvider.KEY_ID,
    TvBrowserContentProvider.DATA_KEY_STARTTIME,
    TvBrowserContentProvider.DATA_KEY_ENDTIME,
    TvBrowserContentProvider.DATA_KEY_TITLE,
    TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
    TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE
  };
  
  public static final Program createProgramFromDataCursor(Context context, Cursor cursor) {
    Program result = null;
    
    if(cursor != null && !cursor.isClosed() && cursor.moveToFirst() && cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME) != -1) {
      Channel channel = createChannelFromCursor(context, cursor);
      
      if(channel != null) {
        final long startTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
        final long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
        final String title = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
        final String shortDescription = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
        final String description = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));
        final String episodeTitle = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
        
        result = new Program(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)), startTime, endTime, title, shortDescription, description, episodeTitle, channel);
      }
    }
    
    return result;
  }
  
  public static final Channel createChannelFromCursor(Context context, Cursor cursor) {
    Channel result = null;
    
    if(cursor != null && !cursor.isClosed() && cursor.moveToFirst()) {
      int nameColumn = cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      
      if(nameColumn == -1) {
        int channelId = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
        
        if(channelId >= 0) {
          Cursor channelCursor = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, TvBrowserContentProvider.KEY_ID + "=" + channelId, null, null);
          
          try {
            if(channelCursor != null && !channelCursor.isClosed() && channelCursor.moveToFirst()) {
              result = new Channel(channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)), channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)), channelCursor.getBlob(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));
            }
          }finally {
            IOUtils.closeCursor(channelCursor);
          }
        }
      }
      else {
        int startTimeColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        
        String keyColumnName = TvBrowserContentProvider.KEY_ID;
        
        if(startTimeColumn != -1) {
          keyColumnName = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
        }
        
        result = new Channel(cursor.getInt(cursor.getColumnIndex(keyColumnName)), cursor.getString(nameColumn), cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));
      }
    }
    
    return result;
  }
  
  public static final boolean markProgram(Context context, Program program) {
    boolean result = false;
    
    final long token = Binder.clearCallingIdentity();
    Cursor programs = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, program.getId()), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_MARKING}, null, null, null);
    
    try {
      if(programs != null && programs.moveToFirst()) {
        if(programs.getInt(programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING)) == 0) {
          ContentValues mark = new ContentValues();
          mark.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, true);
          
          result = context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, program.getId()), mark, null, null) > 0;
          
          UiUtils.sendMarkingChangedBroadcast(context, program.getId());
        }
        else {
          result = true;
        }
      }
    }finally {
      IOUtils.closeCursor(programs);
      Binder.restoreCallingIdentity(token);
    }
    
    return result;
  }
  
  public static final boolean unmarkProgram(Context context, Program program) {
    boolean result = false;
    
    final long token = Binder.clearCallingIdentity();
    Cursor programs = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, program.getId()), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_MARKING}, null, null, null);
    
    try {
      if(programs != null && programs.moveToFirst()) {
        if(programs.getInt(programs.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING)) == 1 && !PluginHandler.isMarkedByPlugins(program.getId())) {
          ContentValues mark = new ContentValues();
          mark.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, false);
          
          result = context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, program.getId()), mark, null, null) > 0;
          
          UiUtils.sendMarkingChangedBroadcast(context, program.getId());
        }
        else {
          result = true;
        }
      }
    }finally {
      IOUtils.closeCursor(programs);
      Binder.restoreCallingIdentity(token);
    }
    
    return result;
  }
}
