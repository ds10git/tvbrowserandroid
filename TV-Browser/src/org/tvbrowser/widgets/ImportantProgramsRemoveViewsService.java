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
import org.tvbrowser.tvbrowser.CompatUtils;
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
    private PendingIntent mPendingUpdate;
    private PendingIntent mPendingRunning;
    private int mAppWidgetId;
    
    private int mIdIndex;
    private int mStartTimeIndex;
    private int mEndTimeIndex;
    private int mTitleIndex;
    private int mChannelNameIndex;
    private int mOrderNumberIndex;
    private int mLogoIndex;
    private int mEpisodeIndex;
    
    private int mVerticalPadding;
    
    private boolean mShowChannelName;
    private boolean mShowChannelLogo;
    private boolean mShowBigChannelLogo;
    private boolean mShowEpisode;
    private boolean mShowOrderNumber;
    private boolean mChannelClickToProgramsList;
    private float mTextScale;
    
    private void executeQuery() {
      if(mCursor != null && !mCursor.isClosed()) {
        mCursor.close();
      }
      
      cancelAlarms();
      
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
      
      if(pref.getBoolean(mAppWidgetId+"_"+mContext.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING);
      }
      
      if(pref.getBoolean(mAppWidgetId+"_"+mContext.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE);
      }
      
      if(pref.getBoolean(mAppWidgetId+"_"+mContext.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
      }
            
      if(pref.getBoolean(mAppWidgetId+"_"+mContext.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED), true)) {
        columns.add(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC);
      }
      
      if(!columns.isEmpty()) {
        where = " ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis() + " ) AND ( " + TextUtils.join(" OR ", columns) + " ) AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE;
      }
      else {
        where += TvBrowserContentProvider.DATA_KEY_ENDTIME + "<0 ";
      }
      
      String limit = "";
      
      if(pref.getBoolean(mAppWidgetId+"_"+mContext.getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT), false)) {
        limit = " LIMIT " + String.valueOf(pref.getInt(mAppWidgetId+"_"+mContext.getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT), 15));
      }
      
      final long token = Binder.clearCallingIdentity();
      try {
          mCursor = getApplicationContext().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + ", " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + limit);
          
          mMarkingColumsIndexMap = new HashMap<String, Integer>();
          
          mIdIndex = mCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
          mStartTimeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
          mEndTimeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
          mTitleIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
          mChannelNameIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
          mOrderNumberIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
          mLogoIndex = mCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
          mEpisodeIndex = mCursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
                    
          final String logoNamePref = PrefUtils.getStringValue(R.string.PREF_WIDGET_CHANNEL_LOGO_NAME, R.string.pref_widget_channel_logo_name_default);
          
          mShowEpisode = PrefUtils.getBooleanValue(R.string.PREF_WIDGET_SHOW_EPISODE, R.bool.pref_widget_show_episode_default);
          mShowChannelName = (logoNamePref.equals("0") || logoNamePref.equals("2"));
          mShowChannelLogo = (logoNamePref.equals("0") || logoNamePref.equals("1") || logoNamePref.equals("3"));
          mShowBigChannelLogo = logoNamePref.equals("3");
          mShowOrderNumber = PrefUtils.getBooleanValue(R.string.PREF_WIDGET_SHOW_SORT_NUMBER, R.bool.pref_widget_show_sort_number_default);
          mChannelClickToProgramsList = PrefUtils.getBooleanValue(R.string.PREF_WIDGET_CLICK_TO_CHANNEL_TO_LIST, R.bool.pref_widget_click_to_channel_to_list_default);
          mTextScale = Float.valueOf(PrefUtils.getStringValue(R.string.PREF_WIDGET_TEXT_SCALE, R.string.pref_widget_text_scale_default));
          mVerticalPadding = UiUtils.convertDpToPixel((int)(Float.parseFloat(PrefUtils.getStringValue(R.string.PREF_WIDGET_VERTICAL_PADDING_SIZE, R.string.pref_widget_vertical_padding_size_default))/2),mContext.getResources());
          
          for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
            final int index = mCursor.getColumnIndex(column);
            
            if(index >= 0) {
              mMarkingColumsIndexMap.put(column, Integer.valueOf(index));
            }
          }
          
          if(mCursor.getCount() > 0 && mCursor.moveToFirst()) {
            final long startTime = mCursor.getLong(mStartTimeIndex);
                        
            if(startTime > System.currentTimeMillis()) {
              final AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
              
              final Intent update = new Intent(SettingConstants.UPDATE_IMPORTANT_APP_WIDGET);
              update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
              
              final PendingIntent pending = PendingIntent.getBroadcast(mContext, (int)(startTime/60000), update, PendingIntent.FLAG_UPDATE_CURRENT);
              
              alarm.set(AlarmManager.RTC, startTime + 100, pending);
            }
            else {
              startRunningAlarm();
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
      mTextScale = 1.0f;
      
      executeQuery();
    }

    @Override
    public void onDataSetChanged() {
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
        
        return mCursor.getLong(mIdIndex);
      }
      
      return position;
    }
    
    @Override
    public RemoteViews getViewAt(int position) {
      mCursor.moveToPosition(position);

      final String id = mCursor.getString(mIdIndex);
      final long startTime = mCursor.getLong(mStartTimeIndex);
      final long endTime = mCursor.getLong(mEndTimeIndex);
      final String title = mCursor.getString(mTitleIndex);
      
      String name = mCursor.getString(mChannelNameIndex);
      final String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
      String number = null;
      final String episodeTitle = mShowEpisode ? mCursor.getString(mEpisodeIndex) : null;
      
      if(shortName != null) {
        name = shortName;
      }
            
      if(mShowOrderNumber) {
        number = mCursor.getString(mOrderNumberIndex);
        
        if(number == null) {
          number = "0";
        }
        
        number += ".";
        
        name =  number + " " + name;
      }
      
      Drawable logo = null;
      
      int channelKey = mCursor.getInt(mLogoIndex);
      
      if(mShowChannelLogo) {
        if(mShowBigChannelLogo) {
          logo = SettingConstants.MEDIUM_LOGO_MAP.get(channelKey);
        }
        else {
          logo = SettingConstants.SMALL_LOGO_MAP.get(channelKey);
        }
      }
      
      final RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.important_programs_widget_row);
      
      CompatUtils.setRemoteViewsPadding(rv, R.id.important_programs_widget_row, 0, mVerticalPadding, 0, mVerticalPadding);
      
      final String date = UiUtils.formatDate(startTime, mContext, false, true, true);
      final String time = DateFormat.getTimeFormat(mContext).format(new Date(startTime));
      
      if(startTime <= System.currentTimeMillis() && endTime > System.currentTimeMillis()) {
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
      else if(number != null) {
        rv.setTextViewText(R.id.important_programs_widget_row_channel_name1, number);
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
      
      if(!CompatUtils.isKeyguardWidget(mAppWidgetId, mContext)) {
        final Intent fillInIntent = new Intent();
        fillInIntent.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, Long.valueOf(id));
        
        rv.setOnClickFillInIntent(R.id.important_programs_widget_row_program, fillInIntent);
        
        if(mChannelClickToProgramsList) {
          final Intent startTvbProgramList = new Intent();
          startTvbProgramList.putExtra(SettingConstants.CHANNEL_ID_EXTRA, channelKey);
          startTvbProgramList.putExtra(SettingConstants.START_TIME_EXTRA, startTime);
          
          rv.setOnClickFillInIntent(R.id.important_programs_widget_row_channel, startTvbProgramList);
        }
      }
      
      float titleFontSize = mTextScale * UiUtils.convertPixelsToSp(mContext.getResources().getDimension(R.dimen.title_font_size),mContext);
      
      rv.setFloat(R.id.important_programs_widget_row_channel_name1, "setTextSize", titleFontSize);
      rv.setFloat(R.id.important_programs_widget_row_start_date1, "setTextSize", titleFontSize);
      rv.setFloat(R.id.important_programs_widget_row_title1, "setTextSize", titleFontSize);
      rv.setFloat(R.id.important_programs_widget_row_start_time1, "setTextSize", titleFontSize);
      rv.setFloat(R.id.important_programs_widget_row_episode, "setTextSize", mTextScale * UiUtils.convertPixelsToSp(mContext.getResources().getDimension(R.dimen.episode_font_size),mContext));
      
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
      
      cancelAlarms();
    }
    
    private void cancelAlarms() {
      if(mPendingUpdate != null) {
        ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(mPendingUpdate);
        mPendingUpdate = null;
      }
      if(mPendingRunning != null) {
        ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(mPendingRunning);
        mPendingRunning = null;
      }
    }
    
    private void startRunningAlarm() {
      if(mPendingRunning == null) {
        final Intent update = new Intent(SettingConstants.UPDATE_IMPORTANT_APP_WIDGET);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        
        mPendingRunning = PendingIntent.getBroadcast(mContext, (int)mAppWidgetId, update, PendingIntent.FLAG_UPDATE_CURRENT);
        
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        
        alarm.setRepeating(AlarmManager.RTC, ((System.currentTimeMillis()/60000) * 60000) + 60100, 60000, mPendingRunning);
      }
    }
  }
}
