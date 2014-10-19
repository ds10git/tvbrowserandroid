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
package org.tvbrowser.tvbrowser;

import java.util.Comparator;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.text.TextUtils;

/**
 * A class with values of a ChannelFilter.
 * <p>
 * @author René Mach
 */
public class ChannelFilterValues {
  public static final Comparator<ChannelFilterValues> CHANNEL_FILTER_VALUES_COMPARATOR = new Comparator<ChannelFilterValues>() {
    @Override
    public int compare(ChannelFilterValues lhs, ChannelFilterValues rhs) {
      return lhs.toString().compareToIgnoreCase(rhs.toString());
    }
  };
  
  private String mId;
  private String mName;
  private String mWhereClause;
  
  public ChannelFilterValues(String id, String name, String whereClause) {
    mId = id;
    mName = name;
    mWhereClause = whereClause;
  }
  
  public ChannelFilterValues(String id, String values) {
    mId = id;
    
    int index = values.indexOf("##_##");
    
    mName = values.substring(0,index);
    
    String[] ids = values.substring(index+5).split(";");
    
    mWhereClause = " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IN (" + TextUtils.join(", ", ids) + ") ";
  }
  
  public String getId() {
    return mId;
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  public String getWhereClause() {
    return mWhereClause;
  }
}
