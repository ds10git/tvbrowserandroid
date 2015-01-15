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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.style.ImageSpan;

public class Favorite implements Serializable, Cloneable, Comparable<Favorite> {
  public static final int KEYWORD_ONLY_TITLE_TYPE = 0;
  public static final int KEYWORD_TYPE = 1;
  public static final int RESTRICTION_RULES_TYPE = 2;
  
  public static final String FAVORITE_EXTRA = "FAVORITE_EXTRA";
  public static final String SEARCH_EXTRA = "SEARCH_EXTRA";
  
  public static final String OLD_NAME_KEY = "OLD_NAME_KEY";
  
  public static final String START_DAY_COLUMN = "startDayOfWeek";
  
  public static final String KEY_MARKING_ICON = "org.tvbrowser.tvbrowser.Favorite";
  
  private String mName;
  private String mSearch;
  private boolean mRemind;
  private int mDurationRestrictionMinimum;
  private int mDurationRestrictionMaximum;
  /* Time restrictions are stored as time in minutes after
   * midnight in timezone UTC on the date of 2014-12-31.
   */
  private int mTimeRestrictionStart;
  private int mTimeRestrictionEnd;
  private int[] mDayRestriction;
  private int[] mAttributeRestrictionIndices;
  private int[] mChannelRestrictionIDs;
  private String[] mExclusions;
  private long[] mUniqueProgramIds;
  
  private int mType;
  
  private final static String[] PROJECTION = {
    TvBrowserContentProvider.KEY_ID,
    TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
    TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
    TvBrowserContentProvider.DATA_KEY_STARTTIME,
    TvBrowserContentProvider.DATA_KEY_ENDTIME,
    TvBrowserContentProvider.DATA_KEY_TITLE,
    TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE,
    TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER,
    TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER,
    TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER,
    TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
    TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
    TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
    TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
    TvBrowserContentProvider.DATA_KEY_GENRE
  };
  
  private static Hashtable<Long, boolean[]> DATA_REFRESH_TABLE = null;
  
  private long mFavoriteId;
  
  public Favorite() {
    this(null, "", KEYWORD_ONLY_TITLE_TYPE, true, -1, -1, null, null, null, -1, -1, null, null);
  }
  
  public Favorite(long id, String saveLine) {
    mFavoriteId = id;
    
    String[] values = saveLine.split(";;");
    
    mName = values[0];
    mSearch = values[1];
    
    try {
      mType = Integer.parseInt(values[2]);
    }catch(NumberFormatException e) {
      boolean onlyTitle = Boolean.valueOf(values[2]);
      
      if(onlyTitle) {
        mType = KEYWORD_ONLY_TITLE_TYPE;
      }
      else {
        mType = KEYWORD_TYPE;
      }
    }
    
    if(values.length > 3) {
      mRemind = Boolean.valueOf(values[3]);
    }
    
    if(values.length > 4) {
      if(values[4].equals("null")) {
        mTimeRestrictionStart = -1;
        mTimeRestrictionEnd = -1;
      }
      else {
        String[] parts = values[4].split(",");
        
        try {
          mTimeRestrictionStart = Integer.parseInt(parts[0]);
          mTimeRestrictionEnd = Integer.parseInt(parts[1]);
        }catch(NumberFormatException nfe) {
          mTimeRestrictionStart = -1;
          mTimeRestrictionEnd = -1;
        }
      }
      
      parseArray(DAY_RESTRICTION_TYPE, values[5]);
      parseArray(CHANNEL_RESTRICTION_TYPE, values[6]);
    }
    else {
      mTimeRestrictionStart = -1;
      mTimeRestrictionEnd = -1;
      mDayRestriction = null;
      mChannelRestrictionIDs = null;
    }
    
    if(values.length > 7) {
      if(values[7].equals("null")) {
        mExclusions = null;
      }
      else if(values[7].contains(",")) {
        mExclusions = values[7].split(",");
      }
      else {
        mExclusions = new String[1];
        mExclusions[0] = values[7];
      }
    }
    else {
      mExclusions = null;
    }
    
    if(values.length > 8) {
      if(values[8].equals("null")) {
        mDurationRestrictionMinimum = -1;
        mDurationRestrictionMaximum = -1;
      }
      else {
        String[] parts = values[8].split(",");
        
        try {
          mDurationRestrictionMinimum = Integer.parseInt(parts[0]);
          mDurationRestrictionMaximum = Integer.parseInt(parts[1]);
        }catch(NumberFormatException nfe) {
          mDurationRestrictionMinimum = -1;
          mDurationRestrictionMaximum = -1;
        }
      }
    }
    else {
      mDurationRestrictionMinimum = -1;
      mDurationRestrictionMaximum = -1;
    }
    
    if(values.length > 9) {
      parseArray(ATTRIBUTE_RESTRICTION_TYPE, values[9]);
    }
    
    if(values.length > 10) {
      if(values[10].equals("null")) {
        mUniqueProgramIds = null;
      }
      else {
        String[] parts = values[10].split(",");
        
        mUniqueProgramIds = new long[parts.length];
        
        for(int i = 0; i < parts.length; i++) {
          mUniqueProgramIds[i] = Long.parseLong(parts[i]);
        }
      }
    }
  }
  
