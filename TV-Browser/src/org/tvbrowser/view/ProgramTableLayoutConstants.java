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

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.widget.TextView;

public class ProgramTableLayoutConstants {
  static final int HOURS = 28;
  
  static final int GRAY_VALUE = 100;
  
  static final Paint BLOCK_PAINT = new Paint();
  static final Paint LINE_PAINT = new Paint();
  static final Paint CHANNEL_LINE_PAINT = new Paint();
  
  static final TextPaint TIME_BLOCK_TIME_PAINT = new TextPaint();
  
  static int FONT_SIZE_ASCENT;
  static int COLUMN_WIDTH = -1;
  static int GAP = -1;
  static int ROW_HEADER = -1;
  static int TIME_TITLE_GAP = -1;
  static int PADDING_SIDE = 0;
  
  static int BIG_FONT_DESCEND;
  static int BIG_MAX_FONT_HEIGHT;
  
  static int SMALL_FONT_DESCEND;
  static int SMALL_MAX_FONT_HEIGHT;
  
  static int SUPER_SMALL_FONT_DESCEND;
  static int SUPER_SMALL_MAX_FONT_HEIGHT;
  
  static int CHANNEL_FONT_DESCEND;
  static int CHANNEL_MAX_FONT_HEIGHT;
  
  static final Paint CHANNEL_BACKGROUND_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
  
  static int CHANNEL_BAR_HEIGHT;
  
  static final TextPaint CHANNEL_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
  
