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

import java.util.Calendar;
import java.util.Date;

import org.tvbrowser.settings.SettingConstants;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;

public class ProgramPanel extends View {
  private Date mStartTime;
  private long mEndTime;
  private String mTitle;
  private String mEpisode;
  private String mGenre;
  private String mPictureCopyright;
  private String mInfoString;
  private BitmapDrawable mPicture;

  private boolean mIsExpired;
  
  private int mChannelID;
  
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
  
  public void setInfoString(String value) {
    if(value != null) {
      Object[] result = getBreakerText(value, getTextWidth() - mStartTimeBounds.width() - ProgramTableLayoutConstants.TIME_TITLE_GAP, ProgramTableLayoutConstants.NOT_EXPIRED_PICTURE_COPYRIGHT_PAINT);
      
      mInfoString = result[0].toString();
      mSuperSmallCount += (Integer)result[1];
    }
  }
  
  /*
   * Breaks the given String into string with line breaks at needed positions.
   */
  private static final Object[] getBreakerText(String temp, int width, Paint toCheck) {
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
  
  public int getTextWidth() {
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
          mPicture.setColorFilter(getResources().getColor(org.tvbrowser.tvbrowser.R.color.dark_gray), PorterDuff.Mode.DARKEN);
        }
        else {
          mPicture.setColorFilter(getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
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
    
    // draw additonal infos
    if(mInfoString != null) {
      lines = mInfoString.split("\n");
      
      for(int i = 0; i < lines.length; i++) {
        canvas.drawText(lines[i], 0, (i+1) * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT - ProgramTableLayoutConstants.SUPER_SMALL_FONT_DESCEND, toUseForPictureCopyright);
      }
      
      canvas.translate(0, lines.length * ProgramTableLayoutConstants.SUPER_SMALL_MAX_FONT_HEIGHT);
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
        mPicture.setColorFilter(getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
      }
      
      handler.post(new Runnable() {
        @Override
        public void run() {
          try {
            invalidate();
          }catch(Throwable t) {Log.d("info5", "", t);};
        }
      });  
    }
  }
  
  public boolean isOnAir() {
    return mStartTime.getTime() <= System.currentTimeMillis() && mEndTime > System.currentTimeMillis();
  }
  
  public boolean isExpired() {
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
}
