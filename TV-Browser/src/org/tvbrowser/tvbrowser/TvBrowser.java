package org.tvbrowser.tvbrowser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

//
public class TvBrowser extends FragmentActivity implements
    ActionBar.TabListener {
  private static final String TAG = "TVB";
  private static final String PROG_TABLE_ACTIVATED = "PROG_TABLE_ACTIVATED";
  private IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
  
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
   * keep every loaded fragment in memory. If this becomes too memory intensive,
   * it may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  SectionsPagerAdapter mSectionsPagerAdapter;
  
  private int mToDownloadChannels;
  private Thread mChannelUpdateThread;
  private boolean updateRunning;
  private boolean selectingChannels;
  private ActionBar actionBar;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  ViewPager mViewPager;
  
  SimpleCursorAdapter adapter;
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putBoolean("updateRunning", updateRunning);
    outState.putBoolean("selectionChannels", selectingChannels);

    super.onSaveInstanceState(outState);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tv_browser);
    
    if(savedInstanceState != null) {
      updateRunning = savedInstanceState.getBoolean("updateRunning", false);
      selectingChannels = savedInstanceState.getBoolean("selectionChannels", false);
    }
//test
    // Set up the action bar.
    actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(
        getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.setOffscreenPageLimit(3);

    // When swiping between different sections, select the corresponding
    // tab. We can also use ActionBar.Tab#select() to do this if we have
    // a reference to the Tab.
    mViewPager
        .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
          }
        });

    // For each of the sections in the app, add a tab to the action bar.
    for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
      // Create a tab with text corresponding to the page title defined by
      // the adapter. Also specify this Activity object, which implements
      // the TabListener interface, as the callback (listener) for when
      // this tab is selected.
      actionBar.addTab(actionBar.newTab()
          .setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
    }

  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    // Don't allow use of version after date
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2013);
    cal.set(Calendar.MONTH, Calendar.OCTOBER);
    cal.set(Calendar.DAY_OF_MONTH, 6);
    
    if(cal.getTimeInMillis() < System.currentTimeMillis()) {    
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setTitle(R.string.versionExpired);
      builder.setMessage(R.string.versionExpiredMsg);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          System.exit(0);
        }
      });
      
      AlertDialog dialog = builder.create();
      dialog.show();
    }
    
    mToDownloadChannels = -1;
    
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    Set<String> channels = preferences.getStringSet(SettingConstants.SUBSCRIBED_CHANNELS, null);
    
    if(!selectingChannels && (channels == null || channels.isEmpty())) {
      selectingChannels = true;
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setMessage(R.string.no_channels);
      builder.setPositiveButton(R.string.select_channels, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          selectChannels(true);
        }
      });
      
      builder.setNegativeButton(R.string.dont_select_channels, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      AlertDialog dialog = builder.create();
      dialog.show();
    }
