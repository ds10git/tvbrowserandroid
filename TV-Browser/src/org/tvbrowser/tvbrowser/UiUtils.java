package org.tvbrowser.tvbrowser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.AvoidXfermode;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class UiUtils {
  public static void showProgramInfo(Activity activity, long id) {

    Cursor c = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
    
    c.moveToFirst();
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    
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
    
    Cursor channel = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
    
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
  
  public static void createContextMenu(Activity activity, ContextMenu menu) { 
    activity.getMenuInflater().inflate(R.menu.program_context, menu);
  }
  
  public static boolean handleContextMenuSelection(Activity activity, MenuItem item, long programID, View menuView) {
    Cursor info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
    
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
      
      if(menuView != null) {
        if(current != null && current.contains("calendar")) {
          menuView.setBackgroundColor(activity.getResources().getColor(R.color.mark_color_calendar));
        }
        else {
          menuView.setBackgroundColor(activity.getResources().getColor(R.color.mark_color));
        }      
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
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
      
      if(menuView != null) {
        menuView.setBackgroundResource(android.R.drawable.list_selector_background);
      }
    }
    else if(item.getItemId() == R.id.prog_create_calendar_entry) {
      info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        
        if(channel.getCount() > 0) {
          channel.moveToFirst();
          
          // Create a new insertion Intent.
          Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT);
          
          addCalendarEntry.setType(activity.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
          
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
          activity.startActivity(addCalendarEntry);
          
          if(current != null && current.contains("calendar")) {
            return true;
          }
          else if(current == null) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "calendar");
          }
          else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";calendar");
          }
          
          if(menuView != null) {
            menuView.setBackgroundColor(activity.getResources().getColor(R.color.mark_color_calendar));
          }
        }
        
        channel.close();
      }
      
      info.close();
    }
    
    if(values.size() > 0) {
      if(menuView != null) {
        menuView.invalidate();
      }
      
      Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
      intent.putExtra(SettingConstants.MARKINGS_ID, programID);
      
      LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
      
      activity.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
    }
    
    return true;
  }
}
