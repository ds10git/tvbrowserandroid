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

import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.view.InfoPreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;

/**
 * The preferences fragment for the plugins.
 * 
 * @author René Mach
 */
public class PluginPreferencesFragment extends PreferenceFragment {

  @Override
  public void onDetach() {
    super.onDetach();
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final String mPluginId;
    if (savedInstanceState == null) {
      mPluginId = getArguments().getString("pluginId");
    } else {
      // Orientation Change
      mPluginId = savedInstanceState.getString("pluginId");
    }

    // Load the preferences from an XML resource
    PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
    // add preferences using preferenceScreen.addPreference()
    this.setPreferenceScreen(preferenceScreen);
    preferenceScreen.setTitle("ccccc");

    final PluginPreferencesActivity activity = PluginPreferencesActivity.getInstance();

    if (activity != null) {
      final PluginManager pluginManager = activity.getPluginManager();
      final PluginServiceConnection pluginConnection = PluginPreferencesActivity.getInstance().getServiceConnectionWithId(mPluginId);

      //  pluginConnection.unbindPlugin(getActivity().getApplicationContext());

      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

      if (pluginConnection != null) {
        final CheckBoxPreference activated = new CheckBoxPreference(getActivity());
        activated.setTitle(R.string.pref_activated);
        activated.setKey(mPluginId + "_ACTIVATED");
        activated.setChecked(pref.getBoolean(pluginConnection.getId() + "_ACTIVATED", true));

        preferenceScreen.addPreference(activated);

        String description = pluginConnection.getPluginDescription();

        if (description != null) {
          InfoPreference descriptionPref = new InfoPreference(getActivity());
          descriptionPref.setTitle(R.string.pref_plugins_description);
          descriptionPref.setSummary(description);
          preferenceScreen.addPreference(descriptionPref);
        }

        final AtomicReference<Preference> startSetupRef = new AtomicReference<Preference>(null);
        if (pluginConnection != null && pluginConnection.hasPreferences()) {
          final Preference startSetup = new Preference(getActivity());
          startSetup.setTitle(R.string.pref_open);
          startSetup.setKey(mPluginId);
          startSetup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              try {
                if (pluginConnection != null) {
                  Plugin plugin = pluginConnection.getPlugin();

                  if (plugin != null && pluginConnection.isBound(getActivity().getApplicationContext())) {
                    plugin.openPreferences(IOUtils.getChannelList(getActivity()));
                  } else {
                    final Context bindContext = getActivity();

                    if (pluginConnection.bindPlugin(bindContext, null)) {
                      pluginConnection.getPlugin().onActivation(pluginManager);
                      pluginConnection.getPlugin().openPreferences(IOUtils.getChannelList(bindContext));
                      pluginConnection.callOnDeactivation();

                      pluginConnection.unbindPlugin(bindContext);
                    }
                  }


                }
              } catch (Throwable e) {
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
          public boolean onPreferenceChange(Preference preference, final Object newValue) {
            if (pluginConnection != null) {
              final AtomicReference<Context> mBindContextRef = new AtomicReference<Context>(null);

              Runnable runnable = new Runnable() {
                @Override
                public void run() {
                  if (startSetupRef.get() != null) {
                    startSetupRef.get().setEnabled((Boolean) newValue);
                  }

                  if ((Boolean) newValue) {
                    try {
                      pluginConnection.getPlugin().onActivation(pluginManager);
                    } catch (RemoteException e) {
                      e.printStackTrace();
                    }
                  } else {
                    pluginConnection.callOnDeactivation();
                  }

                  if (mBindContextRef.get() != null) {
                    pluginConnection.callOnDeactivation();
                    pluginConnection.unbindPlugin(mBindContextRef.get());
                  }
                }
              };

              Plugin plugin = pluginConnection.getPlugin();

              if (plugin == null) {
                mBindContextRef.set(getActivity());
                if (pluginConnection.bindPlugin(mBindContextRef.get(), null)) {
                  runnable.run();
                }
              } else {
                runnable.run();
              }
            }

            return true;
          }
        });

        String license = pluginConnection.getPluginLicense();

        if (license != null) {
          InfoPreference licensePref = new InfoPreference(getActivity());
          licensePref.setTitle(R.string.pref_plugins_license);
          licensePref.setSummary(Html.fromHtml(license));

          preferenceScreen.addPreference(licensePref);
        }
      }
    }
  }
}
