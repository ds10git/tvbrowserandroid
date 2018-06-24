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

import org.tvbrowser.App;
import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SparseArrayCompat;

public final class SettingConstants {

  SettingConstants() {}

  public static final int ACCEPTED_DAY_COUNT = 8;

  public static final String LOG_FILE_NAME_DATA_UPDATE = "data-update-log.txt";
  public static final String LOG_FILE_NAME_REMINDER = "reminder-log.txt";
  public static final String LOG_FILE_NAME_PLUGINS = "plugin-log.txt";

  private static final String REMINDER_PAUSE_KEY = "REMINDER_PAUSE_KEY";

  public static final String ALL_FILTER_ID = "filter.allFilter";
  
  public static final String EPG_FREE_KEY = "EPG_FREE";
  
  public static final String EPG_DONATE_KEY = "EPG_DONATE_KEY";
  public static final String EPG_DONATE_GROUP_KEY = "epgdonategroup";
  public static final String EPG_DONATE_DEFAULT_URL = "http://epgdonatedata.natsu-no-yuki.de/";
  
  public static final String URL_SYNC_BASE = "https://www.tvbrowser-app.de/";
  
  public static final String EPG_DONATE_DONATION_INFO_PERCENT_KEY = "CURRENT_DONATION_PERCENT";
  public static final String EPG_DONATE_DONATION_INFO_AMOUNT_KEY_PREFIX = "CURRENT_DONATION_AMOUNT_";
  
  public static final String[] EPG_FREE_LEVEL_NAMES = {"base","more00-16","more16-00","picture00-16","picture16-00"};
  public static final String[] EPG_DONATE_LEVEL_NAMES = {"base","more","picture"};
  
  public static final long DATA_LAST_DATE_NO_DATA = 0;
  
  public static final String CHANNEL_DOWNLOAD_COMPLETE = "org.tvbrowser.CHANNEL_DOWNLOAD_COMPLETE";
  public static final String MARKINGS_CHANGED = "org.tvbrowser.MARKINGS_CHANGED";
  public static final String FAVORITES_CHANGED = "org.tvbrowser.FAVORTES_CHANGED";
  public static final String DONT_WANT_TO_SEE_CHANGED = "org.tvbrowser.DONT_WANT_TO_SEE_CHANGED";
  public static final String DATA_UPDATE_DONE = "org.tvbrowser.DATA_UPDATE_DONE";
  public static final String PROGRAM_REMINDED_FOR = "org.tvbrowser.PROGRAM_REMINDED_FOR";
  public static final String REMINDER_DOWN_DONE = "org.tvbrowser.REMINDER_DOWN_DONE";
  public static final String SYNCHRONIZE_UP_DONE = "org.tvbrowser.SYNCHRONIZE_UP_DONE";
  public static final String CHANNEL_UPDATE_DONE = "org.tvbrowser.CHANNEL_UPDATE_DONE";
  public static final String REFRESH_VIEWS = "org.tvbrowser.REFRESH_VIEWS";
  public static final String UPDATE_TIME_BUTTONS = "org.tvbrowser.UPDATE_TIME_BUTTONS";
  public static final String SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT = "org.tvbrowser.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT";
  public static final String REMINDER_PROGRAM_ID_EXTRA = "REMINDER_PROGRAM_ID_EXTRA";
  public static final String CHANNEL_ID_EXTRA = "CHANNEL_ID_EXTRA";
  public static final String DAY_POSITION_EXTRA = "DAY_POSITION_EXTRA";
  public static final String FILTER_POSITION_EXTRA = "FILTER_POSITION_EXTRA";
  public static final String NO_BACK_STACKUP_EXTRA = "NO_BACK_STACKUP_EXTRA";
  public static final String EXTRA_START_TIME = "START_TIME_EXTRA";
  public static final String EXTRA_END_TIME = "EXTRA_END_TIME";
  public static final String SCROLL_POSITION_EXTRA = "SCROLL_POSITION_EXTRA";
  public static final String TIME_DATA_UPDATE_EXTRA = "TIME_DATA_UPDATE_EXTRA";
  public static final String EXTRA_DATA_UPDATE_TYPE_INTERNET_CONNECTION = "internetConnectionType";
  public static final String EXTRA_DATA_UPDATE_TYPE = "dataUpdateType";
  
