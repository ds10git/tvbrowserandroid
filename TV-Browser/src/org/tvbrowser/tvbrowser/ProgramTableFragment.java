package org.tvbrowser.tvbrowser;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

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
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
  
  private long mCurrentDay;
  private View mMenuView;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mUpdateMarkingsReceiver;
  private BroadcastReceiver mRefreshReceiver;
  
  private int mCurrentLogoValue;
  private boolean mPictureShown;
  
  public void scrollToNow() {
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
      }while((System.currentTimeMillis() - c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))) > ((int)(1.5 * 60 * 60000)) && c.moveToNext());
      
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
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    mUpdatingRunningPrograms = false;
    mUpdatingLayout = false;
    mCurrentDay = 0;
    
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
            
            if(!isDetached()) {
              LinearLayout main = (LinearLayout)getView().findViewById(R.id.MAIN_LAYOUT);
              
              for(int k = 0; k < main.getChildCount(); k++) {
                View mainChild = main.getChildAt(k);
                
                if(mainChild instanceof LinearLayout) {
                  LinearLayout layout = (LinearLayout)mainChild;
                  
                  for(int i = 0; i < layout.getChildCount(); i++) {
                    View child = layout.getChildAt(i);
                    
                    if(child instanceof LinearLayout) {
                      LinearLayout block = (LinearLayout)child;
                      
                      for(int j = 0; j < block.getChildCount(); j++) {
                        View blockChild = block.getChildAt(j);
                        
                        if(blockChild instanceof RelativeLayout) {
                          final RelativeLayout progPanel = (RelativeLayout)blockChild;
                          
                          if(progPanel.getTag() != null && getActivity() != null) {
                                
                            Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                            
                            if(c.getCount() > 0) {
                              c.moveToFirst();
                              
                              boolean expiredTest = false;
                              
                              if(progPanel.getTag(R.id.expired_tag) != null) {
                                expiredTest = (Boolean)progPanel.getTag(R.id.expired_tag);
                              }
                                                                  
                              final boolean expired = expiredTest;
                              
                              long startTime = c.getLong(0);
                              long endTime = c.getLong(1);
                              
                              final TextView title = (TextView)progPanel.findViewById(R.id.prog_panel_title);
                              final TextView text = (TextView)progPanel.findViewById(R.id.prog_panel_start_time);
                              final TextView genre = (TextView)progPanel.findViewById(R.id.prog_panel_genre);
                              final TextView episode = (TextView)progPanel.findViewById(R.id.prog_panel_episode);
                              
                              if(!expired && endTime <= System.currentTimeMillis()) {
                                handler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                    text.setTextColor(Color.rgb(190, 190, 190));
                                    title.setTextColor(Color.rgb(190, 190, 190));
                                    episode.setTextColor(Color.rgb(190, 190, 190));
                                    genre.setTextColor(Color.rgb(190, 190, 190));
                                    
                                    Drawable[] compoundDrawables = genre.getCompoundDrawables();
                                    
                                    if(compoundDrawables != null && compoundDrawables[1] != null) {
                                      compoundDrawables[1].setColorFilter(getActivity().getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
                                    }
                                    
                                    if(!isDetached() && mKeepRunning && !isRemoving()) {
                                      Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                                      
                                      if(c.getCount() > 0) {
                                        c.moveToFirst();
                                        UiUtils.handleMarkings(getActivity(), c, progPanel, null);
                                      }
                                      
                                      c.close();
                                      
                                      progPanel.setTag(R.id.expired_tag, true);
                                    }
                                  }
                                });
                              }
                              else if(System.currentTimeMillis() >= startTime && System.currentTimeMillis() <= endTime) {
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
                            }
                            
                            c.close();
                          }                            
                        }
                      }
                    }
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
    
    container.removeAllViews();
    
    View programTable = inflater.inflate(R.layout.program_table, container);
    
    Calendar cal = Calendar.getInstance();
    cal.set(2013, Calendar.DECEMBER, 31);
    
    cal.add(Calendar.DAY_OF_YEAR, 1);
    
    long testDay = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
    long dayStart = mCurrentDay * 24 * 60 * 60 * 1000;
    dayStart -=  TimeZone.getDefault().getOffset(dayStart);
    
    if(testDay == mCurrentDay && System.currentTimeMillis() - dayStart < 4 * 60 * 60 * 1000) {
      dayStart = --mCurrentDay * 24 * 60 * 60 * 1000 - TimeZone.getDefault().getOffset(dayStart);
    }
        
    long dayEnd = (mCurrentDay+1) * 24 * 60 * 60 * 1000 + 4 * 60 * 60 * 1000 - TimeZone.getDefault().getOffset(dayStart);
        
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME +  " >= " + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + dayEnd;
    
    StringBuilder where3 = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where3.append(" = 1");
    
    Cursor channels = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,TvBrowserContentProvider.CHANNEL_KEY_LOGO}, where3.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    int columnWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
    int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

    String[] projection = null;
    
    mPictureShown = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_PROGRAM_TABLE), false);
    
    if(mPictureShown) {
      projection = new String[9];
      
      projection[7] = TvBrowserContentProvider.DATA_KEY_PICTURE;
      projection[8] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
    }
    else {
      projection = new String[7];
    }
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    int logoValue = mCurrentLogoValue = Integer.parseInt(pref.getString(getActivity().getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAM_TABLE), "0"));
    
    projection[0] = TvBrowserContentProvider.KEY_ID;
    projection[1] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    projection[2] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
    projection[3] = TvBrowserContentProvider.DATA_KEY_TITLE;
    projection[4] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
    projection[5] = TvBrowserContentProvider.DATA_KEY_GENRE;
    projection[6] = TvBrowserContentProvider.DATA_KEY_MARKING_VALUES;
    
    if(channels.getCount() > 0) {
      channels.moveToFirst();
      
      LinearLayout channelBar = (LinearLayout)programTable.findViewById(R.id.program_table_channel_bar);
      
      LinearLayout[] timeBlockLines = new LinearLayout[14];
      
      timeBlockLines[0] = (LinearLayout)programTable.findViewById(R.id.zero_two);
      timeBlockLines[1] = (LinearLayout)programTable.findViewById(R.id.two_four);
      timeBlockLines[2] = (LinearLayout)programTable.findViewById(R.id.four_six);
      timeBlockLines[3] = (LinearLayout)programTable.findViewById(R.id.six_eight);
      timeBlockLines[4] = (LinearLayout)programTable.findViewById(R.id.eight_ten);
      timeBlockLines[5] = (LinearLayout)programTable.findViewById(R.id.ten_twelfe);
      timeBlockLines[6] = (LinearLayout)programTable.findViewById(R.id.twefle_fourteen);
      timeBlockLines[7] = (LinearLayout)programTable.findViewById(R.id.fourteen_sixteen);
      timeBlockLines[8] = (LinearLayout)programTable.findViewById(R.id.sixteen_eighteen);
      timeBlockLines[9] = (LinearLayout)programTable.findViewById(R.id.eighteen_twenty);
      timeBlockLines[10] = (LinearLayout)programTable.findViewById(R.id.twenty_twentytwo);
      timeBlockLines[11] = (LinearLayout)programTable.findViewById(R.id.twentytwo_twentyfour);
      timeBlockLines[12] = (LinearLayout)programTable.findViewById(R.id.twentyfour_twentysix);
      timeBlockLines[13] = (LinearLayout)programTable.findViewById(R.id.twentysix_twentyeight);
      
      do {
        String name = channels.getString(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
        
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
          Log.d("info", "padding " + leftPadding + " " + columnWidth);
          
          if(leftPadding > 0) {
            text.setPadding(leftPadding / 2, 0, leftPadding / 2, 0);
          }
        }
        
        channelBar.addView(text);

        channelBar.addView(inflater.inflate(R.layout.separator_line, channelBar, false));
        
        String where2 = where + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channels.getInt(0);
        
        Cursor cursor = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where2, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
        
        if(cursor.getCount() > 0) {
          cursor.moveToFirst();
          
          int count = 1;
          int hour = 0;
          
          for(LinearLayout layoutLine : timeBlockLines) {
            count = createTimeBlockForChannel(hour,hour+2,cursor,layoutLine,inflater,count);
            layoutLine.addView(inflater.inflate(R.layout.separator_line, layoutLine, false));
            
            hour += 2;
          }
        }
        else {          
          for(LinearLayout layoutLine : timeBlockLines) {
            LinearLayout block = (LinearLayout)inflater.inflate(R.layout.program_block, layoutLine, false);
            layoutLine.addView(block);
            layoutLine.addView(inflater.inflate(R.layout.separator_line, layoutLine, false));
          }
        }
        
        cursor.close();
      }while(channels.moveToNext());
    }
    
    channels.close();
    
    if(testDay == mCurrentDay) {
      scrollToNow();
    }
    
    mUpdatingLayout = false;
  }
  
  private void setDayString(TextView currentDay) {
    Date date = new Date(mCurrentDay * 1000 * 60 * 60 * 24);
    
    String longDate = DateFormat.getLongDateFormat(getActivity()).format(date);
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    longDate = longDate.replaceAll("\\s+"+cal.get(Calendar.YEAR), "");
    
    currentDay.setText(UiUtils.LONG_DAY_FORMAT.format(date) + "\n" + longDate);
  }
  
  private void changeDay(int count) {
    mCurrentDay += count;
    
    TextView day = (TextView)getView().findViewById(R.id.show_current_day);
    
    setDayString(day);
    
    RelativeLayout layout = (RelativeLayout)getView().findViewWithTag("LAYOUT");
    
    updateView(getActivity().getLayoutInflater(), layout);
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    RelativeLayout programTableLayout = (RelativeLayout)inflater.inflate(R.layout.program_table_layout, null);
    
    if(mCurrentDay == 0) {
      mCurrentDay = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
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
    
    /*
    LinearLayout layout = new LinearLayout(container.getContext());
    layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
    layout.setTag("LAYOUT");*/
    
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
              Log.d("info", "padding " + leftPadding + " " + columnWidth);
              
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
  
  private int createTimeBlockForChannel(int start, int end, Cursor cursor, LinearLayout parent, LayoutInflater inflater, int count) {
    LinearLayout block = (LinearLayout)inflater.inflate(R.layout.program_block, parent, false);
    
    parent.addView(block);

    if(!cursor.isAfterLast()) {
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
      
      if(start >= 24) {
        start -= 24;
        end -= 24;
      }
      
      while(cal.get(Calendar.HOUR_OF_DAY) >= start && cal.get(Calendar.HOUR_OF_DAY) < end) {
        View panel = inflater.inflate(R.layout.program_panel, block, false);
        registerForContextMenu(panel);
        panel.setOnClickListener(mClickListener);
        panel.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
                
        count++;
        
        UiUtils.handleMarkings(getActivity(), cursor, panel, null);
        
        TextView startTime = (TextView)panel.findViewById(R.id.prog_panel_start_time);
        TextView title = (TextView)panel.findViewById(R.id.prog_panel_title);
        TextView genre = (TextView)panel.findViewById(R.id.prog_panel_genre);
        TextView episode = (TextView)panel.findViewById(R.id.prog_panel_episode);
        
        long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
        
        boolean expired = false;
        
        if(endTime <= System.currentTimeMillis()) {
          panel.setTag(R.id.expired_tag, true);
          startTime.setTextColor(Color.rgb(190, 190, 190));
          title.setTextColor(Color.rgb(190, 190, 190));
          episode.setTextColor(Color.rgb(190, 190, 190));
          genre.setTextColor(Color.rgb(190, 190, 190));
          expired = true;
        }
        else if(System.currentTimeMillis() <= endTime) {
          int[] attrs = new int[] { android.R.attr.textColorSecondary };
          TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
          int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
          a.recycle();
          
          startTime.setTextColor(DEFAULT_TEXT_COLOR);
          title.setTextColor(DEFAULT_TEXT_COLOR);
          episode.setTextColor(DEFAULT_TEXT_COLOR);
          genre.setTextColor(DEFAULT_TEXT_COLOR);
        }
        
        startTime.setText(DateFormat.getTimeFormat(getActivity()).format(cal.getTime()));
        title.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
                
        if(!cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
          episode.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)));
          episode.setVisibility(TextView.VISIBLE);
        }
        else {
          episode.setVisibility(TextView.GONE);
        }
        
        if(!cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
          genre.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)));
          genre.setVisibility(TextView.VISIBLE);
        }
        else {
          genre.setVisibility(TextView.GONE);
        }
        
        int pictureColumn = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);
        
        if(pictureColumn != -1 && !cursor.isNull(pictureColumn)) {
          byte[] logoData = cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE));
          Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
                    
          BitmapDrawable l = new BitmapDrawable(getResources(), logo);
          
          if(expired) {
            l.setColorFilter(getActivity().getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
          }
          
          l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
          
          genre.setCompoundDrawables(null, l, null, null);
          genre.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)) + (genre.getText() != null && genre.getText().toString().trim().length() > 0 ? "\n" + genre.getText() : ""));
          genre.setVisibility(View.VISIBLE);
        }
        
        block.addView(panel);
        
        
        if(!cursor.moveToNext()) {
          break;
        }
        
        cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
      }
    }
    
    return count;
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
        
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(mCurrentDay * 24 * 60 * 60 * 1000);
    
    if (Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1) {
      select.getCalendarView().setFirstDayOfWeek(Calendar.MONDAY);
    }
    
    select.setMinDate(dayStart - 24 * 60 * 60 * 1000);
    select.setMaxDate(dayStart + 15 * (24 * 60 * 60 * 1000));
    select.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Calendar cal = Calendar.getInstance();
        cal.set(select.getYear(), select.getMonth(), select.getDayOfMonth());
        
        mCurrentDay = cal.getTimeInMillis() / 24 / 60 / 60 / 1000;

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
    
    builder.show();}catch(Throwable t) {Log.d("dateselect", "", t);}
  }
}
