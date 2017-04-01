package org.tvbrowser.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/** Spinner extension that calls onItemSelected even when the selection is the same as its previous value 
 * 
 *  Taken from http://stackoverflow.com/questions/5335306/how-can-i-get-an-event-in-android-spinner-when-the-current-selected-item-is-sele
 *  
 * */
@SuppressLint("ClickableViewAccessibility")
public class NDSpinner extends android.support.v7.widget.AppCompatSpinner {
  private GestureDetector mClickDetector;
  private OnCreateContextMenuListener mLongClickListener;
  
  public NDSpinner(Context context) { 
    super(context);
    setClickDetector(context);
  }

  public NDSpinner(Context context, AttributeSet attrs){ 
    super(context, attrs);
    setClickDetector(context);
  }

  public NDSpinner(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setClickDetector(context);
    
  }
  
  private void setClickDetector(Context context) {
    setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return mClickDetector.onTouchEvent(event);
      }
    });
    
    mClickDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      public void onLongPress(MotionEvent e) {
        if(mLongClickListener != null) {
          if(mLongClickListener instanceof Fragment) {
            Activity test = ((Fragment)mLongClickListener).getActivity();
            
            if(test != null) {
              test.openContextMenu(NDSpinner.this);
            }
          }
        }
      }
      
      @Override
      public boolean onSingleTapConfirmed(MotionEvent e) {
        return performClick();
      }
    });
  }
    
  @Override
  public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
    mLongClickListener = l;
    super.setOnCreateContextMenuListener(l);
  }

  @Override 
  public void setSelection(int position) {
    super.setSelection(position);
    
    boolean sameSelected = position == getSelectedItemPosition();
    if (sameSelected) {
      // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
      OnItemSelectedListener listener = getOnItemSelectedListener();
      
      if(listener != null) {
        listener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
      }
    }
  }
  
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return true;
  }
}