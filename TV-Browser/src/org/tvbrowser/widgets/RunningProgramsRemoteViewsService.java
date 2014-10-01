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

import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * Service for showing currently running programs as widget.
 * 
 * @author René Mach
 */
public class RunningProgramsRemoteViewsService extends RemoteViewsService {

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new RunningProgramsRemoteViewsFactory(getApplicationContext(),intent.getExtras());
  }
  
  class RunningProgramsRemoteViewsFactory implements RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;
    private Handler mUpdateHandler;
    private Runnable mUpdateRunnable;
    
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
      
      mUpdateHandler.removeCallbacks(mUpdateRunnable);
      
      final String[] projection = new String[] {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID
      };
      
      final String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " +
      TvBrowserContentProvider.DATA_KEY_ENDTIME + ">" + System.currentTimeMillis() + " ) AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE;
            
      final long token = Binder.clearCallingIdentity();
      try {
        mCursor = getApplicationContext().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
        
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
        
        final String logoNamePref = PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_RUNNING, R.string.channel_logo_name_running_default);
        
        mShowEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_RUNNING_LIST, R.bool.show_episode_in_running_list_default);
        mShowChannelName = (logoNamePref.equals("0") || logoNamePref.equals("2"));
        mShowChannelLogo = (logoNamePref.equals("0") || logoNamePref.equals("1") || logoNamePref.equals("3"));
        mShowOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_RUNNING_LIST, R.bool.show_sort_number_in_running_list_default);
        mChannelClickToProgramsList = PrefUtils.getBooleanValue(R.string.PREF_RUNNING_LIST_CLICK_TO_CHANNEL_TO_LIST, R.bool.pref_running_list_click_to_channel_to_list_default);
        
        if(mCursor.getCount() > 0) {
          mUpdateHandler.postDelayed(mUpdateRunnable, ((System.currentTimeMillis() / 60000) * 60000 + 62000) - System.currentTimeMillis());
        }
      } finally {
          Binder.restoreCallingIdentity(token);
      }
    }
    
    public RunningProgramsRemoteViewsFactory(Context context, Bundle extras) {
      mContext = context;
      PrefUtils.initialize(context);
      SettingConstants.initializeLogoMap(context, false);
      
      if(extras != null) {
        mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
      }
      else {
        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
      }
    }
        
    @Override
    public void onCreate() {
      mUpdateHandler = new Handler();
      mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
          final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
          
          if(pm.isScreenOn()) {
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.running_widget_list_view);
              
            mUpdateHandler.postDelayed(mUpdateRunnable, 60000);
          }
        }
      };
      
      executeQuery();
    }

    @Override
    public void onDataSetChanged() {
      executeQuery();
    }

    @Override
    public void onDestroy() {
      if(mCursor != null && !mCursor.isClosed()) {
        mCursor.close();
      }
    }

    @Override
    public int getCount() {
      if(mCursor != null && !mCursor.isClosed()) {
        return mCursor.getCount();
      }
      
      return 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
      mCursor.moveToPosition(position);
      
      final byte idIndex = (byte)((mColumnIndicies >> 28) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      final byte startTimeIndex = (byte)((mColumnIndicies >> 24) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      final byte endTimeIndex = (byte)((mColumnIndicies >> 20) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
      final byte titleIndex = (byte)((mColumnIndicies >> 16) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
      final byte channelNameIndex = (byte)((mColumnIndicies >> 12) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      final byte orderNumberIndex = (byte)((mColumnIndicies >> 8) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      final byte logoIndex = (byte)((mColumnIndicies >> 4) & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      final byte episodeIndex = (byte)(mColumnIndicies & 0xF);//mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      
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
      
      final RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.running_programs_widget_row);
      
      final String time = DateFormat.getTimeFormat(mContext).format(new Date(startTime));
      
      if(startTime <= System.currentTimeMillis() && endTime > System.currentTimeMillis()) { 
        final int length = (int)(endTime - startTime) / 60000;
        final int progress = (int)(System.currentTimeMillis() - startTime) / 60000;
        rv.setProgressBar(R.id.running_programs_widget_row_progress, length, progress, false);
      }
      
      rv.setTextViewText(R.id.running_programs_widget_row_start_time, time);
      rv.setTextViewText(R.id.running_programs_widget_row_title, title);
      
      if(mShowChannelName || logo == null) {
        rv.setTextViewText(R.id.running_programs_widget_row_channel_name, name);
        rv.setViewVisibility(R.id.running_programs_widget_row_channel_name, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.running_programs_widget_row_channel_name, View.GONE);
      }
            
      if(logo != null && ((BitmapDrawable)logo).getBitmap() != null) {
        rv.setImageViewBitmap(R.id.running_programs_widget_row_channel_logo, ((BitmapDrawable)logo).getBitmap());
        rv.setViewVisibility(R.id.running_programs_widget_row_channel_logo, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.running_programs_widget_row_channel_logo, View.GONE);
      }
      
      if(episodeTitle != null) {
        rv.setTextViewText(R.id.running_programs_widget_row_episode, episodeTitle);
        rv.setViewVisibility(R.id.running_programs_widget_row_episode, View.VISIBLE);
      }
      else {
        rv.setViewVisibility(R.id.running_programs_widget_row_episode, View.GONE);
      }
      
      final Intent fillInIntent = new Intent();
      fillInIntent.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, Long.valueOf(id));
      
      rv.setOnClickFillInIntent(R.id.running_programs_widget_row_program, fillInIntent);
      
      if(mChannelClickToProgramsList) {
        final Intent startTvbProgramList = new Intent();
        startTvbProgramList.putExtra(SettingConstants.CHANNEL_ID_EXTRA, channelKey);
        startTvbProgramList.putExtra(SettingConstants.START_TIME_EXTRA, startTime);
        
        rv.setOnClickFillInIntent(R.id.running_programs_widget_row_channel, startTvbProgramList);
      }
      
      return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
      return null;
    }

    @Override
    public int getViewTypeCount() {
      return 1;
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
    public boolean hasStableIds() {
      return true;
    }
    
  }

}
