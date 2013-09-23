package org.tvbrowser.tvbrowser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class TvBrowserSearchResults extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
  private SimpleCursorAdapter adapter;

  private static String QUERY_EXTRA_KEY = "QUERY_EXTRA_KEY";
  private static String QUERY_EXTRA_ID_KEY = "QUERY_EXTRA_ID_KEY";
//
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE
    };
  /*  
            TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,*/
    
    registerForContextMenu(getListView());
    
    // Create a new adapter and bind it to the List View
    //adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, new String[] {TvBrowserContentProvider.DATA_KEY_TITLE}, new int[] {android.R.id.text1}, 0);
 // Create a new Adapter an bind it to the List View
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(this,/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL},0);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        Log.d("TEST", " COLUMN " + columnIndex);
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
            startDay.setText(getResources().getText(R.string.today));
          }
          else if(dateDay == todayDay + 1) {
            startDay.setText(getResources().getText(R.string.tomorrow));
          }
          
          text.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(progDate));

          String value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
          
          if(value != null && value.trim().length() > 0) {
            ((RelativeLayout)view.getParent()).setBackgroundResource(R.color.mark_color);
          }
          else {
            ((RelativeLayout)view.getParent()).setBackgroundResource(android.R.drawable.list_selector_background);
            //((LinearLayout)view.getParent()).setBackgroundColor(view.getBackground().get);
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
            text.setTextColor(TvBrowserSearchResults.this.getResources().getColor(R.color.running_color));
          }
          else {
            int[] attrs = new int[] { android.R.attr.textColorSecondary };
            TypedArray a = TvBrowserSearchResults.this.getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
            int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
            a.recycle();
            
            text.setTextColor(DEFAULT_TEXT_COLOR);
            //text.setTextColor(getActivity().getResources().getColor(android.R.color.primary_text_dark));
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
  });
    
    setListAdapter(adapter);
    
    // Initiate the Cursor Loader
    getLoaderManager().initLoader(0, null, this);
    
    // Get the launch Intent
    parseIntent(getIntent());
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    TvBrowserSearchResults.this.getMenuInflater().inflate(R.menu.popupmenu, menu);
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    parseIntent(getIntent());
  }
  
  private void parseIntent(Intent intent) {
    // If the Activity was started to service a Search request, extract the search query.
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String searchQuery = intent.getStringExtra(SearchManager.QUERY);
      
      // Perfom the search, passing in the search query as an argument to the Cursor Loader
      Bundle args = new Bundle();
      args.putString(QUERY_EXTRA_KEY, searchQuery);
      
      // Restart the Cursor Loader to execute the new query
      getLoaderManager().restartLoader(0, args, this);
    }
    else if(Intent.ACTION_VIEW.equals(intent.getAction())) {
      try {
        long key = Long.parseLong(intent.getData().getPathSegments().get(1));
        
        Bundle args = new Bundle();
        args.putLong(QUERY_EXTRA_ID_KEY, key);
        
        getLoaderManager().restartLoader(0, args, this);
      }catch(NumberFormatException e) {}
      //Log.d("TVB", String.valueOf(intent.getData().getPathSegments().get(1)));
    }
  }
  
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String query = "0";
    
    long ID = -1;
    
    if(args != null) {
      // Extract the search query from the arguments.
      query = args.getString(QUERY_EXTRA_KEY);
      ID = args.getLong(QUERY_EXTRA_ID_KEY, -1);
    }
    
    // Construct the new query in form of a Cursor Loader
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
    };
    /*
    String[] projection = {
        TvBrowserContentProvider.KEY_ID, 
        TvBrowserContentProvider.DATA_KEY_TITLE, 
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE, 
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES
        };*/
    String where = "(" + TvBrowserContentProvider.DATA_KEY_TITLE + " LIKE \"%" + query + "%\" OR " + TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE + " LIKE \"%" + query + "%\") AND " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " >= " + System.currentTimeMillis();
    String[] whereArgs = null;
    String sortOrder = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    
    Uri uri = TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL;
    
    if(ID != -1) {
      where = null;
      uri = ContentUris.withAppendedId(uri, ID);
    }
    
    // Create the new Cursor loader
    return new CursorLoader(this, uri, projection, where, whereArgs, sortOrder);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    adapter.swapCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    adapter.swapCursor(null);
  }


  @Override
  public boolean onContextItemSelected(MenuItem item) {
    // TODO Auto-generated method stub
    
    long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
    
    Cursor info = TvBrowserSearchResults.this.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
    
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
      info = TvBrowserSearchResults.this.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = TvBrowserSearchResults.this.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        Log.d("TVB", "channel cursor " + channel.getCount());
        if(channel.getCount() > 0) {
          channel.moveToFirst();
       // Create a new insertion Intent.
          Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT/*, CalendarContract.Events.CONTENT_URI*/);
          Log.d("TVB",TvBrowserSearchResults.this.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
          
          //Intent intent = new Intent(Intent.ACTION_INSERT);
          addCalendarEntry.setType(TvBrowserSearchResults.this.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
               
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
          startActivity(addCalendarEntry);
          
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
      TvBrowserSearchResults.this.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
    }
    
    return true;
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    Cursor c = TvBrowserSearchResults.this.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
    
    c.moveToFirst();
    
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowserSearchResults.this);
    
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
    
    Cursor channel = TvBrowserSearchResults.this.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
    
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
    
  }
}
