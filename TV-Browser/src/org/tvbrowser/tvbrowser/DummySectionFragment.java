package org.tvbrowser.tvbrowser;

import java.util.ArrayList;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
      //Log.d(TAG, String.valueOf(rootView));
      
      final RunningProgramsListFragment running = (RunningProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.runningListFragment);
      Log.d("TVB", String.valueOf(running));
      //final RunningProgramsListFragment running = (RunningProgramsListFragment)rootView.findViewById(R.id.runningListFragment);
      
      View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          running.setWhereClauseID(v.getId());
        }
      };
      
      rootView.findViewById(R.id.now_button).setOnClickListener(listener);
      rootView.findViewById(R.id.button_6).setOnClickListener(listener);
      rootView.findViewById(R.id.button_12).setOnClickListener(listener);
      rootView.findViewById(R.id.button_16).setOnClickListener(listener);
      rootView.findViewById(R.id.button_2015).setOnClickListener(listener);
      rootView.findViewById(R.id.button_23).setOnClickListener(listener);
      
   /*   View list = rootView.findViewById(R.id.runningListFragment);
      
      Log.d(TAG, String.valueOf(list));
      
      /*((ListView)rootView).setOnItemClickListener(new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          // TODO Auto-generated method stub
          
        }
      
      });/*.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Object value = v.getTag();
          
          Log.d(TAG, " value " + String.valueOf(value));
        }
      });*/
    }
    else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
      rootView = inflater.inflate(R.layout.program_list_fragment,
          container, false);
      
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
      Set<String> channels = preferences.getStringSet(SettingConstants.SUBSCRIBED_CHANNELS, null);
      
      if(channels != null) {
        ContentResolver cr = getActivity().getContentResolver();
        
        StringBuilder where = new StringBuilder(TvBrowserContentProvider.KEY_ID);
        where.append(" IN (");
  
        for(String key : channels) {
          where.append(key);
          where.append(", ");
        }
        
        where.delete(where.length()-2,where.length());
        
        where.append(")");
        
     //   Log.d(TAG, where.toString());
        
        Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
        
        if(channelCursor.getCount() > 0) {
          channelCursor.moveToFirst();
          
          LinearLayout parent = (LinearLayout)rootView.findViewById(R.id.button_bar);
          
          final ProgramsListFragment programList = (ProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.programListFragment);
          
          View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              programList.setChannelID((Long)v.getTag());
            }
          };
          
          Button all = (Button)parent.findViewById(R.id.all_channels);
          all.setTag(Long.valueOf(-1));
          all.setOnClickListener(listener);
          
          do {
            Button channelButton = new Button(getActivity(),null,android.R.attr.buttonBarButtonStyle);
            channelButton.setTag(channelCursor.getLong(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
            
            channelButton.setOnClickListener(listener);
            
            //Log.d("TVB", String.valueOf(running));
            //final RunningProgramsListFragment running = (RunningProgramsListFragment)rootView.findViewById(R.id.runningListFragment);
            
            channelButton.setText(channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
            
            parent.addView(channelButton);
          }while(channelCursor.moveToNext());
        }
        
        channelCursor.close();
      }
    }
    else {
      rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
        container, false);
    }
    
    return rootView;
  }
}
