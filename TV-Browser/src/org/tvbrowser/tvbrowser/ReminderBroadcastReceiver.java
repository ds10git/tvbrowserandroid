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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.Notification;
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
import android.text.format.DateFormat;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
  public static final String tag = null;

  @Override
  public void onReceive(Context context, Intent intent) {
    PrefUtils.initialize(context);
    
    Logging.log(tag, "ReminderBroadcastReceiver.onReceive " + intent + " " + context, Logging.REMINDER_TYPE, context);
    long programID = intent.getLongExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, -1);
    
    Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' reminder is paused '" + SettingConstants.isReminderPaused(context) + "'", Logging.REMINDER_TYPE, context);
    
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
      
      Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' NIGHT MODE ACTIVATED '" + PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED, R.bool.pref_reminder_night_mode_activated_default) + "' sound '" + sound + "' vibrate '" + vibrate + "' led '" + led + "'", Logging.REMINDER_TYPE, context);
      
      if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_ACTIVATED, R.bool.pref_reminder_night_mode_activated_default)) {
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
        
        if(start <= minutes && minutes <= end) {
          showReminder = !PrefUtils.getBooleanValue(R.string.PREF_REMINDER_NIGHT_MODE_NO_REMINDER, R.bool.pref_reminder_night_mode_no_reminder_default);
          
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' CURRENTLY NIGHT MODE, Don't show '" + !showReminder + "'", Logging.REMINDER_TYPE, context);
          
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
          }
        }
      }
      
      if(PrefUtils.getBooleanValue(R.string.PREF_REMINDER_WORK_MODE_ACTIVATED, R.bool.pref_reminder_work_mode_activated_default)) {
        Calendar now = Calendar.getInstance();
        int[] values = getValuesForDay(now.get(Calendar.DAY_OF_WEEK));
        
        int minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        
        if(values[1] < values[0]) {
          if(minutes < values[0]) {
            minutes += 24 * 60;
          }
          
          values[1] += 24 * 60;
        }
        
        boolean isWorkMode = values[0] <= minutes && minutes <= values[1];
        
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
          
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' CURRENTLY WORK MODE, Don't show '" + !showReminder + "'", Logging.REMINDER_TYPE, context);
          
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
          }
        }
        
      }
      
      Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' showReminder '" + showReminder + "' sound '" + sound + "' vibrate '" + vibrate + "' led '" + led + "'", Logging.REMINDER_TYPE, context);
      
      if(showReminder) {
        Logging.log(tag,  new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' CONTEXT: " + context + " contentResolver: " + context.getContentResolver(), Logging.REMINDER_TYPE, context);
        Cursor values = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), SettingConstants.REMINDER_PROJECTION, null, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
        Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' Tried to load program with given ID, cursor size: " + values.getCount(), Logging.REMINDER_TYPE, context);
        if(values.moveToFirst()) {
          NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
          Program program = ProgramUtils.createProgramFromDataCursor(context, values);
          
          String channelName = program.getChannel().getChannelName();//values.getString(values.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
          String title = program.getTitle();//values.getString(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
          String episode = program.getEpisodeTitle();//values.getString(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
          
          long startTime = program.getStartTimeInUTC();//values.getLong(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
         // long endTime = values.getLong(values.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          Bitmap logo = UiUtils.createBitmapFromByteArray(/*values.getBlob(values.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO))*/program.getChannel().getIcon());
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' LOADED VALUES:  title '" + title + "' channelName '" + channelName + "' episode '" + episode + "' logo " + logo, Logging.REMINDER_TYPE, context);
          if(logo != null) {              
            int width =  context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
            int height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
            
            float scale = 1;
            
            if(logo.getWidth() > width-4) {
              scale = ((float)width-4)/logo.getWidth();
            }
            
            if(logo.getHeight() * scale > height-4) {
              scale = ((float)height-4)/logo.getHeight();
            }
            
            if(scale < 1) {
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
          
          if(sound) {
            builder.setSound(soundUri);
          }
          else {
            builder.setDefaults(0);
          }
          
          if(vibrate) {
            builder.setVibrate(new long[] {1000,200,1000,400,1000,600});
          }
          
          builder.setAutoCancel(true);
          builder.setContentInfo(channelName);
          
          if(led) {
            builder.setLights(Color.GREEN, 1000, 2000);
          }
          
          java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(context);
          String value = ((SimpleDateFormat)mTimeFormat).toLocalizedPattern();
          
          if((value.charAt(0) == 'H' && value.charAt(1) != 'H') || (value.charAt(0) == 'h' && value.charAt(1) != 'h')) {
            value = value.charAt(0) + value;
          }
          
          SimpleDateFormat timeFormat = new SimpleDateFormat(value, Locale.getDefault());
          
          builder.setContentTitle(timeFormat.format(new Date(startTime)) + " " + title);
          
          if(episode != null) {
            builder.setContentText(episode);
          }
          
          Intent startInfo = new Intent(context, InfoActivity.class);
          startInfo.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          startInfo.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
          startInfo.setAction("actionstring" + System.currentTimeMillis());
          
          builder.setContentIntent(PendingIntent.getActivity(context, 0, startInfo, 0));
          Notification notification = builder.build();
          
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' Create notification with intent: " + startInfo, Logging.REMINDER_TYPE, context);
          ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(title,(int)(startTime / 60000), notification);
          Logging.log(tag, new Date(System.currentTimeMillis()) + ": ProgramID for Reminder '" + programID + "' Notification was send.", Logging.REMINDER_TYPE, context);
          
          Intent broadcastProgram = new Intent(SettingConstants.PROGRAM_REMINDED_FOR);
          broadcastProgram.putExtra(SettingConstants.EXTRA_REMINDED_PROGRAM, program);
          
          context.sendBroadcast(broadcastProgram,"org.tvbrowser.permission.RECEIVE_PROGRAMS");
        }
        
        values.close();
      }
    }
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

}
