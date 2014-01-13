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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class DummySectionFragment extends Fragment {
  /**
   * The fragment argument representing the section number for this fragment.
   */
  public static final String ARG_SECTION_NUMBER = "section_number";

  private BroadcastReceiver mChannelUpdateReceiver;
  
  public DummySectionFragment() {
  }
  
  public void updateChannels() {
    if(mChannelUpdateReceiver != null) {
      mChannelUpdateReceiver.onReceive(null, null);
    }
  }
  
  private static final class DateSelection {
    private long mTime;
    private Context mContext;
    
    public DateSelection(long time, Context context) {
      mTime = time;
      mContext = context;
    }
    
    @Override
    public String toString() {
      if(mTime >= 0) {
        return UiUtils.formatDate(mTime, mContext, false, true).toString();
      }
      
      return mContext.getResources().getString(R.string.all_data);
    }
    
    public long getTime() {
      return mTime;
    }
  }
  
  private static final class ChannelSelection {
    private int mID;
    private String mOrderNumber;
    private String mName;
    private Drawable mLogo;
    
    public ChannelSelection(int ID, String orderNumber, String name, Drawable logo) {
      mID = ID;
      mOrderNumber = orderNumber;
      mName = name;
      mLogo = logo;
    }
    
    public int getID() {
      return mID;
    }
    
    public String getOrderNumber() {
      return mOrderNumber;
    }
    
    public String getName() {
      return mName;
    }
    
    public Drawable getLogo() {
      return mLogo;
    }
    
    @Override
    public String toString() {
      return mName;
    }
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = null;
    
    if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
      rootView = inflater.inflate(R.layout.running_program_fragment,
          container, false);
      
      final RunningProgramsListFragment running = (RunningProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.runningListFragment);
      final LinearLayout timeBar = (LinearLayout)rootView.findViewById(R.id.runnning_time_bar);
      
      final Button before = (Button)rootView.findViewById(R.id.button_before1);
      final Button after = (Button)rootView.findViewById(R.id.button_after1);
            
      final Button now = (Button)rootView.findViewById(R.id.now_button);
      final Spinner date = (Spinner)rootView.findViewById(R.id.running_date_selection);
      now.setTag(Integer.valueOf(-1));
      
      final View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
          
          if(running != null) {
            if(pref.getString(getResources().getString(R.string.RUNNING_PROGRAMS_LAYOUT), SettingConstants.DEFAULT_RUNNING_PROGRAMS_LIST_LAYOUT).equals("0")) {
              timeBar.removeView(before);
              timeBar.removeView(after);
              
              int index = timeBar.indexOfChild(v);
              
              timeBar.addView(after, index+1);
              
              before.setBackgroundResource(android.R.drawable.list_selector_background);
              
              if(!v.equals(now)) {
                timeBar.addView(before, index);
              }
            }
            
            if(v.equals(now) && date.getCount() > 0) {
              date.setSelection(0);
            }
            
            running.setWhereClauseTime(v.getTag());
          }
        }
      };
      final View.OnClickListener timeRange = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if(running != null) {
            running.setTimeRangeID(v.getId());
          }
        }
      };
      
      LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
      
      IntentFilter timeButtonsUpdateFilter = new IntentFilter(SettingConstants.UPDATE_TIME_BUTTONS);
      
      final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          for(int i = timeBar.getChildCount() - 1; i >= 0; i--) {
            Button button = (Button)timeBar.getChildAt(i);
            
            if(button != null) {
              button.setOnClickListener(null);
              timeBar.removeViewAt(i);
            }
          }
          
          if(getActivity() != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            now.setOnClickListener(listener);
            
            if(pref.getString(getResources().getString(R.string.RUNNING_PROGRAMS_LAYOUT), SettingConstants.DEFAULT_RUNNING_PROGRAMS_LIST_LAYOUT).equals("0")) {
              before.setOnClickListener(timeRange);
              after.setOnClickListener(timeRange);
              
              timeBar.addView(now);
              timeBar.addView(after);
            }
            else {
              timeBar.addView(now);
            }
            
            ArrayList<Integer> values = new ArrayList<Integer>();
            
            int[] defaultValues = getResources().getIntArray(R.array.time_button_defaults);
            
            for(int i = 1; i <= 6; i++) {
              try {
                Class<?> string = R.string.class;
                
                Field setting = string.getDeclaredField("TIME_BUTTON_" + i);
                
                Integer value = Integer.valueOf(pref.getInt(getResources().getString((Integer)setting.get(string)), defaultValues[i-1]));
                
                if(value >= -1 && !values.contains(value)) {
                  values.add(value);
                }
              } catch (Exception e) {}
            }
            
            if(pref.getBoolean(getString(R.string.SORT_RUNNING_TIMES), false)) {
              Collections.sort(values);
            }
            
            for(Integer value : values) {
              getActivity().getLayoutInflater().inflate(R.layout.time_button, timeBar);
              
              Calendar cal = Calendar.getInstance();
              cal.set(Calendar.HOUR_OF_DAY, value / 60);
              cal.set(Calendar.MINUTE, value % 60);
              
              Button time = (Button)timeBar.getChildAt(timeBar.getChildCount()-1);
              time.setText(DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(cal.getTime()));
              time.setTag(value);
              time.setOnClickListener(listener);
            }
          }
        }
      };
      
      localBroadcastManager.registerReceiver(receiver, timeButtonsUpdateFilter);
      receiver.onReceive(null, null);
      
      
      
      ArrayList<DateSelection> dateEntries = new ArrayList<DummySectionFragment.DateSelection>();
      
      final ArrayAdapter<DateSelection> dateAdapter = new ArrayAdapter<DummySectionFragment.DateSelection>(getActivity(), android.R.layout.simple_spinner_item, dateEntries);
      dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      date.setAdapter(dateAdapter);

      date.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, 
            int pos, long id) {
          DateSelection selection = dateAdapter.getItem(pos);
          
          running.setDay(selection.getTime());
        }
        
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
          running.setDay(-1);
        }
      });
      
      BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if(getActivity() != null && !isDetached()) {
            int pos = date.getSelectedItemPosition();
            
            dateAdapter.clear();
          
            //dateAdapter.add(new DateSelection(-1, getActivity()));
          
            Cursor dates = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME}, null, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
            
            if(dates.moveToLast()) {
              long last = dates.getLong(0);
              
              Calendar lastDay = Calendar.getInstance();
              lastDay.setTimeInMillis(last);
              
              lastDay.set(Calendar.HOUR_OF_DAY, 0);
              lastDay.set(Calendar.MINUTE, 0);
              lastDay.set(Calendar.SECOND, 0);
              lastDay.set(Calendar.MILLISECOND, 0);
              
              Calendar today = Calendar.getInstance();
              today.set(Calendar.HOUR_OF_DAY, 0);
              today.set(Calendar.MINUTE, 0);
              today.set(Calendar.SECOND, 0);
              today.set(Calendar.MILLISECOND, 0);
              
              long todayStart = today.getTimeInMillis();
              long lastStart = lastDay.getTimeInMillis();
              
              for(long day = todayStart; day <= lastStart; day += (24 * 60 * 60000)) {
                dateAdapter.add(new DateSelection(day, getActivity()));
              }
            }
            
            dates.close();
            
            if(date.getCount() > pos) {
              date.setSelection(pos);
            }
            else {
              date.setSelection(date.getCount()-1);
            }
          }
        }
      };
      
      IntentFilter dataUpdateFilter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
      
      localBroadcastManager.registerReceiver(dataUpdateReceiver, dataUpdateFilter);
      dataUpdateReceiver.onReceive(null, null);
    }
    else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
        rootView = inflater.inflate(R.layout.program_list_fragment,
            container, false);
        
        final ProgramsListFragment programList = (ProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.programListFragment);
        
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        
        IntentFilter channelUpdateFilter = new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE);
        IntentFilter dataUpdateFilter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
        
        final Spinner date = (Spinner)rootView.findViewById(R.id.date_selection);
        
        ArrayList<DateSelection> dateEntries = new ArrayList<DummySectionFragment.DateSelection>();
        
        final ArrayAdapter<DateSelection> dateAdapter = new ArrayAdapter<DummySectionFragment.DateSelection>(getActivity(), android.R.layout.simple_spinner_item, dateEntries);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        date.setAdapter(dateAdapter);
        
        date.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, 
              int pos, long id) {
            DateSelection selection = dateAdapter.getItem(pos);
            
            programList.setDay(selection.getTime());
          }
          
          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            programList.setDay(-1);
          }
        });
        
        BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if(getActivity() != null && !isDetached()) {
              dateAdapter.clear();
            
              dateAdapter.add(new DateSelection(-1, getActivity()));
            
              Cursor dates = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME}, null, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
              
              if(dates.moveToLast()) {
                long last = dates.getLong(0);
                
                Calendar lastDay = Calendar.getInstance();
                lastDay.setTimeInMillis(last);
                
                lastDay.set(Calendar.HOUR_OF_DAY, 0);
                lastDay.set(Calendar.MINUTE, 0);
                lastDay.set(Calendar.SECOND, 0);
                lastDay.set(Calendar.MILLISECOND, 0);
                
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                
                long todayStart = today.getTimeInMillis();
                long lastStart = lastDay.getTimeInMillis();
                
                for(long day = todayStart; day <= lastStart; day += (24 * 60 * 60000)) {
                  dateAdapter.add(new DateSelection(day, getActivity()));
                }
              }
              
              dates.close();
            }
          }
        };
        
        localBroadcastManager.registerReceiver(dataUpdateReceiver, dataUpdateFilter);
        dataUpdateReceiver.onReceive(null, null);
        
        final Spinner channel = (Spinner)rootView.findViewById(R.id.channel_selection);
        
        final Button minus = (Button)rootView.findViewById(R.id.channel_minus);
        minus.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.list_selector_background));
        
        final Button plus = (Button)rootView.findViewById(R.id.channel_plus);
        plus.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.list_selector_background));
                
        final ArrayList<ChannelSelection> channelEntries = new ArrayList<DummySectionFragment.ChannelSelection>();
        
        final ArrayAdapter<ChannelSelection> channelAdapter = new ArrayAdapter<DummySectionFragment.ChannelSelection>(getActivity(), android.R.layout.simple_spinner_item, channelEntries) {
          @Override
          public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
          }
          
          private View getView(int position, View convertView, ViewGroup parent, int id) {
            if(convertView == null) {
              LayoutInflater inflater = getActivity().getLayoutInflater();
              
              convertView = inflater.inflate(id, parent, false);
              ((TextView)convertView).setCompoundDrawablePadding(10);
              ((TextView)convertView).setGravity(Gravity.CENTER_VERTICAL);
            }
            
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            
            int logoValue = Integer.parseInt(pref.getString(getActivity().getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAMS_LIST), "1"));
            boolean showOrderNumber = pref.getBoolean(getResources().getString(R.string.SHOW_SORT_NUMBER_IN_PROGRAMS_LIST), false);
            
            ChannelSelection sel = getItem(position);
            
            TextView text = (TextView)convertView;
            
            if(sel.getID() == -1) {
              text.setText(getResources().getString(R.string.all_channels));
              text.setCompoundDrawables(null, null, null, null);
            }
            else {
              if(logoValue == 0 || logoValue == 2 || sel.getLogo() == null) {
                if(showOrderNumber) {
                  text.setText(sel.getOrderNumber() + sel.getName());
                }
                else {
                  text.setText(sel.getName());
                }
              }
              else if(showOrderNumber) {
                text.setText(sel.getOrderNumber());
              }
              else {
                text.setText("");
              }
              
              if((logoValue == 0 || logoValue == 1) && sel.getLogo() != null) {
                Drawable l = sel.getLogo();
                                
                text.setCompoundDrawables(l, null, null, null);
              }
              else {
                text.setCompoundDrawables(null, null, null, null);
              }
            }
            
            return convertView;
          }
          
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent, android.R.layout.simple_spinner_item);
          }
        };
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channel.setAdapter(channelAdapter);
        
        channel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, 
              int pos, long id) {
            ChannelSelection selection = channelAdapter.getItem(pos);
            
            if(programList != null) {
              programList.setChannelID(selection.getID());
            }
          }
          
          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            programList.setChannelID(-1);
          }
        });
        
        View.OnClickListener onClick = new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            int pos = channel.getSelectedItemPosition();
            
            if(v.equals(minus)) {
              if(--pos < 0) {
                pos = channel.getCount()-1;
              }
              
              channel.setSelection(pos);
            }
            else {
              if(++pos >= channel.getCount()) {
                pos = 0;
              }
              
              channel.setSelection(pos);
            }
          }
        };
        
        minus.setOnClickListener(onClick);
        plus.setOnClickListener(onClick);
        
        final Spinner filter = (Spinner)rootView.findViewById(R.id.program_selection);
        
        ArrayList<String> filterEntries = new ArrayList<String>();
        
        final ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, filterEntries) {
          @Override
          public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
          }
          
          private View getView(int position, View convertView, ViewGroup parent, int id) {
            if(convertView == null) {
              LayoutInflater inflater = getActivity().getLayoutInflater();
              
              convertView = inflater.inflate(id, parent, false);
              ((TextView)convertView).setGravity(Gravity.CENTER_VERTICAL);
            }
            
            String sel = getItem(position);
            
            TextView text = (TextView)convertView;
            text.setText(sel);
            
            switch(position) {
              case 0: convertView.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.list_selector_background));break;
              case 1: convertView.setBackgroundResource(R.color.mark_color_favorite);break;
              case 2: convertView.setBackgroundResource(R.color.mark_color);break;
              case 3: convertView.setBackgroundResource(R.color.mark_color_calendar);break;
              case 4: convertView.setBackgroundResource(R.color.mark_color_sync_favorite);break;
              case 5: convertView.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.list_selector_background));break;
            }
            
            return convertView;
          }
          
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent, android.R.layout.simple_spinner_item);
          }
        };
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filter.setAdapter(filterAdapter);
        
        filterAdapter.add(" "  + getActivity().getString(R.string.all_programs));
        filterAdapter.add(getResources().getString(R.string.title_favorites));
        filterAdapter.add(getResources().getString(R.string.marking_value_marked));
        
        if(Build.VERSION.SDK_INT >= 14) {
          filterAdapter.add(getResources().getString(R.string.marking_value_reminder) + "/" + getResources().getString(R.string.marking_value_calendar));
        }
        else {
          filterAdapter.add(getResources().getString(R.string.marking_value_reminder));
        }
        
        filterAdapter.add(getResources().getString(R.string.marking_value_sync));
        filterAdapter.add(" "  + getResources().getString(R.string.action_dont_want_to_see));
        
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if(programList != null) {
              programList.setMarkFilter(pos);
            }
          }
          
          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            programList.setMarkFilter(-1);
          }
        });
        
        mChannelUpdateReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if(getActivity() != null && !isDetached()) {
              channelAdapter.clear();
              
              channelAdapter.add(new ChannelSelection(-1, "0", getResources().getString(R.string.all_channels), null));
              
              ContentResolver cr = getActivity().getContentResolver();
              
              StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
              where.append("=1");
              
              Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_LOGO,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
              
              if(channelCursor.getCount() > 0) {
                channelCursor.moveToFirst();
                  
                do {
                  boolean hasLogo = !channelCursor.isNull(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
                  
                  LayerDrawable logoDrawable = null;
                  
                  if(hasLogo) {
                    byte[] logoData = channelCursor.getBlob(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
                    
                    if(logoData != null && logoData.length > 0) {
                      Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
                      
                      if(logo != null) {
                        BitmapDrawable l = new BitmapDrawable(getResources(), logo);
                                                
                        ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
                        background.setBounds(0, 0, logo.getWidth()+2,logo.getHeight()+2);
                        
                        logoDrawable = new LayerDrawable(new Drawable[] {background,l});
                        logoDrawable.setBounds(background.getBounds());
                        
                        l.setBounds(2, 2, logo.getWidth(), logo.getHeight());
                      }
                    }
                  }
                  
                  String name = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
                  String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
                  
                  if(shortName != null) {
                    name = shortName;
                  }
                  
                  ChannelSelection channelSel = new ChannelSelection(channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)), channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER)) + ". ", name, logoDrawable);
                  
                  channelAdapter.add(channelSel);
                }while(channelCursor.moveToNext());
              }
              
              channelCursor.close();
            }
          }
        };
        
        localBroadcastManager.registerReceiver(mChannelUpdateReceiver, channelUpdateFilter);
        mChannelUpdateReceiver.onReceive(null, null);
        
        IntentFilter showChannelFilter = new IntentFilter(SettingConstants.SHOW_ALL_PROGRAMS_FOR_CHANNEL_INTENT);
        
        BroadcastReceiver showChannel = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if(getActivity() instanceof TvBrowser && !isDetached()) {
              int id = intent.getIntExtra(SettingConstants.CHANNEL_ID_EXTRA, -1);
              long startTime = intent.getLongExtra(SettingConstants.START_TIME_EXTRA, -1);
              
              ChannelSelection current = (ChannelSelection)channel.getSelectedItem();
              boolean found = false;
              
              if(current == null || current.getID() != id) {
                for(int i = 0; i < channelEntries.size(); i++) {
                  ChannelSelection sel = channelEntries.get(i);
                  
                  if(sel.getID() == id) {
                    channel.setSelection(i);
                    found = true;
                    break;
                  }
                }
              }
              
              filter.setSelection(0);
              date.setSelection(0);
              
              programList.setScrollTime(startTime);
              
              if(!found) {
                programList.scrollToTime();
              }
              
              ((TvBrowser)getActivity()).showProgramsListTab();
            }
          }
        };
        
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(showChannel, showChannelFilter);
        
        IntentFilter scrollToTimeFilter = new IntentFilter(SettingConstants.SCROLL_TO_TIME_INTENT);
        
        BroadcastReceiver scrollToTimeReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if(getActivity() instanceof TvBrowser && !isDetached()) {
              long startTime = intent.getLongExtra(SettingConstants.START_TIME_EXTRA, (long)-2);
                            
              Log.d("info1", "" + startTime);
              if(startTime >= 0) {
                programList.setScrollTime(startTime);
                programList.scrollToTime();
              }
            }
          }
        };
        
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(scrollToTimeReceiver, scrollToTimeFilter);
    }
    else {
      rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
        container, false);
    }
    
    return rootView;
  }
}
