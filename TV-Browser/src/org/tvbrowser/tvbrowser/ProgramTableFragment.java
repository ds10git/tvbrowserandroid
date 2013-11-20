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
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.view.ProgramPanel;
import org.tvbrowser.view.ProgramPanelLayout;

import android.annotation.SuppressLint;
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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
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

public class ProgramTableFragment extends Fragment {
  private boolean mKeepRunning;
  private boolean mUpdatingLayout;
  private boolean mUpdatingRunningPrograms;
  private Thread mUpdateThread;
  private View.OnClickListener mClickListener;
  
  private View mMenuView;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mUpdateMarkingsReceiver;
  private BroadcastReceiver mRefreshReceiver;
  
  private int mCurrentLogoValue;
  private boolean mPictureShown;
  private int mTimeBlockSize;
  
  private ProgramPanelLayout mProgramPanelLayout;
  
  private Calendar mCurrentDate;
  
  private boolean mDaySet;
  
  public void scrollToNow() {
    if(isResumed()) {
      StringBuilder where = new StringBuilder();
      where.append(" (( ");
      where.append(TvBrowserContentProvider.DATA_KEY_STARTTIME);
      where.append(" <= ");
      where.append(System.currentTimeMillis());
      where.append(" ) AND ( ");
      where.append(System.currentTimeMillis());
      where.append(" <= ");
      where.append(TvBrowserContentProvider.DATA_KEY_ENDTIME);    
      where.append(" )) ");
      
      Cursor c = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME}, where.toString(), null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
      
      if(c.getCount() > 0) {
        c.moveToFirst();
        
        long id = -1;
        
        do {
          id = c.getLong(c.getColumnIndex(TvBrowserContentProvider.KEY_ID));
        }while((System.currentTimeMillis() - c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))) > ((int)(1.25 * 60 * 60000)) && c.moveToNext());
        
