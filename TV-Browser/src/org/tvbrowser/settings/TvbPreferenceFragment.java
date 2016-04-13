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
package org.tvbrowser.settings;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.TvDataUpdateService;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;

public class TvbPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    String category = getArguments().getString(getString(R.string.pref_category_key));
        
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    if(getString(R.string.category_download).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_download);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_AUTO_UPDATE_TYPE));
    }
    else if(getString(R.string.category_epgpaid).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_epgpaid);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_EPGPAID_DOWNLOAD_MAX));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_EPGPAID_USER));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_EPGPAID_PASSWORD));
    }
    else if(getString(R.string.category_database).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_database);
      
      ListPreference path = (ListPreference)findPreference(getString(R.string.PREF_DATABASE_PATH));
      
      path.setEnabled(!TvDataUpdateService.isRunning());
      
      File external = Environment.getExternalStorageDirectory();
      
      final ArrayList<String> entries = new ArrayList<String>();
      final ArrayList<String> entryValues = new ArrayList<String>();
      
      String defaultValue = getString(R.string.pref_database_path_default);
      
      entries.add(getString(R.string.pref_database_selection_internal));
      entryValues.add(defaultValue);
      
      String currentValue = pref.getString(getString(R.string.PREF_DATABASE_PATH), defaultValue);
      String summary = getString(R.string.pref_database_selection_unavailable);
      
      if(currentValue.equals(defaultValue)) {
        summary = entries.get(0);
      }
      
      if(external != null && external.isDirectory()) {
        File[] sdcards = new File(external.getAbsolutePath().toString().substring(0, external.getAbsolutePath().indexOf(File.separator, 1))).listFiles(new FileFilter() {
          @Override
          public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().toLowerCase().contains("sdcard");
          }
        });
        
        File appExternal = getActivity().getExternalFilesDir(null);
        String appFilePathPart = appExternal.getAbsolutePath().replace(external.getAbsolutePath(), "") + File.separator;
        
        Arrays.sort(sdcards);
        
        for(File sdcard : sdcards) {
          File test = new File(sdcard,appFilePathPart);
          
          if(test.isDirectory() || test.mkdirs()) {
            entries.add(sdcard.getAbsolutePath());
            entryValues.add(test.getAbsolutePath());
            
            if(test.getAbsolutePath().equals(currentValue)) {
              summary = sdcard.getAbsolutePath();
            }
          }
        }
      }
      
      path.setEntries(entries.toArray(new String[entries.size()]));
      path.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
      path.setSummary(summary);
    }
    else if(getString(R.string.category_start).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_start);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.TAB_TO_SHOW_AT_START));
    }
    else if(getString(R.string.category_theme).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_layout);
      
      onSharedPreferenceChanged(pref, getString(R.string.PREF_SHOW_PROGRESS));
      onSharedPreferenceChanged(pref, getString(R.string.PREF_COLOR_STYLE));
      onSharedPreferenceChanged(pref, getString(R.string.PREF_LOGO_BORDER));
    }
    else if(getString(R.string.category_reminder).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_reminder);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_TIME));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_SOUND_VALUE));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE));

      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_MONDAY_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_ACTIVATED));
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_ACTIVATED));
    }
    else if(getString(R.string.category_time_buttons).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_time_buttons);
      onSharedPreferenceChanged(pref,getString(R.string.TIME_BUTTON_COUNT));
    }
    else if(getString(R.string.category_running_programs).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_running);
    }
    else if(getString(R.string.category_programs_list).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_programs_list);
    }
    else if(getString(R.string.category_program_table).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_program_table);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PROG_TABLE_ACTIVATED));
    }
    else if(getString(R.string.category_list).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_program_lists);
    }
    else if(getString(R.string.category_details).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_details);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.SHOW_PICTURE_IN_DETAILS));
    }
    else if(getString(R.string.category_news).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_news);
      
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_NEWS_SHOW));
    }
    else if(getString(R.string.category_widgets).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_widgets);
    }
    else if(getString(R.string.category_sync).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_sync);
    }
    else if(getString(R.string.category_additional_infos).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_additonal_infos);
    }
    else if(getString(R.string.category_i_dont_want_to_see).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_i_dont_want_to_see);
      onSharedPreferenceChanged(pref,getResources().getString(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE));
    }
    else if(getString(R.string.category_debug).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_debug);
    }
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    
    PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(getActivity() != null) {
      if(key.equals(getString(R.string.PREF_LOGO_BORDER))) {
        CheckBoxPreference transparent = (CheckBoxPreference)findPreference(key);
        ColorPreference borderColor = (ColorPreference)findPreference(getString(R.string.PREF_LOGO_BORDER_COLOR));
        
        if(transparent != null && borderColor != null) {
          borderColor.setEnabled(transparent.isChecked());
        }
      }
      else if(key.equals(getString(R.string.PREF_EPGPAID_USER))) {
        String userName = sharedPreferences.getString(key, null);
        
        EditTextPreference textPref = (EditTextPreference)findPreference(key);
        
        if(textPref != null && userName != null && userName.trim().length() > 0) {
          textPref.setSummary(userName);
        }
        else if(textPref != null) {
          textPref.setSummary(getString(R.string.pref_epgpaid_user_empty_summary));
        }
      }
      else if(key.equals(getString(R.string.PREF_EPGPAID_PASSWORD))) {
        String password = sharedPreferences.getString(key, null);
        
        EditTextPreference textPref = (EditTextPreference)findPreference(key);
        
        if(textPref != null && password != null && password.trim().length() > 0) {
          textPref.setSummary(getString(R.string.pref_epgpaid_user_password_set_summary));
        }
        else if(textPref != null) {
          textPref.setSummary(getString(R.string.pref_epgpaid_user_empty_summary));
        }
      }
      else if(key.equals(getString(R.string.PREF_AUTO_UPDATE_START_TIME))) {
        Editor edit = sharedPreferences.edit();
        edit.putLong(getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), 0);
        edit.commit();
        
        IOUtils.handleDataUpdatePreferences(getActivity());
      }
      else if(key.equals(getString(R.string.PREF_NEWS_SHOW))) {
        CheckBoxPreference showNews = (CheckBoxPreference)findPreference(key);
        ListPreference newsType = (ListPreference)findPreference(getString(R.string.PREF_NEWS_TYPE));
        
        if(showNews != null && newsType != null) {
          newsType.setEnabled(showNews.isChecked());
        }
      }
      else if(key.equals(getString(R.string.TIME_BUTTON_COUNT))) {
        PreferenceScreen screen = (PreferenceScreen)findPreference(getString(R.string.TIME_BUTTON_PREFERENCES_SUB_SCREEN));
        
        if(screen != null) {
          int timeButtonCount = sharedPreferences.getInt(key, getResources().getInteger(R.integer.time_button_count_default));
          
          int currentTimeButtonCount = screen.getPreferenceCount() - 1;
          
          for(int i = currentTimeButtonCount; i > timeButtonCount; i--) {
            screen.removePreference(screen.getPreference(i));
          }
          
          for(int i = currentTimeButtonCount + 1; i <= timeButtonCount; i++) {
            TimePreference timePref = new TimePreference(getActivity(), null);
            
            String index = String.valueOf(i);
            int defaultValue = 0;
            
            switch(i) {
              case 2: index = "TWO"; defaultValue = getResources().getInteger(R.integer.time_button_2_default);break;
              case 3: index = "THREE"; defaultValue = getResources().getInteger(R.integer.time_button_3_default);break;
              case 4: index = "FOUR"; defaultValue = getResources().getInteger(R.integer.time_button_4_default);break;
              case 5: index = "FIVE"; defaultValue = getResources().getInteger(R.integer.time_button_5_default);break;
              case 6: index = "SIX"; defaultValue = getResources().getInteger(R.integer.time_button_6_default);break;
            }
            
            timePref.setDefaultValue(defaultValue);
            timePref.setKey(getString(R.string.time_button_key_prefix) + index);
            timePref.setSummary(R.string.pref_time_button_hint);
            timePref.onSetInitialValue(true, defaultValue);
            
            screen.addPreference(timePref);
          }
        }
      }
      else if(key.equals(getString(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS)) || key.equals(getString(R.string.PREF_WIDGET_SIMPLE_ICON))) {
        UiUtils.reloadWidgets(getActivity().getApplicationContext());
      }
      else if(key.equals(getString(R.string.PREF_REMINDER_SOUND_VALUE)) || key.equals(getString(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE))
          || key.equals(getString(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE))) {
        Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        String defaultValue = null;
        
        if(key.equals(getString(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE))) {
          defaultValue = getString(R.string.pref_reminder_night_mode_sound_value_default);
        }
        else if(key.equals(getString(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE))) {
          defaultValue = getString(R.string.pref_reminder_work_mode_sound_value_default);
        }
        
        String tone = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(key, defaultValue);
        
        Uri sound = defaultUri;
        
        if(tone != null) {
          sound = Uri.parse(tone);
        }
        
        RingtonePreference ringtone = (RingtonePreference)findPreference(key);
        
        if(ringtone != null) {
          Ringtone notification = RingtoneManager.getRingtone(getActivity(), sound);
          
          if(notification != null && (tone == null || tone.trim().length() > 0)) {
            ringtone.setTitle(notification.getTitle(getActivity()));
          }
          else {
            ringtone.setTitle(R.string.pref_reminder_no_sound);
          }
        }
      }
      else if(key.equals(getResources().getString(R.string.DAYS_TO_DOWNLOAD)) 
          || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAMS_LIST))
          || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE))
          || key.equals(getResources().getString(R.string.DETAIL_PICTURE_ZOOM))
          || key.equals(getResources().getString(R.string.TAB_TO_SHOW_AT_START))
          || key.equals(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE))
          || key.equals(getResources().getString(R.string.PREF_RUNNING_DIVIDER_SIZE))
          || key.equals(getResources().getString(R.string.PREF_PROGRAM_LISTS_VERTICAL_PADDING_SIZE))
          || key.equals(getResources().getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE))
          || key.equals(getResources().getString(R.string.PREF_REMINDER_TIME))
          || key.equals(getResources().getString(R.string.PREF_REMINDER_TIME_SECOND))
          || key.equals(getResources().getString(R.string.DETAIL_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PREF_PROGRAM_LISTS_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PROG_TABLE_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_TYPE))
          || key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_RANGE))
          || key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_FREQUENCY))
          || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_RUNNING))
          || key.equals(getResources().getString(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE))
          || key.equals(getResources().getString(R.string.SHOW_DATE_FOR_PROGRAMS_LIST))
          || key.equals(getResources().getString(R.string.SHOW_CHANNEL_FOR_PROGRAMS_LIST))
          || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_LISTS))
          || key.equals(getResources().getString(R.string.PREF_COLOR_STYLE))
          || key.equals(getResources().getString(R.string.PREF_WIDGET_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PREF_WIDGET_CHANNEL_LOGO_NAME))
          || key.equals(getResources().getString(R.string.PREF_WIDGET_LISTS_DIVIDER_SIZE))
          || key.equals(getResources().getString(R.string.PREF_WIDGET_VERTICAL_PADDING_SIZE))
          || key.equals(getResources().getString(R.string.PREF_FAVORITE_TAB_LAYOUT))
          || key.equals(getResources().getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_HEADER))
          || key.equals(getResources().getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_LIST))
          || key.equals(getResources().getString(R.string.PREF_NEWS_TYPE))
          || key.equals(getResources().getString(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION))
          || key.equals(getResources().getString(R.string.PREF_AUTO_CHANNEL_UPDATE_FREQUENCY))
          || key.equals(getResources().getString(R.string.PREF_DATABASE_PATH))
          || key.equals(getResources().getString(R.string.PREF_REMINDER_PRIORITY_VALUE))
          || key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_PRIORITY_VALUE))
          || key.equals(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_PRIORITY_VALUE))
          || key.equals(getResources().getString(R.string.PREF_EPGPAID_DOWNLOAD_MAX))
          ) {
        ListPreference lp = (ListPreference) findPreference(key);
        
        if(lp != null) {
          lp.setSummary("dummy"); // required or will not update
          
          String value = String.valueOf(lp.getEntry());
          
          if(value.endsWith("%")) {
            value += "%";
          }
          
          lp.setSummary(value);
        }
        
        if(key.equals(getResources().getString(R.string.PREF_WIDGET_LISTS_DIVIDER_SIZE)) || 
            key.equals(getResources().getString(R.string.PREF_WIDGET_VERTICAL_PADDING_SIZE)) ||
            key.equals(getResources().getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_HEADER)) || 
            key.equals(getResources().getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_LIST))) {
          UiUtils.reloadWidgets(getActivity());
        }
        if(key.equals(getResources().getString(R.string.PREF_COLOR_STYLE))) {
          ListPreference currentStyle = (ListPreference)findPreference(key);
          CheckBoxPreference showProgress = (CheckBoxPreference) findPreference(getString(R.string.PREF_SHOW_PROGRESS));
          
          ColorPreference onAirBackground = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND));
          ColorPreference onAirProgress = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_ON_AIR_PROGRESS));
          ColorPreference marked = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_MARKED));
          ColorPreference markedFavorite = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_FAVORITE));
          ColorPreference markedReminder = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_REMINDER));
          ColorPreference markedSync = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_SYNC));
          
          if(onAirBackground != null && onAirProgress != null && marked != null && markedFavorite != null
              && markedReminder != null && markedSync != null && showProgress != null) {
            int currentStyleValue = 0;
            
            if(currentStyle != null && currentStyle.getValue() != null) {
              currentStyleValue = Integer.parseInt(currentStyle.getValue());
            }
            
            if(currentStyleValue == 1) {
              int color = getResources().getColor(R.color.pref_color_on_air_background_tvb_style_default);
              onAirBackground.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_on_air_progress_tvb_style_default);
              onAirProgress.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_tvb_style_default);
              marked.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_favorite_tvb_style_default);
              markedFavorite.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_reminder_tvb_style_default);
              markedReminder.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_sync_tvb_style_favorite_default);
              markedSync.setColors(color, color);
              
              
              if(sharedPreferences.getInt(getString(R.string.PREF_RUNNING_TIME_SELECTION), getResources().getColor(R.color.pref_color_running_time_selection_background_glow_style_default)) == getResources().getColor(R.color.pref_color_running_time_selection_background_glow_style_default)) {
                Editor edit = (Editor)sharedPreferences.edit();
                edit.putInt(getString(R.string.PREF_RUNNING_TIME_SELECTION), getResources().getColor(R.color.pref_color_running_time_selection_background_tvb_style_default));
                edit.commit();
              }
            }
            else if(currentStyleValue == 2) {
              int color = getResources().getColor(R.color.pref_color_on_air_background_glow_style_default);
              onAirBackground.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_on_air_progress_glow_style_default);
              onAirProgress.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_glow_style_default);
              marked.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_favorite_glow_style_default);
              markedFavorite.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_reminder_glow_style_default);
              markedReminder.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_sync_glow_style_favorite_default);
              markedSync.setColors(color, color);
              
              if(sharedPreferences.getInt(getString(R.string.PREF_RUNNING_TIME_SELECTION), getResources().getColor(R.color.pref_color_running_time_selection_background_tvb_style_default)) == getResources().getColor(R.color.pref_color_running_time_selection_background_tvb_style_default)) {
                Editor edit = (Editor)sharedPreferences.edit();
                edit.putInt(getString(R.string.PREF_RUNNING_TIME_SELECTION), getResources().getColor(R.color.pref_color_running_time_selection_background_glow_style_default));
                edit.commit();
              }
            }
            else if(currentStyleValue == 3) {
              int color = getResources().getColor(R.color.pref_color_on_air_background_decent_dark_style_default);
              onAirBackground.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_on_air_progress_decent_dark_style_default);
              onAirProgress.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_decent_dark_style_default);
              marked.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_favorite_decent_dark_style_default);
              markedFavorite.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_reminder_decent_dark_style_default);
              markedReminder.setColors(color, color);
              color = getResources().getColor(R.color.pref_color_mark_sync_decent_dark_style_favorite_default);
              markedSync.setColors(color, color);
              
              if(sharedPreferences.getInt(getString(R.string.PREF_RUNNING_TIME_SELECTION), getResources().getColor(R.color.pref_color_running_time_selection_background_tvb_style_default)) == getResources().getColor(R.color.pref_color_running_time_selection_background_tvb_style_default)) {
                Editor edit = (Editor)sharedPreferences.edit();
                edit.putInt(getString(R.string.PREF_RUNNING_TIME_SELECTION), getResources().getColor(R.color.pref_color_running_time_selection_background_decent_dark_style_default));
                edit.commit();
              }
            }
            else if(currentStyleValue == 0) {
              int color = sharedPreferences.getInt(getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND_USER_DEFINED), getResources().getColor(R.color.pref_color_on_air_background_tvb_style_default));
              onAirBackground.setColors(color, color);
              color = sharedPreferences.getInt(getString(R.string.PREF_COLOR_ON_AIR_PROGRESS_USER_DEFINED), getResources().getColor(R.color.pref_color_on_air_progress_tvb_style_default));
              onAirProgress.setColors(color, color);
              color = sharedPreferences.getInt(getString(R.string.PREF_COLOR_MARKED_USER_DEFINED), getResources().getColor(R.color.pref_color_mark_tvb_style_default));
              marked.setColors(color, color);
              color = sharedPreferences.getInt(getString(R.string.PREF_COLOR_FAVORITE_USER_DEFINED), getResources().getColor(R.color.pref_color_mark_favorite_tvb_style_default));
              markedFavorite.setColors(color, color);
              color = sharedPreferences.getInt(getString(R.string.PREF_COLOR_REMINDER_USER_DEFINED), getResources().getColor(R.color.pref_color_mark_reminder_tvb_style_default));
              markedReminder.setColors(color, color);
              color = sharedPreferences.getInt(getString(R.string.PREF_COLOR_SYNC_USER_DEFINED), getResources().getColor(R.color.pref_color_mark_sync_tvb_style_favorite_default));
              markedSync.setColors(color, color);
            }
            
            onAirBackground.setEnabled(currentStyle.getValue().equals("0") && showProgress.isChecked());
            onAirProgress.setEnabled(onAirBackground.isEnabled());
            marked.setEnabled(currentStyle.getValue().equals("0"));
            markedFavorite.setEnabled(marked.isEnabled());
            markedReminder.setEnabled(marked.isEnabled());
            markedSync.setEnabled(marked.isEnabled());
          }
        }
        else if(key.equals(getString(R.string.PREF_REMINDER_TIME)) || key.equals(getString(R.string.PREF_REMINDER_TIME_SECOND))) {
          ListPreference reminderTime = (ListPreference) findPreference(getString(R.string.PREF_REMINDER_TIME));
          ListPreference reminderTimeSecond = (ListPreference) findPreference(getString(R.string.PREF_REMINDER_TIME_SECOND));
          
          if(reminderTime != null && reminderTimeSecond != null 
              && reminderTime.getValue() != null && reminderTimeSecond.getValue() != null 
              && reminderTime.getValue().equals(reminderTimeSecond.getValue())) {
            reminderTimeSecond.setValue(getString(R.string.pref_reminder_time_second_default));
          }
        }
        else if(key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_TYPE))) {
          ListPreference type = (ListPreference)findPreference(getResources().getString(R.string.PREF_AUTO_UPDATE_TYPE));
          ListPreference range = (ListPreference)findPreference(getResources().getString(R.string.PREF_AUTO_UPDATE_RANGE));
          ListPreference frequency = (ListPreference)findPreference(getResources().getString(R.string.PREF_AUTO_UPDATE_FREQUENCY));
          
          CheckBoxPreference wifi = (CheckBoxPreference)findPreference(getResources().getString(R.string.PREF_AUTO_UPDATE_ONLY_WIFI));
          TimePreference startTime = (TimePreference)findPreference(getResources().getString(R.string.PREF_AUTO_UPDATE_START_TIME));
          
          if(type != null) {
            boolean noAutoUpdate = type.getValue().equals("0");
            boolean timeRange = type.getValue().equals("2");
            
            range.setEnabled(!noAutoUpdate);
            frequency.setEnabled(!noAutoUpdate);
            wifi.setEnabled(!noAutoUpdate);
            
            startTime.setEnabled(timeRange);
          }
        }
        else if(key.equals(getResources().getString(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE))) {
          ListPreference dontWantToSeeType = (ListPreference)findPreference(key);
          
          if(dontWantToSeeType != null) {            
            findPreference(getResources().getString(R.string.PREF_I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR)).setEnabled(dontWantToSeeType.getValue().equals(getResources().getStringArray(R.array.pref_simple_string_value_array2)[1]));
          }
        }
      }
      else if(key.equals(getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND))) {
        setUserColorValue(sharedPreferences,key,R.string.PREF_COLOR_ON_AIR_BACKGROUND_USER_DEFINED);
      }
      else if(key.equals(getString(R.string.PREF_COLOR_ON_AIR_PROGRESS))) {
        setUserColorValue(sharedPreferences,key,R.string.PREF_COLOR_ON_AIR_PROGRESS_USER_DEFINED);
      }
      else if(key.equals(getString(R.string.PREF_COLOR_MARKED))) {
        setUserColorValue(sharedPreferences,key,R.string.PREF_COLOR_MARKED_USER_DEFINED);
      }
      else if(key.equals(getString(R.string.PREF_COLOR_FAVORITE))) {
        setUserColorValue(sharedPreferences,key,R.string.PREF_COLOR_FAVORITE_USER_DEFINED);
      }
      else if(key.equals(getString(R.string.PREF_COLOR_REMINDER))) {
        setUserColorValue(sharedPreferences,key,R.string.PREF_COLOR_REMINDER_USER_DEFINED);
      }
      else if(key.equals(getString(R.string.PREF_COLOR_SYNC))) {
        setUserColorValue(sharedPreferences,key,R.string.PREF_COLOR_SYNC_USER_DEFINED);
      }
      else if(key.equals(getResources().getString(R.string.PROG_TABLE_ACTIVATED))) {
        CheckBoxPreference progTable = (CheckBoxPreference) findPreference(key);
        
        CheckBoxPreference progTableDelayed = (CheckBoxPreference) findPreference(getString(R.string.PROG_TABLE_DELAYED));
        
        ListPreference blockSize = (ListPreference) findPreference(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE));
        CheckBoxPreference spreadOverBlocks = (CheckBoxPreference) findPreference(getResources().getString(R.string.PROG_PANEL_GROW));
        
        ListPreference channelLogoName = (ListPreference) findPreference(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE));
        ListPreference layout = (ListPreference) findPreference(getResources().getString(R.string.PROG_TABLE_LAYOUT));
        CheckBoxPreference pictures = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE));
        CheckBoxPreference genre = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_GENRE_IN_PROGRAM_TABLE));
        CheckBoxPreference episode = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_EPISODE_IN_PROGRAM_TABLE));
        CheckBoxPreference infos = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_INFO_IN_PROGRAM_TABLE));
        CheckBoxPreference showOrderNumber = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_SORT_NUMBER_IN_PROGRAM_TABLE));
        ListPreference zoomText = (ListPreference) findPreference(getResources().getString(R.string.PROG_TABLE_TEXT_SCALE));
        
        ColumnWidthPreference columnWidth = (ColumnWidthPreference) findPreference(getResources().getString(R.string.PROG_TABLE_COLUMN_WIDTH));
              
        boolean isTimeBlock = layout == null || layout.getValue() == null || layout.getValue().equals("0");
        
        if(progTable != null) {
          if(progTable.isChecked()) {
            AlertDialog.Builder warning = new AlertDialog.Builder(getActivity());
            
            warning.setTitle(R.string.warning_title);
            warning.setMessage(R.string.pref_prog_table_activation_warning);
            
            warning.setPositiveButton(android.R.string.ok, null);
            warning.show();
          }
          
          if(progTableDelayed != null) {
            progTableDelayed.setEnabled(progTable.isChecked());
          }
          if(layout != null) {
            layout.setEnabled(progTable.isChecked());
          }
          if(blockSize != null) {
            blockSize.setEnabled(progTable.isChecked() && isTimeBlock);
          }
          if(spreadOverBlocks != null) {
            spreadOverBlocks.setEnabled(progTable.isChecked() && isTimeBlock);
          }
          if(channelLogoName != null) {
            channelLogoName.setEnabled(progTable.isChecked());
          }
          if(pictures != null) {
            pictures.setEnabled(progTable.isChecked());
          }
          if(columnWidth != null) {
            columnWidth.setEnabled(progTable.isChecked());
          }
          if(episode != null) {
            episode.setEnabled(progTable.isChecked());
          }
          if(genre != null) {
            genre.setEnabled(progTable.isChecked());
          }
          if(infos != null) {
            infos.setEnabled(progTable.isChecked());
          }
          if(showOrderNumber != null) {
            showOrderNumber.setEnabled(progTable.isChecked());
          }
          if(zoomText != null) {
            zoomText.setEnabled(progTable.isChecked());
          }
        }
      }
      else if(key.equals(getResources().getString(R.string.PROG_TABLE_LAYOUT))) {
        ListPreference layout = (ListPreference) findPreference(key);
        
        CheckBoxPreference progTable = (CheckBoxPreference) findPreference(getResources().getString(R.string.PROG_TABLE_ACTIVATED));
        
        ListPreference blockSize = (ListPreference) findPreference(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE));
        CheckBoxPreference spreadOverBlocks = (CheckBoxPreference) findPreference(getResources().getString(R.string.PROG_PANEL_GROW));
  
        boolean isTimeBlock = layout == null || layout.getValue() == null || layout.getValue().equals("0");
        
        if(layout != null) {
          if(blockSize != null) {
            blockSize.setEnabled(progTable.isChecked() && isTimeBlock);
          }
          if(spreadOverBlocks != null) {
            spreadOverBlocks.setEnabled(progTable.isChecked() && isTimeBlock);
          }
        }
      }
      else if(key.equals(getResources().getString(R.string.SHOW_PICTURE_IN_DETAILS))) {
        CheckBoxPreference picturesInDetails = (CheckBoxPreference) findPreference(key);
        ListPreference pictureZoom = (ListPreference) findPreference(getResources().getString(R.string.DETAIL_PICTURE_ZOOM));
        ListPreference pictureDescPos = (ListPreference)findPreference(getString(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION));
        
        if(picturesInDetails != null && pictureZoom != null && pictureDescPos != null) {
          pictureZoom.setEnabled(picturesInDetails.isChecked());
          pictureDescPos.setEnabled(picturesInDetails.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED)) || key.equals(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_NO_REMINDER))) {
        CheckBoxPreference nightModeActivatedPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED));
        CheckBoxPreference noReminder = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_NO_REMINDER));
        ListPreference priority = (ListPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_PRIORITY_VALUE));
        
        RingtonePreference sound = (RingtonePreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE));
        CheckBoxPreference vibrate = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_VIBRATE));
        CheckBoxPreference led = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_LED));
        
        TimePreference start = (TimePreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_START));
        TimePreference end = (TimePreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_END));
        
        if(nightModeActivatedPref != null && noReminder != null) {
          boolean nightMode = nightModeActivatedPref.isChecked();
          boolean onlyStatus = noReminder.isChecked();
          
          noReminder.setEnabled(nightMode);
          start.setEnabled(nightMode);
          end.setEnabled(nightMode);
          
          priority.setEnabled(nightMode && !onlyStatus);
          sound.setEnabled(nightMode && !onlyStatus);
          vibrate.setEnabled(nightMode && !onlyStatus);
          led.setEnabled(nightMode && !onlyStatus);
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED)) || key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_NO_REMINDER))) {
        CheckBoxPreference nightModeActivatedPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED));
        CheckBoxPreference noReminder = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_NO_REMINDER));
        ListPreference priority = (ListPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_PRIORITY_VALUE));
        
        RingtonePreference sound = (RingtonePreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE));
        CheckBoxPreference vibrate = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_VIBRATE));
        CheckBoxPreference led = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_LED));
        
        PreferenceScreen days = (PreferenceScreen)findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_DAYS));
        
        if(nightModeActivatedPref != null && noReminder != null) {
          boolean nightMode = nightModeActivatedPref.isChecked();
          boolean onlyStatus = noReminder.isChecked();
          
          noReminder.setEnabled(nightMode);
          days.setEnabled(nightMode);
          
          priority.setEnabled(nightMode && !onlyStatus);
          sound.setEnabled(nightMode && !onlyStatus);
          vibrate.setEnabled(nightMode && !onlyStatus);
          led.setEnabled(nightMode && !onlyStatus);
        }
        
        if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED))) {
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_MONDAY_ACTIVATED));
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_ACTIVATED));
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_ACTIVATED));
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_ACTIVATED));
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_ACTIVATED));
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_ACTIVATED));
          onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_ACTIVATED));
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_SHOW_PROGRESS))) {
        CheckBoxPreference showProgress = (CheckBoxPreference) findPreference(key);
        ListPreference currentStyle = (ListPreference)findPreference(getString(R.string.PREF_COLOR_STYLE));
        
        ColorPreference onAirBackground = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND));
        ColorPreference onAirProgress = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_ON_AIR_PROGRESS));
        
        if(showProgress != null) {
          if(onAirBackground != null) {
            onAirBackground.setEnabled(showProgress.isChecked() && currentStyle.getValue().equals("0"));
          }
          if(onAirProgress != null) {
            onAirProgress.setEnabled(showProgress.isChecked() && currentStyle.getValue().equals("0"));
          }
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_MONDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_MONDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_MONDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_ACTIVATED))) {
        CheckBoxPreference activated = (CheckBoxPreference) findPreference(key);
        
        TimePreference start = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_START));
        TimePreference end = (TimePreference) findPreference(getString(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_END));
        
        if(activated != null) {
          start.setEnabled(activated.isChecked());
          end.setEnabled(activated.isChecked());
        }
      }
    }
  }
  
  
  
  private void setUserColorValue(SharedPreferences pref, String key, int valueKey) {
    ListPreference currentStyle = (ListPreference)findPreference(getString(R.string.PREF_COLOR_STYLE));
    ColorPreference color = (ColorPreference)findPreference(key);
    
    if(currentStyle != null && currentStyle.getValue() != null && currentStyle.getValue().equals("0")) {
      Editor edit = pref.edit();
      edit.putInt(getString(valueKey), color.getColor());
      edit.commit();
    }
  }
}
