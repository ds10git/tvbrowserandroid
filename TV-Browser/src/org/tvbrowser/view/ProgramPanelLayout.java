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
package org.tvbrowser.view;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ProgramPanelLayout extends ViewGroup {
  private static final int HOURS = 28;
  
  private static int COLUMN_WIDTH = -1;
  private static int GAP = -1;
  private static int ROW_HEADER = -1;
  
  private ArrayList<Integer> mChannelIDsOrdered;
  private int[] mBlockHeights;
  private int[] mBlockCumulatedHeights;
  private int mBlockSize;
  private Calendar mDay;
  
  private static final Paint BLOCK_PAINT = new Paint();
  private static final Paint LINE_PAINT = new Paint();
  
  private static final TextPaint TIME_BLOCK_TIME_PAINT = new TextPaint();
  
  private static int FONT_SIZE_ASCENT;
  
  private static final int GRAY_VALUE = 230;
  
  public ProgramPanelLayout(Context context, final ArrayList<Integer> channelIDsOrdered, int blockSize, final Calendar day) {
    super(context);
    
    mChannelIDsOrdered = channelIDsOrdered;
    
    if(COLUMN_WIDTH == -1) {
      // Get the screen's density scale
      final float scale = getResources().getDisplayMetrics().density;
      // Convert the dps to pixels, based on density scale
      COLUMN_WIDTH = (int) (200 * scale + 0.5f);
      GAP = (int) (1 * scale + 0.5f);
      ROW_HEADER = (int)(scale * 28);
      BLOCK_PAINT.setColor(Color.rgb(GRAY_VALUE, GRAY_VALUE, GRAY_VALUE));
      LINE_PAINT.setColor(Color.LTGRAY);
      
      TIME_BLOCK_TIME_PAINT.setTextSize(scale * 20);
      TIME_BLOCK_TIME_PAINT.setColor(new TextView(getContext()).getTextColors().getDefaultColor());
      FONT_SIZE_ASCENT = Math.abs(TIME_BLOCK_TIME_PAINT.getFontMetricsInt().ascent);
    }
    
    mBlockHeights = new int[(HOURS/blockSize) + (HOURS % blockSize > 0 ? 1 : 0)];
    mBlockCumulatedHeights = new int[mBlockHeights.length];
    mBlockSize = blockSize;
    mDay = day;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int[][] blockHeightCalc = new int[mBlockHeights.length][mChannelIDsOrdered.size()];
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = mChannelIDsOrdered.indexOf(Integer.valueOf(progPanel.getChannelID()));
      int block = progPanel.getStartHour(mDay) / mBlockSize;
      
      progPanel.measure(COLUMN_WIDTH, heightMeasureSpec);
      blockHeightCalc[block][sortIndex] += progPanel.getMeasuredHeight();
    }
    
    int height = 0;
    
    for(int block = 0; block < blockHeightCalc.length; block++) {
      int maxBlockHeight = 0;
      
      for(int column : blockHeightCalc[block]) {
        maxBlockHeight = Math.max(column, maxBlockHeight);
      }
      
      height += maxBlockHeight;
      mBlockHeights[block] = maxBlockHeight;
      
      if(block < blockHeightCalc.length) {
        mBlockCumulatedHeights[block] = height;
      }
    }
    
    setMeasuredDimension(ROW_HEADER + GAP + (COLUMN_WIDTH+GAP) * mChannelIDsOrdered.size(), height);
  }
  
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int[][] currentBlockHeight = new int[mBlockHeights.length][mChannelIDsOrdered.size()];
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = mChannelIDsOrdered.indexOf(Integer.valueOf(progPanel.getChannelID()));
      int block = progPanel.getStartHour(mDay) / mBlockSize;
      
      int x = l + ROW_HEADER + GAP + sortIndex * (COLUMN_WIDTH + GAP);
      int y = t + currentBlockHeight[block][sortIndex];
      
      if(block > 0) {
        y += mBlockCumulatedHeights[block-1];
      }
      
      currentBlockHeight[block][sortIndex] += progPanel.getMeasuredHeight();
      
      progPanel.layout(x, y, x + COLUMN_WIDTH + GAP, y + progPanel.getMeasuredHeight());
    }
  }
  
  @Override
  protected void dispatchDraw(Canvas canvas) {
    for(int i = 0; i < mBlockHeights.length; i++) {
      if(i % 2 == 1) {
        canvas.drawRect(0, mBlockCumulatedHeights[i-1], canvas.getWidth(), mBlockCumulatedHeights[i-1] + mBlockHeights[i], BLOCK_PAINT);
      }
      
      int y = FONT_SIZE_ASCENT;
      
      if(i > 0) {
        y += mBlockCumulatedHeights[i-1];
      }
      
      int time = i * mBlockSize;
      
      if(time >= 24) {
        time -= 24;
      }
      
      String value = String.valueOf(time);
      
      if(value.length() == 1) {
        value = "0" + value;
      }
      
      float length = TIME_BLOCK_TIME_PAINT.measureText(value);
      
      canvas.drawText(value, ROW_HEADER / 2 - length/2, y, TIME_BLOCK_TIME_PAINT);
    }
    
    for(int i = 0; i < mChannelIDsOrdered.size(); i++) {
      int x = ROW_HEADER + i * (COLUMN_WIDTH + GAP);
      canvas.drawLine(x, 0, x, canvas.getHeight(), LINE_PAINT);
    }
    
    super.dispatchDraw(canvas);
  }
  
  public void clear() {
    for(int i = getChildCount()-1; i >= 0; i--) {
      View view = getChildAt(i);
      removeView(view);
      ((ProgramPanel)view).clear();
      view = null;
    }
  }
}
