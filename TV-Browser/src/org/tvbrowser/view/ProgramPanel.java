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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.TextPaint;
import android.view.View;

public class ProgramPanel extends View {
  private Date mStartTime;
  private final long mEndTime;
  private String mTitle;
  private String mEpisode;
  private String mGenre;
  private String mPictureCopyright;
  private ColorLine[] mCategoriesString;
  private BitmapDrawable mPicture;

  private boolean mIsExpired;
  
  private final int mChannelID;
  
  private Rect mStartTimeBounds = new Rect();
  private int mBigRowCount;
  private int mSmallRowCount;
  private int mSuperSmallCount;
  private String mStartTimeString;
  
  public ProgramPanel(Context context, final long startTime, final long endTime, final String title, final int channelID) {
    super(context);
    
    mBigRowCount = 0;
    mSmallRowCount = 0;
    mSuperSmallCount = 0;
    mEndTime = endTime;
    mChannelID = channelID;
    setStartTime(startTime);
    setTitle(title);
  }
  
  private void setTitle(String title) {
    Object[] result = getBreakerText(title, getTextWidth() - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP, ProgramTableLayoutConstants.NOT_EXPIRED_TITLE_PAINT);
    
    mTitle = result[0].toString();
    mBigRowCount = (Integer)result[1];
  }
  
  private void setStartTime(long startTime) {
    mStartTime = new Date(startTime);
    
    mStartTimeString = ProgramTableLayoutConstants.TIME_FORMAT.format(mStartTime);
    
    ProgramTableLayoutConstants.NOT_EXPIRED_TITLE_PAINT.getTextBounds(mStartTimeString, 0, mStartTimeString.length(), mStartTimeBounds);
  }
  
  public void setInfoString(Spannable value) {
    if(value != null && value.toString().trim().length() > 0) {
      Object[] result = getBreakerText(value.toString(), getTextWidth() - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP, ProgramTableLayoutConstants.NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT);
      
      String all = result[0].toString();
      
      String[] lines = all.split("\n");
      
      mCategoriesString = new ColorLine[lines.length];
      
      HashMap<String, Integer> categoryColorMap = IOUtils.loadCategoryColorMap(getContext());
      
      for(int i = 0; i < lines.length; i++) {
        mCategoriesString[i] = new ColorLine();
        
        String[] lineParts = lines[i].split(",");
        
        for(int j = 0; j < lineParts.length-1; j++) {
          mCategoriesString[i].addEntry(new ColorEntry(categoryColorMap.get(lineParts[j].trim()), lineParts[j], true));
        }
        
        if(lineParts.length > 0) {
          mCategoriesString[i].addEntry(new ColorEntry(categoryColorMap.get(lineParts[lineParts.length-1].trim()), lineParts[lineParts.length-1], (i != lines.length-1)));
        }
      }      
      
      mSuperSmallCount += (Integer)result[1];
    }
  }
  
  /*
   * Breaks the given String into string with line breaks at needed positions.
   */
  private static Object[] getBreakerText(String temp, int width, Paint toCheck) {
    StringBuilder parts = new StringBuilder();
    
    temp = temp.trim().replace("\u00AD", "");
    
    int rowCount = 0;
    
    do {
      int length = toCheck.breakText(temp, true, width, null);
      float measured = toCheck.measureText(temp);
      
      if(length < temp.length() && measured >= width) {
        int bestBreak = temp.lastIndexOf("-", length-1);
        
        if(bestBreak == -1) {
          bestBreak = temp.lastIndexOf(" ", length-1);
        }
        
        if(bestBreak == -1) {
          bestBreak = temp.lastIndexOf(",", length-1);
        }
        
        if(bestBreak == -1) {
          bestBreak = temp.lastIndexOf("/", length-1);
        }
        
        if(bestBreak > 0) {
          parts.append(temp.substring(0, bestBreak+1).trim()).append("\n");
          temp = temp.substring(bestBreak+1).trim();
        }
        else {
          parts.append(temp.substring(0, length-1).trim()).append("\n");
          temp = temp.substring(length-1).trim();
        }
      }
      else {
        parts.append(temp);
        temp = "";
      }
      
      rowCount++;
    }while(!temp.trim().isEmpty());
    
    return new Object[] {parts.toString(),rowCount};
  }

  public void setGenre(String genre) {
    if(genre != null && !genre.trim().isEmpty()) {
      Object[] result = getBreakerText(genre.trim(), getTextWidth() - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP, ProgramTableLayoutConstants.NOT_EXPIRED_GENRE_EPISODE_PAINT);
      
      mGenre = result[0].toString();
      mSmallRowCount += (Integer)result[1];
    }
  }
  
  private int getTextWidth() {
    return ProgramTableLayoutConstants.COLUMN_WIDTH - ProgramTableLayoutConstants.PADDING_SIDE * 3;
  }
  
