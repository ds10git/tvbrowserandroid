/*
 * TV-Browser for Android
 * Copyright (C) 2015 René Mach (rene@tvbrowser.org)
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

import org.tvbrowser.tvbrowser.R;

import android.content.Context;
import android.preference.ListPreference;
import androidx.appcompat.app.AlertDialog;
import android.util.AttributeSet;

public class DirectoryPreference extends ListPreference {
  public DirectoryPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
    
  @Override
  protected void onClick() {
    if(getSummary().equals(getContext().getString(R.string.pref_database_selection_unavailable))) {
      new AlertDialog.Builder(getContext()).setTitle(R.string.warning_title).setMessage(R.string.pref_database_selection_warning_unavailable).setPositiveButton(R.string.pref_database_selection_warning_unavailable_ok, (dialog, which) -> DirectoryPreference.super.onClick()).setNegativeButton(android.R.string.cancel, null).setCancelable(false).show();
    }
    else {
      super.onClick();
    }
  }
  
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    final String oldValue = getValue();
    
    super.onDialogClosed(positiveResult);
    
    if(positiveResult) {
      String newValue = getValue();
      
      if(!newValue.equals(getContext().getString(org.tvbrowser.tvbrowser.R.string.pref_database_path_default))) {
        new AlertDialog.Builder(getContext()).setTitle(R.string.warning_title).setMessage(R.string.pref_database_selection_warning_move).setPositiveButton(R.string.pref_database_selection_warning_move_ok, null)
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> setValue(oldValue)).setCancelable(false).show();
      }
    }
  }
}