        if(id != -1 && getView() != null) {
          final View view = getView().findViewWithTag(Long.valueOf(id));
          
          if(view != null) {
            final ScrollView scroll = (ScrollView)getView().findViewById(R.id.vertical_program_table_scroll);
            
            scroll.post(new Runnable() {
              @Override
              public void run() {
                int location[] = new int[2];
                view.getLocationInWindow(location);
                
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                
                scroll.scrollTo(scroll.getScrollX(), scroll.getScrollY()+location[1]-display.getHeight()/3);
              }
            });
          }
        }
      }
      
      c.close();
    }
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    mUpdatingRunningPrograms = false;
    mUpdatingLayout = false;
    mCurrentDate = null;
    //mCurrentDay = 0;
    
    mClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Long id = (Long)v.getTag();
        
        if(id != null) {
          UiUtils.showProgramInfo(getActivity(), id);
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
        long id = intent.getLongExtra(SettingConstants.MARKINGS_ID, 0);
        
        if(id > 0 && getView() != null) {
          View view = getView().findViewWithTag(id);
          
          if(view != null) {
            Cursor cursor = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME}, null, null, null);
            
            if(cursor.getCount() > 0) {
              cursor.moveToFirst();
              
              UiUtils.handleMarkings(getActivity(), cursor, view, null);
            }
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
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
    
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
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
    }
    if(mRefreshReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshReceiver);
    }
    if(mUpdateMarkingsReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mUpdateMarkingsReceiver);
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
                        if(!isDetached() && mKeepRunning) {
                          Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                          
                          if(c.getCount() > 0) {
                            c.moveToFirst();
                            UiUtils.handleMarkings(getActivity(), c, progPanel, null);
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
    
  /*  Calendar cal = Calendar.getInstance();
    cal.set(2013, Calendar.DECEMBER, 31);
    
    cal.add(Calendar.DAY_OF_YEAR, 1);*/
    
    Calendar value = Calendar.getInstance();
    
    if(!mDaySet && value.get(Calendar.DAY_OF_YEAR) == mCurrentDate.get(Calendar.DAY_OF_YEAR) && value.get(Calendar.HOUR_OF_DAY) < 4) {
      value.setTime(mCurrentDate.getTime());
      value.add(Calendar.DAY_OF_YEAR, -1);
    }
    else {
      value.setTime(mCurrentDate.getTime());
    }
    
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
        
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME +  " >= " + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + dayEnd;
    
    StringBuilder where3 = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where3.append(" = 1");
    
    Cursor channels = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,TvBrowserContentProvider.CHANNEL_KEY_LOGO}, where3.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    int columnWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
    int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

    String[] projection = null;
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    mPictureShown = pref.getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE), false);
    
    if(mPictureShown) {
      projection = new String[10];
      
      projection[8] = TvBrowserContentProvider.DATA_KEY_PICTURE;
      projection[9] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
    }
    else {
      projection = new String[8];
    }
    
    mTimeBlockSize = Integer.parseInt(pref.getString(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE),"2"));
    
    int logoValue = mCurrentLogoValue = Integer.parseInt(pref.getString(getActivity().getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE), "0"));
    
    projection[0] = TvBrowserContentProvider.KEY_ID;
    projection[1] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    projection[2] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
    projection[3] = TvBrowserContentProvider.DATA_KEY_TITLE;
    projection[4] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
    projection[5] = TvBrowserContentProvider.DATA_KEY_GENRE;
    projection[6] = TvBrowserContentProvider.DATA_KEY_MARKING_VALUES;
    projection[7] = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
    
    LinearLayout channelBar = (LinearLayout)programTable.findViewById(R.id.program_table_channel_bar);
    ArrayList<Integer> channelIDsOrdered = new ArrayList<Integer>();
    
    while(channels.moveToNext()) {
      channelIDsOrdered.add(Integer.valueOf(channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.KEY_ID))));
      
      String name = channels.getString(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
      
      String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
      
      if(shortName != null) {
        name = shortName;
      }
      
      boolean hasLogo = !channels.isNull(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
      
      TextView text = (TextView)inflater.inflate(R.layout.channel_label, channelBar,false);
      text.setTag(channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
      
      if(logoValue == 0 || logoValue == 2 || !hasLogo) {
        text.setText(name);
      }
      
      if((logoValue == 0 || logoValue == 1) && hasLogo) {
        byte[] logoData = channels.getBlob(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
        Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
        BitmapDrawable l = new BitmapDrawable(getResources(), logo);
        
        int height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics());
        
        float percent = height / (float)logo.getHeight(); 
        
        if(percent < 1) {
          l.setBounds(0, 0, (int)(logo.getWidth() * percent), height);
        }
        else {
          l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
        }
        
        text.setCompoundDrawables(l, null, null, null);
        
        int leftPadding = 2 * padding;
        
        if(logoValue == 0 || logoValue == 2 || !hasLogo) {
          leftPadding += text.getPaint().measureText(name);
        }
        if((logoValue == 0 || logoValue == 1) && hasLogo) {
          leftPadding += l.getBounds().width();
        }
        
        leftPadding = (int)(columnWidth - leftPadding);
        
        if(leftPadding > 0) {
          text.setPadding(leftPadding / 2, 0, leftPadding / 2, 0);
        }
      }
      
      channelBar.addView(text);

      channelBar.addView(inflater.inflate(R.layout.separator_line, channelBar, false));
    }
        
    if(channels.getCount() > 0) {
      /*Calendar day = Calendar.getInstance();
      day.setTimeInMillis(mCurrentDay * 1000 * 60 * 60 * 24);*/
      
      mProgramPanelLayout = new ProgramPanelLayout(getActivity(), channelIDsOrdered, mTimeBlockSize, value);
      ViewGroup test = (ViewGroup)programTable.findViewById(R.id.vertical_program_table_scroll);
      test.addView(mProgramPanelLayout);
      
      Cursor cursor = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
      
      while(cursor.moveToNext()) {
        addPanel(cursor, mProgramPanelLayout);
      }
      
      cursor.close();
    }
    
    Calendar test = Calendar.getInstance();
    
    if(isResumed() && (test.get(Calendar.DAY_OF_YEAR) == mCurrentDate.get(Calendar.DAY_OF_YEAR) || test.get(Calendar.DAY_OF_YEAR) - 1 == mCurrentDate.get(Calendar.DAY_OF_YEAR))) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          scrollToNow();
        }
      });
    }
    
    mUpdatingLayout = false;
  }
  
  private void setDayString(TextView currentDay) {
    Date date = mCurrentDate.getTime();
    
    String longDate = DateFormat.getLongDateFormat(getActivity()).format(date);
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    longDate = longDate.replaceAll("\\s+"+cal.get(Calendar.YEAR), "");
    
    currentDay.setText(UiUtils.LONG_DAY_FORMAT.format(date) + "\n" + longDate);
  }
  
  private void changeDay(int count) {
    mCurrentDate.add(Calendar.DAY_OF_YEAR, count);
    
    TextView day = (TextView)getView().findViewById(R.id.show_current_day);
    
    setDayString(day);
    
    RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
    
    updateView(getActivity().getLayoutInflater(), layout);
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    RelativeLayout programTableLayout = (RelativeLayout)inflater.inflate(R.layout.program_table_layout, null);
    
    if(mCurrentDate == null) {
      mCurrentDate = Calendar.getInstance();
      mDaySet = false;
    }
    
    TextView currentDay = (TextView)programTableLayout.findViewById(R.id.show_current_day);
    
    setDayString(currentDay);
    
    currentDay.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        selectDate(view);
      }
    });
    
    programTableLayout.findViewById(R.id.switch_to_previous_day).setOnClickListener(new View.OnClickListener() {
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
    
    updateView(inflater,layout);
    
    return programTableLayout;
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
      
      return UiUtils.handleContextMenuSelection(getActivity(), item, programID, temp);
    }
    
    return false;
  }
  
  public boolean updatePictures() {
    boolean toShow = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE), false);
    
    if(mPictureShown != toShow) {
      updateView(getActivity().getLayoutInflater(), (RelativeLayout)getView().findViewWithTag("LAYOUT"));
    }
    
    return mPictureShown != toShow;
  }
  
  public void updateChannelBar() {
    LinearLayout channelBar = (LinearLayout)getView().findViewById(R.id.program_table_channel_bar);
    
    String[] projection = {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO};
    
    int columnWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
    int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    int logoValue = Integer.parseInt(pref.getString(getActivity().getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE), "0"));
    
    if(channelBar != null && logoValue != mCurrentLogoValue) {
      mCurrentLogoValue = logoValue;
      
      for(int i = 0; i < channelBar.getChildCount(); i++) {
        View view = channelBar.getChildAt(i);
        
        if(view instanceof TextView && view.getTag() instanceof Integer) {
          TextView text = (TextView)view;
          Integer channelKey = (Integer)text.getTag();
          
          Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelKey), projection, null, null, null);
          
          if(channel.moveToFirst()) {
            boolean hasLogo = !channel.isNull(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
            String name = channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            
            String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
            
            if(shortName != null) {
              name = shortName;
            }
            
            if(logoValue == 0 || logoValue == 2 || !hasLogo) {
              text.setText(name);
            }
            else {
              text.setText("");
            }
            
            if((logoValue == 0 || logoValue == 1) && hasLogo) {
              byte[] logoData = channel.getBlob(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
              Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
              BitmapDrawable l = new BitmapDrawable(getResources(), logo);
              
              int height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics());
              
              float percent = height / (float)logo.getHeight(); 
              
              if(percent < 1) {
                l.setBounds(0, 0, (int)(logo.getWidth() * percent), height);
              }
              else {
                l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
              }
              
              text.setCompoundDrawables(l, null, null, null);
              
              int leftPadding = 2 * padding;
              
              if(logoValue == 0 || logoValue == 2 || !hasLogo) {
                leftPadding += text.getPaint().measureText(name);
              }
              if((logoValue == 0 || logoValue == 1) && hasLogo) {
                leftPadding += l.getBounds().width();
              }
              
              leftPadding = (int)(columnWidth - leftPadding);
              
              if(leftPadding > 0) {
                text.setPadding(leftPadding / 2, 0, leftPadding / 2, 0);
              }
              else {
                text.setPadding(2, 0, 0, 0);
              }
            }
            else {
              text.setPadding(2, 0, 0, 0);
              text.setCompoundDrawables(null, null, null, null);
            }
          }
          
          channel.close();
        }
      }
    }
  }
  
  private void addPanel(final Cursor cursor, final ProgramPanelLayout layout) {
    long startTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
    long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
    String title = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
    int channelID = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
    
    ProgramPanel panel = new ProgramPanel(getActivity(),startTime,endTime,title,channelID);
    
    panel.setGenre(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)));
    panel.setEpisode(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)));
    panel.setOnClickListener(mClickListener);
    panel.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
    
    registerForContextMenu(panel);
    
    int pictureColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);
    
    if(pictureColumn != -1 && !cursor.isNull(pictureColumn)) {
      byte[] logoData = cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE));
      Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
                
      BitmapDrawable l = new BitmapDrawable(getResources(), logo);
      l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
      
      panel.setPicture(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)), l);
    }
    
    layout.addView(panel);
    
    UiUtils.handleMarkings(getActivity(), cursor, startTime, endTime, panel, null);
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
        
   /* Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(mCurrentDay * 24 * 60 * 60 * 1000);*/
    
    if (Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1) {
      select.getCalendarView().setFirstDayOfWeek(Calendar.MONDAY);
    }
    
    select.setMinDate(dayStart - 24 * 60 * 60 * 1000);
    select.setMaxDate(dayStart + 15 * (24 * 60 * 60 * 1000));
    select.init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH), null);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        mCurrentDate.set(select.getYear(), select.getMonth(), select.getDayOfMonth());

        setDayString((TextView)getView().findViewById(R.id.show_current_day));
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
    if(mTimeBlockSize != Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getResources().getString(R.string.PROG_PANEL_TIME_BLOCK_SIZE),"2"))) {
      RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
      
      updateView(getActivity().getLayoutInflater(), layout);
      
      return true;
    }
    
    return false;
  }
}
