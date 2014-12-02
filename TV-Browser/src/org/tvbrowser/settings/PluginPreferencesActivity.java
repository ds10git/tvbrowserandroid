package org.tvbrowser.settings;

import java.util.List;

import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;

import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.util.Log;

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
      this.startPreferencePanel(header.fragment, header.fragmentArguments, header.titleRes, header.title, null, 0);
  }
  
}
