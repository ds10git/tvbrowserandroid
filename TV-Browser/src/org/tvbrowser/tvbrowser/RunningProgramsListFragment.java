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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.support.v4.util.LongSparseArray;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RunningProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String WHERE_CLAUSE_KEY = "WHERE_CLAUSE_KEY";
  private static final int AT_TIME_ID = -1;
  
  private int mOrientation;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private int mWhereClauseTime;
  private int mTimeRangeID;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mMarkingChangeReceiver;
  
  private static final GradientDrawable BEFORE_GRADIENT;
  private static final GradientDrawable AFTER_GRADIENT;
  
  private ArrayAdapter<ChannelProgramBlock> runningProgramListAdapter;
  
  private ArrayList<ChannelProgramBlock> mProgramBlockList;
  private ArrayList<ChannelProgramBlock> mCurrentViewList;
  
  private SparseArray<BitmapDrawable> mLogoMap;
  private LongSparseArray<String> mMarkingsMap;
  
  private long mCurrentTime;
  
  int mProgramIDColumn;
  int mStartTimeColumn;
  int mEndTimeColumn;
  int mTitleColumn;
  int mPictureColumn;
  int mPictureCopyrightColumn;
  int mCategoryColumn;
  int mGenreColumn;
  int mEpsiodeColumn;
  int mChannelNameColumn;
  int mChannelIDColumn;
  
  private boolean showPicture;
  private boolean showGenre;
  private boolean showEpisode;
  private boolean showInfo;
  private boolean mShowOrderNumber;
  
  private boolean mIsCompactLayout;
    
  private View.OnClickListener mOnCliickListener;
  private View mContextView;
  private long mContextProgramID;
  
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
        if(mWhereClauseTime == -1) {
          startUpdateThread();
        }
        else {
          if(mIsCompactLayout) {
            new Thread() {
              public void run() {
                ViewGroup list = (ViewGroup)getListView();
                
                for(int i = 0; i < list.getChildCount(); i++) {
                  CompactLayoutViewHolder holder = (CompactLayoutViewHolder) list.getChildAt(i).getTag();
                  
                  if(holder.mPrevious.getVisibility() == View.VISIBLE) {
                    if(holder.mPreviousStartTimeValue <= System.currentTimeMillis()) {
                      String markingValues = mMarkingsMap.get(holder.mPreviousProgramID);
                      
                      UiUtils.handleMarkings(getActivity(), null, holder.mPreviousStartTimeValue, holder.mPreviousEndTimeValue, holder.mPrevious, markingValues, handler);
                    }
                  }
                  
                  if(holder.mNowStartTimeValue <= System.currentTimeMillis()) {
                    String markingValues = mMarkingsMap.get(holder.mNowProgramID);
                    
                    UiUtils.handleMarkings(getActivity(), null, holder.mNowStartTimeValue, holder.mNowEndTimeValue, holder.mNow, markingValues, handler);
                  }

                  if(holder.mNextStartTimeValue <= System.currentTimeMillis()) {
                    String markingValues = mMarkingsMap.get(holder.mNextProgramID);
                    
                    UiUtils.handleMarkings(getActivity(), null, holder.mNextStartTimeValue, holder.mNextEndTimeValue, holder.mNext, markingValues, handler);
                  }
                }
              }
            }.start();
          }
          else {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            
            showPicture = pref.getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false);
            showGenre = pref.getBoolean(getResources().getString(R.string.SHOW_GENRE_IN_LISTS), true);
            showEpisode = pref.getBoolean(getResources().getString(R.string.SHOW_EPISODE_IN_LISTS), true);
            showInfo = pref.getBoolean(getResources().getString(R.string.SHOW_INFO_IN_LISTS), true);
            mShowOrderNumber = pref.getBoolean(getResources().getString(R.string.SHOW_SORT_NUMBER_IN_LISTS), true);
            
            runningProgramListAdapter.notifyDataSetChanged();
          }
        }
      }
    };
    
    mMarkingChangeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, final Intent intent) {
        new Thread() {
          public void run() {
            long programID = intent.getLongExtra(SettingConstants.MARKINGS_ID, -1);
            
            if(mMarkingsMap.indexOfKey(programID) >= 0) {
              Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, TvBrowserContentProvider.DATA_KEY_STARTTIME, TvBrowserContentProvider.DATA_KEY_ENDTIME}, null, null, null);
              
              if(c.moveToFirst()) {
                final String newMarkingValues = c.getString(0);
                final long startTime = c.getLong(1);
                final long endTime = c.getLong(2);
                
                mMarkingsMap.put(programID, newMarkingValues);
                
                final View view = getListView().findViewWithTag(programID);
                
                if(view != null) {
                  UiUtils.handleMarkings(getActivity(), null, startTime, endTime, view, newMarkingValues, handler);
                }
              }
              
              c.close();
            }
          }
        }.start();
      }
    };
    
    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    IntentFilter markingsFilter = new IntentFilter(SettingConstants.MARKINGS_CHANGED);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMarkingChangeReceiver, markingsFilter);
  }
  
  public void setWhereClauseTime(Object time) {
    if(time instanceof Integer) {
      int testValue = ((Integer) time).intValue();
      
      if(testValue != mWhereClauseTime) {
        
        Button test = (Button)((View)getView().getParent()).findViewWithTag(Integer.valueOf(mWhereClauseTime));
        
        if(test != null) {
          test.setBackgroundResource(android.R.drawable.list_selector_background);
        }
        
        setTimeRangeID(-2);
              
        mWhereClauseTime = testValue;
        /*
        if(mDataUpdateReceiver != null) {
          LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
        }
        */
        startUpdateThread();
      }
      else {
        setTimeRangeID(AT_TIME_ID);
      }
    }
  }
  
  public void setTimeRangeID(int id) {
    Button test = (Button)((View)getView().getParent()).findViewById(mTimeRangeID);
    
    if(test != null) {
      test.setBackgroundResource(android.R.drawable.list_selector_background);
      test.setPadding(15, 0, 15, 0);
    }
    
    mTimeRangeID = id;
    
    test = (Button)((View)getView().getParent()).findViewById(mTimeRangeID);
    
    if(test != null) {
      test.setBackgroundResource(R.color.filter_selection);
      test.setPadding(15, 0, 15, 0);
    }
    
    test = (Button)((View)getView().getParent()).findViewWithTag(mWhereClauseTime);
    
    mCurrentViewList.clear();
    
    switch(mTimeRangeID) {
      case AT_TIME_ID:
        if(test != null) {
          test.setBackgroundResource(R.color.filter_selection);
        }
        
        for(ChannelProgramBlock block : mProgramBlockList) {
          if(block.mNowStart > 0) {
            mCurrentViewList.add(block);
          }
        }
        break;
      case R.id.button_before1: 
        if(test != null) {
          test.setBackgroundDrawable(BEFORE_GRADIENT);
        }
        
        for(ChannelProgramBlock block : mProgramBlockList) {
          if(block.mPreviousStart > 0) {
            mCurrentViewList.add(block);
          }
        }
        
        break;
      case R.id.button_after1:
        if(test != null) {
          test.setBackgroundDrawable(AFTER_GRADIENT);
        }
        
        for(ChannelProgramBlock block : mProgramBlockList) {
          if(block.mNextStart > 0) {
            mCurrentViewList.add(block);
          }
        }
        break;
    }
    
    runningProgramListAdapter.notifyDataSetChanged();
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(WHERE_CLAUSE_KEY, mWhereClauseTime);
    super.onSaveInstanceState(outState);
  }
  
  private static final class LongLayoutViewHolder {
    View mLayout;
    
    TextView mStartTime;
    TextView mEndTime;
    TextView mChannel;
    TextView mTitle;
    ImageView mPicture;
    TextView mPictureCopyright;
    TextView mInfoLabel;
    TextView mGenreLabel;
    TextView mEpisodeLabel;
    
    public void setColor(int color) {
      mStartTime.setTextColor(color);
      mEndTime.setTextColor(color);
      mChannel.setTextColor(color);
      mTitle.setTextColor(color);
      mPictureCopyright.setTextColor(color);
      mInfoLabel.setTextColor(color);
      mGenreLabel.setTextColor(color);
      mEpisodeLabel.setTextColor(color);
    }
  }
  
  private static final class CompactLayoutViewHolder {
    static final int PREVIOUS = 0;
    static final int NOW = 1;
    static final int NEXT = 2;
    
    long mPreviousProgramID;
    long mNowProgramID;
    long mNextProgramID;
    
    long mPreviousStartTimeValue;
    long mNowStartTimeValue;
    long mNextStartTimeValue;
    
    long mPreviousEndTimeValue;
    long mNowEndTimeValue;
    long mNextEndTimeValue;
    
    int mCurrentOrientation;
        
    TextView mChannel;
    
    View mPrevious;
    View mNow;
    View mNext;
    
    View mSeparator1;
    View mSeparator2;
    
    TextView mPreviousStartTime;
    TextView mPreviousTitle;
    TextView mPreviousEpisode;
    
    TextView mNowStartTime;
    TextView mNowTitle;
    TextView mNowEpisode;
    
    TextView mNextStartTime;
    TextView mNextTitle;
    TextView mNextEpisode;
    
    public void setVisibility(int type, int visibility) {
      switch(type) {
        case PREVIOUS:
          mPrevious.setVisibility(visibility);
          mPreviousStartTime.setVisibility(visibility);
          mPreviousTitle.setVisibility(visibility);
          mPreviousEpisode.setVisibility(visibility);
          break;
        case NOW:
          mNow.setVisibility(visibility);
          mNowStartTime.setVisibility(visibility);
          mNowTitle.setVisibility(visibility);
          mNowEpisode.setVisibility(visibility);
          break;
        case NEXT:
          mNext.setVisibility(visibility);
          mNextStartTime.setVisibility(visibility);
          mNextTitle.setVisibility(visibility);
          mNextEpisode.setVisibility(visibility);
          break;
      }
    }
    
    public void setSeparatorVisibility(int visibility) {
       if(mSeparator1 != null) {
         mSeparator1.setVisibility(visibility);
         mSeparator2.setVisibility(visibility);
       }
    }
    
    public boolean orientationChanged(int orientation) {
      return mCurrentOrientation != orientation;
    }
    
    public void setColor(int type, int color) {
      switch (type) {
        case PREVIOUS:
          mPreviousEpisode.setTextColor(color);
          mPreviousTitle.setTextColor(color);
          mPreviousStartTime.setTextColor(color);
          break;
        case NOW:
          mNowEpisode.setTextColor(color);
          mNowTitle.setTextColor(color);
          mNowStartTime.setTextColor(color);
          break;
        case NEXT:
          mNextEpisode.setTextColor(color);
          mNextTitle.setTextColor(color);
          mNextStartTime.setTextColor(color);
          break;
      }
    }
  }
  
  
  private boolean fillCompactLayout(final CompactLayoutViewHolder viewHolder, final int type, final ChannelProgramBlock block, final java.text.DateFormat timeFormat, final int DEFAULT_TEXT_COLOR, boolean channelSet) {
    TextView startTimeView = null;
    TextView titleView = null;
    TextView episodeView = null;
    View layout = null;
    
    long startTime = 0;
    long endTime = 0;
    long programID = -1;
    String title = null;
    String episode = null;
    
    switch(type) {
      case CompactLayoutViewHolder.PREVIOUS:
        layout = viewHolder.mPrevious;
        startTimeView = viewHolder.mPreviousStartTime;
        titleView = viewHolder.mPreviousTitle;
        episodeView = viewHolder.mPreviousEpisode;
        startTime = block.mPreviousStart;
        endTime = block.mPreviousEnd;
        title = block.mPreviousTitle;
        episode = block.mPreviousEpisode;
        programID = block.mPreviousProgramID;
        break;
      case CompactLayoutViewHolder.NOW:
        layout = viewHolder.mNow;
        startTimeView = viewHolder.mNowStartTime;
        titleView = viewHolder.mNowTitle;
        episodeView = viewHolder.mNowEpisode;
        startTime = block.mNowStart;
        endTime = block.mNowEnd;
        title = block.mNowTitle;
        episode = block.mNowEpisode;
        programID = block.mNowProgramID;
        break;
      case CompactLayoutViewHolder.NEXT:
        layout = viewHolder.mNext;
        startTimeView = viewHolder.mNextStartTime;
        titleView = viewHolder.mNextTitle;
        episodeView = viewHolder.mNextEpisode;
        startTime = block.mNextStart;
        endTime = block.mNextEnd;
        title = block.mNextTitle;
        episode = block.mNextEpisode;
        programID = block.mNextProgramID;
        break;
    }
    
    if(startTime > 0 && title != null) {
      switch(type) {
        case CompactLayoutViewHolder.PREVIOUS:
          viewHolder.mPreviousStartTimeValue = startTime;
          viewHolder.mPreviousEndTimeValue = endTime;
          viewHolder.mPreviousProgramID = programID;
          break;
        case CompactLayoutViewHolder.NOW:
          viewHolder.mNowStartTimeValue = startTime;
          viewHolder.mNowEndTimeValue = endTime;
          viewHolder.mNowProgramID = programID;
          break;
        case CompactLayoutViewHolder.NEXT:
          viewHolder.mNextStartTimeValue = startTime;
          viewHolder.mNextEndTimeValue = endTime;
          viewHolder.mNextProgramID = programID;
          break;
      }
      
      viewHolder.setVisibility(type, View.VISIBLE);
    
      startTimeView.setText(timeFormat.format(startTime));
      titleView.setText(title);
      
      if(!showEpisode || episode == null || episode.trim().length() == 0) {
        episodeView.setVisibility(View.GONE);
      }
      else {
        episodeView.setText(episode);
        episodeView.setVisibility(View.VISIBLE);
      }
      
      if(endTime <= System.currentTimeMillis()) {
        viewHolder.setColor(type, SettingConstants.EXPIRED_COLOR);
      }
      else {
        viewHolder.setColor(type, DEFAULT_TEXT_COLOR);
      }
      
      if(!channelSet) {
        BitmapDrawable logo = mLogoMap.get(block.mChannelID);
        
        viewHolder.mChannel.setCompoundDrawables(null, logo, null, null);
        
        String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(block.mChannelName);
        
        if(shortName == null) {
          shortName = block.mChannelName;
        }
        
        if(mShowOrderNumber) {
          shortName = block.mChannelOrderNumber + ". " + shortName;
        }
        
        viewHolder.mChannel.setText(shortName);
        
        channelSet = true;
      }
      
      layout.setTag(Long.valueOf(programID));
      layout.setOnClickListener(mOnCliickListener);
      
      final String markingsValue = mMarkingsMap.get(programID);
      
      if(startTime <= System.currentTimeMillis() || (markingsValue != null && markingsValue.trim().length() > 0)) {
        final long startTime1 = startTime;
        final long endTime1 = endTime;
        final View layout1 = layout;
        
        new Thread() {
          public void run() {
            UiUtils.handleMarkings(getActivity(), null, startTime1, endTime1, layout1, markingsValue, handler);
          }
        }.start();
      }
      else {
        layout.setBackgroundDrawable(getActivity().getResources().getDrawable(android.R.drawable.list_selector_background));
      }
    }
    else {
      viewHolder.setVisibility(type, View.GONE);
      
      if(type == CompactLayoutViewHolder.PREVIOUS) {
        viewHolder.setSeparatorVisibility(View.GONE);
      }
    }
    
    return channelSet;
  }
  
  private View getCompactView(View convertView, ViewGroup parent, java.text.DateFormat timeFormat, ChannelProgramBlock block, int DEFAULT_TEXT_COLOR) {
    CompactLayoutViewHolder viewHolder = null;
    
    if(convertView == null || convertView.getTag() instanceof LongLayoutViewHolder || ((CompactLayoutViewHolder)convertView.getTag()).orientationChanged(mOrientation)) {
      convertView = getActivity().getLayoutInflater().inflate(R.layout.compact_program_panel, parent, false);
      
      viewHolder = new CompactLayoutViewHolder();
      
      viewHolder.mCurrentOrientation = mOrientation;
      viewHolder.mChannel = (TextView)convertView.findViewById(R.id.running_compact_channel_label);
      
      viewHolder.mSeparator1 = convertView.findViewById(R.id.running_separator_1);
      viewHolder.mSeparator2 = convertView.findViewById(R.id.running_separator_2);
      
      viewHolder.mPrevious = convertView.findViewById(R.id.running_compact_previous);
      viewHolder.mNow = convertView.findViewById(R.id.running_compact_now);
      viewHolder.mNext = convertView.findViewById(R.id.running_compact_next);
      
      registerForContextMenu(viewHolder.mPrevious);
      registerForContextMenu(viewHolder.mNow);
      registerForContextMenu(viewHolder.mNext);
      
      viewHolder.mPreviousStartTime = (TextView)convertView.findViewById(R.id.running_compact_previous_start);
      viewHolder.mNowStartTime = (TextView)convertView.findViewById(R.id.running_compact_now_start);
      viewHolder.mNextStartTime = (TextView)convertView.findViewById(R.id.running_compact_next_start);
      
      viewHolder.mPreviousTitle = (TextView)convertView.findViewById(R.id.running_compact_previous_title);
      viewHolder.mNowTitle = (TextView)convertView.findViewById(R.id.running_compact_now_title);
      viewHolder.mNextTitle = (TextView)convertView.findViewById(R.id.running_compact_next_title);
      
      viewHolder.mPreviousEpisode = (TextView)convertView.findViewById(R.id.running_compact_previous_episode);
      viewHolder.mNowEpisode = (TextView)convertView.findViewById(R.id.running_compact_now_episode);
      viewHolder.mNextEpisode = (TextView)convertView.findViewById(R.id.running_compact_next_episode);
      
      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (CompactLayoutViewHolder)convertView.getTag();
    }
    
    if(viewHolder != null && block != null /*&& mCurrentCursor != null && !mCurrentCursor.isClosed()*/) {
      boolean channelSet = false;
      
      if(mWhereClauseTime != -1) {
        viewHolder.setSeparatorVisibility(View.VISIBLE);
        channelSet = fillCompactLayout(viewHolder, CompactLayoutViewHolder.PREVIOUS, block, timeFormat, DEFAULT_TEXT_COLOR, channelSet);
      }
      else {       
        viewHolder.setVisibility(CompactLayoutViewHolder.PREVIOUS, View.GONE);
        viewHolder.setSeparatorVisibility(View.GONE);
      }
      
      channelSet = fillCompactLayout(viewHolder, CompactLayoutViewHolder.NOW, block, timeFormat, DEFAULT_TEXT_COLOR, channelSet);
      channelSet = fillCompactLayout(viewHolder, CompactLayoutViewHolder.NEXT, block, timeFormat, DEFAULT_TEXT_COLOR, channelSet);
      
      ViewGroup group = (ViewGroup)convertView;
      
      for(int i = 0; i < group.getChildCount(); i++) {
        View child = group.getChildAt(i);
        
        if(child instanceof LinearLayout) {
          RelativeLayout.LayoutParams para = (RelativeLayout.LayoutParams)child.getLayoutParams();
        
          para.height = -2;
        }
      }
    }
    
    return convertView;
  }
  
  private View getLongView(View convertView, ViewGroup parent, java.text.DateFormat timeFormat, ChannelProgramBlock block, int DEFAULT_TEXT_COLOR) {
    LongLayoutViewHolder viewHolder = null;
    
    if(convertView == null || convertView.getTag() instanceof CompactLayoutViewHolder) {
      convertView = getActivity().getLayoutInflater().inflate(R.layout.running_list_entries, parent, false);
      
      viewHolder = new LongLayoutViewHolder();
      
      viewHolder.mLayout = convertView.findViewById(R.id.running_layout);
      viewHolder.mLayout.setBackgroundResource(android.R.drawable.list_selector_background);
      
      viewHolder.mStartTime = (TextView)convertView.findViewById(R.id.startTimeLabel);
      viewHolder.mEndTime = (TextView)convertView.findViewById(R.id.endTimeLabel);
      viewHolder.mChannel = (TextView)convertView.findViewById(R.id.channelLabel);
      viewHolder.mTitle = (TextView)convertView.findViewById(R.id.titleLabel);
      viewHolder.mPicture = (ImageView)convertView.findViewById(R.id.picture);
      viewHolder.mPictureCopyright = (TextView)convertView.findViewById(R.id.picture_copyright);
      viewHolder.mInfoLabel = (TextView)convertView.findViewById(R.id.info_label);
      viewHolder.mGenreLabel = (TextView)convertView.findViewById(R.id.genre_label);
      viewHolder.mEpisodeLabel = (TextView)convertView.findViewById(R.id.episodeLabel);
      
      convertView.setTag(viewHolder);
      
      registerForContextMenu(viewHolder.mLayout);
    }
    else {
      viewHolder = (LongLayoutViewHolder)convertView.getTag();
    }
    
    long startTime = 0;
    long endTime = 0;
    String title = null;
    long programID = -1;
    String markingValue = null;
    
    switch(mTimeRangeID) {
      case R.id.button_before1: 
        startTime = block.mPreviousStart;
        endTime = block.mPreviousEnd;
        title = block.mPreviousTitle;
        programID = block.mPreviousProgramID;
        break; 
      case R.id.button_after1:
        startTime = block.mNextStart;
        endTime = block.mNextEnd;
        title = block.mNextTitle;
        programID = block.mNextProgramID;
        break;
      default:
        startTime = block.mNowStart;
        endTime = block.mNowEnd;
        title = block.mNowTitle;
        programID = block.mNowProgramID;
        break;
    };
    
    if(startTime > 0 && title != null) {
      Long availableProgramID = (Long)viewHolder.mLayout.getTag();
      
      if(availableProgramID == null || availableProgramID.longValue() != programID) {
        String channel = block.mChannelName;
        String genre = null;
        String episode = null;
        String category = null;
        String pictureCopyright = null;
        
        if(mShowOrderNumber) {
          channel = block.mChannelOrderNumber + ". " + channel;
        }
        
        byte[] picture = null;
        
        switch(mTimeRangeID) {
          case R.id.button_before1:
            genre = block.mPreviousGenre;
            episode = block.mPreviousEpisode;
            category = block.mPreviousCategory;
            pictureCopyright = block.mPreviousPictureCopyright;
            picture = block.mPreviousPicture;
            break; 
          case R.id.button_after1:
            genre = block.mNextGenre;
            episode = block.mNextEpisode;
            category = block.mNextCategory;
            pictureCopyright = block.mNextPictureCopyright;
            picture = block.mNextPicture;
            break;
          default:
            genre = block.mNowGenre;
            episode = block.mNowEpisode;
            category = block.mNowCategory;
            pictureCopyright = block.mNowPictureCopyright;
            picture = block.mNowPicture;
            break;
        };
        
        viewHolder.mStartTime.setText(timeFormat.format(new Date(startTime)));
        viewHolder.mEndTime.setText(getResources().getString(R.string.running_until) + " " + timeFormat.format(new Date(endTime)));
        viewHolder.mTitle.setText(title);
        viewHolder.mChannel.setText(channel);
        
        viewHolder.mLayout.setTag(programID);
        viewHolder.mLayout.setOnClickListener(mOnCliickListener);
        
        if(showPicture) {
          if(pictureCopyright != null && pictureCopyright.trim().length() > 0 && picture != null && picture.length > 0) {
            Bitmap logo = BitmapFactory.decodeByteArray(picture, 0, picture.length);
            
            BitmapDrawable l = new BitmapDrawable(getResources(), logo);
            
            l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
            
            if(endTime <= System.currentTimeMillis()) {
              l.setColorFilter(getActivity().getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
            }
            
            viewHolder.mPicture.setImageDrawable(l);
            viewHolder.mPictureCopyright.setText(pictureCopyright);
            
            viewHolder.mPicture.setVisibility(View.VISIBLE);
            viewHolder.mPictureCopyright.setVisibility(View.VISIBLE);
          }
          else {
            viewHolder.mPicture.setVisibility(View.GONE);
            viewHolder.mPictureCopyright.setVisibility(View.GONE);
          }
        }
        else {
          viewHolder.mPicture.setVisibility(View.GONE);
          viewHolder.mPictureCopyright.setVisibility(View.GONE);
        }
        
        if(showGenre && genre != null && genre.trim().length() > 0) {
          viewHolder.mGenreLabel.setVisibility(View.VISIBLE);
          viewHolder.mGenreLabel.setText(genre);
        }
        else {
          viewHolder.mGenreLabel.setVisibility(View.GONE);
        }
        
        if(showEpisode && episode != null && episode.trim().length() > 0) {
          viewHolder.mEpisodeLabel.setVisibility(View.VISIBLE);
          viewHolder.mEpisodeLabel.setText(episode);
        }
        else {
          viewHolder.mEpisodeLabel.setVisibility(View.GONE);
        }
        
        if(showInfo && category != null && category.trim().length() > 0) {
          viewHolder.mInfoLabel.setVisibility(View.VISIBLE);
          viewHolder.mInfoLabel.setText(category);
        }
        else {
          viewHolder.mInfoLabel.setVisibility(View.GONE);
        }
        
        if(endTime <= System.currentTimeMillis()) {
          viewHolder.setColor(SettingConstants.EXPIRED_COLOR);
        }
        else {
          viewHolder.setColor(DEFAULT_TEXT_COLOR);
        }
      }
      
      final String markingsValue = mMarkingsMap.get(programID);
      
      if(startTime <= System.currentTimeMillis() || (markingsValue != null && markingsValue.trim().length() > 0)) {
        final long startTime1 = startTime;
        final long endTime1 = endTime;
        final View layout1 = viewHolder.mLayout;
        
        new Thread() {
          public void run() {
            UiUtils.handleMarkings(getActivity(), null, startTime1, endTime1, layout1, markingsValue, handler);
          }
        }.start();
      }
      else {
        viewHolder.mLayout.setBackgroundDrawable(getActivity().getResources().getDrawable(android.R.drawable.list_selector_background));
      }
      /*
      final long startTime1 = startTime;
      final long endTime1 = endTime;
      final View layout1 = convertView;
      final String markingValue1 = markingValue;
      
      new Thread() {
        public void run() {
          UiUtils.handleMarkings(getActivity(), null, startTime1, endTime1, layout1, markingValue1, handler);
        }
      }.start();*/
    }
    
    return convertView;
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
    
    mLogoMap = new SparseArray<BitmapDrawable>();
    mTimeRangeID = -1;
    
    mMarkingsMap = new LongSparseArray<String>();
    
    mOnCliickListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Long tag = (Long)v.getTag();
        
        if(tag != null) {
          UiUtils.showProgramInfo(getActivity(), tag.longValue());
        }
      }
    };
        
    mProgramBlockList = new ArrayList<RunningProgramsListFragment.ChannelProgramBlock>();
    mCurrentViewList = new ArrayList<RunningProgramsListFragment.ChannelProgramBlock>();
    
    java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(getActivity());
    String value = ((SimpleDateFormat)mTimeFormat).toLocalizedPattern();
    
    if((value.charAt(0) == 'H' && value.charAt(1) != 'H') || (value.charAt(0) == 'h' && value.charAt(1) != 'h')) {
      value = value.charAt(0) + value;
    }
    
    final java.text.DateFormat timeFormat = new SimpleDateFormat(value, Locale.getDefault());
    int[] attrs = new int[] { android.R.attr.textColorSecondary };
    TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
    final int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
    a.recycle();
        
    runningProgramListAdapter = new ArrayAdapter<RunningProgramsListFragment.ChannelProgramBlock>(getActivity(), R.layout.running_list_entries, mCurrentViewList) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        ChannelProgramBlock block = getItem(position);
        
        if(mIsCompactLayout) {
          return getCompactView(convertView, parent, timeFormat, block, DEFAULT_TEXT_COLOR);
        }
        else {
          return getLongView(convertView, parent, timeFormat, block, DEFAULT_TEXT_COLOR);
        }
      }
    };
    
    setListAdapter(runningProgramListAdapter);
    
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
    if(mMarkingChangeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMarkingChangeReceiver);
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
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    mIsCompactLayout = pref.getString(getResources().getString(R.string.RUNNING_PROGRAMS_LAYOUT), SettingConstants.DEFAULT_RUNNING_PROGRAMS_LIST_LAYOUT).equals("1");
    showPicture = pref.getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false);
    showGenre = pref.getBoolean(getResources().getString(R.string.SHOW_GENRE_IN_LISTS), true);
    showEpisode = pref.getBoolean(getResources().getString(R.string.SHOW_EPISODE_IN_LISTS), true);
    showInfo = pref.getBoolean(getResources().getString(R.string.SHOW_INFO_IN_LISTS), true);
    mShowOrderNumber = pref.getBoolean(getResources().getString(R.string.SHOW_SORT_NUMBER_IN_LISTS), true);
    
    if(showPicture) {
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
    projection[11] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    projection[12] = TvBrowserContentProvider.CHANNEL_KEY_NAME;
    projection[13] = TvBrowserContentProvider.CHANNEL_KEY_LOGO;
    
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 30);
    
    Calendar now = Calendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    
    if(mWhereClauseTime >= 0) {
      cal.set(Calendar.HOUR_OF_DAY, mWhereClauseTime / 60);
      cal.set(Calendar.MINUTE, mWhereClauseTime % 60);
      
      if(pref.getBoolean(getResources().getString(R.string.RUNNING_PROGRAMS_NEXT_DAY), true)) {
        int test = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        
        if((test - mWhereClauseTime) > 180) {
          cal.add(Calendar.DAY_OF_YEAR, 1);
        }
      }
    }
    else {
      cal.setTimeInMillis(System.currentTimeMillis());
    }
    
    if(getView().getParent() != null) {
      Button test = (Button)((View)getView().getParent()).findViewWithTag(Integer.valueOf(mWhereClauseTime));
      
      if(test != null) {
        test.setBackgroundResource(R.color.filter_selection);
      }      
    }
    
    mCurrentTime = ((long)cal.getTimeInMillis() / 60000) * 60000;

    String sort = TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC";
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " <= " + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + (mCurrentTime - (60000 * 60 * 12)) + " ) ";
    
    if(mWhereClauseTime == -1) {
      where = " ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + (mCurrentTime + (60000 * 60 * 12)) + " ) ";
    }
    else {
      where += " OR ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + (mCurrentTime + (60000 * 60 * 12)) + " ) ";
    }
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " COLLATE NOCASE, " + sort);
    
    return loader;
  }
  
  private static final class ChannelProgramBlock {
    public int mChannelID;
    private String mChannelName;
    private int mChannelOrderNumber;
    
    public int mPreviousPosition;
    public long mPreviousStart;
    public long mPreviousEnd;
    public long mPreviousProgramID;
    public String mPreviousTitle;
    public String mPreviousEpisode;
    public String mPreviousGenre;
    public String mPreviousCategory;
    public String mPreviousPictureCopyright;
    public byte[] mPreviousPicture;

    public int mNowPosition;
    public long mNowStart;
    public long mNowEnd;
    public long mNowProgramID;
    public String mNowTitle;
    public String mNowEpisode;
    public String mNowGenre;
    public String mNowCategory;
    public String mNowPictureCopyright;
    public byte[] mNowPicture;
    
    public int mNextPosition;
    public long mNextStart;
    public long mNextEnd;
    public long mNextProgramID;
    public String mNextTitle;
    public String mNextEpisode;
    public String mNextGenre;
    public String mNextCategory;
    public String mNextPictureCopyright;
    public byte[] mNextPicture;

    public boolean mIsComplete;
    
    public ChannelProgramBlock() {
      mIsComplete = false;
    }
  }

  @Override
  public synchronized void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, final Cursor c) {
    SparseArray<ChannelProgramBlock> channelProgramMap = new SparseArray<ChannelProgramBlock>();
    SparseArray<ChannelProgramBlock> currentProgramMap = new SparseArray<ChannelProgramBlock>();
    
    mProgramBlockList.clear();
    mCurrentViewList.clear();
    mMarkingsMap.clear();
    
    mProgramIDColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
    mStartTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
    mEndTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
    mTitleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
    mPictureColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);
    mPictureCopyrightColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT);
    mCategoryColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES);
    mGenreColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE);
    mEpsiodeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
    mChannelNameColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
    mChannelIDColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    int channelOrderColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    int logoColumn =  c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
    int markingColumn =  c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES);
    
    while(c.moveToNext()) {
      int channelID = c.getInt(mChannelIDColumn);
      
      ChannelProgramBlock block = channelProgramMap.get(channelID);
      
      if(block == null) {
        block = new ChannelProgramBlock();
        
        channelProgramMap.put(channelID, block);
        mProgramBlockList.add(block);
      }
      
      if(!block.mIsComplete) {
        long startTime = c.getLong(mStartTimeColumn);
        long endTime = c.getLong(mEndTimeColumn);
        long programID = c.getLong(mProgramIDColumn);
        String title = c.getString(mTitleColumn);
        String episode = c.getString(mEpsiodeColumn);
        String markingValue = c.getString(markingColumn);
        
        String channelName = c.getString(mChannelNameColumn);
        int channelOrderNumber = c.getInt(channelOrderColumn);

        String genre = null;
        String category = null;
        String pictureCopyright = null;
        byte[] picture = null;

        if(!mIsCompactLayout) {
          if(showGenre) {
            genre = c.getString(mGenreColumn);
          }
          
          if(showInfo) {
            category = IOUtils.getInfoString(c.getInt(mCategoryColumn), getResources());
          }
          
          if(showPicture && mPictureColumn > -1) {
            pictureCopyright = c.getString(mPictureCopyrightColumn);
            picture = c.getBlob(mPictureColumn);
          }
        }
        
        BitmapDrawable logo = mLogoMap.get(channelID);
        
        if(logo == null) {
          if(mLogoMap.indexOfKey(channelID) < 0) {
            byte[] logoData = c.getBlob(logoColumn);
            
            if(logoData != null && logoData.length > 0) {
              Bitmap logoBitmap = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
              
              logo = new BitmapDrawable(getResources(), logoBitmap);
              
              float scale = UiUtils.convertDpToPixel(15, getResources()) / (float)logoBitmap.getHeight();
              
              logo.setBounds(0, 0, (int)(logoBitmap.getWidth() * scale), (int)(logoBitmap.getHeight() * scale));
            }
            
            mLogoMap.put(channelID, logo);
          }
        }
        
        block.mChannelID = channelID;
        block.mChannelName = channelName;
        block.mChannelOrderNumber = channelOrderNumber;
        
        if(startTime <= mCurrentTime) {
          if(endTime <= mCurrentTime) {
            block.mPreviousPosition = c.getPosition();
            block.mPreviousProgramID = programID;
            block.mPreviousStart = startTime;
            block.mPreviousEnd = endTime;
            block.mPreviousTitle = title;
            block.mPreviousEpisode = episode;
            block.mPreviousGenre = genre;
            block.mPreviousPicture = picture;
            block.mPreviousPictureCopyright = pictureCopyright;
            block.mPreviousCategory = category;
          }
          else if(startTime <= mCurrentTime && mCurrentTime < endTime) {
            block.mNowPosition = c.getPosition();
            block.mNowProgramID = programID;
            block.mNowStart = startTime;
            block.mNowEnd = endTime;
            block.mNowTitle = title;
            block.mNowEpisode = episode;
            block.mNowGenre = genre;
            block.mNowPicture = picture;
            block.mNowPictureCopyright = pictureCopyright;
            block.mNowCategory = category;
            
            if(currentProgramMap.indexOfKey(channelID) <= 0) { 
              currentProgramMap.put(channelID, block);
              mCurrentViewList.add(block);
            }
          }
        }
        else {
          block.mNextPosition = c.getPosition();
          block.mNextStart = startTime;
          block.mNextEnd = endTime;
          block.mNextProgramID = programID;
          block.mNextTitle = title;
          block.mNextEpisode = episode;
          block.mNextGenre = genre;
          block.mNextPicture = picture;
          block.mNextPictureCopyright = pictureCopyright;
          block.mNextCategory = category;
          
          block.mIsComplete = true;
        }
        
        mMarkingsMap.put(programID, markingValue);
      }
    }
    
    c.close();
    currentProgramMap.clear();
    channelProgramMap.clear();
    runningProgramListAdapter.notifyDataSetChanged();
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    mCurrentViewList.clear();
    mProgramBlockList.clear();
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    Long test = (Long)v.getTag();
    
    if(test != null) {
      mContextProgramID = test.longValue();
      mContextView = v;
      UiUtils.createContextMenu(getActivity(), menu, mContextProgramID);
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    UiUtils.handleContextMenuSelection(getActivity(), item, mContextProgramID, mContextView);
    
    return false;
  }
  
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    
    mOrientation = newConfig.orientation;
    
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          // TODO Auto-generated method stub
          runningProgramListAdapter.notifyDataSetChanged();
        }
      });
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
      handler.post(new Runnable() {
        
        @Override
        public void run() {
          // TODO Auto-generated method stub
          runningProgramListAdapter.notifyDataSetChanged();
        }
      });
    }
  }
}