/*    
    new Thread() {
      public void run() {

        String[] projection = {
            TvBrowserContentProvider.KEY_ID,
            TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
            TvBrowserContentProvider.DATA_KEY_TITLE,
            TvBrowserContentProvider.DATA_KEY_STARTTIME,
            TvBrowserContentProvider.DATA_KEY_ENDTIME,
            TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION
        };
        
        String where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis();
        where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis();
        //String where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis();
       // String where = null;
        Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
        
        
        Log.d(TAG, "DATA-COUNT " + c.getCount());
        
        if(c.getCount() > 0) {
          c.moveToFirst();
          
          int lastChannel = 0;
          int count = 0;
          
          do {
            String[] onlyName = {
                TvBrowserContentProvider.CHANNEL_KEY_NAME
            };
            
            int channelId = c.getInt(1);
            
            Cursor channel = getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelId), onlyName, null, null, null);
            
            String channelName = String.valueOf(c.getInt(1));
            
            if(channel.getCount() > 0) {
              channel.moveToFirst();
              
              channelName = channel.getString(0);
            }
            
            channel.close();
            
            if(channelId == lastChannel) {
              count++;
            }
            else {
              lastChannel = channelId;
              count = 0;
            }
            
            if(count < 2) {
              Log.d(TAG, channelName + " " + DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(c.getLong(3))) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(c.getLong(4))) + " " + c.getString(2) + " LENGTH " + (c.getLong(4) - c.getLong(3)));
            }
          }while(c.moveToNext());
        }
        
        c.close();
      }
    }.start();
    */
  }


  
  private void waitForChannelUpdate() {
    if(mChannelUpdateThread == null || !mChannelUpdateThread.isAlive()) {
      mChannelUpdateThread = new Thread() {
        public void run() {
          while(mToDownloadChannels > 0) {
            try {
              sleep(500);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
          
          if(mToDownloadChannels == 0) {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                showChannelSelection();
              }
            });
          }
        }
      };
      mChannelUpdateThread.start();
    }
  }
  
  private void showChannelSelection() {
   // Log.d(TAG, "select channels");
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME
        };
    
    ContentResolver cr = getContentResolver();
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, null, null, null);
    
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    Set<String> currentChannels = preferences.getStringSet(SettingConstants.SUBSCRIBED_CHANNELS, null);
    
    final ArrayList<ChannelSelection> channelSource = new ArrayList<TvBrowser.ChannelSelection>();
    ArrayList<CharSequence> channelNames = new ArrayList<CharSequence>();
    //final ArrayList<Integer> selectedChannels = new ArrayList<Integer>();
    final boolean[] currentlySelected = new boolean[channels.getCount()];
    
    int i = 1;
    
    if(channels.moveToFirst()) {
      int key = channels.getInt(0);
      String name = channels.getString(1);
      
      channelSource.add(new ChannelSelection(key, name, i));
      channelNames.add(name);
      
      if(currentChannels != null) {
        currentlySelected[i-1] = currentChannels.contains(String.valueOf(key));
      }
      else {
        currentlySelected[i-1] = false;
      }
      
      i++;
      
      while(channels.moveToNext()) {
        key = channels.getInt(0);
        name = channels.getString(1);
        
        if(currentChannels != null) {
          currentlySelected[i-1] = currentChannels.contains(String.valueOf(key));
        }
        else {
          currentlySelected[i-1] = false;
        }
        
        channelSource.add(new ChannelSelection(key, name, i));
        channelNames.add(name);
        
        i++;
      }
    }
    
    channels.close();
    
    if(!channelSource.isEmpty()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.select_channels);
      builder.setMultiChoiceItems(channelNames.toArray(new CharSequence[channelNames.size()]), currentlySelected, new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
          currentlySelected[which] = isChecked;
          /*if(isChecked) {
            selectedChannels.add(which);
          } 
          else if(selectedChannels.contains(which)){
            selectedChannels.remove(new Integer(which));
          }*/
        }
      });
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {        
        @Override
        public void onClick(DialogInterface dialog, int which) {
          SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
          Editor editor = preferences.edit();
          
          HashSet<String> channelSet = new HashSet<String>();
          
          for(int i = 0; i < currentlySelected.length; i++) {
            if(currentlySelected[i]) {
              channelSet.add(String.valueOf(channelSource.get(i).getKey()));
            }
          }
          
          editor.putStringSet(SettingConstants.SUBSCRIBED_CHANNELS, channelSet);
          
          editor.commit();
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {        
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      builder.show();
    }
    
    selectingChannels = false;
  }
  
  private static class ChannelSelection {
    private String mName;
    private int mKey;
    private int mSortNumber;
    
    public ChannelSelection(int key, String name, int sortNumber) {
      mKey = key;
      mName = name;
      mSortNumber = sortNumber;
    }
    
    public int getKey() {
      return mKey;
    }
    
    public void setSortNumber(int value) {
      mSortNumber = value;
    }
    
    public int getSortNumber() {
      return mSortNumber;
    }
    
    public String toString() {
      return mSortNumber + ". " + mName;
    }
  }
  
  
  
  private void selectChannels(boolean loadAgain) {
    if(loadAgain || getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, null, null, null).getCount() < 1) {
      final DownloadManager download = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
      
      Uri uri = Uri.parse("http://www.tvbrowser.org/listings/groups.txt");
      
      DownloadManager.Request request = new Request(uri);
      
      
      final long reference = download.enqueue(request);
      mToDownloadChannels = 1;
      waitForChannelUpdate();
      
      BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
          long receiveReference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
          
          if(reference == receiveReference) {
            unregisterReceiver(this);
            updateGroups(download,reference);
          }
        };
      };
      
      registerReceiver(receiver, filter);
    }
    else {
      showChannelSelection();
    }
   // HttpURLConnection.
  }
  
  private void updateGroups(DownloadManager download, long reference) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(download.openDownloadedFile(reference).getFileDescriptor())));
      
      ContentResolver cr = TvBrowser.this.getContentResolver();

      String line = null;
      
      while((line = in.readLine()) != null) {
        String[] parts = line.split(";");
        
        // Construct a where clause to make sure we don't already have ths group in the provider.
        String w = TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = '" + SettingConstants.EPG_FREE_KEY + "' AND " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = '" + parts[0] + "'";
        
        // If the group is new, insert it into the provider.
        Cursor query = cr.query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, w, null, null);
        
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID, SettingConstants.EPG_FREE_KEY);
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_ID, parts[0]);
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_NAME, parts[1]);
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_PROVIDER, parts[2]);
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_DESCRIPTION, parts[3]);
        
        StringBuilder builder = new StringBuilder(parts[4]);
        
        for(int i = 5; i < parts.length; i++) {
          builder.append(";");
          builder.append(parts[i]);
        }
        
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS, builder.toString());
        
        if(query == null || query.getCount() == 0) {
          // The group is not already known, so insert it
          Uri insert = cr.insert(TvBrowserContentProvider.CONTENT_URI_GROUPS, values);
          
          loadChannelForGroup(download, cr.query(insert, null, null, null, null));
        }
        else {
          cr.update(TvBrowserContentProvider.CONTENT_URI_GROUPS, values, w, null);
          
          loadChannelForGroup(download, cr.query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, w, null, null));
        }
        
        query.close();
      }
      
      in.close();
      
     // cr.delete(download.getUriForDownloadedFile(reference), null, null);
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    mToDownloadChannels--;
  }
  
  private synchronized void loadChannelForGroup(final DownloadManager download, final Cursor cursor) { 
    int index = cursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS);
    
    if(index >= 0) {
      cursor.moveToFirst();
      
      String temp = cursor.getString(index);
      
      index = cursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      final String groupId = cursor.getString(index);
      
      String[] mirrors = null;
      
      if(temp.contains(";")) {
        mirrors = temp.split(";");
      }
      else {
        mirrors = new String[1];
        mirrors[0] = temp;
      }
      
      int idIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
      final int keyID = cursor.getInt(idIndex);
      
      for(String mirror : mirrors) {
        
        if(isConnectedToServer(mirror,5000)) {
          if(!mirror.endsWith("/")) {
            mirror += "/";
          }
        //  Log.i(TAG, "LOADING: " + mirror+groupId+"_channellist.gz");
          mToDownloadChannels++;
          final long requestId = download.enqueue(new Request(Uri.parse(mirror+groupId+"_channellist.gz")));
          
          BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
              long receiveReference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
              
              if(requestId == receiveReference) {
                unregisterReceiver(this);
                addChannels(download,requestId,keyID,groupId);
              }
            };
          };
          
          registerReceiver(receiver, filter);
          
          break;
        }
      }
      
      cursor.close();
    }
  }
  
  // Cursor contains the channel group
  public void addChannels(DownloadManager download, long reference, int uniqueGroupID,final String groupId) {
    try {
      BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(download.openDownloadedFile(reference).getFileDescriptor())),"ISO-8859-1"));
      
      String line = null;
      
      while((line = read.readLine()) != null) {
        String[] parts = line.split(";");
        
        String baseCountry = parts[0];
        String timeZone = parts[1];
        String channelId = parts[2];
        String name = parts[3];
        String copyright = parts[4];
        String website = parts[5];
        String logoUrl = parts[6];
        int category = Integer.parseInt(parts[7]);
        
        StringBuilder fullName = new StringBuilder();
        
        int i = 8;
        
        if(parts.length > i) {
            do {
              fullName.append(parts[i]);
              fullName.append(";");
            }while(!parts[i++].endsWith("\""));
            
            fullName.deleteCharAt(fullName.length()-1);
        }
        
        if(fullName.length() == 0) {
          fullName.append(name);
        }
        
        String allCountries = baseCountry;
        String joinedChannel = "";
        
        if(parts.length > i) {
          allCountries = parts[i++];
        }
        
        if(parts.length > i) {
          joinedChannel = parts[i];
        }
        
        String where = TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = " + uniqueGroupID + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = '" + channelId + "'";
        
        ContentResolver cr = getContentResolver();
        
        Cursor query = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where, null, null);
        
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.GROUP_KEY_GROUP_ID, uniqueGroupID);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, channelId);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_BASE_COUNTRY, baseCountry);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE, timeZone);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_NAME, name);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_COPYRIGHT, copyright);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_WEBSITE, website);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_LOGO_URL, logoUrl);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_CATEGORY, category);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_FULL_NAME, fullName.toString().replaceAll("\"", ""));
        values.put(TvBrowserContentProvider.CHANNEL_KEY_ALL_COUNTRIES, allCountries);
        values.put(TvBrowserContentProvider.CHANNEL_KEY_JOINED_CHANNEL_ID, joinedChannel);
        
        if(query == null || query.getCount() == 0) {
          // add channel
          cr.insert(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values);
        }
        else {
          // update channel
          cr.update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, where, null);
        }
        
        query.close();
      }
      read.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    mToDownloadChannels--;
  }
  
  private class NetworkCheck extends Thread {
    private String mUrl;
    private int mTimeout;
    
    private boolean mSuccess;
    
    public NetworkCheck(String url, int timeout) {
      mUrl = url;
      mTimeout = timeout;
      mSuccess = false;
    }
    
    public void run() {
      try{
        URL myUrl = new URL(mUrl);
        
        
        URLConnection connection;
        connection = myUrl.openConnection();
        connection.setConnectTimeout(mTimeout);
        
        HttpURLConnection httpConnection = (HttpURLConnection)connection;
        int responseCode = httpConnection.getResponseCode();
        
        mSuccess = responseCode == HttpURLConnection.HTTP_OK;
    } catch (Exception e) {
        // Handle your exceptions
   //   Log.d(TAG, "CONNECTIONCHECK", e);
      mSuccess = false;
    }
    }
    
    public boolean success() {
      return mSuccess;
    }
  }
  
  public boolean isConnectedToServer(String url, int timeout) {
    NetworkCheck check = new NetworkCheck(url,timeout);
    check.start();
    try {
      check.join();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return check.success();
  }

  
  

  
  
  
  
  

  
  
  

  
  
  
  private void sortChannels() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    Set<String> channelSet = preferences.getStringSet(SettingConstants.SUBSCRIBED_CHANNELS, null);
    
    if(channelSet != null) {
      ContentResolver cr = getContentResolver();
      
      StringBuilder where = new StringBuilder(TvBrowserContentProvider.KEY_ID);
      where.append(" IN (");

      for(String key : channelSet) {
        where.append(key);
        where.append(", ");
      }
      
      where.delete(where.length()-2,where.length());
      
      where.append(")");
      ListView channelSort = (ListView)getLayoutInflater().inflate(R.layout.channel_sort_list, null);
      
      
      String[] projection = {
          TvBrowserContentProvider.KEY_ID,
          TvBrowserContentProvider.CHANNEL_KEY_NAME,
          TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER
          };
      
      Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
      
      final ArrayList<ChannelSelection> channelSource = new ArrayList<TvBrowser.ChannelSelection>();
      
      
      
      if(channels.moveToFirst()) {
        int key = channels.getInt(0);
        String name = channels.getString(1);
        
        int order = 0;
        
        if(!channels.isNull(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER))) {
          order = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));
        }
        
        channelSource.add(new ChannelSelection(key, name, order));
                
        while(channels.moveToNext()) {
          key = channels.getInt(0);
          name = channels.getString(1);
          
          order = 0;
          
          if(!channels.isNull(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER))) {
            order = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));
          }
          
          channelSource.add(new ChannelSelection(key, name, order));
                    
          Log.d("TVB", order + " order ");
        }
        
        
      }
      
      channels.close();
      
      channelSort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, int position,
            long id) {
          Log.d("TVB",String.valueOf(view) + " " + position + " " + id);
          AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
          final NumberPicker number = (NumberPicker)getLayoutInflater().inflate(R.layout.sort_number_selection, null);
          number.setMinValue(1);
          number.setMaxValue(channelSource.size());
          
          builder.setView(number);
          
          final ChannelSelection selection = channelSource.get(position);
          
          if(selection.getSortNumber() > 0 && selection.getSortNumber() < channelSource.size()+1) {
            number.setValue(selection.getSortNumber());
          }
          
          builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              selection.setSortNumber(number.getValue());
              ((TextView)view).setText(selection.toString());
            }
          });
          
          builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              // TODO Auto-generated method stub
              
            }
          });
          
          builder.show();
        }
      });
      
      ArrayAdapter<ChannelSelection> aa = new ArrayAdapter<TvBrowser.ChannelSelection>(getApplicationContext(), R.layout.channel_row, channelSource);
      channelSort.setAdapter(aa);
      
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.action_sort_channels);
      builder.setView(channelSort);
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          for(ChannelSelection selection : channelSource) {
            ContentValues values = new ContentValues();
            values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, selection.getSortNumber());
            
            int count = getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, selection.getKey()), values, null, null);
            Log.d("TVB", " SORT " + values + " c " + count);
          }
        }
      });
      builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      
      builder.show();
    }
  }

  public boolean isOnline() {
    ConnectivityManager cm =
        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
        return true;
    }
    return false;
  }

  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_update:
        if(isOnline()) {
          startService(new Intent(TvBrowser.this, TvDataUpdateService.class));Log.d(TvDataUpdateService.TAG, "returned from intent");
        }
        else {
          AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
          builder.setTitle(R.string.no_network);
          builder.setMessage(R.string.no_network_info);
          
          builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              
            }
          });
          
          builder.show();
        }
        break;
      case R.id.action_load_channels_again: selectChannels(true);break;
      case R.id.action_select_channels: selectChannels(false);break;
      case R.id.action_sort_channels: sortChannels();break;
      case R.id.action_delete_all_data: getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.KEY_ID + " > 0", null);break;
      case R.id.action_program_table_activated: item.setChecked(!item.isChecked());
                                                Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                                                edit.putBoolean(PROG_TABLE_ACTIVATED, item.isChecked());
                                                edit.commit();
                                                
                                                if(!item.isChecked()) {
                                                  //mViewPager.fi
                                                  mSectionsPagerAdapter.destroyItem(mViewPager, 3, mSectionsPagerAdapter.getRegisteredFragment(3));
                                                  mSectionsPagerAdapter.notifyDataSetChanged();
                                                  actionBar.removeTabAt(3);
                                                  
                                                }
                                                else {
                                                  
                                                  actionBar.addTab(actionBar.newTab()
                                                      .setText(mSectionsPagerAdapter.getPageTitle(3)).setTabListener(this));
                                                  mSectionsPagerAdapter.instantiateItem(mViewPager, 3);
                                                  mSectionsPagerAdapter.notifyDataSetChanged();
                                                }
                                                break;
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.tv_browser, menu);
    
    //  Associate searchable configuration with the SearchView
    SearchManager searchManager =
           (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView =
            (SearchView) menu.findItem(R.id.search).getActionView();
    searchView.setSearchableInfo(
            searchManager.getSearchableInfo(getComponentName()));
    
    
    MenuItem item = menu.findItem(R.id.action_program_table_activated);
    
    item.setChecked(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(PROG_TABLE_ACTIVATED, true));
    
    return true;
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in
    // the ViewPager.
    mViewPager.setCurrentItem(tab.getPosition());
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one
   * of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
    
    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a DummySectionFragment (defined as a static inner class
      // below) with the page number as its lone argument.
      Fragment fragment = null;
      
      if(position < 2) {
        fragment = new DummySectionFragment();
      }
      else if(position == 2) {
        fragment = new FavoritesFragment();
      }
      else if(position == 3) {
        fragment = new ProgramTableFragment();
      }
      
      Bundle args = new Bundle();
      args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
      fragment.setArguments(args);
      
      return fragment;
    }

    @Override
    public int getCount() {
      // Show 3 total pages.
      if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(PROG_TABLE_ACTIVATED, true)) {
        return 4;
      }
      
      return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      Locale l = Locale.getDefault();
            
      switch (position) {
        case 0:
          return getString(R.string.title_section1).toUpperCase(l);
        case 1:
          return getString(R.string.title_section2).toUpperCase(l);
        case 2:
          return getString(R.string.title_section4).toUpperCase(l);
        case 3:
          return getString(R.string.title_section3).toUpperCase(l);
      }
      return null;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
  }

  /**
   * A dummy fragment representing a section of the app, but that simply
   * displays dummy text.
   */
 /* public class DummySectionFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this fragment.
     */
  /*  public static final String ARG_SECTION_NUMBER = "section_number";

    public DummySectionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
      
      View rootView = null;
      
      if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
        rootView = inflater.inflate(R.layout.running_program_fragment,
            container, false);
        Log.d(TAG, String.valueOf(rootView));
        
        getSupportFragmentManager().findFragmentById(R.id.runningListFragment);
        
        final RunningProgramsListFragment running = (RunningProgramsListFragment)rootView.findViewById(R.id.runningListFragment);
        
        rootView.findViewById(R.id.button_6).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            // TODO Auto-generated method stub
            
          }
        });
        
        
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
/*      }
      else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
        rootView = inflater.inflate(R.layout.program_list_fragment,
            container, false);        
      }
      else {
        rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
          container, false);
      }
      
      return rootView;
    }
  }*/

  
}
