package org.tvbrowser.tvbrowser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ProgramTableFragment extends Fragment {
  
  private ArrayList<View> mRunningPrograms;
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private View.OnClickListener mClickListener;
 // int horizontalID;
  private long mCurrentDay;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    mRunningPrograms = new ArrayList<View>();
    mClickListener = new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        Long id = (Long)v.getTag();
        
        if(id != null) {
        Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
        
        c.moveToFirst();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        TableLayout table = new TableLayout(builder.getContext());
        table.setShrinkAllColumns(true);
        
        TableRow row = new TableRow(table.getContext());
        TableRow row0 = new TableRow(table.getContext());
        
        TextView date = new TextView(row.getContext());
        date.setTextColor(Color.rgb(200, 0, 0));
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        
        Date start = new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
        SimpleDateFormat day = new SimpleDateFormat("EEE",Locale.getDefault());
        
        long channelID = c.getLong(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
        
        Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        
        channel.moveToFirst();
        
        date.setText(day.format(start) + " " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(start) + " - " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)))) + " " + channel.getString(0));
        
        channel.close();
        
        row0.addView(date);
        
        TextView title = new TextView(row.getContext());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(null, Typeface.BOLD);
        title.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
        
        row.addView(title);
        
        table.addView(row0);
        table.addView(row);
        
        if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
          TextView genre = new TextView(table.getContext());
          genre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
          genre.setTypeface(null, Typeface.ITALIC);
          genre.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)));
          
          TableRow rowGenre = new TableRow(table.getContext());
          
          rowGenre.addView(genre);
          table.addView(rowGenre);
        }
        
        if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
          TextView episode = new TextView(table.getContext());
          episode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
          episode.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)));
          episode.setTextColor(Color.GRAY);
          
          TableRow rowEpisode = new TableRow(table.getContext());
          
          rowEpisode.addView(episode);
          table.addView(rowEpisode);
        }
        
        if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
          TextView desc = new TextView(table.getContext());
          desc.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)));
         /* desc.setSingleLine(false);*/
          desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
          /*desc.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);*/
          
          TableRow rowDescription = new TableRow(table.getContext());
          
          rowDescription.addView(desc);
          table.addView(rowDescription);
        }
        

        
        
        

        
        c.close();
            
        builder.setView(table);
        builder.show();
        
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
  public void onDetach() {
    super.onDetach();
    
    mKeepRunning = false;
  }
  
  private void createUpdateThread() {
    mUpdateThread = new Thread() {
      public void run() {
        while(mKeepRunning) {
          try {
            if(mKeepRunning && TvBrowserContentProvider.INFORM_FOR_CHANGES) {
              Log.d("test", String.valueOf(getView().findViewWithTag("LAYOUT")));
              /*handler.post(new Runnable() {
                @Override
                public void run() {*/
                  if(!isDetached()) {
                    if(mRunningPrograms.isEmpty()) {
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
                    
              /*      LinearLayout main = (LinearLayout)getView().findViewWithTag("MAIN_LAYOUT");
                    Log.d("test", " XX " + String.valueOf(main));
                    for(int k = 0; k < main.getChildCount(); k++) {
                      View mainChild = main.getChildAt(k);
                      
                      if(mainChild instanceof LinearLayout) {
                        LinearLayout layout = (LinearLayout)mainChild;
                        Log.d("test", " LL " + String.valueOf(layout));
                        for(int i = 0; i < layout.getChildCount(); i++) {
                          View child = layout.getChildAt(i);
                          
                          if(child instanceof LinearLayout) {
                            LinearLayout block = (LinearLayout)child;
                            Log.d("test", " BB " + String.valueOf(block));
                            for(int j = 0; j < block.getChildCount(); j++) {
                              View blockChild = block.getChildAt(j);
                              
                              if(blockChild instanceof LinearLayout) {
                                final LinearLayout progPanel = (LinearLayout)blockChild;
                                
                                final TextView title = (TextView)progPanel.findViewById(R.id.prog_panel_title);
    Log.d("test", " CC " + String.valueOf(title));
                                if(title != null) {
                                  handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                      
                                  Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, (Long)title.getTag()), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
                                  
                                  if(c.getCount() > 0) {
                                    c.moveToFirst();
                                    
                                    String value = c.getString(2);
                                    
                                    if(value != null && value.trim().length() > 0) {
                                      progPanel.setBackgroundColor(getActivity().getResources().getColor(R.color.mark_color));
                                    }
                                    else {
                                      progPanel.setBackgroundResource(android.R.drawable.list_selector_background);
                                    }
                                    
                                    long startTime = c.getLong(0);
                                    long endTime = c.getLong(1);
                                    
                                    TextView text = (TextView)progPanel.findViewById(R.id.prog_panel_start_time);
                                    TextView episode = (TextView)progPanel.findViewById(R.id.prog_panel_episode);
                                    
                                    if(endTime <= System.currentTimeMillis()) {
                                      text.setTextColor(Color.rgb(190, 190, 190));
                                      title.setTextColor(Color.rgb(190, 190, 190));
                                      episode.setTextColor(Color.rgb(190, 190, 190));
                                    }
                                    else if(System.currentTimeMillis() >= startTime && System.currentTimeMillis() <= endTime) {
                                      text.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                      title.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                      episode.setTextColor(getActivity().getResources().getColor(R.color.running_color));
                                    }
                                    else {
                                      int[] attrs = new int[] { android.R.attr.textColorSecondary };
                                      TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
                                      int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
                                      a.recycle();
                                      
                                      text.setTextColor(DEFAULT_TEXT_COLOR);
                                      title.setTextColor(DEFAULT_TEXT_COLOR);
                                      episode.setTextColor(DEFAULT_TEXT_COLOR);
                                    }                                
                                    
                                    //Log.d("test", "xxx "+ String.valueOf(text.getTag()) + String.valueOf(text.getText()));
                                  }
                                    c.close();
                                  
                                    }
                                  });
                                  
                                  
                                }                            
                              }
                            }
                            
                          }
                          
                          
                        }
                      }
                    }*/

                    //Log.d("test", "xxx "+ String.valueOf(((LinearLayout)getView().findViewWithTag("zwero_two")).findViewById(R.layout.program_block)));
                //    updateView(getActivity().getLayoutInflater(), (ViewGroup)getView());*/
                  }
                }
            //  });
           // }
            sleep(60000);
          } catch (InterruptedException e) {
          }
        }
      }
    };
    mUpdateThread.setPriority(Thread.MIN_PRIORITY);
  }
    
  public void updateView(LayoutInflater inflater, ViewGroup container) {
    Log.d("neu", "hier");
    container.removeAllViews();
    View programTable = inflater.inflate(R.layout.program_table_layout, container);
    
    Calendar cal = Calendar.getInstance();
    cal.set(2013, Calendar.DECEMBER, 31);
    Log.d("neu", String.valueOf(cal.getTimeInMillis()));
    
    cal.add(Calendar.DAY_OF_YEAR, 1);
    Log.d("neu", " x " + String.valueOf(cal.get(Calendar.YEAR)));
    
    
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
      
      LinearLayout zero_two = (LinearLayout)programTable.findViewById(R.id.zero_two);
      LinearLayout two_four = (LinearLayout)programTable.findViewById(R.id.two_four);
      LinearLayout four_six = (LinearLayout)programTable.findViewById(R.id.four_six);
      LinearLayout six_eight = (LinearLayout)programTable.findViewById(R.id.six_eight);
      LinearLayout eight_ten = (LinearLayout)programTable.findViewById(R.id.eight_ten);
      LinearLayout ten_twelfe = (LinearLayout)programTable.findViewById(R.id.ten_twelfe);
      LinearLayout twefle_fourteen = (LinearLayout)programTable.findViewById(R.id.twefle_fourteen);
      LinearLayout fourteen_sixteen = (LinearLayout)programTable.findViewById(R.id.fourteen_sixteen);
      LinearLayout sixteen_eighteen = (LinearLayout)programTable.findViewById(R.id.sixteen_eighteen);
      LinearLayout eighteen_twenty = (LinearLayout)programTable.findViewById(R.id.eighteen_twenty);
      LinearLayout twenty_twentytwo = (LinearLayout)programTable.findViewById(R.id.twenty_twentytwo);
      LinearLayout twentytwo_twentyfour = (LinearLayout)programTable.findViewById(R.id.twentytwo_twentyfour);
      LinearLayout twentyfour_twentysix = (LinearLayout)programTable.findViewById(R.id.twentyfour_twentysix);
      LinearLayout twentysix_twentyeight = (LinearLayout)programTable.findViewById(R.id.twentysix_twentyeight);
      
      do {
        String name = channels.getString(1);
        
        TextView text = (TextView)inflater.inflate(R.layout.channel_label, channelBar,false);
        text.setText(name);
        channelBar.addView(text);

        channelBar.addView(inflater.inflate(R.layout.separator_line, channelBar, false));
        
        String where2 = where + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + channels.getInt(0);
        
        Cursor cursor = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, where2, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
        Log.d("test", where2 + " " + cursor.getCount());
        
        if(cursor.getCount() > 0) {
          cursor.moveToFirst();
          
          int count = 1;
          
          count = createTimeBlockForChannel(0,2,cursor,zero_two,inflater,count);
          zero_two.addView(inflater.inflate(R.layout.separator_line, zero_two, false));
          
          count = createTimeBlockForChannel(2,4,cursor,two_four,inflater,count);
          two_four.addView(inflater.inflate(R.layout.separator_line, two_four, false));
          
          count = createTimeBlockForChannel(4,6,cursor,four_six,inflater,count);
          four_six.addView(inflater.inflate(R.layout.separator_line, four_six, false));
          
          count = createTimeBlockForChannel(6,8,cursor,six_eight,inflater,count);
          six_eight.addView(inflater.inflate(R.layout.separator_line, six_eight, false));
          
          count = createTimeBlockForChannel(8,10,cursor,eight_ten,inflater,count);
          eight_ten.addView(inflater.inflate(R.layout.separator_line, eight_ten, false));
          
          count = createTimeBlockForChannel(10,12,cursor,ten_twelfe,inflater,count);
          ten_twelfe.addView(inflater.inflate(R.layout.separator_line, ten_twelfe, false));
          
          count = createTimeBlockForChannel(12,14,cursor,twefle_fourteen,inflater,count);
          twefle_fourteen.addView(inflater.inflate(R.layout.separator_line, twefle_fourteen, false));
          
          count = createTimeBlockForChannel(14,16,cursor,fourteen_sixteen,inflater,count);
          fourteen_sixteen.addView(inflater.inflate(R.layout.separator_line, fourteen_sixteen, false));
          
          count = createTimeBlockForChannel(16,18,cursor,sixteen_eighteen,inflater,count);
          sixteen_eighteen.addView(inflater.inflate(R.layout.separator_line, sixteen_eighteen, false));
          
          count = createTimeBlockForChannel(18,20,cursor,eighteen_twenty,inflater,count);
          eighteen_twenty.addView(inflater.inflate(R.layout.separator_line, eighteen_twenty, false));
          
          count = createTimeBlockForChannel(20,22,cursor,twenty_twentytwo,inflater,count);
          twenty_twentytwo.addView(inflater.inflate(R.layout.separator_line, twenty_twentytwo, false));
          
          count = createTimeBlockForChannel(22,24,cursor,twentytwo_twentyfour,inflater,count);
          twentytwo_twentyfour.addView(inflater.inflate(R.layout.separator_line, twentytwo_twentyfour, false));
          
          count = createTimeBlockForChannel(24,26,cursor,twentyfour_twentysix,inflater,count);
          twentyfour_twentysix.addView(inflater.inflate(R.layout.separator_line, twentyfour_twentysix, false));
          
          count = createTimeBlockForChannel(26,28,cursor,twentysix_twentyeight,inflater,count);
          twentysix_twentyeight.addView(inflater.inflate(R.layout.separator_line, twentysix_twentyeight, false));
        }
        
        cursor.close();
      }while(channels.moveToNext());
    }
    
    channels.close();
  }
  
  
 // @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    LinearLayout layout = new LinearLayout(container.getContext());
    layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
    layout.setTag("LAYOUT");
    //horizontalID = horizontal.getId();
    
  //  updateView(inflater,horizontal);
    updateView(inflater,layout);
    
    return layout;
  }
  
  private View mMenuView;
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    getActivity().getMenuInflater().inflate(R.menu.prog_table_context, menu);
    mMenuView = v;
  }
  
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    Log.d("test", "progcontext");
    if(mMenuView != null) {
      //View test = mMenuView.findViewById(R.id.prog_panel_title);
      View temp = mMenuView;
      mMenuView = null;
    
      long programID = ((Long)temp.getTag());
    
    Cursor info = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
    
    String current = null;
    
    if(info.getCount() > 0) {
      info.moveToFirst();
      
      if(!info.isNull(0)) {
        current = info.getString(0);
      }
    }
    
    info.close();
    
    ContentValues values = new ContentValues();
    
    if(item.getItemId() == R.id.prog_mark_item) {
      if(current != null && current.contains("marked")) {
        return true;
      }
      else if(current == null) {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "marked");
      }
      else {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";marked");
      }
      
      Log.d("TVB","MARK " + programID);
      if(current != null && current.contains("calendar")) {
        temp.setBackgroundColor(getActivity().getResources().getColor(R.color.mark_color_calendar));
      }
      else {
        temp.setBackgroundColor(getActivity().getResources().getColor(R.color.mark_color));
      }      
    }
    else if(item.getItemId() == R.id.prog_unmark_item){
      if(current == null || current.trim().length() == 0) {
        return true;
      }
      /*
      if(current.contains(";marked")) {
        current = current.replace(";marked", "");
      }
      else if(current.contains("marked;")) {
        current = current.replace("marked;", "");
      }
      else if(current.contains("marked")) {
        current = current.replace("marked", "");
      }
      */
      
      current = "";
      
      Log.d("TVB", String.valueOf(current));
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
      temp.setBackgroundResource(android.R.drawable.list_selector_background);
      Log.d("TVB","UNMARK " + programID);
    }
    else if(item.getItemId() == R.id.prog_create_calendar_entry) {
      info = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        Log.d("TVB", "channel cursor " + channel.getCount());
        if(channel.getCount() > 0) {
          channel.moveToFirst();
       // Create a new insertion Intent.
          Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT/*, CalendarContract.Events.CONTENT_URI*/);
          Log.d("TVB", getActivity().getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
          
          //Intent intent = new Intent(Intent.ACTION_INSERT);
          addCalendarEntry.setType(getActivity().getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
               
          //addCalendarEntry.putExtra(Events.STATUS, 1);
          //addCalendarEntry.putExtra(Events.VISIBLE, 0);
          //addCalendarEntry.putExtra(Events.HAS_ALARM, 1);
          
          String desc = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
            desc = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
            
            if(desc != null && desc.trim().toLowerCase().equals("null")) {
              desc = null;
            }
          }
          
          String episode = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
            episode = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
            
            if(episode != null && episode.trim().toLowerCase().equals("null")) {
              episode = null;
            }
          }
          addCalendarEntry.putExtra(Events.EVENT_LOCATION, channel.getString(0));
          // Add the calendar event details
          addCalendarEntry.putExtra(Events.TITLE, info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
          
          String description = null;
          
          if(episode != null) {
            description = episode;
          }
          
          if(desc != null) {
            if(description != null) {
              description += "\n\n" + desc;
            }
            else {
              description = desc;
            }
          }
          
          if(description != null) {
            addCalendarEntry.putExtra(Events.DESCRIPTION, description);
          }
          
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)));
          
          // Use the Calendar app to add the new event.
          startActivity(addCalendarEntry);
          
          if(current != null && current.contains("calendar")) {
            return true;
          }
          else if(current == null) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "calendar");
          }
          else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";calendar");
          }
                    
          temp.setBackgroundColor(getActivity().getResources().getColor(R.color.mark_color_calendar)); 
        }
        
        channel.close();
      }
      
      info.close();
    }
    
    if(values.size() > 0) {
      /*if(marked) {
        Log.d("TVB", String.valueOf(((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView));
        ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView.setBackgroundResource(R.color.mark_color);//.invalidate();
      }
      else {
        ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView.setBackgroundResource(android.R.drawable.list_selector_background);//.invalidate();
      }*/
      temp.invalidate();
      
     // item.get v.invalidate();
      getActivity().getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
    }
    return true;
    }
    
    return false;
  }
  
  private int createTimeBlockForChannel(int start, int end, Cursor cursor, LinearLayout parent, LayoutInflater inflater, int count) {
    
   // Log.d("test", "hour " + cal.get(Calendar.HOUR_OF_DAY) + " " + new Date(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))));

    LinearLayout block = (LinearLayout)inflater.inflate(R.layout.program_block, parent, false);
    /*block.setLayoutParams(new LayoutParams(300, LayoutParams.WRAP_CONTENT));
    block.setOrientation(LinearLayout.VERTICAL);
    block.setPadding(0,0, 5, 0);*/
    
    parent.addView(block);

    if(!cursor.isAfterLast()) {
    
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));

    
    
  //  boolean retValue = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) == channelID;
    
    if(start >= 24) {
      
      start -= 24;
      end -= 24;
      
      Log.d("test", " start " + start);
    }
    
    while(cal.get(Calendar.HOUR_OF_DAY) >= start && cal.get(Calendar.HOUR_OF_DAY) < end) {
      Log.d("info",String.valueOf(System.currentTimeMillis()));
      View panel = inflater.inflate(R.layout.program_panel, block, false);
      registerForContextMenu(panel);
      panel.setOnClickListener(mClickListener);
      panel.setTag(cursor.getLong(0));
      
      if(count == 1 && panel != null) {
        mRunningPrograms.add(panel);
      }
      
      count++;
      
      String value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
      
      if(value != null && value.trim().length() > 0) {
        if(value.contains("calendar")) {
          panel.setBackgroundColor(getActivity().getResources().getColor(R.color.mark_color_calendar));
        }
        else {
          panel.setBackgroundColor(getActivity().getResources().getColor(R.color.mark_color));
        }
      }
      else {
        panel.setBackgroundResource(android.R.drawable.list_selector_background);
      }
      
      TextView startTime = (TextView)panel.findViewById(R.id.prog_panel_start_time);
      TextView title = (TextView)panel.findViewById(R.id.prog_panel_title);
      TextView genre = (TextView)panel.findViewById(R.id.prog_panel_genre);
      TextView episode = (TextView)panel.findViewById(R.id.prog_panel_episode);
      
      long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
      
      if(endTime <= System.currentTimeMillis()) {
        startTime.setTextColor(Color.rgb(190, 190, 190));
        title.setTextColor(Color.rgb(190, 190, 190));
        episode.setTextColor(Color.rgb(190, 190, 190));
        genre.setTextColor(Color.rgb(190, 190, 190));
      }
      else if(System.currentTimeMillis() >= cal.getTimeInMillis() && System.currentTimeMillis() <= endTime) {
      //  mRunningPrograms.add(panel);
        startTime.setTextColor(getActivity().getResources().getColor(R.color.running_color));
        title.setTextColor(getActivity().getResources().getColor(R.color.running_color));
        episode.setTextColor(getActivity().getResources().getColor(R.color.running_color));
        genre.setTextColor(getActivity().getResources().getColor(R.color.running_color));
      }
      else {
        int[] attrs = new int[] { android.R.attr.textColorSecondary };
        TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
        int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
        a.recycle();
        
        startTime.setTextColor(DEFAULT_TEXT_COLOR);
        title.setTextColor(DEFAULT_TEXT_COLOR);
        episode.setTextColor(DEFAULT_TEXT_COLOR);
        genre.setTextColor(DEFAULT_TEXT_COLOR);
      }
      
      startTime.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(cal.getTime()));
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
       // retValue = false;
        break;
      }
      
     // retValue = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) == channelID;
      cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
    }
    }
    
    return count;
  }
  
}
