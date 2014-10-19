/*
 * TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
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

import java.util.Comparator;
import java.util.Map;

import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * An activity to edit the available filters.
 * <p>
 * @author René Mach
 */
public class ActivityFilterListEdit extends Activity implements ChannelFilter {
  private ListView mFilterList;
  private ArrayAdapter<ChannelFilterImpl> mFilterListAdapter;
  private static final Comparator<ChannelFilterImpl> CHANNEL_FILTER_COMPARATOR = new Comparator<ChannelFilterImpl>() {
    @Override
    public int compare(ChannelFilterImpl lhs, ChannelFilterImpl rhs) {
      return lhs.getName().compareToIgnoreCase(rhs.getName());
    }
  };
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if(SettingConstants.IS_DARK_THEME) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    mFilterListAdapter = new ArrayAdapter<ActivityFilterListEdit.ChannelFilterImpl>(ActivityFilterListEdit.this, android.R.layout.simple_list_item_1);
    
    setContentView(R.layout.activity_filter_list_edit);
    
    mFilterList = (ListView)findViewById(R.id.activity_edit_filter_list_list);
    mFilterList.setAdapter(mFilterListAdapter);
    
    SharedPreferences pref = getSharedPreferences(SettingConstants.FILTER_PREFERENCES, Context.MODE_PRIVATE);
    
    Map<String,?> filterValues = pref.getAll();
    
    for(String key : filterValues.keySet()) {
      Object values = filterValues.get(key);
      
      if(key.startsWith("filter.") && values instanceof String && values != null) {
        ChannelFilterImpl channelFilter = new ChannelFilterImpl(ActivityFilterListEdit.this, key, (String)values);
        
        mFilterListAdapter.add(channelFilter);
      }
    }
    
    mFilterListAdapter.sort(CHANNEL_FILTER_COMPARATOR);
    
    mFilterListAdapter.notifyDataSetChanged();
    
    registerForContextMenu(mFilterList);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_filter_list_edit_menu, menu);
    
    return true;
  }
  
  public void ok(View view) {
    finish();
  }
  
  private ChannelFilterImpl mCurrentFilter = null;
  private boolean mIsNewFilter = false;
  
  public void addFilter(MenuItem item) {
    mCurrentFilter = new ChannelFilterImpl(ActivityFilterListEdit.this);
    mIsNewFilter = true;
    
    UiUtils.showChannelFilterSelection(ActivityFilterListEdit.this, this, mFilterList);
  }
  
  private static final class ChannelFilterImpl  {
    private String mId;
    private String mName;
    private Context mContext;
    private int[] mFilteredChannelIds;
    
    public ChannelFilterImpl(Context context, String id, String values) {
      mContext = context;
      mId = id;
      
      int index = values.indexOf("##_##");
      
      mName = values.substring(0,index);
      
      String[] ids = values.substring(index+5).split(";");
      
      mFilteredChannelIds = new int[ids.length];
      
      for(int i = 0; i < ids.length; i++) {
        mFilteredChannelIds[i] = Integer.parseInt(ids[i]);
      }
    }
    
    public ChannelFilterImpl(Context context) {
      mContext = context;
      mId = "filter." + System.currentTimeMillis();
      mName = "";
      mFilteredChannelIds = null;
    }
    
    public int[] getFilteredChannelIds() {
      return mFilteredChannelIds;
    }
    
    @Override
    public String toString() {
      return getName();
    }
    
    public String getName() {
      return mName;
    }
    
    public String getId() {
      return mId;
    }

    public void setFilterValues(String name, int[] filteredChannelIds) {
      mName = name;
      mFilteredChannelIds = filteredChannelIds;
      
      Editor edit = mContext.getSharedPreferences(SettingConstants.FILTER_PREFERENCES, Context.MODE_PRIVATE).edit();
      
      StringBuilder value = new StringBuilder(name);
      value.append("##_##");
      
      for(int i = 0; i < filteredChannelIds.length-1; i++) {
        value.append(filteredChannelIds[i]).append(";");
      }
      
      if(filteredChannelIds.length > 0) {
        value.append(filteredChannelIds[filteredChannelIds.length-1]);
      }
      
      edit.putString(mId, value.toString());
      edit.commit();
    }
  }

  @Override
  public int[] getFilteredChannelIds() {
    return mCurrentFilter.getFilteredChannelIds();
  }

  @Override
  public String getName() {
    return mCurrentFilter.getName();
  }

  @Override
  public void setFilterValues(String name, int[] filteredChannelIds) {
    if(name != null && filteredChannelIds != null) {
      mCurrentFilter.setFilterValues(name, filteredChannelIds);
      
      if(mIsNewFilter) {
        mFilterListAdapter.add(mCurrentFilter);
        
        mIsNewFilter = false;
        mCurrentFilter = null;
      }
      
      mFilterListAdapter.sort(CHANNEL_FILTER_COMPARATOR);
      mFilterListAdapter.notifyDataSetChanged();
    }
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    mCurrentFilter = (ChannelFilterImpl)mFilterList.getItemAtPosition(((AdapterView.AdapterContextMenuInfo)menuInfo).position);
    mIsNewFilter = false;
    
    getMenuInflater().inflate(R.menu.activity_edit_filter_list_context, menu);
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(mCurrentFilter != null) {
      if(item.getItemId() == R.id.activity_edit_filter_list_action_edit) {
        UiUtils.showChannelFilterSelection(ActivityFilterListEdit.this, this, mFilterList);
      }
      else if(item.getItemId() == R.id.activity_edit_filter_list_action_delete) {
        mFilterListAdapter.remove(mCurrentFilter);
        mFilterListAdapter.notifyDataSetChanged();
        
        Editor edit = getSharedPreferences(SettingConstants.FILTER_PREFERENCES, Context.MODE_PRIVATE).edit();
        edit.remove(mCurrentFilter.getId());
        edit.commit();
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ActivityFilterListEdit.this);
        
        String test = pref.getString(getString(R.string.CURRENT_FILTER_ID), SettingConstants.ALL_FILTER_ID);
        
        if(test.equals(mCurrentFilter.getId())) {
          edit = pref.edit();
          edit.putString(getString(R.string.CURRENT_FILTER_ID), SettingConstants.ALL_FILTER_ID);
          edit.commit();
        }
      }
      
      return true;
    }
    
    return false;
  }
}
