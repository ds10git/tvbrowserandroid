package org.tvbrowser.view;

import org.tvbrowser.tvbrowser.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class SeparatorDrawable extends Drawable {
  private Paint mLineColor = new Paint();
  private Paint mDividerColor = new Paint();
  
  public SeparatorDrawable(Context context) {
    mLineColor.setStyle(Paint.Style.STROKE);
    mLineColor.setColor(context.getResources().getColor(R.color.running_separator_color));
    
    mDividerColor.setStyle(Paint.Style.FILL_AND_STROKE);
    mDividerColor.setColor(context.getResources().getColor(R.color.separator_color_light));
  }
  
  @Override
  public void draw(Canvas canvas) {
    canvas.drawLine(getBounds().left, getBounds().top, getBounds().width(), getBounds().top, mLineColor);
    
    if(getBounds().height() > 2) {
      canvas.drawRect(getBounds().left, getBounds().top+1, getBounds().width(), getBounds().bottom-1, mDividerColor);
      canvas.drawLine(getBounds().left, getBounds().bottom-1, getBounds().width(), getBounds().bottom-1, mLineColor);
    }
  }

  @Override
  public int getOpacity() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setAlpha(int alpha) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    // TODO Auto-generated method stub
    
  }
  
}
