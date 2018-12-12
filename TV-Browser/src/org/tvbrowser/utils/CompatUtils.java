/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.IllegalFormatConversionException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.tvbrowser.tvbrowser.R;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.LocaleList;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TimePicker;

/**
 * A class that uses the current available method of deprecated methods on the Build.VERSION of the running device.
 * 
 * @author René Mach
 */
@SuppressLint("NewApi")
public final class CompatUtils {

  CompatUtils() {}

  public static boolean isKeyguardWidget(int appWidgetId, Context context) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
      
      return appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    }
    
    return false;
  }
  
  /**
   * Sets the view padding for View with viewId of RemoveViews views.
   * Will only work from JELLY_BEAN.
   * <p>
   * @param views The RemoteViews that contains viewId to set the padding for.
   * @param viewId The viewId to set the padding for
   * @param left Left padding in pixels.
   * @param top Top padding in pixels.
   * @param right Right padding in pixels.
   * @param bottom Bottom padding in pixels.
   */
  public static void setRemoteViewsPadding(RemoteViews views, int viewId, int left, int top, int right, int bottom) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      views.setViewPadding(viewId, left, top, right, bottom);
    }
  }
  
  @SuppressWarnings("deprecation")
  public static void setBackground(View view, Drawable draw) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackground(draw);
    }
    else {
      view.setBackgroundDrawable(draw);
    }
  }

  @SuppressWarnings("deprecation")
  public static boolean isInteractive(PowerManager pm) {
    boolean result;
    
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
      result = pm.isInteractive();
    }
    else {
      result = pm.isScreenOn();
    }
    
    return result;
  }

  public static void setExactAlarmAndAllowWhileIdle(AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation) {
    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      try {
        Method setExactAndAllowWhileIdle = alarm.getClass().getDeclaredMethod("setExactAndAllowWhileIdle", int.class, long.class, PendingIntent.class);
        setExactAndAllowWhileIdle.setAccessible(true);
        setExactAndAllowWhileIdle.invoke(alarm, type, triggerAtMillis, operation);
      } catch (Throwable t) {
        setAlarmExact(alarm, type, triggerAtMillis, operation);
      }
    }
    else {
      setAlarmExact(alarm, type, triggerAtMillis, operation);
    }
  }

  public static void setAlarmExact(AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      alarm.setExact(type, triggerAtMillis, operation);
    }
    else {
      alarm.set(type, triggerAtMillis, operation);
    }
  }

  public static void setAlarm(Context context, AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation, PendingIntent info) {
    setAlarm(context,alarm,type,triggerAtMillis,operation,info,PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.PREF_REMINDER_AS_ALARM_CLOCK), context.getResources().getBoolean(R.bool.pref_reminder_as_alarm_clock_default)));
  }

  public static void setAlarm(Context context, AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation, PendingIntent info, final boolean asAlaramClock) {
    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      // Cheap workaround for Marshmallow doze mode
      if(asAlaramClock) {
        alarm.setAlarmClock(new AlarmClockInfo(triggerAtMillis, info), operation);
      }
      else {
        try {
          Method setExactAndAllowWhileIdle = alarm.getClass().getDeclaredMethod("setExactAndAllowWhileIdle", int.class, long.class, PendingIntent.class);
          setExactAndAllowWhileIdle.setAccessible(true);
          setExactAndAllowWhileIdle.invoke(alarm, type, triggerAtMillis, operation);
        } catch (Throwable t) {
          Log.d("info22", "", t);
          alarm.setExact(type, triggerAtMillis, operation);
        }
      }
    }
    else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      alarm.setExact(type, triggerAtMillis, operation);
    }
    else {
      alarm.set(type, triggerAtMillis, operation);
    }
  }
  
  /*public static String getExternalDocumentsDir() {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return Environment.DIRECTORY_DOCUMENTS;
    }
    else {
      return Environment.DIRECTORY_DOWNLOADS;
    }
  }
  
  public static void quitLooperSafely(Looper looper) {
    if(looper != null) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        looper.quitSafely();
      }
      else {
        looper.quit();
      }
    }
  }*/
  
  public static boolean acceptFileAsSdCard(File file) {
    if(Build.VERSION.SDK_INT >= 23) {
      return file.isDirectory();
    }
    else {
      return file.isDirectory() && file.getName().toLowerCase(Locale.GERMAN).contains("sdcard");
    }
  }

  @SuppressWarnings("deprecation")
  public static void setTimePickerHour(final TimePicker timePicker, final int hour) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
      timePicker.setHour(hour);
    }
    else {
      timePicker.setCurrentHour(hour);
    }
  }

  @SuppressWarnings("deprecation")
  public static int getTimePickerHour(final TimePicker timePicker) {
    int hour;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      hour = timePicker.getHour();
    }
    else {
      hour = timePicker.getCurrentHour();
    }
    return hour;
  }

  @SuppressWarnings("deprecation")
  public static void setTimePickerMinute(final TimePicker timePicker, final int minute) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
      timePicker.setMinute(minute);
    }
    else {
      timePicker.setCurrentMinute(minute);
    }
  }

  @SuppressWarnings("deprecation")
  public static int getTimePickerMinute(final TimePicker timePicker) {
    int minute;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      minute = timePicker.getMinute();
    }
    else {
      minute = timePicker.getCurrentMinute();
    }
    return minute;
  }

  public static boolean isAtLeastAndroidN() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
  }

  public static boolean isAtLeastAndroidO() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
  }

  public static boolean startForegroundService(final Context context, final Intent service) {
    boolean result;

    if(isAtLeastAndroidO()) {
      result = context.startForegroundService(service) != null;
    }
    else {
      result = context.startService(service) != null;
    }

    return result;
  }

  @SuppressWarnings("deprecation")
  public static Spanned fromHtml(String html){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    } else {
      return Html.fromHtml(html);
    }
  }

  @SuppressWarnings("deprecation")
  public static Spanned fromHtml(String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY, imageGetter, tagHandler);
    } else {
      return Html.fromHtml(source, imageGetter, tagHandler);
    }
  }

  @SuppressWarnings("deprecation")
  @Nullable
  public static NetworkInfo getNetworkInfo(@Nullable final ConnectivityManager connectivityManager, final int type) {
    if (connectivityManager!=null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return connectivityManager.getNetworkInfo(type);
      } else {
        final Network[] networks = connectivityManager.getAllNetworks();
        if (networks != null) {
          for (final Network network : networks) {
            if (network != null) {
              final NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
              if (networkInfo != null && networkInfo.getType() == type) {
                return networkInfo;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Wraps context with {@link WorkaroundContextForSamsungLDateTimeBug} instance if needed.
   */
  @Nullable
  public static Context getDatePickerContext(final @Nullable Context context) {
    if (context!=null && (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP
            || Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1)) {
      return new WorkaroundContextForSamsungLDateTimeBug(context);
    }

    return context;
  }

  /**
   * Workaround for Samsung Lollipop devices that may crash due to wrong string resource supplied
   * to {@code SimpleMonthView}'s content description.
   */
  private static class WorkaroundContextForSamsungLDateTimeBug extends ContextWrapper {
    private Resources mWrappedResources;

    private WorkaroundContextForSamsungLDateTimeBug(final @NonNull Context context) {
      super(context);
    }

    @Override
    public Resources getResources() {
      if (mWrappedResources == null) {
        final Resources r = super.getResources();
        mWrappedResources = new WrappedResources(
                r.getAssets(), r.getDisplayMetrics(), r.getConfiguration()) {};
      }
      return mWrappedResources;
    }

    private class WrappedResources extends Resources {
      @SuppressWarnings("deprecation")
      WrappedResources(final AssetManager assets, final DisplayMetrics displayMetrics,
                       final Configuration configuration) {
        super(assets, displayMetrics, configuration);
      }

      @NonNull
      @Override
      public String getString(final int id, final Object... formatArgs) throws NotFoundException {
        try {
          return super.getString(id, formatArgs);
        } catch (IllegalFormatConversionException conversationException) {
          String template = super.getString(id);
          final char conversion = conversationException.getConversion();
          // Trying to replace either all digit patterns (%d) or first one (%1$d).
          template = template.replaceAll(Pattern.quote("%" + conversion), "%s")
                  .replaceAll(Pattern.quote("%1$" + conversion), "%s");
          Locale locale = getPrimaryUserLocale(getConfiguration());
          if (locale==null) {
            locale = Locale.getDefault();
          }
          return String.format(locale, template, formatArgs);
        }
      }
    }
  }

  @SuppressWarnings({"deprecation", "WeakerAccess"})
  @Nullable
  public static Locale getPrimaryUserLocale(@NonNull final Configuration configuration) {
    final Locale result;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      final LocaleList locales = configuration.getLocales();
      if (locales.size() > 0) {
        result = locales.get(0);
      } else {
        result = null;
      }
    } else {
      result = configuration.locale;
    }
    return result;
  }

  public static boolean showWidgetRefreshInfo() {
    boolean show = PrefUtils.getBooleanValue(R.string.PREF_WIDGET_REFRESH_WARNING, R.bool.pref_widget_refresh_warning_default);

    if(show && !Build.MANUFACTURER.toLowerCase(Locale.GERMAN).contains("huawei")) {
      show = false;
      PrefUtils.setBooleanValue(R.string.PREF_WIDGET_REFRESH_WARNING, false);
    }

    return show && isAtLeastAndroidO();
  }
}