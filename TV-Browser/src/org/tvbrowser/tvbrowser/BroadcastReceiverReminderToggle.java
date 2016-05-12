package org.tvbrowser.tvbrowser;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.UiUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiverReminderToggle extends BroadcastReceiver {
  public BroadcastReceiverReminderToggle() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("info8", "onReceive");
    
    SettingConstants.setReminderPaused(context, !SettingConstants.isReminderPaused(context));
    UiUtils.updateToggleReminderStateWidget(context);
  }
}
