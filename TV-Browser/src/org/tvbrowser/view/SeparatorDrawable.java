/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
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

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.PrefUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class SeparatorDrawable extends Drawable {
  private final Paint mLineColor = new Paint();
  private final Paint mDividerColor = new Paint();
  
  public SeparatorDrawable(Context context) {
    updateColors(context);
  }
  
  public void updateColors(Context context) {
    PrefUtils.initialize(context.getApplicationContext());
    
    mLineColor.setStyle(Paint.Style.STROKE);
    mLineColor.setColor(PrefUtils.getIntValue(R.string.PREF_COLOR_SEPARATOR_LINE, ContextCompat.getColor(context, R.color.pref_color_separator_line)));
    
    mDividerColor.setStyle(Paint.Style.FILL_AND_STROKE);
    mDividerColor.setColor(PrefUtils.getIntValue(R.string.PREF_COLOR_SEPARATOR_SPACE, ContextCompat.getColor(context, R.color.pref_color_separator_space)));
  }
  
  @Override
  public void draw(@NonNull Canvas canvas) {
    canvas.drawLine(getBounds().left, getBounds().top, getBounds().width(), getBounds().top, mLineColor);
    
    if(getBounds().height() > 2) {
      canvas.drawRect(getBounds().left, getBounds().top+1, getBounds().width(), getBounds().bottom-1, mDividerColor);
      canvas.drawLine(getBounds().left, getBounds().bottom-1, getBounds().width(), getBounds().bottom-1, mLineColor);
    }
  }

  @Override
  public int getOpacity() {
    // TODO Auto-generated method stub
    return PixelFormat.UNKNOWN;
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
