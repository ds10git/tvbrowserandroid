/*
 * TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

public class ActivityFavoriteEdit extends Activity implements ChannelFilter {
  private Favorite mFavorite;
  private EditText mSearchValue;
  private EditText mName;
  private Spinner mTypeSelection;
  private CheckBox mRemind;
  private TextView mDuration;
  private TextView mTime;
  private TextView mDays;
  private TextView mChannels;
  private TextView mAttributes;
  private EditText mExclusions;
  
  private int mCheckedCount;
  private Favorite mOldFavorite;
  private View mOkButton;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if(SettingConstants.IS_DARK_THEME) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    setContentView(R.layout.activity_favorite_edit);
    
    mSearchValue = (EditText)findViewById(R.id.activity_edit_favorite_input_id_search_value);
    mName = (EditText)findViewById(R.id.activity_edit_favorite_input_id_name);
    mTypeSelection = (Spinner)findViewById(R.id.activity_edit_favorite_input_id_type);
    mRemind = (CheckBox)findViewById(R.id.activity_edit_favorite_input_id_remind);
    mDuration = (TextView)findViewById(R.id.activity_edit_favorite_input_id_restriction_duration);
    mTime = (TextView)findViewById(R.id.activity_edit_favorite_input_id_restriction_time);
    mDays = (TextView)findViewById(R.id.activity_edit_favorite_input_id_restriction_day);
    mChannels = (TextView)findViewById(R.id.activity_edit_favorite_input_id_restriction_channel);
    mAttributes = (TextView)findViewById(R.id.activity_edit_favorite_input_id_restriction_attributes);
    mExclusions = (EditText)findViewById(R.id.activity_edit_favorite_input_id_restriction_exclusion);
    
    int color = getResources().getColor(android.R.color.primary_text_light);
    
    if(SettingConstants.IS_DARK_THEME) {
      color = getResources().getColor(android.R.color.primary_text_dark);
    }
    
    mDuration.setTextColor(color);
    mTime.setTextColor(color);
    mDays.setTextColor(color);
    mChannels.setTextColor(color);
    mAttributes.setTextColor(color);
    
    mSearchValue.requestFocusFromTouch();
    
    mFavorite = (Favorite)getIntent().getSerializableExtra(Favorite.FAVORITE_EXTRA);
    
    if(mFavorite != null) {
      mSearchValue.setEnabled(mFavorite.getType() != Favorite.RESTRICTION_RULES_TYPE);
      
      if(mFavorite.getType() == Favorite.RESTRICTION_RULES_TYPE) {
        mSearchValue.setText(getString(R.string.activity_edit_favorite_input_text_all_value));
      }
      
      mOldFavorite = mFavorite.copy();
      mSearchValue.setText(mFavorite.getSearchValue());
      mName.setText(mFavorite.getName());
      mTypeSelection.setSelection(mFavorite.getType());
      mRemind.setChecked(mFavorite.remind());
      
      if(mFavorite.isHavingExclusions()) {
        mExclusions.setText(TextUtils.join(", ", mFavorite.getExclusions()));
      }
    }
    else {
      mFavorite = new Favorite();
      
      String search = getIntent().getStringExtra(Favorite.SEARCH_EXTRA);
      
      if(search != null) {
        mFavorite.setSearchValue(search);
        mSearchValue.setText(search);
      }
    }
    
    mTypeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mSearchValue.setEnabled(position != Favorite.RESTRICTION_RULES_TYPE);
        
        if(position == Favorite.RESTRICTION_RULES_TYPE) {
          mSearchValue.setText(getString(R.string.activity_edit_favorite_input_text_all_value));
        }
        else {
          mSearchValue.setText(mFavorite.getSearchValue());
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
    });
    
    mOkButton = findViewById(R.id.favorite_ok);
    
    if(mTypeSelection.getSelectedItemPosition() != Favorite.RESTRICTION_RULES_TYPE) {
      mOkButton.setEnabled(mFavorite.getSearchValue() != null && mFavorite.getSearchValue().trim().length() > 0);
    }
    else {
      mOkButton.setEnabled(mFavorite.isHavingRestriction());
    }
    
    mSearchValue.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      
      @Override
      public void afterTextChanged(Editable s) {
        if(mTypeSelection.getSelectedItemPosition() != Favorite.RESTRICTION_RULES_TYPE) {
          mOkButton.setEnabled(mSearchValue.getText().toString().trim().length() > 0);
        }
        else {
          mOkButton.setEnabled(mFavorite.isHavingRestriction());
        }
      }
    });
    
    mExclusions.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      
      @Override
      public void afterTextChanged(Editable s) {
        updateOkButton();
      }
    });
    
    handleDurationView();
    handleTimeView();
    handleDayView();
    handleChannelView();
    handleAttributeView();
  }
  
  public void changeDuration(View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityFavoriteEdit.this);
    
    View timeSelection = getLayoutInflater().inflate(R.layout.dialog_favorite_selection_duration, (ViewGroup)mSearchValue.getRootView(), false);
    
    final CheckBox minimumSelected = (CheckBox)timeSelection.findViewById(R.id.dialog_favorite_selection_id_selection_duration_minimum);
    final CheckBox maximumSelected = (CheckBox)timeSelection.findViewById(R.id.dialog_favorite_selection_id_selection_duration_maximum);
    final TimePicker minimum = (TimePicker)timeSelection.findViewById(R.id.dialog_favorite_selection_id_input_duration_minimum);
    final TimePicker maximum = (TimePicker)timeSelection.findViewById(R.id.dialog_favorite_selection_id_input_duration_maximum);
    
    minimum.setIs24HourView(true);
    maximum.setIs24HourView(true);
    
    if(mFavorite.isDurationRestricted()) {
      if(mFavorite.getDurationRestrictionMinimum() >= 0) {
        minimumSelected.setChecked(true);
        
        minimum.setCurrentHour(mFavorite.getDurationRestrictionMinimum() / 60);
        minimum.setCurrentMinute(mFavorite.getDurationRestrictionMinimum() % 60);
      }
      else {
        minimum.setEnabled(false);
        minimum.setCurrentHour(0);
        minimum.setCurrentMinute(0);
      }
      
      if(mFavorite.getDurationRestrictionMaximum() > 0) {
        maximumSelected.setChecked(true);
        
        maximum.setCurrentHour(mFavorite.getDurationRestrictionMaximum() / 60);
        maximum.setCurrentMinute(mFavorite.getDurationRestrictionMaximum() % 60);
      }
      else {
        maximum.setEnabled(false);
        maximum.setCurrentHour(0);
        maximum.setCurrentMinute(0);
      }
    }
    else {
      minimum.setEnabled(false);
      minimum.setCurrentHour(0);
      minimum.setCurrentMinute(0);
      
      maximum.setEnabled(false);
      maximum.setCurrentHour(0);
      maximum.setCurrentMinute(0);
    }
    
    minimumSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        minimum.setEnabled(isChecked);
      }
    });
    
    maximumSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        maximum.setEnabled(isChecked);
      }
    });
    
    builder.setView(timeSelection);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        int minimumValue = -1;
        int maximumValue = -1;
        
        if(minimumSelected.isChecked()) {
          minimumValue = minimum.getCurrentHour() * 60 + minimum.getCurrentMinute();
        }
        
        if(maximumSelected.isChecked()) {
          maximumValue = maximum.getCurrentHour() * 60 + maximum.getCurrentMinute();
          
          if(maximumValue == 0) {
            maximumValue = -1;
          }
        }
        
        if(minimumValue > maximumValue && maximumValue != -1) {
          maximumValue = -1;
        }
        
        mFavorite.setDurationRestrictionMinimum(minimumValue);
        mFavorite.setDurationRestrictionMaximum(maximumValue);
        
        updateOkButton();
        handleDurationView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  private void updateOkButton() {
    if(mTypeSelection.getSelectedItemPosition() == Favorite.RESTRICTION_RULES_TYPE) {
      mOkButton.setEnabled(mFavorite.isHavingRestriction() || mExclusions.getText().toString().trim().length() > 0);
    }
  }
  
  public void changeTime(View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityFavoriteEdit.this);
    
    View timeSelection = getLayoutInflater().inflate(R.layout.favorite_time_selection, (ViewGroup)mSearchValue.getRootView(), false);
    
    final TimePicker from = (TimePicker)timeSelection.findViewById(R.id.favorite_time_selection_from);
    final TimePicker to = (TimePicker)timeSelection.findViewById(R.id.favorite_time_selection_to);
    
    from.setIs24HourView(DateFormat.is24HourFormat(ActivityFavoriteEdit.this));
    to.setIs24HourView(DateFormat.is24HourFormat(ActivityFavoriteEdit.this));
    
    if(mFavorite.isTimeRestricted()) {
      Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      IOUtils.normalizeTime(utc, mFavorite.getTimeRestrictionStart(), 0);
      
      Calendar current = Calendar.getInstance();
      current.setTime(utc.getTime());
      
      from.setCurrentHour(current.get(Calendar.HOUR_OF_DAY));
      from.setCurrentMinute(current.get(Calendar.MINUTE));
      
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionEnd() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionEnd() % 60);
      
      current.setTime(utc.getTime());
      
      to.setCurrentHour(current.get(Calendar.HOUR_OF_DAY));
      to.setCurrentMinute(current.get(Calendar.MINUTE));
    }
    else {
      from.setCurrentHour(0);
      from.setCurrentMinute(0);
      
      to.setCurrentHour(23);
      to.setCurrentMinute(59);
    }
    
    builder.setView(timeSelection);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Calendar current = Calendar.getInstance();
        IOUtils.normalizeTime(current, from.getCurrentHour(), from.getCurrentMinute(), 0);
        
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTime(current.getTime());
        
        int start = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE);
        
        current.set(Calendar.HOUR_OF_DAY, to.getCurrentHour());
        current.set(Calendar.MINUTE, to.getCurrentMinute());
        
        utc.setTime(current.getTime());
        
        int end = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE);
        
        if((start == end) || (start == 0 && end == 23*60 + 59)) {
          start = -1;
          end = -1;
        }
        
        mFavorite.setTimeRestrictionStart(start);
        mFavorite.setTimeRestrictionEnd(end);
        
        updateOkButton();
        handleTimeView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  public void changeDays(View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityFavoriteEdit.this);
    
    final Calendar dayCal = Calendar.getInstance();
    final Locale locale = Locale.getDefault();
    
    String[] dayArray = new String[7];
    
    for(int day = Calendar.MONDAY; day <= Calendar.SATURDAY; day++) {
      dayCal.set(Calendar.DAY_OF_WEEK, day);
      dayArray[day-2] = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale);
    }
    
    dayCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    dayArray[6] = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale);
    
    final boolean[] checked = new boolean[7];
    mCheckedCount = 0;
    
    Arrays.fill(checked, false);
    
    if(mFavorite.isDayRestricted()) {
      for(int day : mFavorite.getDayRestriction()) {
        if(day == Calendar.SUNDAY) {
          checked[6] = true;
          mCheckedCount++;
        }
        else {
          checked[day-2] = true;
          mCheckedCount++;
        }
      }
    }
    
    builder.setMultiChoiceItems(dayArray, checked, new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        checked[which] = isChecked;
        
        if(isChecked) {
          mCheckedCount++;
        }
        else {
          mCheckedCount--;
        }
      }
    });
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if(mCheckedCount < 7 && mCheckedCount > 0) {
          int[] days =  new int[mCheckedCount];
          
          int dayIndex = 0;
                    
          for(int i = 0; i < checked.length; i++) {
            if(checked[i]) {
              if(i == 6) {
                days[dayIndex++] = Calendar.SUNDAY;
              }
              else {
                days[dayIndex++] = i+2;
              }
            }
          }
          
          mFavorite.setDayRestriction(days);
        }
        else {
          mFavorite.setDayRestriction(null);
        }
        
        updateOkButton();
        handleDayView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  private void handleDurationView() {
    StringBuilder timeString = new StringBuilder();
    
    if(mFavorite.isDurationRestricted()) {
      int minimum = mFavorite.getDurationRestrictionMinimum();
      int maximum = mFavorite.getDurationRestrictionMaximum();
      String minutes = getString(R.string.activity_edit_favorite_input_text_duration_minutes);
      String max = getString(R.string.activity_edit_favorite_input_text_duration_maximum);
      
      if(minimum != -1) {
        max = max.toLowerCase(Locale.getDefault());
        
        timeString.append(getString(R.string.activity_edit_favorite_input_text_duration_minimum));
        timeString.append(" ");
        timeString.append(minimum);
        timeString.append(" ");
        timeString.append(minutes);
        
        if(maximum != -1) {
          timeString.append(" ");
          timeString.append(getString(R.string.activity_edit_favorite_input_text_duration_and));
          timeString.append(" ");
        }
      }
      if(maximum != -1) {
        timeString.append(max);
        timeString.append(" ");
        timeString.append(maximum);
        timeString.append(" ");
        timeString.append(minutes);
      }
    }
    else {
      timeString.append(getString(R.string.activity_edit_favorite_input_text_duration_unrestricted));
    }
    
    mDuration.setText(timeString.toString());
  }
  
  private void handleTimeView() {
    java.text.DateFormat timeFormat = DateFormat.getTimeFormat(ActivityFavoriteEdit.this);
    
    Date fromFormat = null;
    Date toFormat = null;
    
    if(mFavorite.isTimeRestricted()) {
      Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      IOUtils.normalizeTime(utc, mFavorite.getTimeRestrictionStart(), 0);
            
      fromFormat = utc.getTime();
      
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionEnd() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionEnd() % 60);
      
      toFormat = utc.getTime();
    }
    else {
      Calendar now = Calendar.getInstance();
      now.set(Calendar.HOUR_OF_DAY, 0);
      now.set(Calendar.MINUTE, 0);
      
      fromFormat = now.getTime();
      
      now.set(Calendar.HOUR_OF_DAY, 23);
      now.set(Calendar.MINUTE, 59);
      
      toFormat = now.getTime();
    }
    
    StringBuilder timeString = new StringBuilder();
    
    timeString.append(timeFormat.format(fromFormat));
    timeString.append(" ");
    timeString.append(getString(R.string.favorite_time_to));
    timeString.append(" ");
    timeString.append(timeFormat.format(toFormat));
    
    mTime.setText(timeString.toString());
  }
  
  private void handleDayView() {
    if(!mFavorite.isDayRestricted()) {
      mDays.setText(R.string.favorite_days_default);
    }
    else {
      Locale locale = Locale.getDefault();
      
      Calendar dayNames = Calendar.getInstance();
      
      ArrayList<String> days = new ArrayList<String>();
      
      String sunday = null;
      
      for(int day : mFavorite.getDayRestriction()) {
        dayNames.set(Calendar.DAY_OF_WEEK, day);
        
        if(day == Calendar.SUNDAY) {
          sunday = dayNames.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale);
        }
        else {
          days.add(dayNames.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale));
        }
      }
      
      if(sunday != null) {
        days.add(sunday);
      }
      
      mDays.setText(TextUtils.join(" ", days));
    }
  }
    
  private void handleChannelView() {
    if(mFavorite.isChannelRestricted()) {
      String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME
      };
      
      int[] ids = mFavorite.getChannelRestrictionIDs();
      
      StringBuilder where = new StringBuilder();
      
      where.append(TvBrowserContentProvider.KEY_ID).append(" IN ( ");
      
      for(int i = 0; i < ids.length-1; i++) {
        where.append(ids[i]).append(", ");
      }
      
      where.append(ids[ids.length-1]).append(" ) ");
      
      Cursor channelNames = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);
      
      channelNames.moveToPosition(-1);
      
      ArrayList<String> nameList = new ArrayList<String>();
      
      int nameColumn = channelNames.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      
      while(channelNames.moveToNext()) {
        nameList.add(channelNames.getString(nameColumn));
      }
      
      channelNames.close();
      
      mChannels.setText(TextUtils.join(", ", nameList));
    }
    else {
      mChannels.setText(R.string.activity_edit_favorite_input_text_all_value);
    }
  } 
  
  public void changeChannels(View view) {
    UiUtils.showChannelFilterSelection(ActivityFavoriteEdit.this, this, (ViewGroup)mSearchValue.getRootView());
  }
  
  private void handleAttributeView() {
    if(mFavorite.isAttributeRestricted()) {
      ArrayList<String> selectedAttributes = new ArrayList<String>();
      
      int[] restrictionIndices = mFavorite.getAttributeRestrictionIndices();
      String[] names = IOUtils.getInfoStringArrayNames(getResources());
      
      for(int index : restrictionIndices) {
        selectedAttributes.add(names[index]);
      }
      
      mAttributes.setText(TextUtils.join(", ", selectedAttributes));
    }
    else {
      mAttributes.setText(R.string.activity_edit_favorite_input_text_duration_unrestricted);
    }
  }
  
  public void changeAttributes(View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityFavoriteEdit.this);
    
    String[] names = IOUtils.getInfoStringArrayNames(getResources());
    final boolean[] selection = new boolean[names.length];
    
    Arrays.fill(selection, false);
    
    if(mFavorite.isAttributeRestricted()) {
      int[] restrictionIndices = mFavorite.getAttributeRestrictionIndices();
      Log.d("info2", " xx " + restrictionIndices);
      for(int index : restrictionIndices) {
        Log.d("info2", " yy " + index);
        selection[index] = true;
      }
    }
    
    builder.setMultiChoiceItems(names, selection, new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        selection[which] = isChecked;
      }
    });
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        ArrayList<Integer> selectedAttributes = new ArrayList<Integer>();
        
        for(int i = 0; i < selection.length; i++) {
          if(selection[i]) {
            selectedAttributes.add(Integer.valueOf(i));
          }
        }
        
        if(!selectedAttributes.isEmpty()) {
          int[] restrictedAttributes = new int[selectedAttributes.size()];
          
          for(int i = 0; i < restrictedAttributes.length; i++) {
            restrictedAttributes[i] = selectedAttributes.get(i).intValue();
          }
          
          mFavorite.setAttributeRestrictionIndices(restrictedAttributes);
        }
        else {
          mFavorite.setAttributeRestrictionIndices(null);
        }
        
        updateOkButton();
        handleAttributeView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  public void cancel(View view) {
    finish();
  }
  
  public void ok(View view) {
    findViewById(R.id.favorite_ok).setEnabled(false);
    findViewById(R.id.favorite_cancel).setEnabled(false);

    mFavorite.setName(mName.getText().toString());
    mFavorite.setSearchValue(mSearchValue.getText().toString());
    mFavorite.setType(mTypeSelection.getSelectedItemPosition());
    mFavorite.setRemind(mRemind.isChecked());
    
    String exclusions = mExclusions.getText().toString();
    
    if(exclusions.trim().length() > 0) {
      if(exclusions.contains(",")) {
        mFavorite.setExclusions(exclusions.split(",\\s+"));
      }
      else {
        mFavorite.setExclusions(new String[] {exclusions.trim()});
      }
    }
    else {
      mFavorite.setExclusions(null);
    }
    
    if(mFavorite.getName().trim().length() == 0) {
      if(mFavorite.getType() == Favorite.RESTRICTION_RULES_TYPE) {
        mFavorite.setName(getResources().getStringArray(R.array.activity_edit_favorite_input_text_type)[Favorite.RESTRICTION_RULES_TYPE] + " - " +
                          DateFormat.getMediumDateFormat(ActivityFavoriteEdit.this).format(new Date(System.currentTimeMillis())) + " " + 
                          DateFormat.getTimeFormat(ActivityFavoriteEdit.this).format(new Date(System.currentTimeMillis())));
      }
      else {
        mFavorite.setName(mFavorite.getSearchValue());
      }
    }
    
    final Intent intent = new Intent(SettingConstants.FAVORITES_CHANGED);
    
    if(mOldFavorite != null) {
      intent.putExtra(Favorite.OLD_NAME_KEY, mOldFavorite.getName());
    }
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ActivityFavoriteEdit.this);
    
    Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
    HashSet<String> newFavoriteList = new HashSet<String>();
    
    boolean added = false;
    
    for(String favorite : favoritesSet) {
      String[] values = favorite.split(";;");
      
      if(mOldFavorite != null && values[0].equals(mOldFavorite.getName())) {                
        newFavoriteList.add(mFavorite.getSaveString());
        added = true;
      }
      else {
        newFavoriteList.add(favorite);
      }
    }
      
    if(!added) {
      newFavoriteList.add(mFavorite.getSaveString());
    }
    
    Editor edit = prefs.edit();
    edit.putStringSet(SettingConstants.FAVORITE_LIST, newFavoriteList);
    edit.commit();
    
    intent.putExtra(Favorite.FAVORITE_EXTRA, mFavorite);
    
    final Context context = getApplicationContext();
    final ContentResolver resolver = getContentResolver();
    
    new Thread() {
      @Override
      public void run() {
        if(mOldFavorite != null) {
          Favorite.removeFavoriteMarking(context, resolver, mOldFavorite);
        }
        
        Favorite.updateFavoriteMarking(context, resolver, mFavorite);
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
      }
    }.start();
        
    finish();
  }

  @Override
  public int[] getFilteredChannelIds() {
    return mFavorite.getChannelRestrictionIDs();
  }

  @Override
  public String getName() {
    return null;
  }


  @Override
  public void setFilterValues(String name, int[] filteredChannelIds) {
    mFavorite.setChannelRestrictionIDs(filteredChannelIds);
    
    updateOkButton();
    handleChannelView();
  }
}
