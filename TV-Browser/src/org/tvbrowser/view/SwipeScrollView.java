package org.tvbrowser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class SwipeScrollView extends ScrollView {
  private static final int SWIPE_NONE = -1;
  private static final int SWIPE_LEFT = 0;
  private static final int SWIPE_RIGHT = 1;
  
  private static final int MIN_DISTANCE = 200;
  private float mXStart = 0;
  private float mYStart = 0;
  
  private int mSwipe = SWIPE_NONE;
  
  public SwipeScrollView(Context context) {
    super(context);
  }
  
  public SwipeScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public SwipeScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }
  
  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    boolean intercept = false;
    boolean inRange = false;
    
    if(ev.getAction() == MotionEvent.ACTION_DOWN) {
      mSwipe = SWIPE_NONE;
      mXStart = ev.getX();
      mYStart = ev.getY();
    }
    else if(ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) {
      if(Math.abs(ev.getY()-mYStart) < MIN_DISTANCE/4) {
        inRange = true;
      }
      if(Math.abs(ev.getX()-mXStart) > MIN_DISTANCE) {
        intercept = true;
        
        if(ev.getX()-mXStart < 0) {
          mSwipe = SWIPE_LEFT;
        }
        else {
          mSwipe = SWIPE_RIGHT;
        }
      }
    }
    
    if(!intercept && !inRange) {
      intercept = super.onInterceptTouchEvent(ev);
    }
    
    return intercept;
  }
  
  public boolean isSwipeLeft() {
    return mSwipe == SWIPE_LEFT;
  }
  
  public boolean isSwipeRight() {
    return mSwipe == SWIPE_RIGHT;
  }
  
  public void resetSwipe() {
    mSwipe = SWIPE_NONE;
  }
}
