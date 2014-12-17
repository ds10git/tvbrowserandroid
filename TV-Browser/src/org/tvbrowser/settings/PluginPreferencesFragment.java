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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.IOUtils;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.view.InfoPreference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.transition.ArcMotion;
import android.util.Log;
import android.widget.TextView;

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
    
    final PluginServiceConnection pluginConnection = ((PluginPreferencesActivity)getActivity()).getServiceConnectionWithId(mPluginId);
    
  //  pluginConnection.unbindPlugin(getActivity().getApplicationContext());
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    
    if(pluginConnection != null) {
      Log.d("info24", ""+pluginConnection.getId());
      final CheckBoxPreference activated =  new CheckBoxPreference(getActivity());
      activated.setTitle(R.string.pref_activated);
      activated.setKey(mPluginId+"_ACTIVATED");
      activated.setChecked(pref.getBoolean(pluginConnection.getId()+"_ACTIVATED", true));
      
      preferenceScreen.addPreference(activated);
      
      String description = pluginConnection.getPluginDescription();
      
      if(description != null) {
        InfoPreference descriptionPref = new InfoPreference(getActivity());
        descriptionPref.setTitle(R.string.pref_plugins_description);
        descriptionPref.setSummary(description);
        preferenceScreen.addPreference(descriptionPref);
      }
      
      final Handler handler = new Handler();
      final Context context = getActivity();
      
      final AtomicReference<Preference> startSetupRef = new AtomicReference<Preference>(null);
      if(pluginConnection != null && pluginConnection.hasPreferences()) {
        final Preference startSetup = new Preference(getActivity());
        startSetup.setTitle(R.string.pref_open);
        startSetup.setKey(mPluginId);
        startSetup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            String logtext = "onPreferenceClick: pluginConntection: " + (pluginConnection != null) +" ";
            
            try {
              
              
              if(pluginConnection != null ) {
                Plugin plugin = pluginConnection.getPlugin();
                
                logtext += "PLUGIN " + plugin + " isBound " + pluginConnection.isBound(getActivity().getApplicationContext());
                //Log.d("info24", );
                if(plugin != null && pluginConnection.isBound(getActivity().getApplicationContext())) {
                  logtext += " openPreferences " + IOUtils.getChannelList(getActivity());
                  plugin.openPreferences(IOUtils.getChannelList(getActivity()));
                }
                else {
                  final Context bindContext = getActivity(); 
                  logtext += " bindPlugin ";
                  if(pluginConnection.bindPlugin(bindContext, null)) {
                    logtext += " BOUND PLUGIN ";
                    Log.d("info23", logtext);
                    pluginConnection.getPlugin().onActivation(((PluginPreferencesActivity)getActivity()).getPluginManager());
                    pluginConnection.getPlugin().openPreferences(IOUtils.getChannelList(bindContext));
                    pluginConnection.callOnDeactivation();
                    logtext += " UNBIND PLUGIN ";
                    pluginConnection.unbindPlugin(bindContext);
                  }
                  else {
                    logtext += " COULD NOT BIND TO PLUGIN ";
                  }
                }
                
                
              }
            } catch (Throwable e) {
              // TODO Auto-generated catch block
              Log.d("info24","",e);
              
              logtext += e.getMessage();
            }
            /*
            final String message = logtext;
            
            handler.post(new Runnable() {
              @Override
              public void run() {
                TextView view = new TextView(getActivity());
                view.setText(message);
                view.setTextIsSelectable(true);
                view.setTextSize(18f);
                
                new AlertDialog.Builder(context).setView(view).setPositiveButton(android.R.string.ok, null).show();
              }
            });*/
            
            return true;
          }
        });
        
        preferenceScreen.addPreference(startSetup);
        
        startSetup.setEnabled(activated.isChecked());
        startSetupRef.set(startSetup);
      }
      
      activated.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
          if(pluginConnection != null) {
            final AtomicReference<Context> mBindContextRef = new AtomicReference<Context>(null); 
            
            Runnable runnable = new Runnable() {
              @Override
              public void run() {
                if(startSetupRef.get() != null) {
                  startSetupRef.get().setEnabled((Boolean)newValue);
                }
                
                if((Boolean)newValue) {
                  try {
                    pluginConnection.getPlugin().onActivation(((PluginPreferencesActivity)getActivity()).getPluginManager());
                  } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                }
                else {
                  pluginConnection.callOnDeactivation();
                }
                
                if(mBindContextRef.get() != null) {
                  pluginConnection.callOnDeactivation();
                  pluginConnection.unbindPlugin(mBindContextRef.get() );
                }
              }
            };
            
            Plugin plugin = pluginConnection.getPlugin();
            
            if(plugin == null) {
              mBindContextRef.set(getActivity());
              if(pluginConnection.bindPlugin(mBindContextRef.get(), null)) {
                runnable.run();
              }
            }
            else {
              runnable.run();
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
    }
  }
  
  
}