  public void setEpisode(String episode) {
    if(episode != null && !episode.trim().isEmpty()) {
      Object[] result = getBreakerText(episode.trim(), getTextWidth() - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP, ProgramTableLayoutConstants.NOT_EXPIRED_GENRE_EPISODE_PAINT);
      
      mEpisode = result[0].toString();
      mSmallRowCount += (Integer)result[1];
    }
  }
  
  public void setPicture(String copyright, BitmapDrawable picture) {
    if(copyright != null && !copyright.trim().isEmpty() && picture != null) {
      Object[] result = getBreakerText(copyright.trim(), ProgramTableLayoutConstants.COLUMN_WIDTH - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP, ProgramTableLayoutConstants.NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT);
      
      mPictureCopyright = result[0].toString();
      mSuperSmallCount += (Integer)result[1];
      
      mPicture = picture;
      
      float percent = mPicture.getBounds().width() / (float)(getTextWidth() - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP);
      
      if(percent > 1) {
        mPicture.setBounds(0, 0, (int)(mPicture.getBounds().width() / percent), (int)(mPicture.getBounds().height() / percent));
      }
      
      if(isExpired()) {
        if(SettingConstants.IS_DARK_THEME) {
          mPicture.setColorFilter(ContextCompat.getColor(getContext(), org.tvbrowser.tvbrowser.R.color.dark_gray), PorterDuff.Mode.DARKEN);
        }
        else {
          mPicture.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
        }
      }
    }
  }
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    
    int pictureHeight = 0;
    
    if(mPictureCopyright != null && !mPictureCopyright.trim().isEmpty() && mPicture != null) {
      pictureHeight = mPicture.getBounds().height();
    }
    
    height = Math.max(height, ProgramTableLayoutConstants.BIG_MAX_FONT_HEIGHT * mBigRowCount + ProgramTableLayoutConstants.SMALL_MAX_FONT_HEIGHT * mSmallRowCount + pictureHeight + mSuperSmallCount * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT);
    
    setMeasuredDimension(ProgramTableLayoutConstants.COLUMN_WIDTH, height);
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    
    canvas.translate(ProgramTableLayoutConstants.PADDING_SIDE, 0);
    
    TextPaint toUseForTimeAndTitle = ProgramTableLayoutConstants.NOT_EXPIRED_TITLE_PAINT;
    TextPaint toUseForGenreAndEpisode = ProgramTableLayoutConstants.NOT_EXPIRED_GENRE_EPISODE_PAINT;
    TextPaint toUseForPictureCopyright = ProgramTableLayoutConstants.NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT;
    
    if(isExpired()) {
      toUseForTimeAndTitle = ProgramTableLayoutConstants.EXPIRED_TITLE_PAINT;
      toUseForGenreAndEpisode = ProgramTableLayoutConstants.EXPIRED_GENRE_EPISODE_PAINT;
      toUseForPictureCopyright = ProgramTableLayoutConstants.EXPIRED_PICTURE_COPYRIGHT_PAINT;
    }
    
    // draw start time
    canvas.drawText(mStartTimeString, 0, ProgramTableLayoutConstants.BIG_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.BIG_FONT_DESCEND, toUseForTimeAndTitle);
    
    canvas.translate(mStartTimeBounds.width() + ProgramTableLayoutConstants.TIME_TITLE_GAP, 0);
    
    String[] lines = mTitle.split("\n");
    
    // draw title
    for(int i = 0; i < lines.length; i++) {
      canvas.drawText(lines[i], 0, (i+1) * ProgramTableLayoutConstants.BIG_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.BIG_FONT_DESCEND, toUseForTimeAndTitle);
    }
    
    canvas.translate(0, lines.length * ProgramTableLayoutConstants.BIG_MAX_FONT_HEIGHT);
    
    // draw picture copyright and picture
    if(mPictureCopyright != null && mPicture != null) {
      mPicture.draw(canvas);
      
      canvas.translate(0, mPicture.getBounds().height());
      
      lines = mPictureCopyright.split("\n");
      
      for(int i = 0; i < lines.length; i++) {
        canvas.drawText(lines[i], 0, (i+1) * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SUPER_SMALL_FONT_DESCEND, toUseForPictureCopyright);
      }
      
      canvas.translate(0, lines.length * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT);
    }
    
