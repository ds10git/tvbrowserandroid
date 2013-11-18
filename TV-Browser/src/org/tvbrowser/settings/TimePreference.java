package org.tvbrowser.settings;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
  private Calendar mTime;
  private TimePicker mTimePicker;
  
  public TimePreference(Context context, AttributeSet attrs) {
    super(context,attrs);
    
    setPositiveButtonText(android.R.string.ok);
    setNegativeButtonText(android.R.string.cancel);
    
    mTime = new GregorianCalendar();
  }
  
  @Override
  protected View onCreateDialogView() {
    mTimePicker = new TimePicker(getContext());
    mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
    
    return mTimePicker;
  }
  
  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    
    mTimePicker.setCurrentHour(mTime.get(Calendar.HOUR_OF_DAY));
    mTimePicker.setCurrentMinute(mTime.get(Calendar.MINUTE));
  }
  
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    
    if(positiveResult) {
      mTime.set(Calendar.HOUR_OF_DAY, mTimePicker.getCurrentHour());
      mTime.set(Calendar.MINUTE, mTimePicker.getCurrentMinute());
      
      int minutes = mTimePicker.getCurrentHour() * 60 + mTimePicker.getCurrentMinute();
      
      setTitle(DateFormat.getTimeFormat(getContext()).format(mTime.getTime()));
      
      if (callChangeListener(minutes)) {
        persistInt(minutes);
      }
    }
  }
  
  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getInt(index, 0));
  }
  
  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    int minutes = 0;
    
    if(restorePersistedValue) {
      if(defaultValue == null) {
        minutes = getPersistedInt(0);
      }
      else {
        minutes = getPersistedInt((Integer)defaultValue);
      }
    }
    else {
      minutes = (Integer)defaultValue;
    }
    
    mTime.set(Calendar.HOUR_OF_DAY, minutes / 60);
    mTime.set(Calendar.MINUTE, minutes % 60);
    
    setTitle(DateFormat.getTimeFormat(getContext()).format(mTime.getTime()));
  }
}

