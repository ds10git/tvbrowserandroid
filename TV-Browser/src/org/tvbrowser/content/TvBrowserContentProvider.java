package org.tvbrowser.content;

import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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
  public static final Uri CONTENT_URI_GROUPS = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/groups");
  public static final Uri CONTENT_URI_CHANNELS = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/channels");
  public static final Uri CONTENT_URI_DATA = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/data");
  public static final Uri CONTENT_URI_DATA_UPDATE = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/dataupdate");
  public static final Uri CONTENT_URI_DATA_WITH_CHANNEL = Uri.parse("content://org.tvbrowser.tvbrowsercontentprovider/datachannels");
    
  public static final String KEY_ID = "_id";
  
  public static boolean INFORM_FOR_CHANGES = true;
  
  private TvBrowserDataBaseHelper mDataBaseHelper;
  
  private static final int GROUPS = 1;
  private static final int GROUP_ID = 2;
  
  private static final int CHANNELS = 10;
  private static final int CHANNEL_ID = 11;

  private static final int DATA = 20;
  private static final int DATA_ID = 21;

  private static final int DATA_CHANNELS = 30;
  private static final int DATA_CHANNEL_ID = 31;
  
  private static final int DATA_UPDATE = 40;
  private static final int DATA_UPDATE_ID = 41;    
  
  private static final int SEARCH = 100;
  
  
  
  private static final HashMap<String,String> SEARCH_PROJECTION_MAP;
  
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
  public static final String CHANNEL_KEY_CATEGORY = "category";
  public static final String CHANNEL_KEY_FULL_NAME = "fullName";
  public static final String CHANNEL_KEY_ALL_COUNTRIES = "allCountries";
  public static final String CHANNEL_KEY_JOINED_CHANNEL_ID = "joinedChannelID";
  public static final String CHANNEL_KEY_ORDER_NUMBER = "orderNumber";
  
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
  public static final String DATA_KEY_MARKING_VALUES = "markingValues";
  
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
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "dataupdate", DATA_UPDATE);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "dataupdate/#", DATA_UPDATE_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "datachannels", DATA_CHANNELS);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", "datachannels/#", DATA_CHANNEL_ID);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_SHORTCUT, SEARCH);
    uriMatcher.addURI("org.tvbrowser.tvbrowsercontentprovider", SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SEARCH);
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
      case CHANNELS: count = database.delete(TvBrowserDataBaseHelper.CHANNEL_TABLE, where, whereArgs);break;
      case CHANNEL_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.delete(TvBrowserDataBaseHelper.CHANNEL_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
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
    }
    
    throw new SQLException("Failed to insert row into " + uri);
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
    long rowID = database.insert(TvBrowserDataBaseHelper.CHANNEL_TABLE, "channel", values);
    
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

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteDatabase database = mDataBaseHelper.getWritableDatabase();
    
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    
    // If no sort order is specified, sort by date / time
    String orderBy = null;
    Log.d("TVB", String.valueOf(uri));
    // If this is a row query, limit the result set to teh pased in row.
    switch(uriMatcher.match(uri)) {
      case SEARCH: qb.appendWhere("(" + DATA_KEY_TITLE + " LIKE \"%" + uri.getPathSegments().get(1) + "%\" OR " + DATA_KEY_EPISODE_TITLE + " LIKE \"%" +  uri.getPathSegments().get(1) + "%\") AND " + DATA_KEY_STARTTIME + " >= " + System.currentTimeMillis());
                   qb.setProjectionMap(SEARCH_PROJECTION_MAP);
                   qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE);
                   orderBy = DATA_KEY_STARTTIME;
                   break;
      case GROUP_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case GROUPS: qb.setTables(TvBrowserDataBaseHelper.GROUPS_TABLE);
                   orderBy = GROUP_KEY_GROUP_ID;break;
      case CHANNEL_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case CHANNELS: qb.setTables(TvBrowserDataBaseHelper.CHANNEL_TABLE);
                    orderBy = CHANNEL_KEY_NAME;break;
      case DATA_UPDATE_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case DATA_UPDATE: qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE);
                    orderBy = CHANNEL_KEY_CHANNEL_ID;break;
      case DATA_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
      case DATA: qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE);
                    orderBy = CHANNEL_KEY_CHANNEL_ID;break;
      case DATA_CHANNEL_ID: qb.appendWhere(TvBrowserDataBaseHelper.DATA_TABLE + "." + KEY_ID + "=" + uri.getPathSegments().get(1) + " AND ");
      case DATA_CHANNELS: qb.setTables(TvBrowserDataBaseHelper.DATA_TABLE + " , " + TvBrowserDataBaseHelper.CHANNEL_TABLE);
                    orderBy = CHANNEL_KEY_ORDER_NUMBER + " , " + CHANNEL_KEY_CHANNEL_ID;
                    qb.appendWhere(TvBrowserDataBaseHelper.CHANNEL_TABLE + "." + KEY_ID + " = " + TvBrowserDataBaseHelper.DATA_TABLE + "." + CHANNEL_KEY_CHANNEL_ID);

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
   /* else if (orderBy == null) {
      orderBy = " COLLATE NOCASE";
    }*/
    
    
    Log.d("TVB", qb.buildQuery(projection, selection, null, null, sortOrder, null));
    // Apply the query to the underling database.
    Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, orderBy);
    
    // Register the contexts ContentResolver to be notified if the cursor result set changes.
    
    if(INFORM_FOR_CHANGES) {
      c.setNotificationUri(getContext().getContentResolver(), uri);
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
      case CHANNELS: count = database.update(TvBrowserDataBaseHelper.CHANNEL_TABLE, values, where, whereArgs);break;
      case CHANNEL_ID: {String segment = uri.getPathSegments().get(1);
      data_with_channel = true;
      count = database.update(TvBrowserDataBaseHelper.CHANNEL_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
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

  
  // Helper class for opneing, creating, and managing database version control
  private static class TvBrowserDataBaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "TvBrowserContentProvider";
    private static final String DATABASE_NAME = "tvbrowser.db";
    private static final int DATABASE_VERSION = 1;
    private static final String GROUPS_TABLE = "channelGroups";
    private static final String CHANNEL_TABLE = "channels";
    private static final String DATA_TABLE = "data";
    
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
        + CHANNEL_KEY_CATEGORY + " INTEGER NOT NULL, "
        + CHANNEL_KEY_FULL_NAME + " TEXT, "
        + CHANNEL_KEY_ALL_COUNTRIES + " TEXT, "
        + CHANNEL_KEY_JOINED_CHANNEL_ID + " TEXT, "
        + CHANNEL_KEY_ORDER_NUMBER + " INTEGER);";
    
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
        + DATA_KEY_MARKING_VALUES + " TEXT);";
    
    public TvBrowserDataBaseHelper(Context context, String name,
        CursorFactory factory, int version) {
      super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(CREATE_GROUPS_TABLE);
      db.execSQL(CREATE_CHANNEL_TABLE);
      db.execSQL(CREATE_DATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + CHANNEL_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + GROUPS_TABLE);
      
      onCreate(db);
    }
  }
}
