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

import java.util.Collections;
import java.util.List;

import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;

/**
 * The preferences activity for the plugins.
 * 
 * @author René Mach
 */
public class PluginPreferencesActivity extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if(SettingConstants.IS_DARK_THEME) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    super.onCreate(savedInstanceState);
  }
  
  @Override
  public void onBuildHeaders(List<Header> target) {
    Log.d("info23","hier " + PluginHandler.PLUGIN_LIST);
    
    
    if(PluginHandler.PLUGIN_LIST != null) {
      Log.d("info23","hier1");  
      for(int i = 0; i < PluginHandler.PLUGIN_LIST.size(); i++) {
        PluginServiceConnection pluginConnection  = PluginHandler.PLUGIN_LIST.get(i);
        try {
          Log.d("info23","hier2 " + pluginConnection + " " + pluginConnection.isConnected());
        if(pluginConnection.isConnected()) {
          Header header = new Header();
        
          Plugin plugin = pluginConnection.getPlugin();
          
          
            header.title = plugin.getName() + " " + plugin.getVersion();
            
            if(plugin.getAuthor() != null) {
              header.summary = getString(R.string.pref_plugins_author) + " " + plugin.getAuthor();
            }
            
            header.fragment = PluginPreferencesFragment.class.getName();
            
            Bundle b = new Bundle();
            b.putString("category", header.title.toString());
            b.putInt("PluginIndex", Integer.valueOf(i));
            b.putString("pluginId", pluginConnection.getId());
            header.fragmentArguments = b;
            
            target.add(header);
          }
        } catch (RemoteException e) {
          Log.d("info23", "", e);
        }
      }
    }
  }
  
  @Override
  public void onHeaderClick(Header header, int position) {
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
