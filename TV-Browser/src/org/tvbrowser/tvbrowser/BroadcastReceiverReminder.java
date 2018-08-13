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
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowser;

import java.util.Calendar;
import java.util.Date;

import org.tvbrowser.App;
import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.ProgramOrdered;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.tvbrowser.utils.CompatUtils;

public class BroadcastReceiverReminder extends BroadcastReceiver {
  public static final String tag = null;

  @Override
  public void onReceive(final Context context, Intent intent) {
    PrefUtils.initialize(context);
    SettingConstants.initialize(context);

    Logging.log(tag, "ReminderBroadcastaReceiver.onReceive " + intent + " " + context, Logging.TYPE_REMINDER, context);
    long programID = intent.getLongExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, -1);
    
    Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' reminder is paused '" + SettingConstants.isReminderPaused(context) + "'", Logging.TYPE_REMINDER, context);
    
    if(!SettingConstants.isReminderPaused(context) && programID >= 0) {
      Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      
      String tone = PrefUtils.getStringValue(R.string.PREF_REMINDER_SOUND_VALUE, null);
      
      Uri soundUri = defaultUri;
      
      if(tone != null) {
        soundUri = Uri.parse(tone);
      }
                  
      boolean sound = tone == null || tone.trim().length() > 0;
      boolean vibrate = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_VIBRATE, R.bool.pref_reminder_vibrate_default);
      boolean led = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_LED, R.bool.pref_reminder_led_default);
      
      boolean showReminder = true;

      boolean isWorkMode = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED, R.bool.pref_reminder_work_mode_activated_default);
      boolean isNightMode = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED, R.bool.pref_reminder_night_mode_activated_default);

      Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' NIGHT MODE ACTIVATED '" + PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED, R.bool.pref_reminder_night_mode_activated_default) + "' sound '" + sound + "' vibrate '" + vibrate + "' led '" + led + "'", Logging.TYPE_REMINDER, context);
      
      //TODO add setting for priority
      int priority = getPriorityForPreferenceValue(PrefUtils.getStringValue(R.string.PREF_REMINDER_PRIORITY_VALUE, R.string.pref_reminder_priority_default));
      
      if(isNightMode) {
        int start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_NIGHT_MODE_START, R.integer.pref_reminder_night_mode_start_default);
        int end =  PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_NIGHT_MODE_END, R.integer.pref_reminder_night_mode_end_default);
        
        Calendar now = Calendar.getInstance();
        
        int minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        
        if(end < start) {
          if(minutes < start) {
            minutes += 24 * 60;
          }
          
          end += 24 * 60;
        }

        isNightMode = start <= minutes && minutes <= end;

        if(isNightMode) {
          showReminder = !PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_NO_REMINDER, R.bool.pref_reminder_night_mode_no_reminder_default);
          
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' CURRENTLY NIGHT MODE, Don't show '" + !showReminder + "'", Logging.TYPE_REMINDER, context);
          
          if(showReminder) {
            tone = PrefUtils.getStringValue(R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE, R.string.pref_reminder_night_mode_sound_value_default);
            
            if(tone != null) {
              soundUri = Uri.parse(tone);
            }
            else {
              soundUri = defaultUri;
            }
            
            sound = tone == null || tone.trim().length() > 0;
            vibrate = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_VIBRATE, R.bool.pref_reminder_night_mode_vibrate_default);
            led = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_LED, R.bool.pref_reminder_night_mode_led_default);
            priority = getPriorityForPreferenceValue(PrefUtils.getStringValue(R.string.PREF_REMINDER_NIGHT_MODE_PRIORITY_VALUE, R.string.pref_reminder_priority_default));
          }
        }
      }
      
      if(isWorkMode) {
        Calendar now = Calendar.getInstance();
        int[] values = getValuesForDay(now.get(Calendar.DAY_OF_WEEK));
        
        int minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        
        if(values[1] < values[0]) {
          if(minutes < values[0]) {
            minutes += 24 * 60;
          }
          
          values[1] += 24 * 60;
        }
        
        isWorkMode = values[0] <= minutes && minutes <= values[1];
        
        if(!isWorkMode) {
          now.add(Calendar.DAY_OF_YEAR, -1);
          
          values = getValuesForDay(now.get(Calendar.DAY_OF_WEEK));
          
          minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
          
          if(values[1] < values[0]) {
            if(minutes < values[0]) {
              minutes += 24 * 60;
            }
            
            values[1] += 24 * 60;
            
            isWorkMode = values[0] <= minutes && minutes <= values[1];
          }
        }
        
        if(isWorkMode) {
          showReminder = !PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_NO_REMINDER, R.bool.pref_reminder_work_mode_no_reminder_default);
          
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' CURRENTLY WORK MODE, Don't show '" + !showReminder + "'", Logging.TYPE_REMINDER, context);
          
          if(showReminder) {
            tone = PrefUtils.getStringValue(R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE, R.string.pref_reminder_work_mode_sound_value_default);
            
            if(tone != null) {
              soundUri = Uri.parse(tone);
            }
            else {
              soundUri = defaultUri;
            }
            
            sound = tone == null || tone.trim().length() > 0;
            vibrate = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_VIBRATE, R.bool.pref_reminder_work_mode_vibrate_default);
            led = PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_LED, R.bool.pref_reminder_work_mode_led_default);
            priority = getPriorityForPreferenceValue(PrefUtils.getStringValue(R.string.PREF_REMINDER_WORK_MODE_PRIORITY_VALUE, R.string.pref_reminder_priority_default));
          }
        }
        
      }
      
      Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' showReminder '" + showReminder + "' sound '" + sound + "' vibrate '" + vibrate + "' led '" + led + "'", Logging.TYPE_REMINDER, context);
      
      if(showReminder && IOUtils.isDatabaseAccessible(context)) {
        Logging.log(tag,  new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' CONTEXT: " + context + " contentResolver: " + context.getContentResolver(), Logging.TYPE_REMINDER, context);
        Cursor values = null; try {
        values = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), SettingConstants.REMINDER_PROJECTION, null, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
        assert values != null;
        Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' Tried to load program with given ID, cursor size: " + values.getCount(), Logging.TYPE_REMINDER, context);

        final App app = App.get();

        if(values.moveToFirst() && app != null) {
          String notificationId = app.getNotificationChannelId(App.TYPE_NOTIFICATION_REMINDER_DAY);

          if(isNightMode) {
            notificationId = app.getNotificationChannelId(App.TYPE_NOTIFICATION_REMINDER_NIGHT);
          }
          if(isWorkMode) {
            notificationId = app.getNotificationChannelId(App.TYPE_NOTIFICATION_REMINDER_WORK);
          }

          final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationId);
          
          // high priority notification
          builder.setPriority(priority);
          
          ProgramOrdered programOrdered = ProgramUtils.createProgramOrderedFromDataCursor(context, values);

          if(programOrdered != null) {
            Program program = programOrdered.getProgram();

            final String channelName = SettingConstants.getShortChannelNameIfAvailable(program.getChannel().getChannelName());//values.getString(values.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            final int orderNumber = programOrdered.getChannel().getOrderNumber();
            final String title = program.getTitle();//values.getString(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
            final String episode = program.getEpisodeTitle();//values.getString(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
            
            long startTime = program.getStartTimeInUTC();//values.getLong(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
           // long endTime = values.getLong(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
            
            Bitmap logo = UiUtils.createBitmapFromByteArray(/*values.getBlob(values.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO))*/program.getChannel().getIcon());
            Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' LOADED VALUES:  title '" + title + "' channelName '" + channelName + "' episode '" + episode + "' logo " + logo, Logging.TYPE_REMINDER, context);
            if(logo != null) {              
              int width =  context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
              int height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
              
              float scale = 1;

              if(logo.getWidth() > logo.getHeight()) {
                if(logo.getWidth() > width-4 || logo.getWidth() < (width-width/10.)) {
                  scale = ((float)width-4)/logo.getWidth();
                }
              }
              else {
                if(logo.getHeight() * scale > height-4 || logo.getHeight() < (height-height/10.)) {
                  scale = ((float)height-4)/logo.getHeight();
                }
              }

              if(scale != 1) {
                logo = Bitmap.createScaledBitmap(logo, (int)(logo.getWidth() * scale), (int)(logo.getHeight() * scale), true);
              }
              
              Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
              Canvas canvas = new Canvas(bitmap);
              canvas.drawColor(SettingConstants.LOGO_BACKGROUND_COLOR);
              canvas.drawBitmap(logo, width/2 - logo.getWidth()/2, height/2 - logo.getHeight()/2, null);
              
              builder.setLargeIcon(bitmap);
            }
            
            builder.setSmallIcon(R.drawable.ic_stat_reminder);
            builder.setWhen(startTime);

            if(!CompatUtils.isAtLeastAndroidO()) {
              if(sound) {
                builder.setSound(soundUri);
              }
              else {
                builder.setDefaults(0);
                builder.setSound(null);
              }

              if(vibrate) {
                builder.setVibrate(new long[] {1000,200,1000,400,1000,600});
              }
              else {
                builder.setVibrate(null);
              }

              if(led) {
                Log.d("info11", PrefUtils.getIntValue(R.string.PREF_REMINDER_COLOR_LED, ContextCompat.getColor(context, R.color.pref_reminder_color_led_default)) + " " + Color.GREEN + " " + Color.RED + " " + Color.YELLOW);

                builder.setLights(PrefUtils.getIntValue(R.string.PREF_REMINDER_COLOR_LED, ContextCompat.getColor(context, R.color.pref_reminder_color_led_default)), 1000, 2000);
              }
            }

            String channelInfo = (orderNumber != -1 ? orderNumber + ". " : "") + channelName;

            builder.setAutoCancel(true);

            if(CompatUtils.isAtLeastAndroidN()) {
              builder.setSubText(UiUtils.formatDate(startTime,context,false, true, true, java.text.DateFormat.SHORT, true) + " \u2022 " + channelInfo);
            }
            else {
              builder.setContentInfo(channelInfo);
            }

            builder.setContentTitle((!CompatUtils.isAtLeastAndroidN() ? UiUtils.formatDate(startTime,context,false, true, true, java.text.DateFormat.SHORT, true) + " " : "") + UiUtils.getTimeFormat(context).format(new Date(startTime)) + " " + title);
            
            if(episode != null) {
              builder.setContentText(episode);
            }

            Intent startInfo = new Intent(context, InfoActivity.class);
            startInfo.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startInfo.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
            startInfo.setAction("actionstring" + System.currentTimeMillis());
            
            builder.setContentIntent(PendingIntent.getActivity(context, 0, startInfo, 0));
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER);
            
            Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' Create notification with intent: " + startInfo, Logging.TYPE_REMINDER, context);
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
              notificationManager.notify(title,(int)(startTime / 60000), builder.build());
            }
            Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' Notification was send.", Logging.TYPE_REMINDER, context);
            
            Intent broadcastProgram = new Intent(SettingConstants.PROGRAM_REMINDED_FOR);
            broadcastProgram.putExtra(SettingConstants.EXTRA_REMINDED_PROGRAM, program);
            
            context.sendBroadcast(broadcastProgram,"org.tvbrowser.permission.RECEIVE_PROGRAMS");
          }
        }
        
        } finally {IOUtils.close(values);}
      }
    }
    
    new Thread("UPDATE REMINDER FROM BROADCAST THREAD") {
      @Override
      public void run() {
        try {
          Thread.sleep(400);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        
        ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context.getApplicationContext());
      }
    }.start();
  }
  
  private int[] getValuesForDay(int day) {
    int start = -1;
    int end = -1;
    
    switch(day) {
      case Calendar.MONDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_MONDAY_ACTIVATED, R.bool.pref_reminder_work_mode_monday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_MONDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_MONDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
      case Calendar.TUESDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_ACTIVATED, R.bool.pref_reminder_work_mode_tuesday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_TUESDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
      case Calendar.WEDNESDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_ACTIVATED, R.bool.pref_reminder_work_mode_wednesday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_WEDNESDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
      case Calendar.THURSDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_ACTIVATED, R.bool.pref_reminder_work_mode_thursday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_THURSDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
      case Calendar.FRIDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_ACTIVATED, R.bool.pref_reminder_work_mode_friday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_FRIDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
      case Calendar.SATURDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_ACTIVATED, R.bool.pref_reminder_work_mode_saturday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_SATURDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
      case Calendar.SUNDAY:
        if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_ACTIVATED, R.bool.pref_reminder_work_mode_sunday_activated_default)) {
          start = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_START, R.integer.pref_reminder_work_mode_start_default);
          end = PrefUtils.getIntValueWithDefaultKey(R.string.PREF_REMINDER_WORK_MODE_SUNDAY_END, R.integer.pref_reminder_work_mode_end_default);
        }
        break;
    }
    
    return new int[] {start,end};
  }

  private int getPriorityForPreferenceValue(String value) {
    int result = NotificationCompat.PRIORITY_DEFAULT;
    
    switch(Integer.parseInt(value)) {
      case 1: result = NotificationCompat.PRIORITY_MIN; break;
      case 2: result = NotificationCompat.PRIORITY_LOW; break;
      case 3: result = NotificationCompat.PRIORITY_HIGH; break;
      case 4: result = NotificationCompat.PRIORITY_MAX; break;
    }
    
    return result;
  }
}
