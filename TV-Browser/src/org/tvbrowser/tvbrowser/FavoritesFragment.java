package org.tvbrowser.tvbrowser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String FAVORITE_LIST = "FAVORITE_LIST";
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  private SimpleCursorAdapter adapter;
  private ArrayAdapter<Favorite> mFavoriteAdapter;
  private ArrayList<Favorite> mFavoriteList;
  
  private String mWhereClause;
  
  private Handler handler;
  
  private boolean mFavoriteContext;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.favorite_fragment_layout, container, false);
    
    return v;
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    handler = new Handler();
    
    mWhereClause = null;
    
    mFavoriteList = new ArrayList<FavoritesFragment.Favorite>();
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    Set<String> favoritesSet = prefs.getStringSet(FAVORITE_LIST, new HashSet<String>());
        
    for(String favorite : favoritesSet) {
      String[] values = favorite.split(";;");
      
      mFavoriteList.add(new Favorite(values[0], values[1], Boolean.valueOf(values[2])));
    }
        
    mFavoriteAdapter = new ArrayAdapter<Favorite>(getActivity(), android.R.layout.simple_list_item_activated_1,mFavoriteList);
    
    ListView favorites = (ListView)getView().findViewById(R.id.favorite_list);
    favorites.setAdapter(mFavoriteAdapter);
    favorites.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    favorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View v, int position,
          long id) {
        Favorite fav = mFavoriteList.get(position);
        mWhereClause = fav.getWhereClause();
        
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached()) {
              getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
            }
          }
        });
      }
    });
        
    ListView favoriteProgramList = (ListView)getView().findViewById(R.id.favorite_program_list);
    
    registerForContextMenu(favoriteProgramList);
    
    favoriteProgramList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View v, int position,
          long id) {
        mViewAndClickHandler.onListItemClick(null, v, position, id);
      }
    });
    
    registerForContextMenu(favorites);
    
    Button add = (Button)getView().findViewById(R.id.add_favorite);
    add.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        editFavorite(null);
      }
    });
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity());
    
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl},0);
    adapter.setViewBinder(mViewAndClickHandler);
        
    favoriteProgramList.setAdapter(adapter);
  }
  
  @Override
  public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis() + " ) ";
    
    if(mWhereClause != null) {
      where += mWhereClause;
    }
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    return loader;
  }

  private void updateFavorites() {
    HashSet<String> favoriteSet = new HashSet<String>();
    
    for(Favorite fav : mFavoriteList) {
      favoriteSet.add(fav.getSaveString());
    }
    
    Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
    
    edit.putStringSet(FAVORITE_LIST, favoriteSet);
    
    edit.commit();
    
    mFavoriteAdapter.notifyDataSetChanged();
  }
  
  private void editFavorite(final Favorite fav) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    
    final View input = getActivity().getLayoutInflater().inflate(R.layout.add_favorite_layout, null);
    
    if(fav != null) {
      ((EditText)input.findViewById(R.id.favorite_name)).setText(fav.getName());
      ((EditText)input.findViewById(R.id.favorite_search_value)).setText(fav.getSearchValue());
      ((CheckBox)input.findViewById(R.id.favorite_only_title)).setChecked(fav.searchOnlyTitle());
    }
    
    builder.setView(input);
    
    builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        String name = ((EditText)input.findViewById(R.id.favorite_name)).getText().toString();
        String search = ((EditText)input.findViewById(R.id.favorite_search_value)).getText().toString();
        boolean onlyTitle = ((CheckBox)input.findViewById(R.id.favorite_only_title)).isChecked();
        
        if(name == null || name.trim().length() == 0) {
          name = search;
        }
        
        if(search != null) {
          if(fav == null) {
            mFavoriteList.add(new Favorite(name, search, onlyTitle));
            mFavoriteAdapter.notifyDataSetChanged();
          }
          else {
            fav.setValues(name, search, onlyTitle);
          }
        }
        
        updateFavorites();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    
    AlertDialog dialog = builder.create();
    dialog.show();
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    if(v.getId() == R.id.favorite_list) {
      mFavoriteContext = true;
      getActivity().getMenuInflater().inflate(R.menu.favorite_context, menu);
    }
    else {
      mViewAndClickHandler.onCreateContextMenu(menu, v, menuInfo);
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(mFavoriteContext) {
      int pos = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
      
      if(item.getItemId() == R.id.delete_favorite) {
        mFavoriteList.remove(pos);
        updateFavorites();
      }
      else if(item.getItemId() == R.id.edit_favorite) {
        editFavorite(mFavoriteList.get(pos));
      }
      
      mFavoriteContext = false;
      return true;
    }
    else {
      mFavoriteContext = false;
      return mViewAndClickHandler.onContextItemSelected(item);
    }
  }
  
  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    adapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    adapter.swapCursor(null);
  }
  
  private static class Favorite {
    private String mName;
    private String mSearch;
    private boolean mOnlyTitle;
    
    public Favorite(String name, String search, boolean onlyTitle) {
      mName = name;
      mSearch = search;
      mOnlyTitle = onlyTitle;
    }
    
    public boolean searchOnlyTitle() {
      return mOnlyTitle;
    }
    
    public String getName() {
      return mName;
    }
    
    public String getSearchValue() {
      return mSearch;
    }
    
    public void setValues(String name, String search, boolean onlyTitle) {
      mName = name;
      mSearch = search;
      mOnlyTitle = onlyTitle;
    }
    
    public String toString() {
      return mName;
    }
    
    public String getWhereClause() {
      StringBuilder builder = new StringBuilder();
      
      builder.append(" AND ((");
      builder.append(TvBrowserContentProvider.DATA_KEY_TITLE);
      builder.append(" LIKE \"%");
      builder.append(mSearch);
      builder.append("%\")");
      
      if(!mOnlyTitle) {
        String[] values = {TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL};
        
        for(String value : values) {
          builder.append(" OR (");
          builder.append(value);
          builder.append(" LIKE \"%");
          builder.append(mSearch);
          builder.append("%\")");
        }
      }
      
      builder.append(")");
      
      return builder.toString();
    }
    
    public String getSaveString() {
      return mName + ";;" + mSearch + ";;" + String.valueOf(mOnlyTitle);
    }
  }
}
