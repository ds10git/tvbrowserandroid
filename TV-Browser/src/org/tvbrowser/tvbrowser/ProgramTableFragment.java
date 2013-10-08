package org.tvbrowser.tvbrowser;

import java.util.Calendar;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ProgramTableFragment extends Fragment {
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private View.OnClickListener mClickListener;
  
  private long mCurrentDay;
  private View mMenuView;
  
  private BroadcastReceiver mDataUpdateReceiver;
  
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
      }while((System.currentTimeMillis() - c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))) > (3 * 60 * 60000) && c.moveToNext());
      
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
  
    BroadcastReceiver receiver = new BroadcastReceiver() {
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
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    
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
    
    createUpdateThread();
    
    mUpdateThread.start();
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
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(!isDetached() && getView() != null) {
              LinearLayout layout = (LinearLayout)getView().findViewWithTag("LAYOUT");
              
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
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    mKeepRunning = false;
    
    if(mDataUpdateReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
    }
  }
  
  private void createUpdateThread() {
    mUpdateThread = new Thread() {
      public void run() {
        while(mKeepRunning) {
          try {
            if(mKeepRunning && TvBrowserContentProvider.INFORM_FOR_CHANGES) {
              /*handler.post(new Runnable() {
                @Override
                public void run() {*/
                  if(!isDetached()) {
                  /*  if(mRunningPrograms.isEmpty()) {
                        handler.post(new Runnable() {
                          
                          @Override
                          public void run() {
                            if(getView().findViewWithTag("LAYOUT") != null) {
                              LinearLayout view = (LinearLayout)getView().findViewWithTag("LAYOUT");

                            // TODO Auto-generated method stub
                            updateView(getActivity().getLayoutInflater(), view);
                            }
                          }
                        });
                        

                    }
                    else {
                      for(int i = mRunningPrograms.size()-1; i >= 0; i--) {
                        View view = mRunningPrograms.get(i);
                        Log.d("test", " running " + String.valueOf(view));
                        if(view != null) {
                          Long tag = (Long)view.getTag();
                          Log.d("test", " TAG " + String.valueOf(tag));
                          Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, tag), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_GENRE}, null, null, null);
                          
                          if(c.getCount() > 0) {
                            c.moveToFirst();
                            long endTime = c.getLong(1);
                            long startTime = c.getLong(0);
                          
                            if(endTime <= System.currentTimeMillis()) {
                              final View internal = view; 
                              
                              handler.post(new Runnable() {
                                @Override
                                public void run() {
                                  // TODO Auto-generated method stub
                                  Log.d("test", " OLD PROG " + String.valueOf(((TextView)internal.findViewById(R.id.prog_panel_title)).getText()));
                                  ((TextView)internal.findViewById(R.id.prog_panel_start_time)).setTextColor(Color.rgb(190, 190, 190));
                                  ((TextView)internal.findViewById(R.id.prog_panel_title)).setTextColor(Color.rgb(190, 190, 190));
                                  ((TextView)internal.findViewById(R.id.prog_panel_episode)).setTextColor(Color.rgb(190, 190, 190));  
                                  ((TextView)internal.findViewById(R.id.prog_panel_genre)).setTextColor(Color.rgb(190, 190, 190));
                                }
                              });
                              mRunningPrograms.remove(i);
                              
                              String where = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + c.getInt(3) + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " >= " + endTime;
                              
                              Cursor next = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID},where , null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
                              
                              if(next.getCount() > 0) {
                                next.moveToFirst();
                                Log.d("test"," ID " + next.getLong(0));
                                view = getView().findViewWithTag(next.getLong(0));
                                
                                if(view != null) {
                                  Log.d("test"," view " + String.valueOf(((TextView)view.findViewById(R.id.prog_panel_title)).getText()));
                                  boolean add = true;
                                  
                                  while((endTime = next.getLong(2)) <= System.currentTimeMillis()) {
                                    final View internal1 = view;
                                    
                                    handler.post(new Runnable() {
                                      @Override
                                      public void run() {
                                        if(internal1 != null) {
                                          ((TextView)internal1.findViewById(R.id.prog_panel_start_time)).setTextColor(Color.rgb(190, 190, 190));
                                          ((TextView)internal1.findViewById(R.id.prog_panel_title)).setTextColor(Color.rgb(190, 190, 190));
                                          ((TextView)internal1.findViewById(R.id.prog_panel_episode)).setTextColor(Color.rgb(190, 190, 190));
                                          ((TextView)internal1.findViewById(R.id.prog_panel_genre)).setTextColor(Color.rgb(190, 190, 190));
                                        }
                                      }
                                    });
                                    
                                    if(!next.moveToNext()) {
                                      add = false;
                                      break;
                                    }
                                    else {
                                      view = getView().findViewWithTag(next.getLong(0));
                                    }
                                  }
                                  
                                  if(add) {
                                    startTime = next.getLong(1);
                                    
                                    mRunningPrograms.add(view);
                                  }
                                }
                              }
                              
                              next.close();
                              //mRunningPrograms.add(panel);
                            }
                            
                            if(view != null && System.currentTimeMillis() >= startTime && System.currentTimeMillis() <= endTime) {
                              final View internal1 = view;
                              
                              handler.post(new Runnable() {
                                @Override
                                public void run() {
                                  ((TextView)internal1.findViewById(R.id.prog_panel_start_time)).setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                  ((TextView)internal1.findViewById(R.id.prog_panel_title)).setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                  ((TextView)internal1.findViewById(R.id.prog_panel_episode)).setTextColor(getActivity().getResources().getColor(R.color.running_color));                              
                                  ((TextView)internal1.findViewById(R.id.prog_panel_genre)).setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                }
                              });
                            }
                          }
                          c.close();
                        }
                        else {
                          mRunningPrograms.remove(i);
                        }
                      }
                    }
                    */
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
                                          progPanel.setTag(R.id.expired_tag, true);
                                         // progPanel.setTag(R.id.on_air_tag, false);
                                          
                                          if(!isDetached()) {
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
                                    else if(/*!onAir && */System.currentTimeMillis() >= startTime && System.currentTimeMillis() <= endTime) {
                                     /* handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                          text.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                          title.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                          episode.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                          genre.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                          progPanel.setTag(R.id.on_air_tag, true);
                                          
                                          Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                                          
                                          if(c.getCount() > 0) {
                                            c.moveToFirst();
                                            UiUtils.handleMarkings(getActivity(), c, progPanel, null);
                                          }
                                          
                                          c.close();
                                        }
                                      });*/
                                      
                                      handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                          Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                                          
                                          if(c.getCount() > 0) {
                                            c.moveToFirst();
                                            UiUtils.handleMarkings(getActivity(), c, progPanel, null);
                                          }
                                          
                                          c.close();
                                        }
                                      });
                                    }
                                   /* else if(onAir) {
                                      handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                          Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)progPanel.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                                          
                                          if(c.getCount() > 0) {
                                            c.moveToFirst();
                                            UiUtils.handleMarkings(getActivity(), c, progPanel, null);
                                          }
                                          
                                          c.close();
                                        }
                                      });
                                    }*/
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
            sleep(60000);
          } catch (InterruptedException e) {
          }
        }
      }
    };
    mUpdateThread.setPriority(Thread.MIN_PRIORITY);
  }
    
  public void updateView(LayoutInflater inflater, ViewGroup container) {
    container.removeAllViews();
    View programTable = inflater.inflate(R.layout.program_table_layout, container);
    
    Calendar cal = Calendar.getInstance();
    cal.set(2013, Calendar.DECEMBER, 31);
    
    cal.add(Calendar.DAY_OF_YEAR, 1);
    
    long day = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
    long dayStart = day * 24 * 60 * 60 * 1000;
    dayStart -=  TimeZone.getDefault().getOffset(dayStart);
    
    if(System.currentTimeMillis() - dayStart < 4 * 60 * 60 * 1000) {
      dayStart = --day * 24 * 60 * 60 * 1000 - TimeZone.getDefault().getOffset(dayStart);
    }
    
    mCurrentDay = day;
    
    long dayEnd = (day+1) * 24 * 60 * 60 * 1000 + 4 * 60 * 60 * 1000 - TimeZone.getDefault().getOffset(dayStart);
        
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME +  " >= " + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + dayEnd;
    
    StringBuilder where3 = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where3.append(" = 1");
    
    Cursor channels = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, where3.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
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
        String name = channels.getString(1);
        
        TextView text = (TextView)inflater.inflate(R.layout.channel_label, channelBar,false);
        text.setText(name);
        channelBar.addView(text);

        channelBar.addView(inflater.inflate(R.layout.separator_line, channelBar, false));
        
        String where2 = where + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channels.getInt(0);
        
        Cursor cursor = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, where2, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
        
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
  }
  
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    LinearLayout layout = new LinearLayout(container.getContext());
    layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
    layout.setTag("LAYOUT");
    
    updateView(inflater,layout);
    
    return layout;
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
        panel.setTag(cursor.getLong(0));
                
        count++;
        
        UiUtils.handleMarkings(getActivity(), cursor, panel, null);
        
        TextView startTime = (TextView)panel.findViewById(R.id.prog_panel_start_time);
        TextView title = (TextView)panel.findViewById(R.id.prog_panel_title);
        TextView genre = (TextView)panel.findViewById(R.id.prog_panel_genre);
        TextView episode = (TextView)panel.findViewById(R.id.prog_panel_episode);
        
        long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
        
        if(endTime <= System.currentTimeMillis()) {
          panel.setTag(R.id.expired_tag, true);
          startTime.setTextColor(Color.rgb(190, 190, 190));
          title.setTextColor(Color.rgb(190, 190, 190));
          episode.setTextColor(Color.rgb(190, 190, 190));
          genre.setTextColor(Color.rgb(190, 190, 190));
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
        
        block.addView(panel);
        
        
        if(!cursor.moveToNext()) {
          break;
        }
        
        cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
      }
    }
    
    return count;
  }
  
}
