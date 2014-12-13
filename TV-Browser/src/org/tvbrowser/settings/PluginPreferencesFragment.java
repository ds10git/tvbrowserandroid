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

import java.util.concurrent.atomic.AtomicReference;

import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.view.InfoPreference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.util.Log;

/**
 * The preferences fragment for the plugins.
 * 
 * @author René Mach
 */
public class PluginPreferencesFragment extends PreferenceFragment {
  private String mPluginId;
  
  @Override
  public void onDetach() {
    // TODO Auto-generated method stub
    super.onDetach();
    Log.d("info24", "cccc");
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if (savedInstanceState == null) {
      mPluginId = getArguments().getString("pluginId");
    }
    else {
        // Orientation Change
        mPluginId = savedInstanceState.getString("pluginId");
    }
    
    // Load the preferences from an XML resource
    PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
    // add prefrences using preferenceScreen.addPreference()
    this.setPreferenceScreen(preferenceScreen);
    preferenceScreen.setTitle("ccccc");
    
    final PluginServiceConnection pluginConnection = PluginHandler.getConnectionForId(mPluginId);
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    if(pluginConnection != null && pluginConnection.isConnected()) {
      Log.d("info24", ""+pluginConnection.getId());
      final CheckBoxPreference activated =  new CheckBoxPreference(getActivity());
      activated.setTitle(R.string.pref_activated);
      activated.setKey(mPluginId+"_ACTIVATED");
      activated.setChecked(pref.getBoolean(pluginConnection.getId()+"_ACTIVATED", true));
      
      preferenceScreen.addPreference(activated);
      
      try {
        String description = pluginConnection.getPluginDescription();
        
        if(description != null) {
          InfoPreference descriptionPref = new InfoPreference(getActivity());
          descriptionPref.setTitle(R.string.pref_plugins_description);
          descriptionPref.setSummary(description);
          preferenceScreen.addPreference(descriptionPref);
        }
        
        final AtomicReference<Preference> startSetupRef = new AtomicReference<Preference>(null);
        if(pluginConnection != null && pluginConnection.isConnected() && pluginConnection.getPlugin().hasPreferences()) {
          final Preference startSetup = new Preference(getActivity());
          startSetup.setTitle(R.string.pref_open);
          startSetup.setKey(mPluginId);
          startSetup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              StackTraceElement[] els = Thread.currentThread().getStackTrace();
              
              for(StackTraceElement el : els) {
                Log.d("info24", el.toString());
              }
              
              try {
                if(pluginConnection != null && pluginConnection.isConnected()) {
                  pluginConnection.getPlugin().openPreferences(PluginHandler.getPluginManager().getSubscribedChannels());
                }
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
            if(pluginConnection != null && pluginConnection.isConnected()) {
              if(startSetupRef.get() != null) {
                startSetupRef.get().setEnabled((Boolean)newValue);
              }
              
              if((Boolean)newValue) {
                pluginConnection.callOnActivation();
              }
              else {
                pluginConnection.callOnDeactivation();
              }
            }
            
            return true;
          }
        });
        
        String license = pluginConnection.getPluginLicense();
        
        if(license != null) {
          InfoPreference licensePref = new InfoPreference(getActivity());
          licensePref.setTitle(R.string.pref_plugins_license);
          licensePref.setSummary(Html.fromHtml(license));
          
          preferenceScreen.addPreference(licensePref);
        }
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  
}
