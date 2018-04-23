package org.tvbrowser.job;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Logging;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.TvDataUpdateService;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class JobDataUpdateAuto extends Job {
  public static final String TAG = "job_data_update_auto";

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    Log.d("info9","onRunJob");
    Result result = Result.FAILURE;
    final String updateType = PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default);

    boolean autoUpdate = !updateType.equals("0");
    final boolean internetConnectionType = updateType.equals("1");
    boolean timeUpdateType = updateType.equals("2");

    if(autoUpdate && !TvDataUpdateService.isRunning() && IOUtils.isBatterySufficient(getContext())) {
      Intent startDownload = new Intent(getContext(), TvDataUpdateService.class);
      startDownload.putExtra(TvDataUpdateService.KEY_TYPE, TvDataUpdateService.TYPE_TV_DATA);
      startDownload.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startDownload.putExtra(SettingConstants.EXTRA_DATA_UPDATE_TYPE, TvDataUpdateService.TYPE_UPDATE_AUTO);
      startDownload.putExtra(SettingConstants.EXTRA_DATA_UPDATE_TYPE_INTERNET_CONNECTION, internetConnectionType);

      int daysToDownload = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_RANGE, R.string.pref_auto_update_range_default));

      startDownload.putExtra(getContext().getString(R.string.DAYS_TO_DOWNLOAD), daysToDownload);

      Logging.openLogForDataUpdate(getContext().getApplicationContext());
      Logging.log(TAG, "UPDATE START INTENT " + startDownload, Logging.TYPE_DATA_UPDATE, getContext().getApplicationContext());
      Logging.closeLogForDataUpdate();

      if(CompatUtils.startForegroundService(getContext(),startDownload)) {
        result = Result.SUCCESS;
      }
    }

    return result;
  }

  public static void startNow(final Context context) {
    new JobRequest.Builder(TAG).startNow().build().schedule();
  }

  public static void scheduleJob(final Context context) {
    final String updateType = PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default);
    final boolean onlyWifi = PrefUtils.getBooleanValue(R.string.PREF_AUTO_UPDATE_ONLY_WIFI, R.bool.pref_auto_update_only_wifi_default);

    boolean autoUpdate = !updateType.equals("0");
    final boolean internetConnectionType = updateType.equals("1");
    boolean timeUpdateType = updateType.equals("2");
    Log.d("info9","canceled: " + JobManager.instance().cancelAllForTag(TAG));

    if(autoUpdate) {
      JobRequest.Builder builder = new JobRequest.Builder(TAG)
          .setRequiresBatteryNotLow(true);
      long timeCurrent = PrefUtils.getLongValue(R.string.AUTO_UPDATE_CURRENT_START_TIME,0);

      if(timeUpdateType) {
        int days = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default));
        int time = PrefUtils.getIntValue(R.string.PREF_AUTO_UPDATE_START_TIME, R.integer.pref_auto_update_start_time_default);

        Log.d("info9","TIME: "+time+" "+days+" " + new Date(timeCurrent));
        if(timeCurrent == 0 || timeCurrent < System.currentTimeMillis()+1000) {
          if (PrefUtils.getStringValue(R.string.PREF_EPGPAID_USER, "").trim().length() > 0 &&
              PrefUtils.getStringValue(R.string.PREF_EPGPAID_PASSWORD, "").trim().length() > 0) {
            Calendar test = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            test.set(Calendar.SECOND, 0);
            test.set(Calendar.MILLISECOND, 0);

            int timeTest = time;

            do {
              timeTest = time + ((int) (Math.random() * 6 * 60));
              test.set(Calendar.HOUR_OF_DAY, timeTest / 60);
              test.set(Calendar.MINUTE, timeTest % 60);
            } while (test.get(Calendar.HOUR_OF_DAY) >= 23 || test.get(Calendar.HOUR_OF_DAY) < 4 ||
                (test.get(Calendar.HOUR_OF_DAY) >= 15 && test.get(Calendar.HOUR_OF_DAY) < 17));

            time = timeTest;
          } else {
            time += ((int) (Math.random() * 6 * 60));
          }

          final Calendar now = Calendar.getInstance();
          int currentTime = now.get(Calendar.HOUR_OF_DAY)*60 + now.get(Calendar.MINUTE);

          if (currentTime > time) {
            time = time + 24 * 60;
          }

          Log.d("info9","currentTime: " + currentTime);

          time = Math.max((time - currentTime) + (days * 24 * 60),1);
         // time = 1;
          long end = (time + 60) * 60000L;
         // end = (time + 1) * 60000L;
          PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putLong(context.getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), System.currentTimeMillis() + (time * 60000L)).commit();
          Log.d("info9", "START " + new Date(System.currentTimeMillis() + (time * 60000L)) + " END " + new Date(System.currentTimeMillis() + end));
          builder.setExecutionWindow(time * 60000L, end);
        }
        else {
          builder.setExecutionWindow(timeCurrent-System.currentTimeMillis(),timeCurrent-System.currentTimeMillis()+60000L);
        }
      }
      else {
        long possibleFirst = PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE,0) + 12 * 60 * 60000L;
Log.d("info9","possibleFirst " + new Date(possibleFirst));
        long start = 30000L;

        if(possibleFirst > System.currentTimeMillis()) {
          start += possibleFirst - System.currentTimeMillis();
        }

        long end = start + 60*60000L;

        if(timeCurrent + 60*60000L < end && timeCurrent > System.currentTimeMillis()+60000L) {
          end = timeCurrent - System.currentTimeMillis();
        }
        else {
          PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putLong(context.getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), System.currentTimeMillis() + start).commit();
        }
        Log.d("info9","NET START " +new Date(System.currentTimeMillis()+start) + " END " + new Date(System.currentTimeMillis()+end));
        builder.setExecutionWindow(start,end);
      }

      if(onlyWifi) {
        builder.setRequiredNetworkType(JobRequest.NetworkType.UNMETERED);
      }

      builder.setRequirementsEnforced(true);

      int jobId = builder.build().schedule();
Log.d("info9","jobID " + jobId);
      PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putInt(context.getString(R.string.PREF_AUTO_UPDATE_JOB_ID),jobId).commit();
    }
  }

  public static void cancelJob(final Context context) {
    Log.d("info9","cancelJob");

    final String prefKey = context.getString(R.string.PREF_AUTO_UPDATE_JOB_ID);
    final SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context);

    int jobId = pref.getInt(prefKey,-1);

    if(jobId != -1) {
      JobManager.instance().cancel(jobId);
      pref.edit().remove(prefKey).commit();
    }
  }
}
