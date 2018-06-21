/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.tvbrowser;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.tvbrowser.filter.ChannelFilter;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;
import org.tvbrowser.widgets.RunningProgramsListWidget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class InfoActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    PrefUtils.initialize(InfoActivity.this);
  }
  
  private ViewGroup mViewParent;
  
  @Override
  public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
    mViewParent = (ViewGroup)parent;
    return super.onCreateView(parent, name, context, attrs);
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    Intent intent = getIntent();
    
    long programID = intent.getLongExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, -1);
    
    if(programID >= 0) {
      UiUtils.showProgramInfo(this, programID, this, getCurrentFocus(), new Handler());
    }
    else if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
      final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(InfoActivity.this);
      final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
      
      final ArrayList<Integer> values = new ArrayList<>();
      
      int[] defaultValues = getResources().getIntArray(R.array.time_button_defaults);
      
      int timeButtonCount = pref.getInt(getString(R.string.TIME_BUTTON_COUNT),getResources().getInteger(R.integer.time_button_count_default));
      
      int currentValue = pref.getInt(appWidgetId + "_" + getString(R.string.WIDGET_CONFIG_RUNNING_TIME), getResources().getInteger(R.integer.widget_config_running_time_default));
      
      for(int i = 1; i <= Math.min(timeButtonCount, getResources().getInteger(R.integer.time_button_count_default)); i++) {
        try {
          Class<?> string = R.string.class;
          
          Field setting = string.getDeclaredField("TIME_BUTTON_" + i);
          
          Integer value = pref.getInt(getResources().getString((Integer) setting.get(string)), defaultValues[i - 1]);
          
          if(value >= -1 && !values.contains(value)) {
            values.add(value);
          }
        } catch (Exception ignored) {}
      }
      
      for(int i = 7; i <= timeButtonCount; i++) {
          Integer value = pref.getInt("TIME_BUTTON_" + i, 0);
          
          if(value >= -1 && !values.contains(value)) {
            values.add(value);
          }
      }
      
      if(PrefUtils.getBooleanValue(R.string.SORT_RUNNING_TIMES, R.bool.sort_running_times_default)) {
        Collections.sort(values);
      }
      
      final boolean hasNext = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
      final int indexOffset = hasNext ? 1 : 0;
      
      int selection = 0;
      
      if(currentValue == -2 && hasNext) {
        selection = 1;
      }
      
      for(int i = 0; i < values.size(); i++) {
        if(values.get(i) == currentValue) {
          selection = i+1+indexOffset;
          break;
        }
      }
      
      ArrayList<String> formatedTimes = new ArrayList<>();
      formatedTimes.add(getString(R.string.button_now));
      
      if(hasNext) {
        formatedTimes.add(getString(R.string.button_after));
      }
      
      for(int i = 0; i < values.size(); i++) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, values.get(i) / 60);
        cal.set(Calendar.MINUTE, values.get(i) % 60);
        
        formatedTimes.add(DateFormat.getTimeFormat(InfoActivity.this).format(cal.getTime()));
      }
      
      AlertDialog.Builder builder = new AlertDialog.Builder(InfoActivity.this);
      
      builder.setTitle(R.string.widget_running_select_time_title);
      
      builder.setSingleChoiceItems(formatedTimes.toArray(new String[formatedTimes.size()]), selection, (dialog, which) -> {
        int value = -1;

        if(which == 1 && hasNext) {
          value = -2;
        }
        else if(which > 1 || (!hasNext && which > 0)) {
          value = values.get(which-1-indexOffset);
        }

        Editor edit = pref.edit();
        edit.putInt(appWidgetId + "_" + getString(R.string.WIDGET_CONFIG_RUNNING_TIME), value);
        edit.commit();

        Intent update = new Intent(getApplicationContext(), RunningProgramsListWidget.class);
        update.setAction(SettingConstants.UPDATE_RUNNING_APP_WIDGET);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        sendBroadcast(update);

        dialog.dismiss();
        finish();
      });
      builder.setOnCancelListener(dialog -> finish());
      
      if(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        builder.show();
      }
      else {
        finish();
      }
    }
    else if(intent.hasExtra(SettingConstants.WIDGET_CHANNEL_SELECTION_EXTRA)) {
      final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(InfoActivity.this);
      final int appWidgetId = intent.getIntExtra(SettingConstants.WIDGET_CHANNEL_SELECTION_EXTRA, AppWidgetManager.INVALID_APPWIDGET_ID);
      
      if(SettingConstants.IS_DARK_THEME) {
        setTheme(R.style.AppDarkTheme);
      }
      else {
        setTheme(R.style.AppTheme);
      }
      
      UiUtils.showChannelFilterSelection(InfoActivity.this, new ChannelFilter() {
        @Override
        public void setFilterValues(String name, int[] filteredChannelIds) {
          StringBuilder value = new StringBuilder();
          
          if(filteredChannelIds != null) {
            for(int i = 0; i < filteredChannelIds.length-1; i++) {
              value.append(filteredChannelIds[i]).append(",");
            }
            
            if(filteredChannelIds.length > 0) {
              value.append(String.valueOf(filteredChannelIds[filteredChannelIds.length - 1]));
            }
          }
          
          Editor edit = pref.edit();
          edit.putString(appWidgetId+"_"+getString(R.string.WIDGET_CONFIG_PROGRAM_LIST_CHANNELS), value.toString());
          edit.commit();
          
          AppWidgetManager.getInstance(getApplicationContext()).notifyAppWidgetViewDataChanged(appWidgetId, R.id.important_widget_list_view);
          
          finish();
        }
        
        @Override
        public String getName() {
          return null;
        }
        
        @Override
        public int[] getFilteredChannelIds() {
          String values = pref.getString(appWidgetId+"_"+getString(R.string.WIDGET_CONFIG_PROGRAM_LIST_CHANNELS), "");
          
          String[] parts = values.split(",");
          
          
          int[] result = new int[values.trim().length() > 0 ? parts.length : 0];
          
          for(int i = 0; i < result.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
          }
          
          return result;
        }
      }, mViewParent, this::finish);
    }
    else {
      finish();
    }
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
    setIntent(intent);
  }
}
