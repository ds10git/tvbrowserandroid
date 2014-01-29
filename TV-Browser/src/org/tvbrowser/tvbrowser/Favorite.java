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
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES
    };
    
    String where = favorite.getWhereClause();
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    else {
      where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
    }
    
    where +=  " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      
      do {
        long id = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
        String marking = "";
        
        if(!cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
          marking = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
        }
        
        String[] parts = marking.split(";");
        
        StringBuilder newValue = new StringBuilder();
        
        for(String part : parts) {
          if(!part.equalsIgnoreCase(SettingConstants.MARK_VALUE_FAVORITE) && !(favorite.mRemind && part.equalsIgnoreCase(SettingConstants.MARK_VALUE_REMINDER))) {
            newValue.append(part);
            newValue.append(";");
          }
        }
        
        if(newValue.length() > 0) {
          newValue.deleteCharAt(newValue.length()-1);
        }
          
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, newValue.toString());
        
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
  
  public static void updateFavoriteMarking(Context context, ContentResolver resolver, Favorite favorite) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES
    };
    
    String where = favorite.getWhereClause();
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    else {
      where += " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER;
    }
    
    where += " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      int idColumn = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      int startTimeColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      int markColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES);
      
      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
      ArrayList<Intent> markingIntentList = new ArrayList<Intent>();
      
      do {
        long id = cursor.getLong(idColumn);
        String marking = "";
        
        if(!cursor.isNull(markColumn)) {
          marking = cursor.getString(markColumn);
        }
        
        if(!marking.contains(SettingConstants.MARK_VALUE_FAVORITE) || (favorite.mRemind && !marking.contains(SettingConstants.MARK_VALUE_REMINDER))
            || (!favorite.mRemind && marking.contains(SettingConstants.MARK_VALUE_REMINDER))) {
          if(!favorite.mRemind && marking.contains(SettingConstants.MARK_VALUE_REMINDER)) {
            marking = marking.replace(SettingConstants.MARK_VALUE_REMINDER, "").replace(";;", ";");
            
            if(marking.endsWith(";")) {
              marking = marking.substring(0,marking.length()-1);
            }
            
            UiUtils.removeReminder(context, id);
          }
          
          StringBuilder value = new StringBuilder();
          value.append(marking.trim());
          
          if(!marking.contains(SettingConstants.MARK_VALUE_FAVORITE)) {
            if(value.length() != 0) {
              value.append(";");
            }
          
            value.append(SettingConstants.MARK_VALUE_FAVORITE);
          }
          
          if(favorite.mRemind && !marking.contains(SettingConstants.MARK_VALUE_REMINDER)) {
            if(value.length() != 0) {
              value.append(";");
            }
          
            value.append(SettingConstants.MARK_VALUE_REMINDER);
            UiUtils.addReminder(context, id, cursor.getLong(startTimeColumn));
          }
          
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, value.toString());
          
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
  
  @Override
  public boolean equals(Object o) {
    if(o instanceof Favorite) {
      return ((Favorite)o).mName.equals(mName);
    }
    
    return super.equals(o);
  }
}
