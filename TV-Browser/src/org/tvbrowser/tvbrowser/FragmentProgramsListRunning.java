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

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.text.Spannable;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.LoaderUpdater.UnsupportedFragmentException;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;
import org.tvbrowser.view.SeparatorDrawable;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

public class FragmentProgramsListRunning extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener {
  private static final String WHERE_CLAUSE_KEY = "WHERE_CLAUSE_KEY";
  private static final String DAY_CLAUSE_KEY = "DAY_CLAUSE_KEY";
  private static final int TIMEOUT_LAST_EXTRA_CLICK = 750;
    
  private Handler handler = new Handler();
  
 // private boolean mKeepRunning;
  //private Thread mUpdateThread;
  private int mWhereClauseTime;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mMarkingChangeReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  private BroadcastReceiver mChannelUpdateDone;
  
  private static final GradientDrawable BEFORE_GRADIENT;
  private static final GradientDrawable AFTER_GRADIENT;
  
  private ArrayAdapter<ChannelProgramBlock> mRunningProgramListAdapter;
  
  private ArrayList<ChannelProgramBlock> mProgramBlockList;
  private ArrayList<ChannelProgramBlock> mCurrentViewList;
  
  private LongSparseArray<String[]> mMarkingsMap;
  private LongSparseArray<String> mTitleMap;
  
  private long mCurrentTime;

  private boolean showEpisode;
  private boolean showInfo;
  private boolean mShowOrderNumber;
      
  private View.OnClickListener mOnClickListener;
  private View.OnClickListener mChannelSwitchListener;
  private View mContextView;
  private long mContextProgramID;
  private long mDayStart;
  
  private ListView mListView;
  private ViewGroup mTimeBar;
  private Spinner mDateSelection;
  
  private ArrayAdapter<DateSelection> mDateAdapter;
  
  private Button mTimeExtra;
  private long mLastExtraClick;
  private LoaderUpdater mLoaderUpdater;

  private int mStartTime = Integer.MIN_VALUE;

  private long mNextReload;
  
  static {
    BEFORE_GRADIENT = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[] {Color.argb(0x84, 0, 0, 0xff),Color.WHITE});
    BEFORE_GRADIENT.setCornerRadius(0f);
    
