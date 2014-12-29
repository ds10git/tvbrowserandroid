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
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.TvBrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

public class WidgetOnClickReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(final Context context, final Intent intent) {
    long programID = intent.getLongExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, -1);
    
    if(intent.hasExtra(SettingConstants.CHANNEL_ID_EXTRA) && intent.hasExtra(SettingConstants.START_TIME_EXTRA)) {
      Intent startTVB = new Intent(context, TvBrowser.class);
      startTVB.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startTVB.putExtra(SettingConstants.CHANNEL_ID_EXTRA, intent.getExtras().getInt(SettingConstants.CHANNEL_ID_EXTRA));
      startTVB.putExtra(SettingConstants.START_TIME_EXTRA, intent.getExtras().getLong(SettingConstants.START_TIME_EXTRA));
      startTVB.putExtra(SettingConstants.NO_BACK_STACKUP_EXTRA, true);
      
      context.getApplicationContext().startActivity(startTVB);
      
      
      /*
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          final Intent send = new Intent(SettingConstants.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT);
          send.putExtra(SettingConstants.CHANNEL_ID_EXTRA, intent.getExtras().getInt(SettingConstants.CHANNEL_ID_EXTRA));
          send.putExtra(SettingConstants.START_TIME_EXTRA, intent.getExtras().getLong(SettingConstants.START_TIME_EXTRA));
          send.putExtra(SettingConstants.NO_BACK_STACKUP_EXTRA, true);
          
          LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(send);
        }
      }, 1000);*/
    }
    else if(programID >= 0) {
      Intent startInfo = new Intent(context, InfoActivity.class);
      startInfo.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
      startInfo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      context.startActivity(startInfo);
    }
  }
}
