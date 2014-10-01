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
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
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
    return new ImportantProgramsRemoteViewsFactory(getApplicationContext(),intent.getExtras());
  }

  class ImportantProgramsRemoteViewsFactory implements RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;
    private HashMap<String, Integer> mMarkingColumsIndexMap;
    private Handler mUpdateHandler;
    private Runnable mUpdateRunnable;
    private PendingIntent mPendingUpdate;
    private int mAppWidgetId;
    
    private int mColumnIndicies;
    
    private boolean mShowChannelName;
    private boolean mShowChannelLogo;
    private boolean mShowEpisode;
    private boolean mShowOrderNumber;
    private boolean mChannelClickToProgramsList;
    
    private void executeQuery() {
      if(mCursor != null && !mCursor.isClosed()) {
        mCursor.close();
      }
      
      final String[] projection = new String[] {
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
            
      String where = "";
      
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
      
      ArrayList<String> columns = new ArrayList<String>();
      
      if(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING);
      }
      
      if(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE);
      }
      
      if(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
      }
      
      if(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_CALENDER), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR);
      }
      
      if(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC);
      }
      
      if(!columns.isEmpty()) {
        where = " ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis() + " ) AND ( " + TextUtils.join(" OR ", columns) + " ) AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE;
      }
      else {
        where += TvBrowserContentProvider.DATA_KEY_ENDTIME + "<0 ";
      }
      
      String limit = "";
      
      if(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT), false)) {
        limit = " LIMIT " + String.valueOf(pref.getInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT), 15));
      }
      
      final long token = Binder.clearCallingIdentity();
      try {
          mCursor = getApplicationContext().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + ", " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + limit);
          
          mMarkingColumsIndexMap = new HashMap<String, Integer>();
          
          final byte idIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
          final byte startTimeIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
          final byte endTimeIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
          final byte titleIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
          final byte channelNameIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
          final byte orderNumberIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
          final byte logoIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
          final byte episodeIndex = (byte)mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
          
          mColumnIndicies = 0;
          mColumnIndicies = idIndex & 0xF;
          mColumnIndicies = (mColumnIndicies << 4) | (startTimeIndex & 0xF);
          mColumnIndicies = (mColumnIndicies << 4) | (endTimeIndex & 0xF);
          mColumnIndicies = (mColumnIndicies << 4) | (titleIndex & 0xF);
          mColumnIndicies = (mColumnIndicies << 4) | (channelNameIndex & 0xF);
          mColumnIndicies = (mColumnIndicies << 4) | (orderNumberIndex & 0xF);
          mColumnIndicies = (mColumnIndicies << 4) | (logoIndex & 0xF);
          mColumnIndicies = (mColumnIndicies << 4) | (episodeIndex & 0xF);
          
          final String logoNamePref = PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_PROGRAM_LISTS, R.string.channel_logo_name_program_lists_default);
          
          mShowEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_LISTS, R.bool.show_episode_in_lists_default);
          mShowChannelName = (logoNamePref.equals("0") || logoNamePref.equals("2"));
          mShowChannelLogo = (logoNamePref.equals("0") || logoNamePref.equals("1"));
          mShowOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_LISTS, R.bool.show_sort_number_in_lists_default);
          mChannelClickToProgramsList = PrefUtils.getBooleanValue(R.string.PREF_PROGRAM_LISTS_CLICK_TO_CHANNEL_TO_LIST, R.bool.pref_program_lists_click_to_channel_to_list_default);
          
          for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
            final int index = mCursor.getColumnIndex(column);
            
            if(index >= 0) {
              mMarkingColumsIndexMap.put(column, Integer.valueOf(index));
            }
          }
          
          if(mCursor.getCount() > 0 && mCursor.moveToFirst()) {
            final long startTime = mCursor.getLong(startTimeIndex);
            
            final AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            
            if(mPendingUpdate != null) {
              alarm.cancel(mPendingUpdate);
            }
            
            if(startTime > System.currentTimeMillis()) {
              final Intent update = new Intent(mContext,ProgramsWidgetsUpdateReceiver.class);
              
              final PendingIntent pending = PendingIntent.getBroadcast(mContext, (int)(startTime/60000), update, PendingIntent.FLAG_UPDATE_CURRENT);
              
              alarm.set(AlarmManager.RTC, startTime + 2000, pending);
            }
          }
      } finally {
          Binder.restoreCallingIdentity(token);
      }
    }
    
    public ImportantProgramsRemoteViewsFactory(Context context, Bundle extras) {
      PrefUtils.initialize(context);
      mContext = context;
      SettingConstants.initializeLogoMap(context, false);
      
      if(extras != null && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
        mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
      }
      else {
        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
      }
    }
    
    @Override
    public void onCreate() {
      mUpdateHandler = new Handler();
      
      executeQuery();
    }

    @Override
    public void onDataSetChanged() {
      if(mUpdateRunnable != null) {
        mUpdateHandler.removeCallbacks(mUpdateRunnable);
        mUpdateRunnable = null;
      }
      
      executeQuery();
    }
    
    @Override
    public int getCount() {
      if(mCursor != null && !mCursor.isClosed()) {
        return mCursor.getCount();
      }
      
      return 0;
    }

    @Override
    public long getItemId(int position) {
      if(mCursor != null && !mCursor.isClosed()) {
        mCursor.moveToPosition(position);
        
        return mCursor.getLong((mColumnIndicies >> 28) & 0xF);
      }
      
      return position;
    }
    
    @Override
    public RemoteViews getViewAt(int position) {
      mCursor.moveToPosition(position);
            
      final byte idIndex = (byte)((mColumnIndicies >> 28) & 0xF);
      final byte startTimeIndex = (byte)((mColumnIndicies >> 24) & 0xF);
      final byte endTimeIndex = (byte)((mColumnIndicies >> 20) & 0xF);
      final byte titleIndex = (byte)((mColumnIndicies >> 16) & 0xF);
      final byte channelNameIndex = (byte)((mColumnIndicies >> 12) & 0xF);
      final byte orderNumberIndex = (byte)((mColumnIndicies >> 8) & 0xF);
      final byte logoIndex = (byte)((mColumnIndicies >> 4) & 0xF);
      final byte episodeIndex = (byte)(mColumnIndicies & 0xF);

      final String id = mCursor.getString(idIndex);
      final long startTime = mCursor.getLong(startTimeIndex);
      final long endTime = mCursor.getLong(endTimeIndex);
      final String title = mCursor.getString(titleIndex);
      
      String name = mCursor.getString(channelNameIndex);
      final String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
      String number = null;
      final String episodeTitle = mShowEpisode ? mCursor.getString(episodeIndex) : null;
      
      if(shortName != null) {
        name = shortName;
      }
            
      if(mShowOrderNumber) {
        number = mCursor.getString(orderNumberIndex);
        
        if(number == null) {
          number = "0";
        }
        
        number += ".";
        
        name =  number + " " + name;
      }
      
      Drawable logo = null;
      
      int channelKey = mCursor.getInt(logoIndex);
      
      if(mShowChannelLogo) {
        logo = SettingConstants.SMALL_LOGO_MAP.get(channelKey);
      }
      
      final RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.important_programs_widget_row);
      
      final String date = UiUtils.formatDate(startTime, mContext, false, true, true);
      final String time = DateFormat.getTimeFormat(mContext).format(new Date(startTime));
      
      if(startTime <= System.currentTimeMillis() && endTime > System.currentTimeMillis()) {
        if(mUpdateRunnable == null) {
          mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
              final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
              
              if(pm.isScreenOn()) {
                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext.getApplicationContext());
                appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.important_widget_list_view);
              }
            }
          };
          
          mUpdateHandler.postDelayed(mUpdateRunnable, ((System.currentTimeMillis() / 60000) * 60000 + 62000) - System.currentTimeMillis());
        }
        
        final int length = (int)(endTime - startTime) / 60000;
        final int progress = (int)(System.currentTimeMillis() - startTime) / 60000;
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
      
      if(mShowChannelName || logo == null) {
        rv.setTextViewText(R.id.important_programs_widget_row_channel_name1, name);
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_name1, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.important_programs_widget_row_channel_name1, View.GONE);
      }
      
      ArrayList<String> markedColumns = new ArrayList<String>();
      
      for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        final Integer index = mMarkingColumsIndexMap.get(column);
        
        if(index != null && mCursor.getInt(index.intValue()) == 1) {
          markedColumns.add(column);
        }
      }
      
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
      
      final Intent fillInIntent = new Intent();
      fillInIntent.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, Long.valueOf(id));
      
      rv.setOnClickFillInIntent(R.id.important_programs_widget_row_program, fillInIntent);
      
      if(mChannelClickToProgramsList) {
        final Intent startTvbProgramList = new Intent();
        startTvbProgramList.putExtra(SettingConstants.CHANNEL_ID_EXTRA, channelKey);
        startTvbProgramList.putExtra(SettingConstants.START_TIME_EXTRA, startTime);
        
        rv.setOnClickFillInIntent(R.id.important_programs_widget_row_channel, startTvbProgramList);
      }
      
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
      if(mCursor != null && !mCursor.isClosed()) {
        mCursor.close();
      }
    }
    
  }
}
