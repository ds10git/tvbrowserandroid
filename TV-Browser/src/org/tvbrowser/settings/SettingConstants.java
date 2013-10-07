package org.tvbrowser.settings;

import java.util.HashMap;

import org.tvbrowser.tvbrowser.R;

public class SettingConstants {
  public static final String EPG_FREE_KEY = "EPG_FREE";
  public static final String[] LEVEL_NAMES = {"base","more00-16","more16-00","picture00-16","picture16-00"}; 
  public static final int[] DOWNLOAD_DAYS = {0,1,2,3,7,14};
  public static final String CHANNEL_DOWNLOAD_COMPLETE = "org.tvbrowser.CHANNEL_DOWNLOAD_COMPLETE";
  public static final String MARKINGS_CHANGED = "org.tvbrowser.MARKINGS_CHANGED";
  public static final String FAVORITES_CHANGED = "org.tvbrowser.FAVORTES_CHANGED";
  public static final String DATA_UPDATE_DONE = "org.tvbrowser.DATA_UPDATE_DONE";
  public static final String MARKINGS_ID = "MARKINGS_ID";
  public static final String FAVORITE_LIST = "FAVORITE_LIST";
  public static final String MARK_VALUE = "marked";
  public static final String MARK_VALUE_FAVORITE = "favorite";
  public static final String MARK_VALUE_CALENDAR = "calendar";
  public static final String MARK_VALUE_SYNC_FAVORITE = "syncfav";
  
  public static final String USER_NAME = "CAR";
  public static final String USER_PASSWORD = "BICYCLE";
  
  public static final String SPONSORING_DATE = "SPONSORING_DATE";
  
  public static final String TERMS_ACCEPTED = "TERMS_ACCEPTED";
  
  public static final int DEFAULT_DAY_START = 0;
  public static final int DEFAULT_DAY_END = 24;
  
  public static final HashMap<String, Integer> MARK_COLOR_MAP = new HashMap<String, Integer>();
  
  static {
    MARK_COLOR_MAP.put(MARK_VALUE, R.color.mark_color);
    MARK_COLOR_MAP.put(MARK_VALUE_CALENDAR, R.color.mark_color_calendar);
    MARK_COLOR_MAP.put(MARK_VALUE_FAVORITE, R.color.mark_color_favorite);
    MARK_COLOR_MAP.put(MARK_VALUE_SYNC_FAVORITE, R.color.mark_color_sync_favorite);
  }
}
