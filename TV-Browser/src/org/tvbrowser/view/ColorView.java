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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import org.tvbrowser.utils.UiUtils;

/**
 * A view that displays a color with a border.
 * 
 * @author René Mach
 */
public class ColorView extends View {
  private Paint mCheckerBoardPaint;
  private final Paint mColorPaint = new Paint();
  private final Paint mBorderPaint = new Paint();
  
  public ColorView(Context context) {
    super(context);
    
    init(context);
  }
  
  public ColorView(Context context, AttributeSet attrs) {
    super(context, attrs);
    
    init(context);
  }
  
  public ColorView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    
    init(context);
  }
  
  private void init(Context context) {
    mColorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    
    mCheckerBoardPaint = createCheckerBoard(UiUtils.convertDpToPixel(16, context.getResources()));
    mBorderPaint.setStyle(Paint.Style.STROKE);
    mBorderPaint.setColor(new TextView(context).getTextColors().getDefaultColor());
  }
  
  public void setColor(int color) {
    mColorPaint.setColor(color);
    invalidate();
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawRect(getPaddingLeft() + 2, getPaddingTop() + 2, getWidth() - getPaddingLeft() - getPaddingRight() - 1, getHeight() - getPaddingTop() - getPaddingBottom() - 1, mCheckerBoardPaint);
    canvas.drawRect(getPaddingLeft() + 1, getPaddingTop() + 1, getWidth() - getPaddingLeft() - getPaddingRight() - 1, getHeight() - getPaddingTop() - getPaddingBottom() - 1, mColorPaint);
    canvas.drawRect(getPaddingLeft() + 1, getPaddingTop() + 1, getWidth() - getPaddingLeft() - getPaddingRight() - 1, getHeight() - getPaddingTop() - getPaddingBottom() - 1, mBorderPaint);
  }
  
  public int getColor() {
    return mColorPaint.getColor();
  }

  private static Paint createCheckerBoard(final int pixelSize)
  {
    final Bitmap bitmap = Bitmap.createBitmap(pixelSize * 2, pixelSize * 2, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint paint = new Paint();
    final Rect rect = new Rect(0, 0, pixelSize, pixelSize);

    paint.setColor(Color.WHITE);
    canvas.drawPaint(paint);
    paint.setColor(Color.GRAY);
    canvas.drawRect(rect, paint);
    rect.offset(pixelSize, pixelSize);
    canvas.drawRect(rect, paint);

    paint.reset();
    paint.setShader(new BitmapShader(bitmap, BitmapShader.TileMode.REPEAT, BitmapShader.TileMode.REPEAT));

    return paint;
  }
}