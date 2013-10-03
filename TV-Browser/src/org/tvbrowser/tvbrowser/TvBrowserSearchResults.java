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
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    registerForContextMenu(getListView());
    
    // Create a new Adapter an bind it to the List View
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(this);
    adapter = new SimpleCursorAdapter(this,/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl},0);
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
  }
  
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String query = "0";
    
    long ID = -1;
    
    if(args != null) {
      // Extract the search query from the arguments.
      query = args.getString(QUERY_EXTRA_KEY);
      ID = args.getLong(QUERY_EXTRA_ID_KEY, -1);
    }
    
    // Construct the new query in form of a Cursor Loader
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    String where = "(" + TvBrowserContentProvider.DATA_KEY_TITLE + " LIKE \"%" + query + "%\" OR " + TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE + " LIKE \"%" + query + "%\") AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " >= " + System.currentTimeMillis();
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
