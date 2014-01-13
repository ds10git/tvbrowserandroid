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
package org.tvbrowser.settings;

import java.util.HashMap;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.tvbrowser.UiUtils;

import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.SparseArray;

public class SettingConstants {
  public static final int ACCEPTED_DAY_COUNT = 8;
  
  public static final String EPG_FREE_KEY = "EPG_FREE";
  public static final String[] LEVEL_NAMES = {"base","more00-16","more16-00","picture00-16","picture16-00"};
  public static final String CHANNEL_DOWNLOAD_COMPLETE = "org.tvbrowser.CHANNEL_DOWNLOAD_COMPLETE";
  public static final String MARKINGS_CHANGED = "org.tvbrowser.MARKINGS_CHANGED";
  public static final String FAVORITES_CHANGED = "org.tvbrowser.FAVORTES_CHANGED";
  public static final String DONT_WANT_TO_SEE_CHANGED = "org.tvbrowser.DONT_WANT_TO_SEE_CHANGED";
  public static final String DATA_UPDATE_DONE = "org.tvbrowser.DATA_UPDATE_DONE";
  public static final String CHANNEL_UPDATE_DONE = "org.tvbrowser.CHANNEL_UPDATE_DONE";
  public static final String REFRESH_VIEWS = "org.tvbrowser.REFRESH_VIEWS";
  public static final String UPDATE_TIME_BUTTONS = "org.tvbrowser.UPDATE_TIME_BUTTONS";
  public static final String REMINDER_INTENT = "org.tvbrowser.REMINDER_INTENT";
  public static final String SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT = "org.tvbrowser.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT";
  public static final String SCROLL_TO_TIME_INTENT = "org.tvbrowser.SCROLL_TO_TIME_INTENT";
  public static final String REMINDER_PROGRAM_ID_EXTRA = "REMINDER_PROGRAM_ID_EXTRA";
  public static final String CHANNEL_ID_EXTRA = "CHANNEL_ID_EXTRA";
  public static final String START_TIME_EXTRA = "START_TIME_EXTRA";
  public static final String DONT_WANT_TO_SEE_ADDED_EXTRA = "DONT_WANT_TO_SEE_ADDED_EXTRA";
  
  public static final String DIVIDER_DEFAULT = "10";
  
  public static boolean IS_DARK_THEME = false;
  
  public static final SparseArray<LayerDrawable> SMALL_LOGO_MAP = new SparseArray<LayerDrawable>();
  
