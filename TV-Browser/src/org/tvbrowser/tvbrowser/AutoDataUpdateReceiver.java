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

import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutoDataUpdateReceiver extends BroadcastReceiver {
  private static Thread mUpdateThread;
  
  @Override
  public void onReceive(final Context context, Intent intent) {
    PrefUtils.initialize(context);
    
    Log.d("info","xxxyyy");
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    
    String updateType = PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default);
    
    boolean autoUpdate = !updateType.equals("0");
    boolean internetConnection = updateType.equals("1");
    boolean timeUpdate = updateType.equals("2");
    Log.d("info", "au " + autoUpdate + " " + updateType);
    Log.d("info", "ic " + internetConnection);
    Log.d("info", "tu " + timeUpdate);
    if(autoUpdate) {
      if(internetConnection) {
        int days = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default)) + 1;
        
        long lastDate = PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE, R.integer.last_data_update_default);
        
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastDate);
        
        int dayDiff = (int)((System.currentTimeMillis() - last.getTimeInMillis()) / 60000. / 60. / 24.);
        Log.d("info", "dayDiff " + dayDiff + " " + days);

        autoUpdate = dayDiff >= days;
      }
      else if(timeUpdate) {
        autoUpdate = intent.getBooleanExtra(SettingConstants.TIME_DATA_UPDATE_EXTRA, false);
      }
      else {
        autoUpdate = false;
      }
      Log.d("info", "" + autoUpdate);
      if(autoUpdate) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        
        boolean onlyWifi = PrefUtils.getBooleanValue(R.string.PREF_AUTO_UPDATE_ONLY_WIFI, R.bool.pref_auto_update_only_wifi_default);
        
        boolean isConnected = wifi != null && wifi.isConnectedOrConnecting();
        
        if(!onlyWifi) {
          isConnected = isConnected || mobile != null && mobile.isConnectedOrConnecting();
        }
        Log.d("info", "isconn " + isConnected);
        
        if (isConnected && (mUpdateThread == null || !mUpdateThread.isAlive())) {
          mUpdateThread = new Thread() {
            @Override
            public void run() {
              try {
                sleep(10000);
              } catch (InterruptedException e) {}
              Log.d("info", "autoUpdate");
              if(!TvDataUpdateService.IS_RUNNING) {
                Intent startDownload = new Intent(context, TvDataUpdateService.class);
                startDownload.putExtra(TvDataUpdateService.TYPE, TvDataUpdateService.TV_DATA_TYPE);
                
                int daysToDownload = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_RANGE, R.string.pref_auto_update_range_default));
                
                startDownload.putExtra(context.getString(R.string.DAYS_TO_DOWNLOAD), daysToDownload);
                
                context.startService(startDownload);
              }
            }
          };
          mUpdateThread.start();
          
          Editor edit = pref.edit();
          edit.putLong(context.getString(R.string.LAST_DATA_UPDATE), System.currentTimeMillis());
          edit.commit();
        }
      }
      
      IOUtils.handleDataUpdatePreferences(context);
    }
  }

}
