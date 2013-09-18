package org.tvbrowser.tvbrowser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;


public class TvBrowser extends FragmentActivity implements
    ActionBar.TabListener {
  private IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
  private static final String TAG = "TVB";
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
  private HashMap<Long,ChannelUpdate> downloadIDs;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  ViewPager mViewPager;
  
  SimpleCursorAdapter adapter;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tv_browser);
    
//test
    // Set up the action bar.
    final ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(
        getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);

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
    
    // Don't allow use of version after date
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2013);
    cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
    cal.set(Calendar.DAY_OF_MONTH, 22);
    
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
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    mToDownloadChannels = -1;
    
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    Set<String> channels = preferences.getStringSet(SettingConstants.SUBSCRIBED_CHANNELS, null);
    
    if(channels == null || channels.isEmpty()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setMessage(R.string.no_channels);
      builder.setPositiveButton(R.string.select_channels, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          selectChannels();
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

  private void calculateMissingEnds() {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION
    };
    
    Cursor c = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, null, null, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
  //  Log.d(TAG, "DATA-COUNT " + c.getCount());
    
    if(c.getCount() > 0) {
      c.moveToFirst();
            
      do {
        long progID = c.getLong(0);
        int channelKey = c.getInt(1);
        long end = c.getLong(4);
        
        c.moveToNext();
        
        if(end == 0) {
          long nextStart = c.getLong(3);
          
          if(c.getInt(1) == channelKey) {
            ContentValues values = new ContentValues();
            values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, nextStart);
            
            int n = getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, progID), values, null, null);
         //   Log.d(TAG, "UPDATED " + channelKey + " end " + new Date(end) + " " + c.getString(2));
          }
        }
      }while(!c.isLast());
    }
    
    c.close();
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
    
    final ArrayList<ChannelSelection> channelSource = new ArrayList<TvBrowser.ChannelSelection>();
    ArrayList<CharSequence> channelNames = new ArrayList<CharSequence>();
    final ArrayList<Integer> selectedChannels = new ArrayList<Integer>();
    
    if(channels.moveToFirst()) {
      int key = channels.getInt(0);
      String name = channels.getString(1);
      
      channelSource.add(new ChannelSelection(key, name));
      channelNames.add(name);
      
      while(channels.moveToNext()) {
        key = channels.getInt(0);
        name = channels.getString(1);
        
        channelSource.add(new ChannelSelection(key, name));
        channelNames.add(name);
      }
    }
    
    channels.close();
    
    if(!channelSource.isEmpty()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.select_channels);
      builder.setMultiChoiceItems(channelNames.toArray(new CharSequence[channelNames.size()]), null, new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
          if(isChecked) {
            selectedChannels.add(which);
          } 
          else if(selectedChannels.contains(which)){
            selectedChannels.remove(new Integer(which));
          }
        }
      });
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {        
        @Override
        public void onClick(DialogInterface dialog, int which) {
          SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
          Editor editor = preferences.edit();
          
          HashSet<String> channelSet = new HashSet<String>();
          
          for(Integer index : selectedChannels) {
            channelSet.add(String.valueOf(channelSource.get(index.intValue()).getKey()));
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
  }
  
  private static class ChannelSelection {
    private String mName;
    private int mKey;
    
    public ChannelSelection(int key, String name) {
      mKey = key;
      mName = name;
    }
    
    public String toString() {
      return mName;
    }
    
    public int getKey() {
      return mKey;
    }
  }
  
  
  
  private void selectChannels() {
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

  private void updateTvData() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    Set<String> channels = preferences.getStringSet(SettingConstants.SUBSCRIBED_CHANNELS, null);
    
    if(channels != null) {
      ContentResolver cr = getContentResolver();
      
      StringBuilder where = new StringBuilder(TvBrowserContentProvider.KEY_ID);
      where.append(" IN (");

      for(String key : channels) {
        where.append(key);
        where.append(", ");
      }
      
      where.delete(where.length()-2,where.length());
      
      where.append(")");
      
   //   Log.d(TAG, where.toString());
      
      ArrayList<ChannelUpdate> downloadList = new ArrayList<ChannelUpdate>();
      
      Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where.toString(), null, TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
      
      if(channelCursor.getCount() > 0) {
        channelCursor.moveToFirst();
        
        int lastGroup = -1;
        String mirrorURL = null;
        String groupId = null;
        Summary summary = null;
        
        do {
          int groupKey = channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
          int channelKey = channelCursor.getInt(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          String timeZone = channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_TIMEZONE));
          
          if(lastGroup != groupKey) {
            Cursor group = cr.query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_GROUPS, groupKey), null, null, null, null);
            
            if(group.getCount() > 0) {
              group.moveToFirst();
              mirrorURL = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_MIRRORS));
              
              if(mirrorURL.contains(";")) {
                mirrorURL = mirrorURL.substring(0, mirrorURL.indexOf(";"));
                
                if(!mirrorURL.endsWith("/")) {
                  mirrorURL += "/";
                }
              }
              
              groupId = group.getString(group.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID));
              
              summary = readSummary(mirrorURL + groupId + "_summary.gz");
            }
            
            group.close();
          }
          
          if(summary != null) {
            ChannelFrame frame = summary.getChannelFrame(channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)));
            
            if(frame != null) {
              Calendar startDate = summary.getStartDate();
              
              Calendar now = Calendar.getInstance();
              now.add(Calendar.DAY_OF_MONTH, -1);

              Calendar to = Calendar.getInstance();
              to.add(Calendar.DAY_OF_MONTH, 3);

              
              for(int i = 0; i < frame.getDayCount(); i++) {
                startDate.add(Calendar.DAY_OF_YEAR, 1);
                
                if(startDate.compareTo(now) >= 0 && startDate.compareTo(to) <= 0) {
                  int[] version = frame.getVersionForDay(i);
                  // load only base files
                  for(int level = 0; level < 1; level++) {
                    if(version[level] > 0) {
                      String month = String.valueOf(startDate.get(Calendar.MONTH)+1);
                      String day = String.valueOf(startDate.get(Calendar.DAY_OF_MONTH));
                      
                      if(month.length() == 1) {
                        month = "0" + month;
                      }
                      
                      if(day.length() == 1) {
                        day = "0" + day;
                      }
                      
                      StringBuilder dateFile = new StringBuilder();
                      dateFile.append(mirrorURL);
                      dateFile.append(startDate.get(Calendar.YEAR));
                      dateFile.append("-");
                      dateFile.append(month);
                      dateFile.append("-");
                      dateFile.append(day);
                      dateFile.append("_");
                      dateFile.append(frame.getCountry());
                      dateFile.append("_");
                      dateFile.append(frame.getChannelID());
                      dateFile.append("_");
                      dateFile.append(SettingConstants.LEVEL_NAMES[level]);
                      dateFile.append("_full.prog.gz");
                                            
                      downloadList.add(new ChannelUpdate(dateFile.toString(), channelKey, timeZone, startDate.getTimeInMillis()));
                    }
                  }
                }
              }
            }
          }
          
          lastGroup = groupKey;
        }while(channelCursor.moveToNext());
        
      }
      
      channelCursor.close();
      
      final DownloadManager download = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
      downloadIDs = new HashMap<Long,ChannelUpdate>();
      
      BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
          long receiveReference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
          
          if(downloadIDs.containsKey(Long.valueOf(receiveReference))) {
            ChannelUpdate update = downloadIDs.remove(Long.valueOf(receiveReference));
            updateData(download,receiveReference, update);
          }
          
          if(downloadIDs.isEmpty()) {
            unregisterReceiver(this);
          }
        };
      };
      
      registerReceiver(receiver, filter);
      
      for(ChannelUpdate data : downloadList) {
        Request request = new Request(Uri.parse(data.getUrl()));
        request.setDestinationInExternalFilesDir(getApplicationContext(), null, data.getUrl().substring(data.getUrl().lastIndexOf("/")));
        //Environment.getExternalStorageDirectory()
        
        long id = download.enqueue(request);
        downloadIDs.put(id,data);
      }
    }
  }
  
  private class ChannelUpdate {
    private String mUrl;
    private long mChannelID;
    private String mTimeZone;
    private long mDate;
    
    public ChannelUpdate(String url, long channelID, String timezone, long date) {
      mUrl = url;
      mChannelID = channelID;
      mTimeZone = timezone;
      mDate = date;
    }
    
    public String getUrl() {
      return mUrl;
    }
    
    public long getChannelID() {
      return mChannelID;
    }
    
    public TimeZone getTimeZone() {
      return TimeZone.getTimeZone(mTimeZone);
    }
    
    public long getDate() {
      return mDate;
    }
  }
  
  private synchronized void updateData(DownloadManager download, long reference, ChannelUpdate update) {
    try {
      File dataFile = new File(getExternalFilesDir(null),update.getUrl().substring(update.getUrl().lastIndexOf("/")));
      
      BufferedInputStream in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(dataFile)));
      
      byte fileVersion = (byte)in.read();
      byte dataVersion = (byte)in.read();
      
      int frameCount = in.read();
      
    //  Log.d(TAG, " FILE VERSION " + fileVersion + " DATA VERSION " + dataVersion + " FRAME COUNT " + frameCount);
      
      for(int i = 0; i < frameCount; i++) {
        // ID of this program frame
        byte frameId = (byte)in.read();
        // number of program fields
        byte fieldCount = (byte)in.read();
        
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID, frameId);
        values.put(TvBrowserContentProvider.DATA_KEY_UNIX_DATE, update.getDate());
        values.put(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID, update.getChannelID());
        
        String where = TvBrowserContentProvider.DATA_KEY_DATE_PROG_ID + " = " + frameId +
            " AND " + TvBrowserContentProvider.DATA_KEY_UNIX_DATE + " = " + update.getDate() +
            " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + update.getChannelID();
        
     //   Log.d(TAG, frameId + " " + new Date(update.getDate()) + " " + update.getChannelID() + " " + update.getUrl() + " FRAME COUNT " + i);
        
        for(byte field = 0; field < fieldCount; field++) {
          byte fieldType = (byte)in.read();
          
          int dataCount = ((in.read() & 0xFF) << 16) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
          
      //    Log.d(TAG, "FIELD TYPE: " + String.valueOf(Integer.toHexString(fieldType)) + " BYTECOUNT " + dataCount);
          
          byte[] data = new byte[dataCount];
          
          /*for(int j = 0; j < dataCount; j++) {
            data[j] = (byte)(in.read() & 0xFF);
          }*/
          in.read(data);
          
     //     Log.d(TAG, "DATA: " + new String(data));
          
          switch(fieldType) {
            case 1: {
                            int startTime = getIntForBytes(data);
                       //     Log.d(TAG, "startTimeValue " + startTime);
                            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            utc.setTimeInMillis(update.getDate());
                     //       Log.d(TAG, "utc" + String.valueOf(utc.getTime()));
                            
                            Calendar cal = Calendar.getInstance(update.getTimeZone());
                            cal.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
                            cal.set(Calendar.MONTH, utc.get(Calendar.MONTH));
                            cal.set(Calendar.YEAR, utc.get(Calendar.YEAR));
                            
                            cal.set(Calendar.HOUR_OF_DAY, startTime / 60);
                            cal.set(Calendar.MINUTE, startTime % 60);
                            cal.set(Calendar.SECOND, 0);
                            
                         //   Log.d(TAG, "cal " + String.valueOf(cal.getTime()));
                            
                            values.put(TvBrowserContentProvider.DATA_KEY_STARTTIME, cal.getTimeInMillis());
                         }break;
            case 2: {
              int endTime = getIntForBytes(data);
              
              Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
              utc.setTimeInMillis(update.getDate());
              
              Calendar cal = Calendar.getInstance(update.getTimeZone());
              cal.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
              cal.set(Calendar.MONTH, utc.get(Calendar.MONTH));
              cal.set(Calendar.YEAR, utc.get(Calendar.YEAR));
              
              cal.set(Calendar.HOUR_OF_DAY, endTime / 60);
              cal.set(Calendar.MINUTE, endTime % 60);
              cal.set(Calendar.SECOND, 0);
              
              Long o = values.getAsLong(TvBrowserContentProvider.DATA_KEY_STARTTIME);
              
              if(o instanceof Long) {
                if(o > cal.getTimeInMillis()) {
                  cal.add(Calendar.DAY_OF_YEAR, 1);
                }
              }
              
              values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, cal.getTimeInMillis());
           }break;
            case 3: values.put(TvBrowserContentProvider.DATA_KEY_TITLE, new String(data));break;
            case 4: values.put(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL, new String(data));break;
            case 5: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE, new String(data));break;
            case 6: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL, new String(data));break;
            case 7: values.put(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION, new String(data));break;
            case 8: values.put(TvBrowserContentProvider.DATA_KEY_DESCRIPTION, new String(data));break;
            case 0xA: values.put(TvBrowserContentProvider.DATA_KEY_ACTORS, new String(data));break;
            case 0xB: values.put(TvBrowserContentProvider.DATA_KEY_REGIE, new String(data));break;
            case 0xC: values.put(TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO, new String(data));break;
            case 0xD: values.put(TvBrowserContentProvider.DATA_KEY_CATEGORIES, getIntForBytes(data));break;
            case 0xE: values.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT, getIntForBytes(data));break;
            case 0xF: values.put(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK, new String(data));break;
            case 0x10: values.put(TvBrowserContentProvider.DATA_KEY_GENRE, new String(data));break;
            case 0x11: values.put(TvBrowserContentProvider.DATA_KEY_ORIGIN, new String(data));break;
            case 0x12: values.put(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME, getIntForBytes(data));break;
            case 0x13: values.put(TvBrowserContentProvider.DATA_KEY_VPS, getIntForBytes(data));break;
            case 0x14: values.put(TvBrowserContentProvider.DATA_KEY_SCRIPT, new String(data));break;
            case 0x15: values.put(TvBrowserContentProvider.DATA_KEY_REPETITION_FROM, new String(data));break;
            case 0x16: values.put(TvBrowserContentProvider.DATA_KEY_MUSIC, new String(data));break;
            case 0x17: values.put(TvBrowserContentProvider.DATA_KEY_MODERATION, new String(data));break;
            case 0x18: values.put(TvBrowserContentProvider.DATA_KEY_YEAR, getIntForBytes(data));break;
            case 0x19: values.put(TvBrowserContentProvider.DATA_KEY_REPETITION_ON, new String(data));break;
            case 0x1A: values.put(TvBrowserContentProvider.DATA_KEY_PICTURE, data);break;
            case 0x1B: values.put(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT, new String(data));break;
            case 0x1C: values.put(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION, new String(data));break;
            case 0x1D: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER, getIntForBytes(data));break;
            case 0x1E: values.put(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT, getIntForBytes(data));break;
            case 0x1F: values.put(TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER, getIntForBytes(data));break;
            case 0x20: values.put(TvBrowserContentProvider.DATA_KEY_PRODUCER, new String(data));break;
            case 0x21: values.put(TvBrowserContentProvider.DATA_KEY_CAMERA, new String(data));break;
            case 0x22: values.put(TvBrowserContentProvider.DATA_KEY_CUT, new String(data));break;
            case 0x23: values.put(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS, new String(data));break;
            case 0x24: values.put(TvBrowserContentProvider.DATA_KEY_RATING, getIntForBytes(data));break;
            case 0x25: values.put(TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM, new String(data));break;
            case 0x26: values.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING, new String(data));break;
            case 0x27: values.put(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, getIntForBytes(data));break;
            case 0x28: values.put(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, new String(data));break;
            case 0x29: values.put(TvBrowserContentProvider.DATA_KEY_SERIES, new String(data));break;
          }
          
          data = null;
        }
        
        if(values.get(TvBrowserContentProvider.DATA_KEY_ENDTIME) == null) {
          values.put(TvBrowserContentProvider.DATA_KEY_ENDTIME, 0);
        }
        /*
        for(String key : values.keySet()) {
          Log.d(TAG, key + " " + values.get(key));
        }*/
        
        Cursor test = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, null, where, null, null);
        
        if(test.getCount() > 0) {
          // program known update it
          getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_DATA, values, where, null);
        }
        else {
          
          Uri inserted = getContentResolver().insert(TvBrowserContentProvider.CONTENT_URI_DATA, values);
        //  Log.d(TAG, "INSERT: " + String.valueOf(inserted));
        }
        
        values.clear();
        values = null;
        
        test.close();
      }
      
      in.close();
      dataFile.delete();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      Log.d(TAG, "UPDATE_DATA", e);
    }
    
    if(downloadIDs != null && downloadIDs.isEmpty()) {
      calculateMissingEnds();
    }
  }
  
  
  
  private int getIntForBytes(byte[] value) {
    int count = value.length - 1;
    
    int result = 0;
    
    for(byte b : value) {
     // Log.d(TAG,"BYTE " + ((int)b) + " SHIFT " + (count * 8));
      
      result = result | ((((int)b) & 0xFF) << (count * 8));
      
      if(count == 0) {
        byte test1 = 0x1c;
        int test =  0xFF | (b & 0xFF);
     //   Log.d(TAG,"TEST " + test + " " +test1);
      }
      
      count--;
    }
    
    return result;
  }
  
  private Summary readSummary(final String summaryurl) {
    final Summary summary = new Summary();
    
    Thread load = new Thread() {
      public void run() {
        URL url;
        try {
          url = new URL(summaryurl);
        //  Log.d(TAG, summaryurl);
          URLConnection connection;
          connection = url.openConnection();
          
          HttpURLConnection httpConnection = (HttpURLConnection)connection;
          if(httpConnection != null) {
          int responseCode = httpConnection.getResponseCode();
          
          if(responseCode == HttpURLConnection.HTTP_OK) {
          //  Log.d(TAG, "HTTP_OK");
            InputStream in = httpConnection.getInputStream();
            
            if("gzip".equalsIgnoreCase(httpConnection.getHeaderField("Content-Encoding"))) {
              in = new GZIPInputStream(in);
            }
            
            // Buffer the input
        //    in = new BufferedInputStream(in);
            
         /*   byte loaded[] = new byte[0];
            
            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1)
            {
              byte[] newLoad = new byte[loaded.length + count];
              
              System.arraycopy(loaded, 0, newLoad, 0, loaded.length);
              System.arraycopy(data, 0, newLoad, loaded.length, count);
              
              loaded = newLoad;
            }
            
            Log.d(TAG, String.valueOf(loaded.length));
            
            in.close();
            
            in = new GZIPInputStream(new ByteArrayInputStream(loaded));*/
            
            //read file version
            summary.setVersion(in.read());
            
            long daysSince1970 = ((in.read() & 0xFF) << 16 ) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            
            summary.setStartDaySince1970(daysSince1970);
            
            summary.setLevels(in.read());
            
            int frameCount = (in.read() & 0xFF << 8) | (in.read() & 0xFF);
            //Log.d(TAG, " days since 1970 " + summary.getStartDaySince1970());
            
            for(int i = 0; i < frameCount; i++) {
              int byteCount = in.read();
              
              byte[] value = new byte[byteCount];
              
              in.read(value);
              
              String country = new String(value);
              
              byteCount = in.read();
              
              value = new byte[byteCount];
              
              in.read(value);
              
              String channelID = new String(value);
             // Log.d(TAG, country + " " + channelID);
              
              int dayCount = in.read();
              
              ChannelFrame frame = new ChannelFrame(country, channelID, dayCount);
              
              
             // Log.d(TAG, String.valueOf(dayCount));
              for(int day = 0; day < dayCount; day++) {
                int[] values = new int[summary.getLevels()];
                
                for(int j = 0; j < values.length; j++) {
                  values[j] = in.read();
                }
                
                frame.add(day, values);
              }
              
              summary.addChannelFrame(frame);
            }
            
          }
          }
        } catch (Exception e) {
          // TODO Auto-generated catch block
          Log.d(TAG, "SUMMARY", e);
        }
      }
    };
    load.start();
    try {
      load.join();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return summary;
  }
  
  private static class Summary {
    private int mVersion;
    private long mStartDaySince1970;
    private int mLevels;

    private ArrayList<ChannelFrame> mFrameList;
    
    public Summary() {
      mFrameList = new ArrayList<TvBrowser.ChannelFrame>();
    }
    
    public void setVersion(int version) {
      mVersion = version;
    }
    
    public void setStartDaySince1970(long days) {
      mStartDaySince1970 = days;
    }
    
    public void setLevels(int levels) {
      mLevels = levels;
    }
    
    public void addChannelFrame(ChannelFrame frame) {
      mFrameList.add(frame);
    }
    
    public int getLevels() {
      return mLevels;
    }
    
    public long getStartDaySince1970() {
      return mStartDaySince1970;
    }
    
    public Calendar getStartDate() {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      cal.setTimeInMillis(mStartDaySince1970 * 24 * 60 * 60000);
      
      return cal;
    }
    
    public ChannelFrame getChannelFrame(String channelID) {
  //    Log.d(TAG, "CHANNELID " + channelID);
      for(ChannelFrame frame : mFrameList) {
        if(frame.mChannelID.equals(channelID)) {
          return frame;
        }
      }
      
      return null;
    }
  }
  
  private static class ChannelFrame {
    private String mCountry;
    private String mChannelID;
    private int mDayCount;
    
    private HashMap<Integer,int[]> mLevelVersions;
    
    public ChannelFrame(String country, String channelID, int dayCount) {
      mCountry = country;
      mChannelID = channelID;
      mDayCount = dayCount;
      
      mLevelVersions = new HashMap<Integer, int[]>();
    }
    
    public void add(int day, int[] levelVersions) {
      mLevelVersions.put(day, levelVersions);
    }
    
    public int[] getVersionForDay(int day) {
      return mLevelVersions.get(new Integer(day));
    }
    
    public int getDayCount() {
      return mDayCount;
    }
    
    public String getCountry() {
      return mCountry;
    }
    
    public String getChannelID() {
      return mChannelID;
    }
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_update: updateTvData();break;
      case R.id.action_settings: selectChannels();break;
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.tv_browser, menu);
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

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a DummySectionFragment (defined as a static inner class
      // below) with the page number as its lone argument.
      Fragment fragment = new DummySectionFragment();
      Bundle args = new Bundle();
      args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public int getCount() {
      // Show 3 total pages.
      return 2;
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
          return getString(R.string.title_section3).toUpperCase(l);
      }
      return null;
    }
  }

  /**
   * A dummy fragment representing a section of the app, but that simply
   * displays dummy text.
   */
  public static class DummySectionFragment extends Fragment {
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
      }
      else {
        rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
          container, false);
      }
      
      return rootView;
    }
  }

  
}
