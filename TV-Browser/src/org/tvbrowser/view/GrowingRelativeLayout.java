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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class GrowingRelativeLayout extends RelativeLayout {
  public GrowingRelativeLayout(Context context) {
    super(context);
  }
  
  public GrowingRelativeLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public GrowingRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    boolean grow = getChildAt(1) instanceof LinearLayout;
    
    for(int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      
      grow = grow || (child.getVisibility() == View.GONE);
      
      if(child instanceof LinearLayout) {
        if(grow) {
          RelativeLayout.LayoutParams para = (RelativeLayout.LayoutParams)child.getLayoutParams();

          para.height = getMeasuredHeight();
        }
      }
    }
    
    if(grow) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }
  
  /*
   * Possible bug fix for bug:
   * https://code.google.com/p/android/issues/detail?id=55933
   */
  @Override
  public void addChildrenForAccessibility(ArrayList<View> childrenForAccessibility) {}
}
