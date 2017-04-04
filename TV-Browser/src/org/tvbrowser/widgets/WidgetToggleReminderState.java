package org.tvbrowser.widgets;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.BroadcastReceiverReminderToggle;
import org.tvbrowser.tvbrowser.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

public class WidgetToggleReminderState extends AppWidgetProvider {
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    // There may be multiple widgets active, so update all of them
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId);
    }
  }
  
  public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
    
    // Construct the RemoteViews object
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_toggle_reminder_state);
    
    if(SettingConstants.isReminderPaused(context)) {
      views.setInt(R.id.widget_toggle_reminder_state_button, "setBackgroundColor", Color.argb(128, 200, 0, 0));
      views.setImageViewResource(R.id.widget_toggle_reminder_state_button, R.drawable.ic_stat_reminder_halted);
    }
    else {
      views.setInt(R.id.widget_toggle_reminder_state_button, "setBackgroundColor", Color.argb(128, 0, 200, 0));
      views.setImageViewResource(R.id.widget_toggle_reminder_state_button, R.drawable.ic_stat_reminder);
    }
    
    Intent intentClick = new Intent(context,BroadcastReceiverReminderToggle.class);
    
    PendingIntent startService = PendingIntent.getBroadcast(context, appWidgetId, intentClick, PendingIntent.FLAG_UPDATE_CURRENT);
    views.setOnClickPendingIntent(R.id.widget_toggle_reminder_state_button, startService);
    
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views);
  }
}
