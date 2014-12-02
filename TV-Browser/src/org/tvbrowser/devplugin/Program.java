/*
 * TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.devplugin;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable class with informations about a TV-Browser program.
 * 
 * @author René Mach
 */
public final class Program implements Parcelable {
  private static final int VERSION = 1;
  
  private long mId;
  private long mStartTime;
  private long mEndTime;
  private String mTitle;
  private String mShortDescription;
  private String mDescription;
  private String mEpisodeTitle;
  private Channel mChannel;
  
  public static final Parcelable.Creator<Program> CREATOR = new Parcelable.Creator<Program>() {

    @Override
    public Program createFromParcel(Parcel source) {
      return new Program(source);
    }

    @Override
    public Program[] newArray(int size) {
      return new Program[size];
    }
  };
  public Program(Parcel source) {
    readFromParcel(source);
  }
  
  public Program(long id, long startTime, long endTime, String title, String shortDescription, String description, String episodeTitle, Channel channel) {
    mId = id;
    mStartTime = startTime;
    mEndTime = endTime;
    mTitle = title;
    mShortDescription = shortDescription;
    mDescription = description;
    mEpisodeTitle = episodeTitle;
    mChannel = channel;
  }
  
  public long getId() {
    return mId;
  }
  
  public long getStartTimeInUTC() {
    return mStartTime;
  }
  
  public long getEndTimeInUTC() {
    return mEndTime;
  }
  
  public String getShortDescription() {
    return mShortDescription;
  }
  
  public String getDescription() {
    return mDescription;
  }
  
  public String getTitle() {
    return mTitle;
  }
  
  public String getEpisodeTitle() {
    return mEpisodeTitle;
  }
  
  public int getVersion() {
    return VERSION;
  }
  
  public Channel getChannel() {
    return mChannel;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public void readFromParcel(Parcel source) {
    source.readInt(); // read version
    mId = source.readLong();
    mStartTime = source.readLong();
    mEndTime = source.readLong();
    mTitle = source.readString();
    mShortDescription = (String)source.readValue(String.class.getClassLoader());
    mDescription = (String)source.readValue(String.class.getClassLoader());
    mEpisodeTitle = (String)source.readValue(String.class.getClassLoader());
    mChannel = new Channel(source);
  }
  
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(VERSION);
    dest.writeLong(mId);
    dest.writeLong(mStartTime);
    dest.writeLong(mEndTime);
    dest.writeString(mTitle);
    dest.writeValue(mShortDescription);
    dest.writeValue(mDescription);
    dest.writeValue(mEpisodeTitle);
    mChannel.writeToParcel(dest, flags);
  }
}
