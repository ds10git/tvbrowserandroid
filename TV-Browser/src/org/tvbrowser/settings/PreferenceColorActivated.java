package org.tvbrowser.settings;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.UiUtils;
import org.tvbrowser.view.ColorView;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;

public class PreferenceColorActivated extends DialogPreference {
  private boolean mAlwaysActivated;
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
        TypedValue typedValue =  new TypedValue();
        context.getResources().getValue(resId, typedValue, true);
        
        int[] values = new int[2];
            
        if(typedValue.type == TypedValue.TYPE_STRING) {
          values = IOUtils.getActivatedColorFor(typedValue.string.toString());
        }
        else {
          values[0] = 1;
          values[1] = ContextCompat.getColor(context, resId);
        }
               
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
    
    String xmlns = "http://schemas.android.com/apk/res-auto";
    
    mAlwaysActivated = attrs.getAttributeBooleanValue(xmlns, "alwaysActivated", false);
    
    if(mAlwaysActivated) {
      mActivated = true;
    }
    
    setDialogLayoutResource(org.tvbrowser.tvbrowser.R.layout.color_preference_dialog);
    setWidgetLayoutResource(org.tvbrowser.tvbrowser.R.layout.widget_color_activated);
  }
  
  
  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    
    mActivatedSelection = view.findViewById(R.id.widget_color_activated_selection);
    mColorView = view.findViewById(R.id.widget_color_activated_color);
    
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
      
      if(mAlwaysActivated) {
        persistInt(mColor);
      }
      else{
        String value = String.valueOf(mActivated) + ";" + String.valueOf(mColor);
        
        if (callChangeListener(value)) {          
          persistString(value);
        }
      }
    }
  }
  
  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    
    mDialogActivatedSelection = view.findViewById(R.id.color_pref_color_activated);
    
    if(mAlwaysActivated) {
      mDialogActivatedSelection.setVisibility(View.GONE);
      mDialogActivatedSelection.setChecked(true);
    }
    else {
      mDialogActivatedSelection.setVisibility(View.VISIBLE);
      mDialogActivatedSelection.setChecked(mActivated);
    }
    
    mDialogColorView = view.findViewById(R.id.color_pref_color_view);
    mDialogColorView.setColor(mColor);
    
    int[] colors = UiUtils.getColorValues(mColor);
    
    final SeekBar red = view.findViewById(R.id.color_pref_red1);
    final SeekBar green = view.findViewById(R.id.color_pref_green1);
    final SeekBar blue = view.findViewById(R.id.color_pref_blue1);
    final SeekBar alpha = view.findViewById(R.id.color_pref_alpha1);
    final EditText hex = view.findViewById(R.id.color_pref_hex_input);
    
    final Button reset = view.findViewById(R.id.color_pref_reset);
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
    hex.setText(String.format("%08x", mColor));

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
          
          int color = UiUtils.getColorForValues(colorValues);
          
          mDialogColorView.setColor(color);
          hex.setText(String.format("%08x", color));
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
        hex.setEnabled(isChecked);
        reset.setEnabled(isChecked);
        red.setEnabled(isChecked);
        green.setEnabled(isChecked);
        blue.setEnabled(isChecked);
        alpha.setEnabled(isChecked);
      }
    };
    
    hex.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
        
      }
      
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // TODO Auto-generated method stub
        
      }
      
      @Override
      public void afterTextChanged(Editable s) {
        String value = s.toString();
                  
        if(value.trim().length() == 8) {
          try {
            int[] colorValues = UiUtils.getColorValues((int)Long.parseLong(value, 16));
            
            alpha.setProgress(colorValues[0]);
            red.setProgress(colorValues[1]);
            green.setProgress(colorValues[2]);
            blue.setProgress(colorValues[3]);
          }catch(NumberFormatException nfe) {
            Log.d("info4", "", nfe);
          }
        }
      }
    });
    
    checkChangeListener.onCheckedChanged(mActivatedSelection, mActivated);
    
    mDialogActivatedSelection.setOnCheckedChangeListener(checkChangeListener);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    TypedValue v = new TypedValue();
    a.getValue(index, v);
    
    if(v.type == TypedValue.TYPE_STRING) {
      return v.string.toString();
    }
    else {
      return v.data;
    }
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if(!restorePersistedValue) {
      if(defaultValue == null) {
        if(mAlwaysActivated) {
          mActivated = true;
          mColor = getPersistedInt(-16777216);
        }
        else {
          int[] values = IOUtils.getActivatedColorFor("false;-16777216");
          
          mActivated = values[0] == 1;
          mColor = values[1];
        }
      }
      else {
        if(mAlwaysActivated) {
          mActivated = true;
          mColor = (Integer)defaultValue;
        }
        else {
          int[] values = IOUtils.getActivatedColorFor((String)defaultValue);
          
          mActivated = values[0] == 1;
          mColor = values[1];
        }
      }
    }
    else {
      if(defaultValue == null) {
        if(mAlwaysActivated) {
          mActivated = true;
          mColor = getPersistedInt(-16777216);
        }
        else {
          int[] values = IOUtils.getActivatedColorFor(getPersistedString("false;-16777216"));
          
          mActivated = values[0] == 1;
          mColor = values[1];
        }
      }
      else {
        if(mAlwaysActivated) {
          mActivated = true;
          mColor = getPersistedInt((Integer)defaultValue);
        }
        else {
          int[] values = IOUtils.getActivatedColorFor(getPersistedString((String)defaultValue));
          
          mActivated = values[0] == 1;
          mColor = values[1];
        }
      }
    }
  }
  
  public void setColors(int color, int defaultColor) {
    mColor = color;
    
    if(mAlwaysActivated) {
      persistInt(color);
    }
    else {
      persistString(String.valueOf(mActivated) + ";" + String.valueOf(mColor));
    }
    
    mDefaultColor = defaultColor;
  }
  
  public int getColor() {
    return mColor;
  }
}
