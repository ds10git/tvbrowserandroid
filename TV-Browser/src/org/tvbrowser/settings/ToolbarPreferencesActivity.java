package org.tvbrowser.settings;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.UiUtils;

import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ToolbarPreferencesActivity extends PreferenceActivity {
  protected Toolbar mToolBar;
  
  @Override
  protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
    resid = UiUtils.getThemeResourceId();
    
    super.onApplyThemeResource(theme, resid, first);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTheme(UiUtils.getThemeResourceId());
    
    ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
    View content = root.getChildAt(0);
    LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_pref, null);

    root.removeAllViews();
    toolbarContainer.addView(content);
    root.addView(toolbarContainer);

    mToolBar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
    
    mToolBar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
    mToolBar.setTitle(getTitle());
    mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
    mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    });
  }
  }
