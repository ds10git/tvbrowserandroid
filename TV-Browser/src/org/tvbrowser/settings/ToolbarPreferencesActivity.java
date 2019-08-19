package org.tvbrowser.settings;

import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public abstract class ToolbarPreferencesActivity extends PreferenceActivity {
  private AppCompatDelegate mDelegate;
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    setTheme(UiUtils.getThemeResourceId(UiUtils.TYPE_THEME_TOOLBAR, PrefUtils.isDarkTheme()));
    getDelegate().installViewFactory();
    getDelegate().onCreate(savedInstanceState);
    super.onCreate(savedInstanceState);
  }
  @Override
  protected void onPostCreate(final Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    getDelegate().onPostCreate(savedInstanceState);
  }

  @NonNull
  @Override
  public MenuInflater getMenuInflater() {
    return getDelegate().getMenuInflater();
  }

  @Override
  public void setContentView(@LayoutRes final int layoutResId) {
    getDelegate().setContentView(layoutResId);
  }

  @Override
  public void setContentView(final View view) {
    getDelegate().setContentView(view);
  }

  @Override
  public void setContentView(final View view, ViewGroup.LayoutParams params) {
    getDelegate().setContentView(view, params);
  }

  @Override
  public void addContentView(final View view, ViewGroup.LayoutParams params) {
    getDelegate().addContentView(view, params);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    getDelegate().onPostResume();
  }

  @Override
  protected void onTitleChanged(final CharSequence title, final int color) {
    super.onTitleChanged(title, color);
    getDelegate().setTitle(title);
  }

  @Override
  public void onConfigurationChanged(final Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    getDelegate().onConfigurationChanged(newConfig);
  }

  @Override
  protected void onStop() {
    getDelegate().onStop();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    getDelegate().onDestroy();
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      // Navigates to the parent activity following the style guide (up action/ up navigation).
      // Simple back navigation is done by the back button (e. g. previous preference screen).
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  final AppCompatDelegate getDelegate() {
    if (mDelegate == null) {
      mDelegate = AppCompatDelegate.create(this, null);
    }
    return mDelegate;
  }
}