package org.tvbrowser.widgets;

import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.TvBrowser;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ImportantProgramsListWidget extends AppWidgetProvider {
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    final int n = appWidgetIds.length;
    
    for(int i = 0; i < n; i++) {
      int appWidgetId = appWidgetIds[i];
      
      Intent intent = new Intent(context, ImportantProgramsRemoveViewsService.class);
      
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.important_programs_widget);
      
      views.setRemoteAdapter(appWidgetId,R.id.important_widget_list_view, intent);
      views.setEmptyView(R.id.important_widget_list_view, R.id.important_widget_empty_text);
      
      Intent tvb = new Intent(context, TvBrowser.class);
      
      views.setTextViewText(R.id.important_widget_header, "TV-Browser important programs");
      
      PendingIntent tvbstart = PendingIntent.getActivity(context, 0, tvb, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setOnClickPendingIntent(R.id.important_widget_header, tvbstart);
      
      Intent templateIntent = new Intent(context, InfoActivity.class);
      templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      
      PendingIntent templatePendingIntent = PendingIntent.getActivity(context, 0, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      
      views.setPendingIntentTemplate(R.id.important_widget_list_view, templatePendingIntent);
      
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }
}
