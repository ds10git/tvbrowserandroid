/*
 * TV-Browser for Android
 * Copyright (C) 2018 René Mach (rene@tvbrowser.org)
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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.MenuItem;
import android.widget.FrameLayout;

import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

/**
 * Activity for plugin preferences.
 *
 * @author René Mach
 * @since 0.6.5
 */
public class ActivityPluginFragment extends AppCompatActivity {
  public static final String EXTRA_HEADER = "HEADER_PREFERENCES";

  private static final int CONTENT_VIEW_ID = 12345678;

  @Override
  protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
    resid = UiUtils.getThemeResourceId(UiUtils.TYPE_THEME_TOOLBAR, PrefUtils.isDarkTheme());

    super.onApplyThemeResource(theme, resid, first);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    PreferenceActivity.Header header = null;

    if (intent != null && intent.hasExtra(EXTRA_HEADER)) {
      header = intent.getParcelableExtra(EXTRA_HEADER);
      getDelegate().getSupportActionBar().setTitle(header.title);
      getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getDelegate().getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    FrameLayout frame = new FrameLayout(this);
    frame.setId(CONTENT_VIEW_ID);
    setContentView(frame, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

    if (header != null) {
      final Fragment f = Fragment.instantiate(this, header.fragment, header.fragmentArguments);

      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.add(CONTENT_VIEW_ID, f).commit();
    } else {
      finish();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}