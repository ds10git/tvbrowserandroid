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
 * A parcelable class with information about a TV-Browser channel.
 * 
 * @author René Mach
 */
public final class Channel implements Parcelable {
  private static final int VERSION = 1;
  
  private int mId;
  private String mChannelName;
  private byte[] mChannelIcon;
  
  public static final Creator<Channel> CREATOR = new Creator<Channel>() {
    @Override
    public Channel createFromParcel(Parcel source) {
      return new Channel(source);
    }

    @Override
    public Channel[] newArray(int size) {
      return new Channel[size];
    }
  };
  
  /**
   * Creates an instance of this class from the given Parcel.
   * <p>
   * @param source The Parcel to read the values of this Channel.
   */
  public Channel(Parcel source) {
    readFromParcel(source);
  }
  
  /**
   * Creates an instance of this class.
   * <p>
   * @param id The unique id of the the TV-Browser channel.
   * @param channelName The name of the TV-Browser channel.
   * @param channelIcon The data of the icon of the TV-Browser channel.
   */
  public Channel(int id, String channelName, byte[] channelIcon) {
    mId = id;
    mChannelName = channelName;
    mChannelIcon = channelIcon;
  }
  
  /**
   * Gets the unique id of this Channel.
   * <p>
   * @return The unique id of this Channel.
   */
  public int getChannelId() {
    return mId;
  }
  
  /**
   * Gets the name of this Channel.
   * <p>
   * @return The name of this Channel.
   */
  public String getChannelName() {
    return mChannelName;
  }
  
  /**
   * Gets the data of the icon of this Channel.
   * <p>
   * @return A byte array with the data of the icon of this channel or <code>null</code>.
   */
  public byte[] getIcon() {
    return mChannelIcon;
  }
  
  /**
   * Gets the interface version of this Channel.
   * <o>
   * @return The interface version of this Channel.
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
    mId = source.readInt();
    mChannelName = source.readString();
    
    int iconSize = source.readInt();
    
    if(iconSize > 0) {
      mChannelIcon = new byte[iconSize];
      source.readByteArray(mChannelIcon);
    }
    else {
      mChannelIcon = null;
    }
  }
  
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(VERSION);
    dest.writeInt(mId);
    dest.writeString(mChannelName);
    dest.writeInt((mChannelIcon != null ? mChannelIcon.length : 0));
    
    if(mChannelIcon != null) {
      dest.writeByteArray(mChannelIcon);
    }
  }
}
