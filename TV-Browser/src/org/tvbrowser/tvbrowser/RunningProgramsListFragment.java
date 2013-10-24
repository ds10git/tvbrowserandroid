package org.tvbrowser.tvbrowser;

import java.util.Calendar;
import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RunningProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String WHERE_CLAUSE_KEY = "WHERE_CLAUSE_KEY";
  SimpleCursorAdapter adapter;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private int mWhereClauseID;
  private int mTimeRangeID;
  
  private BroadcastReceiver mDataUpdateReceiver;
  
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
            if(!isDetached() && mKeepRunning) {
              getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
            }
          }
        });
      }
    };
    
    IntentFilter intent = new IntentFilter(SettingConstants.DATA_UPDATE_DONE);
    
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDataUpdateReceiver, intent);
  }
  
  public void setWhereClauseID(int id) {
    Button test = (Button)((View)getView().getParent()).findViewById(mWhereClauseID);
    
    if(test != null) {
      test.setBackgroundResource(android.R.drawable.list_selector_background);
    }
    
    if(mDataUpdateReceiver != null) {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataUpdateReceiver);
    }
    
    mWhereClauseID = id;
    
    if(mKeepRunning) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(!isDetached()) {
            getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
          }
        }
      });
    }
  }
  
  public void setTimeRangeID(int id) {
    Button test = (Button)((View)getView().getParent()).findViewById(mTimeRangeID);
    
    if(test != null) {
      test.setBackgroundResource(android.R.drawable.list_selector_background);
    }
    
    mTimeRangeID = id;
    
    if(mKeepRunning) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(!isDetached()) {
            getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
          }
        }
      });
    }
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(WHERE_CLAUSE_KEY, mWhereClauseID);
    super.onSaveInstanceState(outState);
  }
    
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    if(savedInstanceState != null) {
      mWhereClauseID = savedInstanceState.getInt(WHERE_CLAUSE_KEY,R.id.now_button);
    }
    else {
      mWhereClauseID = R.id.now_button;
    }
    
    mTimeRangeID = R.id.button_at;
    
    registerForContextMenu(getListView());
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
        TvBrowserContentProvider.DATA_KEY_CATEGORIES
    };
    
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(getActivity(),R.layout.running_list_entries,null,
        projection,new int[] {R.id.startTimeLabel,R.id.endTimeLabel,R.id.channelLabel,R.id.titleLabel,R.id.episodeLabel,R.id.genre_label,R.id.picture_copyright,R.id.info_label},0);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        boolean showPicture = pref.getBoolean(view.getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false);
        boolean showGenre = pref.getBoolean(view.getResources().getString(R.string.SHOW_GENRE_IN_LISTS), true);
        boolean showEpisode = pref.getBoolean(view.getResources().getString(R.string.SHOW_EPISODE_IN_LISTS), true);
        boolean showInfo = pref.getBoolean(view.getResources().getString(R.string.SHOW_INFO_IN_LISTS), true);
        
        if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) {
          int channelID = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
          
          Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), null, null, null, null);
          
          if(channel.getCount() > 0) {
            channel.moveToFirst();
            TextView text = (TextView)view;
            text.setText(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
          }
          channel.close();
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) {
          if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) || !showInfo) {
            view.setVisibility(View.GONE);
          }
          else {
            int info = cursor.getInt(columnIndex);
            
            if(info != 0) {
              view.setVisibility(View.VISIBLE);
              ((TextView)view).setText(IOUtils.getInfoString(info,view.getResources()));
            }
            else {
              view.setVisibility(View.GONE);
            }
          }
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          TextView text = (TextView)view;
          text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
          text.setText(DateFormat.getTimeFormat(getActivity()).format(new Date(date)));
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          TextView text = (TextView)view;
          text.setText(DateFormat.getTimeFormat(getActivity()).format(new Date(date)));
          
          UiUtils.handleMarkings(getActivity(), cursor, ((RelativeLayout)view.getParent()), null);
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)) {
          TextView text = (TextView)view;
          
          long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          if(end <= System.currentTimeMillis()) {
            text.setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.info_label)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.picture_copyright)).setTextColor(Color.rgb(200, 200, 200));
          }
          else if(System.currentTimeMillis() <= end) {
            int[] attrs = new int[] { android.R.attr.textColorSecondary };
            TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
            int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
            a.recycle();
            
            text.setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.info_label)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.picture_copyright)).setTextColor(DEFAULT_TEXT_COLOR);
          }
          //
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) {
          if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) || !showEpisode) {
            view.setVisibility(View.GONE);
          }
          else {
            view.setVisibility(View.VISIBLE);
          }
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) {
          TextView text = (TextView)view;
          
          if(cursor.isNull(columnIndex) || !showGenre) {
            text.setVisibility(View.GONE);
          }
          else {
            text.setVisibility(View.VISIBLE);
          }
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)) {
          TextView text = (TextView)view;
          ImageView picture = (ImageView)((RelativeLayout)text.getParent()).findViewById(R.id.picture);
          
          if(!cursor.isNull(columnIndex) && cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE) != -1 && showPicture) {
            byte[] logoData = cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE));
            Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
                      
            BitmapDrawable l = new BitmapDrawable(view.getResources(), logo);
            
            l.setBounds(0, 0, logo.getWidth(), logo.getHeight());
            
            long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
            
            if(end <= System.currentTimeMillis()) {
              l.setColorFilter(getActivity().getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.LIGHTEN);
            }
            
            picture.setImageDrawable(l);
            
            text.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));
            text.setVisibility(View.VISIBLE);
            picture.setVisibility(View.VISIBLE);
          }
          else {
            picture.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
          }
        }
        
        return false;
      }
  });
    
    setListAdapter(adapter);
    
    getLoaderManager().initLoader(0, null, this);
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
            if(mKeepRunning) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  if(!isDetached() &&  mKeepRunning && !isRemoving()) {
                    getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
                  }
                }
              });
            }
            
            sleep(60000);
          } catch (InterruptedException e) {
          }
        }
      }
    };
  }

  @Override
  public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] projection = null;
    
    if(PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean(getResources().getString(R.string.SHOW_PICTURE_IN_LISTS), false)) {
      projection = new String[13];
      
      projection[12] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[12];
    }
    
    projection[0] = TvBrowserContentProvider.KEY_ID;
    projection[1] = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
    projection[2] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    projection[3] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
    projection[4] = TvBrowserContentProvider.DATA_KEY_TITLE;
    projection[5] = TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION;
    projection[6] = TvBrowserContentProvider.DATA_KEY_MARKING_VALUES;
    projection[7] = TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER;
    projection[8] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
    projection[9] = TvBrowserContentProvider.DATA_KEY_GENRE;
    projection[10] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
    projection[11] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 30);
    
    switch(mWhereClauseID) {
      case R.id.button_6: 
        cal.set(Calendar.HOUR_OF_DAY, 6);break;
      case R.id.button_12:
        cal.set(Calendar.HOUR_OF_DAY, 12);break;
      case R.id.button_16:
        cal.set(Calendar.HOUR_OF_DAY, 16);break;
      case R.id.button_2015:
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 15);break;
      case R.id.button_23:
        cal.set(Calendar.HOUR_OF_DAY, 23);break;
      default: cal.setTimeInMillis(System.currentTimeMillis());break;
    }

    if(getView().getParent() != null) {
      Button test = (Button)((View)getView().getParent()).findViewById(mWhereClauseID);
      
      if(test != null) {
        test.setBackgroundResource(R.color.filter_selection);
      }
      
      test = (Button)((View)getView().getParent()).findViewById(mTimeRangeID);
      
      if(test != null) {
        test.setBackgroundResource(R.color.filter_selection);
      }
    }
    
    long time = ((long)cal.getTimeInMillis() / 60000) * 60000;

    switch (mTimeRangeID) {
      case R.id.button_before: time -= (60000 * 60);break;
      case R.id.button_after: time += (60000 * 60);break;
    }
    
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + time + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + time;
        
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    return loader;
  }

  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    adapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    adapter.swapCursor(null);
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {

    long programID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    UiUtils.createContextMenu(getActivity(), menu, programID);
  }
  
 // @Override
 /* public boolean onContextItemSelected(MenuItem item) {
    if(item.getMenuInfo() != null) {
      long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
    
      return UiUtils.handleContextMenuSelection(getActivity(), item, programID, null);
    }
    
    return false;
  }*/
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    UiUtils.showProgramInfo(getActivity(), id);
  }
}
