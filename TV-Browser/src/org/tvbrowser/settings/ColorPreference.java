/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
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
import org.tvbrowser.tvbrowser.UiUtils;
import org.tvbrowser.view.ColorView;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

/**
 * A dialog preference to configure color values.
 * <p>
 * @author René Mach
 */
public class ColorPreference extends DialogPreference {
  private ColorView mColorView;
  private ColorView mDialogColorView;
  private int mColor;
  private int mDefaultColor;
  
  public ColorPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context,attrs);
  }
  
  public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context,attrs);
  }
  
  private void init(Context context, AttributeSet attrs) {
    String namespace = "http://schemas.android.com/apk/res/android";
    
    mDefaultColor = attrs.getAttributeUnsignedIntValue(namespace, "defaultValue", -1);
    
    if(mDefaultColor == -1) {
      int resId =  attrs.getAttributeResourceValue(namespace, "defaultValue", -1);
      
      if(resId != -1) {
        mDefaultColor = context.getResources().getColor(resId);
      }
      else {
        mDefaultColor = 0;
      }
    }
    
    setDialogLayoutResource(org.tvbrowser.tvbrowser.R.layout.color_preference_dialog);
    setWidgetLayoutResource(org.tvbrowser.tvbrowser.R.layout.color_widget);
  }
  
  
  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    
    mColorView = (ColorView)view.findViewById(R.id.color_view);
    
    mColorView.setColor(mColor);
  }
  
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    
    if(positiveResult && mDialogColorView != null) {
      mColor = mDialogColorView.getColor();
      mColorView.setColor(mColor);
      
      if (callChangeListener(mColor)) {
        persistInt(mColor);
      }
    }
  }
  
  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    
    mDialogColorView = (ColorView)view.findViewById(R.id.color_pref_color_view);
    mDialogColorView.setColor(mColor);
    
    int[] colors = UiUtils.getColorValues(mColor);
    
    final SeekBar red = (SeekBar)view.findViewById(R.id.color_pref_red1);
    final SeekBar green = (SeekBar)view.findViewById(R.id.color_pref_green1);
    final SeekBar blue = (SeekBar)view.findViewById(R.id.color_pref_blue1);
    final SeekBar alpha = (SeekBar)view.findViewById(R.id.color_pref_alpha1);
    
    Button reset = (Button)view.findViewById(R.id.color_pref_reset);
    reset.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int[] colors = UiUtils.getColorValues(mDefaultColor);
        
        red.setProgress(colors[1]);
        green.setProgress(colors[2]);
        blue.setProgress(colors[3]);
        alpha.setProgress(colors[0]);
      }
    });
    
    red.setProgress(colors[1]);
    green.setProgress(colors[2]);
    blue.setProgress(colors[3]);
    alpha.setProgress(colors[0]);

    SeekBar.OnSeekBarChangeListener changeListener = new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
      
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}
      
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int index = -1;
        
        if(seekBar.equals(red)) {
          index = 1;
        }
        else if(seekBar.equals(green)) {
          index = 2;
        }
        else if(seekBar.equals(blue)) {
          index = 3;
        }
        else if(seekBar.equals(alpha)) {
          index = 0;
        }
        if(index >= 0) {
          int[] colorValues = UiUtils.getColorValues(mDialogColorView.getColor());
          colorValues[index] = progress;
          
          mDialogColorView.setColor(UiUtils.getColorForValues(colorValues));
        }
      }
    };
    
    red.setOnSeekBarChangeListener(changeListener);
    green.setOnSeekBarChangeListener(changeListener);
    blue.setOnSeekBarChangeListener(changeListener);
    alpha.setOnSeekBarChangeListener(changeListener);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getInt(index, 0));
  }
  

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if(restorePersistedValue) {
      if(defaultValue == null) {
        mColor = getPersistedInt(0);
      }
      else {
        mColor = getPersistedInt((Integer)defaultValue);
      }
    }
    else {
      mColor = (Integer)defaultValue;
    }
  }
  
  public void setColors(int color, int defaultColor) {
    mColor = color;
    persistInt(mColor);
    
    mDefaultColor = defaultColor;
  }
  
  public int getColor() {
    return mColor;
  }
}
