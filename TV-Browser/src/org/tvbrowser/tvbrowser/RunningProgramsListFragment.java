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
import java.util.HashMap;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class RunningProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String WHERE_CLAUSE_KEY = "WHERE_CLAUSE_KEY";
  private static final int AT_TIME_ID = -1;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private int mWhereClauseTime;
  private int mTimeRangeID;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  
  private static final GradientDrawable BEFORE_GRADIENT;
  private static final GradientDrawable AFTER_GRADIENT;
  
  private ArrayAdapter<ChannelProgramBlock> runningProgramListAdapter;
  private Cursor mCurrentCursor;
  
  private ArrayList<ChannelProgramBlock> mProgramBlockList;
  private ArrayList<ChannelProgramBlock> mCurrentViewList;
  
  private long mCurrentTime;
  
  private boolean showPicture;
  private boolean showGenre;
  private boolean showEpisode;
  private boolean showInfo;
  
  private boolean mIsCompactLayout;
  
  private int mProgramIDColumn;
  private int mStartTimeColumn;
  private int mEndTimeColumn;
  private int mTitleColumn;
  private int mChannelNameColumn;
  private int mPictureColumn;
  private int mPictureCopyrightColumn;
  private int mCategoryColumn;
  private int mGenreColumn;
  private int mEpsiodeColumn;
  
  private View.OnClickListener mOnCliickListener;
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
          SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
          
          showPicture = pref.getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false);
          showGenre = pref.getBoolean(getResources().getString(R.string.SHOW_GENRE_IN_LISTS), true);
          showEpisode = pref.getBoolean(getResources().getString(R.string.SHOW_EPISODE_IN_LISTS), true);
          showInfo = pref.getBoolean(getResources().getString(R.string.SHOW_INFO_IN_LISTS), true);
          
          runningProgramListAdapter.notifyDataSetChanged();
        }
      }
    };
    
    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
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
        
        if(mDataUpdateReceiver != null) {
          LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
        }
        
        startUpdateThread();
      }
      else {
        setTimeRangeID(AT_TIME_ID);
      }
    }
  }
  
  public void setTimeRangeID(int id) {Log.d("info3","hier");
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
          if(block.mNowPosition >= 0) {
            mCurrentViewList.add(block);
          }
        }
        break;
      case R.id.button_before1: 
        if(test != null) {
          test.setBackgroundDrawable(BEFORE_GRADIENT);
        }
        
        for(ChannelProgramBlock block : mProgramBlockList) {
          if(block.mPreviousPosition >= 0) {
            mCurrentViewList.add(block);
          }
        }
        
        break;
      case R.id.button_after1:
        if(test != null) {
          test.setBackgroundDrawable(AFTER_GRADIENT);
        }
        
        for(ChannelProgramBlock block : mProgramBlockList) {
          if(block.mNextPosition >= 0) {
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
    TextView mChannel;
    
    View mPrevious;
    View mNow;
    View mNext;
    
    TextView mPreviousStartTime;
    TextView mPreviousTitle;
    TextView mPreviousEpisode;
    
    TextView mNowStartTime;
    TextView mNowTitle;
    TextView mNowEpisode;
    
    TextView mNextStartTime;
    TextView mNextTitle;
    TextView mNextEpisode;
    
    public void setPreviousVisibility(int visibility) {
      mPrevious.setVisibility(visibility);
      mPreviousStartTime.setVisibility(visibility);
      mPreviousTitle.setVisibility(visibility);
      mPreviousEpisode.setVisibility(visibility);
    }
    
    public void setPreviousColor(int color) {
      mPreviousEpisode.setTextColor(color);
      mPreviousTitle.setTextColor(color);
      mPreviousStartTime.setTextColor(color);
    }
    
    public void setNowVisibility(int visibility) {
      mNow.setVisibility(visibility);
      mNowStartTime.setVisibility(visibility);
      mNowTitle.setVisibility(visibility);
      mNowEpisode.setVisibility(visibility);
    }
    
    public void setNowColor(int color) {
      mNowEpisode.setTextColor(color);
      mNowTitle.setTextColor(color);
      mNowStartTime.setTextColor(color);
    }
    
    public void setNextVisibility(int visibility) {
      mNext.setVisibility(visibility);
      mNextStartTime.setVisibility(visibility);
      mNextTitle.setVisibility(visibility);
      mNextEpisode.setVisibility(visibility);
    }

    public void setNextColor(int color) {
      mNextEpisode.setTextColor(color);
      mNextTitle.setTextColor(color);
      mNextStartTime.setTextColor(color);
    }
  }
  
  
  private View getCompactView(View convertView, ViewGroup parent, java.text.DateFormat timeFormat, ChannelProgramBlock block, int DEFAULT_TEXT_COLOR) {
    CompactLayoutViewHolder viewHolder = null;
    
    if(convertView == null || convertView.getTag() instanceof LongLayoutViewHolder) {
      convertView = getActivity().getLayoutInflater().inflate(R.layout.compact_program_panel, parent, false);
      
      viewHolder = new CompactLayoutViewHolder();
      
      viewHolder.mChannel = (TextView)convertView.findViewById(R.id.running_compact_channel_label);
      
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
    
    if(viewHolder != null && block != null && mCurrentCursor != null && !mCurrentCursor.isClosed()) {
      boolean channelSet = false;
      
      if(mWhereClauseTime != -1 && block.mPreviousPosition >= 0) {
        if(mCurrentCursor.moveToPosition(block.mPreviousPosition)) {
          viewHolder.setPreviousVisibility(View.VISIBLE);
          
          long startTime = mCurrentCursor.getLong(mStartTimeColumn);
          String title = mCurrentCursor.getString(mTitleColumn);
          String episode = mCurrentCursor.getString(mEpsiodeColumn);
          
          viewHolder.mPreviousStartTime.setText(timeFormat.format(new Date(startTime)));
          viewHolder.mPreviousTitle.setText(title);
          
          if(!showEpisode || episode == null || episode.trim().length() == 0) {
            viewHolder.mPreviousEpisode.setVisibility(View.GONE);
          }
          else {
            viewHolder.mPreviousEpisode.setText(episode);
            viewHolder.mPreviousEpisode.setVisibility(View.VISIBLE);
          }
          
          long endTime = mCurrentCursor.getLong(mEndTimeColumn);
          
          if(endTime <= System.currentTimeMillis()) {
            viewHolder.setPreviousColor(SettingConstants.EXPIRED_COLOR);
          }
          else {
            viewHolder.setPreviousColor(DEFAULT_TEXT_COLOR);
          }

          String name = mCurrentCursor.getString(mChannelNameColumn);
          
          String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
          
          if(shortName != null) {
            viewHolder.mChannel.setText(shortName);
          }
          else {
            viewHolder.mChannel.setText(name);
          }
          
          viewHolder.mPrevious.setTag(Long.valueOf(mCurrentCursor.getLong(mProgramIDColumn)));
          viewHolder.mPrevious.setOnClickListener(mOnCliickListener);
          UiUtils.handleMarkings(getActivity(), mCurrentCursor, viewHolder.mPrevious, null);
        }
        else {
          viewHolder.setPreviousVisibility(View.GONE);
        }
      }
      else {
        viewHolder.setPreviousVisibility(View.GONE);
      }
      
      if(block.mNowPosition >= 0) {
        if(mCurrentCursor.moveToPosition(block.mNowPosition)) {
          viewHolder.setNowVisibility(View.VISIBLE);
          
          long startTime = mCurrentCursor.getLong(mStartTimeColumn);
          String title = mCurrentCursor.getString(mTitleColumn);
          String episode = mCurrentCursor.getString(mEpsiodeColumn);
          
          viewHolder.mNowStartTime.setText(timeFormat.format(new Date(startTime)));
          viewHolder.mNowTitle.setText(title);
          
          if(!showEpisode || episode == null || episode.trim().length() == 0) {
            viewHolder.mNowEpisode.setVisibility(View.GONE);
          }
          else {
            viewHolder.mNowEpisode.setText(episode);
            viewHolder.mNowEpisode.setVisibility(View.VISIBLE);
          }
          
          long endTime = mCurrentCursor.getLong(mEndTimeColumn);
          
          if(endTime <= System.currentTimeMillis()) {
            viewHolder.setNowColor(SettingConstants.EXPIRED_COLOR);
          }
          else {
            viewHolder.setNowColor(DEFAULT_TEXT_COLOR);
          }

          if(!channelSet) {
            String name = mCurrentCursor.getString(mChannelNameColumn);
            
            String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
            
            if(shortName != null) {
              viewHolder.mChannel.setText(shortName);
            }
            else {
              viewHolder.mChannel.setText(name);
            }
          }
          
          viewHolder.mNow.setTag(Long.valueOf(mCurrentCursor.getLong(mProgramIDColumn)));
          viewHolder.mNow.setOnClickListener(mOnCliickListener);
          UiUtils.handleMarkings(getActivity(), mCurrentCursor, viewHolder.mNow, null);
        }
        else {
          viewHolder.setNowVisibility(View.GONE);
        }
      }
      else {
        viewHolder.setNowVisibility(View.GONE);
      }
      
      if(block.mNextPosition >= 0) {
        if(mCurrentCursor.moveToPosition(block.mNextPosition)) {
          viewHolder.setNextVisibility(View.VISIBLE);
          
          long startTime = mCurrentCursor.getLong(mStartTimeColumn);
          String title = mCurrentCursor.getString(mTitleColumn);
          String episode = mCurrentCursor.getString(mEpsiodeColumn);
          
          viewHolder.mNextStartTime.setText(timeFormat.format(new Date(startTime)));
          viewHolder.mNextTitle.setText(title);
          
          if(!showEpisode || episode == null || episode.trim().length() == 0) {
            viewHolder.mNextEpisode.setVisibility(View.GONE);
          }
          else {
            viewHolder.mNextEpisode.setText(episode);
            viewHolder.mNextEpisode.setVisibility(View.VISIBLE);
          }
          
          long endTime = mCurrentCursor.getLong(mEndTimeColumn);
          
          if(endTime <= System.currentTimeMillis()) {
            viewHolder.setNextColor(SettingConstants.EXPIRED_COLOR);
          }
          else {
            viewHolder.setNextColor(DEFAULT_TEXT_COLOR);
          }
          
          if(!channelSet) {
            String name = mCurrentCursor.getString(mChannelNameColumn);
            
            String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
            
            if(shortName != null) {
              viewHolder.mChannel.setText(shortName);
            }
            else {
              viewHolder.mChannel.setText(name);
            }
          }
                    
          viewHolder.mNext.setTag(Long.valueOf(mCurrentCursor.getLong(mProgramIDColumn)));
          viewHolder.mNext.setOnClickListener(mOnCliickListener);
          UiUtils.handleMarkings(getActivity(), mCurrentCursor, viewHolder.mNext, null);
        }
        else {
          viewHolder.setNextVisibility(View.GONE);
        }
      }
      else {
        viewHolder.setNextVisibility(View.GONE);
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
    
    int cursorPosition = -1;
    
    switch(mTimeRangeID) {
      case R.id.button_before1: cursorPosition = block.mPreviousPosition;break; 
      case R.id.button_after1: cursorPosition = block.mNextPosition;break;
      default: cursorPosition = block.mNowPosition;break;
    };
    
    if(cursorPosition != -1 && mCurrentCursor != null && !mCurrentCursor.isClosed()) {
      mCurrentCursor.moveToPosition(cursorPosition);
      
      long startTime = mCurrentCursor.getLong(mStartTimeColumn);
      long endTime = mCurrentCursor.getLong(mEndTimeColumn);
      String title = mCurrentCursor.getString(mTitleColumn);
      String channel = mCurrentCursor.getString(mChannelNameColumn);
      
      viewHolder.mStartTime.setText(timeFormat.format(new Date(startTime)));
      viewHolder.mEndTime.setText(getResources().getString(R.string.running_until) + " " + timeFormat.format(new Date(endTime)));
      viewHolder.mTitle.setText(title);
      viewHolder.mChannel.setText(channel);
      
      if(showPicture) {
        String pictureCopyright = mCurrentCursor.getString(mPictureCopyrightColumn);
        byte[] logoData = mCurrentCursor.getBlob(mPictureColumn);
        
        if(pictureCopyright != null && pictureCopyright.trim().length() > 0 && logoData != null && logoData.length > 0) {
          Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
          
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
        
        viewHolder.mLayout.setTag(Long.valueOf(mCurrentCursor.getLong(mProgramIDColumn)));
        viewHolder.mLayout.setOnClickListener(mOnCliickListener);
      }
      else {
        viewHolder.mPicture.setVisibility(View.GONE);
        viewHolder.mPictureCopyright.setVisibility(View.GONE);
      }
      
      if(showGenre) {
        String genre = mCurrentCursor.getString(mGenreColumn);
        
        if(genre != null && genre.trim().length() > 0) {
          viewHolder.mGenreLabel.setVisibility(View.VISIBLE);
          viewHolder.mGenreLabel.setText(genre);
        }
        else {
          viewHolder.mGenreLabel.setVisibility(View.GONE);
        }
      }
      else {
        viewHolder.mGenreLabel.setVisibility(View.GONE);
      }
      
      if(showEpisode) {
        String episode = mCurrentCursor.getString(mEpsiodeColumn);
        
        if(episode != null && episode.trim().length() > 0) {
          viewHolder.mEpisodeLabel.setVisibility(View.VISIBLE);
          viewHolder.mEpisodeLabel.setText(episode);
        }
        else {
          viewHolder.mEpisodeLabel.setVisibility(View.GONE);
        }
      }
      else {
        viewHolder.mEpisodeLabel.setVisibility(View.GONE);
      }
      
      if(showInfo) {
        String info = IOUtils.getInfoString(mCurrentCursor.getInt(mCategoryColumn), getResources());
        
        if(info != null && info.trim().length() > 0) {
          viewHolder.mInfoLabel.setVisibility(View.VISIBLE);
          viewHolder.mInfoLabel.setText(info);
        }
        else {
          viewHolder.mInfoLabel.setVisibility(View.GONE);
        }
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
      
      UiUtils.handleMarkings(getActivity(), mCurrentCursor, convertView, null);
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
    
    mTimeRangeID = -1;
    
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
    
    mIsCompactLayout = pref.getString(getResources().getString(R.string.RUNNING_PROGRAMS_LAYOUT), "0").equals("1");
    showPicture = pref.getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false);
    showGenre = pref.getBoolean(getResources().getString(R.string.SHOW_GENRE_IN_LISTS), true);
    showEpisode = pref.getBoolean(getResources().getString(R.string.SHOW_EPISODE_IN_LISTS), true);
    showInfo = pref.getBoolean(getResources().getString(R.string.SHOW_INFO_IN_LISTS), true);
    
    if(showPicture) {
      projection = new String[14];
      
      projection[13] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[13];
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
        test.setBackgroundResource(R.color.filter_selection);
      }      
    }
    
    mCurrentTime = ((long)cal.getTimeInMillis() / 60000) * 60000;

    String sort = TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC";
    
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " <= " + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + (mCurrentTime - (60000 * 60 * 12)) + " ) ";
    where += " OR ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + (mCurrentTime + (60000 * 60 * 12)) + " ) ";
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " COLLATE NOCASE, " + sort);
    
    return loader;
  }
  
  private static final class ChannelProgramBlock {
    public int mPreviousPosition;
    public int mNowPosition;
    public int mNextPosition;
    
    public boolean mIsComplete;
    
    public ChannelProgramBlock() {
      mPreviousPosition = -1;
     
      mIsComplete = false;
      
      mNowPosition = -1;
      mNextPosition = -1;
    }
  }

  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    HashMap<Integer,ChannelProgramBlock> channelProgramMap = new HashMap<Integer,ChannelProgramBlock>();
    
    mProgramBlockList.clear();
    mCurrentViewList.clear();
    
    if(mCurrentCursor != null && !mCurrentCursor.isClosed()) {
      mCurrentCursor.close();
    }
    
    mCurrentCursor = c;
    
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
    
    int channelIDColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    while(c.moveToNext()) {
      int channelID = c.getInt(channelIDColumn);
      
      ChannelProgramBlock block = channelProgramMap.get(Integer.valueOf(channelID));
      
      if(block == null) {
        block = new ChannelProgramBlock();
        
        channelProgramMap.put(Integer.valueOf(channelID), block);
        mProgramBlockList.add(block);
      }
      
      if(!block.mIsComplete) {
        long startTime = c.getLong(mStartTimeColumn);
        long endTime = c.getLong(mEndTimeColumn);
        
        if(startTime <= mCurrentTime) {
          if(endTime <= mCurrentTime) {
            block.mPreviousPosition = c.getPosition();
          }
          else {
            block.mNowPosition = c.getPosition();
            mCurrentViewList.add(block);
          }
        }
        else {
          block.mNextPosition = c.getPosition();
          block.mIsComplete = true;
        }
      }
    }
    
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
    
    if(test == null) {
      mContextProgramID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
      UiUtils.createContextMenu(getActivity(), menu, mContextProgramID);
    }
    else {
      mContextProgramID = test.longValue();
      UiUtils.createContextMenu(getActivity(), menu, mContextProgramID);
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    UiUtils.handleContextMenuSelection(getActivity(), item, mContextProgramID, null);
    
    return false;
  }
}
