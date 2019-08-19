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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
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

public class NumberPreference extends DialogPreference {
  private NumberPicker mNumberPicker;
  private int mCurrentNumber;
  
  private final int mMinValue;
  private final int mMaxValue;

  public NumberPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
        
    String xmlns = "http://schemas.android.com/apk/res-auto";
    
    mMinValue = attrs.getAttributeIntValue(xmlns, "minValue", 0);
    mMaxValue = attrs.getAttributeIntValue(xmlns, "maxValue", 100);
    
    setPositiveButtonText(android.R.string.ok);
    setNegativeButtonText(android.R.string.cancel);
  }

  @Override
  protected View onCreateDialogView() {
    mNumberPicker = new NumberPicker(getContext());
    
    return mNumberPicker;
  }
  
  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    
    mNumberPicker.setMinValue(mMinValue);
    mNumberPicker.setMaxValue(mMaxValue);
    mNumberPicker.setValue(mCurrentNumber);
  }
  
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    
    if(positiveResult) {   
      mCurrentNumber = mNumberPicker.getValue();
      
      setSummary(String.valueOf(mCurrentNumber));
      
      if (callChangeListener(mCurrentNumber)) {
        persistInt(mCurrentNumber);
      }
    }
  }
  
  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getInt(index, 0));
  }
  
  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if(restorePersistedValue) {
      if(defaultValue == null) {
        mCurrentNumber = getPersistedInt(0);
      }
      else {
        mCurrentNumber = getPersistedInt((Integer)defaultValue);
      }
    }
    else {
      mCurrentNumber = (Integer)defaultValue;
    }
    
    setSummary(String.valueOf(mCurrentNumber));
  }
}
