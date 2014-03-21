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
package org.tvbrowser.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.tvbrowser.tvbrowser.IOUtils;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class TvBrowserContentProvider extends ContentProvider {
  public static final String AUTHORITY = "org.tvbrowser.tvbrowsercontentprovider";
  public static final Uri CONTENT_URI_GROUPS = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/groups");
  public static final Uri CONTENT_URI_CHANNELS = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/channels");
  public static final Uri CONTENT_URI_DATA = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/data");
  public static final Uri RAW_QUERY_CONTENT_URI_DATA = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/rawdata");
  public static final Uri CONTENT_URI_DATA_UPDATE = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/dataupdate");
  public static final Uri CONTENT_URI_DATA_WITH_CHANNEL = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/datachannels");
  public static final Uri CONTENT_URI_DATA_VERSION = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/dataversion");
    
  public static final String KEY_ID = "_id";
  
  public static boolean INFORM_FOR_CHANGES = true;
  
  private TvBrowserDataBaseHelper mDataBaseHelper;
  
  private static final int GROUPS = 1;
  private static final int GROUP_ID = 2;
  
  private static final int CHANNELS = 10;
  private static final int CHANNEL_ID = 11;

  private static final int DATA = 20;
  private static final int DATA_ID = 21;
  
  private static final int RAW_DATA = 22;
  private static final int RAW_DATA_ID = 23;

  private static final int DATA_CHANNELS = 30;
  private static final int DATA_CHANNEL_ID = 31;
  
  private static final int DATA_UPDATE = 40;
  private static final int DATA_UPDATE_ID = 41;
  
  private static final int DATA_VERSION = 50;
  private static final int DATA_VERSION_ID = 51;
  
  private static final int SEARCH = 100;
  
  private static final HashMap<String,String> SEARCH_PROJECTION_MAP;
  
  // column name for CONCAT raw queries
  public static final String CONCAT_TABLE_PLACE_HOLDER = "concatTablePlaceHolder";
  public static final String CONCAT_RAW_KEY = "concatRawQueryColumn";
  
  // Column names for group table
  public static final String GROUP_KEY_DATA_SERVICE_ID = "dataServiceID";
  public static final String GROUP_KEY_GROUP_ID = "groupID";
  public static final String GROUP_KEY_GROUP_NAME = "groupName";
  public static final String GROUP_KEY_GROUP_PROVIDER = "groupProvider";
  public static final String GROUP_KEY_GROUP_DESCRIPTION = "groupDescription";
  public static final String GROUP_KEY_GROUP_MIRRORS = "groupMirrors";
  
  // Column names for the channel table
  public static final String CHANNEL_KEY_BASE_COUNTRY = "baseCountry";
  public static final String CHANNEL_KEY_TIMEZONE = "timeZone";
  public static final String CHANNEL_KEY_CHANNEL_ID = "channelID";
  public static final String CHANNEL_KEY_NAME = "name";
  public static final String CHANNEL_KEY_COPYRIGHT = "copyright";
  public static final String CHANNEL_KEY_WEBSITE = "website";
  public static final String CHANNEL_KEY_LOGO_URL = "logoURL";
  public static final String CHANNEL_KEY_LOGO = "logo";
  public static final String CHANNEL_KEY_CATEGORY = "category";
  public static final String CHANNEL_KEY_FULL_NAME = "fullName";
  public static final String CHANNEL_KEY_ALL_COUNTRIES = "allCountries";
  public static final String CHANNEL_KEY_JOINED_CHANNEL_ID = "joinedChannelID";
  public static final String CHANNEL_KEY_ORDER_NUMBER = "orderNumber";
  public static final String CHANNEL_KEY_SELECTION = "isSelected";
  public static final String CHANNEL_KEY_USER_CHANNEL_NAME = "userChannelName";
  public static final String CHANNEL_KEY_USER_ICON = "userChannelIcon";
  public static final String CHANNEL_KEY_USER_START_TIME = "userStartTime";
  public static final String CHANNEL_KEY_USER_END_TIME = "userEndTime";
  
  // Column names for the data table
  public static final String DATA_KEY_STARTTIME = "startTime";
  public static final String DATA_KEY_ENDTIME = "endTime";
  public static final String DATA_KEY_TITLE = "title";
  public static final String DATA_KEY_TITLE_ORIGINAL = "titleOriginal";
  public static final String DATA_KEY_EPISODE_TITLE = "episodeTitle";
  public static final String DATA_KEY_EPISODE_TITLE_ORIGINAL = "episodeTitleOriginal";
  public static final String DATA_KEY_SHORT_DESCRIPTION = "shortDescription";
  public static final String DATA_KEY_DESCRIPTION = "description";
  public static final String DATA_KEY_ACTORS = "actors";
  public static final String DATA_KEY_REGIE = "regie";
  public static final String DATA_KEY_CUSTOM_INFO = "customInfo";
  public static final String DATA_KEY_CATEGORIES = "categories";
  public static final String DATA_KEY_AGE_LIMIT = "ageLimit";
  public static final String DATA_KEY_WEBSITE_LINK = "websiteLink";
  public static final String DATA_KEY_GENRE = "genre";
  public static final String DATA_KEY_ORIGIN = "origin";
  public static final String DATA_KEY_NETTO_PLAY_TIME = "nettoPlayTime";
  public static final String DATA_KEY_VPS = "vps";
  public static final String DATA_KEY_SCRIPT = "script";
  public static final String DATA_KEY_REPETITION_FROM = "repetitionFrom";
  public static final String DATA_KEY_MUSIC = "music";
  public static final String DATA_KEY_MODERATION = "moderation";
  public static final String DATA_KEY_YEAR = "year";
  public static final String DATA_KEY_REPETITION_ON = "repetitionOn";
  public static final String DATA_KEY_PICTURE = "picture";
  public static final String DATA_KEY_PICTURE_COPYRIGHT = "pictureCopyright";
  public static final String DATA_KEY_PICTURE_DESCRIPTION = "pictureDescription";
  public static final String DATA_KEY_EPISODE_NUMBER = "episodeNumber";
  public static final String DATA_KEY_EPISODE_COUNT = "episodeCount";
  public static final String DATA_KEY_SEASON_NUMBER = "seasonNumber";
  public static final String DATA_KEY_PRODUCER = "producer";
  public static final String DATA_KEY_CAMERA = "camera";
  public static final String DATA_KEY_CUT = "cut";
  public static final String DATA_KEY_OTHER_PERSONS = "otherPersons";
  public static final String DATA_KEY_RATING = "rating";
  public static final String DATA_KEY_PRODUCTION_FIRM = "productionFirm";
  public static final String DATA_KEY_AGE_LIMIT_STRING = "ageLimitString";
  public static final String DATA_KEY_LAST_PRODUCTION_YEAR = "lastProductionYear";
  public static final String DATA_KEY_ADDITIONAL_INFO = "additionalInfo";
  public static final String DATA_KEY_SERIES = "series";
  public static final String DATA_KEY_UNIX_DATE = "unixDate";
  public static final String DATA_KEY_DATE_PROG_ID = "dateProgID";
  public static final String DATA_KEY_DONT_WANT_TO_SEE = "dontWantToSee";
  public static final String DATA_KEY_REMOVED_REMINDER = "removedReminder";
  public static final String DATA_KEY_MARKING_MARKING = "markingMarking";
  public static final String DATA_KEY_MARKING_FAVORITE = "markingFavorite";
  public static final String DATA_KEY_MARKING_FAVORITE_REMINDER = "markingFavoriteReminder";
  public static final String DATA_KEY_MARKING_REMINDER = "markingReminder";
  public static final String DATA_KEY_MARKING_CALENDAR = "markingCalendar";
  public static final String DATA_KEY_MARKING_SYNC = "markingSync";
  
  // Column names for data version table
  public static final String VERSION_KEY_DAYS_SINCE_1970 = "daysSince1970";
  public static final String VERSION_KEY_BASE_VERSION = "baseVersion";
  public static final String VERSION_KEY_MORE0016_VERSION = "more0016Version";
  public static final String VERSION_KEY_MORE1600_VERSION = "more1600Version";
  public static final String VERSION_KEY_PICTURE0016_VERSION = "picture0016Version";
  public static final String VERSION_KEY_PICTURE1600_VERSION = "picture1600Version";
  
  public static final String[] MARKING_COLUMNS = {
    DATA_KEY_MARKING_MARKING,
    DATA_KEY_MARKING_FAVORITE,
    DATA_KEY_MARKING_FAVORITE_REMINDER,
    DATA_KEY_MARKING_REMINDER,
    DATA_KEY_MARKING_CALENDAR,
    DATA_KEY_MARKING_SYNC
  };
  
  static {
    SEARCH_PROJECTION_MAP = new HashMap<String, String>();
    SEARCH_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_1, DATA_KEY_TITLE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
    SEARCH_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_2, DATA_KEY_EPISODE_TITLE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
    SEARCH_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,KEY_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
    SEARCH_PROJECTION_MAP.put("_id", KEY_ID + " AS " + "_id");
  }
  
  private static final UriMatcher uriMatcher;
  
  // Allocate the UriMatcher object, where a URI ending in 'earthquakes' will correspond to a request
  // for all eathquakes, and 'earthquakes' with a traisling '/[rowID]' will represent a single earthquake row.
  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "groups", GROUPS);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "groups/#", GROUP_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "channels", CHANNELS);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "channels/#", CHANNEL_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "data", DATA);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "data/#", DATA_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "rawdata", RAW_DATA);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "rawdata/#", RAW_DATA_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "dataupdate", DATA_UPDATE);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "dataupdate/#", DATA_UPDATE_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "datachannels", DATA_CHANNELS);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "datachannels/#", DATA_CHANNEL_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_SHORTCUT, SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "dataversion", DATA_VERSION);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "dataversion/#", DATA_VERSION_ID);
  }
  
  public static final String[] getColumnArrayWithMarkingColums(String... columns) {
    String[] projection = new String[columns.length + TvBrowserContentProvider.MARKING_COLUMNS.length];
    
    System.arraycopy(TvBrowserContentProvider.MARKING_COLUMNS, 0, projection, 0, TvBrowserContentProvider.MARKING_COLUMNS.length);
    System.arraycopy(columns, 0, projection, TvBrowserContentProvider.MARKING_COLUMNS.length, columns.length);
    
    return projection;
 }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    int count;
    
    boolean data_with_channel = false;
    
    switch(uriMatcher.match(uri)) {
      case GROUPS: count = database.delete(TvBrowserDataBaseHelper.GROUPS_TABLE, where, whereArgs);break;
      case GROUP_ID: {String segment = uri.getPathSegments().get(1);
      
      count = database.delete(TvBrowserDataBaseHelper.GROUPS_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case CHANNELS: count = database.delete(CHANNEL_TABLE, where, whereArgs);break;
      case CHANNEL_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.delete(CHANNEL_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case DATA: count = database.delete(TvBrowserDataBaseHelper.DATA_TABLE, where, whereArgs);break;
      case DATA_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.delete(TvBrowserDataBaseHelper.DATA_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case DATA_UPDATE: count = database.delete(TvBrowserDataBaseHelper.DATA_TABLE, where, whereArgs);break;
      case DATA_UPDATE_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.delete(TvBrowserDataBaseHelper.DATA_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case DATA_VERSION: count = database.delete(TvBrowserDataBaseHelper.VERSION_TABLE, where, whereArgs);break;
      case DATA_VERSION_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.delete(TvBrowserDataBaseHelper.VERSION_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      default: throw new IllegalArgumentException("Unsupported URI: " + uri);
    }
    
    getContext().getContentResolver().notifyChange(uri, null);
    
    if(data_with_channel) {
      getContext().getContentResolver().notifyChange(CONTENT_URI_DATA_WITH_CHANNEL, null);
    }
    
    return count;
  }

  @Override
  public String getType(Uri uri) {
    switch(uriMatcher.match(uri)) {
      case GROUPS: return "vnd.andorid.cursor.dir/vnd.tvbrowser.groups";
      case GROUP_ID: return "vnd.andorid.cursor.item/vnd.tvbrowser.groups";
      case CHANNELS: return "vnd.andorid.cursor.dir/vnd.tvbrowser.channels";
      case CHANNEL_ID: return "vnd.andorid.cursor.item/vnd.tvbrowser.channels";
      case DATA: return "vnd.andorid.cursor.dir/vnd.tvbrowser.data";
      case DATA_ID: return "vnd.andorid.cursor.item/vnd.tvbrowser.data";
      case RAW_DATA: return "vnd.andorid.cursor.dir/vnd.tvbrowser.rawdata";
      case RAW_DATA_ID: return "vnd.andorid.cursor.item/vnd.tvbrowser.rawdata";      
      case DATA_UPDATE: return "vnd.andorid.cursor.dir/vnd.tvbrowser.dataupdate";
      case DATA_UPDATE_ID: return "vnd.andorid.cursor.item/vnd.tvbrowser.dataupdate";
      case DATA_CHANNELS: return "vnd.andorid.cursor.dir/vnd.tvbrowser.datachannels";
      case DATA_CHANNEL_ID: return "vnd.andorid.cursor.item/vnd.tvbrowser.datachannels";
      case SEARCH: return SearchManager.SUGGEST_MIME_TYPE;
      
      default: throw new IllegalArgumentException("Unsupported URI: " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    switch(uriMatcher.match(uri)) {
      case GROUPS: return insertGroup(uri, values);
      case CHANNELS: return insertChannel(uri, values);
      case DATA: return insertData(uri, values);
      case DATA_UPDATE: return insertData(uri, values);
      case DATA_VERSION: return insertVersion(uri, values);
    }
    
    throw new SQLException("Failed to insert row into " + uri);
  }
  
  @Override
  public int bulkInsert(Uri uri, ContentValues[] values) {
    switch(uriMatcher.match(uri)) {
      case DATA: return bulkInsertData(uri, values);
      case DATA_UPDATE: return bulkInsertData(uri, values);
    }
  
    throw new SQLException("Failed to insert row into " + uri);
  }
  
  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    ArrayList<ContentProviderResult> result = new ArrayList<ContentProviderResult>(0);
    
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    database.beginTransaction();
    
    HashMap<Uri, Uri> updateUris = new HashMap<Uri, Uri>();
    
    for(ContentProviderOperation op : operations) {
      Uri uri = op.getUri();
      
      ContentValues values = op.resolveValueBackReferences(null, 0);
      
      String segment = uri.getPathSegments().get(1);
      
      int count = database.update(TvBrowserDataBaseHelper.DATA_TABLE, values, KEY_ID + "=" + segment, null);
      
      if(count > 0) {
        updateUris.put(uri, uri);
      }
      
      result.add(new ContentProviderResult(count));
    }
    
    database.setTransactionSuccessful();
    database.endTransaction();
    
    Set<Uri> uris = updateUris.keySet();
    
    if(INFORM_FOR_CHANGES) {
      for(Uri uri : uris) {
        getContext().getContentResolver().notifyChange(uri, null);
        
        switch (uriMatcher.match(uri)) {
          case DATA: getContext().getContentResolver().notifyChange(uri, null);break;
          case DATA_ID: String segment = uri.getPathSegments().get(1);
          getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(CONTENT_URI_DATA_WITH_CHANNEL,Integer.parseInt(segment)),null);
            break;
        }
      }
    }
    
    return result.toArray(new ContentProviderResult[result.size()]);
  }
  
  private int bulkInsertData(Uri uri, ContentValues[] values) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    database.beginTransaction();
    
    int count = 0;
    
    for(ContentValues value : values) {
      long rowID = database.insert(TvBrowserDataBaseHelper.DATA_TABLE, "channel", value);
      
      if(rowID != -1) {
        Uri newUri = ContentUris.withAppendedId(CONTENT_URI_DATA, rowID);
        
        if(INFORM_FOR_CHANGES) {
          getContext().getContentResolver().notifyChange(newUri, null);
        }
      
        count++;
      }
    }
    
    database.setTransactionSuccessful();
    database.endTransaction();
    
    // Return a URI to the newly inserted row on success.
    if(count >= 0) {
      return count;
    }
    
    throw new SQLException("Failed to insert row into " + uri + " " + count);
  }
  
  private Uri insertVersion(Uri uri, ContentValues values) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    // Insert the new row. The call to databse.insert will return the row number if it is successfull.
    long rowID = database.insert(TvBrowserDataBaseHelper.VERSION_TABLE, "version", values);
    
    // Return a URI to the newly inserted row on success.
    
    if(rowID >= 0) {
      Uri newUri = ContentUris.withAppendedId(CONTENT_URI_DATA, rowID);
      
      if(INFORM_FOR_CHANGES) {
        getContext().getContentResolver().notifyChange(newUri, null);
      }
      
      return newUri;
    }
    
    throw new SQLException("Failed to insert row into " + uri + " " + rowID);
  }

  
  private Uri insertData(Uri uri, ContentValues values) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    // Insert the new row. The call to databse.insert will return the row number if it is successfull.
    long rowID = database.insert(TvBrowserDataBaseHelper.DATA_TABLE, "channel", values);
    
    // Return a URI to the newly inserted row on success.
    
    if(rowID >= 0) {
      Uri newUri = ContentUris.withAppendedId(CONTENT_URI_DATA, rowID);
      
      if(INFORM_FOR_CHANGES) {
        getContext().getContentResolver().notifyChange(newUri, null);
      }
      
      return newUri;
    }
    
    throw new SQLException("Failed to insert row into " + uri + " " + rowID);
  }
  
  private Uri insertChannel(Uri uri, ContentValues values) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    // Insert the new row. The call to databse.insert will return the row number if it is successfull.
    long rowID = database.insert(CHANNEL_TABLE, "channel", values);
    
    // Return a URI to the newly inserted row on success.
    
    if(rowID >= 0) {
      Uri newUri = ContentUris.withAppendedId(CONTENT_URI_CHANNELS, rowID);
      
      if(INFORM_FOR_CHANGES) {
        getContext().getContentResolver().notifyChange(newUri, null);
      }
      
      return newUri;
    }
    
    throw new SQLException("Failed to insert row into " + uri + " " + rowID);
  }
  
  private Uri insertGroup(Uri uri, ContentValues values) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    // Insert the new row. The call to databse.insert will return the row number if it is successfull.
    long rowID = database.insert(TvBrowserDataBaseHelper.GROUPS_TABLE, "group", values);
    
    // Return a URI to the newly inserted row on success.
    
    if(rowID > 0) {
      Uri newUri = ContentUris.withAppendedId(CONTENT_URI_GROUPS, rowID);
      
      if(INFORM_FOR_CHANGES) {
        getContext().getContentResolver().notifyChange(newUri, null);
      }
      
      return newUri;
    }
    
    throw new SQLException("Failed to insert row into " + uri + " " + rowID);
  }

  @Override
  public boolean onCreate() {
    mDataBaseHelper = new TvBrowserDataBaseHelper(getContext(), TvBrowserDataBaseHelper.DATABASE_NAME, null, TvBrowserDataBaseHelper.DATABASE_VERSION);
    
    return true;
  }

  private Cursor rawQueryData(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    if(projection != null) {
      for(int i = 0; i < projection.length; i++) {
        if(projection[i].equals(KEY_ID) || projection[i].equals(CHANNEL_KEY_CHANNEL_ID)) {
          projection[i] = TvBrowserDataBaseHelper.DATA_TABLE + "." + projection[i]+ " AS " + projection[i];
        }
      }
    }
    
    if(selectionArgs != null) {
      for(int i = 0; i < selectionArgs.length; i++) {
        if(selectionArgs[i].equals(KEY_ID)) {
          selectionArgs[i] = TvBrowserDataBaseHelper.DATA_TABLE + "." + selectionArgs[i];
        }
      }
    }
    
    StringBuilder sql = new StringBuilder();
    
    sql.append("SELECT ");
    
    if(projection == null) {
      sql.append("*, ");
    }
    else {
      for(int i = 0; i < projection.length; i++) {
        if(i > 0) {
          sql.append(", ");
        }
        
        sql.append(projection[i]);
      }
    }
    
    if(selection != null && selection.contains(KEY_ID) && !selection.contains("."+KEY_ID)) {
      selection = selection.replace(KEY_ID, TvBrowserDataBaseHelper.DATA_TABLE + "."+KEY_ID);
    }
    if(selection != null && selection.contains(CHANNEL_KEY_CHANNEL_ID) && !selection.contains("."+CHANNEL_KEY_CHANNEL_ID)) {
      selection = selection.replace(CHANNEL_KEY_CHANNEL_ID, TvBrowserDataBaseHelper.DATA_TABLE + "."+CHANNEL_KEY_CHANNEL_ID);
    }
    
    String sel = selection.replace(CONCAT_TABLE_PLACE_HOLDER, " FROM " + TvBrowserDataBaseHelper.DATA_TABLE + ", " + CHANNEL_TABLE + " WHERE ");
    
    sql.append(sel);
    
    sql.append(" AND " + CHANNEL_TABLE + "." + KEY_ID + "=" + TvBrowserDataBaseHelper.DATA_TABLE + "." + CHANNEL_KEY_CHANNEL_ID);
    
    String orderBy = CHANNEL_KEY_CHANNEL_ID;
    
    if(sortOrder != null && sortOrder.trim().length() > 0) {
      orderBy = sortOrder;
    }
    
    if(orderBy != null && !orderBy.contains("NOCASE") && !orderBy.contains("COLLATE")) {
      orderBy += " COLLATE NOCASE";
    }
    
    if(orderBy != null) {
      sql.append(" ORDER BY ").append(orderBy);
    }
    
    // Apply the query to the underling database.
    Cursor c = database.rawQuery(sql.toString(), selectionArgs);
    
    // Register the contexts ContentResolver to be notified if the cursor result set changes.
    
    if(INFORM_FOR_CHANGES) {
      c.setNotificationUri(getContext().getContentResolver(), uri);
    }
    
    return c;
  }
  
  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    
    // If no sort order is specified, sort by date / time
    String orderBy = null;
    
    // If this is a row query, limit the result set to teh pased in row.
    switch(uriMatcher.match(uri)) {
      case SEARCH: String search = uri.getPathSegments().get(1).replace("\"", "");
                   qb.appendWhere("(" + DATA_KEY_TITLE + " LIKE \"%" + search + "%\" OR " + DATA_KEY_EPISODE_TITLE + " LIKE \"%" +  search + "%\") AND " + DATA_KEY_STARTTIME + ">=" + System.currentTimeMillis());
                   qb.setProjectionMap(SEARCH_PROJECTION_MAP);
                   qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE);
                   orderBy = DATA_KEY_STARTTIME;
                   break;
      case GROUP_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case GROUPS: qb.setTables(TvBrowserDataBaseHelper.GROUPS_TABLE);
                   orderBy = GROUP_KEY_GROUP_ID;break;
      case CHANNEL_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case CHANNELS: qb.setTables(CHANNEL_TABLE);
                    orderBy = CHANNEL_KEY_NAME;break;
      case DATA_VERSION_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case DATA_VERSION: qb.setTables(TvBrowserDataBaseHelper.VERSION_TABLE);
                    orderBy = VERSION_KEY_DAYS_SINCE_1970;break;
      case DATA_UPDATE_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case DATA_UPDATE: qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE);
                    orderBy = CHANNEL_KEY_CHANNEL_ID;break;
      case DATA_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case DATA: qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE);
                    orderBy = CHANNEL_KEY_CHANNEL_ID;break;
      case RAW_DATA_ID: selection += " " + KEY_ID + "=" + uri.getPathSegments().get(1);
      case RAW_DATA: return rawQueryData(CONTENT_URI_DATA, projection, selection, selectionArgs, sortOrder);
      case DATA_CHANNEL_ID: qb.appendWhere(TvBrowserDataBaseHelper.DATA_TABLE + "." + KEY_ID + "=" + uri.getPathSegments().get(1) + " AND ");
      case DATA_CHANNELS: qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE + " , " + CHANNEL_TABLE);
                    orderBy = CHANNEL_KEY_ORDER_NUMBER + " , " + CHANNEL_KEY_CHANNEL_ID;
                    qb.appendWhere(CHANNEL_TABLE + "." + KEY_ID + "=" + TvBrowserDataBaseHelper.DATA_TABLE + "." + CHANNEL_KEY_CHANNEL_ID);

                    if(projection != null) {
                      for(int i = 0; i < projection.length; i++) {
                        if(projection[i].equals(KEY_ID) || projection[i].equals(CHANNEL_KEY_CHANNEL_ID)) {
                          projection[i] = TvBrowserDataBaseHelper.DATA_TABLE + "." + projection[i]+ " AS " + projection[i];
                        }
                      }
                    }
                    
                    if(selectionArgs != null) {
                      for(int i = 0; i < selectionArgs.length; i++) {
                        if(selectionArgs[i].equals(KEY_ID)) {
                          selectionArgs[i] = TvBrowserDataBaseHelper.DATA_TABLE + "." + selectionArgs[i];
                        }
                      }
                    }
                    
                    if(selection != null && selection.contains(KEY_ID) && !selection.contains("."+KEY_ID)) {
                      selection = selection.replace(KEY_ID, TvBrowserDataBaseHelper.DATA_TABLE + "."+KEY_ID);
                    }
                    if(selection != null && selection.contains(CHANNEL_KEY_CHANNEL_ID) && !selection.contains("."+CHANNEL_KEY_CHANNEL_ID)) {
                      selection = selection.replace(CHANNEL_KEY_CHANNEL_ID, TvBrowserDataBaseHelper.DATA_TABLE + "."+CHANNEL_KEY_CHANNEL_ID);
                    }
                    
                    break;

      default: break;
    }
    
    if(sortOrder != null && sortOrder.trim().length() > 0) {
      orderBy = sortOrder;
    }
    
    if(orderBy != null && !orderBy.contains("NOCASE") && !orderBy.contains("COLLATE")) {
      orderBy += " COLLATE NOCASE";
    }
    
    // Apply the query to the underling database.
    Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, orderBy);
    
    // Register the contexts ContentResolver to be notified if the cursor result set changes.
    
    if(INFORM_FOR_CHANGES) {
      c.setNotificationUri(getContext().getContentResolver(), uri);
      
      switch (uriMatcher.match(uri)) {
        case DATA: c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_DATA_WITH_CHANNEL);break;
        case DATA_ID: String segment = uri.getPathSegments().get(1);
          c.setNotificationUri(getContext().getContentResolver(), ContentUris.withAppendedId(CONTENT_URI_DATA_WITH_CHANNEL,Integer.parseInt(segment)));
          break;
      }
    }
    
    return c;
  }
  
  @Override
  public int update(Uri uri, ContentValues values, String where,
      String[] whereArgs) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    int count;
    
    boolean data_with_channel = false;
    
    switch(uriMatcher.match(uri)) {
      case GROUPS: count = database.update(TvBrowserDataBaseHelper.GROUPS_TABLE, values, where, whereArgs);break;
      case GROUP_ID: {String segment = uri.getPathSegments().get(1);
      
      count = database.update(TvBrowserDataBaseHelper.GROUPS_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case CHANNELS: count = database.update(CHANNEL_TABLE, values, where, whereArgs);break;
      case CHANNEL_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.update(CHANNEL_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case DATA_VERSION: count = database.update(TvBrowserDataBaseHelper.VERSION_TABLE, values, where, whereArgs);break;
      case DATA_VERSION_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.update(TvBrowserDataBaseHelper.VERSION_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case DATA_UPDATE: count = database.update(TvBrowserDataBaseHelper.DATA_TABLE, values, where, whereArgs);break;
      case DATA_UPDATE_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.update(TvBrowserDataBaseHelper.DATA_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      case DATA: count = database.update(TvBrowserDataBaseHelper.DATA_TABLE, values, where, whereArgs);break;
      case DATA_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.update(TvBrowserDataBaseHelper.DATA_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
      }break;
      default: throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    
    if(INFORM_FOR_CHANGES) {
      getContext().getContentResolver().notifyChange(uri, null);
      
      if(data_with_channel) {
        getContext().getContentResolver().notifyChange(CONTENT_URI_DATA_WITH_CHANNEL, null);
      }
    }
    
    return count;
  }

  public static final String CHANNEL_TABLE = "channels";
  
  // Helper class for opneing, creating, and managing database version control
  private static class TvBrowserDataBaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tvbrowser.db";
    
    private static final String GROUPS_TABLE = "channelGroups";
    
    private static final String DATA_TABLE = "data";
    private static final String VERSION_TABLE = "dataVersion";
    
    private static final String CREATE_GROUPS_TABLE = "create table " + GROUPS_TABLE + " (" + KEY_ID + " integer primary key autoincrement, "
        + GROUP_KEY_DATA_SERVICE_ID + " TEXT NOT NULL, "
        + GROUP_KEY_GROUP_ID + " TEXT NOT NULL, "
        + GROUP_KEY_GROUP_NAME + " TEXT NOT NULL, "
        + GROUP_KEY_GROUP_PROVIDER + " TEXT NOT NULL, "
        + GROUP_KEY_GROUP_DESCRIPTION + " TEXT NOT NULL, "
        + GROUP_KEY_GROUP_MIRRORS + " TEXT NOT NULL);";
    
    private static final String CREATE_CHANNEL_TABLE = "create table " + CHANNEL_TABLE + " (" + KEY_ID + " integer primary key autoincrement, "
        + GROUP_KEY_GROUP_ID + " INTEGER REFERENCES " + GROUPS_TABLE + "(" + KEY_ID + ") NOT NULL, "
        + CHANNEL_KEY_CHANNEL_ID + " TEXT NOT NULL, "
        + CHANNEL_KEY_BASE_COUNTRY + " TEXT NOT NULL, "
        + CHANNEL_KEY_TIMEZONE + " TEXT NOT NULL, "
        + CHANNEL_KEY_NAME + " TEXT NOT NULL, "
        + CHANNEL_KEY_COPYRIGHT + " TEXT NOT NULL, "
        + CHANNEL_KEY_WEBSITE + " TEXT NOT NULL, "
        + CHANNEL_KEY_LOGO_URL + " TEXT, "
        + CHANNEL_KEY_LOGO + " BLOB, "
        + CHANNEL_KEY_CATEGORY + " INTEGER NOT NULL, "
        + CHANNEL_KEY_FULL_NAME + " TEXT, "
        + CHANNEL_KEY_ALL_COUNTRIES + " TEXT, "
        + CHANNEL_KEY_JOINED_CHANNEL_ID + " TEXT, "
        + CHANNEL_KEY_ORDER_NUMBER + " INTEGER, "
        + CHANNEL_KEY_SELECTION + " INTEGER NOT NULL DEFAULT 0, "
        + CHANNEL_KEY_USER_CHANNEL_NAME + " TEXT, "
        + CHANNEL_KEY_USER_ICON + " BLOB, "
        + CHANNEL_KEY_USER_START_TIME + " INTEGER, "
        + CHANNEL_KEY_USER_END_TIME + " INTEGER);";
    
    private static final String CREATE_DATA_TABLE = "create table " + DATA_TABLE + " (" + KEY_ID + " integer primary key autoincrement, "
        + CHANNEL_KEY_CHANNEL_ID + " INTEGER REFERENCES " + CHANNEL_TABLE + "(" + KEY_ID + ") NOT NULL, "
        + DATA_KEY_STARTTIME + " INTEGER NOT NULL, "
        + DATA_KEY_ENDTIME + " INTEGER NOT NULL, "
        + DATA_KEY_TITLE + " TEXT NOT NULL, "
        + DATA_KEY_SHORT_DESCRIPTION + " TEXT, "
        + DATA_KEY_DESCRIPTION + " TEXT, "
        + DATA_KEY_TITLE_ORIGINAL + " TEXT, "
        + DATA_KEY_EPISODE_TITLE + " TEXT, "
        + DATA_KEY_EPISODE_TITLE_ORIGINAL + " TEXT, "
        + DATA_KEY_ACTORS + " TEXT, "
        + DATA_KEY_REGIE + " TEXT, "
        + DATA_KEY_CUSTOM_INFO + " TEXT, "
        + DATA_KEY_CATEGORIES + " INTEGER, "
        + DATA_KEY_AGE_LIMIT + " INTEGER, "
        + DATA_KEY_WEBSITE_LINK+ " TEXT, "
        + DATA_KEY_GENRE + " TEXT, "
        + DATA_KEY_ORIGIN + " TEXT, "
        + DATA_KEY_NETTO_PLAY_TIME + " INTEGER, "
        + DATA_KEY_VPS + " INTEGER, "
        + DATA_KEY_SCRIPT + " TEXT, "
        + DATA_KEY_REPETITION_FROM + " TEXT, "
        + DATA_KEY_REPETITION_ON + " TEXT, "
        + DATA_KEY_MUSIC + " TEXT, "
        + DATA_KEY_MODERATION + " TEXT, "
        + DATA_KEY_YEAR + " INTEGER, "
        + DATA_KEY_PICTURE + " BLOB, "
        + DATA_KEY_PICTURE_COPYRIGHT + " TEXT, "
        + DATA_KEY_PICTURE_DESCRIPTION + " TEXT, "
        + DATA_KEY_EPISODE_NUMBER + " INTEGER, "
        + DATA_KEY_EPISODE_COUNT + " INTEGER, "
        + DATA_KEY_SEASON_NUMBER + " INTEGER, "
        + DATA_KEY_PRODUCER + " TEXT, "
        + DATA_KEY_CAMERA + " TEXT, "
        + DATA_KEY_CUT + " TEXT, "
        + DATA_KEY_OTHER_PERSONS + " TEXT, "
        + DATA_KEY_RATING + " INTEGER, "
        + DATA_KEY_PRODUCTION_FIRM + " TEXT, "
        + DATA_KEY_AGE_LIMIT_STRING+ " TEXT, "
        + DATA_KEY_LAST_PRODUCTION_YEAR + " INTEGER, "
        + DATA_KEY_ADDITIONAL_INFO + " TEXT, "
        + DATA_KEY_SERIES + " TEXT, "
        + DATA_KEY_UNIX_DATE + " INTEGER, "
        + DATA_KEY_DATE_PROG_ID + " INTEGER, "
        + DATA_KEY_DONT_WANT_TO_SEE + " INTEGER DEFAULT 0, "
        + DATA_KEY_REMOVED_REMINDER	+ " INTEGER DEFAULT 0, "
        + DATA_KEY_MARKING_MARKING + " INTEGER DEFAULT 0, "
        + DATA_KEY_MARKING_FAVORITE + " INTEGER DEFAULT 0, "
        + DATA_KEY_MARKING_FAVORITE_REMINDER + " INTEGER DEFAULT 0, "
        + DATA_KEY_MARKING_REMINDER + " INTEGER DEFAULT 0, "
        + DATA_KEY_MARKING_CALENDAR + " INTEGER DEFAULT 0, "
        + DATA_KEY_MARKING_SYNC + " INTEGER DEFAULT 0"
        + ");";
    
    private static final String CREATE_VERSION_TABLE = "create table " + VERSION_TABLE + " (" + KEY_ID + " integer primary key autoincrement, "
        + CHANNEL_KEY_CHANNEL_ID + " INTEGER REFERENCES " + CHANNEL_TABLE + "(" + KEY_ID + ") NOT NULL, "
        + VERSION_KEY_DAYS_SINCE_1970 + " INTEGER NOT NULL, "
        + VERSION_KEY_BASE_VERSION + " INTEGER, "
        + VERSION_KEY_MORE0016_VERSION + " INTEGER, "
        + VERSION_KEY_MORE1600_VERSION + " INTEGER, "
        + VERSION_KEY_PICTURE0016_VERSION + " INTEGER, "
        + VERSION_KEY_PICTURE1600_VERSION + " INTEGER);";
    
    public TvBrowserDataBaseHelper(Context context, String name,
        CursorFactory factory, int version) {
      super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(CREATE_GROUPS_TABLE);
      db.execSQL(CREATE_CHANNEL_TABLE);
      db.execSQL(CREATE_DATA_TABLE);
      db.execSQL(CREATE_VERSION_TABLE);
    }

    private static final int DATABASE_VERSION = 6;
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.d("info4", "oldVersion " + oldVersion + " newVersion " + newVersion);
      if(oldVersion == 1 && newVersion > 1) {
        boolean logoFound = false;
        
        Cursor c = db.rawQuery("PRAGMA table_info(" + CHANNEL_TABLE + ")", null);
        
        while(c.moveToNext()) {
          if(c.getString(c.getColumnIndex("name")).equals(CHANNEL_KEY_LOGO)) {
            logoFound = true;
            break;
          }
        }
        
        IOUtils.closeSafely(c);
      
        if(!logoFound) {
          db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
          db.execSQL("DROP TABLE IF EXISTS " + CHANNEL_TABLE);
          db.execSQL("DROP TABLE IF EXISTS " + GROUPS_TABLE);
          db.execSQL("DROP TABLE IF EXISTS " + VERSION_TABLE);
        
          onCreate(db);
        }
      }
      
      if(oldVersion < 4) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + DATA_TABLE + ")", null);
        
        boolean dontWantColumnFound = false;
        
        while(c.moveToNext()) {
          if(c.getString(c.getColumnIndex("name")).equals(DATA_KEY_DONT_WANT_TO_SEE)) {
            dontWantColumnFound = true;
            break;
          }
        }
        
        if(!dontWantColumnFound) {
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_DONT_WANT_TO_SEE + " INTEGER DEFAULT 0");
        }
      }
      
      if(oldVersion < 5) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + DATA_TABLE + ")", null);
        
        boolean removedReminderColumnFound = false;
        
        while(c.moveToNext()) {
          if(c.getString(c.getColumnIndex("name")).equals(DATA_KEY_REMOVED_REMINDER)) {
            removedReminderColumnFound = true;
            break;
          }
        }
        
        if(!removedReminderColumnFound) {
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_REMOVED_REMINDER + " INTEGER DEFAULT 0");
        }
      }
      
      if(oldVersion < 6) {
        final String DATA_KEY_MARKING_VALUES = "markingValues";
        
        Cursor c = db.rawQuery("PRAGMA table_info(" + DATA_TABLE + ")", null);
        
        boolean oldMarkingColumnFound = false;
        
        while(c.moveToNext()) {
          if(c.getString(c.getColumnIndex("name")).equals(DATA_KEY_MARKING_VALUES)) {
            oldMarkingColumnFound = true;
            break;
          }
        }
        
        if(oldMarkingColumnFound) {
          Log.d("info4", "OLD MARKING COLUMN FOUND");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_MARKING_MARKING + " INTEGER DEFAULT 0");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_MARKING_FAVORITE + " INTEGER DEFAULT 0");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_MARKING_FAVORITE_REMINDER + " INTEGER DEFAULT 0");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_MARKING_REMINDER + " INTEGER DEFAULT 0");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_MARKING_CALENDAR + " INTEGER DEFAULT 0");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " ADD COLUMN " + DATA_KEY_MARKING_SYNC + " INTEGER DEFAULT 0");
          
          Cursor markings = db.query(DATA_TABLE, new String[] {KEY_ID,DATA_KEY_MARKING_VALUES}, "ifnull("+DATA_KEY_MARKING_VALUES+", '') != ''", null, null, null, KEY_ID);
          
          if(markings.getCount() > 0) {
            markings.moveToFirst();
            
            ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
            
            do {
              long id = markings.getLong(0);
              String markingValue = markings.getString(1);
              
              ContentValues values = new ContentValues();
              
              final String MARK_VALUE = "marked";
              final String MARK_VALUE_FAVORITE = "favorite";
              final String MARK_VALUE_CALENDAR = "calendar";
              final String MARK_VALUE_SYNC_FAVORITE = "syncfav";
              final String MARK_VALUE_REMINDER = "reminder";
              
              if(markingValue.contains(MARK_VALUE)) {
                values.put(DATA_KEY_MARKING_MARKING, true);
              }
              if(markingValue.contains(MARK_VALUE_FAVORITE)) {
                values.put(DATA_KEY_MARKING_FAVORITE, true);
              }
              if(markingValue.contains(MARK_VALUE_REMINDER)) {
                if(markingValue.contains(MARK_VALUE_FAVORITE)) {
                  values.put(DATA_KEY_MARKING_FAVORITE_REMINDER, true);
                }
                else {
                  values.put(DATA_KEY_MARKING_REMINDER, true);
                }
              }
              if(markingValue.contains(MARK_VALUE_CALENDAR)) {
                values.put(DATA_KEY_MARKING_CALENDAR, true);
              }
              if(markingValue.contains(MARK_VALUE_SYNC_FAVORITE)) {
                values.put(DATA_KEY_MARKING_SYNC, true);
              }
              
              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id));
              opBuilder.withValues(values);
              
              updateValuesList.add(opBuilder.build());
            }while(markings.moveToNext());
            
            if(!updateValuesList.isEmpty()) {
              db.beginTransaction();
              
              for(ContentProviderOperation op : updateValuesList) {
                Uri uri = op.getUri();
                ContentValues values = op.resolveValueBackReferences(null, 0);
                
                String segment = uri.getPathSegments().get(1);
                
                db.update(TvBrowserDataBaseHelper.DATA_TABLE, values, KEY_ID + "=" + segment, null);
              }
              
              db.setTransactionSuccessful();
              db.endTransaction();
            }
          }
          
          Log.d("info4", "ALTER TABLE " + DATA_TABLE + " RENAME TO " + DATA_TABLE + "_old");
          db.execSQL("ALTER TABLE " + DATA_TABLE + " RENAME TO " + DATA_TABLE + "_old");
          Log.d("info4", CREATE_DATA_TABLE);
          db.execSQL(CREATE_DATA_TABLE);
          
          StringBuilder columnsSeparated = new StringBuilder();
          
          columnsSeparated.append(KEY_ID).append(",");
          columnsSeparated.append(CHANNEL_KEY_CHANNEL_ID).append(",");
          columnsSeparated.append(DATA_KEY_STARTTIME).append(",");
          columnsSeparated.append(DATA_KEY_ENDTIME).append(",");
          columnsSeparated.append(DATA_KEY_TITLE).append(",");
          columnsSeparated.append(DATA_KEY_TITLE_ORIGINAL).append(",");
          columnsSeparated.append(DATA_KEY_EPISODE_TITLE).append(",");
          columnsSeparated.append(DATA_KEY_EPISODE_TITLE_ORIGINAL).append(",");
          columnsSeparated.append(DATA_KEY_SHORT_DESCRIPTION).append(",");
          columnsSeparated.append(DATA_KEY_DESCRIPTION).append(",");
          columnsSeparated.append(DATA_KEY_ACTORS).append(",");
          columnsSeparated.append(DATA_KEY_REGIE).append(",");
          columnsSeparated.append(DATA_KEY_CUSTOM_INFO).append(",");
          columnsSeparated.append(DATA_KEY_CATEGORIES).append(",");
          columnsSeparated.append(DATA_KEY_AGE_LIMIT).append(",");
          columnsSeparated.append(DATA_KEY_WEBSITE_LINK).append(",");
          columnsSeparated.append(DATA_KEY_GENRE).append(",");
          columnsSeparated.append(DATA_KEY_ORIGIN).append(",");
          columnsSeparated.append(DATA_KEY_NETTO_PLAY_TIME).append(",");
          columnsSeparated.append(DATA_KEY_VPS).append(",");
          columnsSeparated.append(DATA_KEY_SCRIPT).append(",");
          columnsSeparated.append(DATA_KEY_REPETITION_FROM).append(",");
          columnsSeparated.append(DATA_KEY_MUSIC).append(",");
          columnsSeparated.append(DATA_KEY_MODERATION).append(",");
          columnsSeparated.append(DATA_KEY_YEAR).append(",");
          columnsSeparated.append(DATA_KEY_REPETITION_ON).append(",");
          columnsSeparated.append(DATA_KEY_PICTURE).append(",");
          columnsSeparated.append(DATA_KEY_PICTURE_COPYRIGHT).append(",");
          columnsSeparated.append(DATA_KEY_PICTURE_DESCRIPTION).append(",");
          columnsSeparated.append(DATA_KEY_EPISODE_NUMBER).append(",");
          columnsSeparated.append(DATA_KEY_EPISODE_COUNT).append(",");
          columnsSeparated.append(DATA_KEY_SEASON_NUMBER).append(",");
          columnsSeparated.append(DATA_KEY_PRODUCER).append(",");
          columnsSeparated.append(DATA_KEY_CAMERA).append(",");
          columnsSeparated.append(DATA_KEY_CUT).append(",");
          columnsSeparated.append(DATA_KEY_OTHER_PERSONS).append(",");
          columnsSeparated.append(DATA_KEY_RATING).append(",");
          columnsSeparated.append(DATA_KEY_PRODUCTION_FIRM).append(",");
          columnsSeparated.append(DATA_KEY_AGE_LIMIT_STRING).append(",");
          columnsSeparated.append(DATA_KEY_LAST_PRODUCTION_YEAR).append(",");
          columnsSeparated.append(DATA_KEY_ADDITIONAL_INFO).append(",");
          columnsSeparated.append(DATA_KEY_SERIES).append(",");
          columnsSeparated.append(DATA_KEY_UNIX_DATE).append(",");
          columnsSeparated.append(DATA_KEY_DATE_PROG_ID).append(",");
          columnsSeparated.append(DATA_KEY_DONT_WANT_TO_SEE).append(",");
          columnsSeparated.append(DATA_KEY_REMOVED_REMINDER).append(",");
          columnsSeparated.append(DATA_KEY_MARKING_MARKING).append(",");
          columnsSeparated.append(DATA_KEY_MARKING_FAVORITE).append(",");
          columnsSeparated.append(DATA_KEY_MARKING_FAVORITE_REMINDER).append(",");
          columnsSeparated.append(DATA_KEY_MARKING_REMINDER).append(",");
          columnsSeparated.append(DATA_KEY_MARKING_CALENDAR).append(",");
          columnsSeparated.append(DATA_KEY_MARKING_SYNC);
          
          db.beginTransaction();
          Log.d("info4", "INSERT INTO " + DATA_TABLE + "(" + columnsSeparated + ") SELECT "
              + columnsSeparated + " FROM " + DATA_TABLE + "_old;");
          db.execSQL("INSERT INTO " + DATA_TABLE + "(" + columnsSeparated + ") SELECT "
              + columnsSeparated + " FROM " + DATA_TABLE + "_old;");
          
          db.setTransactionSuccessful();
          db.endTransaction();
          
          Log.d("info4","DROP TABLE " + DATA_TABLE + "_old;");
          db.execSQL("DROP TABLE " + DATA_TABLE + "_old;");
        }
      }
    }
  }
}
