package org.tvbrowser.tvbrowser;

import java.util.Date;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;

public class ServiceUpdateReminders extends Service {
  public static final String EXTRA_FIRST_STARTUP = "extraFirstStartup";
  private static final int MAX_REMINDERS = 50;
  
  private static final String[] PROJECTION = {
      TvBrowserContentProvider.KEY_ID,
      TvBrowserContentProvider.DATA_KEY_STARTTIME
    };
  
  private Thread mUpdateRemindersThread;
  
  public ServiceUpdateReminders() {
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
  
  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    if(mUpdateRemindersThread == null || !mUpdateRemindersThread.isAlive()) {
      mUpdateRemindersThread = new Thread("UPDATE REMINDERS THREAD") {
        @Override
        public void run() {
          if(IOUtils.isDatabaseAccessible(ServiceUpdateReminders.this)) {
            boolean firstStart = intent != null ? intent.getBooleanExtra(EXTRA_FIRST_STARTUP, false) : false;
            
            StringBuilder where = new StringBuilder(" ( " + TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER + " OR " + TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER + " ) AND ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis() + " ) ");
            
            if(!firstStart) {
              where.append(" AND ( ").append(TvBrowserContentProvider.DATA_KEY_STARTTIME).append(" >= ").append((System.currentTimeMillis()-200)).append(" ) ");
            }
            
            try {
              Cursor alarms = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, PROJECTION, where.toString(), null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC LIMIT " + MAX_REMINDERS);
              
              try {
                if(IOUtils.prepareAccess(alarms)) {
                  while(alarms.moveToNext()) {
                    long id = alarms.getLong(alarms.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                    long startTime = alarms.getLong(alarms.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
                    
                    IOUtils.removeReminder(getApplicationContext(), id);
                    addReminder(getApplicationContext(), id, startTime, UpdateAlarmValue.class, firstStart);
                  }
                }
              }finally {
                IOUtils.close(alarms);
              }
            }catch(IllegalStateException ise) {
              //Ignore, only make sure TV-Browser didn't crash after moving of database
            }
          }
          else {
            try {
              sleep(500);
            } catch (InterruptedException e) {}
          }
          
          stopSelf();
        };
      };
      mUpdateRemindersThread.start();
    }
        
    return Service.START_NOT_STICKY;
  }
  
  private void addReminder(Context context, long programID, long startTime, Class<?> caller, boolean firstCreation) {try {
    Logging.log(ReminderBroadcastReceiver.tag, "addReminder called from: " + caller + " for programID: '" + programID + "' with start time: " + new Date(startTime), Logging.TYPE_REMINDER, context);
    
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    int reminderTime = PrefUtils.getStringValueAsInt(R.string.PREF_REMINDER_TIME, R.string.pref_reminder_time_default) * 60000;
    int reminderTimeSecond = PrefUtils.getStringValueAsInt(R.string.PREF_REMINDER_TIME_SECOND, R.string.pref_reminder_time_default) * 60000;
    
    boolean remindAgain = reminderTimeSecond >= 0 && reminderTime != reminderTimeSecond;
    
    Intent remind = new Intent(context.getApplicationContext(),ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    if(startTime <= 0 && IOUtils.isDatabaseAccessible(context)) {
      Cursor time = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME}, null, null, null);
      
      if(time.moveToFirst()) {
        startTime = time.getLong(0);
      }
      
      time.close();
    }
    
    if(startTime >= System.currentTimeMillis()) {
      PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
      Intent startInfo = new Intent(context, InfoActivity.class);
      startInfo.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startInfo.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
      
      PendingIntent start = PendingIntent.getActivity(context, (int)programID, startInfo, PendingIntent.FLAG_UPDATE_CURRENT);
      
      if(startTime-reminderTime > System.currentTimeMillis()-200) {        
        Logging.log(ReminderBroadcastReceiver.tag, "Create Reminder at " + new Date(startTime-reminderTime) + " with programID: '" + programID + "' " + pending.toString(), Logging.TYPE_REMINDER, context);
        CompatUtils.setAlarm(alarmManager,AlarmManager.RTC_WAKEUP, startTime-reminderTime, pending, start);
      }
      else if(firstCreation) {
        Logging.log(ReminderBroadcastReceiver.tag, "Create Reminder at " + new Date(System.currentTimeMillis()) + " with programID: '" + programID + "' " + pending.toString(), Logging.TYPE_REMINDER, context);
        CompatUtils.setAlarm(alarmManager,AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pending, start);
      }
      
      if(remindAgain && startTime-reminderTimeSecond > System.currentTimeMillis()) {
        pending = PendingIntent.getBroadcast(context, (int)-programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
        
        Logging.log(ReminderBroadcastReceiver.tag, "Create Reminder at " + new Date(startTime-reminderTimeSecond) + " with programID: '-" + programID + "' " + pending.toString(), Logging.TYPE_REMINDER, context);
        CompatUtils.setAlarm(alarmManager,AlarmManager.RTC_WAKEUP, startTime-reminderTimeSecond, pending, start);
      }
    }
    else {
      Logging.log(ReminderBroadcastReceiver.tag, "Reminder for programID: '" + programID + "' not created, starttime in past: " + new Date(startTime) + " of now: " + new Date(System.currentTimeMillis()), Logging.TYPE_REMINDER, context);
    }
  }catch(Throwable t) {t.printStackTrace();}
  }
    
  public static final void startReminderUpdate(Context context) {
    startReminderUpdate(context,false);
  }
  
  public static final void startReminderUpdate(Context context, boolean firstStart) {
    startReminderUpdate(context,false,-1);
  }
  
  public static final void startReminderUpdate(Context context, boolean firstStart, long ignoreId) {
    Intent updateAlarms = new Intent(context, ServiceUpdateReminders.class);
    updateAlarms.putExtra(ServiceUpdateReminders.EXTRA_FIRST_STARTUP, firstStart);
    context.startService(updateAlarms);
  }
}