  public static final String EXTRA_DATA_DATE_LAST_KNOWN = "dataDateLastKnown";
  public static final String EXTRA_REMINDED_PROGRAM = "remindedProgram";
  
  public static final String SYNCHRONIZE_SHOW_INFO_EXTRA = "SYNCHRONIZE_SHOW_INFO_EXTRA";
  public static final String SYNCHRONIZE_UP_URL_EXTRA = "SYNCHRONIZE_UP_URL_EXTRA";
  public static final String SYNCHRONIZE_UP_VALUE_EXTRA = "SYNCHRONIZE_UP_STRING_EXTRA";
  
  public static final String WIDGET_CHANNEL_SELECTION_EXTRA = "WIDGET_CHANNEL_SELECTION_EXTRA";

  public static final String START_WITH_TIME_WIDGET_RUNNING = "org.tvbrowser.START_WITH_TIME";
  public static final String SELECT_TIME_WIDGET_RUNNING = "org.tvbrowser.SELECT_TIME_WIDGET_RUNNING";
  public static final String HANDLE_APP_WIDGET_CLICK = "org.tvbrowser.HANDLE_APP_WIDGET_CLICK";
  public static final String UPDATE_RUNNING_APP_WIDGET = "org.tvbrowser.UPDATE_RUNNING_APP_WIDGET";
  public static final String UPDATE_IMPORTANT_APP_WIDGET = "org.tvbrowser.UPDATE_IMPORTANT_APP_WIDGET";
  
  public static final int[] CATEGORY_PREF_KEY_ARR = {
    R.string.PREF_INFO_SHOW_BLACK_AND_WHITE,
    R.string.PREF_INFO_SHOW_FOUR_TO_THREE,
    R.string.PREF_INFO_SHOW_SIXTEEN_TO_NINE,
    R.string.PREF_INFO_SHOW_MONO,
    R.string.PREF_INFO_SHOW_STEREO,
    R.string.PREF_INFO_SHOW_DOLBY_SOURROUND,
    R.string.PREF_INFO_SHOW_DOLBY_DIGITAL,
    R.string.PREF_INFO_SHOW_SECOND_ADUIO_PROGRAM,
    R.string.PREF_INFO_SHOW_CLOSED_CAPTION,
    R.string.PREF_INFO_SHOW_LIVE,
    R.string.PREF_INFO_SHOW_OMU,
    R.string.PREF_INFO_SHOW_FILM,
    R.string.PREF_INFO_SHOW_SERIES,
    R.string.PREF_INFO_SHOW_NEW,
    R.string.PREF_INFO_SHOW_AUDIO_DESCRIPTION,
    R.string.PREF_INFO_SHOW_NEWS,
    R.string.PREF_INFO_SHOW_SHOW,
    R.string.PREF_INFO_SHOW_MAGAZIN,
    R.string.PREF_INFO_SHOW_HD,
    R.string.PREF_INFO_SHOW_DOCU,
    R.string.PREF_INFO_SHOW_ART,
    R.string.PREF_INFO_SHOW_SPORT,
    R.string.PREF_INFO_SHOW_CHILDREN,
    R.string.PREF_INFO_SHOW_OTHER,
    R.string.PREF_INFO_SHOW_SIGN_LANGUAGE
  };
  
