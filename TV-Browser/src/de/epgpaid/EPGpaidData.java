/*
 * EPGpaid data: A supplement data plugin for TV-Browser.
 * Copyright: (c) 2018 René Mach
 */
package de.epgpaid;

import android.content.SharedPreferences;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.PrefUtils;

public final class EPGpaidData {
  public static final int TYPE_DATE_FROM = 1;
  public static final int TYPE_DATE_UNTIL = 2;

  public static void setDateValue(final int type, final long date) {
    final SharedPreferences.Editor edit = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL).edit();

    switch (type) {
      case TYPE_DATE_FROM:PrefUtils.putLong(edit, R.string.PREF_EPGPAID_ACCESS_FROM, date);break;
      case TYPE_DATE_UNTIL:PrefUtils.putLong(edit, R.string.PREF_EPGPAID_ACCESS_UNTIL, date);break;
    }

    edit.apply();
  }
}
