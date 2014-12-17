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
package org.tvbrowser.settings;

import java.util.List;

import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * The preferences activity for the plugins.
 * 
 * @author René Mach
 */
public class PluginPreferencesActivity extends PreferenceActivity {
  private static final String HEADER_SELECTION_EXTRA = "HEADER_SELECTION";
  
  private static PluginServiceConnection[] PLUGIN_SERVICE_CONNECTIONS;
  private static PluginManager PLUGIN_MANAGER;
  
  private static int LAST_POS = 0;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if(SettingConstants.IS_DARK_THEME) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    super.onCreate(savedInstanceState);
  }
    
  @Override
  protected void onResume() {
    super.onResume();
    
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {        
        if(getListAdapter() != null && LAST_POS < getListAdapter().getCount()) {
          Object header = getListAdapter().getItem(LAST_POS);
          
          if(header != null) {
            if(isMultiPane()) {
              switchToHeader(((Header)header).fragment, ((Header)header).fragmentArguments);
              onHeaderClick((Header)header, LAST_POS);
            }
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
    
    Log.d("info23", "onBuildHeaders");
    if(PLUGIN_SERVICE_CONNECTIONS != null) {
      for(PluginServiceConnection pluginConnection : PLUGIN_SERVICE_CONNECTIONS) {
        int id = 1;
        
        if(pluginConnection != null) {
          Header header = new Header();
          
          header.title = pluginConnection.getPluginName() + " " + pluginConnection.getPluginVersion();
          
          if(pluginConnection.getPluginAuthor() != null) {
            header.summary = getString(R.string.pref_plugins_author) + " " + pluginConnection.getPluginAuthor();
          }
          
          header.fragment = PluginPreferencesFragment.class.getName();
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
      startPreferencePanel(header.fragment, header.fragmentArguments, header.titleRes, header.title, null, 0);
    }
  }
  
  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Override
  protected boolean isValidFragment(String fragmentName) {
    return fragmentName.equals(PluginPreferencesFragment.class.getCanonicalName()) || super.isValidFragment(fragmentName);
  }
}
