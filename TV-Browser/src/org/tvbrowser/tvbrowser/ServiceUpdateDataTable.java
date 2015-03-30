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
import android.support.v4.content.LocalBroadcastManager;

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
