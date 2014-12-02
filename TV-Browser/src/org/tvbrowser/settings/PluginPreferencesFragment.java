package org.tvbrowser.settings;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class PluginPreferencesFragment extends PreferenceFragment {
  private String mPluginId;
  private String mCategory;
  private int mIndex;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if (savedInstanceState == null) {
      mCategory = getArguments().getString("category");
      mIndex = getArguments().getInt("PluginIndex");
      mPluginId = getArguments().getString("pluginId");
    }
    else {
        // Orientation Change
        mCategory = savedInstanceState.getString("category");
        mIndex = savedInstanceState.getInt("PluginIndex");
        mPluginId = savedInstanceState.getString("pluginId");
    }
    

    // Load the preferences from an XML resource
    PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
    // add prefrences using preferenceScreen.addPreference()
    this.setPreferenceScreen(preferenceScreen);
    
    final PluginServiceConnection pluginConnection = PluginHandler.PLUGIN_LIST.get(mIndex);
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    if(pluginConnection.isConnected()) {
      Log.d("info23", ""+pluginConnection.getId());
      final CheckBoxPreference activated =  new CheckBoxPreference(getActivity());
      activated.setTitle(R.string.pref_activated);
      activated.setKey(mPluginId+"_ACTIVATED");
      activated.setChecked(pref.getBoolean(pluginConnection.getId()+"_ACTIVATED", true));
      
      preferenceScreen.addPreference(activated);
      
      try {
        final AtomicReference<Preference> startSetupRef = new AtomicReference<Preference>(null);
        if(pluginConnection.getPlugin().hasPreferences()) {
          final Preference startSetup = new Preference(getActivity());
          startSetup.setTitle(R.string.pref_open);
          startSetup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              ArrayList<Channel> channelList = new ArrayList<Channel>();
              
              Cursor channels = getActivity().getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.KEY_ID);
              
              if(channels != null) {
                channels.moveToPosition(-1);
                
                int keyColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
                int iconColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
                
                while(channels.moveToNext()) {
                  channelList.add(new Channel(channels.getInt(keyColumn), channels.getString(nameColumn), channels.getBlob(iconColumn)));
                }
              }
              try {
                pluginConnection.getPlugin().openPreferences(channelList);
              } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              
              return true;
            }
          });
          
          preferenceScreen.addPreference(startSetup);
          
          startSetup.setEnabled(activated.isChecked());
          startSetupRef.set(startSetup);
        }
        
        activated.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference, Object newValue) {
            if(startSetupRef.get() != null) {
              startSetupRef.get().setEnabled((Boolean)newValue);
            }
            
            if((Boolean)newValue) {
              pluginConnection.callOnActivation();
            }
            else {
              pluginConnection.callOnDeactivation();
            }
            
            return true;
          }
        });
        
        String license = pluginConnection.getPlugin().getLicense();
        
        if(license != null) {
          Preference licensePref = new Preference(getActivity());
          licensePref.setTitle(R.string.pref_plugins_license);
          licensePref.setSummary(license);
          
          preferenceScreen.addPreference(licensePref);
        }
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
