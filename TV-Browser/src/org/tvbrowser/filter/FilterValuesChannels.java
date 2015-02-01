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
package org.tvbrowser.filter;

import java.util.Arrays;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.tvbrowser.WhereClause;
import org.tvbrowser.utils.UiUtils;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;

/**
 * A class with values of a ChannelFilter.
 * <p>
 * @author René Mach
 */
public class FilterValuesChannels extends FilterValues implements ChannelFilter {
  private int[] mChannelIds;
  
  public FilterValuesChannels() {
    this("",new int[0]);
  }
  
  public FilterValuesChannels(String name, int[] channelIds) {
    super(name);
    mChannelIds = channelIds;
  }
  
  protected FilterValuesChannels(String name, String values) {
    super(name);
    
    String[] ids = values.split(";");
    
    mChannelIds = new int[ids.length];
    
    for(int i = 0; i < mChannelIds.length; i++) {
      mChannelIds[i] = Integer.parseInt(ids[i]);
    }
  }
      
  public WhereClause getWhereClause(Context context) {
    StringBuilder where = new StringBuilder();
    
    if(mChannelIds.length > 0) {
      where.append(" AND ").append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID).append(" IN ( ");
      
      for(int i = 0; i < mChannelIds.length-1; i++) {
        where.append(mChannelIds[i]).append(", ");
      }
      
      where.append(mChannelIds[mChannelIds.length-1]).append(" ) ");
    }
    
    return new WhereClause(where.toString(),null);
  }

  @Override
  protected String getSaveString() {
    StringBuilder save = new StringBuilder();
    
    for(int i = 0; i < mChannelIds.length-1; i++) {
      save.append(mChannelIds[i]).append(";");
    }
    
    if(mChannelIds.length > 0) {
      save.append(mChannelIds[mChannelIds.length-1]);
    }
    
    return save.toString();
  }
  
  private Runnable mCallback;
  
  @Override
  public void edit(Context context, Runnable callback, ViewGroup parent) {
    mCallback = callback;
    
    UiUtils.showChannelFilterSelection(context, this, parent);
  }

  @Override
  public int[] getFilteredChannelIds() {
    return mChannelIds;
  }

  @Override
  public void setFilterValues(String name, int[] filteredChannelIds) {
    if(name != null && filteredChannelIds != null) {
      mName = name;
      mChannelIds = filteredChannelIds;
      
      if(mCallback != null) {
        mCallback.run();
      }
      
      mCallback = null;
    }
  }
}
