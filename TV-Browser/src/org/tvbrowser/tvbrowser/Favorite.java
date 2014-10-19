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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

public class Favorite implements Serializable, Cloneable, Comparable<Favorite> {
  public static final String FAVORITE_EXTRA = "FAVORITE_EXTRA";
  public static final String SEARCH_EXTRA = "SEARCH_EXTRA";
  
  public static final String OLD_NAME_KEY = "OLD_NAME_KEY";
  
  private static final String START_MINUTE_COLUMN = "startMinute";
  public static final String START_DAY_COLUMN = "startDayOfWeek";
  public static final String DURATION_COLUMN = "duration";
  
  private String mName;
  private String mSearch;
  private boolean mOnlyTitle;
  private boolean mRemind;
  private int mDurationRestrictionMinimum;
  private int mDurationRestrictionMaximum;
  private int mTimeRestrictionStart;
  private int mTimeRestrictionEnd;
  private int[] mDayRestriction;
  private int[] mChannelRestrictionIDs;
  private String[] mExclusions;
  
  private final static String[] PROJECTION = {
    TvBrowserContentProvider.KEY_ID,
    TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
    TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
    TvBrowserContentProvider.DATA_KEY_STARTTIME,
    TvBrowserContentProvider.DATA_KEY_ENDTIME,
    TvBrowserContentProvider.DATA_KEY_TITLE,
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
  
  public Favorite() {
    this(null, "", true, true, -1, -1, null, null, null, -1, -1);
  }
  
  public Favorite(String saveLine) {
    String[] values = saveLine.split(";;");
    
    mName = values[0];
    mSearch = values[1];
    mOnlyTitle = Boolean.valueOf(values[2]);
    
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
  }
  
  private static final int DAY_RESTRICTION_TYPE = 0;
  private static final int CHANNEL_RESTRICTION_TYPE = 1;
  
  private void parseArray(int type, String value) {
    int[] array = null;
    
    if(value.equals("null")) {
      array = null;
    }
    else {
      if(value.contains(",")) {
        String[] parts = value.split(",");
        
        array = new int[parts.length];
        
        for(int i = 0; i < parts.length; i++) {
          array[i] = Integer.parseInt(parts[i]);
        }
      }
      else {
        array = new int[1];
        array[0] = Integer.parseInt(value);
      }      
    }
    
    switch (type) {
      case DAY_RESTRICTION_TYPE: mDayRestriction = array; break;
      case CHANNEL_RESTRICTION_TYPE: mChannelRestrictionIDs = array; break;
    }
  }
  
 /* public Favorite(String name, String search, boolean onlyTitle, boolean remind) {
    this(name, search, onlyTitle, remind, -1, -1, null, null);
  }*/
  
  public Favorite(String name, String search, boolean onlyTitle, boolean remind, int timeRestrictionStart, int timeRestrictionEnd, int[] days, int[] channelIDs, String[] exclusions, int durationRestrictionMinimum, int durationRestrictionMaximum) {
    setValues(name, search, onlyTitle, remind, timeRestrictionStart, timeRestrictionEnd, days, channelIDs, exclusions, durationRestrictionMinimum, durationRestrictionMaximum);
  }
  
  public boolean searchOnlyTitle() {
    return mOnlyTitle;
  }
  
  public void setSearchOnlyTitle(boolean value) {
    mOnlyTitle = value;
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
  
  public boolean isDayRestricted() {
    return mDayRestriction != null;
  }
  
  public int[] getDayRestriction() {
    return mDayRestriction;
  }
  
  public boolean isChannelRestricted() {
    return mChannelRestrictionIDs != null;
  }
  
  public int[] getChannelRestrictionIDs() {
    return mChannelRestrictionIDs;
  }
  
  public void setChannelRestrictionIDs(int[] ids) {
    mChannelRestrictionIDs = ids;
  }
    
  public void setValues(String name, String search, boolean onlyTitle, boolean remind, int timeRestrictionStart, int timeRestrictionEnd, int[] days, int[] channelIDs, String[] exclusions, int durationRestrictionMinimum, int durationRestrictionMaximum) {
    mName = name;
    mSearch = search.replace("\"", "");
    mOnlyTitle = onlyTitle;
    mRemind = remind;
    mTimeRestrictionStart = timeRestrictionStart;
    mTimeRestrictionEnd = timeRestrictionEnd;
    mDayRestriction = days;
    mChannelRestrictionIDs = channelIDs;
    mExclusions = exclusions;
    mDurationRestrictionMinimum = durationRestrictionMinimum;
    mDurationRestrictionMaximum = durationRestrictionMaximum;
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
  
  public void setExclusions(String[] exclusions) {
    mExclusions = exclusions;
  }
  
  public String toString() {
    return mName;
  }
  
  public boolean isValid() {
    return mSearch != null && mSearch.trim().length() > 0;
  }
  
  public String getWhereClause() {
    StringBuilder builder = new StringBuilder(", ");
    builder.append(TvBrowserContentProvider.DATA_KEY_TITLE);
    
    if(!mOnlyTitle) {
      builder.append(" || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_DESCRIPTION);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_ACTORS);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_REGIE);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_SCRIPT);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_CAMERA);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_MODERATION);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_MUSIC);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_PRODUCER);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_GENRE);
      builder.append(",\"\") || ifnull(");
      builder.append(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS);
      builder.append(",\"\")");
    }
    
    builder.append(" AS ");
    builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
    builder.append(" ");
    
    if(isDurationRestricted()) {
      builder.append(", ( ");
      builder.append(TvBrowserContentProvider.DATA_KEY_ENDTIME);
      builder.append(" - ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append(" )/60000 AS ");
      builder.append(DURATION_COLUMN);
    }
    
    if(isTimeRestricted()) {
      builder.append(", (strftime('%H', ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append("/1000, 'unixepoch')*60 + strftime('%H', ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append("/1000, 'unixepoch')) AS ");
      builder.append(START_MINUTE_COLUMN);
    }
    
    if(isDayRestricted()) {
      builder.append(", (strftime('%w', ");
      builder.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      builder.append("/1000, 'unixepoch', 'localtime')+1) AS ");
      builder.append(START_DAY_COLUMN);
    }
    
    builder.append(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER);
    builder.append(" ( ");
    builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
    builder.append(" LIKE \"%");
    builder.append(mSearch);
    builder.append("%\")");
    
    if(isDurationRestricted()) {
      builder.append(" AND (");
      
      if(mDurationRestrictionMinimum >= 0) {
        builder.append(DURATION_COLUMN);
        builder.append(">=");
        builder.append(mDurationRestrictionMinimum);
      }
      
      if(mDurationRestrictionMaximum > 0) {
        if(mDurationRestrictionMinimum >= 0) {
          builder.append(" AND ");
        }
        
        builder.append(DURATION_COLUMN);
        builder.append("<=");
        builder.append(mDurationRestrictionMaximum);
      }
      
      builder.append(" )");
    }
    
    if(isTimeRestricted()) {
      builder.append(" AND (");
      builder.append(START_MINUTE_COLUMN);
      builder.append(">=");
      builder.append(mTimeRestrictionStart);
      
      if(mTimeRestrictionStart > mTimeRestrictionEnd) {
        builder.append(" OR ");
      }
      else {
        builder.append(" AND ");
      }
      
      builder.append(" startMinute<=");
      builder.append(mTimeRestrictionEnd);
      
      builder.append(" )");
    }
    
    if(isDayRestricted()) {
      builder.append(" AND ( ");
      builder.append(START_DAY_COLUMN);
      builder = appendInList(mDayRestriction,builder);
      builder.append(")");
    }
    
    if(isChannelRestricted()) {
      builder.append(" AND ( ");
      builder.append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      builder = appendInList(mChannelRestrictionIDs,builder);
      builder.append(")");
    }
    
    if(isHavingExclusions()) {
      builder.append(" AND NOT ( ");
      
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
  
  public String getSaveString() {
    StringBuilder saveString = new StringBuilder();
    
    saveString.append(mName);
    saveString.append(";;");
    saveString.append(mSearch);
    saveString.append(";;");
    saveString.append(String.valueOf(mOnlyTitle));
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
    
    saveString = appendSaveStringWithArray(mChannelRestrictionIDs, saveString);
    
    saveString.append(";;");
    
    saveString = appendSaveStringWithObjectArray(mExclusions, saveString);
    
    saveString.append(";;");
    
    if(isDurationRestricted()) {
      saveString.append(mDurationRestrictionMinimum).append(",").append(mDurationRestrictionMaximum);
    }
    else {
      saveString.append("null");
    }
    
    return saveString.toString();
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
  
  public static void removeFavoriteMarking(Context context, ContentResolver resolver, Favorite favorite) {
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
        TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER
    };
    
    String where = favorite.getWhereClause();
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    else {
      where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
    }
    
    where +=  " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE + " ) ";
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    try {
      if(cursor.moveToFirst()) {
        ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
        ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
        
        int reminderColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
        
        do {
          long id = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          
          boolean[] test = favoritesMatchesProgram(id, context, resolver, favorite);
          
          if(!test[0]) {
            ContentValues values = new ContentValues();
            
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE, false);
            
            if(favorite.mRemind && !test[1]) {
              values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, false);
              
              if(cursor.getInt(reminderColumnIndex) == 0) {
                UiUtils.removeReminder(context, id);
              }
            }
            
            ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id));
            opBuilder.withValues(values);
            
            updateValuesList.add(opBuilder.build());
            
            Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
            intent.putExtra(SettingConstants.MARKINGS_ID, id);
            
            markingIntentList.add(intent);
          }
        }while(cursor.moveToNext());
        
        if(!updateValuesList.isEmpty()) {
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
      cursor.close();
    }
  }
  
  public static boolean[] favoritesMatchesProgram(long programID, Context context, ContentResolver resolver, Favorite exclude) {
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
  }
  
  public static void handleDataUpdateStarted() {
    DATA_REFRESH_TABLE = new Hashtable<Long, boolean[]>();
  }
  
  public static void handleDataUpdateFinished() {
    if(DATA_REFRESH_TABLE != null) {
      DATA_REFRESH_TABLE.clear();
      DATA_REFRESH_TABLE = null;
    }
  }
  
  public static void updateFavoriteMarking(Context context, ContentResolver resolver, Favorite favorite) {
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
    
    if(cursor.moveToFirst()) {
      int idColumn = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int startTimeColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      int reminderColumnFav = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
      int reminderColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
      int removedReminderColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER);
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      
      do {
        long id = cursor.getLong(idColumn);
        long startTime = cursor.getLong(startTimeColumn);
        
        boolean[] test = favoritesMatchesProgram(id, context, resolver, favorite);
        
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE, true);
        
        if(!test[1] && cursor.getInt(reminderColumnFav) == 1 && !favorite.mRemind) {
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, false);
          
          if(cursor.getInt(reminderColumn) == 0) {
            UiUtils.removeReminder(context, id);
          }
        }
        
        if(favorite.mRemind && cursor.getInt(removedReminderColumn) == 0) {
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, true);
          
          UiUtils.addReminder(context, id, startTime, Favorite.class, true);
        }
        
        ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id));
        opBuilder.withValues(values);
        
        updateValuesList.add(opBuilder.build());
        
        Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
        intent.putExtra(SettingConstants.MARKINGS_ID, id);
        
        markingIntentList.add(intent);
      }while(cursor.moveToNext());
      
      if(!updateValuesList.isEmpty()) {
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
    
    cursor.close();
  }
  
  @Override
  public boolean equals(Object o) {
    if(o instanceof Favorite) {
      return ((Favorite)o).mName.equals(mName);
    }
    
    return super.equals(o);
  }
  
  public Favorite copy() {
    try {
      return (Favorite)clone();
    } catch (CloneNotSupportedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return null;
  }

  @Override
  public int compareTo(Favorite another) {
    return mName.compareToIgnoreCase(another.mName);
  }
}