  public static final int[] CATEGORY_COLOR_PREF_KEY_ARR = {
    R.string.PREF_COLOR_CATEGORY_BLACK_AND_WHITE,
    R.string.PREF_COLOR_CATEGORY_FOUR_TO_THREE,
    R.string.PREF_COLOR_CATEGORY_SIXTEEN_TO_NINE,
    R.string.PREF_COLOR_CATEGORY_MONO,
    R.string.PREF_COLOR_CATEGORY_STEREO,
    R.string.PREF_COLOR_CATEGORY_DOLBY_SOURROUND,
    R.string.PREF_COLOR_CATEGORY_DOLBY_DIGITAL,
    R.string.PREF_COLOR_CATEGORY_SECOND_ADUIO_PROGRAM,
    R.string.PREF_COLOR_CATEGORY_CLOSED_CAPTION,
    R.string.PREF_COLOR_CATEGORY_LIVE,
    R.string.PREF_COLOR_CATEGORY_OMU,
    R.string.PREF_COLOR_CATEGORY_FILM,
    R.string.PREF_COLOR_CATEGORY_SERIES,
    R.string.PREF_COLOR_CATEGORY_NEW,
    R.string.PREF_COLOR_CATEGORY_AUDIO_DESCRIPTION,
    R.string.PREF_COLOR_CATEGORY_NEWS,
    R.string.PREF_COLOR_CATEGORY_SHOW,
    R.string.PREF_COLOR_CATEGORY_MAGAZIN,
    R.string.PREF_COLOR_CATEGORY_HD,
    R.string.PREF_COLOR_CATEGORY_DOCU,
    R.string.PREF_COLOR_CATEGORY_ART,
    R.string.PREF_COLOR_CATEGORY_SPORT,
    R.string.PREF_COLOR_CATEGORY_CHILDREN,
    R.string.PREF_COLOR_CATEGORY_OTHER,
    R.string.PREF_COLOR_CATEGORY_SIGN_LANGUAGE
  };
  
  @SuppressWarnings("SpellCheckingInspection")
  public static final String DONT_WANT_TO_SEE_ADDED_EXTRA = "DONT_WANT_TO_SEE_ADDED_EXTRA";
  
  public static boolean IS_DARK_THEME = false;
    
  public static final String UPDATE_RUNNING_KEY = "updateRunning";
  public static final String SELECTION_CHANNELS_KEY = "selectionChannels";
  
  public static final SparseArrayCompat<Drawable> SMALL_LOGO_MAP = new SparseArrayCompat<>();
  public static final SparseArrayCompat<Drawable> MEDIUM_LOGO_MAP = new SparseArrayCompat<>();
  
  public static String getNumberForDataServiceKey(String key) {
    String result = null;
    
    if(EPG_FREE_KEY.equals(key)) {
      result = "1";
    }
    else if(EPG_DONATE_KEY.equals(key)) {
      result = "2";
    }
    
    return result;
  }
  
  public static String getDataServiceKeyForNumber(String number) {
    String result = null;

    if("1".equals(number)) {
      result = EPG_FREE_KEY;
    }
    else if("2".equals(number)) {
      result = EPG_DONATE_KEY;
    }

    return result;
  }
  
  public static void setReminderPaused(Context context, boolean reminderPaused) {
    Editor editPref = PreferenceManager.getDefaultSharedPreferences(context).edit();
    editPref.putBoolean(REMINDER_PAUSE_KEY, reminderPaused);
    editPref.apply();
  }
  
  public static boolean isReminderPaused(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(REMINDER_PAUSE_KEY, false);
  }
  
