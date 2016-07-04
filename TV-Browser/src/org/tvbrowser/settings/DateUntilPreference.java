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

import java.util.Date;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

public class DateUntilPreference extends Preference {
  private long mValue;
  private String mDefaultSummary;
  
  public DateUntilPreference(Context context, AttributeSet attrs) {
    super(context,attrs);
  }
  
  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getInt(index, 0));
  }
  
  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if(restorePersistedValue) {
      if(defaultValue == null) {
        mValue = getPersistedLong(0);
      }
      else {
        mValue = getPersistedLong((Integer)defaultValue);
      }
    }
    else {
      mValue = (Integer)defaultValue;
    }
    
    if(mDefaultSummary == null) {
      mDefaultSummary = getSummary().toString();
    }
    
    setSummary(mDefaultSummary.replace("{0}", DateFormat.getMediumDateFormat(getContext()).format(new Date(mValue))));
  }
  
  @Override
  protected void onBindView(View view) {
    if(mValue == 0) {
      view.setVisibility(View.GONE);
    }
    
    super.onBindView(view);
  }
  
  public long getValue() {
    return mValue;
  }
}
