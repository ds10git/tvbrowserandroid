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
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class RunningProgramsListWidget extends AppWidgetProvider {
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int n = appWidgetIds.length;
    
    for(int i = 0; i < n; i++) {
      int appWidgetId = appWidgetIds[i];
      
      Intent intent = new Intent(context, RunningProgramsRemoteViewsService.class);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.running_programs_widget);      
      views.setRemoteAdapter(appWidgetId, R.id.running_widget_list_view, intent);
      views.setEmptyView(R.id.running_widget_list_view, R.id.running_widget_empty_text);
      
      Intent tvb = new Intent(context, TvBrowser.class);
            
      PendingIntent tvbstart = PendingIntent.getActivity(context, 0, tvb, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setOnClickPendingIntent(R.id.running_widget_header, tvbstart);
            
      Intent templateIntent = new Intent(SettingConstants.HANDLE_APP_WIDGET_CLICK);
      templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      PendingIntent templatePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      
      views.setPendingIntentTemplate(R.id.running_widget_list_view, templatePendingIntent);
      
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }
}
