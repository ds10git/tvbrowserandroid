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
import java.util.HashMap;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;
import org.tvbrowser.view.ChannelLabel;
import org.tvbrowser.view.CompactProgramTableLayout;
import org.tvbrowser.view.ProgramPanel;
import org.tvbrowser.view.ProgramTableLayout;
import org.tvbrowser.view.ProgramTableLayoutConstants;
import org.tvbrowser.view.TimeBlockProgramTableLayout;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class FragmentProgramTable extends Fragment {
  private boolean mKeepRunning;
  private boolean mUpdatingLayout;
  private boolean mUpdatingRunningPrograms;
  private boolean mShowOrderNumbers;
  private boolean mShowGenre;
  private boolean mShowEpisode;
  private boolean mShowInfo;
  
  private Thread mUpdateThread;
  private View.OnClickListener mClickListener;
  
  private View mMenuView;
  private View mPrevious;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mUpdateMarkingsReceiver;
  private BroadcastReceiver mUpdateChannelsReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  
  private int mCurrentLogoValue;
  private boolean mPictureShown;
  private int mTimeBlockSize;
  private int mOldScrollX;
  
  private ProgramTableLayout mProgramPanelLayout;
  
  private Calendar mCurrentDate;
  
  private boolean mDaySet;
  
  private boolean mGrowPanels;
  
  private ArrayList<Integer> mShowInfos;
  
  private int mStartTimeIndex;
  private int mEndTimeIndex;
  private int mTitleIndex;
  private int mChannelIndex;
  private int mGenreIndex;
  private int mEpisodeIndex;
  private int mKeyIndex;
  private int mPictureIndex;
  private int mPictureCopyrightIndex;
  private int mCategoryIndex;
  
  private HashMap<String, Integer> mMarkingsMap;
  
  public void scrollToTime(int time, final MenuItem timeItem) {
    if(isResumed()) {
      long value = System.currentTimeMillis();
      
      if(time == 0) {
        boolean isInRange = (mCurrentDate.get(Calendar.DAY_OF_YEAR) +1 == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 4;
        
        if(!isInRange && (mCurrentDate.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR))) {
          if(timeItem != null) {
            timeItem.setActionView(R.layout.progressbar);
          }
          
          handler.post(new Runnable() {
            @Override
            public void run() {
              now();
              
              if(timeItem != null) {
                timeItem.setActionView(null);
              }
            }
          });
          
          time = -1;
        }
      }
      
      if(time > 0) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(mCurrentDate.getTimeInMillis());
        time--;
        
        now.set(Calendar.HOUR_OF_DAY, time / 60);
        now.set(Calendar.MINUTE, time % 60);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        
        value = now.getTimeInMillis();
      }
      
      if(time >= 0) {
        StringBuilder where = new StringBuilder();
        
        where.append(" (( ");
        where.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
        where.append("<=");
        where.append(value);
        where.append(" ) AND ( ");
        where.append(value);
        where.append("<=");
        where.append(TvBrowserContentProvider.DATA_KEY_ENDTIME);    
        where.append(" )) ");
        where.append(((TvBrowser)getActivity()).getFilterSelection(false));
        
        String[] infoNames = TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY;
        
        String[] projection = new String[4+infoNames.length];
        
        projection[0] = TvBrowserContentProvider.KEY_ID;
        projection[1] = TvBrowserContentProvider.DATA_KEY_TITLE;
        projection[2] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
        projection[3] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
        
        for(int i = 0; i < infoNames.length; i++) {
          projection[4+i] = infoNames[i];
        }
        
        
        if(IOUtils.isDatabaseAccessible(getActivity())) {
          Cursor c = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where.toString(), null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
          
          if(c.moveToFirst()) {
            long id = -1;
            
            do {
              id = c.getLong(c.getColumnIndex(TvBrowserContentProvider.KEY_ID));
            }while(((getView().findViewWithTag(Long.valueOf(id)) == null) || (value - c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))) > ((int)(1.25 * 60 * 60000))) && c.moveToNext());
            
            if(id != -1 && getView() != null) {
              final View view = getView().findViewWithTag(Long.valueOf(id));
              
              if(view != null) {
                final ScrollView scroll = (ScrollView)getView().findViewById(R.id.vertical_program_table_scroll);
                
                scroll.post(new Runnable() {
                  @Override
                  public void run() {
                    int location[] = new int[2];
                    view.getLocationInWindow(location);
                                        
                    scroll.scrollTo(scroll.getScrollX(), scroll.getScrollY()+location[1]);
                  }
                });
              }
            }
          }
          
          IOUtils.close(c);
        }
      }
    }
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    mShowInfos = new ArrayList<Integer>();
    mMarkingsMap = new HashMap<String, Integer>();
    
    mUpdatingRunningPrograms = false;
    mUpdatingLayout = false;
    mCurrentDate = null;
    mOldScrollX = -1;
    //mCurrentDay = 0;
    
    mClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Long id = (Long)v.getTag();
        
        if(id != null) {
          UiUtils.showProgramInfo(getActivity(), id, null, handler);
        }
      }
    };
  }

  
  Handler handler = new Handler();
  
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
    
    mUpdateMarkingsReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(SettingConstants.EXTRA_MARKINGS_ID, 0);
        
        if(id > 0 && getView() != null) {
          View view = getView().findViewWithTag(id);
          
          if(view != null && IOUtils.isDatabaseAccessible(context)) {
            String[] projection = TvBrowserContentProvider.getColumnArrayWithMarkingColums(TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME);
            
            Cursor cursor = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), projection, null, null, null);
            Log.d("info2", "CURSOR " + cursor);
            if(cursor.getCount() > 0) {
              cursor.moveToFirst();
              Log.d("info2", "SIZE " + cursor.getCount());
              UiUtils.handleMarkings(getActivity(), cursor, view, null, null, true);
            }
            
            cursor.close();
          }
        }
      }
    };
    
    IntentFilter filter = new IntentFilter(SettingConstants.MARKINGS_CHANGED);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mUpdateMarkingsReceiver, filter);
    
    mDataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && getView() != null) {
              RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
              
              if(layout != null) {
                updateView(getActivity().getLayoutInflater(),layout);
              }
            }
          }
        });
      }
    };
    
    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    getActivity().registerReceiver(mDataUpdateReceiver, intent);
    
    mUpdateChannelsReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && getView() != null) {
              RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
              
              if(layout != null) {
                updateView(getActivity().getLayoutInflater(),layout);
              }
            }
          }
        });
      }
    };
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE));
    
    mDontWantToSeeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(!isDetached() && getView() != null) {
          if(intent.getBooleanExtra(SettingConstants.DONT_WANT_TO_SEE_ADDED_EXTRA, true)) {
            if(mProgramPanelLayout != null && IOUtils.isDatabaseAccessible(getActivity())) {
              for(int i = 0; i < mProgramPanelLayout.getChildCount(); i++) {
                View child = mProgramPanelLayout.getChildAt(i);
                
                long programID = (Long)child.getTag();
                
                Cursor test = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA,programID), new String[] {TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE}, null, null, null);
                test.moveToPosition(-1);
                
                if(test.moveToNext()) {
                  if(test.getInt(0) == 1) {
                    child.setVisibility(View.GONE);
                  }
                }
                
                test.close();
              }
            }
          }
          else {
            RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
            
            if(layout != null) {
              updateView(getActivity().getLayoutInflater(),layout);
            }
          }
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
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshReceiver, SettingConstants.RERESH_FILTER);
    
    mKeepRunning = true;
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    mKeepRunning = false;
    
    if(mDataUpdateReceiver != null) {
      getActivity().unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
    if(mUpdateMarkingsReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mUpdateMarkingsReceiver);
    }
    if(mUpdateChannelsReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mUpdateChannelsReceiver);
    }
    if(mDontWantToSeeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDontWantToSeeReceiver);
    }
  }
  
  private void startUpdateThread() {
    if(mUpdateThread == null || !mUpdateThread.isAlive()) {
      mUpdateThread = new Thread() {
        public void run() {
          if(mKeepRunning && TvBrowserContentProvider.INFORM_FOR_CHANGES && !mUpdatingLayout) {
            mUpdatingRunningPrograms = true;
            
            if(!isDetached() && mProgramPanelLayout != null) {
              for(int k = 0; k < mProgramPanelLayout.getChildCount(); k++) {
                View mainChild = mProgramPanelLayout.getChildAt(k);
                
                if(mainChild instanceof ProgramPanel) {
                  final ProgramPanel progPanel = (ProgramPanel)mainChild;
                  
                  if(progPanel.isOnAir()) {
                    handler.post(new Runnable() {
                      @Override
                      public void run() {
                        if(!isDetached() && mKeepRunning && IOUtils.isDatabaseAccessible(getActivity())) {
                          String[] projection = TvBrowserContentProvider.getColumnArrayWithMarkingColums(TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME);
                          
                          Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), projection, null, null, null);
                          
                          if(c.getCount() > 0) {
                            c.moveToFirst();
                            UiUtils.handleMarkings(getActivity(), c, progPanel, null, null, true);
                          }
                          
                          c.close();
                        }
                      }
                    });
                  }
                  else {
                    progPanel.checkExpired(handler);
                  }
                }
              }
            }
          }
        
          mUpdatingRunningPrograms = false;
        }
      };
      
      mUpdateThread.setPriority(Thread.MIN_PRIORITY);
      mUpdateThread.start();
    }
  }
  
  public void updateMarkings() {
    
    if(mUpdateThread == null || !mUpdateThread.isAlive()) {
      mUpdateThread = new Thread() {
        @Override
        public void run() {
          if(!isDetached() && getActivity() != null && IOUtils.isDatabaseAccessible(getActivity())) {
            Calendar value = Calendar.getInstance();
            value.setTime(mCurrentDate.getTime());
            
            value.set(Calendar.HOUR_OF_DAY, 0);
            value.set(Calendar.MINUTE, 0);
            value.set(Calendar.SECOND, 0);
            value.set(Calendar.MILLISECOND, 0);
            
            long dayStart = value.getTimeInMillis();
            
            mDaySet = true;
                
            long dayEnd = dayStart + 28 * 60 * 60 * 1000;
            
            String where = TvBrowserContentProvider.DATA_KEY_STARTTIME +  ">=" + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + dayEnd + " AND ( " + TextUtils.join(" OR ", TvBrowserContentProvider.MARKING_COLUMNS) + " ) ";
            
            String[] projection = TvBrowserContentProvider.getColumnArrayWithMarkingColums(TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME);
            
            Cursor c = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.KEY_ID);
            c.moveToPosition(-1);
            
            int keyColumnIndex = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
            int statTimeColumnIndex = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
            int endTimeColumnIndex = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
            
            HashMap<String, Integer> markingColumsIndexMap = new HashMap<String, Integer>();
            
            for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
              int index = c.getColumnIndex(column);
              
              if(index >= 0) {
                markingColumsIndexMap.put(column, Integer.valueOf(index));
              }
            }
            
            while(c.moveToNext()) {
              long key = c.getLong(keyColumnIndex);
              
              View view = mProgramPanelLayout.findViewWithTag(Long.valueOf(key));
              
              if(view != null) {
                long startTime = c.getLong(statTimeColumnIndex);
                long endTime = c.getLong(endTimeColumnIndex);
                
                ArrayList<String> markedColumns = new ArrayList<String>();
                
                for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
                  Integer index = markingColumsIndexMap.get(column);
                  
                  if(index != null && c.getInt(index.intValue()) == 1) {
                    markedColumns.add(column);
                  }
                }
                
                UiUtils.handleMarkings(getActivity(), null, startTime, endTime, view, IOUtils.getStringArrayFromList(markedColumns), handler, true);
              }
            }
            
            c.close();
          }
        }
      };
      mUpdateThread.start();
    }
  }
  
  public void updateView(LayoutInflater inflater, ViewGroup container) {
    if(mUpdatingRunningPrograms) {
      Thread t = new Thread() {
        public void run() {
          while(mUpdatingRunningPrograms) {
            try {
              sleep(200);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      };
      t.start();
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    
    mUpdatingLayout = true;
    
    if(mProgramPanelLayout != null) {
      mProgramPanelLayout.clear();
    }
    
    container.removeAllViews();
    
    View programTable = inflater.inflate(R.layout.program_table, container);
    
    int[] infoPrefKeyArr = SettingConstants.CATEGORY_PREF_KEY_ARR;
    
    for(int infoKey : infoPrefKeyArr) {
      if(PrefUtils.getBooleanValue(infoKey, R.bool.pref_info_show_default)) {
        mShowInfos.add(Integer.valueOf(infoKey));
      }
    }
    
  /*  Calendar cal = Calendar.getInstance();
    cal.set(2013, Calendar.DECEMBER, 31);
    
    cal.add(Calendar.DAY_OF_YEAR, 1);*/
    
    Calendar value = Calendar.getInstance();
    
    if(!mDaySet && value.get(Calendar.DAY_OF_YEAR) == mCurrentDate.get(Calendar.DAY_OF_YEAR) && value.get(Calendar.HOUR_OF_DAY) < 4) {
      mCurrentDate.add(Calendar.DAY_OF_YEAR, -1);
      
      TextView day = (TextView)((ViewGroup)container.getParent()).findViewById(R.id.show_current_day);
      
      setDayString(day);
    }
    
    value.setTime(mCurrentDate.getTime());
    
    value.set(Calendar.HOUR_OF_DAY, 0);
    value.set(Calendar.MINUTE, 0);
    value.set(Calendar.SECOND, 0);
    value.set(Calendar.MILLISECOND, 0);
    
    //long testDay = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
    long dayStart = value.getTimeInMillis();
   // dayStart -=  TimeZone.getDefault().getOffset(dayStart);
    
  /*  if(!mDaySet && testDay == mCurrentDay && System.currentTimeMillis() - dayStart < 4 * 60 * 60 * 1000) {
      dayStart = --mCurrentDay * 24 * 60 * 60 * 1000 - TimeZone.getDefault().getOffset(dayStart);
    }*/
    
    mDaySet = true;
        
    long dayEnd = dayStart + 28 * 60 * 60 * 1000;
        
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME +  ">=" + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + dayEnd;
    
    StringBuilder where3 = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where3.append("=1");
    
    where3.append(((TvBrowser)getActivity()).getFilterSelection(true).replace(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, TvBrowserContentProvider.KEY_ID));
    
    if(IOUtils.isDatabaseAccessible(getActivity())) {
      Cursor channels = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,TvBrowserContentProvider.CHANNEL_KEY_LOGO}, where3.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      
      try {
        if(IOUtils.prepareAccess(channels)) {
          String[] projection = null;
          
          mPictureShown = PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE, R.bool.prog_table_show_pictures_default);
          mShowGenre = PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_PROGRAM_TABLE, R.bool.prog_table_show_genre_default);
          mShowEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_PROGRAM_TABLE, R.bool.prog_table_show_episode_default);
          mShowInfo = PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_PROGRAM_TABLE, R.bool.prog_table_show_infos_default);
          
          int orderNumberColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
          mShowOrderNumbers = ProgramTableLayoutConstants.getShowOrderNumber();
          
          String[] infoCategories = TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY;
          
          if(mPictureShown) {
            projection = new String[10 + TvBrowserContentProvider.MARKING_COLUMNS.length + infoCategories.length];
            
            projection[projection.length-2] = TvBrowserContentProvider.DATA_KEY_PICTURE;
            projection[projection.length-1] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
          }
          else {
            projection = new String[8 + TvBrowserContentProvider.MARKING_COLUMNS.length + infoCategories.length];
          }
          
          mTimeBlockSize = Integer.parseInt(PrefUtils.getStringValue(R.string.PROG_PANEL_TIME_BLOCK_SIZE, R.string.prog_panel_time_block_size));
          
          projection[0] = TvBrowserContentProvider.KEY_ID;
          projection[1] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
          projection[2] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
          projection[3] = TvBrowserContentProvider.DATA_KEY_TITLE;
          projection[4] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
          projection[5] = TvBrowserContentProvider.DATA_KEY_GENRE;
          projection[6] = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
          projection[7] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
          
          for(int i = 0; i < infoCategories.length; i++) {
            projection[8+i] = infoCategories[i];
          }
          
          int startIndex = 8 + infoCategories.length;
            
          for(int i = startIndex; i < (startIndex + TvBrowserContentProvider.MARKING_COLUMNS.length); i++) {
            projection[i] = TvBrowserContentProvider.MARKING_COLUMNS[i-startIndex];
          }
          
          LinearLayout channelBar = (LinearLayout)programTable.findViewById(R.id.program_table_channel_bar);
          ArrayList<Integer> channelIDsOrdered = new ArrayList<Integer>();
          
          while(channels.moveToNext()) {
            channelIDsOrdered.add(Integer.valueOf(channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.KEY_ID))));
            
            String name = channels.getString(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            
            String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
            
            if(shortName != null) {
              name = shortName;
            }
            
            int orderNumber = channels.getInt(orderNumberColumn);
            
            Bitmap logo = UiUtils.createBitmapFromByteArray(channels.getBlob(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));
      
            if(logo != null) {
              int height = ProgramTableLayoutConstants.getChannelMaxFontHeight();
              
              float percent = height / (float)logo.getHeight();         
              
              if(percent < 1) {
                logo = Bitmap.createScaledBitmap(logo, (int)(logo.getWidth() * percent), height, true);
              }
            }
            
            ChannelLabel channelLabel = new ChannelLabel(getActivity(), name, logo, orderNumber);
            
            channelBar.addView(channelLabel);
          }
              
          if(channels.getCount() > 0) {
            mGrowPanels = PrefUtils.getBooleanValue(R.string.PROG_PANEL_GROW, R.bool.prog_panel_grow_default);
            
            if(PrefUtils.getStringValue(R.string.PROG_TABLE_LAYOUT, R.string.prog_table_layout_default).equals("0")) {
              mProgramPanelLayout = new TimeBlockProgramTableLayout(getActivity(), channelIDsOrdered, mTimeBlockSize, value, mGrowPanels);
            }
            else {
              mProgramPanelLayout = new CompactProgramTableLayout(getActivity(), channelIDsOrdered);
            }
            
            ViewGroup test = (ViewGroup)programTable.findViewById(R.id.vertical_program_table_scroll);
            test.addView(mProgramPanelLayout);
            
            where += UiUtils.getDontWantToSeeFilterString(getActivity());
            where += ((TvBrowser)getActivity()).getCategoryFilterSelection();
            
            Cursor cursor = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
            cursor.moveToPosition(-1);
            
            mStartTimeIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
            mEndTimeIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME);
            mTitleIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
            mChannelIndex = cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
            mGenreIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE);
            mEpisodeIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE);
            mKeyIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
            mPictureIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);
            mPictureCopyrightIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT);
            
            mMarkingsMap.clear();
            
            for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
              int index = cursor.getColumnIndex(column);
              
              if(index >= 0) {
                mMarkingsMap.put(column, Integer.valueOf(index));
              }
            }
            
            mCategoryIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES);
            
            while(cursor.moveToNext()) {
              addPanel(cursor, mProgramPanelLayout);
            }
            
            cursor.close();
          }
          
          if(mProgramPanelLayout instanceof CompactProgramTableLayout) {
            channelBar.removeViewAt(0);
            channelBar.removeViewAt(0);
          }
          
          Calendar test = Calendar.getInstance();
          
          if(test.get(Calendar.DAY_OF_YEAR) == mCurrentDate.get(Calendar.DAY_OF_YEAR) || test.get(Calendar.DAY_OF_YEAR) - 2 == mCurrentDate.get(Calendar.DAY_OF_YEAR)) {
            handler.post(new Runnable() {
              @Override
              public void run() {
                scrollToTime(0,null);
              }
            });
          }
          
          handler.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            @Override
            public void run() {
              if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                View view = getView();
                
                if(view != null) {
                  View scroll = view.findViewById(R.id.horizontal_program_table_scroll);
                  
                  if(scroll != null) {
                    scroll.setScrollX(mOldScrollX);
                  }
                }
              }
              
              mOldScrollX = 0;
            }
          });
        }
      }finally {
        IOUtils.close(channels);
      }
    }
    
    mUpdatingLayout = false;
  }
  
  private void setDayString(TextView currentDay) {
    if(mPrevious != null) {
      final Calendar test = Calendar.getInstance();
      test.add(Calendar.DAY_OF_YEAR, -1);
      test.set(Calendar.HOUR_OF_DAY, 0);
      test.set(Calendar.MINUTE, 0);
      test.set(Calendar.SECOND, 0);
      test.set(Calendar.MILLISECOND, 0);
      
      final Calendar test2 = Calendar.getInstance();
      test2.setTimeInMillis(mCurrentDate.getTimeInMillis());
      test2.set(Calendar.HOUR_OF_DAY, 0);
      test2.set(Calendar.MINUTE, 0);
      test2.set(Calendar.SECOND, 0);
      test2.set(Calendar.MILLISECOND, 0);
      
      handler.post(new Runnable() {
        @Override
        public void run() {
          mPrevious.setEnabled(test2.compareTo(test) > 0);
        }
      });
    }
    Date date = mCurrentDate.getTime();
    
    String longDate = DateFormat.getLongDateFormat(getActivity()).format(date);
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    longDate = longDate.replaceAll("\\s+"+cal.get(Calendar.YEAR), "");
    
    currentDay.setText(UiUtils.LONG_DAY_FORMAT.format(date) + "\n" + longDate);
  }
  
  private void now() {
    mCurrentDate = Calendar.getInstance();
    
    TextView day = (TextView)getView().findViewById(R.id.show_current_day);
    
    setDayString(day);
    
    RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
    
    mOldScrollX = getView().findViewById(R.id.horizontal_program_table_scroll).getScrollX();
    
    updateView(getActivity().getLayoutInflater(), layout);
  }
  
  private void changeDay(int count) {    
    mCurrentDate.add(Calendar.DAY_OF_YEAR, count);
    
    TextView day = (TextView)getView().findViewById(R.id.show_current_day);
    
    setDayString(day);
    
    RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
    
    View parent = getView();
    
    if(parent != null) {
      View horizontalScroll = parent.findViewById(R.id.horizontal_program_table_scroll);
      
      if(horizontalScroll != null) {
        mOldScrollX = horizontalScroll.getScrollX();
    
        updateView(getActivity().getLayoutInflater(), layout);
      }
    }
  }
  
  void updateView(LayoutInflater inflater) {
    RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
    
    if(layout != null && inflater != null) {
      updateView(inflater, layout);
    }
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    RelativeLayout programTableLayout = (RelativeLayout)inflater.inflate(R.layout.program_table_layout, container, false);
    
    if(mCurrentDate == null) {
      mCurrentDate = Calendar.getInstance();
      mDaySet = false;
    }
    
    if(SettingConstants.IS_DARK_THEME) {
      programTableLayout.findViewById(R.id.button_panel).setBackgroundColor(getResources().getColor(R.color.background_material_dark));
    }
    
    ProgramTableLayoutConstants.initialize(getActivity());
    
    TextView currentDay = (TextView)programTableLayout.findViewById(R.id.show_current_day);
    
    setDayString(currentDay);
    
    currentDay.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        selectDate(view);
      }
    });
    
    mPrevious = programTableLayout.findViewById(R.id.switch_to_previous_day);
    
    mPrevious.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        changeDay(-1);
      }
    });
    
    programTableLayout.findViewById(R.id.switch_to_next_day).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        changeDay(1);
      }
    });

    RelativeLayout layout = (RelativeLayout)programTableLayout.findViewById(R.id.program_table_base);
    layout.setTag("LAYOUT");
    
    int startTab = Integer.parseInt(PrefUtils.getStringValue(R.string.TAB_TO_SHOW_AT_START, R.string.tab_to_show_at_start_default));
    
    if(!PrefUtils.getBooleanValue(R.string.PROG_TABLE_DELAYED, R.bool.prog_table_delayed_default) || startTab == 3) {
      updateView(inflater,layout);
    }
    
    return programTableLayout;
  }
  
  public void removed() {
    mDaySet = false;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    mMenuView = v;
    long programID = ((Long)v.getTag());
    
    UiUtils.createContextMenu(getActivity(), menu, programID);
  }
  
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(mMenuView != null) {
      View temp = mMenuView;
      mMenuView = null;
    
      long programID = ((Long)temp.getTag());
      
      return UiUtils.handleContextMenuSelection(getActivity(), item, programID, temp, getActivity().getCurrentFocus());
    }
    
    return false;
  }
  
  public boolean updateTable() {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    boolean toShow = pref.getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE), false);
    boolean toGrow = PrefUtils.getBooleanValue(R.string.PROG_PANEL_GROW, R.bool.prog_panel_grow_default);
    boolean updateLayout = (PrefUtils.getStringValue(R.string.PROG_TABLE_LAYOUT, R.string.prog_table_layout_default).equals("0") && mProgramPanelLayout instanceof CompactProgramTableLayout) || 
        (PrefUtils.getStringValue(R.string.PROG_TABLE_LAYOUT, R.string.prog_table_layout_default).equals("1") && mProgramPanelLayout instanceof TimeBlockProgramTableLayout);
    boolean updateWidth = PrefUtils.getIntValueWithDefaultKey(R.string.PROG_TABLE_COLUMN_WIDTH, R.integer.prog_table_column_width_default) != ProgramTableLayoutConstants.getRawColumnWidth();
    boolean updateTextScale = Float.valueOf(PrefUtils.getStringValue(R.string.PROG_TABLE_TEXT_SCALE, R.string.prog_table_text_scale_default)) != ProgramTableLayoutConstants.getTextScale();
    boolean updateShownValues = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_PROGRAM_TABLE, R.bool.prog_table_show_episode_default) != mShowEpisode ||
        PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_PROGRAM_TABLE, R.bool.prog_table_show_genre_default) != mShowGenre ||
            PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_PROGRAM_TABLE, R.bool.prog_table_show_infos_default) != mShowInfo;
    
    boolean updateInfoValues = PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_PROGRAM_TABLE, R.bool.prog_table_show_infos_default);
    
    if(updateInfoValues) {
      updateInfoValues = false;
      
      int[] infoPrefKeyArr = SettingConstants.CATEGORY_PREF_KEY_ARR;
      
      for(int infoKey : infoPrefKeyArr) {
        boolean isShownSetting = PrefUtils.getBooleanValue(infoKey, R.bool.pref_info_show_default);
        boolean isCurrentlyShown = mShowInfos.contains(Integer.valueOf(infoKey));
        
        if((isShownSetting && !isCurrentlyShown) || (!isShownSetting && isCurrentlyShown)) {
          updateInfoValues = true;
          break;
        }
      }
    }
    
    if(updateTextScale) {
      ProgramTableLayoutConstants.initialize(getActivity());
    }
    else if(updateWidth) {
      ProgramTableLayoutConstants.updateColumnWidth(getActivity());
    }
    
    if(mPictureShown != toShow || mGrowPanels != toGrow || updateLayout || updateWidth || updateTextScale || updateShownValues || updateInfoValues) {
      updateView(getActivity().getLayoutInflater(), (RelativeLayout)getView().findViewWithTag("LAYOUT"));
    }
    
    return mPictureShown != toShow || mGrowPanels != toGrow || updateLayout || updateWidth || updateTextScale || updateShownValues || updateInfoValues;
  }
  
  public void firstLoad(LayoutInflater inflater) {
    if(!mDaySet) {
      updateView(inflater);
    }
  }
  
  public void updateChannelBar() {
    LinearLayout channelBar = (LinearLayout)getView().findViewById(R.id.program_table_channel_bar);
    
    ProgramTableLayoutConstants.updateChannelLogoName(getActivity());
    
    int logoValue = Integer.parseInt(PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE, R.string.channel_logo_name_program_table_default));
    
    if(channelBar != null && (logoValue != mCurrentLogoValue || ProgramTableLayoutConstants.getShowOrderNumber() != mShowOrderNumbers)) {
      mCurrentLogoValue = logoValue;
      mShowOrderNumbers = ProgramTableLayoutConstants.getShowOrderNumber();
      
      for(int i = 0; i < channelBar.getChildCount(); i++) {
        View view = channelBar.getChildAt(i);
        
        if(view instanceof ChannelLabel) {
          ((ChannelLabel)view).updateNameAndLogo();
          view.invalidate();
        }
      }
    }
  }
  
  private void addPanel(final Cursor cursor, final ProgramTableLayout layout) {
    final long programId = cursor.getLong(mKeyIndex);
    final long startTime = cursor.getLong(mStartTimeIndex);
    final long endTime = cursor.getLong(mEndTimeIndex);
    String title = cursor.getString(mTitleIndex);
    int channelID = cursor.getInt(mChannelIndex);
    Spannable categories = IOUtils.getInfoString(cursor.getInt(mCategoryIndex),getResources(),false);
    
    final ProgramPanel panel = new ProgramPanel(getActivity(),startTime,endTime,title,channelID);
    
    if(mShowGenre) {
      panel.setGenre(cursor.getString(mGenreIndex));
    }
    if(mShowEpisode) {
      panel.setEpisode(cursor.getString(mEpisodeIndex));
    }
    if(mShowInfo) {
      panel.setInfoString(categories);
    }
    
    panel.setOnClickListener(mClickListener);
    panel.setTag(programId);
    
    registerForContextMenu(panel);
        
    if(mPictureIndex != -1) {
      Bitmap logo = UiUtils.createBitmapFromByteArray(cursor.getBlob(mPictureIndex));
      
      if(logo != null) {
        BitmapDrawable l = new BitmapDrawable(getResources(), logo);
        l.setBounds(0, 0, (int)(ProgramTableLayoutConstants.getZoom() * logo.getWidth()), (int)(ProgramTableLayoutConstants.getZoom() * logo.getHeight()));
        
        panel.setPicture(cursor.getString(mPictureCopyrightIndex), l);
      }
    }
    
    layout.addView(panel);
    
    ArrayList<String> markedColumns = new ArrayList<String>();
    
    for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
      Integer value = mMarkingsMap.get(column);
      
      if(value != null && cursor.getInt(value.intValue()) == 1) {
        markedColumns.add(column);
      }
      else if(column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING) && ProgramUtils.isMarkedWithIcon(getActivity(), programId)) {
        markedColumns.add(column);
      }
    }
    
    UiUtils.handleMarkings(getActivity(), null, startTime, endTime, panel, IOUtils.getStringArrayFromList(markedColumns), null, true);
  }
  
  @SuppressLint("NewApi")
  public void selectDate(View view) {try {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    
    long testDay = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
    long dayStart = testDay * 24 * 60 * 60 * 1000;
    
    if(System.currentTimeMillis() - dayStart < 4 * 60 * 60 * 1000) {
      dayStart = --testDay * 24 * 60 * 60 * 1000 - TimeZone.getDefault().getOffset(dayStart);
    }
    
    final DatePicker select = new DatePicker(getActivity());
    
    if (Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1 && 
        Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
      select.getCalendarView().setFirstDayOfWeek(Calendar.MONDAY);
    }
    
    select.setMinDate(dayStart - 24 * 60 * 60 * 1000);
    select.setMaxDate(dayStart + 21 * (24 * 60 * 60 * 1000));
    select.init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH), null);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        mCurrentDate.set(select.getYear(), select.getMonth(), select.getDayOfMonth());

        setDayString((TextView)getView().findViewById(R.id.show_current_day));
        
        View view = getView().findViewById(R.id.horizontal_program_table_scroll);
        
        if(view != null) {
          mOldScrollX = view.getScrollX();
        }
        
        updateView(getActivity().getLayoutInflater(), (RelativeLayout)getView().findViewWithTag("LAYOUT"));
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {}
    });
    
    HorizontalScrollView scroll = new HorizontalScrollView(getActivity());
    scroll.addView(select);
    
    builder.setView(scroll);
    
    builder.show();}catch(Throwable t) {}
  }
  
  public boolean checkTimeBlockSize() {
    if(mTimeBlockSize != Integer.parseInt(PrefUtils.getStringValue(R.string.PROG_PANEL_TIME_BLOCK_SIZE, R.string.prog_panel_time_block_size))) {
      RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
      
      updateView(getActivity().getLayoutInflater(), layout);
      
      return true;
    }
    
    return false;
  }
}
