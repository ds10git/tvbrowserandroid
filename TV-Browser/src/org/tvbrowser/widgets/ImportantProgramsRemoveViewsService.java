/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.widgets;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.UiUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * Service for showing important programs as widget.
 * 
 * @author René Mach
 */
public class ImportantProgramsRemoveViewsService extends RemoteViewsService {

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new ImportantProgramsRemoteViewsFactory(getApplicationContext());
  }

  class ImportantProgramsRemoteViewsFactory implements RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;
    private HashMap<String, Integer> mMarkingColumsIndexMap;
    private Handler mUpdateHandler;
    private Runnable mUpdateRunnable;
    private PendingIntent mPendingUpdate;
    
    private Cursor executeQuery() {
      if(mCursor != null && !mCursor.isClosed()) {
        mCursor.close();
      }
      
      String[] projection = new String[] {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
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
      
      String where = " ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis() + " ) AND ( " +
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
          
          if(c.getCount() > 0 && c.moveToFirst()) {
            long startTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
            
            AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            
            if(mPendingUpdate != null) {
              alarm.cancel(mPendingUpdate);
            }
            
            if(startTime > System.currentTimeMillis()) {
              Intent update = new Intent(mContext,ImportantProgramsWidgetUpdateReceiver.class);
              
              PendingIntent pending = PendingIntent.getBroadcast(mContext, (int)(startTime/60000), update, PendingIntent.FLAG_UPDATE_CURRENT);
              
              alarm.set(AlarmManager.RTC, startTime + 5000, pending);
              
              
              Log.d("info6", "ADD PENDING AT " + new Date(startTime+5000) + " " + pending);
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
      mUpdateHandler = new Handler();
      
      mCursor = executeQuery();
    }

    @Override
    public void onDataSetChanged() {
      if(mUpdateRunnable != null) {
        mUpdateHandler.removeCallbacks(mUpdateRunnable);
        mUpdateRunnable = null;
      }
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
      int endTimeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
      int titleIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
      int channelNameIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      int orderNumberIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      int logoIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      int episodeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      
      String id = mCursor.getString(idIndex);
      long startTime = mCursor.getLong(startTimeIndex);
      long endTime = mCursor.getLong(endTimeIndex);
      String title = mCursor.getString(titleIndex);
      
      String name = mCursor.getString(channelNameIndex);
      String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
      String number = null;
      String episodeTitle = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_LISTS, R.bool.show_episode_in_lists_default) ? mCursor.getString(episodeIndex) : null;
      
      if(shortName != null) {
        name = shortName;
      }
      
      String logoNamePref = PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_PROGRAM_LISTS, R.string.channel_logo_name_program_lists_default);
      
      boolean showChannelName = logoNamePref.equals("0") || logoNamePref.equals("2");
      boolean showChannelLogo = logoNamePref.equals("0") || logoNamePref.equals("1");
      
      if(PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_LISTS, R.bool.show_sort_number_in_lists_default)) {
        number = mCursor.getString(orderNumberIndex);
        
        if(number == null) {
          number = "0";
        }
        
        number += ".";
        
        name =  number + " " + name;
      }
      
      Drawable logo = null;
      
      if(showChannelLogo && logoIndex >= 0) {
        int key = mCursor.getInt(logoIndex);
        logo = SettingConstants.SMALL_LOGO_MAP.get(key);
      }
      
      RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.important_programs_widget_row);
      
      String date = UiUtils.formatDate(startTime, mContext, false, true, true);
      String time = DateFormat.getTimeFormat(mContext).format(new Date(startTime));
      
      if(startTime <= System.currentTimeMillis() && endTime > System.currentTimeMillis()) {
        if(mUpdateRunnable == null) {
          mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
              PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
              
              if(pm.isScreenOn()) {
                UiUtils.updateImportantProgramsWidget(mContext);
              }
            }
          };
          
          //long firstRepeat = (System.currentTimeMillis() / 60000) * 60000 + 5000;
          
          //Log.d("info"," FIRST REPEAT " + new Date(firstRepeat));
          
          mUpdateHandler.postDelayed(mUpdateRunnable, 60000);
        }
        
        
        /*
         *  Intent remind = new Intent(context,ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_NO_CREATE);
    Logging.log(ReminderBroadcastReceiver.tag, " Delete reminder for programID '" + programID + "' with pending intent '" + pending + "'", Logging.REMINDER_TYPE, context);
    if(pending != null) {
      alarmManager.cancel(pending);
    }
         * */
        
        
        
        int length = (int)(endTime - startTime) / 60000;
        int progress = (int)(System.currentTimeMillis() - startTime) / 60000;
        rv.setProgressBar(R.id.important_programs_widget_row_progress, length, progress, false);
        rv.setViewVisibility(R.id.important_programs_widget_row_progress, View.VISIBLE);
        rv.setViewVisibility(R.id.important_programs_widget_row_start_date1, View.GONE);
      }
      else {        
        rv.setTextViewText(R.id.important_programs_widget_row_start_date1, date);
        rv.setViewVisibility(R.id.important_programs_widget_row_progress, View.GONE);
        rv.setViewVisibility(R.id.important_programs_widget_row_start_date1, View.VISIBLE);
      }
      
      rv.setTextViewText(R.id.important_programs_widget_row_start_time1, time);
      rv.setTextViewText(R.id.important_programs_widget_row_title1, title);
      
      if(showChannelName || logo == null) {
        rv.setTextViewText(R.id.important_programs_widget_row_channel_name1, name);
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_name1, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_name1, View.GONE);
      }
      
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
        rv.setImageViewBitmap(R.id.important_programs_widget_row_channel_logo1, ((BitmapDrawable)logo).getBitmap());
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_logo1, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_logo1, View.GONE);
      }
      
      if(episodeTitle != null) {
        rv.setTextViewText(R.id.important_programs_widget_row_episode, episodeTitle);
        rv.setViewVisibility(R.id.important_programs_widget_row_episode, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.important_programs_widget_row_episode, View.GONE);
      }
      
 //     Uri uri = Uri.withAppendedPath(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, id);
      
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
