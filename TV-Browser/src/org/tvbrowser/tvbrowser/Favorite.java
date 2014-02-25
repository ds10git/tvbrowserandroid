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
import android.util.Log;

public class Favorite {
  public static final String OLD_NAME_KEY = "OLD_NAME_KEY";
  public static final String NAME_KEY = "NAME_KEY";
  public static final String SEARCH_KEY = "SEARCH_KEY";
  public static final String ONLY_TITLE_KEY = "ONLY_TITLE_KEY";
  public static final String REMIND_KEY = "REMIND_KEY";
  
  private String mName;
  private String mSearch;
  private boolean mOnlyTitle;
  private boolean mRemind;
  
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
  
  public Favorite(String name, String search, boolean onlyTitle, boolean remind) {
    setValues(name, search, onlyTitle, remind);
  }
  
  public boolean searchOnlyTitle() {
    return mOnlyTitle;
  }
  
  public boolean remind() {
    return mRemind;
  }
  
  public String getName() {
    return mName;
  }
  
  public String getSearchValue() {
    return mSearch;
  }
  
  public void setValues(String name, String search, boolean onlyTitle, boolean remind) {
    mName = name;
    mSearch = search.replace("\"", "");
    mOnlyTitle = onlyTitle;
    mRemind = remind;
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
    builder.append(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER);
    builder.append(" ( ");
    builder.append(TvBrowserContentProvider.CONCAT_RAW_KEY);
    builder.append(" LIKE \"%");
    builder.append(mSearch);
    builder.append("%\")");
    
    return builder.toString();
  }
  
  public String getSaveString() {
    return mName + ";;" + mSearch + ";;" + String.valueOf(mOnlyTitle) + ";;" + String.valueOf(mRemind);
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
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      
      int reminderColumnIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
      
      do {
        long id = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
        
        boolean[] test = favoritesMatchesProgram(id, context, resolver, favorite);
        Log.d("test", "" + test[0] + " " + test[1]);
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
      }
    }
    
    cursor.close();
  }
  
  public static boolean[] favoritesMatchesProgram(long programID, Context context, ContentResolver resolver, Favorite exclude) {
    boolean[] returnValue = DATA_REFRESH_TABLE != null ? DATA_REFRESH_TABLE.get(Long.valueOf(programID)) : null;
    
    if(returnValue == null) {
      Set<String> favoritesSet = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
      
      boolean remindFor = false;
      boolean matches = false;
      
      for(String favorite : favoritesSet) {
        String[] values = favorite.split(";;");
        
        boolean remind = false;
        
        if(values.length > 3) {
          remind = Boolean.valueOf(values[3]);
        }
        
        Favorite fav = new Favorite(values[0], values[1], Boolean.valueOf(values[2]), remind);
        
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
          
          if(cursor.getCount() > 0) {
            matches = true;
            
            cursor.moveToFirst();
            
            remindFor = remindFor || cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) == 1;
          }
          
          cursor.close();
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
    DATA_REFRESH_TABLE.clear();
    DATA_REFRESH_TABLE = null;
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
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
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
          
          UiUtils.addReminder(context, id, startTime, Favorite.class);
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
}