  private String mUniqueChannelIds;
  
  private static final int DAY_RESTRICTION_TYPE = 0;
  private static final int CHANNEL_RESTRICTION_TYPE = 1;
  private static final int ATTRIBUTE_RESTRICTION_TYPE = 2;
  
  private void parseArray(int type, String value) {
    int[] array = null;
    
    if(value.equals("null")) {
      array = null;
    }
    else {
      if(type == CHANNEL_RESTRICTION_TYPE && value.contains("#_#")) {
        mUniqueChannelIds = value;
      }
      else {
        String[] parts = value.split(",");
        
        array = new int[parts.length];
        
        for(int i = 0; i < parts.length; i++) {
          array[i] = Integer.parseInt(parts[i]);
        }
      }
    }
    
    switch (type) {
      case DAY_RESTRICTION_TYPE: mDayRestriction = array; break;
      case CHANNEL_RESTRICTION_TYPE: mChannelRestrictionIDs = array; break;
      case ATTRIBUTE_RESTRICTION_TYPE: mAttributeRestrictionIndices = array; break;
    }
  }
  
  private boolean isUniqueChannelRestricted() {
    return mUniqueChannelIds != null && mUniqueChannelIds.trim().length() > 0;
  }
  
  public void loadChannelRestrictionIdsFromUniqueChannelRestriction(Context context) {
    if(isUniqueChannelRestricted()) { 
      String[] parts = mUniqueChannelIds.split(",");
      
      ArrayList<Integer> parsed = new ArrayList<Integer>();
      
      String[] projection = {
          TvBrowserContentProvider.CHANNEL_TABLE + "." + TvBrowserContentProvider.KEY_ID
      };
      
      for(int i = 0; i < parts.length; i++) {
        String[] channelIdParts = parts[i].split("#_#");
        
        if(channelIdParts.length == 3) {
          StringBuilder where = new StringBuilder();
          
          where.append(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID);
          where.append(" IS \"");
          where.append(SettingConstants.getDataServiceKeyForNumber(channelIdParts[0]));
          where.append("\" AND ");
          where.append(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
          where.append(" IS \"");
          where.append(channelIdParts[1]);
          where.append("\" AND ");
          where.append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
          where.append(" IS \"");
          where.append(channelIdParts[2]);
          where.append("\" AND ");
          where.append(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
          
          Cursor channel = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS_WITH_GROUP, projection, where.toString(), null, null);
          
          try {
            if(channel.moveToFirst()) {
              parsed.add(Integer.valueOf(channel.getInt(channel.getColumnIndex(TvBrowserContentProvider.KEY_ID))));
            }
          }finally {
            IOUtils.closeCursor(channel);
          }
        }
      }
      
      if(!parsed.isEmpty()) {
        mChannelRestrictionIDs = new int[parsed.size()];
        
        for(int i = 0; i < mChannelRestrictionIDs.length; i++) {
          mChannelRestrictionIDs[i] = parsed.get(i).intValue();
        }
      }
    }
  }
  
 /* public Favorite(String name, String search, boolean onlyTitle, boolean remind) {
    this(name, search, onlyTitle, remind, -1, -1, null, null);
  }*/
  
  public Favorite(String name, String search, int type, boolean remind, int timeRestrictionStart, int timeRestrictionEnd, int[] days, int[] channelIDs, String[] exclusions, int durationRestrictionMinimum, int durationRestrictionMaximum, int[] attributeRestriction, long[] uniqueProgramIds) {
    mFavoriteId = System.currentTimeMillis();
    setValues(name, search, type, remind, timeRestrictionStart, timeRestrictionEnd, days, channelIDs, exclusions, durationRestrictionMinimum, durationRestrictionMaximum, attributeRestriction, uniqueProgramIds);
  }
  
  public int getType() {
    return mType;
  }
  
  public void setType(int type) {
    mType = type;
  }
  
  public boolean remind() {
    return mRemind;
  }
  
  public void setRemind(boolean value) {
    mRemind = value;
  }
  
  public String getName() {
    return mName;
  }
  
  public void setName(String name) {
    mName = name;
  }
  
  public void setSearchValue(String search) {
    mSearch = search.replace("\"", "");
  }
  
  public String getSearchValue() {
    return mSearch;
  }
  
  public boolean isDurationRestricted() {
    return mDurationRestrictionMinimum >= 0 || mDurationRestrictionMaximum > 0;
  }
  
  public int getDurationRestrictionMinimum() {
    return mDurationRestrictionMinimum;
  }
  
  public int getDurationRestrictionMaximum() {
    return mDurationRestrictionMaximum;
  }
  
  public void setDurationRestrictionMinimum(int minutes) {
    mDurationRestrictionMinimum = minutes;
  }
  
  public void setDurationRestrictionMaximum(int minutes) {
    mDurationRestrictionMaximum = minutes;
  }
  
  public boolean isTimeRestricted() {
    return mTimeRestrictionStart >= 0 && mTimeRestrictionEnd >= 0;
  }
  
  public int getTimeRestrictionStart() {
    return mTimeRestrictionStart;
  }
  
  public void setTimeRestrictionStart(int minutes) {
    mTimeRestrictionStart = minutes;
  }
  
  public int getTimeRestrictionEnd() {
    return mTimeRestrictionEnd;
  }
  
  public void setTimeRestrictionEnd(int minutes) {
    mTimeRestrictionEnd = minutes;
  }
  
  public boolean isHavingRestriction() {
    return isChannelRestricted() || isDayRestricted() || isDurationRestricted() || isHavingExclusions() || isTimeRestricted() || isAttributeRestricted();
  }
  
  public boolean isDayRestricted() {
    return mDayRestriction != null;
  }
  
  public int[] getDayRestriction() {
    return mDayRestriction;
  }
  
  public boolean isChannelRestricted() {
    return mChannelRestrictionIDs != null;
  }
  
  public boolean isAttributeRestricted() {
    return mAttributeRestrictionIndices != null;
  }
  
  public int[] getAttributeRestrictionIndices() {
    return mAttributeRestrictionIndices;
  }
  
  public void setAttributeRestrictionIndices(int[] attributeIndices) {
    mAttributeRestrictionIndices = attributeIndices;
  }
  
  public int[] getChannelRestrictionIDs() {
    return mChannelRestrictionIDs;
  }
  
  public void setChannelRestrictionIDs(int[] ids) {
    mChannelRestrictionIDs = ids;
  }
    
  public void setValues(String name, String search, int type, boolean remind, int timeRestrictionStart, int timeRestrictionEnd, int[] days, int[] channelIDs, String[] exclusions, int durationRestrictionMinimum, int durationRestrictionMaximum, int[] attributeRestriction, long[] uniqueProgramIds) {
    mName = name;
    mSearch = search.replace("\"", "");
    mType = type;
    mRemind = remind;
    mTimeRestrictionStart = timeRestrictionStart;
    mTimeRestrictionEnd = timeRestrictionEnd;
    mDayRestriction = days;
    mChannelRestrictionIDs = channelIDs;
    mExclusions = exclusions;
    mDurationRestrictionMinimum = durationRestrictionMinimum;
    mDurationRestrictionMaximum = durationRestrictionMaximum;
    mAttributeRestrictionIndices = attributeRestriction;
    mUniqueProgramIds = uniqueProgramIds;
  }
  
  public void setDayRestriction(int[] days) {
    mDayRestriction = days;
  }
  
  public boolean isHavingExclusions() {
    return mExclusions != null;
  }
  
  public String[] getExclusions() {
    return mExclusions;
  }
  
  public long[] getUniqueProgramIds() {
    return mUniqueProgramIds;
  }
  
  public void setExclusions(String[] exclusions) {
    mExclusions = exclusions;
  }
  
  public String toString() {
    return mName + (mUniqueProgramIds == null ? "" : " ["+ mUniqueProgramIds.length+"]");
  }
  
  public boolean isValid() {
    return mSearch != null && mSearch.trim().length() > 0;
  }
  
  
  public WhereClause getExternalWhereClause() {
    StringBuilder where = new StringBuilder();
    String[] selectionArgs = null;
    
    if(mUniqueProgramIds != null && mUniqueProgramIds.length > 0) {
      selectionArgs = new String[mUniqueProgramIds.length];
      
      where.append(" ");
      where.append(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER);
      where.append(" ");
      where.append(TvBrowserContentProvider.KEY_ID);
      where.append(" IN ( ");
      
      for(int i = 0; i < mUniqueProgramIds.length-1; i++) {
        where.append("?, ");
        selectionArgs[i] = String.valueOf(mUniqueProgramIds[i]);
      }
      
      selectionArgs[mUniqueProgramIds.length-1] = String.valueOf(mUniqueProgramIds[mUniqueProgramIds.length-1]);
      
      where.append("? ) ");
    }
    else {
      where.append(getWhereClause());
    }
    
    return new WhereClause(where.toString(), selectionArgs);
  }
  
  private String getWhereClause() {
    StringBuilder builder = new StringBuilder();
    
    if(mType == KEYWORD_ONLY_TITLE_TYPE || mType == KEYWORD_TYPE) {
      builder.append(", ");
      builder.append(TvBrowserContentProvider.DATA_KEY_TITLE);
    }
    
    if(mType == KEYWORD_TYPE) {
      builder.append(" || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_DESCRIPTION);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_ACTORS);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_REGIE);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_SCRIPT);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_CAMERA);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_MODERATION);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_MUSIC);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_PRODUCER);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_GENRE);
      builder.append(",\"\") || ' ' || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS);
      builder.append(",\"\")");
    }
    
    if(mType == KEYWORD_ONLY_TITLE_TYPE || mType == KEYWORD_TYPE) {
      builder.append(" AS ");
      builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
      builder.append(" ");
    }
    
    /*if(isDurationRestricted()) {
      builder.append(", ( ");
      builder.append(TvBrowserContentProvider.DATA_KEY_ENDTIME);
      builder.append(" - ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append(" )/60000 AS ");
      builder.append(DURATION_COLUMN);
    }*/
    
    /*if(isTimeRestricted()) {
      builder.append(", (strftime('%H', ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append("/1000, 'unixepoch')*60 + strftime('%H', ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append("/1000, 'unixepoch')) AS ");
      builder.append(START_MINUTE_COLUMN);
    }*/
    
    if(isDayRestricted()) {
      builder.append(", (strftime('%w', ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append("/1000, 'unixepoch', 'localtime')+1) AS ");
      builder.append(START_DAY_COLUMN);
    }
    
    builder.append(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER);
    
    boolean addAnd = false;
    
    if(mType == KEYWORD_ONLY_TITLE_TYPE || mType == KEYWORD_TYPE) {
      builder.append(" ( ");
      builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
      builder.append(" LIKE \"%");
      builder.append(mSearch);
      builder.append("%\")");
      
      addAnd = true;
    }
    
    if(isDurationRestricted()) {
      if(addAnd) {
        builder.append(" AND ");
      }
      
      builder.append(" (");
      
      if(mDurationRestrictionMinimum >= 0) {
        builder.append(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES);
        builder.append(">=");
        builder.append(mDurationRestrictionMinimum);
      }
      
      if(mDurationRestrictionMaximum > 0) {
        if(mDurationRestrictionMinimum >= 0) {
          builder.append(" AND ");
        }
        
        builder.append(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES);
        builder.append("<=");
        builder.append(mDurationRestrictionMaximum);
      }
      
      builder.append(" )");
      
      addAnd = true;
    }
    
    if(isTimeRestricted()) {
      if(addAnd) {
        builder.append(" AND ");
      }
      
      builder.append(" (");
      builder.append(TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT);
      builder.append(">=");
      builder.append(mTimeRestrictionStart);
      
      if(mTimeRestrictionStart > mTimeRestrictionEnd) {
        builder.append(" OR ");
      }
      else {
        builder.append(" AND ");
      }
      
      builder.append(TvBrowserContentProvider.DATA_KEY_UTC_START_MINUTE_AFTER_MIDNIGHT);
      builder.append("<=");
      builder.append(mTimeRestrictionEnd);
      
      builder.append(" )");
      
      addAnd = true;
    }
    
    if(isDayRestricted()) {
      if(addAnd) {
        builder.append(" AND ");
      }
      builder.append(" ( ");
      builder.append(START_DAY_COLUMN);
      builder = appendInList(mDayRestriction,builder);
      builder.append(")");
      
      addAnd = true;
    }
    
    if(isChannelRestricted()) {
      if(addAnd) {
        builder.append(" AND ");
      }
      
      builder.append(" ( ");
      builder.append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      builder = appendInList(mChannelRestrictionIDs,builder);
      builder.append(")");
      
      addAnd = true;
    }
    
    if(isHavingExclusions()) {
      if(addAnd) {
        builder.append(" AND ");
      }
      
      builder.append(" NOT ( ");
      
      for(int i = 0; i < mExclusions.length - 1; i++) {
        builder.append(" ( ");
        builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
        builder.append(" LIKE \"%");
        builder.append(mExclusions[i]);
        builder.append("%\" ) OR ");
      }
      
      builder.append(" ( ");
      builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
      builder.append(" LIKE \"%");
      builder.append(mExclusions[mExclusions.length-1]);
      builder.append("%\" ) ");
      
      builder.append(")");
      
      addAnd = true;
    }
    
    if(isAttributeRestricted()) {
      if(addAnd) {
        builder.append(" AND ");
      }
      
      String[] columnNames = TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY;
      
      builder.append(" ( ");  
      
      for(int i = 0; i < mAttributeRestrictionIndices.length-1; i++) {
        builder.append(columnNames[mAttributeRestrictionIndices[i]]).append(" AND ");
      }
      
      builder.append(columnNames[mAttributeRestrictionIndices[mAttributeRestrictionIndices.length-1]]);
      
      builder.append(" ) ");
    }
    
    return builder.toString();
  }
  
  private StringBuilder appendInList(int[] array, StringBuilder builder) {
    builder.append(" IN (");
    
    for(int i = 0; i < array.length-1; i++) {
      builder.append(array[i]).append(", ");
    }
    
    builder.append(array[array.length-1]);
    builder.append(") ");
    
    return builder;
  }
  
  public long getFavoriteId() {
    return mFavoriteId;
  }
  
  public String getSaveString() {
    return getSaveString(null);
  }
  
  public String getSaveString(Context context) {
    StringBuilder saveString = new StringBuilder();
    
    saveString.append(mName);
    saveString.append(";;");
    saveString.append(mSearch);
    saveString.append(";;");
    saveString.append(String.valueOf(mType));
    saveString.append(";;");
    saveString.append(String.valueOf(mRemind));
    saveString.append(";;");
    
    if(isTimeRestricted()) {
      saveString.append(mTimeRestrictionStart).append(",").append(mTimeRestrictionEnd);
    }
    else {
      saveString.append("null");
    }
    
    saveString.append(";;");
    
    saveString = appendSaveStringWithArray(mDayRestriction, saveString);
    
    saveString.append(";;");
    
    if(context == null) {
      saveString = appendSaveStringWithArray(mChannelRestrictionIDs, saveString);
    }
    else {
      saveString.append(getUniqueChannelRestrictionIds(context));
    }
    
    saveString.append(";;");
    
    saveString = appendSaveStringWithObjectArray(mExclusions, saveString);
    
    saveString.append(";;");
    
    if(isDurationRestricted()) {
      saveString.append(mDurationRestrictionMinimum).append(",").append(mDurationRestrictionMaximum);
    }
    else {
      saveString.append("null");
    }
    
    saveString.append(";;");
    
    saveString = appendSaveStringWithArray(mAttributeRestrictionIndices, saveString);
    
    saveString.append(";;");
    
    saveString = appendSaveStringWithArray(mUniqueProgramIds, saveString);
    
    return saveString.toString();
  }
  
  final static String[] UNIQUE_CHANNEL_RESTRICTION_PROJECTION = {
      TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID,
      TvBrowserContentProvider.GROUP_KEY_GROUP_ID,
      TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID
  };
  
  private String getUniqueChannelRestrictionIds(Context context) {
    String result = null;
        
    if(mChannelRestrictionIDs != null && mChannelRestrictionIDs.length > 0) {
      StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_TABLE);
      where.append(".");
      where.append(TvBrowserContentProvider.KEY_ID);
      where.append(" IN ( ");
      
      for(int i = 0; i < mChannelRestrictionIDs.length-1; i++) {
        where.append(mChannelRestrictionIDs[i]).append(", ");
      }
      
      where.append(mChannelRestrictionIDs[mChannelRestrictionIDs.length-1]);
      where.append(" ) ");
      
      Cursor uniqueChannelIds = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS_WITH_GROUP, UNIQUE_CHANNEL_RESTRICTION_PROJECTION, where.toString(), null, null);
      StringBuilder idBuilder = new StringBuilder();
      
      try {
        uniqueChannelIds.moveToPosition(-1);
        
        int dataServiceIdColumn = uniqueChannelIds.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID);
        int groupIdColumn = uniqueChannelIds.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
        int channelIdColumn = uniqueChannelIds.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
        
        while(uniqueChannelIds.moveToNext()) {
          String dataServiceId = uniqueChannelIds.getString(dataServiceIdColumn);
          String groupId = uniqueChannelIds.getString(groupIdColumn);
          String channelId = uniqueChannelIds.getString(channelIdColumn);
          
          if(idBuilder.length() > 0) {
            idBuilder.append(",");
          }
          
          idBuilder.append(SettingConstants.getNumberForDataServiceKey(dataServiceId)).append("#_#").append(groupId).append("#_#").append(channelId);
        }
      }finally {
        IOUtils.closeCursor(uniqueChannelIds);
      }
      
      if(idBuilder.length() > 0) {
        result = idBuilder.toString();
      }
    }
    
