package org.tvbrowser.tvbrowser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ProgramListViewBinderAndClickHandler implements SimpleCursorAdapter.ViewBinder{
  private Activity mActivity;
  
  public ProgramListViewBinderAndClickHandler(Activity act) {
    mActivity = act;
  }

  @Override
  public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
     if(columnIndex == 1) {
       long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
       
       TextView text = (TextView)view;
       SimpleDateFormat day = new SimpleDateFormat("EEE", Locale.getDefault());
       
       Date progDate = new Date(date);
       
       TextView startDay = (TextView)((View)view.getParent()).findViewById(R.id.startDayLabelPL);
       startDay.setText(day.format(progDate));
       
       long dateDay = date / 1000 / 60 / 60 / 24;
       long todayDay = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
       
       if(dateDay == todayDay) {
         startDay.setText(mActivity.getText(R.string.today));
       }
       else if(dateDay == todayDay + 1) {
         startDay.setText(mActivity.getText(R.string.tomorrow));
       }
       
       SimpleDateFormat df = (SimpleDateFormat)
       DateFormat.getDateInstance(DateFormat.SHORT);
       String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");
         
       SimpleDateFormat mdf = new SimpleDateFormat(pattern);
       
       //String dateDate = DateFormat.getDateInstance(DateFormat.SHORT).format(progDate);
       
       
       text.setText(mdf.format(progDate));

       String value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
       
       if(value != null && value.trim().length() > 0) {
         if(value.contains("calendar")) {
           ((RelativeLayout)view.getParent()).setBackgroundResource(R.color.mark_color_calendar);
         }
         else {
           ((RelativeLayout)view.getParent()).setBackgroundResource(R.color.mark_color);
         }
       }
       else {
         ((RelativeLayout)view.getParent()).setBackgroundResource(android.R.drawable.list_selector_background);
       }
       
       return true;
     }
     else if(columnIndex == 2) {
       TextView text = (TextView)view;
       text.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
       
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
     else if(columnIndex == 5) {
       TextView text = (TextView)view;
       
       long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
       long start = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
       
       if(System.currentTimeMillis() >= start && System.currentTimeMillis() <= end) {
         text.setTextColor(mActivity.getResources().getColor(R.color.running_color));
       }
       else {
         int[] attrs = new int[] { android.R.attr.textColorSecondary };
         TypedArray a = mActivity.getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
         int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
         a.recycle();
         
         text.setTextColor(DEFAULT_TEXT_COLOR);
       }
     }
     else if(columnIndex == 9) {
       if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
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
    mActivity.getMenuInflater().inflate(R.menu.popupmenu, menu);
  }
  
  
  public boolean onContextItemSelected(MenuItem item) {
    // TODO Auto-generated method stub
    Log.d("test", "clickde");
    
    long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
    
    Cursor info = mActivity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
    
    String current = null;
    
    if(info.getCount() > 0) {
      info.moveToFirst();
      
      if(!info.isNull(0)) {
        current = info.getString(0);
      }
    }
    
    info.close();
    
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
    else if(item.getItemId() == R.id.unmark_item){
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
      
      Log.d("TVB","UNMARK " + programID);
    }
    else if(item.getItemId() == R.id.create_calendar_entry) {
      info = mActivity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = mActivity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        Log.d("TVB", "channel cursor " + channel.getCount());
        if(channel.getCount() > 0) {
          channel.moveToFirst();
          // Create a new insertion Intent.
             Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT/*, CalendarContract.Events.CONTENT_URI*/);
             Log.d("TVB", mActivity.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
             
             //Intent intent = new Intent(Intent.ACTION_INSERT);
             addCalendarEntry.setType(mActivity.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
                  
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
             mActivity.startActivity(addCalendarEntry);
             
             if(current != null && current.contains("calendar")) {
               return true;
             }
             else if(current == null) {
               values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "calendar");
             }
             else {
               values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";calendar");
             }
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
      ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView.invalidate();
     // item.get v.invalidate();
      mActivity.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
    }
    
    return true;
  }
  
  public void onListItemClick(ListView l, View v, int position, long id) {
    //super.onListItemClick(l, v, position, id);
    Log.d("TVB", "ID " + id);
    Object o = v.findViewById(R.id.startTimeLabelPL).getTag();
    
    Cursor c = mActivity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
    
    c.moveToFirst();
    
    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
    
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
    
    Cursor channel = mActivity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
    
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
    
    if(o instanceof Long) {
    //  showPopupMenu(v,(Long)o);
      Log.d("TVB", String.valueOf(o));
    }
  }
}
