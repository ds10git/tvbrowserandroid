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

import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mProgamListAdapter;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  
  private long mChannelID;
  private long mScrollTime;
  
  private String mDayClause;
  private String mFilterClause;
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  
  @Override
  public void onResume() {
    super.onResume();
    
    mKeepRunning = true;
    
    startUpdateThread();
  }
  
  @Override
  public void onPause() {
    super.onPause();
    
    mKeepRunning = false;
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    
    mDayClause = "";
    mFilterClause = "";
    
    mDataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };

    mDontWantToSeeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDontWantToSeeReceiver, new IntentFilter(SettingConstants.DONT_WANT_TO_SEE_CHANGED));
    
    mRefreshReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };

    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
    
    mKeepRunning = true;
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    mKeepRunning = false;
    
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
  
  public void setDay(long dayStart) {
    if(dayStart >= 0) {
      mDayClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">=" + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + (dayStart + (24 * 60 * 60000)) + " ) ";
    }
    else {
      mDayClause = "";
    }
    
    startUpdateThread();
  }
  
  public void setMarkFilter(int pos) {
    switch(pos) {
      case 0: mFilterClause = "";;break;
      case 1: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE_FAVORITE + "%' ) ";break;
      case 2: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE + "%' ) ";break;
      case 3: 
        if(Build.VERSION.SDK_INT >= 14) {
          mFilterClause = " AND ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE_CALENDAR + "%' ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE_REMINDER + "%' ) ) "; 
        }
        else {
          mFilterClause = " AND " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE_REMINDER + "%'"; break;
        }break;
      case 4: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE_SYNC_FAVORITE + "%' ) ";break;
      case 5: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE + " ) ";break;
    }
    
    startUpdateThread();
  }
  
  public void setScrollTime(long time) {
    mScrollTime = time;
  }
  
  public void scrollToTime() {
    if(mScrollTime > 0) {
      int testIndex = 0;
      
      if(mScrollTime > System.currentTimeMillis()) {
        Cursor c = mProgamListAdapter.getCursor();
        
        if(c.getCount() > 0) {
          try {
            int index = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
            int count = 0;
            c.moveToFirst();
            while(!c.isClosed() && c.moveToNext()) {
              long startTime = c.getLong(index);
              
              if(startTime >= mScrollTime) {
                testIndex = count;
                break;
              }
              else {
                count++;
              }
            }
          }catch(IllegalStateException e) {}
        }
      }
      mScrollTime = -1;
      final int scollIndex = testIndex;
      
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(getView() != null) {
            setSelection(scollIndex);
          }
        }
      });
    }
  }
  
  public void setChannelID(long id) {
    mChannelID = id;
    
    startUpdateThread();
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mChannelID = -1;
    registerForContextMenu(getListView());
    
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
    mProgamListAdapter = new SimpleCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0);
    mProgamListAdapter.setViewBinder(mViewAndClickHandler);
    
    setListAdapter(mProgamListAdapter);
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    mViewAndClickHandler.onCreateContextMenu(menu, v, menuInfo);
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
  
  private void startUpdateThread() {
    if(mUpdateThread == null || !mUpdateThread.isAlive()) {
      mUpdateThread = new Thread() {
        public void run() {
          handler.post(new Runnable() {
            @Override
            public void run() {
              if(mKeepRunning && !isDetached() && !isRemoving()) {
                getLoaderManager().restartLoader(0, null, ProgramsListFragment.this);
              }
            }
          });
        }
      };
      mUpdateThread.start();
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
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + System.currentTimeMillis() + " ) ";
        
    if(mChannelID != -1) {
      where += "AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + mChannelID;
    }
    
    if(!mFilterClause.contains(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE)) {
      where += " AND ( NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE + " ) ";
    }
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where + mDayClause + mFilterClause, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    return loader;
  }

  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor c) {
    mProgamListAdapter.swapCursor(c);
    scrollToTime();
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    mProgamListAdapter.swapCursor(null);
  }
}
