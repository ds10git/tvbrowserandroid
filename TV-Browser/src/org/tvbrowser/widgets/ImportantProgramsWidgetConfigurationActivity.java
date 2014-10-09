package org.tvbrowser.widgets;

import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class ImportantProgramsWidgetConfigurationActivity extends Activity {
  private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
  
  private EditText mName;
  private CheckBox mMarked;
  private CheckBox mFavorite;
  private CheckBox mReminder;
  private CheckBox mCalendar;
  private CheckBox mSync;
  private CheckBox mLimit;
  
  private EditText mLimitNumber;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    PrefUtils.initialize(ImportantProgramsWidgetConfigurationActivity.this);
    
    if(PrefUtils.getBooleanValue(R.string.DARK_STYLE, R.bool.dark_style_default)) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    setContentView(R.layout.important_programs_widget_configuration);
    
    mName = (EditText)findViewById(R.id.important_programs_widget_config_name_value);
    mMarked = (CheckBox)findViewById(R.id.important_programs_widget_config_show_marked);
    mFavorite = (CheckBox)findViewById(R.id.important_programs_widget_config_show_favorite);
    mReminder = (CheckBox)findViewById(R.id.important_programs_widget_config_show_reminder);
    mCalendar = (CheckBox)findViewById(R.id.important_programs_widget_config_show_calendar);
    mSync = (CheckBox)findViewById(R.id.important_programs_widget_config_show_synchronized);
    mLimit = (CheckBox)findViewById(R.id.important_programs_widget_config_limit_selection);
    mLimitNumber = (EditText)findViewById(R.id.important_programs_widget_config_limit_selection_edit);
    mLimitNumber.setText(String.valueOf(15));
    
    mLimit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mLimitNumber.setEnabled(isChecked);
      }
    });
    
    mMarked.requestFocusFromTouch();
    
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    
    if(extras != null) {
      mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
      
      if(mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ImportantProgramsWidgetConfigurationActivity.this);
      
        mName.setText(pref.getString(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME), getString(R.string.widget_important_default_title)));
        mMarked.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED), true));
        mFavorite.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE), true));
        mReminder.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER), true));
        mCalendar.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_CALENDER), true));
        mSync.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED), true));
        mLimit.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT), false));
        mLimitNumber.setText(String.valueOf(pref.getInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT), 15)));
        mLimitNumber.setEnabled(mLimit.isChecked());
      }
    }
    
    setResult(RESULT_CANCELED, null);
  }
  
  public void cancel(View view) {
    finish();
  }
  
  public void completeConfiguration(View view) {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(ImportantProgramsWidgetConfigurationActivity.this).edit();
    
    edit.putString(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME), mName.getText().toString());
    edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED), mMarked.isChecked());
    edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE), mFavorite.isChecked());
    edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER), mReminder.isChecked());
    edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_CALENDER), mCalendar.isChecked());
    edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED), mSync.isChecked());
    edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT), mLimit.isChecked());
    
    if(mLimitNumber.getText().toString().trim().length() > 0) {
      try {
        int value = Integer.parseInt(mLimitNumber.getText().toString());
        edit.putInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT), value);  
      }catch(NumberFormatException e) {}
    }
    
    edit.commit();
    
    Intent result = new Intent();
    
    result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    setResult(RESULT_OK,result);
    
    if(mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      Intent update = new Intent(SettingConstants.UPDATE_IMPORTANT_APP_WIDGET);
      update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      
      sendBroadcast(update);
    }
 /*   AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
    
    appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.important_widget_header);*/
    //appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.important_widget_list_view);
     
    finish();
  }
}
