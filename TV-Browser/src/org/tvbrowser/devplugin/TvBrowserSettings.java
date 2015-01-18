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
 * A parcelable class with informations about TV-Browser.
 * 
 * @author René Mach
 */
public class TvBrowserSettings implements Parcelable {
  private static final int VERSION = 2;
  
  private boolean mUsesDarkTheme;
  private String mTvbVersion;
  private int mTvbVersionCode;
  private long mFirstKnownProgramId;
  private long mLastKnownProgramId;
  private long mLastKnownDataDate;
  
  public static final Parcelable.Creator<TvBrowserSettings> CREATOR = new Parcelable.Creator<TvBrowserSettings>() {
    @Override
    public TvBrowserSettings createFromParcel(Parcel source) {
      return new TvBrowserSettings(source);
    }

    @Override
    public TvBrowserSettings[] newArray(int size) {
      return new TvBrowserSettings[size];
    }
  };
  
  /**
   * Creates an instance of this class from the given Parcel.
   * <p>
   * @param source The Parcel to read the values of this TvBrowserSettings.
   */
  private TvBrowserSettings(Parcel source) {
    readFromParcel(source);
  }
  
  /**
   * Creates an instance of this class.
   * <p>
   * @param usesDarkTheme <code>true</code> if TV-Browser uses the dark theme, <code>false</code> if not.
   * @param tvbVersion The version of the TV-Browser.
   */
  public TvBrowserSettings(boolean usesDarkTheme, String tvbVersion) {
    this(usesDarkTheme, tvbVersion, -1, -2, -2, 0);
  }
    
  /**
   * Creates an instance of this class.
   * <p>
   * @param usesDarkTheme <code>true</code> if TV-Browser uses the dark theme, <code>false</code> if not.
   * @param tvbVersion The version of the TV-Browser.
   * @param tvbVersionCode The version code of the TV-Browser.
   * @param firstKnownProgramId The first known id of the data.
   * @param lastKnownProgramId The last known id of the data.
   * @param lastKnownDataDate The last known date of the data.
   */
  public TvBrowserSettings(boolean usesDarkTheme, String tvbVersion, int tvbVersionCode, long firstKnownProgramId, long lastKnownProgramId, long lastKnownDataDate) {
    mUsesDarkTheme = usesDarkTheme;
    mTvbVersion = tvbVersion;
    mTvbVersionCode = tvbVersionCode;
    mFirstKnownProgramId = firstKnownProgramId;
    mLastKnownProgramId = lastKnownProgramId;
    mLastKnownDataDate = lastKnownDataDate;
  }
  
  /**
   * Gets the version of TV-Browser.
   * <p>
   * @return The version of TV-Browser.
   */
  public String getTvbVersion() {
    return mTvbVersion;
  }
  
  /**
   * Gets the version code of TV-Browser
   * <p>
   * @return The version code of TV-Browser.
   * @since 0.5.7.2
   */
  public int getTvbVersionCode() {
    return mTvbVersionCode;
  }
  
  /**
   * Gets the first known program id of the data.
   * If <code>-1</code> is returned no data is available,
   * if <code>-2</code> is returned the state of the data is unknown.
   * <p>
   * @return The the first known id of the data.
   * @since 0.5.7.2
   */
  public long getFirstKnownProgramId() {
    return mFirstKnownProgramId;
  }
  
  /**
   * Gets the last known program id of the data.
   * If <code>-1</code> is returned no data is available,
   * if <code>-2</code> is returned the state of the data is unknown.
   * <p>
   * @return The the last known id of the data.
   * @since 0.5.7.2
   */
  public long getLastKnownProgramId() {
    return mLastKnownProgramId;
  }
  
  /**
   * Gets the last known date of the data or <code>0</code> if no data is available.
   * <p>
   * @return The the last known date of the data.
   * @since 0.5.7.2
   */
  public long getLastKnownDataDate() {
    return mLastKnownDataDate;
  }
  
  /**
   * Gets if TV-Browser uses the dark theme.
   * <p>
   * @return <code>true</code> if TV-Browser uses the dark theme, <code>false</code> if not.
   */
  public boolean isUsingDarkTheme() {
    return mUsesDarkTheme;
  }
  
  @Override
  public int describeContents() {
    return 0;
  }
  
  private void readFromParcel(Parcel source) {
    int version = source.readInt(); // read version
    mUsesDarkTheme = source.readByte() == 1;
    mTvbVersion = source.readString();
    
    if(version >= 2) {
      mTvbVersionCode = source.readInt();
      mFirstKnownProgramId = source.readLong();
      mLastKnownProgramId = source.readLong();
      mLastKnownDataDate = source.readLong();
    }
    else {
      mTvbVersionCode = -1;
      mFirstKnownProgramId = -2;
      mLastKnownProgramId = -2;
      mLastKnownDataDate = 0;
    }
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(VERSION);
    dest.writeByte((byte)(mUsesDarkTheme ? 1 : 0));
    dest.writeString(mTvbVersion);
    dest.writeInt(mTvbVersionCode);
    dest.writeLong(mFirstKnownProgramId);
    dest.writeLong(mLastKnownProgramId);
    dest.writeLong(mLastKnownDataDate);
  }
}
