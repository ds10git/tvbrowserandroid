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

import java.util.Calendar;

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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class ProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener {
  private SimpleCursorAdapter mProgramListAdapter;
  
  private Handler handler = new Handler();
    
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  
  private long mChannelID;
  private long mScrollTime;
  private long mDayStart;
  
  private String mDayClause;
  private String mFilterClause;
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  
  private boolean mDontUpdate;
  private int mScrollPos;
  
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
    
    mDayStart = 0;
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
      mDayStart = dayStart;
      mDayClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">=" + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + (dayStart + (24 * 60 * 60000)) + " ) ";
    }
    else {
      mDayClause = "";
      mDayStart = 0;
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
      
      if(mScrollTime <= 1441) {
        mScrollTime--;
        
        if(mDayStart > 0) {
          mScrollTime = mDayStart + mScrollTime * 60000;
        }
        else {
          Calendar now = Calendar.getInstance();
          now.set(Calendar.HOUR_OF_DAY,(int)(mScrollTime / 60));
          now.set(Calendar.MINUTE,(int)(mScrollTime % 60));
          now.set(Calendar.SECOND, 0);
          now.set(Calendar.MILLISECOND, 0);
          
          mScrollTime = now.getTimeInMillis();
          
          if(mScrollTime < System.currentTimeMillis()) {
            mScrollTime += 1440 * 60000;
          }
        }
      }
    
      Cursor c = mProgramListAdapter.getCursor();
      
      if(c.getCount() > 0) {
        try {
          int index = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
          int count = 0;
          c.moveToFirst();
          
          if(!c.isClosed()) {
            do {
              long startTime = c.getLong(index);
              
              if(startTime >= mScrollTime) {
                testIndex = count;
                break;
              }
              else {
                count++;
              }
            }while(c.moveToNext());
          }
        }catch(IllegalStateException e) {}
      }
      
      mScrollTime = -1;
            
      final int scollIndex = testIndex;
      
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(getListView() != null) {
            setSelection(scollIndex);
            handler.post(new Runnable() {
              @Override
              public void run() {
                setSelection(scollIndex);
              }
            });
          }
        }
      });
    }
    else if(mScrollTime == 0) {
      Spinner test = (Spinner)((ViewGroup)getView().getParent()).findViewById(R.id.date_selection);
      
      if(test != null && test.getSelectedItemPosition() > 0) {
        test.setSelection(0);
      }
      else {
        mScrollTime = -1;
  
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(getView() != null) {
              setSelection(0);
            }
          }
        });
      }
    }
  }
  
  public void setChannelID(long id) {
    mChannelID = id;
    
    startUpdateThread();
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    pref.registerOnSharedPreferenceChangeListener(this);
    
    super.onActivityCreated(savedInstanceState);
    mChannelID = -1;
    mDontUpdate = false;
    mScrollPos = -1;
    
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
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0,true);
    
    mProgramListAdapter.setViewBinder(mViewAndClickHandler);
    
    setListAdapter(mProgramListAdapter);
    
    SeparatorDrawable drawable = new SeparatorDrawable(getActivity());
    
    getListView().setDivider(drawable);
    
    setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.devider_size_default));
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  public void setDontUpdate(boolean value) {
    mDontUpdate = value;
  }
  
  public void startUpdateThread() {
    if(!mDontUpdate && (mUpdateThread == null || !mUpdateThread.isAlive())) {
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
    
    if(PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_LISTS, R.bool.show_pictures_in_lists_default)) {
      projection = new String[16];
      
      projection[15] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[15];
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
    projection[14] = TvBrowserContentProvider.CHANNEL_KEY_LOGO;
    
    long time = System.currentTimeMillis();
    
    String where = mDayClause.trim().length() == 0 ? " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + time + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + time + " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + time + " ) " : " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > 0 ) ";
        
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
    mProgramListAdapter.swapCursor(c);
    
    if(mScrollPos == -1) {
      scrollToTime();
    }
    else {
      if(getListView() != null) {
        getListView().setSelection(mScrollPos);
      }
      
      mScrollPos = -1;
    }
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    mProgramListAdapter.swapCursor(null);
  }
  
  private void setDividerSize(String size) {    
    getListView().setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(!isDetached() && getActivity() != null && key != null && getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE) != null && getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE).equals(key)) {
      setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.devider_size_default));
    }
  }
  
  public void setScrollPos(int pos) {
    mScrollPos = pos;
  }
  
  public int getCurrentScrollIndex() {
    int pos = getListView().getFirstVisiblePosition();
    
    View view = getListView().getChildAt(0);
    
    if(view != null && mProgramListAdapter.getCount() > 1 && view.getTop() < 0) {
      pos++;
    }
    
    if(pos < 0) {
      pos = 0;
    }
    
    return pos;
  }
}
