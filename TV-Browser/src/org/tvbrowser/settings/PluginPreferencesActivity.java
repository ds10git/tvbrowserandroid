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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;

import java.util.List;

/**
 * The preferences activity for the plugins.
 * 
 * @author René Mach
 */
public class PluginPreferencesActivity extends ToolbarPreferencesActivity {
  static final int REQUEST_CODE_UNINSTALL = 3;
  private static PluginServiceConnection[] PLUGIN_SERVICE_CONNECTIONS;
  private static PluginManager PLUGIN_MANAGER;
  
  private static int LAST_POS = 0;

  private static PluginPreferencesActivity INSTANCE = null;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    Intent intent = getIntent();
    
    if(intent != null && intent.hasExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)) {
      //getDelegate().getSupportActionBar().setTitle(intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS).getString("category"));
    }

    INSTANCE = this;
  }

  @Override
  protected void onDestroy() {
    INSTANCE = null;

    super.onDestroy();
  }

  static PluginPreferencesActivity getInstance() {
    return INSTANCE;
  }

  @Override
  public void onBackPressed() {
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    new Handler().postDelayed(() -> {
      if(getListAdapter() != null && LAST_POS < getListAdapter().getCount()) {
        Object header = getListAdapter().getItem(LAST_POS);

        if(header != null) {
          if(isMultiPane()) {
            switchToHeader(((Header)header).fragment, ((Header)header).fragmentArguments);
            onHeaderClick((Header)header, LAST_POS);
          }
        }
      }
    }, 500);
  }
    
  public static void clearPlugins() {
    LAST_POS = 0;
    PLUGIN_SERVICE_CONNECTIONS = null;
    PLUGIN_MANAGER = null;
  }
  
  @Override
  public void onBuildHeaders(List<Header> target) {
    if(PluginHandler.hasPlugins()) {
      PLUGIN_MANAGER = PluginHandler.getPluginManager();
      PLUGIN_SERVICE_CONNECTIONS = PluginHandler.getAvailablePlugins();
    }
    else {
      finish();
    }
    
    if(PLUGIN_SERVICE_CONNECTIONS != null) {
      for(PluginServiceConnection pluginConnection : PLUGIN_SERVICE_CONNECTIONS) {
        int id = 1;
        
        if(pluginConnection != null) {
          Header header = new Header();

          header.title = pluginConnection.getPluginName() + " " + pluginConnection.getPluginVersion();
          
          if(pluginConnection.getPluginAuthor() != null) {
            header.summary = getString(R.string.pref_plugins_author) + " " + pluginConnection.getPluginAuthor();
          }
          
          header.fragment = PluginPreferencesFragment.class.getCanonicalName();
          header.id = id++;
          
          Bundle b = new Bundle();
          b.putString("category", header.title.toString());
          b.putString("pluginId", pluginConnection.getId());
          header.fragmentArguments = b;
          
          target.add(header);
        }
      }
    }
  }
  
  PluginManager getPluginManager() {
    return PLUGIN_MANAGER;
  }
  
  PluginServiceConnection getServiceConnectionWithId(String id) {
    PluginServiceConnection result = null;
    
    if(PLUGIN_SERVICE_CONNECTIONS != null) {
      for(PluginServiceConnection connection : PLUGIN_SERVICE_CONNECTIONS) {
        if(connection.getId().equals(id)) {
          result = connection;
        }
      }
    }
    
    return result;
  }

  @Override
  public void onHeaderClick(Header header, int position) {
    LAST_POS = position;

    if(isMultiPane()) {
      switchToHeader(header);
    }
    else {
      Intent startActivity = new Intent(PluginPreferencesActivity.this, ActivityPluginFragment.class);
      startActivity.putExtra(ActivityPluginFragment.EXTRA_HEADER, header);
      startActivity(startActivity);
    }
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Override
  protected boolean isValidFragment(String fragmentName) {
    return fragmentName.equals(PluginPreferencesFragment.class.getCanonicalName()) || super.isValidFragment(fragmentName);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == REQUEST_CODE_UNINSTALL) {
      if (resultCode == RESULT_OK) {
        invalidateHeaders();
      }
    }
    else {
      super.onActivityResult(requestCode,resultCode,data);
    }
  }

  @Override
  public boolean onIsMultiPane() {
    return false;
  }

  @Override
  public View onCreateView(String name, Context context, AttributeSet attrs)
  {
    return super.onCreateView(name, context, attrs);
  }
}
