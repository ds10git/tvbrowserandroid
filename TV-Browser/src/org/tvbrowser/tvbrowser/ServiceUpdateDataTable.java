/*
 * TV-Browser for Android
 * Copyright (C) 2018 René Mach (rene@tvbrowser.org)
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

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ServiceUpdateDataTable extends Service {
  private WakeLock mWakeLock;

  public ServiceUpdateDataTable() {
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Thread background = new Thread("DATA TABLE UPDATE THREAD") {
      @Override
      public void run() {
        while (TvDataUpdateService.isRunning()) {
          try {
            sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        
        updateDataTable();
      }
    };

    background.start();

    return Service.START_STICKY;
  }

  private void updateDataTable() {
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        "DATA TABLE UPDATE");
    mWakeLock.setReferenceCounted(false);
    mWakeLock.acquire(60000);
    
    IOUtils.deleteOldData(getApplicationContext());
    
    PrefUtils.updateDataMetaData(getApplicationContext());
    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SettingConstants.REFRESH_VIEWS));
    
    if(mWakeLock.isHeld()) {
      mWakeLock.release();
    }
    
    IOUtils.setDataTableRefreshTime(getApplicationContext());
    
    stopSelf();
  }
  
  @Override
  public void onDestroy() {
    if(mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
    }
    
    IOUtils.setDataTableRefreshTime(getApplicationContext());
    
    super.onDestroy();
  }
}
