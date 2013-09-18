package org.tvbrowser.tvbrowser;

import java.text.DateFormat;
import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter adapter;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  
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
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
    };
    
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL},0);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
       // Log.d("TVB", " COLUMN " + columnIndex);
        if(columnIndex == 1) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          TextView text = (TextView)view;
          text.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(date)));

          String value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
          
          if(value != null && value.trim().length() > 0) {
            ((LinearLayout)view.getParent()).setBackgroundResource(R.color.mark_color);
          }
          else {
            ((LinearLayout)view.getParent()).setBackgroundResource(android.R.color.background_light);
            //((LinearLayout)view.getParent()).setBackgroundColor(view.getBackground().get);
          }
          
          return true;
        }
        else if(columnIndex == 2) {
          int channelID = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
          
          Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), null, null, null, null);
        //  Log.d("TVB", " CHANNELCURSOR " + channel.getCount());
          if(channel.getCount() > 0) {
            channel.moveToFirst();
            TextView text = (TextView)view;
            text.setText(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
          }
          channel.close();
          
          return true;
        }
        else if(columnIndex == 3) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          TextView text = (TextView)view;
          text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));

          text.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(date)));
          
          return true;
        }
        else if(columnIndex == 4) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          TextView text = (TextView)view;
          text.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(date)));
          
          return true;
        }
                
        return false;
      }
  });
    
    setListAdapter(adapter);
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    Object o = v.findViewById(R.id.startTimeLabelPL).getTag();
    
    if(o instanceof Long) {
      showPopupMenu(v,(Long)o);
      Log.d("TVB", String.valueOf(o));
    }
  }
  
  private void showPopupMenu(final View v, final long programID) {
    PopupMenu popup = new PopupMenu(getActivity(), v);
    popup.getMenuInflater().inflate(R.menu.popupmenu, popup.getMenu());
    
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        Cursor info = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
        
        String current = null;
        
        if(info.getCount() > 0) {
          info.moveToFirst();
          
          if(!info.isNull(0)) {
            current = info.getString(0);
          }
        }
        
        ContentValues values = new ContentValues();
        
        if(item.getItemId() == R.id.mark_item) {
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
          
        }
        else {
          if(current == null || current.trim().length() == 0) {
            return true;
          }
          
          if(current.contains(";marked")) {
            current = current.replace(";marked", "");
          }
          else if(current.contains("marked;")) {
            current = current.replace("marked;", "");
          }
          else if(current.contains("marked")) {
            current = current.replace("marked", "");
          }
          
          Log.d("TVB", String.valueOf(current));
          
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
          
          Log.d("TVB","UNMARK " + programID);
        }
        
        v.invalidate();
        getActivity().getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
        
        return true;
      }
    });
    
    popup.show();
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
                  if(!isDetached()) {
                    getLoaderManager().restartLoader(0, null, ProgramsListFragment.this);
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
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES
    };
    
    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis();
        
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
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
}
