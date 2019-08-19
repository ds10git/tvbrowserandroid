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
import android.graphics.Canvas;
import android.util.AttributeSet;

public class CompactProgramTableLayout extends ProgramTableLayout {

  /** View constructors for XML inflation (used by tools) */
  public CompactProgramTableLayout(Context context, AttributeSet attributeSet, int defStyleAttr) {
    super(context, attributeSet, defStyleAttr);
  }

  public CompactProgramTableLayout(Context context, final ArrayList<Integer> channelIDsOrdered) {
    super(context, channelIDsOrdered);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthSpec = MeasureSpec.makeMeasureSpec(ProgramTableLayoutConstants.COLUMN_WIDTH, MeasureSpec.EXACTLY);
    int[] currentColumnHeight = new int[getColumnCount()];
    
    int maxHeight = 0;
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = getIndexForChannelID(progPanel.getChannelID());
      
      if(sortIndex >= 0) {
        progPanel.measure(widthSpec, 0);
        
        currentColumnHeight[sortIndex] += progPanel.getMeasuredHeight();
        
        maxHeight = Math.max(maxHeight, currentColumnHeight[sortIndex]);
      }
    }
    
    setMeasuredDimension((ProgramTableLayoutConstants.COLUMN_WIDTH+ProgramTableLayoutConstants.GAP) * getColumnCount(), maxHeight);
  }
  
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int[] currentColumnHeight = new int[getColumnCount()];
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = getIndexForChannelID(progPanel.getChannelID());
      
      if(sortIndex >= 0) {
        int x = l + sortIndex * (ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP);
        int y = t + currentColumnHeight[sortIndex];
              
        currentColumnHeight[sortIndex] += progPanel.getMeasuredHeight();
        
        if(progPanel.getVisibility() != GONE) {
          progPanel.layout(x, y, x + ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP, y + progPanel.getMeasuredHeight());
        }
      }
    }
  }
  
  @Override
  protected void dispatchDraw(Canvas canvas) {
    for(int i = 0; i < getColumnCount(); i++) {
      int x = i * (ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP);
      canvas.drawLine(x, 0, x, canvas.getHeight(), ProgramTableLayoutConstants.LINE_PAINT);
    }
    
    super.dispatchDraw(canvas);
  }
}