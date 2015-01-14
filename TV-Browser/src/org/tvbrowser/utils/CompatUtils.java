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

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.widget.RemoteViews;

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
}
