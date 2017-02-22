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
import java.util.Locale;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.filter.CategoryFilter;
import org.tvbrowser.filter.ChannelFilter;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
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

public class ActivityFavoriteEdit extends ActionBarActivity implements ChannelFilter, CategoryFilter {
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
  private Favorite mOriginal;
  
  @Override
  protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
    resid = UiUtils.getThemeResourceId();
    
    super.onApplyThemeResource(theme, resid, first);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
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
    
    int color = ContextCompat.getColor(this, R.color.abc_primary_text_material_light);
    
    if(SettingConstants.IS_DARK_THEME) {
      color = ContextCompat.getColor(this, R.color.abc_primary_text_material_dark);
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
        
        if(search.contains(" AND ")) {
          mFavorite.setType(Favorite.KEYWORD_TYPE);
          mTypeSelection.setSelection(mFavorite.getType());
        }
      }
    }
    
    mOriginal = mFavorite.copy();
    
    mTypeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mSearchValue.setEnabled(position != Favorite.RESTRICTION_RULES_TYPE);
        
        if(position == Favorite.RESTRICTION_RULES_TYPE) {
          mSearchValue.setText(getString(R.string.activity_edit_favorite_input_text_all_value));
          mOkButton.setEnabled(mFavorite.isHavingRestriction());
        }
        else if(mSearchValue.getText().toString().trim().length() == 0 || mSearchValue.getText().toString().equals(getString(R.string.activity_edit_favorite_input_text_all_value))) {
          if(mFavorite.getSearchValue() != null && mFavorite.getSearchValue().equals(getString(R.string.activity_edit_favorite_input_text_all_value))) {
            mSearchValue.setText("");
          }
          else {
            mSearchValue.setText(mFavorite.getSearchValue());
          }
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
        
        CompatUtils.setTimePickerHour(minimum, mFavorite.getDurationRestrictionMinimum() / 60);
        CompatUtils.setTimePickerMinute(minimum, mFavorite.getDurationRestrictionMinimum() % 60);
      }
      else {
        minimum.setEnabled(false);
        CompatUtils.setTimePickerHour(minimum, 0);
        CompatUtils.setTimePickerMinute(minimum, 0);
      }
      
      if(mFavorite.getDurationRestrictionMaximum() > 0) {
        maximumSelected.setChecked(true);
        
        CompatUtils.setTimePickerHour(maximum, mFavorite.getDurationRestrictionMaximum() / 60);
        CompatUtils.setTimePickerMinute(maximum, mFavorite.getDurationRestrictionMaximum() % 60);
      }
      else {
        maximum.setEnabled(false);
        CompatUtils.setTimePickerHour(maximum, 0);
        CompatUtils.setTimePickerMinute(maximum, 0);
      }
    }
    else {
      minimum.setEnabled(false);
      CompatUtils.setTimePickerHour(minimum, 0);
      CompatUtils.setTimePickerMinute(minimum, 0);
      
      maximum.setEnabled(false);
      CompatUtils.setTimePickerHour(maximum, 0);
      CompatUtils.setTimePickerMinute(maximum, 0);
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
        int minimumValue = Favorite.VALUE_RESTRICTION_TIME_DEFAULT;
        int maximumValue = Favorite.VALUE_RESTRICTION_TIME_DEFAULT;
        
        if(minimumSelected.isChecked()) {
          minimumValue = CompatUtils.getTimePickerHour(minimum) * 60 + CompatUtils.getTimePickerMinute(minimum);
        }
        
        if(maximumSelected.isChecked()) {
          maximumValue = CompatUtils.getTimePickerHour(maximum) * 60 + CompatUtils.getTimePickerMinute(maximum);
          
          if(maximumValue == 0) {
            maximumValue = Favorite.VALUE_RESTRICTION_TIME_DEFAULT;
          }
        }
        
        if(minimumValue > maximumValue && maximumValue != Favorite.VALUE_RESTRICTION_TIME_DEFAULT) {
          maximumValue = Favorite.VALUE_RESTRICTION_TIME_DEFAULT;
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

      CompatUtils.setTimePickerHour(from, current.get(Calendar.HOUR_OF_DAY));
      CompatUtils.setTimePickerMinute(from, current.get(Calendar.MINUTE));
      
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionEnd() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionEnd() % 60);
      
      current.setTime(utc.getTime());

      CompatUtils.setTimePickerHour(to, current.get(Calendar.HOUR_OF_DAY));
      CompatUtils.setTimePickerMinute(to, current.get(Calendar.MINUTE));
    }
    else {
      CompatUtils.setTimePickerHour(from, 0);
      CompatUtils.setTimePickerMinute(from, 0);

      CompatUtils.setTimePickerHour(to, 23);
      CompatUtils.setTimePickerMinute(to, 59);
    }
    
    builder.setView(timeSelection);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Calendar current = Calendar.getInstance();
        IOUtils.normalizeTime(current, CompatUtils.getTimePickerHour(from), CompatUtils.getTimePickerMinute(from), 0);
        
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTime(current.getTime());
        
        int start = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE);
        
        current.set(Calendar.HOUR_OF_DAY, CompatUtils.getTimePickerHour(to));
        current.set(Calendar.MINUTE, CompatUtils.getTimePickerMinute(to));
        
        utc.setTime(current.getTime());
        
        int end = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE);
        
        final int unNormalizedFromTime = CompatUtils.getTimePickerHour(from) * 60 + CompatUtils.getTimePickerMinute(from);
        final int unNormalizedToTime = CompatUtils.getTimePickerHour(to) * 60 + CompatUtils.getTimePickerMinute(to);
        
        if((unNormalizedToTime == unNormalizedFromTime) || (unNormalizedFromTime == 0 && unNormalizedToTime == 1439)) {
          start = Favorite.VALUE_RESTRICTION_TIME_DEFAULT;
          end = Favorite.VALUE_RESTRICTION_TIME_DEFAULT;
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
      
      if(minimum != Favorite.VALUE_RESTRICTION_TIME_DEFAULT) {
        max = max.toLowerCase(Locale.getDefault());
        
        timeString.append(getString(R.string.activity_edit_favorite_input_text_duration_minimum));
        timeString.append(" ");
        timeString.append(minimum);
        timeString.append(" ");
        timeString.append(minutes);
        
        if(maximum != Favorite.VALUE_RESTRICTION_TIME_DEFAULT) {
          timeString.append(" ");
          timeString.append(getString(R.string.activity_edit_favorite_input_text_duration_and));
          timeString.append(" ");
        }
      }
      if(maximum != Favorite.VALUE_RESTRICTION_TIME_DEFAULT) {
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
      
      if(IOUtils.isDatabaseAccessible(this)) {
        Cursor channelNames = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);
        
        if(IOUtils.prepareAccess(channelNames)) {
          ArrayList<String> nameList = new ArrayList<String>();
          
          int nameColumn = channelNames.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
          
          while(channelNames.moveToNext()) {
            nameList.add(channelNames.getString(nameColumn));
          }
          
          mChannels.setText(TextUtils.join(", ", nameList));
        }
        
        IOUtils.close(channelNames); 
      }
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
    UiUtils.showCategorySelection(ActivityFavoriteEdit.this, this, (ViewGroup)mSearchValue.getRootView(), null);
  }
  
  public void cancel(View view) {
    finish();
  }
  
  public void ok(View view) {
    boolean notChanged = true;
    
    boolean remindChanged = mOriginal.remind() != mRemind.isChecked();
    
    notChanged = notChanged && mOriginal.getName().equals(mName.getText().toString());
    notChanged = notChanged && mOriginal.getSearchValue().equals(mSearchValue.getText().toString());
    notChanged = notChanged && mOriginal.getType() == mTypeSelection.getSelectedItemPosition();
    
    findViewById(R.id.favorite_ok).setEnabled(false);
    findViewById(R.id.favorite_cancel).setEnabled(false);
        
    mFavorite.setName(mName.getText().toString());
    mFavorite.setSearchValue(mSearchValue.getText().toString());
    mFavorite.setType(mTypeSelection.getSelectedItemPosition());
    mFavorite.setRemind(mRemind.isChecked());
    
    String exclusions = mExclusions.getText().toString();
    
    if(exclusions.trim().length() > 0) {
      if(exclusions.contains(",")) {
        mFavorite.setExclusions(exclusions.split(",\\s*"));
      }
      else {
        mFavorite.setExclusions(new String[] {exclusions.trim()});
      }
    }
    else {
      mFavorite.setExclusions(null);
    }
    
    if(notChanged) {
      notChanged = mOriginal.getDurationRestrictionMinimum() == mFavorite.getDurationRestrictionMinimum() && mOriginal.getDurationRestrictionMaximum() == mFavorite.getDurationRestrictionMaximum();
    }
    
    if(notChanged) {
      notChanged = mOriginal.getTimeRestrictionStart() == mFavorite.getTimeRestrictionStart() && mOriginal.getTimeRestrictionEnd() == mFavorite.getTimeRestrictionEnd();
    }
    
    if(notChanged) {
      String[] orgExclusions = mOriginal.getExclusions();
      String[] newExclusions = mFavorite.getExclusions();
      
      notChanged = orgExclusions == newExclusions;
    
      if(!notChanged && orgExclusions != null && newExclusions != null && orgExclusions.length == newExclusions.length) {
        boolean equal = true;
        
        for(int i = 0; i < orgExclusions.length; i++) {
          if(!orgExclusions[i].equals(newExclusions[i])) {
            equal = false;
            break;
          }
        }
        
        notChanged = equal;
      }
    }
    
    if(notChanged) {
      int[] orgChannelRestrictions = mOriginal.getChannelRestrictionIDs();
      int[] newChannelRestrictions = mFavorite.getChannelRestrictionIDs();
      
      notChanged = orgChannelRestrictions == newChannelRestrictions;
      
      if(!notChanged && orgChannelRestrictions != null && newChannelRestrictions != null && orgChannelRestrictions.length == newChannelRestrictions.length) {
        boolean equal = true;
        
        for(int i = 0; i < orgChannelRestrictions.length; i++) {
          if(orgChannelRestrictions[i] != newChannelRestrictions[i]) {
            equal = false;
            break;
          }
        }
        
        notChanged = equal;
      }
    }
    
    if(notChanged) {
      int[] orgDayRestrictions = mOriginal.getDayRestriction();
      int[] newDayRestrictions = mFavorite.getDayRestriction();
      
      notChanged = orgDayRestrictions == newDayRestrictions;
      
      if(!notChanged && orgDayRestrictions != null && newDayRestrictions != null && orgDayRestrictions.length == newDayRestrictions.length) {
        boolean equal = true;
        
        for(int i = 0; i < orgDayRestrictions.length; i++) {
          if(orgDayRestrictions[i] != newDayRestrictions[i]) {
            equal = false;
            break;
          }
        }
        
        notChanged = equal;
      }
    }
    
    if(notChanged) {
      int[] orgAttributeRestrictionIndices = mOriginal.getAttributeRestrictionIndices();
      int[] newAttributeRestrictionIndices = mFavorite.getAttributeRestrictionIndices();
      
      notChanged = orgAttributeRestrictionIndices == newAttributeRestrictionIndices;
      
      if(!notChanged && orgAttributeRestrictionIndices != null && newAttributeRestrictionIndices != null && orgAttributeRestrictionIndices.length == newAttributeRestrictionIndices.length) {
        boolean equal = true;
        
        for(int i = 0; i < orgAttributeRestrictionIndices.length; i++) {
          if(orgAttributeRestrictionIndices[i] != newAttributeRestrictionIndices[i]) {
            equal = false;
            break;
          }
        }
        
        notChanged = equal;
      }
    }
    
    Log.d("info2", " notChanged " + notChanged + " remindChanged " + remindChanged);
    if(notChanged && remindChanged) {
      mFavorite.save(getApplicationContext());
      
      Favorite.handleFavoriteMarking(getApplicationContext(), mFavorite, Favorite.TYPE_MARK_UPDATE_REMINDERS);
      
      final Intent intent = new Intent(SettingConstants.FAVORITES_CHANGED);
      intent.putExtra(Favorite.FAVORITE_EXTRA, mFavorite);
      intent.putExtra(Favorite.OLD_NAME_KEY, mFavorite.getName());
      LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
    else if(!notChanged) {
      Log.d("info2", "hier");
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
      
      mFavorite.clearUniqueIds();
      mFavorite.save(getApplicationContext());
      
      intent.putExtra(Favorite.FAVORITE_EXTRA, mFavorite);
      
      final Context context = getApplicationContext();
      
      Log.d("info2", "hier4 " + mOldFavorite);
      new Thread() {
        @Override
        public void run() {
          if(mOldFavorite != null) {
            Favorite.handleFavoriteMarking(context, mOldFavorite, Favorite.TYPE_MARK_REMOVE);
          }
          Log.d("info2", "hier5a");
          Favorite.handleFavoriteMarking(context, mFavorite, Favorite.TYPE_MARK_ADD);
          Log.d("info2", "hier5");
          LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
      }.start();
    }
    
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

  @Override
  public int[] getCategoriyIndicies() {
    return mFavorite.getAttributeRestrictionIndices();
  }

  @Override
  public String getOperation() {
    return "AND";
  }

  @Override
  public void setFilterValues(String name, String operation, int[] categoryIndicies) {
    mFavorite.setAttributeRestrictionIndices(categoryIndicies);
    
    updateOkButton();
    
    handleAttributeView();
  }
}
