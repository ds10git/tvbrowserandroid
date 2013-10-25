package org.tvbrowser.tvbrowser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  private SimpleCursorAdapter adapter;
  private ArrayAdapter<Favorite> mFavoriteAdapter;
  private ArrayList<Favorite> mFavoriteList;
  
  private String mWhereClause;
  
  private Handler handler;
  
  private Thread mUpdateThread;
  
  private boolean mFavoriteContext;
  private BroadcastReceiver mReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDataUpdateReceiver;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.favorite_fragment_layout, container, false);
    
    return v;
  }
  
  @Override
  public void onResume() {
    super.onResume();
    
    handler.post(new Runnable() {
      @Override
      public void run() {
        if(!isDetached() && !isRemoving()) {
          mFavoriteAdapter.notifyDataSetChanged();
        }
      }
    });
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    handler = new Handler();
    
    mWhereClause = null;
    
    mFavoriteList = new ArrayList<Favorite>();
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
        
    for(String favorite : favoritesSet) {
      String[] values = favorite.split(";;");
      
      mFavoriteList.add(new Favorite(values[0], values[1], Boolean.valueOf(values[2])));
    }
        
    mFavoriteAdapter = new ArrayAdapter<Favorite>(getActivity(), android.R.layout.simple_list_item_activated_1,mFavoriteList);
    
    final ListView favorites = (ListView)getView().findViewById(R.id.favorite_list);
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
    
    Button synced = (Button)getView().findViewById(R.id.show_sync_favorites);
    synced.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mWhereClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE \"%" + SettingConstants.MARK_VALUE_SYNC_FAVORITE + "%\" ) ";
        
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached()) {
              favorites.setItemChecked(-1, true);
              getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
            }
          }
        });
      }
    });
    
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
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity());
    
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0);
    adapter.setViewBinder(mViewAndClickHandler);
        
    favoriteProgramList.setAdapter(adapter);
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
        
    mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, final Intent intent) {
        if(mUpdateThread == null || !mUpdateThread.isAlive()) {
          mUpdateThread = new Thread() {
            public void run() {
              String oldName = intent.getStringExtra(Favorite.OLD_NAME_KEY);
              
              Favorite fav = null;
              
              if(oldName != null) {
                for(Favorite favorite : mFavoriteList) {
                  if(favorite.getName().equals(oldName)) {
                    fav = favorite;
                    break;
                  }
                }
              }
              
              if(fav == null) {
                fav = new Favorite(intent.getStringExtra(Favorite.NAME_KEY), intent.getStringExtra(Favorite.SEARCH_KEY), intent.getBooleanExtra(Favorite.ONLY_TITLE_KEY, true));
                mFavoriteList.add(fav);
              }
              else {
                fav.setValues(intent.getStringExtra(Favorite.NAME_KEY), intent.getStringExtra(Favorite.SEARCH_KEY), intent.getBooleanExtra(Favorite.ONLY_TITLE_KEY, true));
              }
              
              handler.post(new Runnable() {
                @Override
                public void run() {
                  mFavoriteAdapter.notifyDataSetChanged();
                }
              });
            }
          };
          mUpdateThread.start();
        }
      }
    };
    
    mDataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && !isRemoving()) {
              mFavoriteAdapter.notifyDataSetChanged();
            }
          }
        });
      }
    };
    
    mRefreshReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && !isRemoving()) {
              getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
            }
          }
        });
      }
    };
    
    IntentFilter dataUpdateFilter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, dataUpdateFilter);
    
    IntentFilter filter = new IntentFilter(SettingConstants.FAVORITES_CHANGED);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    if(mReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }
    if(mDataUpdateReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
  }
  
  @Override
  public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] projection = null;
    
    if(PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false)) {
      projection = new String[15];
      
      projection[14] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[14];
    }
    
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
    
    edit.putStringSet(SettingConstants.FAVORITE_LIST, favoriteSet);
    
    edit.commit();
    
    mFavoriteAdapter.notifyDataSetChanged();
  }
  
  private void editFavorite(final Favorite fav) {
    UiUtils.editFavorite(fav, getActivity(), null);    
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
        final Favorite fav = mFavoriteList.remove(pos);
        
        new Thread() {
          public void run() {
            Favorite.removeFavoriteMarking(getActivity().getApplicationContext(), getActivity().getContentResolver(), fav);
          }
        }.start();
        
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
     // return mViewAndClickHandler.onContextItemSelected(item);
    }
    
    return false;
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
}
