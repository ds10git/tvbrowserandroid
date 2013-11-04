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

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class TvBrowserSearchResults extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
  private SimpleCursorAdapter adapter;

  private static String QUERY_EXTRA_KEY = "QUERY_EXTRA_KEY";
  private static String QUERY_EXTRA_ID_KEY = "QUERY_EXTRA_ID_KEY";
  public static String QUERY_EXTRA_EPISODE_KEY = "QUERY_EXTRA_EPISODE_KEY";
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
        TvBrowserContentProvider.DATA_KEY_CATEGORIES
    };
    
    registerForContextMenu(getListView());
    
    // Create a new Adapter an bind it to the List View
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(this);
    adapter = new SimpleCursorAdapter(this,/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0);
    adapter.setViewBinder(mViewAndClickHandler);
    
    setListAdapter(adapter);
    
    // Initiate the Cursor Loader
    getLoaderManager().initLoader(0, null, this);
    
    // Get the launch Intent
    parseIntent(getIntent());
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    long programID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    
    UiUtils.createContextMenu(TvBrowserSearchResults.this, menu, programID);
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    parseIntent(getIntent());
  }
  
  private void parseIntent(Intent intent) {
    // If the Activity was started to service a Search request, extract the search query.
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String searchQuery = intent.getStringExtra(SearchManager.QUERY);
      
      // Perfom the search, passing in the search query as an argument to the Cursor Loader
      Bundle args = new Bundle();
      args.putString(QUERY_EXTRA_KEY, searchQuery);
      
      if(intent.hasExtra(QUERY_EXTRA_EPISODE_KEY)) {
        String episodeQuery = intent.getStringExtra(QUERY_EXTRA_EPISODE_KEY);
        
        args.putString(QUERY_EXTRA_EPISODE_KEY, episodeQuery);
      }
      
      // Restart the Cursor Loader to execute the new query
      getLoaderManager().restartLoader(0, args, this);
    }
    else if(Intent.ACTION_VIEW.equals(intent.getAction())) {
      try {
        long key = Long.parseLong(intent.getData().getPathSegments().get(1));
        
        Bundle args = new Bundle();
        args.putLong(QUERY_EXTRA_ID_KEY, key);
        
        getLoaderManager().restartLoader(0, args, this);
      }catch(NumberFormatException e) {
        // Ignore
      }
    }
    else if(intent.hasExtra(SearchManager.QUERY)) {
      Bundle args = new Bundle();
      args.putString(QUERY_EXTRA_KEY, intent.getStringExtra(SearchManager.QUERY));
      
      if(intent.hasExtra(QUERY_EXTRA_EPISODE_KEY)) {
        String episodeQuery = intent.getStringExtra(QUERY_EXTRA_EPISODE_KEY);
        
        args.putString(QUERY_EXTRA_EPISODE_KEY, episodeQuery);
      }
      
      // Restart the Cursor Loader to execute the new query
      getLoaderManager().restartLoader(0, args, this);
    }
  }
  
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String query = "0";
    String episodeQuery = "0";
    String operation = " OR ";
    
    long ID = -1;
    
    if(args != null) {
      // Extract the search query from the arguments.
      if(args.containsKey(QUERY_EXTRA_KEY)) {
        episodeQuery = query = args.getString(QUERY_EXTRA_KEY);
      }
      
      ID = args.getLong(QUERY_EXTRA_ID_KEY, -1);
            
      if(args.containsKey(QUERY_EXTRA_EPISODE_KEY)) {
        episodeQuery = args.getString(QUERY_EXTRA_EPISODE_KEY);
        operation = " AND ";
      }
    }
    
    
    // Construct the new query in form of a Cursor Loader
    String[] projection = null;
    
    if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false)) {
      projection = new String[15];
      
      projection[14] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[14];
    }
    
    String titleEscape = query.contains("'") ? "\"" : "'";
    String episodeEscape = episodeQuery.contains("'") ? "\"" : "'";
    
    projection[0] = TvBrowserContentProvider.KEY_ID;
    projection[1] = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
    projection[2] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    projection[3] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
    projection[4] = TvBrowserContentProvider.DATA_KEY_TITLE;
    projection[5] = TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION;
    projection[6] = TvBrowserContentProvider.DATA_KEY_MARKING_VALUES;
    projection[7] = TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER;
    projection[8] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
    projection[9] = TvBrowserContentProvider.DATA_KEY_GENRE;
    projection[10] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
    projection[11] = TvBrowserContentProvider.DATA_KEY_UNIX_DATE;
    projection[12] = TvBrowserContentProvider.CHANNEL_KEY_NAME;
    projection[13] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
        
    String where = "(" + TvBrowserContentProvider.DATA_KEY_TITLE + " LIKE " + titleEscape + "%" + query + "%" + titleEscape + operation + TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE + " LIKE " + episodeEscape + "%" + episodeQuery + "%" + episodeEscape + ") AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " >= " + System.currentTimeMillis();
    String[] whereArgs = null;
    String sortOrder = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    
    Uri uri = TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL;
    
    if(ID != -1) {
      where = null;
      uri = ContentUris.withAppendedId(uri, ID);
    }
    
    // Create the new Cursor loader
    return new CursorLoader(this, uri, projection, where, whereArgs, sortOrder);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    adapter.swapCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    adapter.swapCursor(null);
  }


  @Override
  public boolean onContextItemSelected(MenuItem item) {
    return mViewAndClickHandler.onContextItemSelected(item);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    mViewAndClickHandler.onListItemClick(l, v, position, id);
  }
}
