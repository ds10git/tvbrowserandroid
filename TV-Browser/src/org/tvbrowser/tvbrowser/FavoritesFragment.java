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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener, ShowDateInterface {
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  private SimpleCursorAdapter mProgramListAdapter;
  private ArrayAdapter<FavoriteSpinnerEntry> mFavoriteAdapter;
  private ArrayList<FavoriteSpinnerEntry> mFavoriteList;
  
  private ListView mFavoriteProgramList;
  
  private ArrayAdapter<FavoriteSpinnerEntry> mMarkingsAdapter;
  
  private String mWhereClause;
  
  private Handler handler;
  
  private Thread mUpdateThread;
  
  private BroadcastReceiver mReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  
  private boolean mIsRunning;
  
  private boolean mContainsListViewFavoriteSelection;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    int layout = R.layout.favorite_fragment_layout;
    
    if(PrefUtils.getStringValue(R.string.PREF_FAVORITE_TAB_LAYOUT, R.string.pref_favorite_tab_layout_default).equals("1")) {
      layout = R.layout.fragment_favorite_selection_list_layout;
    }
    
    View v = inflater.inflate(layout, container, false);
    
    return v;
  }
  
  public void updateSynchroButton(View view) {
    removeMarkingSelections();
    addMarkingSelections();
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
  private AdapterView<ArrayAdapter<FavoriteSpinnerEntry>> mFavoriteSelection;
  private FavoriteSpinnerEntry mCurrentFavoriteSelection;
  
  private TextView mHelp;
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    handler = new Handler();
    
    mWhereClause = null;
    
    mFavoriteList = new ArrayList<FavoriteSpinnerEntry>();
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
    
    for(String favorite : favoritesSet) {
      mFavoriteList.add(new FavoriteSpinnerEntry(new Favorite(favorite)));
    }
    
    removeMarkingSelections();
    
    Collections.sort(mFavoriteList);
    
    mFavoriteSelection = (AdapterView<ArrayAdapter<FavoriteSpinnerEntry>>)getView().findViewById(R.id.favorite_fragment_selection);
    
    final AtomicInteger rowLayout = new AtomicInteger(android.R.layout.simple_list_item_1);
    
    mContainsListViewFavoriteSelection = false;
    
    if(mFavoriteSelection.getClass().getCanonicalName().equals("android.widget.ListView")) {
      rowLayout.set(android.R.layout.simple_list_item_activated_1);
      
      ListView markings = (ListView)getView().findViewById(R.id.favorite_fragment_selection_markings);
      
      if(markings != null) {
        mMarkingsAdapter = new ArrayAdapter<FavoriteSpinnerEntry>(getActivity(), android.R.layout.simple_list_item_1) {
          public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
              convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            
            String name = getItem(position).toString();
            
            if(!getItem(position).containsFavorite()) {
              
              if(name.equals(getString(R.string.marking_value_marked))) {
                convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_COLOR_KEY, getContext()));
              }
              else if(name.equals(getString(R.string.marking_value_sync))) {
                convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_SYNC_COLOR_KEY, getContext()));
              }
              else {
                convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_REMINDER_COLOR_KEY, getContext()));
              }
            }
            
            ((TextView)convertView).setMaxLines(3);
            ((TextView)convertView).setText(name);
            
            return convertView;
          }
        };
        
        markings.setAdapter(mMarkingsAdapter);
      }
      
      markings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          try {
            Method setItemChecked = mFavoriteSelection.getClass().getMethod("setItemChecked", new Class<?>[]{int.class,boolean.class});
            setItemChecked.invoke(mFavoriteSelection, new Object[]{-1,true});
          } catch (Exception e) {
            Log.d("info2"," ttt ", e);
          }
          
          mCurrentFavoriteSelection = mMarkingsAdapter.getItem(position);
          mWhereClause = mCurrentFavoriteSelection.getWhereClause();
          
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
      
      try {
        Method setChoiceMode = mFavoriteSelection.getClass().getMethod("setChoiceMode", new Class<?>[] {int.class});
        setChoiceMode.invoke(mFavoriteSelection, new Object[] {ListView.CHOICE_MODE_SINGLE});
      } catch (Exception e) {
        Log.d("info2", " fff ", e);
      }
      
      mContainsListViewFavoriteSelection = true;
    }
    
    addMarkingSelections();
    
    mFavoriteAdapter = new ArrayAdapter<FavoriteSpinnerEntry>(getActivity(), rowLayout.get(), mFavoriteList){
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        FavoriteSpinnerEntry entry = getItem(position);
        
        if(convertView == null) {
          convertView = getActivity().getLayoutInflater().inflate(rowLayout.get(), parent, false);
        }
        
        String name = entry.toString();
        
        if(!mContainsListViewFavoriteSelection) {
          Drawable draw = getResources().getDrawable(android.R.drawable.list_selector_background);
          
          if(!entry.containsFavorite()) {
            if(name.equals(getString(R.string.marking_value_marked))) {
              draw = new ColorDrawable(UiUtils.getColor(UiUtils.MARKED_COLOR_KEY, getContext()));
            }
            else if(name.equals(getString(R.string.marking_value_sync))) {
              draw = new ColorDrawable(UiUtils.getColor(UiUtils.MARKED_SYNC_COLOR_KEY, getContext()));
            }
            else {
              draw = new ColorDrawable(UiUtils.getColor(UiUtils.MARKED_REMINDER_COLOR_KEY, getContext()));
            }
          }
          
          CompatUtils.setBackground(convertView, draw);
          convertView.setPadding(UiUtils.convertDpToPixel(5f, getResources()), 0, 0, 0);
        }
        
        ((TextView)convertView).setMaxLines(3);
        ((TextView)convertView).setText(name);
        
        return convertView;
      }
      
      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
      }
    };
    
    mHelp = (TextView)getView().findViewById(R.id.favorite_fragment_help);
    
    mFavoriteSelection.setAdapter(mFavoriteAdapter);
    mFavoriteSelection.getAdapter().registerDataSetObserver(new DataSetObserver() {
      @Override
      public void onChanged() {
        final AtomicInteger position = new AtomicInteger(-1);
        
        if(mCurrentFavoriteSelection != null) {
          for(int i = 0; i < mFavoriteList.size(); i++) {
            FavoriteSpinnerEntry entry = mFavoriteList.get(i);
            
            if((mCurrentFavoriteSelection.containsFavorite() && entry.containsFavorite() && entry.getFavorite().equals(mCurrentFavoriteSelection.getFavorite()))
                || (!mCurrentFavoriteSelection.containsFavorite()) && !entry.containsFavorite() && mCurrentFavoriteSelection.toString().equals(entry.toString())) {
              position.set(i);
              break;
            }
          }
        }
        
        if(position.get() == -1 && mFavoriteAdapter.getCount() > 0) {
          position.set(0);
        }
        
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(mContainsListViewFavoriteSelection) {
              try {
                Method setItemChecked = mFavoriteSelection.getClass().getMethod("setItemChecked", new Class<?>[]{int.class,boolean.class});
                setItemChecked.invoke(mFavoriteSelection, new Object[]{position.get(),true});
                
                mCurrentFavoriteSelection = mFavoriteList.get(position.get());
                mWhereClause = mCurrentFavoriteSelection.getWhereClause();
                
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    if(!isDetached() && getActivity() != null) {
                      getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
                    }
                  }
                });
              } catch (Exception e) {
                Log.d("info2", " tt ", e);
              }
            }
            else {
              mFavoriteSelection.setSelection(position.get());
            }
          }
        });
      }
    });
    
    mFavoriteSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mCurrentFavoriteSelection = mFavoriteList.get(position);
        mWhereClause = mCurrentFavoriteSelection.getWhereClause();
        
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && getActivity() != null) {
              getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
            }
          }
        });
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        mWhereClause = null;
        
        if(!isDetached() && getActivity() != null) {
          getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
        }
      }
    });
    
    if(mContainsListViewFavoriteSelection) {
      mFavoriteSelection.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
          mCurrentFavoriteSelection = mFavoriteList.get(position);
          mWhereClause = mCurrentFavoriteSelection.getWhereClause();
          
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
    }
    
    updateSynchroButton(null);
 
    mFavoriteProgramList = (ListView)getView().findViewById(R.id.favorite_program_list);
    mFavoriteProgramList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View v, int position,
          long id) {
        mViewAndClickHandler.onListItemClick(null, v, position, id);
      }
    });
    
    registerForContextMenu(mFavoriteSelection);
            
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
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity(),this);
    
    // Create a new Adapter an bind it to the List View
    mProgramListAdapter = new OrientationHandlingCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_lists_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0, true);
    mProgramListAdapter.setViewBinder(mViewAndClickHandler);
        
    mFavoriteProgramList.setAdapter(mProgramListAdapter);
    
    SeparatorDrawable drawable = new SeparatorDrawable(getActivity());
    
    mFavoriteProgramList.setDivider(drawable);
    
    prefs.registerOnSharedPreferenceChangeListener(this);
    
    setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.divider_size_default));
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
              if(intent.hasExtra(Favorite.FAVORITE_EXTRA)) {
                String oldName = intent.getStringExtra(Favorite.OLD_NAME_KEY);
                
                FavoriteSpinnerEntry fav = null;
                
                if(oldName != null) {
                  for(FavoriteSpinnerEntry favorite : mFavoriteList) {
                    if(favorite.containsFavorite() && favorite.getFavorite().getName().equals(oldName)) {
                      fav = favorite;
                      break;
                    }
                  }
                }
                
                if(fav == null) {
                  fav = new FavoriteSpinnerEntry((Favorite)intent.getSerializableExtra(Favorite.FAVORITE_EXTRA));
                  mCurrentFavoriteSelection = fav;
                  mFavoriteList.add(fav);
                }
                else {
                  Favorite temp = (Favorite)intent.getSerializableExtra(Favorite.FAVORITE_EXTRA);
                  
                  fav.getFavorite().setValues(temp.getName(), temp.getSearchValue(), temp.getType(), temp.remind(), temp.getTimeRestrictionStart(), temp.getTimeRestrictionEnd(), temp.getDayRestriction(), temp.getChannelRestrictionIDs(), temp.getExclusions(), temp.getDurationRestrictionMinimum(), temp.getDurationRestrictionMaximum(), temp.getAttributeRestrictionIndices());
                }
                
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    mFavoriteAdapter.notifyDataSetChanged();
                  }
                });
              }
              else {
                if(!isDetached() && getActivity() != null) {
                  for(FavoriteSpinnerEntry fav : mFavoriteList) {
                    if(fav.containsFavorite()) {
                      Favorite.removeFavoriteMarking(getActivity(), getActivity().getContentResolver(), fav.getFavorite());
                    }
                  }
                  
                  mFavoriteList.clear();

                  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                  
                  Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
                  
                  for(String favorite : favoritesSet) {
                    Favorite fav = new Favorite(favorite);
                    Favorite.updateFavoriteMarking(getActivity(), getActivity().getContentResolver(), fav);
                    mFavoriteList.add(new FavoriteSpinnerEntry(fav));
                  }
                }
              }
              
              removeMarkingSelections();
              
              Collections.sort(mFavoriteList);
              
              addMarkingSelections();
              
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
          handler.post(new Runnable() {
            public void run() {
              mFavoriteAdapter.notifyDataSetChanged();
            }
          });
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
  
  private void removeMarkingSelections() {
    if(!mContainsListViewFavoriteSelection) {
      for(int i = mFavoriteList.size()-1; i >= 0; i--) {
        if(!mFavoriteList.get(i).containsFavorite()) {
          mFavoriteList.remove(i);
        }
      }
    }
    else {
      handler.post(new Runnable() {
        @Override
        public void run() {
          mMarkingsAdapter.clear();
          mMarkingsAdapter.notifyDataSetChanged();
        }
      });
    }
  }
  
  private void addMarkingSelections() {
    final FavoriteSpinnerEntry marked = new FavoriteSpinnerEntry(getString(R.string.marking_value_marked), TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " " + TvBrowserContentProvider.DATA_KEY_MARKING_MARKING);
      
    String name = getString(R.string.marking_value_reminder);
    String whereClause = TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " ";
    
    if(Build.VERSION.SDK_INT >= 14) {
      name = getString(R.string.marking_value_reminder) + "/" + getString(R.string.marking_value_calendar);
      whereClause += " ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER  + " ) ) ";
    }
    else {
      whereClause += " ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER  + " ) ) ";
    }
    
    final FavoriteSpinnerEntry reminder = new FavoriteSpinnerEntry(name, whereClause);
    FavoriteSpinnerEntry syncTemp = null;
    
    if(PrefUtils.getBooleanValue(R.string.PREF_SYNC_FAV_FROM_DESKTOP, R.bool.pref_sync_fav_from_desktop_default) && getActivity().getSharedPreferences("transportation", Context.MODE_PRIVATE).getString(SettingConstants.USER_NAME, "").trim().length() > 0) {
      syncTemp = new FavoriteSpinnerEntry(getString(R.string.marking_value_sync), TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " " + TvBrowserContentProvider.DATA_KEY_MARKING_SYNC);
    }
    
    final FavoriteSpinnerEntry sync = syncTemp;
    
    if(!mContainsListViewFavoriteSelection) {
      mFavoriteList.add(marked);
      mFavoriteList.add(reminder);
      
      if(sync != null) {
        mFavoriteList.add(sync);
      }
    }
    else {
      handler.post(new Runnable() {
        @Override
        public void run() {
          mMarkingsAdapter.add(marked);
          mMarkingsAdapter.add(reminder);
          
          if(sync != null) {
            mMarkingsAdapter.add(sync);
          }
          
          mMarkingsAdapter.notifyDataSetChanged();
        }
      });
    }
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
    where += UiUtils.getDontWantToSeeFilterString(getActivity());
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    return loader;
  }

  private void updateFavorites() {
    HashSet<String> favoriteSet = new HashSet<String>();
    
    for(FavoriteSpinnerEntry fav : mFavoriteList) {
      if(fav.containsFavorite()) {
        favoriteSet.add(fav.getFavorite().getSaveString());
      }
    }
    
    Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
    
    edit.putStringSet(SettingConstants.FAVORITE_LIST, favoriteSet);
    
    edit.commit();
    
    handler.post(new Runnable() {
      @Override
      public void run() {
        mFavoriteAdapter.notifyDataSetChanged();
      }
    });
  }
  
  private void editFavorite(final Favorite fav) {
    UiUtils.editFavorite(fav, getActivity(), null);
  }
  
  private FavoriteSpinnerEntry mCurrentSelection;
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
    ContextMenuInfo menuInfo) {
    int pos = mFavoriteSelection.getSelectedItemPosition();
    
    if(pos == -1 && menuInfo != null && menuInfo instanceof AdapterContextMenuInfo) {
      pos = ((AdapterContextMenuInfo)menuInfo).position;
    }
    
    FavoriteSpinnerEntry entry = mFavoriteList.get(pos);
    
    if(entry.containsFavorite()) {
      mCurrentSelection = entry;
      getActivity().getMenuInflater().inflate(R.menu.favorite_context, menu);
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(mCurrentSelection != null) {
      if(item.getItemId() == R.id.delete_favorite) {
        mFavoriteList.remove(mCurrentSelection);
        
        new Thread() {
          public void run() {
            Favorite.removeFavoriteMarking(getActivity().getApplicationContext(), getActivity().getContentResolver(), mCurrentSelection.getFavorite());
          }
        }.start();
        
        updateFavorites();
      }
      else if(item.getItemId() == R.id.edit_favorite) {
        editFavorite(mCurrentSelection.getFavorite());
      }
      
      return true;
    }
    
    return false;
  }
  
  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    if(c != null && c.getCount() > 0) {
      mHelp.setVisibility(View.GONE);
    }
    else {
      mHelp.setVisibility(View.VISIBLE);
    }
    
    mProgramListAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    mProgramListAdapter.swapCursor(null);
    mHelp.setVisibility(View.VISIBLE);
  }
  

  private void setDividerSize(String size) {    
    mFavoriteProgramList.setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(!isDetached() && getActivity() != null && getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE).equals(key)) {
      setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.divider_size_default));
    }
  }
  
  public void updateProgramsList() {
    mProgramListAdapter.notifyDataSetChanged();
  }

  @Override
  public boolean showDate() {
    return true;
  }
  
  private static final class FavoriteSpinnerEntry implements Comparable<FavoriteSpinnerEntry> {
    private String mName;
    private String mEntryWhereClause;
    private Favorite mFavorite;
    
    public FavoriteSpinnerEntry(String name, String whereClause) {
      mName = name;
      mEntryWhereClause = whereClause;
      mFavorite = null;
    }
    
    public FavoriteSpinnerEntry(Favorite fav) {
      mFavorite = fav;
    }
    
    public boolean containsFavorite() {
      return mFavorite != null;
    }
    
    public Favorite getFavorite() {
      return mFavorite;
    }
    
    public String getWhereClause() { 
      if(containsFavorite()) {
        return mFavorite.getWhereClause();
      }
      
      return mEntryWhereClause;
    }
    
    @Override
    public String toString() {
      if(containsFavorite()) {
        return mFavorite.toString();
      }
      
      return mName;
    }

    @Override
    public int compareTo(FavoriteSpinnerEntry another) {
      return toString().compareToIgnoreCase(another.toString());
    }
  }
}