    AFTER_GRADIENT = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[] {Color.WHITE,Color.argb(0x84, 0, 0, 0xff)});
    AFTER_GRADIENT.setCornerRadius(0f);
  }

  public void setStartTime(int time) {
    mStartTime = time;
  }

  @Override
  public void onResume() {
    super.onResume();

    if(mStartTime != Integer.MIN_VALUE) {
      selectTime(mStartTime);
      mStartTime = Integer.MIN_VALUE;
    }

    mLoaderUpdater.setIsRunning();
    mLoaderUpdater.startUpdate();

    //mListView.getScrollY()
    /*mKeepRunning = true;
    startUpdateThread();*/
  }

  @Override
  public void onPause() {
    mLoaderUpdater.setIsNotRunning();

    super.onPause();
  }
  
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    
    mDataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        handler.post(() -> {
          updateDateSelection();

          if(intent != null) {
            mLoaderUpdater.startUpdate();
          }
        });
      }
    };
    
    mRefreshReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if((mNextReload != -1 && mNextReload < System.currentTimeMillis()) || (mNextReload ==-1 && (mStartTime == -1 || mStartTime == -2))) {
          mLoaderUpdater.startUpdate();
        }
        else {
          showEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_RUNNING_LIST, R.bool.show_episode_in_running_list_default);
          showInfo = PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_RUNNING_LIST, R.bool.show_info_in_running_list_default);
          mShowOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_RUNNING_LIST, R.bool.show_sort_number_in_running_list_default);
          
          new Thread() {
            public void run() {
              if(getActivity() != null && isAdded()) {
                ViewGroup list = getListView();
                
                for(int i = 0; i < list.getChildCount(); i++) {
                  CompactLayoutViewHolder holder = (CompactLayoutViewHolder) list.getChildAt(i).getTag();
                  
                  if(holder.mPrevious.getVisibility() == View.VISIBLE) {
                    if(holder.mPreviousStartTimeValue <= System.currentTimeMillis()) {
                      String[] markedColumns = mMarkingsMap.get(holder.mPreviousProgramID);
                      
                      UiUtils.handleMarkings(getActivity(), null, holder.mPreviousStartTimeValue, holder.mPreviousEndTimeValue, holder.mPrevious, markedColumns, handler);
                    }
                  }
                  
                  if(holder.mNowStartTimeValue <= System.currentTimeMillis()) {
                    String[] markedColumns = mMarkingsMap.get(holder.mNowProgramID);
                    
                    UiUtils.handleMarkings(getActivity(), null, holder.mNowStartTimeValue, holder.mNowEndTimeValue, holder.mNow, markedColumns, handler);
                  }
  
                  if(holder.mNextStartTimeValue <= System.currentTimeMillis()) {
                    String[] markedColumns = mMarkingsMap.get(holder.mNextProgramID);
                    
                    UiUtils.handleMarkings(getActivity(), null, holder.mNextStartTimeValue, holder.mNextEndTimeValue, holder.mNext, markedColumns, handler);
                  }
                }
              }
            }
          }.start();
        }
      }
    };
    
    mDontWantToSeeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mLoaderUpdater.startUpdate();
      }
    };
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDontWantToSeeReceiver, new IntentFilter(SettingConstants.DONT_WANT_TO_SEE_CHANGED));
    
    mMarkingChangeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, final Intent intent) {
        new Thread() {
          public void run() {
            final long programID = intent.getLongExtra(SettingConstants.EXTRA_MARKINGS_ID, -1);
            
            if(mMarkingsMap.indexOfKey(programID) >= 0 && IOUtils.isDatabaseAccessible(getActivity())) {
              String[] projection = TvBrowserContentProvider.getColumnArrayWithMarkingColumns(TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME);
              
              
              Cursor c = null; try {
              c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), projection, null, null, null);
              if(c.moveToFirst()) {
                try {
                  final View view = getListView().findViewWithTag(programID);
                  
                  if(view != null) {
                    ArrayList<String> markedColumns = new ArrayList<>();
                    
                    for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
                      int index = c.getColumnIndex(column);
                      
                      if(index >= 0 && c.getInt(index) >= 1) {
                        markedColumns.add(column);
                      }
                      else if(column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING) && ProgramUtils.isMarkedWithIcon(getActivity(), programID)) {
                        markedColumns.add(column);
                      }
                    }
                    
                    mMarkingsMap.put(programID, IOUtils.getStringArrayFromList(markedColumns));
                    
                    handler.post(() -> getListView().invalidateViews());
                  }
                }catch(NullPointerException ignored) {}
              }
                            
              } finally {IOUtils.close(c);}
            }
          }
        }.start();
      }
    };
    
    mChannelUpdateDone = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mLoaderUpdater.startUpdate();
      }
    };
    
    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    IntentFilter markingsFilter = new IntentFilter(SettingConstants.MARKINGS_CHANGED);
    IntentFilter channelsChanged = new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mChannelUpdateDone, channelsChanged);
    getActivity().registerReceiver(mDataUpdateReceiver, intent);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.REFRESH_FILTER);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMarkingChangeReceiver, markingsFilter);
  }
  
  private void setDay(long start) {
    if(start != mDayStart) {
      mDayStart = start;
      
      Calendar today = Calendar.getInstance();
      today.set(Calendar.HOUR_OF_DAY, 0);
      today.set(Calendar.MINUTE, 0);
      today.set(Calendar.SECOND, 0);
      today.set(Calendar.MILLISECOND, 0);
      
      if((mDayStart > System.currentTimeMillis() || mDayStart < today.getTimeInMillis()) && mWhereClauseTime < System.currentTimeMillis()) {
        Button time = ((ViewGroup) getView().getParent().getParent()).findViewWithTag(mWhereClauseTime);
        Button now = ((ViewGroup) getView().getParent().getParent()).findViewById(R.id.now_button);
        Button next = ((ViewGroup) getView().getParent().getParent()).findViewById(R.id.button_after1);
        
        if(time != null && !time.equals(now) && (next == null || !time.equals(next))) {
          time.performClick();
        }
        else {
          Button button = null;
          if(mTimeBar.getChildCount() > 1) {
            int startIndex = 1;
            
            if(next != null && next.getVisibility() == View.VISIBLE) {
              if(mTimeBar.getChildCount() > 2) {
                startIndex = 2;
              }
            }
            
            for(int i = startIndex; i < mTimeBar.getChildCount(); i++) {
              button = (Button)mTimeBar.getChildAt(i);
              
              if(button.getTag(R.id.time_extra) == null) {
                break;
              }
            }
          }
          
          selectButton(button);
        }
        
        mLoaderUpdater.startUpdate();
      }
      else {
        mLoaderUpdater.startUpdate();
      }
    }
  }
  
  private void setWhereClauseTime(Object time) {
    if(time instanceof Integer) {
      int testValue = (Integer) time;
      
      if(testValue != mWhereClauseTime) {
        final Integer timeTest = mWhereClauseTime;
        
        for(int i = 0; i < mTimeBar.getChildCount(); i++) {
          if(timeTest.equals(mTimeBar.getChildAt(i).getTag())) {
            mTimeBar.getChildAt(i).setBackgroundResource(android.R.drawable.list_selector_background);
          }
        }
        
        int oldWhereClauseTime = mWhereClauseTime;
        
        mWhereClauseTime = testValue;
        
        Calendar now = Calendar.getInstance();
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        if(mWhereClauseTime != -1 && mWhereClauseTime != -2 && PrefUtils.getBooleanValue(R.string.RUNNING_PROGRAMS_NEXT_DAY, R.bool.running_programs_next_day_default)) {
          int test1 = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
          
          if((test1 - mWhereClauseTime) > 180 && mDayStart < System.currentTimeMillis() && mDayStart >= today.getTimeInMillis()) {
            Spinner date = ((ViewGroup)getView().getParent()).findViewById(R.id.running_date_selection);
            
            if(date.getCount() > 2) {
              date.setSelection(2);
            }
          }
          else {
            mLoaderUpdater.startUpdate();
          }
        }
        else if(oldWhereClauseTime != -1 && (mWhereClauseTime == -1 || mWhereClauseTime == -2)) {
          Spinner date = ((ViewGroup)getView().getParent()).findViewById(R.id.running_date_selection);
          
          if(date.getCount() > 1) {
            date.setSelection(1);
          }
          
          mLoaderUpdater.startUpdate();
        }
        else {
          mLoaderUpdater.startUpdate();
        }
      }
    }
  }
    
  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putInt(WHERE_CLAUSE_KEY, mWhereClauseTime);
    outState.putLong(DAY_CLAUSE_KEY, mDayStart);
    super.onSaveInstanceState(outState);
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
    float mCurrentScale;
    
    ViewGroup mChannelInfo;
    ImageView mChannelLogo;
    TextView mChannel;
    
    View mPrevious;
    View mNow;
    View mNext;
    
    View mSeparator1;
    View mSeparator2;
    
    TextView mPreviousStartTime;
    TextView mPreviousTitle;
    TextView mPreviousInfos;
    TextView mPreviousEpisode;
    
    TextView mNowStartTime;
    TextView mNowTitle;
    TextView mNowInfos;
    TextView mNowEpisode;
    
    TextView mNextStartTime;
    TextView mNextTitle;
    TextView mNextInfos;
    TextView mNextEpisode;
    
    void setVisibility(int type, int visibility) {
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
    
    void setSeparatorVisibility(int visibility) {
       if(mSeparator1 != null) {
         mSeparator1.setVisibility(visibility);
         mSeparator2.setVisibility(visibility);
       }
    }
    
    boolean orientationChanged(int orientation) {
      return mCurrentOrientation != orientation;
    }
    
    void setColor(int type, int color) {
      switch (type) {
        case PREVIOUS:
          mPreviousEpisode.setTextColor(color);
          mPreviousTitle.setTextColor(color);
          mPreviousStartTime.setTextColor(color);
          mPreviousInfos.setTextColor(color);
          break;
        case NOW:
          mNowEpisode.setTextColor(color);
          mNowTitle.setTextColor(color);
          mNowStartTime.setTextColor(color);
          mNowInfos.setTextColor(color);
          break;
        case NEXT:
          mNextEpisode.setTextColor(color);
          mNextTitle.setTextColor(color);
          mNextStartTime.setTextColor(color);
          mNextInfos.setTextColor(color);
          break;
      }
    }
  }
  
  
  @SuppressLint("NewApi")
  private boolean fillCompactLayout(final CompactLayoutViewHolder viewHolder, final int type, final ChannelProgramBlock block, final java.text.DateFormat timeFormat, final int DEFAULT_TEXT_COLOR, boolean channelSet) {
    TextView startTimeView = null;
    TextView titleView = null;
    TextView infoView = null;
    TextView episodeView = null;
    View layout = null;
    
    long startTime = 0;
    long endTime = 0;
    long programID = -1;
    String title = null;
    String episode = null;
    Spannable infos = null;
    
    int startTimeResId = 0;
    int endTimeResId = 0;
    
    switch(type) {
      case CompactLayoutViewHolder.PREVIOUS:
        layout = viewHolder.mPrevious;
        startTimeView = viewHolder.mPreviousStartTime;
        titleView = viewHolder.mPreviousTitle;
        episodeView = viewHolder.mPreviousEpisode;
        infoView = viewHolder.mPreviousInfos;
        startTime = block.mPreviousStart;
        endTime = block.mPreviousEnd;
        episode = block.mPreviousEpisode;
        infos = block.mPreviousCategory;
        programID = block.mPreviousProgramID;
        startTimeResId = R.id.running_time_previous_start;
        endTimeResId = R.id.running_time_previous_end;
        break;
      case CompactLayoutViewHolder.NOW:
        layout = viewHolder.mNow;
        startTimeView = viewHolder.mNowStartTime;
        titleView = viewHolder.mNowTitle;
        episodeView = viewHolder.mNowEpisode;
        infoView = viewHolder.mNowInfos;
        startTime = block.mNowStart;
        endTime = block.mNowEnd;
        episode = block.mNowEpisode;
        infos = block.mNowCategory;
        programID = block.mNowProgramID;
        startTimeResId = R.id.running_time_now_start;
        endTimeResId = R.id.running_time_now_end;
        break;
      case CompactLayoutViewHolder.NEXT:
        layout = viewHolder.mNext;
        startTimeView = viewHolder.mNextStartTime;
        titleView = viewHolder.mNextTitle;
        episodeView = viewHolder.mNextEpisode;
        infoView = viewHolder.mNextInfos;
        startTime = block.mNextStart;
        endTime = block.mNextEnd;
        episode = block.mNextEpisode;
        infos = block.mNextCategory;
        programID = block.mNextProgramID;
        startTimeResId = R.id.running_time_next_start;
        endTimeResId = R.id.running_time_next_end;
        break;
    }
    
    title = mTitleMap.get(programID);
    
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
      titleView.setText(ProgramUtils.getMarkIcons(getActivity(), programID, title));
      
      if(!showEpisode || episode == null || episode.trim().length() == 0) {
        episodeView.setVisibility(View.GONE);
      }
      else {
        episodeView.setText(episode);
        episodeView.setVisibility(View.VISIBLE);
      }
      
      if(!showInfo || infos == null || infos.toString().trim().length() == 0) {
        infoView.setVisibility(View.GONE);
      }
      else {
        infoView.setText(infos);
        infoView.setVisibility(View.VISIBLE);
      }
      
      if(endTime <= System.currentTimeMillis()) {
        viewHolder.setColor(type, UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, getActivity()));
      }
      else {
        viewHolder.setColor(type, DEFAULT_TEXT_COLOR);
      }
      
      if(!channelSet) {
        String logoNamePref = PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_RUNNING, R.string.channel_logo_name_running_default);
        
        boolean showChannelName = logoNamePref.equals("0") || logoNamePref.equals("2");
        boolean showChannelLogo = logoNamePref.equals("0") || logoNamePref.equals("1");
        boolean showBigChannelLogo = logoNamePref.equals("3");
        
        Drawable logo = null;
        
        if(showBigChannelLogo) {
          logo = SettingConstants.MEDIUM_LOGO_MAP.get(block.mChannelID);
        }
        else if(showChannelLogo) {
          logo = SettingConstants.SMALL_LOGO_MAP.get(block.mChannelID);
        }
        
        if(logo != null) {
          viewHolder.mChannelLogo.setImageDrawable(logo);
          viewHolder.mChannelLogo.setVisibility(View.VISIBLE);
        }
        else {
          viewHolder.mChannelLogo.setVisibility(View.GONE);
        }
        
        String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(block.mChannelName);
        
        if(shortName == null) {
          shortName = block.mChannelName;
        }
        
        if(mShowOrderNumber && (logo == null || showChannelName)) {
          shortName = block.mChannelOrderNumber + ". " + shortName;
        }
        else if(mShowOrderNumber) {
          shortName = block.mChannelOrderNumber + ".";
        }
        
        if(logo == null || mShowOrderNumber || showChannelName) {
          viewHolder.mChannel.setText(shortName);
          viewHolder.mChannel.setVisibility(View.VISIBLE);
        }
        else {
          viewHolder.mChannel.setVisibility(View.GONE);
        }
        
        viewHolder.mChannelInfo.setTag(block.mChannelID);
        viewHolder.mChannelInfo.setOnClickListener(mChannelSwitchListener);
        
        channelSet = true;
      }
      
      viewHolder.mChannelInfo.setTag(startTimeResId, startTime);
      viewHolder.mChannelInfo.setTag(endTimeResId, endTime);
      
      layout.setTag(programID);
      layout.setOnClickListener(mOnClickListener);
      
      final String[] markingsValue = mMarkingsMap.get(programID);
      
      if(startTime <= System.currentTimeMillis() || (markingsValue != null && markingsValue.length > 0)) {
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
        CompatUtils.setBackground(layout, ContextCompat.getDrawable(getActivity(), android.R.drawable.list_selector_background));
      }
    }
    else {
      int viewType = View.GONE;
      boolean isPortrait = viewHolder.mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT;
      
      Configuration config = getResources().getConfiguration();
      
      if(Build.VERSION.SDK_INT >= 13) {
        if(type == CompactLayoutViewHolder.PREVIOUS) {
          if(config.smallestScreenWidthDp >= 600 && !isPortrait) {
            viewType = View.INVISIBLE;
          }
        }
        else if(type == CompactLayoutViewHolder.NOW && (config.smallestScreenWidthDp >= 600 || !isPortrait)) {
          viewType = View.INVISIBLE;
        }
      }
      
      viewHolder.setVisibility(type, viewType);
      
      if(type == CompactLayoutViewHolder.PREVIOUS) {
        viewHolder.setSeparatorVisibility(viewType);
      }
      
      titleView.setVisibility(View.GONE);
      episodeView.setVisibility(View.GONE);
      infoView.setVisibility(View.GONE);
    }
    
    return channelSet;
  }
  
  private View getCompactView(View convertView, ViewGroup parent, java.text.DateFormat timeFormat, ChannelProgramBlock block, int DEFAULT_TEXT_COLOR) {
    CompactLayoutViewHolder viewHolder = null;
    
    float textScale = Float.valueOf(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_TEXT_SCALE, R.string.pref_program_lists_text_scale_default));
    
    if(convertView == null || ((CompactLayoutViewHolder)convertView.getTag()).orientationChanged(SettingConstants.ORIENTATION) || ((CompactLayoutViewHolder)convertView.getTag()).mCurrentScale !=  textScale) {
      convertView = getActivity().getLayoutInflater().inflate(R.layout.compact_program_panel, parent, false);
      
      UiUtils.scaleTextViews(convertView, textScale);
      
      viewHolder = new CompactLayoutViewHolder();
      
      viewHolder.mCurrentOrientation = SettingConstants.ORIENTATION;
      viewHolder.mCurrentScale = textScale;
      
      viewHolder.mChannelInfo = convertView.findViewById(R.id.running_list_channel_info);
      viewHolder.mChannelLogo = convertView.findViewById(R.id.running_list_channel_logo);
      viewHolder.mChannel = convertView.findViewById(R.id.running_compact_channel_label);
      
      viewHolder.mSeparator1 = convertView.findViewById(R.id.running_separator_1);
      viewHolder.mSeparator2 = convertView.findViewById(R.id.running_separator_2);
      
      viewHolder.mPrevious = convertView.findViewById(R.id.running_compact_previous);
      viewHolder.mNow = convertView.findViewById(R.id.running_compact_now);
      viewHolder.mNext = convertView.findViewById(R.id.running_compact_next);
      
      registerForContextMenu(viewHolder.mPrevious);
      registerForContextMenu(viewHolder.mNow);
      registerForContextMenu(viewHolder.mNext);
      
      viewHolder.mPreviousStartTime = convertView.findViewById(R.id.running_compact_previous_start);
      viewHolder.mNowStartTime = convertView.findViewById(R.id.running_compact_now_start);
      viewHolder.mNextStartTime = convertView.findViewById(R.id.running_compact_next_start);
      
      viewHolder.mPreviousTitle = convertView.findViewById(R.id.running_compact_previous_title);
      viewHolder.mNowTitle = convertView.findViewById(R.id.running_compact_now_title);
      viewHolder.mNextTitle = convertView.findViewById(R.id.running_compact_next_title);
      
      viewHolder.mPreviousInfos = convertView.findViewById(R.id.running_compact_previous_infos);
      viewHolder.mNowInfos = convertView.findViewById(R.id.running_compact_now_infos);
      viewHolder.mNextInfos = convertView.findViewById(R.id.running_compact_next_infos);
      
      viewHolder.mPreviousEpisode = convertView.findViewById(R.id.running_compact_previous_episode);
      viewHolder.mNowEpisode = convertView.findViewById(R.id.running_compact_now_episode);
      viewHolder.mNextEpisode = convertView.findViewById(R.id.running_compact_next_episode);
      
      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (CompactLayoutViewHolder)convertView.getTag();
    }
    
    if(viewHolder != null && block != null /*&& mCurrentCursor != null && !mCurrentCursor.isClosed()*/) {
      viewHolder.mChannelInfo.setTag(R.id.running_time_previous_start, null);
      viewHolder.mChannelInfo.setTag(R.id.running_time_now_start, null);
      viewHolder.mChannelInfo.setTag(R.id.running_time_next_start, null);
      
      viewHolder.mChannelInfo.setTag(R.id.running_time_previous_end, null);
      viewHolder.mChannelInfo.setTag(R.id.running_time_now_end, null);
      viewHolder.mChannelInfo.setTag(R.id.running_time_next_end, null);
      
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
    
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    pref.registerOnSharedPreferenceChangeListener(this);
    
    if(handler == null) {
      handler = new Handler();
    }
    
    try {
      mLoaderUpdater = new LoaderUpdater(FragmentProgramsListRunning.this, handler);
    } catch (UnsupportedFragmentException e) {
      // Ignore
    }
    
    if(savedInstanceState != null) {
      mWhereClauseTime = savedInstanceState.getInt(WHERE_CLAUSE_KEY,-1);
      mDayStart = savedInstanceState.getLong(DAY_CLAUSE_KEY,-1);
    }
    else {
      mWhereClauseTime = -1;
      mDayStart = -1;
    }
        
    mMarkingsMap = new LongSparseArray<>();
    mTitleMap = new LongSparseArray<>();
    
    mOnClickListener = v -> {
      Long tag = (Long)v.getTag();

      if(tag != null) {
        UiUtils.showProgramInfo(getActivity(), tag, getActivity().getCurrentFocus(), handler);
      }
    };
    
    mChannelSwitchListener = v -> {
      Integer id = (Integer)v.getTag();
      boolean handle = PrefUtils.getBooleanValue(R.string.PREF_RUNNING_LIST_CLICK_TO_CHANNEL_TO_LIST, R.bool.pref_running_list_click_to_channel_to_list_default);

      if(handle && id != null) {
        Intent showChannel = new Intent(SettingConstants.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT);
        showChannel.putExtra(SettingConstants.CHANNEL_ID_EXTRA,id);

        Object scrollTime = v.getTag(R.id.running_time_now_start);
        Object endTime = v.getTag(R.id.running_time_now_end);

        if(scrollTime == null) {
          scrollTime = v.getTag(R.id.running_time_previous_start);
          endTime = v.getTag(R.id.running_time_previous_end);
        }

        if(scrollTime == null) {
          scrollTime = v.getTag(R.id.running_time_next_start);
          endTime = v.getTag(R.id.running_time_next_end);
        }

        if(scrollTime != null) {
          showChannel.putExtra(SettingConstants.EXTRA_START_TIME, ((Long)scrollTime).longValue());
          showChannel.putExtra(SettingConstants.EXTRA_END_TIME, ((Long)endTime).longValue());
        }
        else {
          Calendar now = Calendar.getInstance();

          if(mDayStart != -1) {
            now.setTimeInMillis(mDayStart);
          }

          now.set(Calendar.SECOND, 0);
          now.set(Calendar.MILLISECOND, 0);

          if(mWhereClauseTime >= 0) {
            now.set(Calendar.HOUR_OF_DAY, mWhereClauseTime / 60);
            now.set(Calendar.MINUTE, mWhereClauseTime % 60);
          } else if(mWhereClauseTime == -1) {
            now.setTimeInMillis(System.currentTimeMillis());
          }

          showChannel.putExtra(SettingConstants.EXTRA_START_TIME, now.getTimeInMillis());
          showChannel.putExtra(SettingConstants.EXTRA_END_TIME, now.getTimeInMillis());
        }

        if(mDateSelection.getSelectedItemPosition() == 0) {
          showChannel.putExtra(SettingConstants.DAY_POSITION_EXTRA, FragmentProgramsList.INDEX_DATE_YESTERDAY);
        }
        else if(mDateSelection.getSelectedItemPosition() == 1) {
          if(endTime == null || (Long) endTime > System.currentTimeMillis()) {
            showChannel.putExtra(SettingConstants.DAY_POSITION_EXTRA, FragmentProgramsList.INDEX_DATE_TODAY_TOMORROW);
          }
        }

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(showChannel);
      }
    };
        
    mProgramBlockList = new ArrayList<>();
    mCurrentViewList = new ArrayList<>();
    
    java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(getActivity());
    String value = ((SimpleDateFormat)mTimeFormat).toLocalizedPattern();
    
    if((value.charAt(0) == 'H' && value.charAt(1) != 'H') || (value.charAt(0) == 'h' && value.charAt(1) != 'h')) {
      value = value.charAt(0) + value;
    }
    
    final java.text.DateFormat timeFormat = new SimpleDateFormat(value, Locale.getDefault());
    final int DEFAULT_TEXT_COLOR = new TextView(getActivity()).getTextColors().getDefaultColor();
        
    mRunningProgramListAdapter = new ArrayAdapter<FragmentProgramsListRunning.ChannelProgramBlock>(getActivity(), R.layout.running_list_entries, mCurrentViewList) {
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ChannelProgramBlock block = getItem(position);
        
       // if(mIsCompactLayout) {
          return getCompactView(convertView, parent, timeFormat, block, DEFAULT_TEXT_COLOR);
        /*}
        else {
          return getLongView(convertView, parent, timeFormat, block, DEFAULT_TEXT_COLOR);
        }*/
      }
    };
    
    mListView.setAdapter(mRunningProgramListAdapter);
    
    SeparatorDrawable drawable = new SeparatorDrawable(getActivity());
    
    getListView().setDivider(drawable);
    
    setDividerSize(PrefUtils.getStringValue(R.string.PREF_RUNNING_DIVIDER_SIZE, R.string.pref_running_divider_size_default));
    
   // getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onDetach() {
    
    if(mDataUpdateReceiver != null) {
      getActivity().unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
    if(mMarkingChangeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMarkingChangeReceiver);
    }
    if(mDontWantToSeeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDontWantToSeeReceiver);
    }
    if(mChannelUpdateDone != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mChannelUpdateDone);
    }
    
    mLoaderUpdater.setIsNotRunning();
    super.onDetach();
  }
    
 /* private void startUpdateTshread() {
    if(mKeepRunning) {
      if(getLoaderManager().hasRunningLoaders()) {
        getLoaderManager().getLoader(0).cancelLoad();
      }
      
      mUpdateThread = new Thread() {
        public void run() {
          handler.post(new Runnable() {
            @Override
            public void run() {
              if(!isDetached() &&  mKeepRunning && !isRemoving()) {
                getLoaderManager().restartLoader(0, null, FragmentProgramsListRunning.this);
              }
            }
          });
        }
      };
      mUpdateThread.start();
    }
  }
  */
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.running_program_fragment, container, false);
    
    mListView = view.findViewById(R.id.running_list_fragment_list_view);
    mTimeBar = view.findViewById(R.id.runnning_time_bar);
    
    initialize(view);
    
    return view;
  }
  
  private void pickTime(final View v) {
    if(mLastExtraClick + TIMEOUT_LAST_EXTRA_CLICK < System.currentTimeMillis()) {
      if(isViewNotVisible(mTimeExtra)) {
        ((HorizontalScrollView)mTimeBar.getParent()).scrollTo(mTimeExtra.getLeft(), mTimeExtra.getTop());
      }
      
      final int time = (Integer)v.getTag();
      
      final TimePickerDialog pick = new TimePickerDialog(getActivity(), TimePickerDialog.THEME_HOLO_DARK, (view, hourOfDay, minute) -> {
        final Integer selectedTime = hourOfDay * 60 + minute;

        insertTimeExtra(selectedTime);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, selectedTime / 60);
        cal.set(Calendar.MINUTE, selectedTime % 60);

        mTimeExtra.setText(DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(cal.getTime()));
        mTimeExtra.setTag(selectedTime);

        mLastExtraClick = System.currentTimeMillis();
        setWhereClauseTime(selectedTime);
        PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, getActivity()).edit().putInt(getString(R.string.PREF_MISC_LAST_TIME_EXTRA_VALUE), selectedTime).commit();
      }, time/60, time%60, DateFormat.is24HourFormat(getActivity()));
      
      pick.show();
    }
    else {
      mLastExtraClick = System.currentTimeMillis();
      setWhereClauseTime(mWhereClauseTime);
    }
  }
  
  private void initialize(View rootView) {
    final Button now = rootView.findViewById(R.id.now_button);
    final Button next = rootView.findViewById(R.id.button_after1);
    mDateSelection = rootView.findViewById(R.id.running_date_selection);
    now.setTag(-1);
    next.setTag(-2);
    
    final View.OnClickListener listenerClick = v -> {
      if(PrefUtils.getBooleanValue(R.string.PREF_RUNNING_PROGRAMS_SHOW_TIME_PICK_BUTTON_ONLY_WHEN_NEEDED, R.bool.pref_running_programs_show_time_pick_button_only_when_needed_default)) {
        mTimeBar.removeView(mTimeExtra);
      }

      if((v.equals(now) || v.equals(next)) && mDateSelection.getCount() > 1) {
        mDateSelection.setSelection(1);
      }

      setWhereClauseTime(v.getTag());
    };
    
    final View.OnLongClickListener listenerLongClick = v -> {
      pickTime(v);

      return true;
    };
    
    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    
    IntentFilter timeButtonsUpdateFilter = new IntentFilter(SettingConstants.UPDATE_TIME_BUTTONS);
    
    final BroadcastReceiver receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        for(int i = mTimeBar.getChildCount() - 1; i >= 0; i--) {
          Button button = (Button)mTimeBar.getChildAt(i);
          
          if(button != null) {
            button.setOnClickListener(null);
            mTimeBar.removeViewAt(i);
          }
        }
        
        if(getActivity() != null) {
          SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
          now.setOnClickListener(listenerClick);
          mTimeBar.addView(now);
          
          if(PrefUtils.getBooleanValue(R.string.PREF_RUNNING_PROGRAMS_SHOW_NEXT_BUTTON, R.bool.pref_running_programs_show_next_button_default)) {
            next.setOnClickListener(listenerClick);
            mTimeBar.addView(next);
          }
          
          now.setOnLongClickListener(v -> {
            if(mDateSelection.getCount() > 1) {
              mDateSelection.setSelection(1);
            }

            setWhereClauseTime(-2);

            return true;
          });
          
          ArrayList<Integer> values = new ArrayList<>();
          
          int[] defaultValues = getResources().getIntArray(R.array.time_button_defaults);
          
          int timeButtonCount = pref.getInt(getString(R.string.TIME_BUTTON_COUNT),getResources().getInteger(R.integer.time_button_count_default));
          
          for(int i = 1; i <= Math.min(timeButtonCount, getResources().getInteger(R.integer.time_button_count_default)); i++) {
            try {
              Class<?> string = R.string.class;
              
              Field setting = string.getDeclaredField("TIME_BUTTON_" + i);
              
              Integer value = pref.getInt(getResources().getString((Integer) setting.get(string)), defaultValues[i - 1]);
              
              if(value >= -1 && !values.contains(value)) {
                values.add(value);
              }
            } catch (Exception ignored) {}
          }
          
          for(int i = 7; i <= timeButtonCount; i++) {
              Integer value = pref.getInt("TIME_BUTTON_" + i, 0);
              
              if(value >= -1 && !values.contains(value)) {
                values.add(value);
              }
          }
          
          if(PrefUtils.getBooleanValue(R.string.SORT_RUNNING_TIMES, R.bool.sort_running_times_default)) {
            Collections.sort(values);
          }

          Calendar cal = Calendar.getInstance();
          
          int lastExtraTime = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_MISC_LAST_TIME_EXTRA_VALUE, R.integer.pref_misc_last_time_extra_value_default);
          
          cal.set(Calendar.HOUR_OF_DAY, lastExtraTime/60);
          cal.set(Calendar.MINUTE, lastExtraTime%60);
          
          getActivity().getLayoutInflater().inflate(R.layout.time_button, mTimeBar);
          
          mLastExtraClick = 0;
          
          mTimeExtra = (Button)mTimeBar.getChildAt(mTimeBar.getChildCount()-1);
          mTimeExtra.setText(DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(cal.getTime()));
          mTimeExtra.setTag(R.id.time_extra, Boolean.TRUE);
          mTimeExtra.setTypeface(null, Typeface.BOLD_ITALIC);
          mTimeExtra.setTag(lastExtraTime);
          mTimeExtra.setOnClickListener(v -> pickTime(v));
          mTimeExtra.setOnLongClickListener(v -> {
            mLastExtraClick = System.currentTimeMillis();

            if((v.equals(now) || v.equals(next)) && mDateSelection.getCount() > 1) {
              mDateSelection.setSelection(1);
            }

            setWhereClauseTime(v.getTag());

            return true;
          });
          
          if(PrefUtils.getBooleanValue(R.string.PREF_RUNNING_PROGRAMS_SHOW_TIME_PICK_BUTTON_ONLY_WHEN_NEEDED, R.bool.pref_running_programs_show_time_pick_button_only_when_needed_default)
              && lastExtraTime != mWhereClauseTime) {
            mTimeBar.removeView(mTimeExtra);
          }
          
          for(Integer value : values) {
            getActivity().getLayoutInflater().inflate(R.layout.time_button, mTimeBar);
            
            cal.set(Calendar.HOUR_OF_DAY, value / 60);
            cal.set(Calendar.MINUTE, value % 60);
            
            Button time = (Button)mTimeBar.getChildAt(mTimeBar.getChildCount()-1);
            time.setText(DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(cal.getTime()));
            time.setTag(value);
            time.setOnClickListener(listenerClick);
            time.setOnLongClickListener(listenerLongClick);
          }
        }
      }
    };
    
    localBroadcastManager.registerReceiver(receiver, timeButtonsUpdateFilter);
    receiver.onReceive(null, null);
    
    ArrayList<DateSelection> dateEntries = new ArrayList<>();
    
    mDateAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, dateEntries);
    mDateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mDateSelection.setAdapter(mDateAdapter);

    mDateSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(pos >= 0) {
          DateSelection selection = mDateAdapter.getItem(pos);
          
          setDay(selection.getTime());
        }
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        setDay(-1);
      }
    });
    
    updateDateSelection();
    
    if(mDateSelection.getCount() > 1) {
      mDateSelection.setSelection(1);
    }
  }

  private void updateDateSelection() {
    if(getActivity() != null && !isDetached() && mDateSelection != null) {
      final int orgPos = mDateSelection.getSelectedItemPosition();
      int pos = orgPos;
      
      mDateAdapter.clear();
      
      long last = PrefUtils.getLongValueWithDefaultKey(R.string.META_DATA_DATE_LAST_KNOWN, R.integer.meta_data_date_known_default);
      
      Calendar lastDay = Calendar.getInstance();
      lastDay.setTimeInMillis(last);
      
      lastDay.set(Calendar.HOUR_OF_DAY, 4);
      lastDay.set(Calendar.MINUTE, 0);
      lastDay.set(Calendar.SECOND, 0);
      lastDay.set(Calendar.MILLISECOND, 0);
      
      Calendar yesterday = Calendar.getInstance();
      yesterday.set(Calendar.HOUR_OF_DAY, 4);
      yesterday.set(Calendar.MINUTE, 0);
      yesterday.set(Calendar.SECOND, 0);
      yesterday.set(Calendar.MILLISECOND, 0);
      yesterday.add(Calendar.DAY_OF_YEAR, -1);
      
      long yesterdayStart = yesterday.getTimeInMillis();
      long lastStart = lastDay.getTimeInMillis();
      
      Calendar cal = Calendar.getInstance();
      
      for(long day = yesterdayStart; day <= lastStart; day += (24 * 60 * 60000)) {
        cal.setTimeInMillis(day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        
        mDateAdapter.add(new DateSelection(cal.getTimeInMillis(), getActivity()));
      }
      
      if(pos == -1) {
        pos = 1;
      }
      
      if(mDateSelection.getCount() > pos) {
        mDateSelection.setSelection(pos);
      }
      else {
        mDateSelection.setSelection(mDateSelection.getCount()-1);
      }
      
      if(orgPos == pos && pos < mDateAdapter.getCount()) {
        final DateSelection selection = mDateAdapter.getItem(pos);
        setDay(selection.getTime());
      }
    }
  }
  
  private ListView getListView() {
    return mListView;
  }
  
  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] infoCategories = TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY;
    int startIndex = 13 + infoCategories.length;
    
    String[] projection = new String[startIndex + TvBrowserContentProvider.MARKING_COLUMNS.length];
    
    showEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_RUNNING_LIST, R.bool.show_episode_in_running_list_default);
    showInfo = PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_RUNNING_LIST, R.bool.show_info_in_running_list_default);
    mShowOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_RUNNING_LIST, R.bool.show_sort_number_in_running_list_default);
    
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
    projection[10] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    projection[11] = TvBrowserContentProvider.CHANNEL_KEY_NAME;
    projection[12] = TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE;
    
    System.arraycopy(infoCategories, 0, projection, 13, infoCategories.length);
    System.arraycopy(TvBrowserContentProvider.MARKING_COLUMNS, 0, projection, startIndex, TvBrowserContentProvider.MARKING_COLUMNS.length);
    
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 30);
    
    Calendar now = Calendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    
    if(mWhereClauseTime >= 0) {
      if(mDayStart >= 0) {
        cal.setTimeInMillis(mDayStart);
      }
      
      cal.set(Calendar.HOUR_OF_DAY, mWhereClauseTime / 60);
      cal.set(Calendar.MINUTE, mWhereClauseTime % 60);
    }
    if(mWhereClauseTime < 0) {
      cal.setTimeInMillis(System.currentTimeMillis());
    }
    
    final Integer timeTest = mWhereClauseTime;
    
    for(int i = 0; i < mTimeBar.getChildCount(); i++) {
      if(timeTest.equals(mTimeBar.getChildAt(i).getTag())) {
        mTimeBar.getChildAt(i).setBackgroundColor(UiUtils.getColor(UiUtils.RUNNING_TIME_SELECTION_KEY, getActivity()));
        
        if(isViewNotVisible(mTimeBar.getChildAt(i))) {
          ((HorizontalScrollView)mTimeBar.getParent()).scrollTo(mTimeBar.getChildAt(i).getLeft(), mTimeBar.getChildAt(i).getTop());
        }
      }
    }
    
    mCurrentTime = (cal.getTimeInMillis() / 60000) * 60000;
    
    String sort = TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC";
    
    String where = " ( ( "  + TvBrowserContentProvider.DATA_KEY_ENDTIME + "<=" + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">" + (mCurrentTime - (60000 * 60 * 12)) + " ) ";
    
    if(mWhereClauseTime == -1) {
      where = " ( ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">" + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + (mCurrentTime + (60000 * 60 * 12)) + " ) ";
    }
    else {
      where += " OR ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">" + mCurrentTime + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + (mCurrentTime + (60000 * 60 * 12)) + " ) ";
    }
    
    where += ") " + UiUtils.getDontWantToSeeFilterString(getActivity());
    
    where += ((TvBrowser)getActivity()).getFilterSelection(false);
    
    return new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " COLLATE NOCASE, " + sort);
  }
  
  private static final class ChannelProgramBlock {
    public long mCreationTime;
    int mChannelID;
    private String mChannelName;
    private int mChannelOrderNumber;
    
    int mPreviousPosition;
    long mPreviousStart;
    long mPreviousEnd;
    long mPreviousProgramID;
//    public String mPreviousTitle;
String mPreviousEpisode;
    String mPreviousGenre;
    Spannable mPreviousCategory;
    String mPreviousPictureCopyright;
    byte[] mPreviousPicture;

    int mNowPosition;
    long mNowStart;
    long mNowEnd;
    long mNowProgramID;
   // public String mNowTitle;
   String mNowEpisode;
    String mNowGenre;
    Spannable mNowCategory;
    String mNowPictureCopyright;
    byte[] mNowPicture;
    
    int mNextPosition;
    long mNextStart;
    long mNextEnd;
    long mNextProgramID;
   // public String mNextTitle;
   String mNextEpisode;
    String mNextGenre;
    Spannable mNextCategory;
    String mNextPictureCopyright;
    byte[] mNextPicture;

    boolean mIsComplete;
    
    ChannelProgramBlock() {
      mIsComplete = false;
    }
  }
  
  @Override
  public synchronized void onLoadFinished(@NonNull Loader<Cursor> loader, final Cursor c) {
    if(c != null) {
      SparseArrayCompat<ChannelProgramBlock> channelProgramMap = new SparseArrayCompat<>();
      SparseArrayCompat<ChannelProgramBlock> currentProgramMap = new SparseArrayCompat<>();
      boolean showDontWantToSee = PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(getResources().getStringArray(R.array.pref_simple_string_value_array2)[1]);
      
      mProgramBlockList.clear();
      mCurrentViewList.clear();
      mMarkingsMap.clear();
      mTitleMap.clear();

      final int mProgramIDColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      final int mStartTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      final int mEndTimeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
      final int mTitleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
      final int mPictureColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);
      final int mPictureCopyrightColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT);
      final int mCategoryColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES);
      final int mGenreColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE);
      final int mEpsiodeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
      final int mChannelNameColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      final int mChannelIDColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
      int channelOrderColumn = c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      int dontWantToSeeColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE);
      
      HashMap<String, Integer> markingColumnsMap = new HashMap<>();
      
      c.moveToPosition(-1);
      
      for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        int index = c.getColumnIndex(column);
        
        if(index >= 0) {
          markingColumnsMap.put(column, index);
        }
      }
      
      if(c.getCount() > 0) {
        try {
          final ArrayList<Long> timeList = new ArrayList<>();

          while(!c.isClosed() && c.moveToNext()) {
            int channelID = c.getInt(mChannelIDColumn);
            
            ChannelProgramBlock block = channelProgramMap.get(channelID);
            ArrayList<String> markedColumsList = new ArrayList<>();
            
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
              
              for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
                Integer value = markingColumnsMap.get(column);
                
                if(value != null && c.getInt(value) >= 1) {
                  markedColumsList.add(column);
                }
                else if(column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING) && ProgramUtils.isMarkedWithIcon(getActivity(), programID)) {
                  markedColumsList.add(column);
                }
              }
              
              String channelName = c.getString(mChannelNameColumn);
              int channelOrderNumber = c.getInt(channelOrderColumn);
      
              String genre = null;
              Spannable category = null;
              String pictureCopyright = null;
              byte[] picture = null;
              
              if(showInfo) {
                category = IOUtils.getInfoString(c.getInt(mCategoryColumn), getResources());
              }
                          
              if(showDontWantToSee || c.getInt(dontWantToSeeColumn) == 0) {
                block.mChannelID = channelID;
                block.mChannelName = channelName;
                block.mChannelOrderNumber = channelOrderNumber;
                
                if(startTime <= mCurrentTime) {
                  if(endTime <= mCurrentTime || (endTime > mCurrentTime && mWhereClauseTime == -2)) {
                    block.mPreviousPosition = c.getPosition();
                    block.mPreviousProgramID = programID;
                    block.mPreviousStart = startTime;
                    block.mPreviousEnd = endTime;
                    mTitleMap.put(programID, title);
                    block.mPreviousEpisode = episode;
                    block.mPreviousGenre = genre;
                    block.mPreviousPicture = picture;
                    block.mPreviousPictureCopyright = pictureCopyright;
                    block.mPreviousCategory = category;
                    
                    if(mWhereClauseTime == -2 && currentProgramMap.indexOfKey(channelID) < 0) { 
                      currentProgramMap.put(channelID, block);
                      mCurrentViewList.add(block);
                    }
                  }
                  else if(startTime <= mCurrentTime && mCurrentTime < endTime) {
                    block.mNowPosition = c.getPosition();
                    block.mNowProgramID = programID;
                    block.mNowStart = startTime;
                    block.mNowEnd = endTime;
                    mTitleMap.put(programID, title);
                    block.mNowEpisode = episode;
                    block.mNowGenre = genre;
                    block.mNowPicture = picture;
                    block.mNowPictureCopyright = pictureCopyright;
                    block.mNowCategory = category;
                    
                    if(currentProgramMap.indexOfKey(channelID) < 0) { 
                      currentProgramMap.put(channelID, block);
                      mCurrentViewList.add(block);
                    }
                  }
                }
                else if(mWhereClauseTime != -2 || block.mNowPosition > 0) {
                  block.mNextPosition = c.getPosition();
                  block.mNextStart = startTime;
                  block.mNextEnd = endTime;
                  block.mNextProgramID = programID;
                  mTitleMap.put(programID, title);
                  block.mNextEpisode = episode;
                  block.mNextGenre = genre;
                  block.mNextPicture = picture;
                  block.mNextPictureCopyright = pictureCopyright;
                  block.mNextCategory = category;
                  
                  block.mIsComplete = true;
                  
                  if(currentProgramMap.indexOfKey(channelID) < 0) { 
                    currentProgramMap.put(channelID, block);
                    mCurrentViewList.add(block);
                  }
                }
                else if(mWhereClauseTime == -2 && block.mNowPosition == 0) {
                  block.mNowPosition = c.getPosition();
                  block.mNowProgramID = programID;
                  block.mNowStart = startTime;
                  block.mNowEnd = endTime;
                  mTitleMap.put(programID, title);
                  block.mNowEpisode = episode;
                  block.mNowGenre = genre;
                  block.mNowPicture = picture;
                  block.mNowPictureCopyright = pictureCopyright;
                  block.mNowCategory = category;
                  
                  if(currentProgramMap.indexOfKey(channelID) < 0) { 
                    currentProgramMap.put(channelID, block);
                    mCurrentViewList.add(block);
                  }
                }
                
                mMarkingsMap.put(programID, IOUtils.getStringArrayFromList(markedColumsList));
                markedColumsList.clear();
              }
            } else {
              if(mStartTime != -1 && mStartTime != -2 && block.mPreviousStart != 0 && !timeList.contains(block.mPreviousStart)) {
                timeList.add(block.mPreviousStart);
              }
              if(block.mPreviousEnd != 0 && !timeList.contains(block.mPreviousEnd)) {
                timeList.add(block.mPreviousEnd);
              }
              if(block.mNowStart != 0 && !timeList.contains(block.mNowStart)) {
                timeList.add(block.mNowStart);
              }
              if(block.mNowEnd != 0 && !timeList.contains(block.mNowEnd)) {
                timeList.add(block.mNowEnd);
              }
              if(block.mNextStart != 0 && !timeList.contains(block.mNextStart)) {
                timeList.add(block.mNextStart);
              }
            }
          }

          Collections.sort(timeList);
          long now = System.currentTimeMillis();

          mNextReload = -1;

          for(Long time : timeList) {
            if(time > now) {
              mNextReload = time;
              break;
            }
          }
        }catch(IllegalStateException ignored) {}
      }

      IOUtils.close(c); // FIXME should be a call to an adapter's swapCursor to reuse the loader's cursor
      currentProgramMap.clear();
      channelProgramMap.clear();
    }

    mRunningProgramListAdapter.notifyDataSetChanged();
  }

  public int getScrollY() {
    return mListView.getScrollY();
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    mCurrentViewList.clear();
    mProgramBlockList.clear();
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    Long test = (Long)v.getTag();
    
    if(test != null) {
      mContextProgramID = test;
      mContextView = v;
      UiUtils.createContextMenu(getActivity(), menu, mContextProgramID);
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(getUserVisibleHint() && mContextProgramID >= 0) {
      UiUtils.handleContextMenuSelection(getActivity(), item, mContextProgramID, mContextView, getActivity().getCurrentFocus());
      
      mContextProgramID = -1;
      return true;
    }
    
    return false;
  }
  
  private void setDividerSize(String size) {    
    getListView().setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  private void insertTimeExtra(final Integer time) {
    boolean timeExtraFound = false;
    boolean timeAvailable = false;
    
    int insertIndex = 1;
    
    if(mTimeBar.getChildCount() > 1) {
      final Button next = ((ViewGroup) getView().getParent().getParent()).findViewById(R.id.button_after1);
      
      if(next != null && next.getVisibility() == View.VISIBLE) {
        if(mTimeBar.getChildCount() > 2) {
          insertIndex = 2;
        }
      }
      
      for(int i = insertIndex; i < mTimeBar.getChildCount(); i++) {
        Button button = (Button)mTimeBar.getChildAt(i);
        
        if(button.getTag(R.id.time_extra) != null) {
          timeExtraFound = true;
        } else if(time.equals(mTimeBar.getChildAt(i).getTag())) {
          timeAvailable = true;
        }
      }
    }
    
    if(!timeExtraFound && !timeAvailable) {
      mTimeBar.addView(mTimeExtra, insertIndex);
    }
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(!isDetached() && getActivity() != null) {
      if(getString(R.string.PREF_RUNNING_DIVIDER_SIZE).equals(key)) {
        setDividerSize(PrefUtils.getStringValue(R.string.PREF_RUNNING_DIVIDER_SIZE, R.string.pref_running_divider_size_default));
      }
      else if(getString(R.string.PREF_RUNNING_PROGRAMS_SHOW_TIME_PICK_BUTTON_ONLY_WHEN_NEEDED).equals(key)) {
        boolean enabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_running_programs_show_time_pick_button_only_when_needed_default));
        
        if(enabled && !mTimeExtra.getTag().equals(mWhereClauseTime)) {
          mTimeBar.removeView(mTimeExtra);
        }
        else {
          insertTimeExtra(mWhereClauseTime);
        }
      }
      else if(mListView != null && (getString(R.string.PREF_COLOR_SEPARATOR_LINE).equals(key) || getString(R.string.PREF_COLOR_SEPARATOR_SPACE).equals(key))) {
        final Drawable separator = mListView.getDivider();
        
        if(separator instanceof SeparatorDrawable) {
          ((SeparatorDrawable) separator).updateColors(getActivity());
        }
      }
    }
  }
  
  public void selectTime(int time) {
    if(time == Integer.MAX_VALUE) {
      time = -1;
    }
    
    boolean found = false;
    
    for(int i = 0; i < mTimeBar.getChildCount(); i++) {
      View button = mTimeBar.getChildAt(i);

      if(button.getTag().equals(time - 1)) {
        selectButton((Button)button);
        found = true;
        break;
      }
    }

    if(!found) {
      if (time == -1) {
        setWhereClauseTime(-2);
      } else if (time > 0) {
        time--;

        mTimeExtra.setTag(time);

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, time / 60);
        cal.set(Calendar.MINUTE, time % 60);

        mLastExtraClick = System.currentTimeMillis();
        mTimeExtra.setText(DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(cal.getTime()));

        insertTimeExtra(time);

        setWhereClauseTime(mTimeExtra.getTag());

        if (isViewNotVisible(mTimeExtra)) {
          ((HorizontalScrollView) mTimeBar.getParent()).scrollTo(mTimeExtra.getLeft(), mTimeExtra.getTop());
        }
      }
    }
  }
  
  private void selectButton(Button button) {
    if(button != null) {
      button.performClick();
      
      if(isViewNotVisible(button)) {
        ((HorizontalScrollView)mTimeBar.getParent()).scrollTo(button.getLeft(), button.getTop());
      }
    }
  }
  
  private boolean isViewNotVisible(View view) {
    Rect scrollBounds = new Rect();
    ((View)mTimeBar.getParent()).getDrawingRect(scrollBounds);

    float left = view.getX();
    float right = left + view.getWidth();

	  return !(scrollBounds.left < left && scrollBounds.right > right);
  }
}
