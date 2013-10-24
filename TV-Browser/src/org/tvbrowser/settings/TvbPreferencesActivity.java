package org.tvbrowser.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TvbPreferencesActivity extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Display the fragment as the main content.
      getFragmentManager().beginTransaction()
              .replace(android.R.id.content, new TvbPreferenceFragment())
              .commit();
  }
}
