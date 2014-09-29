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

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.view.SeparatorDrawable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class ProgramsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener, ShowDateInterface, ShowChannelInterface {
  private static final int NO_CHANNEL_SELECTION_ID = -1;
  
  private SimpleCursorAdapter mProgramListAdapter;
  
  private Handler handler = new Handler();
    
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  
  private long mChannelID;
  private long mScrollTime;
  private long mDayStart;
  
  private String mDayClause;
  private String mFilterClause;
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  
  private BroadcastReceiver mDataUpdateReceiver;
  private BroadcastReceiver mRefreshReceiver;
  private BroadcastReceiver mDontWantToSeeReceiver;
  private BroadcastReceiver mMarkingsChangedReceiver;
  private BroadcastReceiver mChannelUpdateReceiver;
  
  private boolean mDontUpdate;
  private int mScrollPos;
  
  private ListView mListView;
    
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
    
    mDayStart = 0;
    mDayClause = "";
    mFilterClause = "";
    
    mDataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };

    mDontWantToSeeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDontWantToSeeReceiver, new IntentFilter(SettingConstants.DONT_WANT_TO_SEE_CHANGED));
    
    mRefreshReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        startUpdateThread();
      }
    };

    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
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
    if(mDontWantToSeeReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDontWantToSeeReceiver);
    }
    if(mMarkingsChangedReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMarkingsChangedReceiver);
    }
  }
  
  public void setDay(long dayStart) {
    if(dayStart >= 0) {
      mDayStart = dayStart;
      mDayClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">=" + dayStart + " AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + (dayStart + (24 * 60 * 60000)) + " ) ";
    }
    else {
      mDayClause = "";
      mDayStart = 0;
    }
    
    startUpdateThread();
  }
  
  public void setMarkFilter(int pos) {
    switch(pos) {
      case 0: mFilterClause = "";;break;
      case 1: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE + " ) ";break;
      case 2: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_MARKING + " ) ";break;
      case 3: 
        if(Build.VERSION.SDK_INT >= 14) {
          mFilterClause = " AND ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_CALENDAR + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER + " ) ) ";
        }
        else {
          mFilterClause = " AND ( ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " ) OR ( " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER + " ) ) ";
        }
        break;
      case 4: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_MARKING_SYNC + " ) ";break;
      case 5: mFilterClause = " AND ( " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE + " ) ";break;
    }
    
    startUpdateThread();
  }
  
  public void setScrollTime(long time) {
    mScrollTime = time;
  }
  
  public void scrollToTime() {
    if(mScrollTime > 0) {
      int testIndex = 0;
      
      if(mScrollTime <= 1441) {
        mScrollTime--;
        
        if(mDayStart > 0) {
          mScrollTime = mDayStart + mScrollTime * 60000;
        }
        else {
          Calendar now = Calendar.getInstance();
          now.set(Calendar.HOUR_OF_DAY,(int)(mScrollTime / 60));
          now.set(Calendar.MINUTE,(int)(mScrollTime % 60));
          now.set(Calendar.SECOND, 0);
          now.set(Calendar.MILLISECOND, 0);
          
          mScrollTime = now.getTimeInMillis();
          
          if(mScrollTime < System.currentTimeMillis()) {
            mScrollTime += 1440 * 60000;
          }
        }
      }
    
      Cursor c = mProgramListAdapter.getCursor();
      
      if(c.moveToFirst()) {
        try {
          int index = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME);
          int count = 0;
          
          if(!c.isClosed()) {
            do {
              long startTime = c.getLong(index);
              
              if(startTime >= mScrollTime) {
                testIndex = count;
                break;
              }
              else {
                count++;
              }
            }while(c.moveToNext());
          }
        }catch(IllegalStateException e) {}
      }
      
      mScrollTime = -1;
            
      final int scollIndex = testIndex;
      
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(mListView != null) {
            mListView.setSelection(scollIndex);
            handler.post(new Runnable() {
              @Override
              public void run() {
                mListView.setSelection(scollIndex);
              }
            });
          }
        }
      });
    }
    else if(mScrollTime == 0) {
      Spinner test = (Spinner)((ViewGroup)getView().getParent()).findViewById(R.id.date_selection);
      
      if(test != null && test.getSelectedItemPosition() > 0) {
        test.setSelection(0);
      }
      else {
        mScrollTime = -1;
  
        handler.post(new Runnable() {
          @Override
          public void run() {
            if(getView() != null) {
              mListView.setSelection(0);
            }
          }
        });
      }
    }
  }
  
  public void setChannelID(long id) {
    mChannelID = id;
    
    startUpdateThread();
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.program_list_fragment, container, false);
    
    mListView = (ListView)view.findViewById(R.id.program_list_fragment_list_view);
    
    initialize(view);
    
    return view;
  }
  
  private void initialize(View rootView) {
    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    
    IntentFilter channelUpdateFilter = new IntentFilter(SettingConstants.CHANNEL_UPDATE_DONE);
    IntentFilter dataUpdateFilter = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    final Spinner date = (Spinner)rootView.findViewById(R.id.date_selection);
    
    ArrayList<DateSelection> dateEntries = new ArrayList<DateSelection>();
    
    final ArrayAdapter<DateSelection> dateAdapter = new ArrayAdapter<DateSelection>(getActivity(), android.R.layout.simple_spinner_item, dateEntries);
    dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    date.setAdapter(dateAdapter);
    
    date.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, 
          int pos, long id) {
        DateSelection selection = dateAdapter.getItem(pos);
        
        setDay(selection.getTime());
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        setDay(-1);
      }
    });
    
    BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(getActivity() != null && mKeepRunning) {
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
            
            Calendar yesterday = Calendar.getInstance();
            yesterday.set(Calendar.HOUR_OF_DAY, 0);
            yesterday.set(Calendar.MINUTE, 0);
            yesterday.set(Calendar.SECOND, 0);
            yesterday.set(Calendar.MILLISECOND, 0);
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            
            long yesterdayStart = yesterday.getTimeInMillis();
            long lastStart = lastDay.getTimeInMillis();
            
            for(long day = yesterdayStart; day <= lastStart; day += (24 * 60 * 60000)) {
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
            
    final ArrayList<ChannelSelection> channelEntries = new ArrayList<ChannelSelection>();
    
    final ArrayAdapter<ChannelSelection> channelAdapter = new ArrayAdapter<ChannelSelection>(getActivity(), android.R.layout.simple_spinner_item, channelEntries) {
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
        
        int logoValue = Integer.parseInt(PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_PROGRAMS_LIST, R.string.channel_logo_name_programs_list_default));
        boolean showOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_PROGRAMS_LIST, R.bool.show_sort_number_in_programs_list_default);
        
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
        
        setChannelID(selection.getID());
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        setChannelID(-1);
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
          case 1: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_FAVORITE_COLOR_KEY, getContext()));break;
          case 2: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_COLOR_KEY, getContext()));break;
          case 3: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_REMINDER_COLOR_KEY, getContext()));break;
          case 4: convertView.setBackgroundColor(UiUtils.getColor(UiUtils.MARKED_SYNC_COLOR_KEY, getContext()));break;
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
          setMarkFilter(pos);
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        setMarkFilter(-1);
      }
    });
    
    mChannelUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(getActivity() != null && mKeepRunning) {
          channelAdapter.clear();
          
          channelAdapter.add(new ChannelSelection(-1, "0", getResources().getString(R.string.all_channels), null));
          
          ContentResolver cr = getActivity().getContentResolver();
          
          StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
          
          where.append(((TvBrowser)getActivity()).getChannelFilterSelection().replace(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, TvBrowserContentProvider.KEY_ID));
          
          Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_LOGO,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
          
          if(channelCursor.moveToFirst()) {
            do {
              Bitmap logo = UiUtils.createBitmapFromByteArray(channelCursor.getBlob(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));
                            
              LayerDrawable logoDrawable = null;
              
              if(logo != null) {
                logoDrawable = SettingConstants.createLayerDrawable(22, getActivity(), logo);
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
        if(getActivity() instanceof TvBrowser && mKeepRunning) {
          int id = intent.getIntExtra(SettingConstants.CHANNEL_ID_EXTRA, -1);
          long startTime = intent.getLongExtra(SettingConstants.START_TIME_EXTRA, -1);
          int scrollIndex = intent.getIntExtra(SettingConstants.SCROLL_POSITION_EXTRA, -1);
          
          int daySelection = intent.getIntExtra(SettingConstants.DAY_POSITION_EXTRA, -1);
          int filterSelection = intent.getIntExtra(SettingConstants.FILTER_POSITION_EXTRA, -1);
          
          boolean backstackup = !intent.getBooleanExtra(SettingConstants.NO_BACK_STACKUP_EXTRA, false);
          
          ChannelSelection current = (ChannelSelection)channel.getSelectedItem();
          Log.d("info8", "backstackup " + backstackup);
          if(current != null && filterSelection == -1 && backstackup) {                
            ((TvBrowser)getActivity()).addProgramListState(date.getSelectedItemPosition(), current.getID(), filter.getSelectedItemPosition(), getCurrentScrollIndex());
          }
          
          setDontUpdate(true);
          
          if(current == null || current.getID() != id) {
            for(int i = 0; i < channelEntries.size(); i++) {
              ChannelSelection sel = channelEntries.get(i);
              
              if(sel.getID() == id) {
                channel.setSelection(i);
                break;
              }
            }
          }
          
          if(filterSelection >= 0) {
            filter.setSelection(filterSelection);
          }
          else {
            filter.setSelection(0);
          }
          
          if(daySelection >= 0) {
            date.setSelection(daySelection);
          }
          else {
            date.setSelection(0);
          }
                        
          setScrollPos(scrollIndex);
          setScrollTime(startTime);
          setDontUpdate(false);
          startUpdateThread();
                        
          ((TvBrowser)getActivity()).showProgramsListTab(backstackup);
        }
      }
    };
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(showChannel, showChannelFilter);
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    pref.registerOnSharedPreferenceChangeListener(this);
    
    super.onActivityCreated(savedInstanceState);
    mChannelID = NO_CHANNEL_SELECTION_ID;
    mDontUpdate = false;
    mScrollPos = -1;
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
        TvBrowserContentProvider.DATA_KEY_CATEGORIES
    };
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity(),this);
    
    // Create a new Adapter an bind it to the List View
    mProgramListAdapter = new OrientationHandlingCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_lists_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,R.id.picture_copyright_pl,R.id.info_label_pl},0,true);
    
    mProgramListAdapter.setViewBinder(mViewAndClickHandler);
    
    mListView.setAdapter(mProgramListAdapter);
    
    SeparatorDrawable drawable = new SeparatorDrawable(getActivity());
    
    mListView.setDivider(drawable);
    
    setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.devider_size_default));
    
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  public void setDontUpdate(boolean value) {
    mDontUpdate = value;
  }
  
  public void startUpdateThread() {
    if(!mDontUpdate && (mUpdateThread == null || !mUpdateThread.isAlive())) {
      mUpdateThread = new Thread() {
        public void run() {
          handler.post(new Runnable() {
            @Override
            public void run() {
              if(mKeepRunning && !isRemoving() && !TvDataUpdateService.IS_RUNNING) {
                getLoaderManager().restartLoader(0, null, ProgramsListFragment.this);
              }
            }
          });
        }
      };
      mUpdateThread.start();
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] projection = null;
    
    if(PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_LISTS, R.bool.show_pictures_in_lists_default)) {
      projection = new String[15 + TvBrowserContentProvider.MARKING_COLUMNS.length];
      
      projection[projection.length-1] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[14 + TvBrowserContentProvider.MARKING_COLUMNS.length];
    }
    
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
    projection[10] = TvBrowserContentProvider.DATA_KEY_UNIX_DATE;
    projection[11] = TvBrowserContentProvider.CHANNEL_KEY_NAME;
    projection[12] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    projection[13] = TvBrowserContentProvider.CHANNEL_KEY_LOGO;
    
    int startIndex = 14;

    for(int i = startIndex ; i < (startIndex + TvBrowserContentProvider.MARKING_COLUMNS.length); i++) {
      projection[i] = TvBrowserContentProvider.MARKING_COLUMNS[i-startIndex];
    }
    
    long time = System.currentTimeMillis();
    
    String where = mDayClause.trim().length() == 0 ? " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + "<=" + time + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + time + " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + time + " ) " : " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > 0 ) ";
        
    if(mChannelID != NO_CHANNEL_SELECTION_ID) {
      where += "AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + mChannelID;
    }
    
    if(!mFilterClause.contains(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE)) {
      where += UiUtils.getDontWantToSeeFilterString(getActivity());
    }
    
    where += ((TvBrowser)getActivity()).getChannelFilterSelection();
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where + mDayClause + mFilterClause, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    return loader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    mProgramListAdapter.swapCursor(c);
        
    if(mScrollPos == -1) {
      scrollToTime();
    }
    else {
      if(mListView != null) {
        mListView.setSelection(mScrollPos);
      }
      
      mScrollPos = -1;
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mProgramListAdapter.swapCursor(null);
  }
  
  private void setDividerSize(String size) {    
    mListView.setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(getActivity() != null && mKeepRunning && key != null && getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE) != null && getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE).equals(key)) {
      setDividerSize(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.devider_size_default));
    }
  }
  
  public void setScrollPos(int pos) {
    mScrollPos = pos;
  }
  
  public int getCurrentScrollIndex() {
    int pos = mListView.getFirstVisiblePosition();
    
    View view = mListView.getChildAt(0);
    
    if(view != null && mProgramListAdapter.getCount() > 1 && view.getTop() < 0) {
      pos++;
    }
    
    if(pos < 0) {
      pos = 0;
    }
    
    return pos;
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
  
  public void updateChannels() {
    if(mChannelUpdateReceiver != null && getActivity() != null && mKeepRunning) {
      mChannelUpdateReceiver.onReceive(getActivity(), null);
    }
  }

  @Override
  public boolean showDate() {
    String value = PrefUtils.getStringValue(R.string.SHOW_DATE_FOR_PROGRAMS_LIST, R.string.show_date_for_programs_list_default);
    
    boolean returnValue = true;
    
    switch(Integer.parseInt(value)) {
      case 1: returnValue = (mDayStart == 0);break;
      case 2: returnValue = false;break;
    }
    
    return returnValue;
  }

  @Override
  public boolean showChannel() {
    String value = PrefUtils.getStringValue(R.string.SHOW_CHANNEL_FOR_PROGRAMS_LIST, R.string.show_channel_for_programs_list_default);
    
    boolean returnValue = true;
    
    if(Integer.parseInt(value) == 1) {
      returnValue = (mChannelID == NO_CHANNEL_SELECTION_ID);
    }
    
    return returnValue;
  }
}
