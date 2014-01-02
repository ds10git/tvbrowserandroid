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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class TvbPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    String category = getArguments().getString(getString(R.string.pref_category_key));
    
    if(getString(R.string.category_download).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_download);
    }
    else if(getString(R.string.category_start).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_start);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.TAB_TO_SHOW_AT_START));
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
      addPreferencesFromResource(R.xml.preferences_programs_list);
    }
    else if(getString(R.string.category_details).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_details);
      
      onSharedPreferenceChanged(null,getResources().getString(R.string.SHOW_PICTURE_IN_DETAILS));
    }
    else if(getString(R.string.category_sync).equals(category)) {
      addPreferencesFromResource(R.xml.preferences_sync);
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
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if(key.equals(getResources().getString(R.string.DAYS_TO_DOWNLOAD)) 
        || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAMS_LIST))
        || key.equals(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE))
        || key.equals(getResources().getString(R.string.DETAIL_PICTURE_ZOOM))
        || key.equals(getResources().getString(R.string.TAB_TO_SHOW_AT_START))
        || key.equals(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE))
        || key.equals(getResources().getString(R.string.RUNNING_PROGRAMS_LAYOUT))) {
      ListPreference lp = (ListPreference) findPreference(key);
      
      if(lp != null) {
        lp.setSummary("dummy"); // required or will not update
        
        String value = String.valueOf(lp.getEntry());
        
        if(value.endsWith("%")) {
          value += "%";
        }
        
        lp.setSummary(value);
      }
    }
    else if(key.equals(getResources().getString(R.string.PROG_TABLE_ACTIVATED))) {
      CheckBoxPreference progTable = (CheckBoxPreference) findPreference(key);
      
      ListPreference blockSize = (ListPreference) findPreference(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE));
      CheckBoxPreference spreadOverBlocks = (CheckBoxPreference) findPreference(getResources().getString(R.string.PROG_PANEL_GROW));
      
      ListPreference channelLogoName = (ListPreference) findPreference(getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE));
      ListPreference layout = (ListPreference) findPreference(getResources().getString(R.string.PROG_TABLE_LAYOUT));
      CheckBoxPreference pictures = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE));
      CheckBoxPreference showOrderNumber = (CheckBoxPreference) findPreference(getResources().getString(R.string.SHOW_SORT_NUMBER_IN_PROGRAM_TABLE));
      
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
        if(showOrderNumber != null) {
          showOrderNumber.setEnabled(progTable.isChecked());
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
  }
}
