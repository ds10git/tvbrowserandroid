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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class ZoomableFrameLayout extends FrameLayout {
  
//  private float mScaleFactor = 1.f;
//  private ScaleGestureDetector mScaleDetector;
  
  
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
  /*
  @Override
  public boolean onTouchEvent(MotionEvent ev) {
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
  */
  /*
   * Possible bug fix for bug:
   * https://code.google.com/p/android/issues/detail?id=55933
   */
  @Override
  public void addChildrenForAccessibility(ArrayList<View> childrenForAccessibility) {}
}