    return result;
  }
  
  private StringBuilder appendSaveStringWithArray(int[] array, StringBuilder saveString) {
    if(array != null) {
      for(int i = 0; i < array.length-1; i++) {
        saveString.append(array[i]).append(",");
      }
      
      saveString.append(array[array.length-1]);
    }
    else {
      saveString.append("null");
    }
    
    return saveString;
  }
  
  private StringBuilder appendSaveStringWithArray(long[] array, StringBuilder saveString) {
    if(array != null) {
      for(int i = 0; i < array.length-1; i++) {
        saveString.append(array[i]).append(",");
      }
      
      saveString.append(array[array.length-1]);
    }
    else {
      saveString.append("null");
    }
    
    return saveString;
  }
  
  private StringBuilder appendSaveStringWithObjectArray(Object[] array, StringBuilder saveString) {
    if(array != null) {
      for(int i = 0; i < array.length-1; i++) {
        saveString.append(array[i]).append(",");
      }
      
      saveString.append(array[array.length-1]);
    }
    else {
      saveString.append("null");
    }
    
    return saveString;
  }
  
  public static final int TYPE_MARK_ADD = 0;
  public static final int TYPE_MARK_REMOVE = 1;
  public static final int TYPE_MARK_UPDATE_REMINDERS = 2;
  
  /**
   * Handles the marking of a Favorite.
   * <p>
   * @param context The context to use.
   * @param favorite The favorite to handle marking for.
   * @param type The marking type for the handling.
   */
  public static synchronized void handleFavoriteMarking(Context context, Favorite favorite, int type) {
    switch (type) {
      case TYPE_MARK_ADD: addFavoriteMarkingInternal(context, context.getContentResolver(), favorite, true);break;
      case TYPE_MARK_REMOVE: removeFavoriteMarkingInternal(context, context.getContentResolver(), favorite, true);break;
      case TYPE_MARK_UPDATE_REMINDERS: handleRemindersInternal(context, context.getContentResolver(), favorite);break;
    }
  }
  
  private static void handleRemindersInternal(Context context, ContentResolver resolver, Favorite favorite) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER,
        TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER,
        TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER,
        TvBrowserContentProvider.DATA_KEY_STARTTIME
    };
    
    WhereClause whereClause = favorite.getExternalWhereClause();
    String where = whereClause.getWhere();
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    else {
      where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
    }
    
    where +=  " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
        
    Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, whereClause.getSelectionArgs(), TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    try {
      int idColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int favoriteReminderIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
      int reminderIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
      int startTimeIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      int removedReminderIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER);
      
      cursor.moveToPosition(-1);
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      ArrayList<String> reminderIdList = new ArrayList<String>();
      
      while(!cursor.isClosed() && cursor.moveToNext()) {
        long id = cursor.getLong(idColumnIndex);
        int favoriteReminderMarkingCount = cursor.getInt(favoriteReminderIndex);
        boolean remind = cursor.getInt(reminderIndex) > 0;
        boolean updateMarking = false;
        
        ContentValues values = new ContentValues();
                
        if(favorite.remind()) {
          if(cursor.getInt(removedReminderIndex) == 0) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, favoriteReminderMarkingCount+1);
            
            if(favoriteReminderMarkingCount == 0 && !remind) {
              reminderIdList.add(String.valueOf(id));
              UiUtils.addReminder(context, id, cursor.getLong(startTimeIndex), Favorite.class, true);
              updateMarking = true;
            }
          }
        }
        else {
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, Math.max(0, favoriteReminderMarkingCount-1));
          
          if(favoriteReminderMarkingCount == 1 && !remind) {
            reminderIdList.add(String.valueOf(id));
            UiUtils.removeReminder(context, id);
            updateMarking = true;
          }
        }
        
        if(values.size() > 0) {
          ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id));
          opBuilder.withValues(values);
          
          updateValuesList.add(opBuilder.build());
          
          if(updateMarking) {
            Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
            intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, id);
            
            markingIntentList.add(intent);
          }
        }
      }
      
      if(!updateValuesList.isEmpty()) {
        try {
          if(!reminderIdList.isEmpty()) {
            if(favorite.remind()) {
              ProgramUtils.addReminderIds(context, reminderIdList);
            }
            else {
              ProgramUtils.removeReminderIds(context, reminderIdList);
            }
          }
          
          resolver.applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
          
          LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(context);
          
          for(Intent markUpdate : markingIntentList) {
            localBroadcast.sendBroadcast(markUpdate);
          }
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (OperationApplicationException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        UiUtils.updateImportantProgramsWidget(context.getApplicationContext());
      }
    }finally {
      IOUtils.closeCursor(cursor);
    }
  }
  
  private static void removeFavoriteMarkingInternal(Context context, ContentResolver resolver, Favorite favorite, boolean save) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE,
        TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER,
        TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER
    };
    
    WhereClause whereClause = favorite.getExternalWhereClause();
    
    String where = whereClause.getWhere();
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    else {
      where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
    }
    
    where +=  " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE + ">0 ) ";
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, whereClause.getSelectionArgs(), TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    try {
      int idColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int favoriteMarkerColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE);
      int favoriteReminderColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
      int reminderColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
      
      if(cursor.moveToFirst()) {
        ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
        ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
        ArrayList<String> removedReminderIdList = new ArrayList<String>();
        
        do {
          long id = cursor.getLong(idColumnIndex);
          int favoriteMarkCount = cursor.getInt(favoriteMarkerColumnIndex);
          int favoriteReminderCount = cursor.getInt(favoriteReminderColumnIndex);
          boolean updateMarking = favoriteMarkCount == 1;
          
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE, Math.max(0, favoriteMarkCount-1));
          
          if(favorite.remind()) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, Math.max(0, favoriteReminderCount-1));
            
            if(favoriteReminderCount == 1 && cursor.getInt(reminderColumnIndex) == 0) {
              removedReminderIdList.add(String.valueOf(id));
              UiUtils.removeReminder(context, id);
              updateMarking = true;
            }
          }
            
          ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id));
          opBuilder.withValues(values);
          
          updateValuesList.add(opBuilder.build());
          
          if(updateMarking) {
            Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
            intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, id);
            
            markingIntentList.add(intent);
          }
        }while(!cursor.isClosed() && cursor.moveToNext());
                
        if(!updateValuesList.isEmpty()) {
          if(!removedReminderIdList.isEmpty()) {
            ProgramUtils.removeReminderIds(context, removedReminderIdList);
          }
          
          try {
            resolver.applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
            
            LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(context);
            
            for(Intent markUpdate : markingIntentList) {
              localBroadcast.sendBroadcast(markUpdate);
            }
          } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          
          UiUtils.updateImportantProgramsWidget(context.getApplicationContext());
        }
      }
    }
    finally {
      IOUtils.closeCursor(cursor);
      favorite.mUniqueProgramIds = null;
      
      if(save) {
        favorite.save(context);
      }
    }
  }
  
  public void save(Context context) {
    Editor edit = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FAVORITES, context).edit();
    edit.putString(String.valueOf(getFavoriteId()), getSaveString());
    edit.commit();
  }
  