  static final TextPaint NOT_EXPIRED_TITLE_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
  static final TextPaint EXPIRED_TITLE_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
  static final TextPaint NOT_EXPIRED_GENRE_EPISODE_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
  static final TextPaint EXPIRED_GENRE_EPISODE_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);

  static final TextPaint NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
  static final TextPaint EXPIRED_PICTURE_COPYRIGHT_PAINT = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
  
  static java.text.DateFormat TIME_FORMAT;
  
  static boolean SHOW_LOGO;
  static boolean SHOW_NAME;
  static boolean SHOW_ORDER_NUMBER;
  
  private static int RAW_COLUMN_WIDTH;
  private static float TEXT_SCALE;
  
  static float ZOOM = 1.0f;
  
  static {
    NOT_EXPIRED_TITLE_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    
    //EXPIRED_TITLE_PAINT.setColor(SettingConstants.EXPIRED_COLOR);
    EXPIRED_TITLE_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    
    NOT_EXPIRED_GENRE_EPISODE_PAINT.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
    
    //EXPIRED_GENRE_EPISODE_PAINT.setColor(SettingConstants.EXPIRED_COLOR);
    EXPIRED_GENRE_EPISODE_PAINT.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
    
    NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT.setTypeface(Typeface.DEFAULT);
    
    EXPIRED_PICTURE_COPYRIGHT_PAINT.setTypeface(Typeface.DEFAULT);
    //EXPIRED_PICTURE_COPYRIGHT_PAINT.setColor(SettingConstants.EXPIRED_COLOR);
    
    CHANNEL_PAINT.setTypeface(Typeface.DEFAULT);
  }
  
  public static void initialize(Context context) {
    
    //if(COLUMN_WIDTH == -1) {
      update(context);
    //}
  }
  
  public static void updateColumnWidth(Context context) {
    RAW_COLUMN_WIDTH = PrefUtils.getIntValueWithDefaultKey(R.string.PROG_TABLE_COLUMN_WIDTH, R.integer.prog_table_column_width_default);
    
    final float scale = context.getResources().getDisplayMetrics().density;
    // Convert the dps to pixels, based on density scale
    COLUMN_WIDTH = (int) (RAW_COLUMN_WIDTH * scale + 0.5f);
  }
  
  public static void updateChannelLogoName(Context context) {
    String value = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE),"0");
    
    SHOW_LOGO = !value.equals("2");
    SHOW_NAME = !value.equals("1");
    SHOW_ORDER_NUMBER = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_PROGRAM_TABLE, R.bool.show_sort_number_in_program_table_default);
  }
  
  public static void update(Context context) {
    updateChannelLogoName(context);
    RAW_COLUMN_WIDTH = PrefUtils.getIntValueWithDefaultKey(R.string.PROG_TABLE_COLUMN_WIDTH, R.integer.prog_table_column_width_default);
    
    // Get the screen's density scale 
    final float scale = context.getResources().getDisplayMetrics().density;
    ZOOM = scale;
    // Convert the dps to pixels, based on density scale
    COLUMN_WIDTH = (int) (RAW_COLUMN_WIDTH * scale + 0.5f);
    GAP = (int) (1 * scale + 0.5f);
    TIME_TITLE_GAP = (int) (5 * scale + 0.5f);
    ROW_HEADER = (int)(scale * 28);
    
    EXPIRED_TITLE_PAINT.setColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, context));
    EXPIRED_GENRE_EPISODE_PAINT.setColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, context));
    EXPIRED_PICTURE_COPYRIGHT_PAINT.setColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, context));
        
    BLOCK_PAINT.setColor(Color.argb(30,GRAY_VALUE, GRAY_VALUE, GRAY_VALUE));
    LINE_PAINT.setColor(Color.argb(60, GRAY_VALUE, GRAY_VALUE, GRAY_VALUE));
    CHANNEL_LINE_PAINT.setColor(context.getResources().getColor(R.color.light_gray));
    
    PADDING_SIDE = (int) (2 * scale + 0.5f);
    
    TIME_BLOCK_TIME_PAINT.setTextSize(scale * 20);
    TIME_BLOCK_TIME_PAINT.setColor(new TextView(context).getTextColors().getDefaultColor());
    FONT_SIZE_ASCENT = Math.abs(TIME_BLOCK_TIME_PAINT.getFontMetricsInt().ascent);
    
    CHANNEL_BACKGROUND_PAINT.setColor(context.getResources().getColor(R.color.prog_table_channel_background));
    CHANNEL_BACKGROUND_PAINT.setStyle(Paint.Style.FILL_AND_STROKE);
    
    TIME_FORMAT = DateFormat.getTimeFormat(context);
    String value = ((SimpleDateFormat)TIME_FORMAT).toLocalizedPattern();
    
    if((value.charAt(0) == 'H' && value.charAt(1) != 'H') || (value.charAt(0) == 'h' && value.charAt(1) != 'h')) {
      value = value.charAt(0) + value;
    }
    
    CHANNEL_BAR_HEIGHT = context.getResources().getDimensionPixelSize(R.dimen.prog_table_channel_height);
    
    CHANNEL_PAINT.setColor(context.getResources().getColor(R.color.prog_table_channel_color));
    CHANNEL_PAINT.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.prog_table_channel_font_size));
    CHANNEL_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    
    CHANNEL_FONT_DESCEND = Math.abs(CHANNEL_PAINT.getFontMetricsInt().descent);
    CHANNEL_MAX_FONT_HEIGHT = CHANNEL_FONT_DESCEND + Math.abs(CHANNEL_PAINT.getFontMetricsInt().ascent)+1;
    
    TIME_FORMAT = new SimpleDateFormat(value, Locale.getDefault());
    
    TEXT_SCALE = Float.valueOf(PrefUtils.getStringValue(R.string.PROG_TABLE_TEXT_SCALE, R.string.prog_table_text_scale_default));
    
    float textSize = new TextView(context).getTextSize() * TEXT_SCALE;
    float smallTextSize = (10 * scale + 0.5f) * TEXT_SCALE;
    int color = new TextView(context).getTextColors().getDefaultColor();
    
    NOT_EXPIRED_TITLE_PAINT.setTextSize(textSize);
    NOT_EXPIRED_TITLE_PAINT.setColor(color);
    
    NOT_EXPIRED_GENRE_EPISODE_PAINT.setTextSize(textSize);
    NOT_EXPIRED_GENRE_EPISODE_PAINT.setColor(color);
    
    NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT.setTextSize(smallTextSize);
    NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT.setColor(color);
    
    EXPIRED_TITLE_PAINT.setTextSize(textSize);
    EXPIRED_GENRE_EPISODE_PAINT.setTextSize(textSize);
    EXPIRED_PICTURE_COPYRIGHT_PAINT.setTextSize(smallTextSize);
    
    BIG_FONT_DESCEND = Math.abs(NOT_EXPIRED_TITLE_PAINT.getFontMetricsInt().descent);
    BIG_MAX_FONT_HEIGHT = BIG_FONT_DESCEND + Math.abs(NOT_EXPIRED_TITLE_PAINT.getFontMetricsInt().ascent)+1;
    
    SMALL_FONT_DESCEND = Math.abs(NOT_EXPIRED_GENRE_EPISODE_PAINT.getFontMetricsInt().descent);
    SMALL_MAX_FONT_HEIGHT = SMALL_FONT_DESCEND + Math.abs(NOT_EXPIRED_GENRE_EPISODE_PAINT.getFontMetricsInt().ascent)+1;
    
    SUPER_SMALL_FONT_DESCEND = Math.abs(NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT.getFontMetricsInt().descent);
    SUPER_SMALL_MAX_FONT_HEIGHT = SUPER_SMALL_FONT_DESCEND + Math.abs(NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT.getFontMetricsInt().ascent)+1;
  }
  
  public static int getRawColumnWidth() {
    return RAW_COLUMN_WIDTH;
  }
  
  public static float getTextScale() {
    return TEXT_SCALE;
  }
  
  public static int getChannelBarHeight() {
    return CHANNEL_BAR_HEIGHT;
  }
  
  public static int getChannelMaxFontHeight() {
    return CHANNEL_MAX_FONT_HEIGHT;
  }
  
  public static boolean getShowOrderNumber() {
    return SHOW_ORDER_NUMBER;
  }
  
  public static float getZoom() {
    return ZOOM;
  }
}
