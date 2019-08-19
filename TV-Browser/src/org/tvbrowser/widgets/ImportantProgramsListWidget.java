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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.widgets;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.TvBrowser;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * A widget for important programs.
 * 
 * @author René Mach
 */
public class ImportantProgramsListWidget extends AppWidgetProvider {
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("info2", "ImportantProgramsListWidget " + IOUtils.isInteractive(context) + " " +intent);
    if(intent != null && Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
      UiUtils.updateImportantProgramsWidget(context);
    }
    else {
      if(CompatUtils.isAtLeastAndroidO() || IOUtils.isInteractive(context) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
        if((AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction()) || SettingConstants.UPDATE_IMPORTANT_APP_WIDGET.equals(intent.getAction())) && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) && 
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
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.important_widget_list_view);
          }
        }
        else {
          super.onReceive(context, intent);
        }
      }
    }
  }
  
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
      
    for (int appWidgetId : appWidgetIds) {
      boolean isKeyguard = CompatUtils.isKeyguardWidget(appWidgetId, context);
      int type = PreferenceManager.getDefaultSharedPreferences(context).getInt(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_TYPE), context.getResources().getInteger(R.integer.widget_config_important_type_index_default));
      
      Intent intent = new Intent(context, ImportantProgramsRemoteViewsService.class);
      intent.setData(Uri.parse("org.tvbrowser://importantWidget/" + appWidgetId));
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      RemoteViews views = null;
      
      PrefUtils.initialize(context);
      
      String divider = PrefUtils.getStringValue(R.string.PREF_WIDGET_LISTS_DIVIDER_SIZE, R.string.pref_widget_lists_divider_size_default);
      
      if(divider.equals(context.getString(R.string.divider_tiny))) {
        views = new RemoteViews(context.getPackageName(), R.layout.important_programs_widget_divider_tiny);
      }
      else if(divider.equals(context.getString(R.string.divider_medium))) {
        views = new RemoteViews(context.getPackageName(), R.layout.important_programs_widget_divider_medium);
      }
      else if(divider.equals(context.getString(R.string.divider_big))) {
        views = new RemoteViews(context.getPackageName(), R.layout.important_programs_widget_divider_big);
      }
      else {
        views = new RemoteViews(context.getPackageName(), R.layout.important_programs_widget_divider_small);
      }
      
      if(PrefUtils.getBooleanValue(R.string.PREF_WIDGET_SIMPLE_ICON, R.bool.pref_widget_simple_icon_default)) {
        views.setImageViewResource(R.id.important_widget_header_icon, R.drawable.ic_widget_simple);
      }
      else {
        views.setImageViewResource(R.id.important_widget_header_icon, R.drawable.ic_widget);
      }
      
      int buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_medium;
      int headerDrawable = R.drawable.shape_button_background_corners_rounded_transparency_medium;
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
          case 4:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_rounded_transparency_full;
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
          case 4:
            listDrawable = R.drawable.shape_widget_background_corners_rounded_transparency_full;
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
          case 4:
            headerDrawable = buttonDrawable = R.drawable.shape_button_background_corners_straight_transparency_full;
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
          case 4:
            listDrawable = R.drawable.shape_widget_background_corners_straight_transparency_full;
            break;
            
          default:
            listDrawable = R.drawable.shape_widget_background_corners_straight_transparency_none;
            break;
        }
      }
      
      views.setInt(R.id.important_widget_header_wrapper, "setBackgroundResource", headerDrawable);
      views.setInt(R.id.important_widget_list_view, "setBackgroundResource", listDrawable);
      views.setInt(R.id.important_widget_config, "setBackgroundResource", buttonDrawable);
      views.setInt(R.id.important_widget_empty_text, "setBackgroundResource", listDrawable);
      
      views.setRemoteAdapter(R.id.important_widget_list_view, intent);
      views.setEmptyView(R.id.important_widget_list_view, R.id.important_widget_empty_text);
      
      if(type == 0) {
        views.setTextViewText(R.id.important_widget_header, PreferenceManager.getDefaultSharedPreferences(context).getString(appWidgetId+"_"+context.getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME), context.getString(R.string.widget_important_default_title)));
      }
      else {
        views.setTextViewText(R.id.important_widget_header, context.getString(R.string.title_programs_list));
        views.setInt(R.id.important_widget_config, "setImageResource", android.R.drawable.btn_star);
      }
      
      Intent config = new Intent(context, ImportantProgramsWidgetConfigurationActivity.class);
      config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      if(type == 1) {
        config = new Intent(context, InfoActivity.class);
        config.putExtra(SettingConstants.WIDGET_CHANNEL_SELECTION_EXTRA, appWidgetId);
      }
      
      PendingIntent configStart = PendingIntent.getActivity(context, appWidgetId, config, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setOnClickPendingIntent(R.id.important_widget_config, configStart);
      
      if(!isKeyguard) {
        Intent tvb = new Intent(context, TvBrowser.class);
        tvb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent tvbstart = PendingIntent.getActivity(context, appWidgetId, tvb, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.important_widget_header_info_wrapper, tvbstart);
        
        Intent templateIntent = new Intent(context, WidgetOnClickReceiver.class);
        templateIntent.setAction(SettingConstants.HANDLE_APP_WIDGET_CLICK);
        templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        
        PendingIntent templatePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        views.setPendingIntentTemplate(R.id.important_widget_list_view, templatePendingIntent);
      }
      
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }
  
  @Override
  public void onDisabled(Context context) {
    super.onDisabled(context);
  }
  
  @Override
  public void onDeleted(Context context, int[] appWidgetIds) {
    if(appWidgetIds != null && appWidgetIds.length > 0) {
      Editor edit = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
      
      for(int appWidgetId : appWidgetIds) {
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME));
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT));
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT));
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE));
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED));
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER));
        edit.remove(appWidgetId + "_" + context.getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED));
      }
      
      edit.commit();
    }
    
    super.onDeleted(context, appWidgetIds);
  }
}