  public static void updateLogoMap(Context context) {
    Cursor channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_LOGO}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, null);
    
    SMALL_LOGO_MAP.clear();
    
    if(channels.getCount() > 0 ) {
      int keyIndex = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int logoIndex = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
      
      while(channels.moveToNext()) {
        byte[] logoData = channels.getBlob(logoIndex);
        
        if(logoData != null && logoData.length > 0) {
          Bitmap logoBitmap = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
          
          BitmapDrawable logo1 = new BitmapDrawable(context.getResources(), logoBitmap);
          
          float scale = UiUtils.convertDpToPixel(15, context.getResources()) / (float)logoBitmap.getHeight();
          
          int width = (int)(logoBitmap.getWidth() * scale);
          int height = (int)(logoBitmap.getHeight() * scale);
          
          ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
          background.setBounds(0, 0, width + 2, height + 2);
          
          LayerDrawable logo = new LayerDrawable(new Drawable[] {background,logo1});
          logo.setBounds(0, 0, width + 2, height + 2);
          
          logo1.setBounds(2, 2, width, height);
          
          SMALL_LOGO_MAP.put(channels.getInt(keyIndex), logo);
        }
      }
    }
    
    channels.close();
  }
  
  
  public static int ORIENTATION;
  
  public static boolean UPDATING_FILTER = false;
  
  public static final String[] REMINDER_PROJECTION = new String[] {
    TvBrowserContentProvider.CHANNEL_KEY_NAME,
    TvBrowserContentProvider.CHANNEL_KEY_LOGO,
    TvBrowserContentProvider.DATA_KEY_STARTTIME,
    TvBrowserContentProvider.DATA_KEY_ENDTIME,
    TvBrowserContentProvider.DATA_KEY_TITLE,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE
    };
  
  public static final String MARKINGS_ID = "MARKINGS_ID";
  public static final String FAVORITE_LIST = "FAVORITE_LIST";
  public static final String MARK_VALUE = "marked";
  public static final String MARK_VALUE_FAVORITE = "favorite";
  public static final String MARK_VALUE_CALENDAR = "calendar";
  public static final String MARK_VALUE_SYNC_FAVORITE = "syncfav";
  public static final String MARK_VALUE_REMINDER = "reminder";

  
  /*AlarmManager alarmManager = (AlarmManager) getActivity().getApplicationContext().getSystemService(Context.ALARM_SERVICE);
  
  PendingIntent.get*/
    
  public static final String USER_NAME = "CAR";
  public static final String USER_PASSWORD = "BICYCLE";
  
  public static final String TERMS_ACCEPTED = "TERMS_ACCEPTED";
  public static final String EULA_ACCEPTED = "EULA_ACCEPTED";
  
  public static final String DEFAULT_RUNNING_PROGRAMS_LIST_LAYOUT = "1";
  
  public static final String CHANNEL_DOWNLOAD_SUCCESSFULLY = "CHANNEL_DOWNLOAD_SUCCESSFULLY";
  
  public static final IntentFilter RERESH_FILTER = new IntentFilter(REFRESH_VIEWS);
    
 // public static final HashMap<String, Integer> MARK_COLOR_MAP = new HashMap<String, Integer>();
  public static final HashMap<String, Integer> MARK_COLOR_KEY_MAP = new HashMap<String, Integer>();
  
  public static final HashMap<String, String> SHORT_CHANNEL_NAMES = new HashMap<String, String>();
  
  public static final int NO_CATEGORY = 0;
  public static final int TV_CATEGORY = 1;
  public static final int RADIO_CATEGORY = 1 << 1;
  public static final int CINEMA_CATEGORY = 1 << 2;
  public static final int DIGITAL_CATEGORY = 1 << 4;
  public static final int MUSIC_CATEGORY = 1 << 5;
  public static final int SPORT_CATEGORY = 1 << 6;
  public static final int NEWS_CATEGORY = 1 << 7;
  public static final int NICHE_CATEGORY = 1 << 8;
  public static final int PAY_TV_CATEGORY = 1 << 9;
  
  private static final int GRAY_LIGHT_VALUE = 155;
  private static final int GRAY_DARK_VALUE = 78;
  public static final int LOGO_BACKGROUND_COLOR = Color.WHITE;
  
  public static final int EXPIRED_LIGHT_COLOR = Color.rgb(GRAY_LIGHT_VALUE, GRAY_LIGHT_VALUE, GRAY_LIGHT_VALUE);
  public static final int EXPIRED_DARK_COLOR = Color.rgb(GRAY_DARK_VALUE, GRAY_DARK_VALUE, GRAY_DARK_VALUE);
  
  static {
    MARK_COLOR_KEY_MAP.put(MARK_VALUE, UiUtils.MARKED_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(MARK_VALUE_CALENDAR, UiUtils.MARKED_REMINDER_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(MARK_VALUE_REMINDER, UiUtils.MARKED_REMINDER_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(MARK_VALUE_FAVORITE, UiUtils.MARKED_FAVORITE_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(MARK_VALUE_SYNC_FAVORITE, UiUtils.MARKED_SYNC_COLOR_KEY);
    
    SHORT_CHANNEL_NAMES.put("NDR Niedersachsen", "NDR NDS");
    SHORT_CHANNEL_NAMES.put("NDR Mecklenburg-Vorpommern", "NDR MV");
    SHORT_CHANNEL_NAMES.put("NDR Hamburg", "NDR HH");
    SHORT_CHANNEL_NAMES.put("NDR Schleswig-Holstein", "NDR SH");
    SHORT_CHANNEL_NAMES.put("MDR Sachsen-Anhalt", "MDR ST");
    SHORT_CHANNEL_NAMES.put("MDR Sachsen", "MDR SN");
    SHORT_CHANNEL_NAMES.put("MDR Thüringen", "MDR TH");
    SHORT_CHANNEL_NAMES.put("RBB Berlin", "RBB BE");
    SHORT_CHANNEL_NAMES.put("RBB Brandenburg", "RBB BB");
    SHORT_CHANNEL_NAMES.put("Das Erste (ARD)", "Das Erste");
  }
}