/*  private static boolean[] favoritesMatchesProgramInternal(long programID, Context context, ContentResolver resolver, Favorite exclude) {
    boolean[] returnValue = DATA_REFRESH_TABLE != null ? DATA_REFRESH_TABLE.get(Long.valueOf(programID)) : null;
    
    if(returnValue == null) {
      Set<String> favoritesSet = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
      
      boolean remindFor = false;
      boolean matches = false;
      
      for(String favorite : favoritesSet) {
        Favorite fav = new Favorite(favorite);
        
        if(exclude == null || !fav.equals(exclude)) {
          String where = fav.getWhereClause();
          
          if(where.trim().length() > 0) {
            where += " AND ";
          }
          else {
            where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
          }
          
          where += " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
          where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
          where += " AND " + TvBrowserContentProvider.KEY_ID + "=" + programID;
          
          Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, PROJECTION, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
          
          try {
            if(cursor.getCount() > 0) {
              matches = true;
              
              cursor.moveToFirst();
              
              remindFor = remindFor || cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) == 1;
            }
          }finally {
            cursor.close();
          }
        }
      }
      
      returnValue = new boolean[] {matches,remindFor};
      
      if(DATA_REFRESH_TABLE != null) {
        DATA_REFRESH_TABLE.put(Long.valueOf(programID), returnValue);
      }
    }
    
    return returnValue;
  }*/
  
  public static void handleDataUpdateStarted() {
    DATA_REFRESH_TABLE = new Hashtable<Long, boolean[]>();
  }
  
  public static void handleDataUpdateFinished() {
    if(DATA_REFRESH_TABLE != null) {
      DATA_REFRESH_TABLE.clear();
      DATA_REFRESH_TABLE = null;
    }
  }
  
  private static void addFavoriteMarkingInternal(Context context, ContentResolver resolver, Favorite favorite, boolean save) {
    String where = favorite.getWhereClause();
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    else {
      where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
    }
    
    where += " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, PROJECTION, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    try {
      if(cursor.moveToFirst()) {
        int idColumn = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
        int startTimeColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        int favoriteMarkingColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE);
        int reminderColumnFav = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
        int reminderColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
        int removedReminderColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER);
        
        favorite.mUniqueProgramIds = new long[cursor.getCount()];
        
        int count = 0;
        
        ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
        ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
        ArrayList<String> reminderIdList = new ArrayList<String>();
        
        do {
          long id = cursor.getLong(idColumn);
          long startTime = cursor.getLong(startTimeColumn);
          int markingCount = cursor.getInt(favoriteMarkingColumn);
          boolean markingsChanged = markingCount == 0;
          
          favorite.mUniqueProgramIds[count] = id;
          count++;
          
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE, markingCount+1);
          
          if(favorite.remind() && cursor.getInt(removedReminderColumn) == 0) {
            int favoriteReminderCount = cursor.getInt(reminderColumnFav);
            
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, favoriteReminderCount+1);
            
            if(favoriteReminderCount == 0 && cursor.getInt(reminderColumn) == 0) {
              reminderIdList.add(String.valueOf(id));
              markingsChanged = true;
              UiUtils.addReminder(context, id, startTime, Favorite.class, true);
            }
          }
          
          ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id));
          opBuilder.withValues(values);
          
          updateValuesList.add(opBuilder.build());
          
          if(markingsChanged) {
            Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
            intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, id);
            
            markingIntentList.add(intent);
          }
        }while(cursor.moveToNext());
        
        if(!updateValuesList.isEmpty()) {
          if(!reminderIdList.isEmpty()) {
            ProgramUtils.addReminderIds(context, reminderIdList);
          }
          
          try {
            resolver.applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
            
            LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(context);
            
            for(Intent markUpdate : markingIntentList) {
              localBroadcast.sendBroadcast(markUpdate);
            }
          } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          
          UiUtils.updateImportantProgramsWidget(context.getApplicationContext());
        }
      }
    }finally {
      IOUtils.closeCursor(cursor);
      
      if(save) {
        favorite.save(context);
      }
    }
  }
  
  @Override
  public boolean equals(Object o) {
    if(o instanceof Favorite) {
      return ((Favorite)o).mName.equals(mName);
    }
    
    return super.equals(o);
  }
  
  public Favorite copy() {
    return new Favorite(mFavoriteId,getSaveString());
  }

  @Override
  public int compareTo(Favorite another) {
    return mName.compareToIgnoreCase(another.mName);
  }
  
  public static final Favorite[] getAllFavorites(Context context) {
    SharedPreferences prefFavorites = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FAVORITES, context);
    
    ArrayList<Favorite> favoriteList = new ArrayList<Favorite>();
    Map<String,?> favorites = prefFavorites.getAll();
    
    Set<String> keys = favorites.keySet();
    
    for(String key : keys) {
      String saveLine = (String)favorites.get(key);
      Favorite fav = new Favorite(Long.parseLong(key), saveLine);
      
      if(fav.isValid()) {
        favoriteList.add(fav);
      }
    }
    
    return favoriteList.toArray(new Favorite[favoriteList.size()]);
  }
  
  public static final void deleteFavorite(Context context, Favorite favorite) {
    Favorite.removeFavoriteMarkingInternal(context, context.getContentResolver(), favorite, false);
    
    Editor edit = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FAVORITES, context).edit();
    edit.remove(String.valueOf(favorite.getFavoriteId()));
    edit.commit();
  }
  
  public static final void deleteAllFavorites(Context context) {
    Editor edit = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FAVORITES, context).edit();
    Favorite[] favorites = getAllFavorites(context);
    
    for(Favorite favorite : favorites) {
      Favorite.removeFavoriteMarkingInternal(context, context.getContentResolver(), favorite, false);
      edit.remove(String.valueOf(favorite.getFavoriteId()));
    }
    
    edit.commit();
  }
  
  public static final int getFavoriteMarkIconType(Context context, long programId) {
    int result = 0;
    
    Favorite[] favorites = getAllFavorites(context);
    
    for(Favorite fav : favorites) {
      long[] programIds = fav.getUniqueProgramIds();
      
      if(programIds != null) {
        for(long test : programIds) {
          if(test == programId) {
            result++;
            break;
          }
        }
        
        if(result > 1) {
          break;
        }
      }
    }
    
    return result;
  }
  
  private static ImageSpan MARK_ICON_SINGLE;
  private static ImageSpan MARK_ICON_MULTIPLE;
  
  public static void resetMarkIcons(boolean isDarkTheme) {
    if(!isDarkTheme) {
      if(MARK_ICON_SINGLE != null) {
        MARK_ICON_SINGLE.getDrawable().setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
      }
      if(MARK_ICON_MULTIPLE != null) {
        MARK_ICON_MULTIPLE.getDrawable().setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
      }
    }
    else {
      if(MARK_ICON_SINGLE != null) {
        MARK_ICON_SINGLE.getDrawable().setColorFilter(null);
      }
      if(MARK_ICON_MULTIPLE != null) {
        MARK_ICON_MULTIPLE.getDrawable().setColorFilter(null);
      }
    }
  }
  
  public static final ImageSpan getMarkIcon(Context context, int type) {
    ImageSpan result = null;
    
    if(type > 1) {
      if(MARK_ICON_MULTIPLE == null) {
        MARK_ICON_MULTIPLE = UiUtils.createImageSpan(context, R.drawable.ic_favorite_mark_more);
      }
      
      result = MARK_ICON_MULTIPLE;
    }
    else {
      if(MARK_ICON_SINGLE == null) {
        MARK_ICON_SINGLE = UiUtils.createImageSpan(context, R.drawable.ic_favorite_mark);
      }
      
      result = MARK_ICON_SINGLE;
    }
    
    return result;
  }
}
