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
package org.tvbrowser.tvbrowser;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

public class AutoDataUpdateReceiver extends BroadcastReceiver {
  private static Thread UPDATE_THREAD;
  private static final String TAG = "info9";
  
  @Override
  public void onReceive(final Context context, Intent intent) {
    PowerManager pm = (PowerManager)context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
    final WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TVBAUTOUPDATE_LOCK");
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire(7500);
    
    try {
      PrefUtils.initialize(context);
      
      Logging.openLogForDataUpdate(context);
      Logging.log(TAG, "AUTO DATA UPDATE onReceive Intent: " + intent + " Context: " + context, Logging.TYPE_DATA_UPDATE, context);
      
      final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
      
      String updateType = PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default);
      
      boolean autoUpdate = !updateType.equals("0");
      final boolean internetConnectionType = updateType.equals("1");
      boolean timeUpdateType = updateType.equals("2");
      
      Logging.log(TAG, "AUTO DATA UPDATE autoUpdateActive: " + autoUpdate + " Internet type: " + internetConnectionType + " Time type: " + timeUpdateType, Logging.TYPE_DATA_UPDATE, context);
      
      if(autoUpdate) {
	      if (internetConnectionType) {
		      int days = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default)) + 1;

		      long lastDate = PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE, R.integer.last_data_update_default);

		      Calendar last = Calendar.getInstance();
		      last.setTimeInMillis(lastDate);

		      int dayDiff = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - last.get(Calendar.DAY_OF_YEAR);//(int)((System.currentTimeMillis() - last.getTimeInMillis()) / 60000. / 60. / 24.);

		      if (dayDiff < 0) {
			      dayDiff = Calendar.getInstance().getMaximum(Calendar.DAY_OF_YEAR) + dayDiff;
		      }

		      autoUpdate = dayDiff >= days && (System.currentTimeMillis() - lastDate) / 1000 / 60 / 60 > 12;
	      } else
		      autoUpdate = timeUpdateType && intent.getBooleanExtra(SettingConstants.TIME_DATA_UPDATE_EXTRA, false);

	      Logging.log(TAG, "AUTO DATA UPDATE doUpdateNow ('" + new Date(System.currentTimeMillis()) +"'): " + autoUpdate , Logging.TYPE_DATA_UPDATE, context);
        
        if(autoUpdate) {
          ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
          
          NetworkInfo lan = CompatUtils.getLanNetworkIfPossible(connMgr);
          NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
          NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
          
          final boolean onlyWifi = PrefUtils.getBooleanValue(R.string.PREF_AUTO_UPDATE_ONLY_WIFI, R.bool.pref_auto_update_only_wifi_default);
          
          boolean isConnected = (wifi != null && wifi.isConnectedOrConnecting()) || (lan != null && lan.isConnectedOrConnecting());
          
          if(!onlyWifi) {
            isConnected = isConnected || (mobile != null && mobile.isConnectedOrConnecting());
          }
          
          Logging.log(TAG, "AUTO DATA UPDATE isConnected: " + isConnected + " IS_RUNNING: " + TvDataUpdateService.isRunning() + " UPDATE_THREAD: " + UPDATE_THREAD, Logging.TYPE_DATA_UPDATE, context);
          
          Logging.closeLogForDataUpdate();
          
          if(isConnected && timeUpdateType && PrefUtils.getStringValue(R.string.PREF_EPGPAID_USER, "").trim().length() > 0 && 
              PrefUtils.getStringValue(R.string.PREF_EPGPAID_PASSWORD, "").trim().length() > 0) {
            Calendar test = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            test.setTimeInMillis(System.currentTimeMillis());
            
            isConnected = !(test.get(Calendar.HOUR_OF_DAY) >= 23 || test.get(Calendar.HOUR_OF_DAY) < 4 ||
                (test.get(Calendar.HOUR_OF_DAY) >= 15 && test.get(Calendar.HOUR_OF_DAY) < 17));
          }
          
          if (isConnected && (UPDATE_THREAD == null || !UPDATE_THREAD.isAlive())) {
            IOUtils.handleDataUpdatePreferences(context,true);
            
            if(!TvDataUpdateService.isRunning() && IOUtils.isBatterySufficient(context)) {
              Intent startDownload = new Intent(context, TvDataUpdateService.class);
              startDownload.putExtra(TvDataUpdateService.KEY_TYPE, TvDataUpdateService.TYPE_TV_DATA);
              startDownload.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startDownload.putExtra(SettingConstants.EXTRA_DATA_UPDATE_TYPE, TvDataUpdateService.TYPE_UPDATE_AUTO);
              startDownload.putExtra(SettingConstants.EXTRA_DATA_UPDATE_TYPE_INTERNET_CONNECTION, internetConnectionType);
              
              int daysToDownload = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_RANGE, R.string.pref_auto_update_range_default));
              
              startDownload.putExtra(context.getString(R.string.DAYS_TO_DOWNLOAD), daysToDownload);
              
              Logging.openLogForDataUpdate(context);
              Logging.log(TAG, "UPDATE START INTENT " + startDownload, Logging.TYPE_DATA_UPDATE, context);
              Logging.closeLogForDataUpdate();

              CompatUtils.startForegroundService(context,startDownload);
            }
          }
          else if(!isConnected && timeUpdateType) {
            reschedule(context,pref);
          }
        }
        else {
          Logging.closeLogForDataUpdate();
        }
      }
      else {
        Logging.closeLogForDataUpdate();
      }
    }finally {
      releaseLock(wakeLock);
    }
  }
  
  public static void reschedule(Context context, SharedPreferences pref) {
    IOUtils.removeDataUpdateTime(context, pref);
    
    long current = System.currentTimeMillis() + (30 * 60000);
    
    Editor currentTime = PreferenceManager.getDefaultSharedPreferences(context).edit();
    currentTime.putLong(context.getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), current);
    currentTime.commit();
    
    IOUtils.setDataUpdateTime(context, current, pref);
  }
  
  private void releaseLock(WakeLock wakeLock) {
    try {
      if(wakeLock.isHeld()) {
        wakeLock.release();
      }
    }catch(Throwable ignored) {}
  }
}
