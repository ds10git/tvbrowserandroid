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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.text.Spannable;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

class ProgramListViewBinderAndClickHandler implements SimpleCursorAdapter.ViewBinder{
  private final Activity mActivity;
  private final int mDefaultTextColor;
  private final ShowDateInterface mDateShowInterface;
  private final float mZoom;
  private final Handler mHandler;
  
  public ProgramListViewBinderAndClickHandler(Activity act, ShowDateInterface showDateInterface, Handler handler) {
    mActivity = act;
    mDefaultTextColor = new TextView(mActivity).getTextColors().getDefaultColor();
    mDateShowInterface = showDateInterface;
    mZoom = mActivity.getResources().getDisplayMetrics().density;
    mHandler = handler;
  }

  @Override
  public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
    try {
      boolean showPicture = PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_LISTS, R.bool.show_pictures_in_lists_default);
      boolean showGenre = PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_LISTS, R.bool.show_genre_in_lists_default);
      boolean showEpisode = PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_LISTS, R.bool.show_episode_in_lists_default);
      boolean showInfo = PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_LISTS, R.bool.show_info_in_lists_default);
      boolean showOrderNumber = PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_LISTS, R.bool.show_sort_number_in_lists_default);
      boolean showEndTime = PrefUtils.getBooleanValue(R.string.PREF_PROGRAM_LISTS_SHOW_END_TIME, R.bool.pref_program_lists_show_end_time_default);
      
      String logoNamePref = PrefUtils.getStringValue(R.string.CHANNEL_LOGO_NAME_PROGRAM_LISTS, R.string.channel_logo_name_program_lists_default);
      
      boolean showChannelName = logoNamePref.equals("0") || logoNamePref.equals("2");
      boolean showChannelLogo = logoNamePref.equals("0") || logoNamePref.equals("1");
      boolean showBigChannelLogo = logoNamePref.equals("3");

      if(!cursor.isClosed()) {
        long endTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));

        if (view instanceof TextView) {
          if (endTime < System.currentTimeMillis()) {
            ((TextView) view).setTextColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, mActivity));
          } else {
            ((TextView) view).setTextColor(mDefaultTextColor);
          }
        }
        if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)) {
          TextView title = ((ViewGroup) view.getParent()).findViewById(R.id.titleLabelPL);
          String titleValue = cursor.getString(columnIndex);
          title.setText(ProgramUtils.getMarkIcons(mActivity, cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)), titleValue));

          return true;
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
          TextView until = ((ViewGroup) view.getParent()).findViewById(R.id.untilLabelPL);

          if (showEndTime) {
            TextView text = (TextView) view;
            text.setText(DateFormat.getTimeFormat(mActivity).format(new Date(endTime)));
            text.setVisibility(View.VISIBLE);

            String test = until.getText().toString();

            if (!mDateShowInterface.showDate()) {
              if (test.startsWith(",")) {
                until.setText(test.substring(2));
              }
            } else if (!test.startsWith(",")) {
              until.setText(", " + test);
            }

            until.setVisibility(View.VISIBLE);
          } else {
            view.setVisibility(View.GONE);
            until.setVisibility(View.GONE);
          }

          if (endTime < System.currentTimeMillis()) {
            until.setTextColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, mActivity));
          } else {
            until.setTextColor(mDefaultTextColor);
          }

          return true;
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_UNIX_DATE)) {
          TextView date = ((ViewGroup) view.getParent()).findViewById(R.id.startDayLabelPL);

          if (mDateShowInterface.showDate()) {
            UiUtils.formatDayView(mActivity, cursor, view, R.id.startDayLabelPL);

            if (endTime < System.currentTimeMillis()) {
              date.setTextColor(UiUtils.getColor(UiUtils.EXPIRED_COLOR_KEY, mActivity));
            } else {
              date.setTextColor(mDefaultTextColor);
            }

            date.setVisibility(View.VISIBLE);
            view.setVisibility(View.VISIBLE);
          } else {
            UiUtils.handleMarkings(mActivity, cursor, ((RelativeLayout) view.getParent()), null);
            date.setVisibility(View.GONE);
            ((TextView) view).setText("");

            if (!showEndTime) {
              view.setVisibility(View.GONE);
            }
          }

          return true;
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) {
          boolean show = true;

          if (mDateShowInterface instanceof ShowChannelInterface) {
            show = ((ShowChannelInterface) mDateShowInterface).showChannel();
          }

          if (show) {
            TextView text = (TextView) view;
            ((ViewGroup) view.getParent()).setVisibility(View.VISIBLE);

            String name = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            String shortName = SettingConstants.SHORT_CHANNEL_NAMES.get(name);
            String number = null;

            if (shortName != null) {
              name = shortName;
            }

            if (showOrderNumber) {
              number = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER));

              if (number == null) {
                number = "0";
              }

              number += ".";

              name = number + " " + name;
            }

            int logoIndex = cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);

            Drawable logo = null;

            if ((showBigChannelLogo || showChannelLogo) && logoIndex >= 0) {
              int key = cursor.getInt(logoIndex);

              if (!showBigChannelLogo && (showChannelName || showOrderNumber || mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && (mActivity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE)) {
                logo = SettingConstants.SMALL_LOGO_MAP.get(key);
              } else {
                logo = SettingConstants.MEDIUM_LOGO_MAP.get(key);
              }
            }

            ImageView logoView = ((ViewGroup) view.getParent()).findViewById(R.id.program_list_channel_logo);

            if (logo != null) {
              logoView.setImageDrawable(logo);
              logoView.setVisibility(View.VISIBLE);

              if (!showChannelName && !showOrderNumber) {
                text.setVisibility(View.GONE);
              } else {
                text.setVisibility(View.VISIBLE);
              }
            } else {
              logoView.setVisibility(View.GONE);
              text.setVisibility(View.VISIBLE);
              showChannelName = true;
            }
            if (showChannelName) {
              text.setText(name);
            } else if (showOrderNumber) {
              text.setText(number);
            }
          } else {
            ((ViewGroup) view.getParent()).setVisibility(View.GONE);
          }

          return true;
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));

          java.text.DateFormat mTimeFormat = UiUtils.getTimeFormat(mActivity);

          TextView text = (TextView) view;
          text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
          text.setText(mTimeFormat.format(new Date(date)));

          return true;
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) {
          if (cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)) || !showInfo) {
            view.setVisibility(View.GONE);
          } else {
            int info = cursor.getInt(columnIndex);

            if (info != 0) {
              Spannable text = IOUtils.getInfoString(info, view.getResources());

              if (text != null) {
                view.setVisibility(View.VISIBLE);
                ((TextView) view).setText(text);
              } else {
                view.setVisibility(View.GONE);
              }
            } else {
              view.setVisibility(View.GONE);
            }
          }

          return true;
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) {
          if (cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) || !showEpisode) {
            view.setVisibility(View.GONE);
          } else {
            view.setVisibility(View.VISIBLE);
          }
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) {
          if (cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) || !showGenre) {
            view.setVisibility(View.GONE);
          } else {
            view.setVisibility(View.VISIBLE);
          }
        } else if (columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)) {
          TextView text = (TextView) view;
          ImageView picture = ((RelativeLayout) text.getParent()).findViewById(R.id.picture_pl);

          int pictureIndex = cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE);

          if (pictureIndex >= 0 && showPicture) {
            Bitmap logo = UiUtils.createBitmapFromByteArray(cursor.getBlob(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)));

            if (logo != null) {
              picture.setImageBitmap(Bitmap.createScaledBitmap(logo, (int) (mZoom * logo.getWidth()), (int) (mZoom * logo.getHeight()), false));//.setImageDrawable(l);

              text.setText(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));
              text.setVisibility(View.VISIBLE);
              picture.setVisibility(View.VISIBLE);
            } else {
              view.setVisibility(View.GONE);
              picture.setVisibility(View.GONE);
            }
          } else {
            view.setVisibility(View.GONE);
            picture.setVisibility(View.GONE);
          }
        }
      }
    }catch (IllegalStateException e) {
      // IGNORE
    }catch (StaleDataException e) {
      Log.d("info22","", e);
    }
    
    return false;
  }

  
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    long programID = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    
    UiUtils.createContextMenu(mActivity, menu, programID);
  }
  
  
  public boolean onContextItemSelected(MenuItem item) {
    if(item.getMenuInfo() != null) {
      long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
      
      UiUtils.handleContextMenuSelection(mActivity, item, programID, null, null);
    }
    
    return false;
  }
  
  public void onListItemClick(ListView l, View v, int position, long id) {
    UiUtils.showProgramInfo(mActivity, id, null,mHandler);
  }
}
