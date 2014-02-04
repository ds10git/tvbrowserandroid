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
package org.tvbrowser.tvbrowser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ProgramListViewBinderAndClickHandler implements SimpleCursorAdapter.ViewBinder{
  private Activity mActivity;
  private SharedPreferences mPref;
  private int mDefaultTextColor;
  
  public ProgramListViewBinderAndClickHandler(Activity act) {
    mActivity = act;
    mPref = PreferenceManager.getDefaultSharedPreferences(act);
    mDefaultTextColor = new TextView(mActivity).getTextColors().getDefaultColor();
  }

  @Override
  public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
    boolean showPicture = PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_LISTS, R.bool.show_pictures_in_lists_default);
    boolean showGenre = PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_LISTS, R.bool.show_genre_in_lists_default);
    boolean showEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_LISTS, R.bool.show_episode_in_lists_default);
    boolean showInfo = PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_LISTS, R.bool.show_info_in_lists_default);
    boolean showOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_LISTS, R.bool.show_sort_number_in_lists_default);
    boolean showEndTime = PrefUtils.getBooleanValue(R.string.PREF_PROGRAM_LISTS_SHOW_END_TIME, R.bool.pref_program_lists_show_end_time_default);
    
    String logoNamePref = PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_PROGRAM_LISTS, R.string.channel_logo_name_program_lists_default);
    
    boolean showChannelName = logoNamePref.equals("0") || logoNamePref.equals("2");
    boolean showChannelLogo = logoNamePref.equals("0") || logoNamePref.equals("1");
    
    long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
    
    if(view instanceof TextView) {
      if(endTime < System.currentTimeMillis()) {
        ((TextView) view).setTextColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, mActivity));
      }
      else {
        ((TextView) view).setTextColor(mDefaultTextColor);
      }
    }
    
    if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
      View until = ((ViewGroup)view.getParent()).findViewById(R.id.untilLabelPL);
      
      if(showEndTime) {
        TextView text = (TextView)view;
        text.setText(DateFormat.getTimeFormat(mActivity).format(new Date(endTime)));
        text.setVisibility(View.VISIBLE);
        until.setVisibility(View.VISIBLE);
      }
      else {
        view.setVisibility(View.GONE);
        until.setVisibility(View.GONE);
      }
      
      if(endTime < System.currentTimeMillis()) {
        ((TextView) until).setTextColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, mActivity));
      }
      else {
        ((TextView) until).setTextColor(mDefaultTextColor);
      }
      
      return true;
    } 
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_UNIX_DATE)) {
      UiUtils.formatDayView(mActivity, cursor, view, R.id.startDayLabelPL);
      
      TextView date = (TextView)((ViewGroup)view.getParent()).findViewById(R.id.startDayLabelPL);
      
      if(endTime < System.currentTimeMillis()) {
        date.setTextColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, mActivity));
      }
      else {
        date.setTextColor(mDefaultTextColor);
      }
      
      return true;
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) {
      TextView text = (TextView)view;
      
      String name = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
      String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
      String number = null;
      
      if(shortName != null) {
        name = shortName;
      }
      
      if(showOrderNumber) {
        number = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));
        
        if(number == null) {
          number = "0";
        }
        
        number += ".";
        
        name =  number + " " + name;
      }
      
      int logoIndex = cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      
      Drawable logo = null;
      
      ImageView image = (ImageView)((ViewGroup)view.getParent()).findViewById(R.id.program_list_channel_logo);
      
      if(showChannelLogo && logoIndex >= 0) {
        int key = cursor.getInt(logoIndex);
        
        if(showChannelName || showOrderNumber || mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && (mActivity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE) {
          logo = SettingConstants.SMALL_LOGO_MAP.get(key);
        }
        else {
          logo = SettingConstants.MEDIUM_LOGO_MAP.get(key);
        }
      }
      
      if(logo != null) {
        image.setImageDrawable(logo);
        image.setVisibility(View.VISIBLE);
        
        if(!showChannelName && !showOrderNumber) {
          text.setVisibility(View.GONE);
        }
        else {
          text.setVisibility(View.VISIBLE);
        }
      }
      else {
        image.setVisibility(View.GONE);
        text.setVisibility(View.VISIBLE);
      }
      if(showChannelName) {
        text.setText(name);
      }
      else if(showOrderNumber) {
        text.setText(number);
      }
       
      return true;
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
      long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
      
      java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(mActivity);
      String value = ((SimpleDateFormat)mTimeFormat).toLocalizedPattern();
      
      if((value.charAt(0) == 'H' && value.charAt(1) != 'H') || (value.charAt(0) == 'h' && value.charAt(1) != 'h')) {
        value = value.charAt(0) + value;
      }
      
      mTimeFormat = new SimpleDateFormat(value, Locale.getDefault());
      
      TextView text = (TextView)view;
      text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
      text.setText(mTimeFormat.format(new Date(date)));
       
      return true;
    }
   /* else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)) {
      TextView text = (TextView)view;
      
      if(end <= System.currentTimeMillis()) {
        int DEFAULT_TEXT_COLOR = new TextView(mActivity).getTextColors().getDefaultColor();
        
        text.setTextColor(DEFAULT_TEXT_COLOR);
      }
    }*/
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) {
      if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) || !showInfo) {
        view.setVisibility(View.GONE);
      }
      else {
        int info = cursor.getInt(columnIndex);
        
        if(info != 0) {
          view.setVisibility(View.VISIBLE);
          ((TextView)view).setText(IOUtils.getInfoString(info,view.getResources()));
        }
        else {
          view.setVisibility(View.GONE);
        }
      }
      
      return true;
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) {
      if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) || !showEpisode) {
        view.setVisibility(View.GONE);
      }
      else {
        view.setVisibility(View.VISIBLE);
      }
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) {
      if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) || !showGenre) {
        view.setVisibility(View.GONE);
      }
      else {
        view.setVisibility(View.VISIBLE);
      }
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)) {
      TextView text = (TextView)view;
      ImageView picture = (ImageView)((RelativeLayout)text.getParent()).findViewById(R.id.picture_pl);
      
      int pictureIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);
      
      if(pictureIndex >= 0 && !cursor.isNull(pictureIndex) && showPicture) {
        byte[] logoData = cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE));
        Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
        
        BitmapDrawable l = new BitmapDrawable(view.getResources(), logo);
        l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
        
        picture.setImageDrawable(l);
        
        text.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));
        text.setVisibility(View.VISIBLE);
        picture.setVisibility(View.VISIBLE);
      }
      else {
        view.setVisibility(View.GONE);
        picture.setVisibility(View.GONE);
      }
    }
    
    return false;
  }

  
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    long programID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    
    UiUtils.createContextMenu(mActivity, menu, programID);
  }
  
  
  public boolean onContextItemSelected(MenuItem item) {
    if(item.getMenuInfo() != null) {
      long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
      
      UiUtils.handleContextMenuSelection(mActivity, item, programID, null);
    }
    
    return false;
  }
  
  public void onListItemClick(ListView l, View v, int position, long id) {
    UiUtils.showProgramInfo(mActivity, id);
  }
}
