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

package org.tvbrowser.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.filter.FilterValues;
import org.tvbrowser.filter.FilterValuesChannels;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Favorite;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.ServiceChannelCleaner;
import org.tvbrowser.tvbrowser.ServiceUpdateReminders;
import org.tvbrowser.tvbrowser.TvBrowser;
import org.tvbrowser.tvbrowser.UpdateAlarmValue;

public final class VersionUtils {

  VersionUtils() {}

  @SuppressLint("ApplySharedPref")
  public static void applyUpdates(@NonNull final TvBrowser tvBrowser) {

    try {

      final Context applicationContext = tvBrowser.getApplicationContext();
      final PackageInfo pInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
      final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

      final int currentVersion = pInfo.versionCode;
      final int oldVersion = PrefUtils.getIntValueWithDefaultKey(R.string.OLD_VERSION, R.integer.old_version_default);

      Log.i("VersionUtils", String.format(Locale.getDefault(), "applyUpdates [currentVersion=%d, oldVersion=%d]", currentVersion, oldVersion));
      //PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, getApplicationContext()).edit().remove(getString(R.string.CURRENT_FILTER_ID)).commit();

      if(oldVersion < 422) {
        final SharedPreferences pref = applicationContext.getSharedPreferences("transportation", Context.MODE_PRIVATE);

        final String car = pref.getString(SettingConstants.USER_NAME, null);
        final String bicycle = pref.getString(SettingConstants.USER_PASSWORD, null);

        if(car != null && car.trim().length() > 0 && bicycle != null && bicycle.trim().length() > 0) {
          PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, applicationContext).edit().putBoolean(tvBrowser.getString(R.string.PREF_PRIVACY_TERMS_ACCEPTED_SYNC),true).apply();
        }

        final String userName = PrefUtils.getStringValue(R.string.PREF_EPGPAID_USER, null);
        final String password = PrefUtils.getStringValue(R.string.PREF_EPGPAID_PASSWORD, null);

        if(userName != null && password != null && userName.trim().length() > 0 && password.trim().length() > 0) {
          PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, applicationContext).edit().putBoolean(tvBrowser.getString(R.string.PREF_PRIVACY_TERMS_ACCEPTED_EPGPAID),true).apply();
        }
      }

      if(oldVersion < 419) {
        final File dir = IOUtils.getDownloadDirectory(applicationContext);

        if(dir.isDirectory()) {
          final String[] filesLog = {
              SettingConstants.LOG_FILE_NAME_DATA_UPDATE,
              SettingConstants.LOG_FILE_NAME_REMINDER,
              SettingConstants.LOG_FILE_NAME_PLUGINS
          };

          for(final String fileLog : filesLog) {
            final File log = new File(dir, fileLog);

            if (log.isFile() && !log.delete()) {
              log.deleteOnExit();
            }
          }
        }
      }

