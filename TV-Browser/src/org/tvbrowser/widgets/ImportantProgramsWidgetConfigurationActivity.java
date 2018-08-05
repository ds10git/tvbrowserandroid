package org.tvbrowser.widgets;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Set;

public class ImportantProgramsWidgetConfigurationActivity extends Activity {
  private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
  
  private Spinner mTypeSelection;
  private EditText mName;
  private CheckBox mMarked;
  private CheckBox mFavorite;
  private CheckBox mReminder;
  private CheckBox mSync;
  private CheckBox mLimit;
  
  private EditText mLimitNumber;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    PrefUtils.initialize(ImportantProgramsWidgetConfigurationActivity.this);

    setTheme(UiUtils.getThemeResourceId(UiUtils.TYPE_THEME_DEFAULT, PrefUtils.isDarkTheme()));

    setContentView(R.layout.important_programs_widget_configuration);
        
    final View titleLabel = findViewById(R.id.important_programs_widget_config_name_label);
    final View dividerLabel = findViewById(R.id.important_programs_widget_config_shown_selection_label);
    final View divider = findViewById(R.id.important_programs_widget_config_shown_selection_label_devider);
    
    mTypeSelection = findViewById(R.id.important_programs_widget_config_selection_type);
    mName = findViewById(R.id.important_programs_widget_config_name_value);
    mMarked = findViewById(R.id.important_programs_widget_config_show_marked);
    mFavorite = findViewById(R.id.important_programs_widget_config_show_favorite);
    mReminder = findViewById(R.id.important_programs_widget_config_show_reminder);
    mSync = findViewById(R.id.important_programs_widget_config_show_synchronized);
    mLimit = findViewById(R.id.important_programs_widget_config_limit_selection);
    mLimitNumber = findViewById(R.id.important_programs_widget_config_limit_selection_edit);
    mLimitNumber.setText(String.valueOf(15));

    UiUtils.createAdapterForSpinner(ImportantProgramsWidgetConfigurationActivity.this, mTypeSelection, R.array.widget_important_config_selection_type_entries);

    mLimit.setOnCheckedChangeListener((buttonView, isChecked) -> mLimitNumber.setEnabled(isChecked));
    
    mTypeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int visibility = View.VISIBLE;
        
        if(position == 1) {
          setTitle(R.string.title_programs_list);
          visibility = View.GONE;
        }
        else {
          setTitle(R.string.widget_important_default_title);
        }
        
        titleLabel.setVisibility(visibility);
        dividerLabel.setVisibility(visibility);
        divider.setVisibility(visibility);
        mName.setVisibility(visibility);
        mMarked.setVisibility(visibility);
        mFavorite.setVisibility(visibility);
        mReminder.setVisibility(visibility);
        mSync.setVisibility(visibility);
        mLimit.setVisibility(visibility);
        mLimitNumber.setVisibility(visibility);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
    });
    
    //mMarked.requestFocusFromTouch();
    
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    
    if(extras != null) {
      Log.d("info25",""+intent.getAction());
      Set<String> keys = extras.keySet();
      for(String key : keys) {
        Log.d("info25",key + "="+extras.get(key));
      }

      mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
      
      if(mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ImportantProgramsWidgetConfigurationActivity.this);
      
        int typeIndex = pref.getInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_TYPE), getResources().getInteger(R.integer.widget_config_important_type_index_default));
        
        mTypeSelection.setSelection(typeIndex);
        
        mName.setText(pref.getString(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME), getString(R.string.widget_important_default_title)));
        mMarked.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED), true));
        mFavorite.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE), true));
        mReminder.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER), true));
        mSync.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED), true));
        mLimit.setChecked(pref.getBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT), false));
        mLimitNumber.setText(String.valueOf(pref.getInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT), 15)));
        mLimitNumber.setEnabled(mLimit.isChecked());
      }
    }
    
    setResult(RESULT_CANCELED, null);

    if(CompatUtils.showWidgetRefreshInfo()) {
      Intent info = new Intent(getApplicationContext(), InfoActivity.class);
      info.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
      info.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,mAppWidgetId);
      info.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(info);
    }
  }
  
  public void cancel(View view) {
    finish();
  }
  
  public void completeConfiguration(View view) {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(ImportantProgramsWidgetConfigurationActivity.this).edit();
    
    int type = mTypeSelection.getSelectedItemPosition();
    
    edit.putInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_TYPE), type);
    
    if(type == 0) {
      edit.putString(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME), mName.getText().toString());
      edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED), mMarked.isChecked());
      edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE), mFavorite.isChecked());
      edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER), mReminder.isChecked());
      edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED), mSync.isChecked());
      edit.putBoolean(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT), mLimit.isChecked());
      
      if(mLimitNumber.getText().toString().trim().length() > 0) {
        try {
          int value = Integer.parseInt(mLimitNumber.getText().toString());
          edit.putInt(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT), value);  
        }catch(NumberFormatException ignored) {}
      }
    }
    else {
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_NAME));
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_MARKED));
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_FAVORITE));
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_REMINDER));
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_SHOWN_SYNCHRONIZED));
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT));
      edit.remove(mAppWidgetId+"_"+getString(R.string.WIDGET_CONFIG_IMPORTANT_LIMIT_COUNT));
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
