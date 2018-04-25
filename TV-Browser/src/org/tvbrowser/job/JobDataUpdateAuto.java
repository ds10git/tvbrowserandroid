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
    long lastUpdate = PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE,0);

    if(autoUpdate) {
      JobRequest.Builder builder = new JobRequest.Builder(TAG)
          .setRequiresBatteryNotLow(true);
      long timeCurrent = PrefUtils.getLongValue(R.string.AUTO_UPDATE_CURRENT_START_TIME,0);

      if(timeUpdateType) {
        final Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastUpdate);

        int days = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default));
        int time = PrefUtils.getIntValue(R.string.PREF_AUTO_UPDATE_START_TIME, R.integer.pref_auto_update_start_time_default);

        last.add(Calendar.DAY_OF_YEAR,days+1);

        Log.d("info9","TIME: "+time+" "+days+" " + new Date(timeCurrent));
        if(timeCurrent == 0 || timeCurrent < System.currentTimeMillis()+1000 || ((System.currentTimeMillis() - lastUpdate) < 12*60*60000L) && (timeCurrent < System.currentTimeMillis() + 60 * 60000L)) {
          if (PrefUtils.getStringValue(R.string.PREF_EPGPAID_USER, "").trim().length() > 0 &&
              PrefUtils.getStringValue(R.string.PREF_EPGPAID_PASSWORD, "").trim().length() > 0 &&
              PrefUtils.getLongValue(R.string.PREF_EPGPAID_ACCESS_UNTIL,R.integer.pref_epgpaid_access_until_default) >
              System.currentTimeMillis()) {
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

          last.set(Calendar.HOUR_OF_DAY, time / 60);
          last.set(Calendar.MINUTE, time % 60);
          last.set(Calendar.SECOND, 0);
          last.set(Calendar.MILLISECOND, 0);

          long currentTime = System.currentTimeMillis();

          if(last.getTimeInMillis() < currentTime) {
            last.setTimeInMillis(currentTime + 60000L * 30);
          }

        //  time = Math.max((time - currentTime) + (days * 24 * 60),1);
         // time = 1;
          long end = (last.getTimeInMillis()-currentTime) + 60 * 60000L;
         // end = (time + 1) * 60000L;
          PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).edit().putLong(context.getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), last.getTimeInMillis()).commit();
          Log.d("info9", "START " + last.getTime() + " END " + new Date(currentTime + end));
          builder.setExecutionWindow((last.getTimeInMillis()-currentTime), end);
        }
        else {
          builder.setExecutionWindow(timeCurrent-System.currentTimeMillis(),timeCurrent-System.currentTimeMillis()+60000L);
        }
      }
      else {
        long possibleFirst = lastUpdate + 12 * 60 * 60000L;
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
