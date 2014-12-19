package org.tvbrowser.settings;

import org.tvbrowser.tvbrowser.IOUtils;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.UiUtils;
import org.tvbrowser.view.ColorView;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

public class PreferenceColorActivated extends DialogPreference {
  private boolean mActivated;
  private CheckBox mActivatedSelection;
  private ColorView mColorView;
  private CheckBox mDialogActivatedSelection;
  private ColorView mDialogColorView;
  private int mColor;
  private int mDefaultColor;
  
  public PreferenceColorActivated(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context,attrs);
  }
  
  public PreferenceColorActivated(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context,attrs);
  }
  
  private void init(Context context, AttributeSet attrs) {
    String namespace = "http://schemas.android.com/apk/res/android";
    
    String value = attrs.getAttributeValue(namespace,  "defaultValue");
    
    if(value != null) {
      int resId =  attrs.getAttributeResourceValue(namespace, "defaultValue", -1);
      
      if(resId != -1) {
        int[] values = IOUtils.getColorForCategory(context.getResources().getString(resId));
                
        mActivated = values[0] == 1;
        mColor = mDefaultColor = values[1];
      }
      else {
        mActivated = false;
        mColor = mDefaultColor = -16777216;
      }
    }
    else {
      mActivated = false;
      mColor = mDefaultColor = -16777216;
    }
    
    setDialogLayoutResource(org.tvbrowser.tvbrowser.R.layout.color_preference_dialog);
    setWidgetLayoutResource(org.tvbrowser.tvbrowser.R.layout.widget_color_activated);
  }
  
  
  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    
    mActivatedSelection = (CheckBox)view.findViewById(R.id.widget_color_activated_selection);
    mColorView = (ColorView)view.findViewById(R.id.widget_color_activated_color);
    
    mActivatedSelection.setChecked(mActivated);
    mColorView.setColor(mColor);
    
    handleVisiblity();
  }
  
  private void handleVisiblity() {
    if(mActivated) {
      mColorView.setVisibility(View.VISIBLE);
      mActivatedSelection.setVisibility(View.GONE);
    }
    else {
      mColorView.setVisibility(View.GONE);
      mActivatedSelection.setVisibility(View.VISIBLE);
    }
  }
  
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    
    if(positiveResult && mDialogColorView != null) {
      mColor = mDialogColorView.getColor();
      mColorView.setColor(mColor);
      mActivated = mDialogActivatedSelection.isChecked();
      mActivatedSelection.setChecked(mActivated);
      
      handleVisiblity();
      
      String value = String.valueOf(mActivated) + ";" + String.valueOf(mColor);
      
      if (callChangeListener(value)) {
        persistString(value);
      }
    }
  }
  
  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    
    mDialogActivatedSelection = (CheckBox)view.findViewById(R.id.color_pref_color_activated);
    mDialogActivatedSelection.setVisibility(View.VISIBLE);
    mDialogActivatedSelection.setChecked(mActivated);
    mDialogColorView = (ColorView)view.findViewById(R.id.color_pref_color_view);
    mDialogColorView.setColor(mColor);
    
    int[] colors = UiUtils.getColorValues(mColor);
    
    final SeekBar red = (SeekBar)view.findViewById(R.id.color_pref_red1);
    final SeekBar green = (SeekBar)view.findViewById(R.id.color_pref_green1);
    final SeekBar blue = (SeekBar)view.findViewById(R.id.color_pref_blue1);
    final SeekBar alpha = (SeekBar)view.findViewById(R.id.color_pref_alpha1);
    
    final Button reset = (Button)view.findViewById(R.id.color_pref_reset);
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
    
    CompoundButton.OnCheckedChangeListener checkChangeListener = new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mDialogColorView.setEnabled(isChecked);
        reset.setEnabled(isChecked);
        red.setEnabled(isChecked);
        green.setEnabled(isChecked);
        blue.setEnabled(isChecked);
        alpha.setEnabled(isChecked);
      }
    };
    
    checkChangeListener.onCheckedChanged(mActivatedSelection, mActivated);
    
    mDialogActivatedSelection.setOnCheckedChangeListener(checkChangeListener);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getString(0));
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if(!restorePersistedValue) {
      if(defaultValue == null) {
        int[] values = IOUtils.getColorForCategory("false;-16777216");
        
        mActivated = values[0] == 1;
        mColor = values[1];
      }
      else {
        int[] values = IOUtils.getColorForCategory((String)defaultValue);
        
        mActivated = values[0] == 1;
        mColor = values[1];
      }
    }
    else {
      int[] values = IOUtils.getColorForCategory(getPersistedString((String)defaultValue));
      
      mActivated = values[0] == 1;
      mColor = values[1];
    }
  }
}
