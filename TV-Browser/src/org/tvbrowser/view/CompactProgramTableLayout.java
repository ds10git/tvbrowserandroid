package org.tvbrowser.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;

public class CompactProgramTableLayout extends ProgramTableLayout {
  private ArrayList<Integer> mChannelIDsOrdered;

  public CompactProgramTableLayout(Context context, final ArrayList<Integer> channelIDsOrdered) {
    super(context);
    
    mChannelIDsOrdered = channelIDsOrdered;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthSpec = MeasureSpec.makeMeasureSpec(ProgramTableLayoutConstants.COLUMN_WIDTH, MeasureSpec.EXACTLY);
    int[] currentColumnHeight = new int[mChannelIDsOrdered.size()];
    
    int maxHeight = 0;
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = mChannelIDsOrdered.indexOf(Integer.valueOf(progPanel.getChannelID()));
      
      progPanel.measure(widthSpec, heightMeasureSpec);
      
      currentColumnHeight[sortIndex] += progPanel.getMeasuredHeight();
      
      maxHeight = Math.max(maxHeight, currentColumnHeight[sortIndex]);
    }
    
    setMeasuredDimension(ProgramTableLayoutConstants.ROW_HEADER + ProgramTableLayoutConstants.GAP + (ProgramTableLayoutConstants.COLUMN_WIDTH+ProgramTableLayoutConstants.GAP) * mChannelIDsOrdered.size(), maxHeight);
  }
  
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int[] currentColumnHeight = new int[mChannelIDsOrdered.size()];
    
    for(int i = 0; i < getChildCount(); i++) {
      ProgramPanel progPanel = (ProgramPanel)getChildAt(i);
      
      int sortIndex = mChannelIDsOrdered.indexOf(Integer.valueOf(progPanel.getChannelID()));
            
      int x = l + ProgramTableLayoutConstants.ROW_HEADER + ProgramTableLayoutConstants.GAP + sortIndex * (ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP);
      int y = t + currentColumnHeight[sortIndex];
            
      currentColumnHeight[sortIndex] += progPanel.getMeasuredHeight();
      
      progPanel.layout(x, y, x + ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP, y + progPanel.getMeasuredHeight());
    }
  }
  
  @Override
  protected void dispatchDraw(Canvas canvas) {
    for(int i = 0; i < mChannelIDsOrdered.size(); i++) {
      int x = ProgramTableLayoutConstants.ROW_HEADER + i * (ProgramTableLayoutConstants.COLUMN_WIDTH + ProgramTableLayoutConstants.GAP);
      canvas.drawLine(x, 0, x, canvas.getHeight(), ProgramTableLayoutConstants.LINE_PAINT);
    }
    
    super.dispatchDraw(canvas);
  }
}
