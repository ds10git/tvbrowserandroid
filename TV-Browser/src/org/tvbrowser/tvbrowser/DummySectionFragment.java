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

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

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
      
      IntentFilter channelUpdateFilter = new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE);
      
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
            
            Collections.sort(values);
            
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
      
      localBroadcastManager.registerReceiver(receiver, channelUpdateFilter);
      receiver.onReceive(null, null);
      
    /*  rootView.findViewById(R.id.button_before1).setOnClickListener(timeRange);
      rootView.findViewById(R.id.button_after1).setOnClickListener(timeRange);*/
    }
    else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
        rootView = inflater.inflate(R.layout.program_list_fragment,
            container, false);
        
        final LinearLayout parent = (LinearLayout)rootView.findViewById(R.id.button_bar);
        
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        
        IntentFilter channelUpdateFilter = new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE);
        
        mChannelUpdateReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            Button all = (Button)parent.findViewById(R.id.all_channels);
            
            parent.removeAllViews();
            
            parent.addView(all);
            
            if(getActivity() != null) {
              ContentResolver cr = getActivity().getContentResolver();
              
              StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
              where.append(" = 1");
              
              Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_LOGO}, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
              
              if(channelCursor.getCount() > 0) {
                channelCursor.moveToFirst();
                
                final ProgramsListFragment programList = (ProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.programListFragment);
                View.OnClickListener listener = new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    
                    
                    if(programList != null) {
                      programList.setChannelID((Long)v.getTag());
                    }
                  }
                };
                
                //Button all = (Button)parent.findViewById(R.id.all_channels);
                all.setTag(Long.valueOf(-1));
                all.setOnClickListener(listener);
                
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                
                int logoValue = Integer.parseInt(pref.getString(getActivity().getResources().getString(R.string.CHANNEL_LOGO_NAME_PROGRAMS_LIST), "0"));
                              
                do {
                  boolean hasLogo = !channelCursor.isNull(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
                  
                  Button channelButton = new Button(getActivity(),null,android.R.attr.buttonBarButtonStyle);
                  channelButton.setTag(channelCursor.getLong(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
                  channelButton.setPadding(15, 0, 15, 0);
                  channelButton.setCompoundDrawablePadding(10);
                  channelButton.setOnClickListener(listener);
                  
                  if(logoValue == 0 || logoValue == 2 || !hasLogo) {
                    channelButton.setText(channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
                  }
                  
                  if((logoValue == 0 || logoValue == 1) && hasLogo) {
                    byte[] logoData = channelCursor.getBlob(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
                    Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
                    BitmapDrawable l = new BitmapDrawable(getResources(), logo);
                    l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
                    
                    channelButton.setCompoundDrawables(l, null, null, null);
                  }
                  
                  parent.addView(channelButton);
                }while(channelCursor.moveToNext());
              }
              
              channelCursor.close();
            }
          }
        };
        
        localBroadcastManager.registerReceiver(mChannelUpdateReceiver, channelUpdateFilter);
        mChannelUpdateReceiver.onReceive(null, null);
    }
    else {
      rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
        container, false);
    }
    
    return rootView;
  }
}
