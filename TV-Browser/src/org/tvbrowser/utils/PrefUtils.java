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
package org.tvbrowser.utils;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.filter.FilterValues;
import org.tvbrowser.filter.FilterValuesChannels;
import org.tvbrowser.tvbrowser.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

public class PrefUtils {
  private static Context mContext;
  private static SharedPreferences mPref;
  
  private PrefUtils() {}
  
  public static void initialize(Context context) {
    if(context != null && mContext == null) {
      mContext = context.getApplicationContext();
      mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
  }
  
  public static boolean setIntValue(int prefKey, int value) {
    boolean result = false;
    
    if(mPref != null) {
      Editor edit = mPref.edit();
      edit.putInt(mContext.getString(prefKey), value);
      result = edit.commit();
    }
    
    return result;
  }

  public static Context getContext() {
    return mContext;
  }

  public static int getIntValue(int prefKey, int defaultValue) {
    if(mPref != null) {
      return mPref.getInt(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static int getIntValueWithDefaultKey(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getIntValue(prefKey,mContext.getResources().getInteger(defaultKey));
    }
    
    return -1;
  }
  
  public static long getLongValue(int prefKey, long defaultValue) {
    if(mPref != null) {
      return mPref.getLong(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static long getLongValueWithDefaultKey(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getLongValue(prefKey,mContext.getResources().getInteger(defaultKey));
    }
    
    return -1;
  }
  
  public static boolean setBooleanValue(int prefKey, boolean value) {
    boolean result = false;
    
    if(mPref != null) {
      Editor edit = mPref.edit();
      edit.putBoolean(mContext.getString(prefKey), value);
      result = edit.commit();
    }
    
    return result;
  }
  
  public static boolean getBooleanValue(int prefKey, boolean defaultValue) {
    if(mPref != null) {
      return mPref.getBoolean(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static boolean getBooleanValue(int prefKey, int defaultKey) {
	  return mContext != null && getBooleanValue(prefKey, mContext.getResources().getBoolean(defaultKey));

  }
  
  public static String getStringValue(int prefKey, String defaultValue) {
    if(mPref != null) {
      return mPref.getString(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static String getStringValue(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getStringValue(prefKey,mContext.getResources().getString(defaultKey));
    }
    
    return null;
  }
  
  public static int getStringValueAsInt(int prefKey, String defaultValue) throws NumberFormatException {
    if(mPref != null) {
      String value = mPref.getString(mContext.getString(prefKey), defaultValue);
      
      if(value != null) {
        return Integer.parseInt(value);
      }
    }
    else if(defaultValue != null) {
      return Integer.parseInt(defaultValue);
    }
    
    return Integer.MIN_VALUE;
  }
  
  public static int getStringValueAsInt(int prefKey, int defaultKey) throws NumberFormatException {
    if(mContext != null) {
      String value = getStringValue(prefKey,mContext.getResources().getString(defaultKey));
      
      if(value != null) {
        return Integer.parseInt(value);
      }
    }
    
    return Integer.MIN_VALUE;
  }

  public static Set<String> getStringSetValue(int prefKey, Set<String> defaultValue) {
    if(mPref != null) {
      return mPref.getStringSet(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static Set<String> getStringSetValue(int prefKey, int defaultKey) {
    if(mContext != null) {
      String[] tempValues = mContext.getResources().getStringArray(defaultKey);
      
      HashSet<String> defaultValues = new HashSet<>();
      
      
      Collections.addAll(defaultValues, tempValues);
      return getStringSetValue(prefKey,defaultValues);
    }
    
    return null;
  }
  
  public static final int TYPE_PREFERENCES_SHARED_GLOBAL = 0;
  public static final int TYPE_PREFERENCES_FAVORITES = 1;
  public static final int TYPE_PREFERENCES_FILTERS = 2;
  public static final int TYPE_PREFERENCES_TRANSPORTATION = 3;
  public static final int TYPE_PREFERENCES_MARKINGS = 4;
  public static final int TYPE_PREFERENCES_MARKING_REMINDERS = 5;
  public static final int TYPE_PREFERENCES_MARKING_SYNC = 6;
  
  private static final String PREFERENCES_FAVORITE = "preferencesFavorite";
  private static final String PREFERENCES_FILTER = "filterPreferences";
  private static final String PREFERENCES_TRANSPORTATION = "transportation";
  private static final String PREFERENCES_MARKINGS = "markings";
  private static final String PREFERENCES_MARKING_REMINDERS = "markingsReminders";
  private static final String PREFERENCES_MARKING_SYNC = "markingsSynchronization";

  public static SharedPreferences getSharedPreferences(int type) {
    return getSharedPreferences(type, mContext);
  }

  public static SharedPreferences getSharedPreferences(int type, Context context) {
    SharedPreferences pref = null;
    
    if(context != null) {
      switch(type) {
        case TYPE_PREFERENCES_SHARED_GLOBAL: pref = PreferenceManager.getDefaultSharedPreferences(context);break;
        case TYPE_PREFERENCES_FAVORITES: pref = context.getSharedPreferences(PREFERENCES_FAVORITE, Context.MODE_PRIVATE);break;
        case TYPE_PREFERENCES_FILTERS: pref = context.getSharedPreferences(PREFERENCES_FILTER, Context.MODE_PRIVATE);break;
        case TYPE_PREFERENCES_TRANSPORTATION: pref = context.getSharedPreferences(PREFERENCES_TRANSPORTATION, Context.MODE_PRIVATE);break;
        case TYPE_PREFERENCES_MARKINGS: pref = context.getSharedPreferences(PREFERENCES_MARKINGS, Context.MODE_PRIVATE);break;
        case TYPE_PREFERENCES_MARKING_REMINDERS: pref = context.getSharedPreferences(PREFERENCES_MARKING_REMINDERS, Context.MODE_PRIVATE);break;
        case TYPE_PREFERENCES_MARKING_SYNC: pref = context.getSharedPreferences(PREFERENCES_MARKING_SYNC, Context.MODE_PRIVATE);break;
      }
    }
    
    return pref;
  }
  
  public static void resetDataMetaData(Context context) {
    Editor edit = getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context).edit();
    
    edit.putLong(context.getString(R.string.META_DATA_DATE_FIRST_KNOWN), context.getResources().getInteger(R.integer.meta_data_date_known_default));
    edit.putLong(context.getString(R.string.META_DATA_DATE_LAST_KNOWN), context.getResources().getInteger(R.integer.meta_data_date_known_default));
    edit.putLong(context.getString(R.string.META_DATA_ID_FIRST_KNOWN), context.getResources().getInteger(R.integer.meta_data_id_default));
    edit.putLong(context.getString(R.string.META_DATA_ID_LAST_KNOWN), context.getResources().getInteger(R.integer.meta_data_id_default));
    edit.putLong(context.getString(R.string.LAST_DATA_UPDATE), 0);
    
    edit.commit();
  }
  
  public static void updateDataMetaData(Context context) {
    setMetaDataLongValue(context, R.string.META_DATA_DATE_FIRST_KNOWN);
    setMetaDataLongValue(context, R.string.META_DATA_DATE_LAST_KNOWN);
    setMetaDataLongValue(context, R.string.META_DATA_ID_FIRST_KNOWN);
    setMetaDataLongValue(context, R.string.META_DATA_ID_LAST_KNOWN);
  }
  
  private static void setMetaDataLongValue(Context context, int value) {
    if(IOUtils.isDatabaseAccessible(context)) {
      String sort = null;
      String column = null;
      
      switch (value) {
        case R.string.META_DATA_DATE_FIRST_KNOWN: column = TvBrowserContentProvider.DATA_KEY_STARTTIME; sort =  column + " ASC LIMIT 1";break;
        case R.string.META_DATA_DATE_LAST_KNOWN: column = TvBrowserContentProvider.DATA_KEY_STARTTIME; sort =  column + " DESC LIMIT 1";break;
        case R.string.META_DATA_ID_FIRST_KNOWN: column = TvBrowserContentProvider.KEY_ID; sort =  column + " ASC LIMIT 1";break;
        case R.string.META_DATA_ID_LAST_KNOWN: column = TvBrowserContentProvider.KEY_ID; sort =  column + " DESC LIMIT 1";break;
      }
      
      final Cursor valueCursor = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {column}, null, null, sort);
      
      try {
        if(IOUtils.prepareAccessFirst(valueCursor)) {
          long last = valueCursor.getLong(valueCursor.getColumnIndex(column));
          PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putLong(context.getString(value), last).commit();
        }
      }finally {
        IOUtils.close(valueCursor);
      }
    }
  }
  
  public static void updateChannelSelectionState(Context context) {
    if(IOUtils.isDatabaseAccessible(context)) {
      boolean value = false;
      final Cursor channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null, TvBrowserContentProvider.KEY_ID + " ASC LIMIT 1");
      
      try {
        value = channels != null && channels.getCount() > 0;
      }finally {
        IOUtils.close(channels);
      }
      
      getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putBoolean(context.getString(R.string.CHANNELS_SELECTED), value).commit();
    }
  }
  
  public static boolean getChannelsSelected(Context context) {
    return getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context).getBoolean(context.getString(R.string.CHANNELS_SELECTED), context.getResources().getBoolean(R.bool.channels_selected_default));
  }
  
  private static String getFilterSelection(final Context context, final Set<String> filterIds) {
    final HashSet<FilterValues> filterValues = new HashSet<>();
    
    for(String filterId : filterIds) {
      final FilterValues filter = FilterValues.load(filterId, context);
      
      if(filter != null) {
        filterValues.add(filter);
      }
    }
    
    return getFilterSelection(context, false, filterValues);
  }
  
  public static String getFilterSelection(final Context context, final boolean onlyChannelFilter, final HashSet<FilterValues> filterValues) {
    final StringBuilder channels =  new StringBuilder();
    final StringBuilder result = new StringBuilder();
    
    for(FilterValues values : filterValues) {
      if(values instanceof FilterValuesChannels) {
        final int[] ids = ((FilterValuesChannels) values).getFilteredChannelIds();
        
        for(final int id : ids) {
          if(channels.length() > 0) {
            channels.append(", ");
          }
          
          channels.append(id);
        }
      }
      else if(!onlyChannelFilter) {
        result.append(values.getWhereClause(context).getWhere());
      }
    }
    
    if(channels.length() > 0) {
      result.append(" AND ").append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID).append(" IN ( ");
      result.append(channels);
      result.append(" ) ");
    }
    
    return result.toString();
  }
  
  public static String getFilterSelection(Context context) {
    final SharedPreferences pref = getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context);
        
    int oldVersion = pref.getInt(context.getString(R.string.OLD_VERSION), 379);
    
    Set<String> currentFilterIds = new HashSet<>();
    
    if(oldVersion < 379) {
      final String currentFilterId = pref.getString(context.getString(R.string.CURRENT_FILTER_ID), null);
      
      if(currentFilterId != null) {
        currentFilterIds.add(currentFilterId);
      }
    }
    else {
      currentFilterIds = pref.getStringSet(context.getString(R.string.CURRENT_FILTER_ID), currentFilterIds);
    }
    
    return getFilterSelection(context, currentFilterIds);
  }
  
  public static boolean isNewDate(Context context) {
    Log.d("info6", "LAST KNOWN START DATE " + getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context).getInt(context.getString(R.string.PREF_MISC_LAST_KNOWN_OPEN_DATE), -1) + " - CURRENT DATE " + Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
    return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context).getInt(context.getString(R.string.PREF_MISC_LAST_KNOWN_OPEN_DATE), -1);
  }
  
  public static void updateKnownOpenDate(Context context) {
    getSharedPreferences(TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putInt(context.getString(R.string.PREF_MISC_LAST_KNOWN_OPEN_DATE), Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).commit();
  }

  public static void putLong(final Editor edit, final int prefKey, final long value) {
    edit.putLong(mContext.getString(prefKey),value);
  }

  public static boolean isDarkTheme() {
    return getBooleanValue(R.string.DARK_STYLE, R.bool.dark_style_default);
  }
}
