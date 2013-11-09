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

import org.tvbrowser.tvbrowser.R;

import android.content.IntentFilter;

public class SettingConstants {
  public static final String EPG_FREE_KEY = "EPG_FREE";
  public static final String[] LEVEL_NAMES = {"base","more00-16","more16-00","picture00-16","picture16-00"};
  public static final String CHANNEL_DOWNLOAD_COMPLETE = "org.tvbrowser.CHANNEL_DOWNLOAD_COMPLETE";
  public static final String MARKINGS_CHANGED = "org.tvbrowser.MARKINGS_CHANGED";
  public static final String FAVORITES_CHANGED = "org.tvbrowser.FAVORTES_CHANGED";
  public static final String DATA_UPDATE_DONE = "org.tvbrowser.DATA_UPDATE_DONE";
  public static final String CHANNEL_UPDATE_DONE = "org.tvbrowser.CHANNEL_UPDATE_DONE";
  public static final String REFRESH_VIEWS = "org.tvbrowser.REFRESH_VIEWS";
  public static final String MARKINGS_ID = "MARKINGS_ID";
  public static final String FAVORITE_LIST = "FAVORITE_LIST";
  public static final String MARK_VALUE = "marked";
  public static final String MARK_VALUE_FAVORITE = "favorite";
  public static final String MARK_VALUE_CALENDAR = "calendar";
  public static final String MARK_VALUE_SYNC_FAVORITE = "syncfav";
    
  public static final String USER_NAME = "CAR";
  public static final String USER_PASSWORD = "BICYCLE";
  
  public static final String TERMS_ACCEPTED = "TERMS_ACCEPTED";
  public static final String EULA_ACCEPTED = "EULA_ACCEPTED";
  
  public static final IntentFilter RERESH_FILTER = new IntentFilter(REFRESH_VIEWS);
    
  public static final HashMap<String, Integer> MARK_COLOR_MAP = new HashMap<String, Integer>();
  
  public static final HashMap<String, String> SHORT_CHANNEL_NAMES = new HashMap<String, String>();
  
  static {
    MARK_COLOR_MAP.put(MARK_VALUE, R.color.mark_color);
    MARK_COLOR_MAP.put(MARK_VALUE_CALENDAR, R.color.mark_color_calendar);
    MARK_COLOR_MAP.put(MARK_VALUE_FAVORITE, R.color.mark_color_favorite);
    MARK_COLOR_MAP.put(MARK_VALUE_SYNC_FAVORITE, R.color.mark_color_sync_favorite);
    
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
