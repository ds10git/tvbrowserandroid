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

import android.app.Service;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable class with informations about a TV-Browser plugin receive target.
 * 
 * @author René Mach
 */
public class ReceiveTarget implements Parcelable {
  private static final int VERSION = 1;
  
  private String mServiceClass;
  private int mId;
  private String mTitle;
  
  public static final Parcelable.Creator<ReceiveTarget> CREATOR = new Parcelable.Creator<ReceiveTarget>() {
    @Override
    public ReceiveTarget createFromParcel(Parcel source) {
      return new ReceiveTarget(source);
    }

    @Override
    public ReceiveTarget[] newArray(int size) {
      return new ReceiveTarget[size];
    }
  };
  
  /**
   * Creates an instance of this class.
   * 
   * @param plugin The service that provides this ReceiveTarget.
   * @param id The id of this ReceiveTarget.
   * @param title The title of the context menu entry.
   */
  public ReceiveTarget(Service plugin, String title, int id) {
    mServiceClass = plugin.getClass().getCanonicalName();
    mTitle = title;
    mId = id;
  }
  
  /**
   * Creates an instance of this class from the given Parcel.
   * <p>
   * @param source The Parcel to read the values of this Program.
   */
  public ReceiveTarget(Parcel source) {
    readFromParcel(source);
  }
  
  /**
   * Gets the title of this ReceiveTarget.
   * <p>
   * @return The title for the context menu entry.
   */
  public String getTitle() {
    return mTitle;
  }
  
  /**
   * Gets the canonical name of the service class.
   * <p> 
   * @return The canonical name of the service class.
   */
  public String getServiceClassCanonicalName() {
    return mServiceClass;
  }
  
  /**
   * Gets the id of this ReceiveTarget.
   * <p>
   * @return The id of this ReceiveTarget.
   */
  public int getId() {
    return mId;
  }
  
  /**
   * Checks if this ReceiveTarget is the ReceiveTarget with the given parameter.
   * <p>
   * @param service The service to check this ReceiveTarget for.
   * @param id The id of the ReceiveTarget to check.
   * @return <code>true</code> if this is the ReceiveTarget with the given parameter,
   *         <code>false</code> otherweise.
   */
  public boolean isReceiveTargetOfServiceWithId(Service service, int id) {
    return mId == id && service.getClass().getCanonicalName().equals(mServiceClass);
  }
  
  @Override
  public int describeContents() {
    return 0;
  }

  private void readFromParcel(Parcel source) {
    source.readInt(); // read version
    mServiceClass = source.readString();
    mId = source.readInt();
    mTitle = source.readString();
  }
  
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(VERSION);
    dest.writeString(mServiceClass);
    dest.writeInt(mId);
    dest.writeString(mTitle);
  }
}
