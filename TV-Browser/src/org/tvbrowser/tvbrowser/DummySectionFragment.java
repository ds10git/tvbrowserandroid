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

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

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

  public DummySectionFragment() {
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = null;
    
    if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
      rootView = inflater.inflate(R.layout.running_program_fragment,
          container, false);
      
      final RunningProgramsListFragment running = (RunningProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.runningListFragment);
      
      View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if(running != null) {
            running.setWhereClauseID(v.getId());
          }
        }
      };
      View.OnClickListener timeRange = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if(running != null) {
            running.setTimeRangeID(v.getId());
          }
        }
      };
      
      rootView.findViewById(R.id.now_button).setOnClickListener(listener);
      rootView.findViewById(R.id.button_6).setOnClickListener(listener);
      rootView.findViewById(R.id.button_12).setOnClickListener(listener);
      rootView.findViewById(R.id.button_16).setOnClickListener(listener);
      rootView.findViewById(R.id.button_2015).setOnClickListener(listener);
      rootView.findViewById(R.id.button_23).setOnClickListener(listener);
      
      rootView.findViewById(R.id.button_before).setOnClickListener(timeRange);
      rootView.findViewById(R.id.button_at).setOnClickListener(timeRange);
      rootView.findViewById(R.id.button_after).setOnClickListener(timeRange);
    }
    else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
        rootView = inflater.inflate(R.layout.program_list_fragment,
            container, false);
        
        final LinearLayout parent = (LinearLayout)rootView.findViewById(R.id.button_bar);
        
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        
        IntentFilter channelUpdateFilter = new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE);
        
        final BroadcastReceiver receiver = new BroadcastReceiver() {
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
        
        localBroadcastManager.registerReceiver(receiver, channelUpdateFilter);
        receiver.onReceive(null, null);
    }
    else {
      rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
        container, false);
    }
    
    return rootView;
  }
}
