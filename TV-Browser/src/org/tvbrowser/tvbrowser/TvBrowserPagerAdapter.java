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

package org.tvbrowser.tvbrowser;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.SparseArrayCompat;
import android.view.ViewGroup;

import java.util.Locale;

import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one
 * of the sections/tabs/pages.
 *
 * TODO
 * Should probably use a {@link FragmentStatePagerAdapter} to reduce memory footprint, and
 * to avoid manual fragment state management via <code>registeredFragments</code> array.
 */
final class TvBrowserPagerAdapter extends FragmentPagerAdapter {

  private final SparseArrayCompat<Fragment> registeredFragments = new SparseArrayCompat<>();
  private final TvBrowser tvBrowser;

  TvBrowserPagerAdapter(final TvBrowser tvBrowser, final FragmentManager fragmentManager) {
    super(fragmentManager);
    this.tvBrowser = tvBrowser;
  }

  @Override
  public synchronized Fragment getItem(final int position) {
    Fragment fragment = registeredFragments.get(position);

    if(fragment == null) {
      switch (position) {
        case 0:
          if (IOUtils.isDatabaseAccessible(tvBrowser.getApplicationContext())) {
            fragment = new FragmentProgramsListRunning();
            if (TvBrowser.START_TIME != Integer.MIN_VALUE) {
              ((FragmentProgramsListRunning) fragment).setStartTime(TvBrowser.START_TIME + 1);
              TvBrowser.START_TIME = Integer.MIN_VALUE;
            }
          } else {
            fragment = new Fragment();
          }
          break;
        case 1:
          fragment = FragmentProgramsList.getInstance(
                  tvBrowser.getProgramListScrollTime(),
                  tvBrowser.getProgramListScrollEndTime(),
                  tvBrowser.getProgramListChannelId());
          tvBrowser.setProgramListChannelId(FragmentProgramsList.NO_CHANNEL_SELECTION_ID);
          tvBrowser.setProgramListScrollTime(-1);
          tvBrowser.setProgramListScrollEndTime(-1);
          break;
        case 2:
          fragment = new FragmentFavorites();
          break;
        case 3:
          fragment = new FragmentProgramTable();
          break;
      }
    }
    else if(fragment.isAdded()) {
      fragment = null;
    }

    return fragment;
  }

  @Override
  public int getCount() {
    final int count;
    if (IOUtils.isDatabaseAccessible(tvBrowser.getApplicationContext())) {
      if(PrefUtils.getBooleanValue(R.string.PROG_TABLE_ACTIVATED, R.bool.prog_table_activated_default)) {
        count = 4;
      } else {
        count = 3;
      }
    } else {
      count = 1;
    }
    return count;
  }

  @Override
  public CharSequence getPageTitle(final int position) {
    final @StringRes int resId;
    switch (position) {
      case 0:
        resId = IOUtils.isDatabaseAccessible(tvBrowser.getApplicationContext())
                ? R.string.title_running_programs : R.string.title_database_not_available;
        break;
      case 1:
        resId = R.string.title_programs_list;
        break;
      case 2:
        resId = R.string.title_favorites;
        break;
      case 3:
        resId = R.string.title_program_table;
        break;
      default:
        resId = 0;
        break;
    }
    return resId == 0 ? "" : tvBrowser.getString(resId).toUpperCase(Locale.getDefault());
  }

  @NonNull
  @Override
  public synchronized Object instantiateItem(final ViewGroup container, final int position) {
      final Fragment fragment = (Fragment) super.instantiateItem(container, position);
      registeredFragments.put(position, fragment);
      return fragment;
  }

  @Override
  public void destroyItem(final ViewGroup container, final int position, final Object object) {
      registeredFragments.remove(position);
      super.destroyItem(container, position, object);
  }

  Fragment getRegisteredFragment(final int position) {
      return registeredFragments.get(position);
  }
}