  public static synchronized void initializeLogoMap(Context context, boolean reload) {
    if(SMALL_LOGO_MAP.size() == 0 || MEDIUM_LOGO_MAP.size() == 0 || reload) {
      PrefUtils.initialize(context.getApplicationContext());
      
      if(IOUtils.isDatabaseAccessible(context)) {
        SMALL_LOGO_MAP.clear();
        MEDIUM_LOGO_MAP.clear();
        
        final Cursor channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_LOGO}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, null);
        
        try {
          if(channels!=null && IOUtils.prepareAccess(channels)) {
            int keyIndex = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
            int logoIndex = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
            
            while(channels.moveToNext()) {
              Bitmap logoBitmap = UiUtils.createBitmapFromByteArray(channels.getBlob(logoIndex));
              
              if(logoBitmap != null) {
                SMALL_LOGO_MAP.put(channels.getInt(keyIndex), createDrawable(17,context,logoBitmap));
                MEDIUM_LOGO_MAP.put(channels.getInt(keyIndex), createDrawable(25,context,logoBitmap));
              }
            }
          }
        }finally {
          IOUtils.close(channels);
        }
      }
    }
  }
  
  private static BitmapDrawable createDrawable(int baseHeight, Context context, Bitmap logoBitmap) {
    return new BitmapDrawable(context.getResources(), UiUtils.drawableToBitmap(createLayerDrawable(baseHeight,context,logoBitmap)));
  }
  
  public static LayerDrawable createLayerDrawable(int baseHeight, Context context, Bitmap logoBitmap) {
    boolean withBorder = PrefUtils.getBooleanValue(R.string.PREF_LOGO_BORDER, R.bool.pref_logo_border_default);
    
    int padding = withBorder ? 4 : 3;
    
    float scale = UiUtils.convertDpToPixel(baseHeight, context.getResources()) / (float)logoBitmap.getHeight();
    int maxWidth = UiUtils.convertDpToPixel(80, context.getResources());
    int maxHeight = UiUtils.convertDpToPixel(baseHeight, context.getResources())+padding;
    
    int width = (int)(logoBitmap.getWidth() * scale);
    int height = (int)(logoBitmap.getHeight() * scale);
    
    if(width > maxWidth-padding) {
      width = maxWidth-padding;
      height = (int)(logoBitmap.getHeight() * width/(float)logoBitmap.getWidth());
    }
    
    BitmapDrawable logo1 = new BitmapDrawable(context.getResources(), logoBitmap);

    int backgroundColor = PrefUtils.getIntValue(R.string.PREF_LOGO_BACKGROUND_COLOR, ContextCompat.getColor(context, R.color.pref_logo_background_color_default));

    GradientDrawable background = new GradientDrawable(Orientation.BOTTOM_TOP, new int[] {backgroundColor,backgroundColor});

    LayerDrawable logo = new LayerDrawable(new Drawable[] {background,logo1});
    logo.setBounds(0, 0, maxWidth, maxHeight);
    
    if(PrefUtils.getBooleanValue(R.string.PREF_LOGO_BACKGROUND_FILL, R.bool.pref_logo_background_fill_default)) {
      background.setBounds(0, 0, maxWidth, maxHeight);
    }
    else {
      background.setBounds(maxWidth/2-width/2-padding/2, maxHeight/2-height/2-padding/2, maxWidth/2+width/2+padding/2, maxHeight/2+height/2+padding/2);
    }
    
    logo1.setBounds(maxWidth/2-width/2, maxHeight/2-height/2, maxWidth/2+width/2, maxHeight/2+height/2);
    
    if(withBorder) {
      background.setStroke(1, PrefUtils.getIntValue(R.string.PREF_LOGO_BORDER_COLOR, ContextCompat.getColor(context, R.color.pref_logo_border_color_default)));
    }
    
    return logo;
  }
  
  public static int ORIENTATION;
  
  public static boolean UPDATING_FILTER = false;
  
  public static boolean UPDATING_REMINDERS = false;
  
  public static final String[] REMINDER_PROJECTION = new String[] {
    TvBrowserContentProvider.KEY_ID,
    TvBrowserContentProvider.DATA_KEY_STARTTIME,
    TvBrowserContentProvider.DATA_KEY_ENDTIME,
    TvBrowserContentProvider.DATA_KEY_TITLE,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
    TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_DESCRIPTION,
    TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID
  };
  
  public static final String EXTRA_MARKINGS_ID = "EXTRA_MARKINGS_ID";
  public static final String EXTRA_MARKINGS_ONLY_UPDATE = "EXTRA_MARKINGS_ONLY_UPDATE";

  public static final String USER_NAME = "CAR";
  public static final String USER_PASSWORD = "BICYCLE";
  
  public static final String TERMS_ACCEPTED = "TERMS_ACCEPTED";
  public static final String EULA_ACCEPTED = "EULA_ACCEPTED";
  
  public static final String EXTRA_CHANNEL_DOWNLOAD_SUCCESSFULLY = "EXTRA_CHANNEL_DOWNLOAD_SUCCESSFULLY";
  public static final String EXTRA_CHANNEL_DOWNLOAD_AUTO_UPDATE = "EXTRA_CHANNEL_DOWNLOAD_AUTO_UPDATE";
  
  public static final IntentFilter REFRESH_FILTER = new IntentFilter(REFRESH_VIEWS);
  
  public static final HashMap<String, Integer> MARK_COLOR_KEY_MAP = new HashMap<>();
  
  public static final HashMap<String, String> SHORT_CHANNEL_NAMES = new HashMap<>();
  
  public static final int NO_CATEGORY = 0;
  public static final int TV_CATEGORY = 1;
  public static final int RADIO_CATEGORY = 1 << 1;
  public static final int CINEMA_CATEGORY = 1 << 2;
  @SuppressWarnings("unused")
  public static final int DIGITAL_CATEGORY = 1 << 4;
  @SuppressWarnings("unused")
  public static final int MUSIC_CATEGORY = 1 << 5;
  @SuppressWarnings("unused")
  public static final int SPORT_CATEGORY = 1 << 6;
  @SuppressWarnings("unused")
  public static final int NEWS_CATEGORY = 1 << 7;
  @SuppressWarnings("unused")
  public static final int NICHE_CATEGORY = 1 << 8;
  @SuppressWarnings("unused")
  public static final int PAY_TV_CATEGORY = 1 << 9;
  
  private static final int GRAY_LIGHT_VALUE = 155;
  private static final int GRAY_DARK_VALUE = 78;
  public static final int LOGO_BACKGROUND_COLOR = Color.WHITE;
  
  public static final int EXPIRED_LIGHT_COLOR = Color.rgb(GRAY_LIGHT_VALUE, GRAY_LIGHT_VALUE, GRAY_LIGHT_VALUE);
  public static final int EXPIRED_DARK_COLOR = Color.rgb(GRAY_DARK_VALUE, GRAY_DARK_VALUE, GRAY_DARK_VALUE);
  
  static {
    MARK_COLOR_KEY_MAP.put(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING, UiUtils.MARKED_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, UiUtils.MARKED_REMINDER_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE, UiUtils.MARKED_FAVORITE_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, UiUtils.MARKED_REMINDER_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC, UiUtils.MARKED_SYNC_COLOR_KEY);
    MARK_COLOR_KEY_MAP.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, UiUtils.I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR_KEY);

    final Resources res = App.get().getResources();
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_ndr_nds), res.getString(R.string.short_channel_name_ndr_nds));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_ndr_mv), res.getString(R.string.short_channel_name_ndr_mv));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_ndr_hh), res.getString(R.string.short_channel_name_ndr_hh));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_ndr_sh), res.getString(R.string.short_channel_name_ndr_sh));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_mdr_st), res.getString(R.string.short_channel_name_mdr_st));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_mdr_sn), res.getString(R.string.short_channel_name_mdr_sn));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_mdr_th), res.getString(R.string.short_channel_name_mdr_th));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_rbb_be), res.getString(R.string.short_channel_name_rbb_be));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_rbb_bb), res.getString(R.string.short_channel_name_rbb_bb));
    SHORT_CHANNEL_NAMES.put(res.getString(R.string.long_channel_name_das_erste), res.getString(R.string.short_channel_name_das_erste));
  }
}