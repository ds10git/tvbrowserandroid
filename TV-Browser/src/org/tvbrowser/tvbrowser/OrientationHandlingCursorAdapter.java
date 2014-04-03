/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
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

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;

/**
 * A cursor adapter that handles orientation changes.
 * <p>
 * @author René Mach
 */
public class OrientationHandlingCursorAdapter extends SimpleCursorAdapter {
  private View.OnClickListener mOnClickListener;
  private View.OnClickListener mChannelSwitchListener;
  private View.OnCreateContextMenuListener mContextMenuListener;
  private AdapterView.AdapterContextMenuInfo mContextMenuInfo;
  
  public OrientationHandlingCursorAdapter(final Context context, int layout, Cursor c, String[] from, int[] to, int flags, boolean handleClicks) {
    super(context, layout, c, from, to, flags);
    
    if(handleClicks) {
      mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Long tag = (Long)v.getTag();
          
          if(tag != null) {
            UiUtils.showProgramInfo(context, tag.longValue());
          }
        }
      };
      
      mChannelSwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          ChannelProgInfo tag = (ChannelProgInfo)v.getTag();
          boolean handle = PrefUtils.getBooleanValue(R.string.PREF_PROGRAM_LISTS_CLICK_TO_CHANNEL_TO_LIST, R.bool.pref_program_lists_click_to_channel_to_list_default);
          
          if(handle && tag != null) {
            Intent showChannel = new Intent(SettingConstants.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT);
            showChannel.putExtra(SettingConstants.CHANNEL_ID_EXTRA,tag.mID);         
            showChannel.putExtra(SettingConstants.START_TIME_EXTRA, tag.mStartTime);
            
            LocalBroadcastManager.getInstance(context).sendBroadcastSync(showChannel);
          }
        }
      };
      
      final MenuItem.OnMenuItemClickListener menuClick = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
          if(mContextMenuInfo != null) {
            long programID = mContextMenuInfo.id;
            mContextMenuInfo = null;
            
            return UiUtils.handleContextMenuSelection(context, item, programID, null);
          }
          
          return true;
        }
      };
      
      mContextMenuListener = new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
          long id = ((Long)v.getTag()).longValue();
          mContextMenuInfo = new AdapterView.AdapterContextMenuInfo(v, -1, id);
          
          UiUtils.createContextMenu(context, menu, id);
          
          for(int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setOnMenuItemClickListener(menuClick);
          }
        }
      };
    }
  }

  private static final class ChannelProgInfo {
    public int mID;
    public long mStartTime;
  }
  
  private static final class ProgTag {
    public int mOrientation;
    public float mTextScale;
  }
  
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    float textScale = Float.valueOf(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_TEXT_SCALE, R.string.pref_program_lists_text_scale_default));
    
    if(convertView != null && ((((ProgTag)convertView.getTag()).mOrientation != SettingConstants.ORIENTATION) || ((ProgTag)convertView.getTag()).mTextScale != textScale)) {
      convertView = null;
    }
    
    boolean scale = convertView == null;
    
    View view = super.getView(position, convertView, parent);
    
    if(scale) {
      UiUtils.scaleTextViews(view, textScale);
      
      ProgTag tag = new ProgTag();
      
      tag.mOrientation = SettingConstants.ORIENTATION;
      tag.mTextScale = textScale;
      
      view.setTag(tag);
    }
    
    if(mOnClickListener != null) {
      View listEntry = view.findViewById(R.id.programs_list_row);
      
      if(listEntry.getTag() == null) {
        listEntry.setOnClickListener(mOnClickListener);
        listEntry.setOnCreateContextMenuListener(mContextMenuListener);
      }
      
      listEntry.setTag(getItemId(position));
      
      View channelEntry = view.findViewById(R.id.program_list_channel_info);
      
      ChannelProgInfo info = (ChannelProgInfo)channelEntry.getTag();
      
      if(info == null) {
        info = new ChannelProgInfo();
        channelEntry.setOnClickListener(mChannelSwitchListener);
        channelEntry.setTag(info);
      }
      
      Cursor c = getCursor();
      
      info.mID = c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
      info.mStartTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
    }
    return view;
  }
}
