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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.LoaderUpdater.CallbackObjects;
import org.tvbrowser.tvbrowser.LoaderUpdater.UnsupportedFragmentException;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;
import org.tvbrowser.view.SeparatorDrawable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.TextView;

public class FragmentFavorites extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener, ShowDateInterface, MarkingsUpdateListener, LoaderUpdater.Callback {
  private static final String KEY_UPDATE_FAVORITES_MENU = "updateFavoritesMenu";
  private static final String KEY_EDIT_DELETE_ENABLED = "editDeleteEnabled";
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  private SimpleCursorAdapter mProgramListAdapter;
  private ArrayAdapter<FavoriteSpinnerEntry> mFavoriteAdapter;
  private ArrayList<FavoriteSpinnerEntry> mFavoriteList;
  private DataSetObserver mFavoriteSelectionObserver;
  
  private ListView mFavoriteProgramList;
  private ListView mMarkingsList;
  
  private ArrayAdapter<FavoriteSpinnerEntry> mMarkingsAdapter;
  
  private WhereClause mWhereClause;
  
  private Handler handler;
  private Thread mUpdateThread;
  
  private LoaderUpdater mLoaderUpdate;
  private LoaderUpdater.CallbackObjects mCallback;
   
  private BroadcastReceiver mFavoriteChangedReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  
  private boolean mContainsListViewFavoriteSelection;
  
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    int layout = R.layout.favorite_fragment_layout;
    
    if(PrefUtils.getStringValue(R.string.PREF_FAVORITE_TAB_LAYOUT, R.string.pref_favorite_tab_layout_default).equals("1")) {
      layout = R.layout.fragment_favorite_selection_list_layout;
    }
    
