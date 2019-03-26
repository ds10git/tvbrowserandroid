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

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.TvBrowser;

public class WidgetOnClickReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(final Context context, final Intent intent) {
    long programID = intent.getLongExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, -1);

    if(intent.hasExtra(SettingConstants.EXTRA_START_TIME)) {
      Intent startTVB = new Intent(context, TvBrowser.class);
      startTVB.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);

      if(intent.hasExtra(SettingConstants.CHANNEL_ID_EXTRA)) {
        startTVB.putExtra(SettingConstants.CHANNEL_ID_EXTRA, intent.getExtras().getInt(SettingConstants.CHANNEL_ID_EXTRA));
        startTVB.putExtra(SettingConstants.EXTRA_START_TIME, intent.getExtras().getLong(SettingConstants.EXTRA_START_TIME));
        startTVB.putExtra(SettingConstants.EXTRA_END_TIME, intent.getExtras().getLong(SettingConstants.EXTRA_END_TIME, -1));
        startTVB.putExtra(SettingConstants.NO_BACK_STACKUP_EXTRA, true);
      }
      else {
        TvBrowser.START_TIME = intent.getExtras().getInt(SettingConstants.EXTRA_START_TIME);
      }
      
      context.getApplicationContext().startActivity(startTVB);
    }
    else if(SettingConstants.SELECT_TIME_WIDGET_RUNNING.equals(String.valueOf(intent.getAction())) && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
      Intent config = new Intent(context, InfoActivity.class);
      config.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
      config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));

      context.getApplicationContext().startActivity(config);
    }
    else if(programID >= 0) {
      Intent startInfo = new Intent(context, InfoActivity.class);
      startInfo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startInfo.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
      context.startActivity(startInfo);
    }
  }
}
