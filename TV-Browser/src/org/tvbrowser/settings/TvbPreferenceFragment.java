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

import org.tvbrowser.tvbrowser.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.util.Log;

public class TvbPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    String category = getArguments().getString(getString(R.string.pref_category_key));
    
    if(getString(R.string.category_download).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_download);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_AUTO_UPDATE_TYPE));
    }
    else if(getString(R.string.category_start).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_start);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.TAB_TO_SHOW_AT_START));
    }
    else if(getString(R.string.category_theme).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_layout);
      
      onSharedPreferenceChanged(null, getString(R.string.PREF_SHOW_PROGRESS));
    }
    else if(getString(R.string.category_reminder).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_reminder);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_TIME));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_SOUND_VALUE));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_MONDAY_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_ACTIVATED));
      onSharedPreferenceChanged(null,getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_ACTIVATED));
    }
    else if(getString(R.string.category_time_buttons).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_time_buttons);
    }
    else if(getString(R.string.category_running_programs).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_running);
    }
    else if(getString(R.string.category_programs_list).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_programs_list);
    }
    else if(getString(R.string.category_program_table).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_program_table);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.PROG_TABLE_ACTIVATED));
    }
    else if(getString(R.string.category_list).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_program_lists);
    }
    else if(getString(R.string.category_details).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_details);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.SHOW_PICTURE_IN_DETAILS));
    }
    else if(getString(R.string.category_sync).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_sync);
    }
    else if(getString(R.string.category_email).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_email);
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
      if(key.equals(getString(R.string.PREF_REMINDER_SOUND_VALUE)) || key.equals(getString(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE))
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
          
          if(tone == null || tone.trim().length() > 0) {
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
          || key.equals(getResources().getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE))
          || key.equals(getResources().getString(R.string.PREF_REMINDER_TIME))
          || key.equals(getResources().getString(R.string.DETAIL_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PREF_PROGRAM_LISTS_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PROG_TABLE_TEXT_SCALE))
          || key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_TYPE))
          || key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_RANGE))
          || key.equals(getResources().getString(R.string.PREF_AUTO_UPDATE_FREQUENCY))
          || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_RUNNING))
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
  
        if(key.equals(getResources().getString(R.string.PREF_REMINDER_TIME))) {
          CheckBoxPreference remindAgain = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMIND_AGAIN_AT_START));
          ListPreference reminderTime = (ListPreference) findPreference(key);
          
          if(remindAgain != null) {
            remindAgain.setEnabled(reminderTime.getValue() == null || !reminderTime.getValue().equals("0"));
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
      }
      else if(key.equals(getResources().getString(R.string.PROG_TABLE_ACTIVATED))) {
        CheckBoxPreference progTable = (CheckBoxPreference) findPreference(key);
        
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
        
        if(picturesInDetails != null && pictureZoom != null) {
          pictureZoom.setEnabled(picturesInDetails.isChecked());
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED)) || key.equals(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_NO_REMINDER))) {
        CheckBoxPreference nightModeActivatedPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED));
        CheckBoxPreference noReminder = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_NIGHT_MODE_NO_REMINDER));
        
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
          
          sound.setEnabled(nightMode && !onlyStatus);
          vibrate.setEnabled(nightMode && !onlyStatus);
          led.setEnabled(nightMode && !onlyStatus);
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED)) || key.equals(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_NO_REMINDER))) {
        CheckBoxPreference nightModeActivatedPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED));
        CheckBoxPreference noReminder = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_NO_REMINDER));
        
        RingtonePreference sound = (RingtonePreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE));
        CheckBoxPreference vibrate = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_VIBRATE));
        CheckBoxPreference led = (CheckBoxPreference) findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_LED));
        
        PreferenceScreen days = (PreferenceScreen)findPreference(getResources().getString(R.string.PREF_REMINDER_WORK_MODE_DAYS));
        
        if(nightModeActivatedPref != null && noReminder != null) {
          boolean nightMode = nightModeActivatedPref.isChecked();
          boolean onlyStatus = noReminder.isChecked();
          
          noReminder.setEnabled(nightMode);
          days.setEnabled(nightMode);
          
          sound.setEnabled(nightMode && !onlyStatus);
          vibrate.setEnabled(nightMode && !onlyStatus);
          led.setEnabled(nightMode && !onlyStatus);
        }
      }
      else if(key.equals(getResources().getString(R.string.PREF_SHOW_PROGRESS))) {
        CheckBoxPreference showProgress = (CheckBoxPreference) findPreference(key);
        ColorPreference onAirBackground = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND));
        ColorPreference onAirProgress = (ColorPreference)findPreference(getString(R.string.PREF_COLOR_ON_AIR_PROGRESS));
        
        if(showProgress != null) {
          if(onAirBackground != null) {
            onAirBackground.setEnabled(showProgress.isChecked());
          }
          if(onAirProgress != null) {
            onAirProgress.setEnabled(showProgress.isChecked());
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
}
