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
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.view.SeparatorDrawable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Build;
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
import android.widget.TextView;

public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener {
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  private SimpleCursorAdapter mProgramListAdapter;
  private ArrayAdapter<Favorite> mFavoriteAdapter;
  private ArrayAdapter<String> mMarkingsAdapter;
  private ArrayList<Favorite> mFavoriteList;
  
  private ListView mFavoriteProgramList;
  
  private String mWhereClause;
  
  private Handler handler;
  
  private Thread mUpdateThread;
  
  private BroadcastReceiver mReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  
  private boolean mIsRunning;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.favorite_fragment_layout, container, false);
    
    return v;
  }
  
  public void updateSynchroButton(View view) {
    if(view == null) {
      view = getView();
    }
    
    if(mMarkingsAdapter != null) {
      if(PrefUtils.getBooleanValue(R.string.PREF_SYNC_FAV_FROM_DESKTOP, R.bool.pref_sync_fav_from_desktop_default) && getActivity().getSharedPreferences("transportation", Context.MODE_PRIVATE).getString(SettingConstants.USER_NAME, "").trim().length() > 0) {
        if(mMarkingsAdapter.getCount() < 3) {
          mMarkingsAdapter.add(getResources().getString(R.string.marking_value_sync));
        }
      }
      else {
        mMarkingsAdapter.remove(getResources().getString(R.string.marking_value_sync));
      }
      
      mMarkingsAdapter.notifyDataSetChanged();
    }
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
      
      boolean remind = false;
      
      if(values.length > 3) {
        remind = Boolean.valueOf(values[3]);
      }
      
      mFavoriteList.add(new Favorite(values[0], values[1], Boolean.valueOf(values[2]), remind));
    }
        
    mFavoriteAdapter = new ArrayAdapter<Favorite>(getActivity(), android.R.layout.simple_list_item_activated_1,mFavoriteList);
    
    final ListView markings = (ListView)getView().findViewById(R.id.select_marking_list);
    final ListView favorites = (ListView)getView().findViewById(R.id.favorite_list);
    favorites.setAdapter(mFavoriteAdapter);
    favorites.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    favorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View v, int position,
          long id) {
        markings.setItemChecked(-1, true);
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
    
    
    ArrayList<String> markingList = new ArrayList<String>();
    
    markingList.add(getResources().getString(R.string.marking_value_marked));
    
    if(Build.VERSION.SDK_INT >= 14) {
      markingList.add(getResources().getString(R.string.marking_value_reminder) + "/" + getResources().getString(R.string.marking_value_calendar));
    }
    else {
      markingList.add(getResources().getString(R.string.marking_value_reminder));
    }
    
    mMarkingsAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, markingList) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
          convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_activated_1, null);
        }
        
        ((TextView)convertView).setText(getItem(position));
        
        switch (position) {
          case 0: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_COLOR_KEY, getContext()));break;
          case 1: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_REMINDER_COLOR_KEY, getContext()));break;
          case 2: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_SYNC_COLOR_KEY, getContext()));break;
        }
        
        return convertView;
      }
    };
    
    updateSynchroButton(null);
    markings.setAdapter(mMarkingsAdapter);
    markings.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    markings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View v, int position,
          long id) {
        favorites.setItemChecked(-1, true);
        
        mWhereClause = TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " ";
        
        switch (position) {
          case 0: mWhereClause += TvBrowserContentProvider.DATA_KEY_MARKING_MARKING; break;
          case 1: 
            if(Build.VERSION.SDK_INT >= 14) {
              mWhereClause += " ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER  + " ) ) ";
            }
            else {
              mWhereClause +=  " ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER  + " ) ) ";
            }
            break;
          case 2: mWhereClause += TvBrowserContentProvider.DATA_KEY_MARKING_SYNC; break;
          
          default: mWhereClause += TvBrowserContentProvider.DATA_KEY_STARTTIME + "=0 ";
        }
        
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
    
    mFavoriteProgramList = (ListView)getView().findViewById(R.id.favorite_program_list);
        
    mFavoriteProgramList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
        TvBrowserContentProvider.DATA_KEY_CATEGORIES
    };
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity());
    
    // Create a new Adapter an bind it to the List View
    mProgramListAdapter = new OrientationHandlingCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_lists_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0, true);
    mProgramListAdapter.setViewBinder(mViewAndClickHandler);
        
    mFavoriteProgramList.setAdapter(mProgramListAdapter);
    
    SeparatorDrawable drawable = new SeparatorDrawable(getActivity());
    
    mFavoriteProgramList.setDivider(drawable);
    
    prefs.registerOnSharedPreferenceChangeListener(this);
    
    setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.devider_size_default));
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
                fav = new Favorite(intent.getStringExtra(Favorite.NAME_KEY), intent.getStringExtra(Favorite.SEARCH_KEY), intent.getBooleanExtra(Favorite.ONLY_TITLE_KEY, true), intent.getBooleanExtra(Favorite.REMIND_KEY, true));
                mFavoriteList.add(fav);
              }
              else {
                fav.setValues(intent.getStringExtra(Favorite.NAME_KEY), intent.getStringExtra(Favorite.SEARCH_KEY), intent.getBooleanExtra(Favorite.ONLY_TITLE_KEY, true), intent.getBooleanExtra(Favorite.REMIND_KEY, true));
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
    
    mDontWantToSeeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(!isDetached() && !isRemoving()) {
          mFavoriteAdapter.notifyDataSetChanged();
        }
      }
    };
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDontWantToSeeReceiver, new IntentFilter(SettingConstants.DONT_WANT_TO_SEE_CHANGED));
    
    mRefreshReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && !isRemoving() && mIsRunning) {
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
    
    mIsRunning = true;
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    mIsRunning = false;
    
    if(mReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }
    if(mDataUpdateReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
    if(mDontWantToSeeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDontWantToSeeReceiver);
    }
  }
  
  @Override
  public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] projection = null;
    
    if(PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_LISTS, R.bool.show_pictures_in_lists_default)) {
      projection = new String[14 + TvBrowserContentProvider.MARKING_COLUMNS.length];
      
      projection[projection.length-1] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[13 + TvBrowserContentProvider.MARKING_COLUMNS.length];
    }
    
    projection[0] = TvBrowserContentProvider.KEY_ID;
    projection[1] = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
    projection[2] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    projection[3] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
    projection[4] = TvBrowserContentProvider.DATA_KEY_TITLE;
    projection[5] = TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION;
    projection[6] = TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER;
    projection[7] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
    projection[8] = TvBrowserContentProvider.DATA_KEY_GENRE;
    projection[9] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
    projection[10] = TvBrowserContentProvider.DATA_KEY_UNIX_DATE;
    projection[11] = TvBrowserContentProvider.CHANNEL_KEY_NAME;
    projection[12] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    
    int startIndex = 13;

    for(int i = startIndex ; i < (startIndex + TvBrowserContentProvider.MARKING_COLUMNS.length); i++) {
      projection[i] = TvBrowserContentProvider.MARKING_COLUMNS[i-startIndex];
    }

    String where = mWhereClause;
    
    if(where == null) {
      where = " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=0 ";
    }
    
    if(where.trim().length() > 0) {
      where += " AND ";
    }
    
    where += " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
    where += " AND ( NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE + " ) ";
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
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
    getActivity().getMenuInflater().inflate(R.menu.favorite_context, menu);
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(item.getMenuInfo() != null) {
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
      
      return true;
    }
    
    return false;
  }
  
  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    mProgramListAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    mProgramListAdapter.swapCursor(null);
  }
  

  private void setDividerSize(String size) {    
    mFavoriteProgramList.setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(!isDetached() && getActivity() != null && getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE).equals(key)) {
      setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.devider_size_default));
    }
  }
  
  public void updateProgramsList() {
    mProgramListAdapter.notifyDataSetChanged();
  }
}
