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
import java.util.Locale;

import org.tvbrowser.tvbrowser.R;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.TimePicker;

/**
 * A class that uses the current available method of deprecated methods on the Build.VERSION of the running device.
 * 
 * @author René Mach
 */
@SuppressLint("NewApi")
public class CompatUtils {
  @SuppressWarnings("deprecation")
  public static final void setRemoteViewsAdapter(RemoteViews views, int appWidgetId, int viewId, Intent intent) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      views.setRemoteAdapter(appWidgetId, viewId, intent);
    }
    else {
      views.setRemoteAdapter(viewId, intent);
    }
  }
  
  public static final boolean isKeyguardWidget(int appWidgetId, Context context) {
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
  public static final void setRemoteViewsPadding(RemoteViews views, int viewId, int left, int top, int right, int bottom) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      views.setViewPadding(viewId, left, top, right, bottom);
    }
  }
  
  @SuppressWarnings("deprecation")
  public static final void setBackground(View view, Drawable draw) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackground(draw);
    }
    else {
      view.setBackgroundDrawable(draw);
    }
  }
  
  public static NetworkInfo getLanNetworkIfPossible(ConnectivityManager connMgr) {
    NetworkInfo result = null;
    
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
      result = connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
    }
    
    return result;
  }
  
  @SuppressWarnings("deprecation")
  public static boolean isInteractive(PowerManager pm) {
    boolean result = false;
    
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
      result = pm.isInteractive();
    }
    else {
      result = pm.isScreenOn();
    }
    
    return result;
  }
  
  @SuppressWarnings("deprecation")
  public static final Point getScreenSize(Context context) {
    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    
    Point size = new Point();
    
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
      display.getSize(size);
    }
    else {
      size.set(display.getWidth(), display.getHeight());
    }
    
    return size;
  }
  
  public static final void setAlarmInexact(AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation) {
    alarm.set(type, triggerAtMillis, operation);
  }
  
  public static final void setExactAlarmAndAllowWhileIdle(Context context, AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation) {
    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      try {
        Method setExactAndAllowWhileIdle = alarm.getClass().getDeclaredMethod("setExactAndAllowWhileIdle", int.class, long.class, PendingIntent.class);
        setExactAndAllowWhileIdle.setAccessible(true);
        setExactAndAllowWhileIdle.invoke(alarm, type, triggerAtMillis, operation);
      } catch (Throwable t) {
        setAlarmExact(context, alarm, type, triggerAtMillis, operation);
      }
    }
    else {
      setAlarmExact(context, alarm, type, triggerAtMillis, operation);
    }
  }


  
  public static final void setAlarmExact(Context context, AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      alarm.setExact(type, triggerAtMillis, operation);
    }
    else {
      alarm.set(type, triggerAtMillis, operation);
    }
  }
  
  public static final void setAlarm(Context context, AlarmManager alarm, int type, long triggerAtMillis, PendingIntent operation, PendingIntent info) {
    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      // Cheap workaround for Marshmallow doze mode
      if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.PREF_REMINDER_AS_ALARM_CLOCK), context.getResources().getBoolean(R.bool.pref_reminder_as_alarm_clock_default))) {
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
  
  public static String getExternalDocumentsDir() {
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
  }
  
  public static boolean acceptFileAsSdCard(File file) {
    if(Build.VERSION.SDK_INT >= 23) {
      return file.isDirectory();
    }
    else {
      return file.isDirectory() && file.getName().toLowerCase(Locale.GERMAN).contains("sdcard");
    }
  }

  public static void setTimePickerHour(final TimePicker timePicker, final int hour) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
      timePicker.setHour(hour);
    }
    else {
      //noinspection deprecation
      timePicker.setCurrentHour(hour);
    }
  }

  public static int getTimePickerHour(final TimePicker timePicker) {
    int hour;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      hour = timePicker.getHour();
    }
    else {
      //noinspection deprecation
      hour = timePicker.getCurrentHour();
    }
    return hour;
  }

  public static void setTimePickerMinute(final TimePicker timePicker, final int minute) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
      timePicker.setMinute(minute);
    }
    else {
      //noinspection deprecation
      timePicker.setCurrentMinute(minute);
    }
  }

  public static int getTimePickerMinute(final TimePicker timePicker) {
    int minute;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      minute = timePicker.getMinute();
    }
    else {
      //noinspection deprecation
      minute = timePicker.getCurrentMinute();
    }
    return minute;
  }

  public static boolean isAtLeastAndroidO() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
  }

  public static boolean canRequestPackageInstalls(final Context context) {
    return !isAtLeastAndroidO() || context.getPackageManager().canRequestPackageInstalls();
  }

  public static boolean startForegroundService(final Context context, final Intent service) {
    boolean result = false;

    if(isAtLeastAndroidO()) {
      result = context.startForegroundService(service) != null;
    }
    else {
      result = context.startService(service) != null;
    }

    return result;
  }
}
