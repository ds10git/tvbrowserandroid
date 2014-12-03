/*
 * Plugin Interface for TV-Browser for Android
 * Copyright (c) 2014 René Mach (rene@tvbrowser.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
  
  /**
   * Creates an instance of this class from the given Parcel.
   * <p>
   * @param source The Parcel to read the values of this Program.
   */
  public Program(Parcel source) {
    readFromParcel(source);
  }
  
  /**
   * Creates an instance of this class.
   * <p>
   * @param id The unique id of this TV-Browser program.
   * @param startTime The start time of this TV-Browser Program in milliseconds since 1970 in UTC timezone.
   * @param endTime The end time of this TV-Browser Program in milliseconds since 1970 in UTC timezone.
   * @param title The title of this TV-Browser Program.
   * @param shortDescription The short description of this TV-Browser Program.
   * @param description The full description of this TV-Browser Program.
   * @param episodeTitle The episode title of this TV-Browser Program.
   * @param channel The {@link Channel} of this TV-Browser Program.
   */
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
  
  /**
   * Gets the unique id of this Program.
   * <p>
   * @return The unique id of this Program.
   */
  public long getId() {
    return mId;
  }
  
  /**
   * Gets the start time of this Program in milliseconds since 1970 in UTC timezone.
   * <p>
   * @return The start time of this Program in milliseconds since 1970 in UTC timezone.
   */
  public long getStartTimeInUTC() {
    return mStartTime;
  }
  
  /**
   * Gets the end time of this Program in milliseconds since 1970 in UTC timezone.
   * <p>
   * @return The end time of this Program in milliseconds since 1970 in UTC timezone.
   */
  public long getEndTimeInUTC() {
    return mEndTime;
  }
  
  /**
   * Gets the short description for this program.
   * <p>
   * @return The short description for this program or <code>null</code> if it has no short description.
   */
  public String getShortDescription() {
    return mShortDescription;
  }
  
  /**
   * Gets the full description for this program.
   * <p>
   * @return The full description for this program or <code>null</code> if it has no full description.
   */
  public String getDescription() {
    return mDescription;
  }
  
  /**
   * Gets the title for this Program.
   * <p>
   * @return The title for this Program. 
   */
  public String getTitle() {
    return mTitle;
  }
  
  /**
   * Gets the episode title for this Program.
   * <p>
   * @return The episode title for this Program or <code>null</code> if it has no episode title.
   */
  public String getEpisodeTitle() {
    return mEpisodeTitle;
  }
  
  /**
   * Gets the Channel for this program.
   * <p>
   * @return The {@link Channel} of this program.
   */
  public Channel getChannel() {
    return mChannel;
  }
  
  /**
   * Gets the interface version of this Program.
   * <o>
   * @return The interface version of this Program.
   */
  public int getInterfaceVersion() {
    return VERSION;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  private void readFromParcel(Parcel source) {
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
