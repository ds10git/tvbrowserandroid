package org.tvbrowser.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.PrefUtils;

public class ActivityConfigurationWidgetRunning extends AppCompatActivity {
  private int mAppWidgetId;
  private static final int REQUEST_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_configuration_widget_running);
    PrefUtils.initialize(ActivityConfigurationWidgetRunning.this);

    if(getIntent().hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
      mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

      Intent result = new Intent();

      result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getIntent().getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_ID));
      setResult(RESULT_OK, result);

      if (CompatUtils.showWidgetRefreshInfo()) {
        Intent info = new Intent(getApplicationContext(), InfoActivity.class);
        info.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        info.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        info.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(info,REQUEST_CODE);
      }
      else {
        finish();
      }
    }
    else {
      setResult(RESULT_OK);
      finish();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    finish();
  }
}
