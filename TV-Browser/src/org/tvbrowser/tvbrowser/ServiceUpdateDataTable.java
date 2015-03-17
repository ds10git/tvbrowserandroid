package org.tvbrowser.tvbrowser;

import java.util.Calendar;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
        while (TvDataUpdateService.IS_RUNNING) {
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

    Calendar cal2 = Calendar.getInstance();
    cal2.add(Calendar.DAY_OF_YEAR, -2);

    long daysSince1970 = cal2.getTimeInMillis() / 24 / 60 / 60000;

    try {
      getContentResolver().delete(
          TvBrowserContentProvider.CONTENT_URI_DATA,
          TvBrowserContentProvider.DATA_KEY_STARTTIME + "<"
              + cal2.getTimeInMillis(), null);
    } catch (IllegalArgumentException e) {
    }

    try {
      getContentResolver().delete(
          TvBrowserContentProvider.CONTENT_URI_DATA_VERSION,
          TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + "<"
              + daysSince1970, null);
    } catch (IllegalArgumentException e) {
    }

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
