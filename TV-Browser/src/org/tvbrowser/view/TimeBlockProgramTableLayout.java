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

public class TimeBlockProgramTableLayout extends ProgramTableLayout {
  //private ArrayList<Integer> mChannelIDsOrdered;
  private int[] mBlockHeights;
  private int[] mBlockCumulatedHeights;
  private int mBlockSize;
  private Calendar mCurrentShownDay;
  
  private boolean mGrowToBlock;
  
  public TimeBlockProgramTableLayout(Context context, final ArrayList<Integer> channelIDsOrdered, int blockSize, final Calendar day, boolean growToBlock) {
    super(context, channelIDsOrdered);
    
    //mChannelIDsOrdered = channelIDsOrdered;
    mGrowToBlock = growToBlock;
        
    mBlockHeights = new int[(ProgramTableLayoutConstants.HOURS/blockSize) + (ProgramTableLayoutConstants.HOURS % blockSize > 0 ? 1 : 0)];
    mBlockCumulatedHeights = new int[mBlockHeights.length];
    mBlockSize = blockSize;
    mCurrentShownDay = day;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int[][] blockHeightCalc = new int[mBlockHeights.length][getColumnCount()];
    int[][] blockProgCount = new int[mBlockHeights.length][getColumnCount()];
    
    int widthSpec = MeasureSpec.makeMeasureSpec(ProgramTableLayoutConstants.COLUMN_WIDTH, MeasureSpec.EXACTLY);
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = getIndexForChannelID(progPanel.getChannelID());
      int block = progPanel.getStartHour(mCurrentShownDay) / mBlockSize;
      
      if(block >= 0 && sortIndex >= 0) {
        progPanel.measure(widthSpec, heightMeasureSpec);
        blockHeightCalc[block][sortIndex] += progPanel.getMeasuredHeight();
        blockProgCount[block][sortIndex]++;
      }
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
    
    if(mGrowToBlock) {
      int[][] blockCurrentProgCount = new int[mBlockHeights.length][getColumnCount()];
      
      for(int i = 0; i < getChildCount(); i++) {
        ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
        
        int sortIndex = getIndexForChannelID(progPanel.getChannelID());
        int block = progPanel.getStartHour(mCurrentShownDay) / mBlockSize;
        
        if(block >= 0 && sortIndex >= 0) {
          int maxBlockHeight = mBlockHeights[block];
          int heightDiff = maxBlockHeight - blockHeightCalc[block][sortIndex];
          int blockProgCountValue = blockProgCount[block][sortIndex];
          
          blockCurrentProgCount[block][sortIndex]++;
          
          int addHeight = heightDiff/blockProgCountValue;
          
          int count = 1;
          
          int endBlock = progPanel.getEndHour(mCurrentShownDay) / mBlockSize;
          
          if(blockCurrentProgCount[block][sortIndex] == blockProgCountValue) {
            while((block + count) < (mBlockHeights.length) && blockProgCount[block + count][sortIndex] == 0 && endBlock > block + count) {
              addHeight += mBlockHeights[block + count++];
            }
            
            if(count == 1) {
              addHeight +=  heightDiff%blockProgCountValue;
            }
          }
          
          int newHeightSpec = MeasureSpec.makeMeasureSpec(progPanel.getMeasuredHeight() + addHeight, MeasureSpec.EXACTLY);
          
          progPanel.measure(widthSpec, newHeightSpec);
        }
      }
    }
    
    setMeasuredDimension(ProgramTableLayoutConstants.ROW_HEADER + ProgramTableLayoutConstants.GAP + (ProgramTableLayoutConstants.COLUMN_WIDTH+ProgramTableLayoutConstants.GAP) * getColumnCount(), height);
  }
  
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int[][] currentBlockHeight = new int[mBlockHeights.length][getColumnCount()];
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = getIndexForChannelID(progPanel.getChannelID());
      int block = progPanel.getStartHour(mCurrentShownDay) / mBlockSize;
      
      if(block >= 0 && sortIndex >= 0) {
        int x = l + ProgramTableLayoutConstants.ROW_HEADER + ProgramTableLayoutConstants.GAP + sortIndex * (ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP);
        int y = t + currentBlockHeight[block][sortIndex];
        
        if(block > 0) {
          y += mBlockCumulatedHeights[block-1];
        }
        
        currentBlockHeight[block][sortIndex] += progPanel.getMeasuredHeight();
        
        progPanel.layout(x, y, x + ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP, y + progPanel.getMeasuredHeight());
      }
    }
  }
  
  @Override
  protected void dispatchDraw(Canvas canvas) {
    for(int i = 0; i < mBlockHeights.length; i++) {
      if(i % 2 == 1) {
        canvas.drawRect(0, mBlockCumulatedHeights[i-1], canvas.getWidth(), mBlockCumulatedHeights[i-1] + mBlockHeights[i], ProgramTableLayoutConstants.BLOCK_PAINT);
      }
      
      int y = ProgramTableLayoutConstants.FONT_SIZE_ASCENT;
      
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
      
      float length = ProgramTableLayoutConstants.TIME_BLOCK_TIME_PAINT.measureText(value);
      
      canvas.drawText(value, ProgramTableLayoutConstants.ROW_HEADER / 2 - length/2, y, ProgramTableLayoutConstants.TIME_BLOCK_TIME_PAINT);
    }
    
    for(int i = 0; i < getColumnCount(); i++) {
      int x = ProgramTableLayoutConstants.ROW_HEADER + i * (ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP);
      canvas.drawLine(x, 0, x, canvas.getHeight(), ProgramTableLayoutConstants.LINE_PAINT);
    }
    
    super.dispatchDraw(canvas);
  }
}
