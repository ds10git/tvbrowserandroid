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
 * A parcelable class with informations about a context menu entry of a TV-Browser plugin.
 * 
 * @author René Mach
 */
public final class PluginMenu implements Parcelable {
  private static final int VERSION = 1;
  
  private int mId;
  private String mTitle;
  
  public static final Parcelable.Creator<PluginMenu> CREATOR = new Parcelable.Creator<PluginMenu>() {
    @Override
    public PluginMenu createFromParcel(Parcel source) {
      return new PluginMenu(source);
    }

    @Override
    public PluginMenu[] newArray(int size) {
      return new PluginMenu[size];
    }
  };
  
  /**
   * Creates an instance of this class.
   * 
   * @param id The id of this PluginMenu.
   * @param title The title of the context menu entry.
   */
  public PluginMenu(int id, String title) {
    mId = id;
    mTitle = title;
  }
  
  /**
   * Creates an instance of this class from the given Parcel.
   * <p>
   * @param source The Parcel to read the values of this PluginMenu.
   */
  public PluginMenu(Parcel source) {
    readFromParcel(source);
  }
  
  @Override
  public int describeContents() {
    return 0;
  }

  /**
   * Gets the id of this PluginMenu.
   * <p>
   * @return The id of this PluginMenu.
   */
  public int getId() {
    return mId;
  }
  
  /**
   * Gets the title for the context menu entry of this PluginMenu.
   * <p>
   * @return The title for the context menu entry.
   */
  public String getTitle() {
    return mTitle;
  }
  
  /**
   * Gets the interface version of this PluginMenu.
   * <o>
   * @return The interface version of this PluginMenu.
   */
  public int getInterfaceVersion() {
    return VERSION;
  }
  
  private void readFromParcel(Parcel source) {
    source.readInt(); // read version
    mId = source.readInt();
    mTitle = source.readString();
  }
  
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(VERSION);
    dest.writeInt(mId);
    dest.writeString(mTitle);
  }
}
