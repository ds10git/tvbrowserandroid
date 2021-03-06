package org.tvbrowser.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.tvbrowser.App;

public class PreferenceNotificationChannel extends Preference implements Preference.OnPreferenceClickListener {
  private int mTypeId;

  @SuppressWarnings("unused")
  public PreferenceNotificationChannel(Context context) {
    super(context);
  }

  @SuppressWarnings("unused")
  public PreferenceNotificationChannel(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @SuppressWarnings("unused")
  public PreferenceNotificationChannel(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);

    mTypeId = Integer.parseInt(getSummary().toString());

    TextView summaryView = view.findViewById(android.R.id.summary);
    summaryView.setVisibility(View.GONE);
  }

  @TargetApi(Build.VERSION_CODES.O)
  @Override
  public boolean onPreferenceClick(Preference preference) {
    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName())
        .putExtra(Settings.EXTRA_CHANNEL_ID, App.get().getNotificationChannelId(mTypeId));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);

    return false;
  }
}