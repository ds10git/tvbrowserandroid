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
import java.util.Calendar;
import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RunningProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String WHERE_CLAUSE_KEY = "WHERE_CLAUSE_KEY";
  private SimpleCursorAdapter mRunningProgramListAdapter;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private int mWhereClauseTime;
  private int mTimeRangeID;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  
  private static final GradientDrawable BEFORE_GRADIENT;
  private static final GradientDrawable AFTER_GRADIENT;
  
  static {
    BEFORE_GRADIENT = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[] {Color.argb(0x84, 0, 0, 0xff),Color.WHITE});
    BEFORE_GRADIENT.setCornerRadius(0f);
    
    AFTER_GRADIENT = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[] {Color.WHITE,Color.argb(0x84, 0, 0, 0xff)});
    AFTER_GRADIENT.setCornerRadius(0f);
  }
  
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
    
    mDataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };
    
    mRefreshReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };
    
    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
  }
  
  public void setWhereClauseTime(Object time) {
    if(time instanceof Integer) {
      Button test = (Button)((View)getView().getParent()).findViewWithTag(mWhereClauseTime);
      
      if(test != null) {
        test.setBackgroundResource(android.R.drawable.list_selector_background);
      }
      
      setTimeRangeID(-1);
            
      mWhereClauseTime = ((Integer) time).intValue();
      
      if(mDataUpdateReceiver != null) {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
      }
      
      startUpdateThread();
    }
  }
  
  public void setTimeRangeID(int id) {
    Button test = (Button)((View)getView().getParent()).findViewById(mTimeRangeID);
    
    if(test != null) {
      test.setBackgroundResource(android.R.drawable.list_selector_background);
      test.setPadding(15, 0, 15, 0);
    }
    
    mTimeRangeID = id;
    
    switch(mTimeRangeID) {
      case -1:  break;
    }
    
    startUpdateThread();
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(WHERE_CLAUSE_KEY, mWhereClauseTime);
    super.onSaveInstanceState(outState);
  }
    
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    if(savedInstanceState != null) {
      mWhereClauseTime = savedInstanceState.getInt(WHERE_CLAUSE_KEY,-1);
    }
    else {
      mWhereClauseTime = -1;
    }
    
    mTimeRangeID = -1;
    
    registerForContextMenu(getListView());
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
        TvBrowserContentProvider.DATA_KEY_CATEGORIES
    };
    
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    // Create a new Adapter an bind it to the List View
    mRunningProgramListAdapter = new SimpleCursorAdapter(getActivity(),R.layout.running_list_entries,null,
        projection,new int[] {R.id.startTimeLabel,R.id.endTimeLabel,R.id.channelLabel,R.id.titleLabel,R.id.episodeLabel,R.id.genre_label,R.id.picture_copyright,R.id.info_label},0);
    mRunningProgramListAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        boolean showPicture = pref.getBoolean(view.getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false);
        boolean showGenre = pref.getBoolean(view.getResources().getString(R.string.SHOW_GENRE_IN_LISTS), true);
        boolean showEpisode = pref.getBoolean(view.getResources().getString(R.string.SHOW_EPISODE_IN_LISTS), true);
        boolean showInfo = pref.getBoolean(view.getResources().getString(R.string.SHOW_INFO_IN_LISTS), true);
        
        if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) {
          int channelID = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
          
          Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), null, null, null, null);
          
          if(channel.getCount() > 0) {
            channel.moveToFirst();
            
            TextView text = (TextView)view;
            
            String name = channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
            
            if(shortName != null) {
              name = shortName;
            }
            
            text.setText(name);
          }
          channel.close();
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) {
          if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) || !showInfo) {
            view.setVisibility(View.GONE);
          }
          else {
            int info = cursor.getInt(columnIndex);
            
            if(info != 0) {
              view.setVisibility(View.VISIBLE);
              ((TextView)view).setText(IOUtils.getInfoString(info,view.getResources()));
            }
            else {
              view.setVisibility(View.GONE);
            }
          }
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          TextView text = (TextView)view;
          text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
          text.setText(DateFormat.getTimeFormat(getActivity()).format(new Date(date)));
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          TextView text = (TextView)view;
          text.setText(DateFormat.getTimeFormat(getActivity()).format(new Date(date)));
          
          UiUtils.handleMarkings(getActivity(), cursor, ((RelativeLayout)view.getParent()), null);
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)) {
          TextView text = (TextView)view;
          
          long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          if(end <= System.currentTimeMillis()) {
            text.setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.info_label)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.picture_copyright)).setTextColor(Color.rgb(200, 200, 200));
          }
          else if(System.currentTimeMillis() <= end) {
            int[] attrs = new int[] { android.R.attr.textColorSecondary };
            TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
            int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
            a.recycle();
            
            text.setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.info_label)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.picture_copyright)).setTextColor(DEFAULT_TEXT_COLOR);
          }
          //
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) {
          if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) || !showEpisode) {
            view.setVisibility(View.GONE);
          }
          else {
            view.setVisibility(View.VISIBLE);
          }
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) {
          TextView text = (TextView)view;
          
          if(cursor.isNull(columnIndex) || !showGenre) {
            text.setVisibility(View.GONE);
          }
          else {
            text.setVisibility(View.VISIBLE);
          }
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)) {
          TextView text = (TextView)view;
          ImageView picture = (ImageView)((RelativeLayout)text.getParent()).findViewById(R.id.picture);
          
          if(!cursor.isNull(columnIndex) && cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE) != -1 && showPicture) {
            byte[] logoData = cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE));
            Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
                      
            BitmapDrawable l = new BitmapDrawable(view.getResources(), logo);
            
            l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
            
            long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
            
            if(end <= System.currentTimeMillis()) {
              l.setColorFilter(getActivity().getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
            }
            
            picture.setImageDrawable(l);
            
            text.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));
            text.setVisibility(View.VISIBLE);
            picture.setVisibility(View.VISIBLE);
          }
          else {
            picture.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
          }
        }
        
        return false;
      }
    });
    
    setListAdapter(mRunningProgramListAdapter);
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    if(mDataUpdateReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
    
    mKeepRunning = false;
  }
    
  private void startUpdateThread() {
    if(mUpdateThread == null || !mUpdateThread.isAlive()) {
      mUpdateThread = new Thread() {
        public void run() {
          handler.post(new Runnable() {
            @Override
            public void run() {
              if(!isDetached() &&  mKeepRunning && !isRemoving()) {
                getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
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
      projection = new String[13];
      
      projection[12] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[12];
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
    projection[11] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 30);
    
    if(mWhereClauseTime >= 0) {
      cal.set(Calendar.HOUR_OF_DAY, mWhereClauseTime / 60);
      cal.set(Calendar.MINUTE, mWhereClauseTime % 60);
    }
    else {
      cal.setTimeInMillis(System.currentTimeMillis());
    }
    
    if(getView().getParent() != null) {
      Button test = (Button)((View)getView().getParent()).findViewWithTag(Integer.valueOf(mWhereClauseTime));
      
      
      if(test != null) {
        switch(mTimeRangeID) {
          case -1: test.setBackgroundResource(R.color.filter_selection);break;
          case R.id.button_before1: test.setBackgroundDrawable(BEFORE_GRADIENT);break;
          case R.id.button_after1: test.setBackgroundDrawable(AFTER_GRADIENT);break;
        }
      }
      
      test = (Button)((View)getView().getParent()).findViewById(mTimeRangeID);
      
      if(test != null) {
        test.setBackgroundResource(R.color.filter_selection);
      }
    }
    
    long time = ((long)cal.getTimeInMillis() / 60000) * 60000;

    
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + time + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + time;
    String sort = TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC";
    
    switch (mTimeRangeID) {
      case R.id.button_before1: where = TvBrowserContentProvider.DATA_KEY_ENDTIME + " <= " + time + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + (time - (60000 * 60 * 12)); sort = TvBrowserContentProvider.DATA_KEY_ENDTIME + " DESC"; break;
      case R.id.button_after1: where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + time + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + (time + (60000 * 60 * 12));break;
    }
        
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " COLLATE NOCASE, " + sort);
    
    return loader;
  }

  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    
    switch(mTimeRangeID) {
      case R.id.button_before1: 
      case R.id.button_after1:
      {
        ArrayList<Integer> channelIDList = new ArrayList<Integer>();
        ArrayList<Integer> filterMapList = new ArrayList<Integer>();
        
        int channelColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
        
        while(c.moveToNext()) {
          int channelID = c.getInt(channelColumn);
          
          if(!channelIDList.contains(Integer.valueOf(channelID))) {
            filterMapList.add(Integer.valueOf(c.getPosition()));
            channelIDList.add(Integer.valueOf(channelID));
          }
        }
        
        RunningCursorWrapper wrapper = new RunningCursorWrapper(c);
        wrapper.setFilterMap(filterMapList.toArray(new Integer[filterMapList.size()]));
        c = wrapper;
      }
      break;
    }
    
    mRunningProgramListAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    mRunningProgramListAdapter.swapCursor(null);
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {

    long programID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    UiUtils.createContextMenu(getActivity(), menu, programID);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    UiUtils.showProgramInfo(getActivity(), id);
  }
}
