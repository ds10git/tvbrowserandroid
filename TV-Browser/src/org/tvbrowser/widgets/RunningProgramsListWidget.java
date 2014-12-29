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

import java.util.Calendar;

import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.CompatUtils;
import org.tvbrowser.tvbrowser.IOUtils;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.TvBrowser;
import org.tvbrowser.tvbrowser.UiUtils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A widget for currently running programs.
 * 
 * @author René Mach
 */
public class RunningProgramsListWidget extends AppWidgetProvider {
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("info2", "" + IOUtils.isInteractive(context) + " " +intent);
    if(intent != null && Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
      UiUtils.updateRunningProgramsWidget(context);
    }
    else {
      if(IOUtils.isInteractive(context) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
        if((AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction()) || SettingConstants.UPDATE_RUNNING_APP_WIDGET.equals(intent.getAction())) && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) && 
            (intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) != AppWidgetManager.INVALID_APPWIDGET_ID || intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))) {
          AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
          
          int[] appWidgetIds = null;
          
          if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            appWidgetIds = new int[] {intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)};
          }
          else if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
            appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
          }
          
          if(appWidgetIds != null) {
            onUpdate(context, appWidgetManager, appWidgetIds);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.running_widget_list_view);
          }
        }
        else {
          super.onReceive(context, intent);
        }
      }
    }
  }
  
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int n = appWidgetIds.length;
    
    for(int i = 0; i < n; i++) {
      int appWidgetId = appWidgetIds[i];
      
      boolean isKeyguard = CompatUtils.isKeyguardWidget(appWidgetId, context);
      
      Intent intent = new Intent(context, RunningProgramsRemoteViewsService.class);
      intent.setData(Uri.parse("org.tvbrowser://runningWidget/" + appWidgetId));
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      RemoteViews views = null;
      
      PrefUtils.initialize(context);
      
      String divider = PrefUtils.getStringValue(R.string.PREF_WIDGET_LISTS_DIVIDER_SIZE, R.string.pref_widget_lists_divider_size_default);
      
      if(divider.equals(context.getString(R.string.divider_tiny))) {
        views = new RemoteViews(context.getPackageName(), R.layout.running_programs_widget_divider_tiny);
      }
      else if(divider.equals(context.getString(R.string.divider_medium))) {
        views = new RemoteViews(context.getPackageName(), R.layout.running_programs_widget_divider_medium);
      }
      else if(divider.equals(context.getString(R.string.divider_big))) {
        views = new RemoteViews(context.getPackageName(), R.layout.running_programs_widget_divider_big);
      }
      else {
        views = new RemoteViews(context.getPackageName(), R.layout.running_programs_widget_divider_small);
      }
      
      if(PrefUtils.getBooleanValue(R.string.PREF_WIDGET_SIMPLE_ICON, R.bool.pref_widget_simple_icon_default)) {
        views.setImageViewResource(R.id.running_widget_header_icon, R.drawable.ic_widget_simple);
      }
      else {
        views.setImageViewResource(R.id.running_widget_header_icon, R.drawable.ic_widget);
      }
      
      int buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_high;
      int headerDrawable = R.drawable.shape_button_background_corners_rounded_transparency_high;
      int listDrawable = R.drawable.shape_widget_background_corners_rounded_transparency_medium;
      
      boolean roundedCorners = PrefUtils.getBooleanValue(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS, R.bool.pref_widget_background_rounded_corners_default);
      int headerTransparency = PrefUtils.getStringValueAsInt(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_HEADER, R.string.pref_widget_background_transparency_header_default);
      int listTransparency = PrefUtils.getStringValueAsInt(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_LIST, R.string.pref_widget_background_transparency_list_default);

      if(roundedCorners) {
        switch (headerTransparency) {
          case 1:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_low;
            break;
          case 2:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_medium;
            break;
          case 3:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_high;
            break;

          default:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_none;
            break;
        }
        
        switch(listTransparency) {
          case 1:
            listDrawable = R.drawable.shape_widget_background_corners_rounded_transparency_low;
            break;
          case 2:
            listDrawable = R.drawable.shape_widget_background_corners_rounded_transparency_medium;
            break;
          case 3:
            listDrawable = R.drawable.shape_widget_background_corners_rounded_transparency_high;
            break;
          
          default:
            listDrawable = R.drawable.shape_widget_background_corners_rounded_transparency_none;
            break;
        }
      }
      else {
        switch (headerTransparency) {
          case 1:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_straight_transparency_low;
            break;
          case 2:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_straight_transparency_medium;
            break;
          case 3:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_straight_transparency_high;
            break;

          default:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_straight_transparency_none;
            break;
        }
        
        switch(listTransparency) {
          case 1:
            listDrawable = R.drawable.shape_widget_background_corners_straight_transparency_low;
            break;
          case 2:
            listDrawable = R.drawable.shape_widget_background_corners_straight_transparency_medium;
            break;
          case 3:
            listDrawable = R.drawable.shape_widget_background_corners_straight_transparency_high;
            break;
          
          default:
            listDrawable = R.drawable.shape_widget_background_corners_straight_transparency_none;
            break;
        }
      }
      
      views.setInt(R.id.running_widget_header_wrapper, "setBackgroundResource", headerDrawable);
      views.setInt(R.id.running_widget_list_view, "setBackgroundResource", listDrawable);
      views.setInt(R.id.running_widget_time, "setBackgroundResource", buttonDrawable);
      views.setInt(R.id.running_widget_empty_text, "setBackgroundResource", listDrawable);
      
      if(isKeyguard) {
        views.setViewVisibility(R.id.running_widget_time, View.GONE);
      }
      
      CompatUtils.setRemoteViewsAdapter(views, appWidgetId, R.id.running_widget_list_view, intent);
      views.setEmptyView(R.id.running_widget_list_view, R.id.running_widget_empty_text);
            
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
          
      int currentValue = pref.getInt(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_RUNNING_TIME), context.getResources().getInteger(R.integer.widget_config_running_time_default));

      if(currentValue >= 0) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, currentValue / 60);
        cal.set(Calendar.MINUTE, currentValue % 60);
        
        views.setTextViewText(R.id.running_widget_header, DateFormat.getTimeFormat(context).format(cal.getTime()));
      }
      else if(currentValue == -2) {
        views.setTextViewText(R.id.running_widget_header, context.getString(R.string.button_after));
      }
      else {
        views.setTextViewText(R.id.running_widget_header, context.getString(R.string.widget_running_title));
      }

      if(!isKeyguard) {
        Intent tvb = new Intent(context, TvBrowser.class);
        tvb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent tvbstart = PendingIntent.getActivity(context, 0, tvb, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.running_widget_header_info_wrapper, tvbstart);
        
        Intent config = new Intent(context, InfoActivity.class);
        config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        
        PendingIntent timeSelection = PendingIntent.getActivity(context, appWidgetId, config, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.running_widget_time, timeSelection);
        
        Intent templateIntent = new Intent(SettingConstants.HANDLE_APP_WIDGET_CLICK);
        templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        
        PendingIntent templatePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        views.setPendingIntentTemplate(R.id.running_widget_list_view, templatePendingIntent);
      }
      
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }
}
