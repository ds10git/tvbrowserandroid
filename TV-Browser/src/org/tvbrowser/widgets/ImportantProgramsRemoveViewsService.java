package org.tvbrowser.widgets;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.UiUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class ImportantProgramsRemoveViewsService extends RemoteViewsService {

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new ImportantProgramsRemoteViewsFactory(getApplicationContext());
  }

  class ImportantProgramsRemoteViewsFactory implements RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;
    private HashMap<String, Integer> mMarkingColumsIndexMap;
    
    private Cursor executeQuery() {
      String[] projection = new String[] {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_MARKING_MARKING,
        TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE,
        TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER,
        TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR,
        TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER,
        TvBrowserContentProvider.DATA_KEY_MARKING_SYNC,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID
      };
      
      String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">=" + System.currentTimeMillis() + " ) AND ( " +
          TvBrowserContentProvider.DATA_KEY_MARKING_MARKING + " OR " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE + " OR " +
          TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " OR " + TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR + " OR " +
          TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER + " OR " + TvBrowserContentProvider.DATA_KEY_MARKING_SYNC + " ) AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE;
      
      Cursor c = null;
      
      final long token = Binder.clearCallingIdentity();
      try {
          c = getApplicationContext().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + ", " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
          
          mMarkingColumsIndexMap = new HashMap<String, Integer>();
          
          for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
            int index = c.getColumnIndex(column);
            
            if(index >= 0) {
              mMarkingColumsIndexMap.put(column, Integer.valueOf(index));
            }
          }
      } finally {
          Binder.restoreCallingIdentity(token);
      }
      
      return c; 
    }
    
    public ImportantProgramsRemoteViewsFactory(Context context) {
      PrefUtils.initialize(context);
      mContext = context;
      SettingConstants.updateLogoMap(context);
    }
    
    @Override
    public void onCreate() {
      mCursor = executeQuery();
    }

    @Override
    public void onDataSetChanged() {
      mCursor = executeQuery();
    }
    
    @Override
    public int getCount() {
      if(mCursor != null) {
        return mCursor.getCount();
      }
      
      return 0;
    }

    @Override
    public long getItemId(int position) {
      if(mCursor != null) {
        return mCursor.getLong(mCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
      }
      
      return position;
    }
    
    @Override
    public RemoteViews getViewAt(int position) {
      mCursor.moveToPosition(position);
      
      int idIndex = mCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int startTimeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      int titleIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
      int channelNameIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      int orderNumberIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      int logoIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      int episodeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      
      String id = mCursor.getString(idIndex);
      long startTime = mCursor.getLong(startTimeIndex);
      String title = mCursor.getString(titleIndex);
      String channelName = mCursor.getString(channelNameIndex);
      
      String name = mCursor.getString(channelNameIndex);
      String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
      String number = null;
      String episodeTitle = mCursor.getString(episodeIndex);
      
      if(shortName != null) {
        name = shortName;
      }
      
      if(true) {
        number = mCursor.getString(orderNumberIndex);
        
        if(number == null) {
          number = "0";
        }
        
        number += ".";
        
        name =  number + " " + name;
      }
      
      Drawable logo = null;
      
      if(true && logoIndex >= 0) {
        int key = mCursor.getInt(logoIndex);
        logo = SettingConstants.SMALL_LOGO_MAP.get(key);
      }
      
      RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.important_programs_widget_row);
      
      String date = UiUtils.formatDate(startTime, mContext, false, true);
      String time = DateFormat.getTimeFormat(mContext).format(new Date(startTime));
      
      rv.setTextViewText(R.id.important_programs_widget_row_start_day, date);
      rv.setTextViewText(R.id.important_programs_widget_row_start_time, time);
      rv.setTextViewText(R.id.important_programs_widget_row_title, title);
      rv.setTextViewText(R.id.important_programs_widget_row_channel_name, channelName);
      
      ArrayList<String> markedColumns = new ArrayList<String>();
      
      for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        Integer index = mMarkingColumsIndexMap.get(column);
        
        if(index != null && mCursor.getInt(index.intValue()) == 1) {
          markedColumns.add(column);
        }
      }
      
      
      
      /*LayerDrawable draw = UiUtils.getMarkingsDrawable(mContext, mCursor, 0, 1, IOUtils.getStringArrayFromList(markedColumns), false);
      
      Bitmap b = Bitmap.createBitmap(250, 150, Bitmap.Config.ARGB_8888);
      draw.setBounds(0, 0, 250, 150);
      draw.draw(new Canvas(b));
      
      rv.setImageViewBitmap(R.id.important_programs_widget_row_background, b);*/
      //rv.setInt(R.id.important_programs_widget_row_program, "setBackgroundDrawable", draw);
      
      Log.d("info", "LOGO " + logo);
      if(logo != null && ((BitmapDrawable)logo).getBitmap() != null) {
        rv.setImageViewBitmap(R.id.important_programs_widget_row_channel_logo, ((BitmapDrawable)logo).getBitmap());
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_logo, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_logo, View.GONE);
      }
      
      if(episodeTitle != null) {
        rv.setTextViewText(R.id.important_programs_widget_row_episode, episodeTitle);
        rv.setViewVisibility(R.id.important_programs_widget_row_episode, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.important_programs_widget_row_episode, View.GONE);
      }
      
      Uri uri = Uri.withAppendedPath(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, id);
      
      Intent fillInIntent = new Intent();
      //fillInIntent.setData(uri);
      fillInIntent.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, Long.valueOf(id));
      
      rv.setOnClickFillInIntent(R.id.important_programs_widget_row_program, fillInIntent);
      
      return rv;
    }
    
    @Override
    public int getViewTypeCount() {
      return 1;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }
    
    @Override
    public RemoteViews getLoadingView() {
      return null;
    }

    @Override
    public void onDestroy() {
      mCursor.close();
    }
    
  }
}