    // draw additional info
    if(mCategoriesString != null) {
      //lines = mInfoString.split("\n");
      
      final int oldColor = toUseForPictureCopyright.getColor();
      
      final String separator = ",";
      final float separatorWidth = toUseForPictureCopyright.measureText(separator);
      
      for(int i = 0; i < mCategoriesString.length; i++) {
        canvas.save();
        
        for(Iterator<ColorEntry> it = mCategoriesString[i].getEntries(); it.hasNext();) {
          ColorEntry entry = it.next();
          
          Integer color = entry.getColor();
          
          if(color != null && !isExpired()) {
            toUseForPictureCopyright.setColor(color);
          }
          else {
            toUseForPictureCopyright.setColor(oldColor);
          }
          
          canvas.drawText(entry.getText(), 0, (i+1) * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SUPER_SMALL_FONT_DESCEND, toUseForPictureCopyright);
          
          canvas.translate(entry.measure(toUseForPictureCopyright), 0);
          
          if(entry.needsSeparator()) {
            toUseForPictureCopyright.setColor(oldColor);
            
            canvas.drawText(separator, 0, (i+1) * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SUPER_SMALL_FONT_DESCEND, toUseForPictureCopyright);
            canvas.translate(separatorWidth, 0);
          }
        }
        
        canvas.restore();
        /*
        String first = lines[i].substring(0, lines[i].length()/2);
        String second = lines[i].substring(lines[i].length()/2);
        
        canvas.save();
        
        toUseForPictureCopyright.setColor(Color.BLUE);
        
        canvas.drawText(first, 0, (i+1) * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SUPER_SMALL_FONT_DESCEND, toUseForPictureCopyright);
        
        canvas.translate(toUseForPictureCopyright.measureText(first), 0);
        
        toUseForPictureCopyright.setColor(Color.RED);
        
        canvas.drawText(second, 0, (i+1) * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SUPER_SMALL_FONT_DESCEND, toUseForPictureCopyright);
        
        canvas.restore();*/
      }
      
      toUseForPictureCopyright.setColor(oldColor);
      
      canvas.translate(0, mCategoriesString.length * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT);
    }
    
    // draw genre
    if(mGenre != null) {
      lines = mGenre.split("\n");
      
      for(int i = 0; i < lines.length; i++) {
        canvas.drawText(lines[i], 0, (i+1) * ProgramTableLayoutConstants.SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SMALL_FONT_DESCEND, toUseForGenreAndEpisode);
      }
      
      canvas.translate(0, lines.length * ProgramTableLayoutConstants.SMALL_MAX_FONT_HEIGHT);
    }
    
    // draw episode title
    if(mEpisode != null) {
      lines = mEpisode.split("\n");
      
      for(int i = 0; i < lines.length; i++) {
        canvas.drawText(lines[i], 0, (i+1) * ProgramTableLayoutConstants.SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SMALL_FONT_DESCEND, toUseForGenreAndEpisode);
      }
    }
  }
  
  public void checkExpired(Handler handler) {
    if(!mIsExpired && isExpired()) {
      if(mPicture != null) {
        mPicture.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
      }
      
      handler.post(() -> {
        try {
          invalidate();
        }catch(Throwable ignored) {}
      });
    }
  }
  
  public boolean isOnAir() {
    return mStartTime.getTime() <= System.currentTimeMillis() && mEndTime > System.currentTimeMillis();
  }
  
  private boolean isExpired() {
    if(!mIsExpired) {
      mIsExpired = mEndTime < System.currentTimeMillis();
    }
    
    return mIsExpired;
  }
  
  public int getChannelID() {
    return mChannelID;
  }
  
  public int getEndHour(Calendar test) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(mEndTime);
    
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    
    if(test.get(Calendar.DAY_OF_YEAR) + 1 == cal.get(Calendar.DAY_OF_YEAR)) {
      hour += 24;
    }
    
    return hour;
  }
  
  public int getStartHour(Calendar test) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(mStartTime);
    
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    
    if(test.get(Calendar.DAY_OF_YEAR) + 1 == cal.get(Calendar.DAY_OF_YEAR)) {
      hour += 24;
    }
    
    return hour;
  }
  
  public void clear() {
    mEpisode = null;
    mGenre = null;
    mPicture = null;
    mPictureCopyright = null;
    mStartTimeBounds = null;
    mStartTimeString = null;
    mTitle = null;
  }
  
  @Override
  public String toString() {
    return mStartTime + " '" + mTitle + "' on " + mChannelID;
  }
  
  private static final class ColorEntry {
    private final Integer mColor;
    private final String mText;
    private final boolean mNeedsSeparator;
    
    ColorEntry(Integer color, String text, boolean needsSeparator) {
      mColor = color;
      mText = text;
      mNeedsSeparator = needsSeparator;
    }
    
    float measure(Paint paint) {
      return paint.measureText(mText);
    }
    
    String getText() {
      return mText;
    }
    
    Integer getColor() {
      return mColor;
    }
    
    boolean needsSeparator() {
      return mNeedsSeparator;
    }
  }
  
  private static final class ColorLine {
    private final ArrayList<ColorEntry> mEntryList;
    
    ColorLine() {
      mEntryList = new ArrayList<>();
    }
    
    Iterator<ColorEntry> getEntries() {
      return mEntryList.iterator();
    }
    
    void addEntry(ColorEntry entry) {
      mEntryList.add(entry);
    }
  }
}
