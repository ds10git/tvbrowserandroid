package org.tvbrowser.tvbrowser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.ActionBar;
import android.app.AlertDialog;
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Contacts.SettingsColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TvBrowser extends FragmentActivity implements
    ActionBar.TabListener {
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
  
  private boolean updateRunning;
  private boolean selectingChannels;
  private ActionBar actionBar;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  ViewPager mViewPager;
  
  SimpleCursorAdapter adapter;
  
  Handler handler;
  
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
    
    handler = new Handler();
    
    if(savedInstanceState != null) {
      updateRunning = savedInstanceState.getBoolean("updateRunning", false);
      selectingChannels = savedInstanceState.getBoolean("selectionChannels", false);
    }
    
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
            
            Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(position);
            
            if(fragment instanceof ProgramTableFragment) {
              ((ProgramTableFragment)fragment).scrollToNow();
            }
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
    cal.set(Calendar.DAY_OF_MONTH, 27);
        
    new Thread() {
      public void run() {
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.DAY_OF_YEAR, -2);
        
        getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.DATA_KEY_STARTTIME + " < " + cal2.getTimeInMillis(), null);
      }
    }.start();
    
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
    
    Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION + " = 1", null, null);
    
    if(!selectingChannels && (channels == null || channels.getCount() == 0)) {
      selectingChannels = true;
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      builder.setMessage(R.string.no_channels);
      builder.setPositiveButton(R.string.select_channels, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          selectChannels(false);
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
    
    channels.close();
  }
  
  private void showChannelSelection() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.search_title);
    builder.setMessage(R.string.synchronize_text);
    
    builder.setPositiveButton(R.string.synchronize_ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        final SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
        
        if(pref.getString(SettingConstants.USER_NAME, null) == null || pref.getString(SettingConstants.USER_PASSWORD, null) == null) {
          showUserSetting(true);
        }
        else {
          syncronizeChannels();
        }
      }
    });
    builder.setNegativeButton(R.string.synchronize_cancel, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        showChannelSelectionInternal();
      }
    });
    
    AlertDialog d = builder.create();
    
    d.show();

    ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
  }
  
  private void syncronizeChannels() {
    new Thread() {
      public void run() {
        URL documentUrl;
        try {
          documentUrl = new URL("http://android.tvbrowser.org/hurtzAndroidTvbChannels2.php");
          //documentUrl = new URL("http://android.tvbrowser.org/webtest/android-tvb/data/scripts//hurtzAndroidTvbChannels.php");
          URLConnection connection = documentUrl.openConnection();
          
          SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
          
          String car = pref.getString(SettingConstants.USER_NAME, null);
          String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);
          Log.d("dateinfo", car + " " + bicycle);
          if(car != null && bicycle != null) {
            String userpass = car + ":" + bicycle;
            String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
            
            connection.setRequestProperty ("Authorization", basicAuth);
            
            BufferedReader read = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),"UTF-8"));
            
            String line = null;
            
            int sort = 1;
            
            boolean somethingSynchonized = false;
            
            while((line = read.readLine()) != null) {
              if(line.trim().length() > 0) {
                Log.d("sync", line);
                if(line.contains(":")) {
                  String[] parts = line.split(":");
                  
                  if(parts[0].equals("1")) {
                    String dataService = "EPG_FREE";
                    
                    String where = " ( " + TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID + " = '" + dataService + "' ) AND ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = '" + parts[1] + "' ) ";
                    
                    Cursor group = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_GROUPS, null, where, null, null);
                    
                    if(group.moveToFirst()) {
                      int groupId = group.getInt(group.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                      
                      where = " ( " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID + " = " + groupId + " ) AND ( " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = '" + parts[2] + "' ) ";
                      
                      ContentValues values = new ContentValues();
                      
                      values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 1);
                      values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, sort);
                      
                      int changed = getContentResolver().update(TvBrowserContentProvider.CONTENT_URI_CHANNELS, values, where, null);
                      
                      if(changed > 0) {
                        somethingSynchonized = true;
                      }
                      Log.d("sync", String.valueOf(changed));
                    }
                    
                    group.close();
                    
                    sort++;
                  }
                }
              }
            }
            
            if(somethingSynchonized) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(getApplicationContext(), R.string.synchronize_done, Toast.LENGTH_LONG).show();
                  checkTermsAccepted();
                }
              });
            }
            else {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(getApplicationContext(), R.string.synchronize_error, Toast.LENGTH_LONG).show();
                  showChannelSelectionInternal();
                }
              });
            }
          }
          else {
            showChannelSelectionInternal();
          }
        } catch (Exception e) {
          showChannelSelectionInternal();
          Log.d("dateinfo", "",e);
        }
        
        selectingChannels = false;
      }
    }.start();
    
  }
  
  private void showChannelSelectionInternal() {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION
        };
    
    ContentResolver cr = getContentResolver();
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, null, null, null);
    
    final ArrayList<ChannelSelection> channelSource = new ArrayList<TvBrowser.ChannelSelection>();
    ArrayList<CharSequence> channelNames = new ArrayList<CharSequence>();
    
    final boolean[] wasSelected = new boolean[channels.getCount()];
    final boolean[] currentlySelected = new boolean[channels.getCount()];
    final boolean[] toUnselect = new boolean[channels.getCount()];
    
    int i = 1;
    
    if(channels.moveToFirst()) {
      do {
        int key = channels.getInt(0);
        String name = channels.getString(1);
        int selection = channels.getInt(2);
        
        wasSelected[i-1] = currentlySelected[i-1] = (selection == 1);
        toUnselect[i-1] = false;
        
        channelSource.add(new ChannelSelection(key, name, i));
        channelNames.add(name);
        
        i++;
      }while(channels.moveToNext());
    }
    
    channels.close();
    
    if(!channelSource.isEmpty()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.select_channels);
      builder.setMultiChoiceItems(channelNames.toArray(new CharSequence[channelNames.size()]), currentlySelected, new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
          toUnselect[which] = !isChecked;
        }
      });
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {        
        @Override
        public void onClick(DialogInterface dialog, int which) {    
          boolean somethingSelected = false;
          
          for(int i = 0; i < currentlySelected.length; i++) {
            if(currentlySelected[i]) {
              int key = channelSource.get(i).getKey();
              
              ContentValues values = new ContentValues();
              
              values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 1);
              
              getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, key), values, null, null);
              
              if(!wasSelected[i]) {
                somethingSelected = true;
              }
            }
          }
          
          for(int i = 0; i < toUnselect.length; i++) {
            if(toUnselect[i]) {
              int key = channelSource.get(i).getKey();
              
              ContentValues values = new ContentValues();
              
              values.put(TvBrowserContentProvider.CHANNEL_KEY_SELECTION, 0);
              
              getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, key), values, null, null);

              getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + key, null);
              getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + key, null);
            }
          }
          
          if(somethingSelected) {
            checkTermsAccepted();
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
      return (mSortNumber == 0 ? "-" : mSortNumber) + ". " + mName;
    }
    
    public String getName() {
      return mName;
    }
  }
  
  private void runChannelDownload() {
    Intent updateChannels = new Intent(TvBrowser.this, TvDataUpdateService.class);
    updateChannels.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.CHANNEL_TYPE);
    
    final IntentFilter filter = new IntentFilter(SettingConstants.CHANNEL_DOWNLOAD_COMPLETE);
    
    BroadcastReceiver receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(TvBrowser.this).unregisterReceiver(this);
        showChannelSelection();
      }
    };
    
    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    
    startService(updateChannels);
  }
  
  private void selectChannels(boolean loadAgain) {
    Cursor channels = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, null, null, null);
    
    if(loadAgain || channels.getCount() < 1) {
      if(isOnline()) {
        runChannelDownload();
      }
      else {
        showNoInternetConnection(new Runnable() {
          @Override
          public void run() {
            checkTermsAccepted();
          }
        });
      }
    }
    else {
      showChannelSelection();
    }
    
    channels.close();
  }
  
  private void sortChannels() {
    ContentResolver cr = getContentResolver();
    
    StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
    where.append(" = 1");
    
    LinearLayout main = (LinearLayout)getLayoutInflater().inflate(R.layout.channel_sort_list, null);
    
    ListView channelSort = (ListView)main.findViewById(R.id.channel_sort);
    
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION
        };
    
    Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    final ArrayList<ChannelSelection> channelSource = new ArrayList<TvBrowser.ChannelSelection>();
      
    if(channels.getCount() > 0) {
      if(channels.moveToFirst()) {
        do {
          int key = channels.getInt(0);
          String name = channels.getString(1);
          
          int order = 0;
          
          if(!channels.isNull(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER))) {
            order = channels.getInt(channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));
          }
                    
          channelSource.add(new ChannelSelection(key, name, order));
        }while(channels.moveToNext());
        
        
      }
      
      channels.close();
      
      channelSort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, int position,
            long id) {
          AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
          
          LinearLayout numberSelection = (LinearLayout)getLayoutInflater().inflate(R.layout.sort_number_selection, null);
          
          final NumberPicker number = (NumberPicker)numberSelection.findViewById(R.id.sort_picker);
          number.setMinValue(1);
          number.setMaxValue(channelSource.size());
          
          builder.setView(numberSelection);
          
          final ChannelSelection selection = channelSource.get(position);
          
          TextView name = (TextView)numberSelection.findViewById(R.id.sort_picker_channel_name);
          name.setText(selection.getName());
          
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
      builder.setView(main);
      
      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          for(ChannelSelection selection : channelSource) {
            ContentValues values = new ContentValues();
            values.put(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER, selection.getSortNumber());
            
            getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, selection.getKey()), values, null, null);
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

  private void updateTvData() {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    LinearLayout dataDownload = (LinearLayout)getLayoutInflater().inflate(R.layout.download_selection, null);
    
    final Spinner days = (Spinner)dataDownload.findViewById(R.id.download_days);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.download_selections, android.R.layout.simple_spinner_item);

    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    days.setAdapter(adapter);
    
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    days.setSelection(pref.getInt(TvDataUpdateService.DAYS_TO_LOAD, 2));
    
    builder.setTitle(R.string.download_data);
    builder.setView(dataDownload);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Intent startDownload = new Intent(TvBrowser.this, TvDataUpdateService.class);
        startDownload.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.TV_DATA_TYPE);
        startDownload.putExtra(TvDataUpdateService.DAYS_TO_LOAD, SettingConstants.DOWNLOAD_DAYS[days.getSelectedItemPosition()]);
        
        Editor settings = pref.edit();
        settings.putInt(TvDataUpdateService.DAYS_TO_LOAD, days.getSelectedItemPosition());
        settings.commit();
        
        startService(startDownload);
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    builder.show();
  }
  
  private void showUserSetting(final boolean syncChannels) {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    RelativeLayout username_password_setup = (RelativeLayout)getLayoutInflater().inflate(R.layout.username_password_setup, null);
            
    final SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
    
    final EditText userName = (EditText)username_password_setup.findViewById(R.id.username_entry);
    final EditText password = (EditText)username_password_setup.findViewById(R.id.password_entry);
    
    userName.setText(pref.getString(SettingConstants.USER_NAME, ""));
    password.setText(pref.getString(SettingConstants.USER_PASSWORD, ""));
    
    builder.setView(username_password_setup);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Editor edit = pref.edit();
        
        edit.putString(SettingConstants.USER_NAME, userName.getText().toString().trim());
        edit.putString(SettingConstants.USER_PASSWORD, password.getText().toString().trim());
        
        edit.commit();
        
        if(syncChannels) {
          syncronizeChannels();
        }
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if(syncChannels) {
          showChannelSelectionInternal();
        }
      }
    });
    builder.show();
  }
  
  private void showNoInternetConnection(final Runnable callback) {
    AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
    
    builder.setTitle(R.string.no_network);
    builder.setMessage(R.string.no_network_info);
    
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if(callback != null && isOnline()) {
          callback.run();
        }
      }
    });
    
    builder.show();
  }
  
  private void checkTermsAccepted() {
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    
    String terms = pref.getString(SettingConstants.TERMS_ACCEPTED, "");
    
    if(terms.contains("EPG_FREE")) {
      updateTvData();
    }
    else {
      AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
      
      builder.setTitle(R.string.terms_of_use);
      builder.setMessage(R.string.terms_of_use_text);
      
      builder.setPositiveButton(R.string.terms_of_use_accept, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Editor edit = pref.edit();
          
          edit.putString(SettingConstants.TERMS_ACCEPTED, "EPG_FREE");
          
          edit.commit();
          
          updateTvData();
        }
      });
      
      builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          
        }
      });
      
      AlertDialog d = builder.create();
            
      d.show();

      ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_username_password:
      {  
        showUserSetting(false);
    }
      break;
      case R.id.action_update:
        if(isOnline()) {
          checkTermsAccepted();
        }
        else {
          showNoInternetConnection(null);
        }
        break;
      case R.id.action_about: 
        AlertDialog.Builder builder = new AlertDialog.Builder(TvBrowser.this);
        
        RelativeLayout about = (RelativeLayout)getLayoutInflater().inflate(R.layout.about, null);
        
        try {
          PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
          TextView version = (TextView)about.findViewById(R.id.version);
          version.setText(pInfo.versionName);
        } catch (NameNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        TextView androidVersion = (TextView)about.findViewById(R.id.android_version);
        androidVersion.setText(Build.VERSION.RELEASE);
        
        builder.setTitle(R.string.action_about);
        builder.setView(about);
        
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            
          }
        });
        builder.show();
        break;
      case R.id.action_load_channels_again: selectChannels(true);break;
      case R.id.action_select_channels: selectChannels(false);break;
      case R.id.action_sort_channels: sortChannels();break;
      case R.id.action_load_full_data:
      {
        item.setChecked(!item.isChecked());
        Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        edit.putBoolean(SettingConstants.LOAD_FULL_DATA, item.isChecked());
        edit.commit();
      }
        break;
      case R.id.action_delete_all_data: getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.KEY_ID + " > 0", null);
                                        getContentResolver().delete(TvBrowserContentProvider.CONTENT_URI_DATA_VERSION, TvBrowserContentProvider.KEY_ID + " > 0", null);
                                        break;
      case R.id.action_program_table_activated: item.setChecked(!item.isChecked());
                                                Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                                                edit.putBoolean(SettingConstants.PROG_TABLE_ACTIVATED, item.isChecked());
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
    
    item.setChecked(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingConstants.PROG_TABLE_ACTIVATED, true));
    
    item = menu.findItem(R.id.action_load_full_data);
    
    item.setChecked(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingConstants.LOAD_FULL_DATA, false));
    
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
      if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingConstants.PROG_TABLE_ACTIVATED, true)) {
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
}