      if(oldVersion < 417) {
        PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, applicationContext).edit().putString(tvBrowser.getString(R.string.DETAIL_PICTURE_ZOOM), tvBrowser.getString(R.string.detail_picture_zoom_default)).commit();
      }
      if(oldVersion < 416) {
        PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, applicationContext).edit().remove(tvBrowser.getString(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION)).commit();
        PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, applicationContext).edit().putString(tvBrowser.getString(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION),"1").commit();
      }
      if(oldVersion == 402) {
        PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, applicationContext).edit().remove(tvBrowser.getString(R.string.PREF_REMINDER_AS_ALARM_CLOCK)).commit();
        ServiceUpdateReminders.startReminderUpdate(applicationContext);
      }
      if(oldVersion < 339) {
        applicationContext.startService(new Intent(applicationContext, ServiceChannelCleaner.class));
      }
      if(oldVersion < 332) {
        PrefUtils.updateDataMetaData(applicationContext);
        PrefUtils.updateChannelSelectionState(applicationContext);
      }
      if(oldVersion < 322) {
        final SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, applicationContext);
        final SharedPreferences.Editor edit = pref.edit();
        final Map<String,?> filterMap = pref.getAll();
        for(String key : filterMap.keySet()) {
          if(!key.contains(FilterValues.SEPARATOR_CLASS)) {
            final String values = (String) filterMap.get(key);
            edit.remove(key);
            edit.putString(FilterValuesChannels.class.getCanonicalName()+FilterValues.SEPARATOR_CLASS+key, values);
          }
        }

        edit.commit();
      }
      if(oldVersion > tvBrowser.getResources().getInteger(R.integer.old_version_default) && oldVersion < 314) {
        new Thread("READ SYNCED PROGRAMS ONCE FOR ICON") {
          @Override
          public void run() {
            if(IOUtils.isDatabaseAccessible(applicationContext)) {
              Cursor cursor = null;
              try {
                cursor = applicationContext.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.DATA_KEY_MARKING_SYNC, null, TvBrowserContentProvider.KEY_ID);
                if(cursor!=null && IOUtils.prepareAccess(cursor)) {
                  final int idColumn = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                  final ArrayList<String> syncIdList = new ArrayList<>();
                  while(cursor.moveToNext()) {
                    syncIdList.add(String.valueOf(cursor.getLong(idColumn)));
                  }

                  ProgramUtils.addSyncIds(applicationContext, syncIdList);
                }
              }finally {
                IOUtils.close(cursor);
              }
            }
          }
        }.start();
      }
      if(oldVersion > tvBrowser.getResources().getInteger(R.integer.old_version_default) && oldVersion < 309) {
        new Thread("READ REMINDERS ONCE FOR ICON") {
          @Override
          public void run() {
            if(IOUtils.isDatabaseAccessible(applicationContext)) {
              Cursor cursor = null;
              try {
                cursor = applicationContext.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID}, TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " OR " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, null, TvBrowserContentProvider.KEY_ID);
                if(cursor!=null && IOUtils.prepareAccess(cursor)) {
                  final int idColumn = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                  final ArrayList<String> reminderIdList = new ArrayList<>();
                  while(cursor.moveToNext()) {
                    reminderIdList.add(String.valueOf(cursor.getLong(idColumn)));
                  }

                ProgramUtils.addReminderIds(applicationContext, reminderIdList);
                }
              }finally {
                IOUtils.close(cursor);
              }
            }
          }
        }.start();
      }

      if(oldVersion < 304) {
        final Set<String> favoritesSet = defaultSharedPreferences.getStringSet("FAVORITE_LIST", new HashSet<>());
        int id = 1000;
        for (final String favorite : favoritesSet) {
          final Favorite fav = new Favorite(id++, favorite);
          if(fav.isValid()) {
            fav.save(applicationContext);
          }
          else {
            Favorite.handleFavoriteMarking(applicationContext, fav, Favorite.TYPE_MARK_REMOVE);
          }
        }

        defaultSharedPreferences.edit().remove("FAVORITE_LIST").commit();
      }
      if(oldVersion < 204) {
        final int firstTime = PrefUtils.getStringValueAsInt(R.string.PREF_REMINDER_TIME, R.string.pref_reminder_time_default);
        final boolean remindAgain = defaultSharedPreferences.getBoolean("PREF_REMIND_AGAIN_AT_START", false);
        final SharedPreferences.Editor edit = defaultSharedPreferences.edit();
        edit.remove("PREF_REMIND_AGAIN_AT_START");
        edit.commit();

        if(remindAgain && firstTime > 0) {
          edit.putString(tvBrowser.getString(R.string.PREF_REMINDER_TIME_SECOND), tvBrowser.getString(R.string.pref_reminder_time_default));
          edit.commit();

          applicationContext.sendBroadcast(new Intent(UpdateAlarmValue.class.getCanonicalName()));
        }
      }
      if(oldVersion < 218) {
        final SharedPreferences.Editor edit = defaultSharedPreferences.edit();

        boolean userDefined = addUserColor(applicationContext, defaultSharedPreferences,edit,R.color.pref_color_on_air_background_tvb_style_default,R.string.PREF_COLOR_ON_AIR_BACKGROUND,R.string.PREF_COLOR_ON_AIR_BACKGROUND_USER_DEFINED);
        userDefined = addUserColor(applicationContext, defaultSharedPreferences,edit,R.color.pref_color_on_air_progress_tvb_style_default,R.string.PREF_COLOR_ON_AIR_PROGRESS,R.string.PREF_COLOR_ON_AIR_PROGRESS_USER_DEFINED) || userDefined;
        userDefined = addUserColor(applicationContext, defaultSharedPreferences,edit,R.color.pref_color_mark_tvb_style_default,R.string.PREF_COLOR_MARKED,R.string.PREF_COLOR_MARKED_USER_DEFINED) || userDefined;
        userDefined = addUserColor(applicationContext, defaultSharedPreferences,edit,R.color.pref_color_mark_favorite_tvb_style_default,R.string.PREF_COLOR_FAVORITE,R.string.PREF_COLOR_FAVORITE) || userDefined;
        userDefined = addUserColor(applicationContext, defaultSharedPreferences,edit,R.color.pref_color_mark_reminder_tvb_style_default,R.string.PREF_COLOR_REMINDER,R.string.PREF_COLOR_REMINDER_USER_DEFINED) || userDefined;
        userDefined = addUserColor(applicationContext, defaultSharedPreferences,edit,R.color.pref_color_mark_sync_tvb_style_favorite_default,R.string.PREF_COLOR_SYNC,R.string.PREF_COLOR_SYNC_USER_DEFINED) || userDefined;

        if(userDefined) {
          edit.putString(tvBrowser.getString(R.string.PREF_COLOR_STYLE), "0");
        }

        edit.commit();
      }
      if(oldVersion < 242) {
        final SharedPreferences.Editor edit = defaultSharedPreferences.edit();

        if(defaultSharedPreferences.contains("PREF_WIDGET_BACKGROUND_TRANSPARENCY") && !defaultSharedPreferences.getBoolean("PREF_WIDGET_BACKGROUND_TRANSPARENCY", true)) {
          edit.remove("PREF_WIDGET_BACKGROUND_TRANSPARENCY");
          edit.putString(tvBrowser.getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_HEADER), "0");
          edit.putString(tvBrowser.getString(R.string.PREF_WIDGET_BACKGROUND_TRANSPARENCY_LIST), "0");
          edit.putBoolean(tvBrowser.getString(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS), false);
        }

        if(defaultSharedPreferences.contains("SELECTED_TV_CHANNELS_LIST")) {
          edit.remove("SELECTED_TV_CHANNELS_LIST");
        }
        if(defaultSharedPreferences.contains("SELECTED_RADIO_CHANNELS_LIST")) {
          edit.remove("SELECTED_RADIO_CHANNELS_LIST");
        }
        if(defaultSharedPreferences.contains("SELECTED_CINEMA_CHANNELS_LIST")) {
          edit.remove("SELECTED_CINEMA_CHANNELS_LIST");
        }

        edit.commit();
      }
      if(oldVersion < 284 && tvBrowser.getString(R.string.divider_small).equals(PrefUtils.getStringValue(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.pref_program_lists_divider_size_default))) {

        defaultSharedPreferences.edit().remove(tvBrowser.getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE)).commit();
      }
      if(oldVersion < 287 && PrefUtils.getBooleanValue(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS, true)) {
        defaultSharedPreferences.edit().remove(tvBrowser.getString(R.string.PREF_WIDGET_BACKGROUND_ROUNDED_CORNERS)).commit();

        UiUtils.updateImportantProgramsWidget(applicationContext);
        UiUtils.updateRunningProgramsWidget(applicationContext);
      }
      if(oldVersion < 369) {
        defaultSharedPreferences.edit().putBoolean(tvBrowser.getString(R.string.PREF_EPGPAID_FIRST_DOWNLOAD_DONE), false).commit();
      }
      if(oldVersion < 379) {
        final HashSet<String> values = new HashSet<>();
        final String currentFilterId = PrefUtils.getStringValue(R.string.CURRENT_FILTER_ID, null);

        if(currentFilterId != null) {
          values.add(currentFilterId);
        }

        defaultSharedPreferences.edit().putStringSet(tvBrowser.getString(R.string.CURRENT_FILTER_ID), values).commit();
      }

      if(oldVersion > tvBrowser.getResources().getInteger(R.integer.old_version_default) && oldVersion < currentVersion && PrefUtils.getBooleanValue(R.string.PREF_INFO_VERSION_UPDATE_SHOW, R.bool.pref_info_version_update_show_default)) {
        tvBrowser.setInfoType(TvBrowser.INFO_TYPE_VERSION);
      }
      else if(oldVersion != tvBrowser.getResources().getInteger(R.integer.old_version_default) && PrefUtils.getBooleanValue(R.string.PREF_NEWS_SHOW, R.bool.pref_news_show_default)) {
        final long lastShown = PrefUtils.getLongValue(R.string.NEWS_DATE_LAST_SHOWN, 0);
        final long lastKnown = PrefUtils.getLongValue(R.string.NEWS_DATE_LAST_KNOWN, 0);
        Log.d("info6", "lastShown " + new Date(lastShown) + " " + "lastKnown " + new Date(lastKnown));
        if(lastShown < lastKnown) {
          tvBrowser.setInfoType(TvBrowser.INFO_TYPE_NEWS);
        }
      }

      if(oldVersion != currentVersion) {
        defaultSharedPreferences.edit().putInt(tvBrowser.getString(R.string.OLD_VERSION), currentVersion).commit();
      }
    } catch (final PackageManager.NameNotFoundException nameNotFoundException) {
      nameNotFoundException.printStackTrace();
    }
  }

  private static boolean addUserColor(@NonNull final Context context,
    @NonNull final SharedPreferences pref, @NonNull final SharedPreferences.Editor edit,
    final int defaultColorKey, final int colorKey, final int userColorKey) {
    final int defaultColor = ContextCompat.getColor(context, defaultColorKey);
    final int color = pref.getInt(context.getString(colorKey), defaultColor);
    edit.putInt(context.getString(userColorKey), color);
    return defaultColor != color;
  }
}