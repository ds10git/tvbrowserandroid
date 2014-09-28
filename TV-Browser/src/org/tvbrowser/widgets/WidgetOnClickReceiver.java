package org.tvbrowser.widgets;

import java.util.Date;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.TvBrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class WidgetOnClickReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(final Context context, final Intent intent) {
    long programID = intent.getLongExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, -1);
    
    if(intent.hasExtra(SettingConstants.CHANNEL_ID_EXTRA) && intent.hasExtra(SettingConstants.START_TIME_EXTRA)) {
      Intent startTVB = new Intent(context, TvBrowser.class);
      startTVB.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      context.getApplicationContext().startActivity(startTVB);
      
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          final Intent send = new Intent(SettingConstants.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT);
          send.putExtra(SettingConstants.CHANNEL_ID_EXTRA, intent.getExtras().getInt(SettingConstants.CHANNEL_ID_EXTRA));
          send.putExtra(SettingConstants.START_TIME_EXTRA, intent.getExtras().getLong(SettingConstants.START_TIME_EXTRA));
          send.putExtra(SettingConstants.NO_BACK_STACKUP_EXTRA, true);
          
          LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(send);
        }
      }, 1000);
    }
    else if(programID >= 0) {
      Intent startInfo = new Intent(context, InfoActivity.class);
      startInfo.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
      startInfo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      context.startActivity(startInfo);
    }
  }
}
