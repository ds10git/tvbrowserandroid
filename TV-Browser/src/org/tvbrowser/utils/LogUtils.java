package org.tvbrowser.utils;

import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class LogUtils {
  public static void logProgramData(Context context, String selection, String column) {
    final Cursor c = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, selection, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    try {
      if(IOUtils.prepareAccess(c)) {
        final int titleIndex = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
        final int startIndex = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        final int selectionIndex = c.getColumnIndex(column);
        
        while(c.moveToNext()) {
          Log.d("info12", ""+new Date(c.getLong(startIndex))+" " + c.getString(titleIndex) + " " + c.getInt(selectionIndex));
        }
      }
    }finally {
      IOUtils.close(c);
    }
  }
  
  public static void printStackTrace() {
    StackTraceElement[] els = Thread.currentThread().getStackTrace();
    
    for(StackTraceElement el : els) {
      Log.d("info12", "  "+el.toString());
    }
  }
}
