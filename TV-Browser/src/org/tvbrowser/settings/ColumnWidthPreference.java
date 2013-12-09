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

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class ColumnWidthPreference extends DialogPreference {
  private static final int STEP = 10;
  private static final int MIN_VALUE = 100;
  private static final int MAX_VALUE = 300;
  private static final int DEFAULT_VALUE = 200;
  
  private NumberPicker mNumberPicker;
  private int mWidth;

  public ColumnWidthPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    setPositiveButtonText(android.R.string.ok);
    setNegativeButtonText(android.R.string.cancel);
    
    mWidth = DEFAULT_VALUE;
  }
  
  @Override
  protected View onCreateDialogView() {
    mNumberPicker = new NumberPicker(getContext());
    mNumberPicker.setMinValue(0);
    mNumberPicker.setMaxValue((MAX_VALUE - MIN_VALUE) / 10);
    mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    
    String[] displayedValues = new String[mNumberPicker.getMaxValue() + 1];
    
    for(int i = 0; i < displayedValues.length; i++) {
      displayedValues[i] = String.valueOf(i * STEP + MIN_VALUE);
    }
    
    mNumberPicker.setDisplayedValues(displayedValues);
    
    return mNumberPicker;
  }
  

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    
    mNumberPicker.setValue((mWidth - MIN_VALUE) / STEP);
  }
  

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    
    if(positiveResult) {
      mWidth = mNumberPicker.getValue() * STEP + MIN_VALUE;
      
      setSummary(String.valueOf(mWidth));
      
      if (callChangeListener(mWidth)) {
        persistInt(mWidth);
      }
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getInt(index, DEFAULT_VALUE));
  }
  

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if(restorePersistedValue) {
      if(defaultValue == null) {
        mWidth = getPersistedInt(0);
      }
      else {
        mWidth = getPersistedInt((Integer)defaultValue);
      }
    }
    else {
      mWidth = (Integer)defaultValue;
    }
    
    setSummary(String.valueOf(mWidth));
  }
}
