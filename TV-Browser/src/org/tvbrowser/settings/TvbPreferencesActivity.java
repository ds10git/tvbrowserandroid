/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
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

import java.util.List;

import org.tvbrowser.tvbrowser.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

public class TvbPreferencesActivity extends ToolbarPreferencesActivity {
	/**
	 * Vulnerability fix as mentioned here:
	 * http://securityintelligence.com/wp-content/uploads/2013/12/android-collapses-into-fragments.pdf
	 */
	@Override
	@TargetApi(Build.VERSION_CODES.KITKAT)
	protected boolean isValidFragment(final String fragmentName) {
	  return TvbPreferenceFragment.class.getCanonicalName().equals(fragmentName) ||
	    super.isValidFragment(fragmentName);
	}

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getDelegate().getSupportActionBar().setDisplayShowHomeEnabled(true);
    getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
    public void onBuildHeaders(List<Header> target) {
      loadHeadersFromResource(R.xml.tvbrowser_preferences_header, target);
      
      SharedPreferences pref = getSharedPreferences("transportation", Context.MODE_PRIVATE);
      
      if(pref.getString(SettingConstants.USER_NAME, "").trim().length() == 0 || pref.getString(SettingConstants.USER_PASSWORD, "").trim().length() == 0) {
        String syncCategory = getString(R.string.category_sync);
        
        for(int i = target.size()-1; i >= 0; i--) {
          Header head = target.get(i);
                    
          if(head.fragmentArguments != null && syncCategory.equals(head.fragmentArguments.getString(getString(R.string.pref_category_key)))) {
            target.remove(i);
            break;
          }
        }
      }
  }
}
