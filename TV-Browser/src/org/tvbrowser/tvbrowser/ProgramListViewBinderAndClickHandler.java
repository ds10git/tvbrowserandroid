package org.tvbrowser.tvbrowser;

import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.Activity;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ProgramListViewBinderAndClickHandler implements SimpleCursorAdapter.ViewBinder{
  private Activity mActivity;
  
  public ProgramListViewBinderAndClickHandler(Activity act) {
    mActivity = act;
  }

  @Override
  public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
    if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
      long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
      
      TextView text = (TextView)view;
      text.setText(DateFormat.getTimeFormat(mActivity).format(new Date(date)));
      
      return true;
    } 
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_UNIX_DATE)) {
      UiUtils.formatDayView(mActivity, cursor, view, R.id.startDayLabelPL);
      return true;
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) {
      TextView text = (TextView)view;
      text.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
       
      return true;
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
      long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
       
      TextView text = (TextView)view;
      text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
      text.setText(DateFormat.getTimeFormat(mActivity).format(new Date(date)));
       
      return true;
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)) {
      TextView text = (TextView)view;
      
      long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
      
      if(end <= System.currentTimeMillis()) {
        int[] attrs = new int[] { android.R.attr.textColorSecondary };
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
        int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
        a.recycle();
        
        text.setTextColor(DEFAULT_TEXT_COLOR);
      }
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) {
      if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
        view.setVisibility(View.GONE);
      }
      else {
        view.setVisibility(View.VISIBLE);
      }
    }
    else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) {
      if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
        view.setVisibility(View.GONE);
      }
      else {
        view.setVisibility(View.VISIBLE);
      }
    }     
    return false;
  }

  
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    long programID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    Log.d("TVB", String.valueOf(programID));
    UiUtils.createContextMenu(mActivity, menu, programID);
  }
  
  
  public boolean onContextItemSelected(MenuItem item) {
    if(item.getMenuInfo() != null) {
      long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
      
      UiUtils.handleContextMenuSelection(mActivity, item, programID, null);
    }
    
    return false;
  }
  
  public void onListItemClick(ListView l, View v, int position, long id) {
    UiUtils.showProgramInfo(mActivity, id);
  }
}
