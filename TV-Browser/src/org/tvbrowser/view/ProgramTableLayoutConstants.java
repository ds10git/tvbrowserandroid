package org.tvbrowser.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.widget.TextView;

public class ProgramTableLayoutConstants {
  static final int HOURS = 28;
  
  static final int GRAY_VALUE = 230;
  
  static final Paint BLOCK_PAINT = new Paint();
  static final Paint LINE_PAINT = new Paint();
  
  static final TextPaint TIME_BLOCK_TIME_PAINT = new TextPaint();
  
  static int FONT_SIZE_ASCENT;
  static int COLUMN_WIDTH = -1;
  static int GAP = -1;
  static int ROW_HEADER = -1;
  
  public static void initialize(Context context) {
    if(COLUMN_WIDTH == -1) {
      // Get the screen's density scale
      final float scale = context.getResources().getDisplayMetrics().density;
      // Convert the dps to pixels, based on density scale
      COLUMN_WIDTH = (int) (200 * scale + 0.5f);
      GAP = (int) (1 * scale + 0.5f);
      ROW_HEADER = (int)(scale * 28);
      BLOCK_PAINT.setColor(Color.rgb(GRAY_VALUE, GRAY_VALUE, GRAY_VALUE));
      LINE_PAINT.setColor(Color.LTGRAY);
      
      TIME_BLOCK_TIME_PAINT.setTextSize(scale * 20);
      TIME_BLOCK_TIME_PAINT.setColor(new TextView(context).getTextColors().getDefaultColor());
      FONT_SIZE_ASCENT = Math.abs(TIME_BLOCK_TIME_PAINT.getFontMetricsInt().ascent);
    }
  }
}
