package org.tvbrowser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

public class ZoomableFrameLayout extends FrameLayout {
  
  private float mScaleFactor = 1.f;
  private ScaleGestureDetector mScaleDetector;
  
  
  public ZoomableFrameLayout(Context context) {
    super(context);
    init(context);
  }
  
  public ZoomableFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }
  
  public ZoomableFrameLayout(Context context, AttributeSet attrs,
      int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }
  
  private void init(Context context) {
   // mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    //mScaleDetector.onTouchEvent(ev);
    
    return super.dispatchTouchEvent(ev);
  }
  
  @Override
  public boolean onTouchEvent(MotionEvent ev) {Log.d("info3","ddd");
    
    return super.onTouchEvent(ev);
  }
  
  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      // TODO Auto-generated method stub
      
      return true;
    }
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScaleFactor *= detector.getScaleFactor();
        
        mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
        setScaleX(mScaleFactor);
        setScaleY(mScaleFactor);
        
        invalidate();
        return true;
    }
    
    
}
}
