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

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.TvBrowser;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

/**
 * A widget for important programs.
 * 
 * @author René Mach
 */
public class ImportantProgramsListWidget extends AppWidgetProvider {
  @Override
  public void onReceive(Context context, Intent intent) {
    if(intent.getExtras() != null && intent.getExtras().containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID) && intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) != AppWidgetManager.INVALID_APPWIDGET_ID) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
      
      int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
      onUpdate(context, appWidgetManager, new int[] {appWidgetId});
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.important_widget_list_view);
    }
    else {
      super.onReceive(context, intent);
    }
  }
  
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    final int n = appWidgetIds.length;
    
    for(int i = 0; i < n; i++) {
      int appWidgetId = appWidgetIds[i];
      
      Intent intent = new Intent(context, ImportantProgramsRemoveViewsService.class);
      intent.setData(Uri.parse("org.tvbrowser://importantWidget/" + appWidgetId));
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.important_programs_widget);      
      views.setRemoteAdapter(appWidgetId, R.id.important_widget_list_view, intent);
      views.setEmptyView(R.id.important_widget_list_view, R.id.important_widget_empty_text);
      
      Intent tvb = new Intent(context, TvBrowser.class);
      
      views.setTextViewText(R.id.important_widget_header, PreferenceManager.getDefaultSharedPreferences(context).getString(appWidgetId+"_"+context.getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME), context.getString(R.string.widget_important_default_title)));
      
      PendingIntent tvbstart = PendingIntent.getActivity(context, 0, tvb, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setOnClickPendingIntent(R.id.important_widget_header, tvbstart);
      
      Intent config = new Intent(context, ImportantProgramsWidgetConfigurationActivity.class);
      config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      PendingIntent configStart = PendingIntent.getActivity(context, appWidgetId, config, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setOnClickPendingIntent(R.id.important_widget_config, configStart);
      
      Intent templateIntent = new Intent(SettingConstants.HANDLE_APP_WIDGET_CLICK);
      templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      PendingIntent templatePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      
      views.setPendingIntentTemplate(R.id.important_widget_list_view, templatePendingIntent);
      
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }
}
