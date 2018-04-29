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
package org.tvbrowser.filter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.content.SharedPreferences;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
public class ActivityFilterListEdit extends AppCompatActivity {
  private ListView mFilterList;
  private ArrayAdapter<FilterValues> mFilterListAdapter;
  
  @Override
  protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
    resid = UiUtils.getThemeResourceId();
    
    super.onApplyThemeResource(theme, resid, first);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    mFilterListAdapter = new ArrayAdapter<FilterValues>(ActivityFilterListEdit.this, android.R.layout.simple_list_item_1);
    
    setContentView(R.layout.activity_filter_list_edit);
    
    mFilterList = (ListView)findViewById(R.id.activity_edit_filter_list_list);
    mFilterList.setAdapter(mFilterListAdapter);
    
    SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, ActivityFilterListEdit.this);
    
    Map<String,?> filterValues = pref.getAll();
    Set<String> keySet = filterValues.keySet();
    ArrayList<String> keysToRemove = new ArrayList<>();

    for(final String key : keySet) {
      Object values = filterValues.get(key);
      
      if(key.contains("filter.") && values instanceof String && values != null) {
        FilterValues filter = FilterValues.load(key, (String)values);
        
        if(filter != null) {
          mFilterListAdapter.add(filter);
        }
        else {
          keysToRemove.add(key);
        }
      }
    }

    if(!keysToRemove.isEmpty()) {
      final SharedPreferences.Editor edit = pref.edit();

      for(final String key : keysToRemove) {
        edit.remove(key);
      }

      edit.apply();
    }
    
    mFilterListAdapter.sort(FilterValues.COMPARATOR_FILTER_VALUES);
    
    mFilterListAdapter.notifyDataSetChanged();
    
    registerForContextMenu(mFilterList);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_filter_list_edit_menu, menu);
    
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.activity_edit_filter_list_action_channel_add: addFilter(new FilterValuesChannels());break;
      case R.id.activity_edit_filter_list_action_categories_add: addFilter(new FilterValuesCategories());break;
      case R.id.activity_edit_filter_list_action_keyword_add: addFilter(new FilterValuesKeyword());break;
    }
    
    return true;
  }
  
  public void ok(View view) {
    finish();
  }
  
  private FilterValues mCurrentFilter = null;
  
  public void addFilter(FilterValues filter) {
    mCurrentFilter = filter;
    
    mCurrentFilter.edit(ActivityFilterListEdit.this, new Runnable() {
      @Override
      public void run() {
        mFilterListAdapter.add(mCurrentFilter);
        
        mCurrentFilter.save(getApplicationContext());
        mCurrentFilter = null;
        mFilterListAdapter.sort(FilterValues.COMPARATOR_FILTER_VALUES);
        mFilterListAdapter.notifyDataSetChanged();
      }
    }, mFilterList);
  }
    
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    mCurrentFilter = (FilterValues)mFilterList.getItemAtPosition(((AdapterView.AdapterContextMenuInfo)menuInfo).position);
    
    getMenuInflater().inflate(R.menu.activity_edit_filter_list_context, menu);
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if(mCurrentFilter != null) {
      if(item.getItemId() == R.id.activity_edit_filter_list_action_edit) {
        mCurrentFilter.edit(ActivityFilterListEdit.this, new Runnable() {
          @Override
          public void run() {
            mCurrentFilter.save(getApplicationContext());
            
            mCurrentFilter = null;
            mFilterListAdapter.sort(FilterValues.COMPARATOR_FILTER_VALUES);
            mFilterListAdapter.notifyDataSetChanged();
          }
        }, mFilterList);      
      }
      else if(item.getItemId() == R.id.activity_edit_filter_list_action_delete) {
        mFilterListAdapter.remove(mCurrentFilter);
        mFilterListAdapter.notifyDataSetChanged();
        
        FilterValues.deleteFilter(getApplicationContext(), mCurrentFilter);
                
        mCurrentFilter = null;
      }
      
      return true;
    }
    
    return false;
  }
}