    return inflater.inflate(layout, container, false);
  }
  
  public void updateSynchroButton(View view) {
    removeMarkingSelections();
    addMarkingSelections();
  }
  
  @Override
  public void onResume() {
    super.onResume();
    
    if(getActivity() != null) {
      ProgramUtils.registerMarkingsListener(getActivity(), this);
      
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(!isDetached() && !isRemoving()) {
            mFavoriteAdapter.notifyDataSetChanged();
          }
        }
      });
    }
  }
  
  private boolean mIsStarted = false;
  
  @Override
  public void onStart() {
    try {
      Activity activity = getActivity();
      if(activity != null) {
        int selection = PrefUtils.getIntValue(R.string.PREF_MISC_LAST_FAVORITE_SELECTION, 0);
        Log.d("info13", "selection " + selection + " " + mContainsListViewFavoriteSelection);
        if(mFavoriteSelection != null) {
          Log.d("info13", "count " + mFavoriteSelection.getCount());
        }
        if(selection < 0 && mMarkingsList != null && Math.abs(selection) < mMarkingsList.getChildCount()) {
          selection = Math.abs(selection);
          
          mMarkingsList.performItemClick(null, selection, -1);
        }
        else if(mFavoriteSelection != null && selection >= 0 && selection < mFavoriteSelection.getCount()) {
          if(mContainsListViewFavoriteSelection) {
            mFavoriteSelection.performItemClick(null, selection, -1);
          }
          else {
            mFavoriteSelection.setSelection(selection);
          }
        }
        
        handler.post(new Runnable() {
          @Override
          public void run() {
            mIsStarted = true;
          }
        });
      }
    }catch(Throwable ignored) {
      
    }
    
    super.onStart();
  }
  
  @Override
  public void onStop() {
    mIsStarted = false;
    
    super.onStop();
  }
    
  @Override
  public void onPause() {
    ProgramUtils.unregisterMarkingsListener(getActivity(), this);
    
    super.onPause();
  }
  
  private void startUpdateThread() {
    startUpdateThread(false,false);
  }
  
  private void startUpdateThread(final boolean updateFavoritesMenu, final boolean editDeleteEnabled) {
    mCallback.addOrReplace(new LoaderUpdater.CallbackObject<>(KEY_UPDATE_FAVORITES_MENU, updateFavoritesMenu));
    mCallback.addOrReplace(new LoaderUpdater.CallbackObject<>(KEY_EDIT_DELETE_ENABLED, editDeleteEnabled));
    
    mLoaderUpdate.startUpdate(mCallback);
  }
  
  private AdapterView<ArrayAdapter<FavoriteSpinnerEntry>> mFavoriteSelection;
  private FavoriteSpinnerEntry mCurrentFavoriteSelection;
  
  private TextView mHelp;
    
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    handler = new Handler();
    
    if(mLoaderUpdate == null) {
      try {
        mLoaderUpdate = new LoaderUpdater(FragmentFavorites.this, handler);
      } catch (UnsupportedFragmentException e1) {
        Log.d("info14", "", e1);
        // Ignore
      }
    }
    
    mCallback = new LoaderUpdater.CallbackObjects();
    
    mWhereClause = new WhereClause();
    
    mFavoriteList = new ArrayList<>();
    
    SharedPreferences prefs = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, getActivity());
    
    updateFavoriteList(false);
    removeMarkingSelections();
    
    Collections.sort(mFavoriteList);
    
    mFavoriteSelection = getView().findViewById(R.id.favorite_fragment_selection);
    
    final AtomicInteger rowLayout = new AtomicInteger(android.R.layout.simple_list_item_1);
    
    mContainsListViewFavoriteSelection = false;
    
    if(mFavoriteSelection.getClass().getCanonicalName().equals("android.widget.ListView")) {
      rowLayout.set(android.R.layout.simple_list_item_activated_1);
      
      mMarkingsList = getView().findViewById(R.id.favorite_fragment_selection_markings);
      
      if(mMarkingsList != null) {
        mMarkingsAdapter = new ArrayAdapter<FavoriteSpinnerEntry>(getActivity(), android.R.layout.simple_list_item_1) {
          @NonNull
          public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
        
        mMarkingsList.setAdapter(mMarkingsAdapter);
      }
      
      mMarkingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          try {
            Method setItemChecked = mFavoriteSelection.getClass().getMethod("setItemChecked", int.class,boolean.class);
            setItemChecked.invoke(mFavoriteSelection, -1,true);
          } catch (Exception ignored) {
          }
          
          mCurrentFavoriteSelection = mMarkingsAdapter.getItem(position);
          mWhereClause = getWhereClause();
          
          final TvBrowser tvb = (TvBrowser)getActivity();
          
          if(tvb != null) {
            tvb.updateFavoritesMenu(false);
            
            startUpdateThread();
                        
            saveCurrentSelection(tvb, (position * -1));
          }
        }
      });
      
      try {
        Method setChoiceMode = mFavoriteSelection.getClass().getMethod("setChoiceMode", int.class);
        setChoiceMode.invoke(mFavoriteSelection, ListView.CHOICE_MODE_SINGLE);
      } catch (Exception ignored) {
      }
      
      mContainsListViewFavoriteSelection = true;
    }
    
    addMarkingSelections();
    
    final AtomicReference<Object> backgroundRef = new AtomicReference<>(null);
    
    TypedValue a = new TypedValue();
    getActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
    if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
        // windowBackground is a color
      backgroundRef.set(a.data);
    } else {
        // windowBackground is not a color, probably a drawable
        ///Drawable d = ContextCompat.getDrawable(getActivity(),a.resourceId);
      backgroundRef.set(ContextCompat.getDrawable(getActivity(),a.resourceId));
    }
    
    mFavoriteAdapter = new ArrayAdapter<FavoriteSpinnerEntry>(getActivity(), rowLayout.get(), mFavoriteList){
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent, false);
      }
      
      View getView(int position, View convertView, ViewGroup parent, boolean popup) {
        if(position < getCount()) {
          FavoriteSpinnerEntry entry = getItem(position);
          
          if(convertView == null) {
            convertView = getActivity().getLayoutInflater().inflate(rowLayout.get(), parent, false);
          }
          
          String name = entry.toString();
          
          Drawable layerDrawable = null;
          Drawable backgound = null;
          
          if(backgroundRef.get() instanceof Drawable) {
            backgound = (Drawable)backgroundRef.get();
          }
          else if(backgroundRef.get() instanceof Integer) {
            backgound = new ColorDrawable((Integer) backgroundRef.get());
          }
          
          if(!mContainsListViewFavoriteSelection) {
            Drawable draw = ContextCompat.getDrawable(getContext(),android.R.drawable.list_selector_background);
            
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
            
            if(backgound != null && popup) {
              layerDrawable = new LayerDrawable(new Drawable[] {backgound,draw});
            }
            else {
              layerDrawable = draw;
            }
          }
          else if(popup) {
            layerDrawable = backgound;
          }
          
          if(layerDrawable != null) {
            CompatUtils.setBackground(convertView, layerDrawable);
          }
          
          int pixel = UiUtils.convertDpToPixel(5f, getResources());
          convertView.setPadding(pixel, 0, pixel, 0);
          
          ((TextView)convertView).setMaxLines(3);
          ((TextView)convertView).setText(name);
          
          return convertView;
        }
        
        return null;
      }
      
      @Override
      public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent, true);
      }
    };
    
    mHelp = getView().findViewById(R.id.favorite_fragment_help);
    
    mFavoriteSelectionObserver = new DataSetObserver() {
      @Override
      public void onChanged() {
        if(mIsStarted) {
          final AtomicInteger position = new AtomicInteger(-1);
 //         int currentValue = -1;
          
          if(mCurrentFavoriteSelection != null) {
            for(int i = 0; i < mFavoriteList.size(); i++) {
              FavoriteSpinnerEntry entry = mFavoriteList.get(i);
              
              if((mCurrentFavoriteSelection.containsFavorite() && entry.containsFavorite() && entry.getFavorite().equals(mCurrentFavoriteSelection.getFavorite()))
                  || (!mCurrentFavoriteSelection.containsFavorite()) && !entry.containsFavorite() && mCurrentFavoriteSelection.toString().equals(entry.toString())) {
                position.set(i);
                break;
              }
            }
            
            if(position.get() == -1 && mMarkingsList != null) {
              for(int i = 0; i < mMarkingsAdapter.getCount(); i++) {
                FavoriteSpinnerEntry entry = mMarkingsAdapter.getItem(i);
                
                if(!mCurrentFavoriteSelection.containsFavorite() && !entry.containsFavorite() && mCurrentFavoriteSelection.toString().equals(entry.toString())) {
                  position.set(-2);
                  break;
                }
              }
            }
            
       //     currentValue = mFavoriteSelection.getSelectedItemPosition();
          }
          
          if(position.get() == -1) {
            int lastPosition = PrefUtils.getIntValue(R.string.PREF_MISC_LAST_FAVORITE_SELECTION, 0);
            
            if(lastPosition >= 0 && mFavoriteAdapter.getCount() > lastPosition) {
              position.set(lastPosition);
            }
          }
          
          if(/*currentValue != position.get() &&*/ position.get() >= 0) {
            handler.post(new Runnable() {
              @Override
              public void run() {
                if(mContainsListViewFavoriteSelection) {
                  try {
                    Method setItemChecked = mFavoriteSelection.getClass().getMethod("setItemChecked", int.class,boolean.class);
                    setItemChecked.invoke(mFavoriteSelection, position.get(),true);
                    
                    if(position.get() < mFavoriteList.size()) {
                      selectFavorite(position.get());
                      mFavoriteSelection.setSelection(position.get());
                    }
                  } catch (Exception ignored) {
                  }
                }
                else {
                  mFavoriteSelection.setSelection(position.get());
                }
              }
            });
          }
        }
      }
    };
    
    mFavoriteSelection.setAdapter(mFavoriteAdapter);
    mFavoriteSelection.getAdapter().registerDataSetObserver(mFavoriteSelectionObserver);
    
    mFavoriteSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectFavorite(position);
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        mCurrentSelection = mCurrentFavoriteSelection = null;
        mWhereClause = new WhereClause();
        
        startUpdateThread(true,false);
      }
    });
    
    if(mContainsListViewFavoriteSelection) {
      mFavoriteSelection.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
          selectFavorite(position);
        }
      });
    }
    
    updateSynchroButton(null);
 
    mFavoriteProgramList = getView().findViewById(R.id.favorite_program_list);
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
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity(),this,handler);
    
    // Create a new Adapter an bind it to the List View
    mProgramListAdapter = new OrientationHandlingCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_lists_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0, true, handler);
    mProgramListAdapter.setViewBinder(mViewAndClickHandler);
        
    mFavoriteProgramList.setAdapter(mProgramListAdapter);
    
    SeparatorDrawable drawable = new SeparatorDrawable(getActivity());
    
    mFavoriteProgramList.setDivider(drawable);
    
    prefs.registerOnSharedPreferenceChangeListener(this);
    
    setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.pref_program_lists_divider_size_default));
  }
  
  private WhereClause getWhereClause() {
    WhereClause whereClause = mCurrentFavoriteSelection.getWhereClause();
    
    if(whereClause == null) {
      WhereClause marking = ProgramUtils.getPluginMarkingsSelection(getActivity());
      
      if(marking.getWhere().trim().isEmpty()) {
        marking = new WhereClause(TvBrowserContentProvider.KEY_ID + "<0", null);
      }
      
      whereClause = new WhereClause(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " " + marking.getWhere(), marking.getSelectionArgs());
    }
    
    return whereClause;
  }
  
  private void selectFavorite(final int position) {
    if(position >= 0 && position < mFavoriteList.size()) {
      mCurrentSelection = mCurrentFavoriteSelection = mFavoriteList.get(position);
      mWhereClause = getWhereClause();
      
      final Activity tvb = getActivity();
      
      if(tvb != null) {
        startUpdateThread(true, mCurrentFavoriteSelection.containsFavorite());
        
        saveCurrentSelection(tvb, position);
      }
    }
  }
  
  private void saveCurrentSelection(final Context context, final int position) {
    Log.d("info13", "mIsStarted " + mIsStarted + " " + position);
    if(mIsStarted) {
      boolean success = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putInt(context.getString(R.string.PREF_MISC_LAST_FAVORITE_SELECTION), position).commit();
      
      Log.d("info13", "success " + success + " " + PrefUtils.getIntValue(R.string.PREF_MISC_LAST_FAVORITE_SELECTION, 0));
    }
  }
  
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    
    if(handler == null) {
      handler = new Handler();
    }
    
    if(mLoaderUpdate == null) {
      try {
        mLoaderUpdate = new LoaderUpdater(FragmentFavorites.this, handler);
      } catch (UnsupportedFragmentException e1) {
        Log.d("info14", "", e1);
        // Ignore
      }
    }
    
    mFavoriteChangedReceiver = new BroadcastReceiver() {
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
                  
                  fav.getFavorite().setValues(temp.getName(), temp.getSearchValue(), temp.getType(), temp.remind(), temp.getTimeRestrictionStart(), temp.getTimeRestrictionEnd(), temp.getDayRestriction(), temp.getChannelRestrictionIDs(), temp.getExclusions(), temp.getDurationRestrictionMinimum(), temp.getDurationRestrictionMaximum(), temp.getAttributeRestrictionIndices(), temp.getUniqueProgramIds());
                }
                
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    mFavoriteAdapter.notifyDataSetChanged();
                    mFavoriteSelectionObserver.onChanged();
                  }
                });
              }
              else {
                if(!isDetached() && getActivity() != null) {
                  for(FavoriteSpinnerEntry fav : mFavoriteList) {
                    if(fav.containsFavorite()) {
                      Favorite.handleFavoriteMarking(getActivity(), fav.getFavorite(), Favorite.TYPE_MARK_REMOVE);
                    }
                  }
                  
                  mFavoriteList.clear();
                  updateFavoriteList(true);
                }
              }
              
              handler.post(new Runnable() {
                @Override
                public void run() {
                  mFavoriteAdapter.notifyDataSetChanged();
                  
                  removeMarkingSelections();
                  
                  Collections.sort(mFavoriteList);
                  
                  addMarkingSelections();
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
              mFavoriteList.clear();
              updateFavoriteList(false);
              
              removeMarkingSelections();
              
              Collections.sort(mFavoriteList);
              
              addMarkingSelections();
              
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
        startUpdateThread();
      }
    };
    
    IntentFilter dataUpdateFilter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    getActivity().registerReceiver(mDataUpdateReceiver, dataUpdateFilter);
    
    //LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, dataUpdateFilter);
    
    IntentFilter filter = new IntentFilter(SettingConstants.FAVORITES_CHANGED);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mFavoriteChangedReceiver, filter);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
    
    mLoaderUpdate.setIsRunning();
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
    if(getActivity() != null) {
      try {
        final FavoriteSpinnerEntry marked = new FavoriteSpinnerEntry(getString(R.string.marking_value_marked), null);
          
        String name = getString(R.string.marking_value_reminder);
        String whereClause = TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " ";
        
        whereClause += " ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER  + " ) ) ";
        
        final FavoriteSpinnerEntry reminder = new FavoriteSpinnerEntry(name, new WhereClause(whereClause,null));
        FavoriteSpinnerEntry syncTemp = null;
        
        if(PrefUtils.getBooleanValue(R.string.PREF_SYNC_FAV_FROM_DESKTOP, R.bool.pref_sync_fav_from_desktop_default) && getActivity().getSharedPreferences("transportation", Context.MODE_PRIVATE).getString(SettingConstants.USER_NAME, "").trim().length() > 0) {
          syncTemp = new FavoriteSpinnerEntry(getString(R.string.marking_value_sync), new WhereClause(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " " + TvBrowserContentProvider.DATA_KEY_MARKING_SYNC, null));
        }
        
        final FavoriteSpinnerEntry sync = syncTemp;
        
        if(!mContainsListViewFavoriteSelection) {
          mFavoriteList.add(marked);
          mFavoriteList.add(reminder);
          
          if(sync != null) {
            mFavoriteList.add(sync);
          }
          
          if(mFavoriteAdapter != null) {
            mFavoriteAdapter.notifyDataSetChanged();
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
      }catch(IllegalStateException ignored) {}
    }
  }
  
  @Override
  public void onDetach() {
    mLoaderUpdate.setIsNotRunning();
    
    if(mFavoriteChangedReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mFavoriteChangedReceiver);
    }
    if(mDataUpdateReceiver != null) {
      getActivity().unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
    if(mDontWantToSeeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDontWantToSeeReceiver);
    }
    super.onDetach();
  }
  
  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
    
    System.arraycopy(TvBrowserContentProvider.MARKING_COLUMNS, 0, projection, 13, TvBrowserContentProvider.MARKING_COLUMNS.length);
    
    String where = mWhereClause.getWhere();
    
    if(where == null) {
      where = " " + TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER + " " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=0 ";
    }
    
    if(where.trim().length() > 0 && !where.trim().equals(TvBrowserContentProvider.CONCAT_TABLE_PLACE_HOLDER)) {
      where += " AND ";
    }
    
    where += " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
    where += UiUtils.getDontWantToSeeFilterString(getActivity());
    
    TvBrowser tvb = (TvBrowser)getActivity();
    
    if(tvb != null && !isDetached()) {
      tvb.showSQLquery(where, mWhereClause.getSelectionArgs());
    }
    
    return new CursorLoader(tvb, TvBrowserContentProvider.RAW_QUERY_CONTENT_URI_DATA, projection, where, mWhereClause.getSelectionArgs(), TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
  }

  private void updateFavoriteList(boolean mark) {
    Favorite[] favorites = Favorite.getAllFavorites(getActivity());
    
    for(Favorite favorite : favorites) {
      if(mark) {
        Favorite.handleFavoriteMarking(getActivity(), favorite, Favorite.TYPE_MARK_ADD);
      }
      
      mFavoriteList.add(new FavoriteSpinnerEntry(favorite));
    }
    
    
  }
  
  private void updateFavorites() {
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
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
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
    if(getUserVisibleHint()) {
      if(mCurrentSelection != null) {
        if(item.getItemId() == R.id.delete_favorite) {
          deleteSelectedFavorite();
          return true;
        }
        else if(item.getItemId() == R.id.edit_favorite) {
          editSelectedFavorite();
          return true;
        }
      }
    }
    
    return false;
  }
  
  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader,
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
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    mProgramListAdapter.swapCursor(null);
    mHelp.setVisibility(View.VISIBLE);
  }
  

  private void setDividerSize(String size) {    
    mFavoriteProgramList.setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(!isDetached() && getActivity() != null) {
      if(getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE).equals(key)) {
        setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.pref_program_lists_divider_size_default));
      }
      else if(mFavoriteProgramList != null && (getString(R.string.PREF_COLOR_SEPARATOR_LINE).equals(key) || getString(R.string.PREF_COLOR_SEPARATOR_SPACE).equals(key))) {
        final Drawable separator = mFavoriteProgramList.getDivider();
        
        if(separator instanceof SeparatorDrawable) {
          ((SeparatorDrawable) separator).updateColors(getActivity());
        }
      }
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
    private WhereClause mEntryWhereClause;
    private final Favorite mFavorite;
    
    FavoriteSpinnerEntry(String name, WhereClause whereClause) {
      mName = name;
      mEntryWhereClause = whereClause;
      mFavorite = null;
    }
    
    FavoriteSpinnerEntry(Favorite fav) {
      mFavorite = fav;
    }
    
    boolean containsFavorite() {
      return mFavorite != null;
    }
    
    Favorite getFavorite() {
      return mFavorite;
    }
    
    WhereClause getWhereClause() {
      if(containsFavorite()) {
        return mFavorite.getExternalWhereClause();
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
  
  public void editSelectedFavorite() {
    if(mCurrentSelection != null) {
      editFavorite(mCurrentSelection.getFavorite());
    }
  }
  
  public void deleteSelectedFavorite() {
    if(mCurrentSelection != null) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      
      builder.setTitle(R.string.dialog_favorite_delete_title);
      builder.setMessage(getString(R.string.dialog_favorite_delete_message).replace("{0}", mCurrentSelection.getFavorite().getName()));
      
      builder.setPositiveButton(R.string.dialog_favorite_delete_button, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          mFavoriteList.remove(mCurrentSelection);
          updateFavorites();
          
          if(mCurrentSelection != null) {
            final Favorite current = mCurrentSelection.getFavorite();
            mCurrentSelection = null;
            final Context context = getActivity().getApplicationContext();
            
            new Thread("DELETE FAVORITE REMOVE MARKING THREAD") {
              public void run() {
                Favorite.deleteFavorite(context, current);
                mCurrentSelection = null;
              }
            }.start();
          }
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.show();
    }
  }

  @Override
  public void refreshMarkings() {
    
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(mCurrentFavoriteSelection != null) {
            if(mLoaderUpdate.isRunning() && !isDetached() && !isRemoving()) {
              WhereClause test = mCurrentFavoriteSelection.getWhereClause();
              
              if(test == null) {
                mWhereClause = getWhereClause();
                
                startUpdateThread();
                
                return;
              }
            }
            
            mFavoriteProgramList.invalidateViews();
          }
        }
      });
    
  }

  @Override
  public void handleCallback(CallbackObjects callbackObjects) {
    if(callbackObjects != null) {
      if((Boolean)callbackObjects.getCallbackObjectValue(KEY_UPDATE_FAVORITES_MENU, false)) {
        ((TvBrowser)getActivity()).updateFavoritesMenu((Boolean)callbackObjects.getCallbackObjectValue(KEY_EDIT_DELETE_ENABLED, false));
      }
    }
  }
}
