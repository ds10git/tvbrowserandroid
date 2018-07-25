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
package org.tvbrowser.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.job.JobDataUpdateAuto;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Logging;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.ReminderBroadcastReceiver;
import org.tvbrowser.tvbrowser.ServiceUpdateDataTable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

/**
 * Helper class to supper IO.
 * <p>
 * @author René Mach
 */
public final class IOUtils {
  private static final int REQUEST_CODE_DATA_TABLE_UPDATE = 1235;
  private static final float MIN_BATTERY_LEVEL = 0.1f;

  IOUtils() {}

  /**
   * Creates an integer value from the given byte array.
   * <p>
   * @param value The byte array to convert (Big-Endian).
   * @return The calculated integer value.
   */
  public static int getIntForBytes(byte[] value) {
    int count = value.length - 1;
    
    int result = 0;
    
    for(byte b : value) {
      result = result | ((((int)b) & 0xFF) << (count * 8));
      
      count--;
    }
    
    return result;
  }
  
  private static final int INFO_BLACK_AND_WHITE = 1 << 1;
  private static final int INFO_BLACK_FOUR_TO_THREE = 1 << 2;
  private static final int INFO_BLACK_SIXTEEN_TO_NINE = 1 << 3;
  private static final int INFO_BLACK_MONO = 1 << 4;
  private static final int INFO_BLACK_STEREO = 1 << 5;
  private static final int INFO_BLACK_DOLBY_SURROUND = 1 << 6;
  private static final int INFO_BLACK_DOLBY_DIGITAL = 1 << 7;
  private static final int INFO_BLACK_SECOND_AUDIO_PROGRAM = 1 << 8;
  private static final int INFO_BLACK_SECOND_CLOSED_CAPTION = 1 << 9;
  private static final int INFO_BLACK_SECOND_LIVE = 1 << 10;
  private static final int INFO_BLACK_SECOND_OMU = 1 << 11;
  private static final int INFO_BLACK_SECOND_FILM = 1 << 12;
  private static final int INFO_BLACK_SECOND_SERIES = 1 << 13;
  private static final int INFO_BLACK_SECOND_NEW = 1 << 14;
  private static final int INFO_BLACK_SECOND_AUDIO_DESCRIPTION = 1 << 15;
  private static final int INFO_BLACK_SECOND_NEWS = 1 << 16;
  private static final int INFO_BLACK_SECOND_SHOW = 1 << 17;
  private static final int INFO_BLACK_SECOND_MAGAZINE = 1 << 18;
  private static final int INFO_BLACK_SECOND_HD = 1 << 19;
  private static final int INFO_BLACK_SECOND_DOCU = 1 << 20;
  private static final int INFO_BLACK_SECOND_ART = 1 << 21;
  private static final int INFO_BLACK_SECOND_SPORT = 1 << 22;
  private static final int INFO_BLACK_SECOND_CHILDREN = 1 << 23;
  private static final int INFO_BLACK_SECOND_OTHER = 1 << 24;
  private static final int INFO_BLACK_SECOND_SIGN_LANGUAGE = 1 << 25;
  
  public static final int[] INFO_CATEGORIES_ARRAY = {
    INFO_BLACK_AND_WHITE,
    INFO_BLACK_FOUR_TO_THREE,
    INFO_BLACK_SIXTEEN_TO_NINE,
    INFO_BLACK_MONO,
    INFO_BLACK_STEREO,
    INFO_BLACK_DOLBY_SURROUND,
    INFO_BLACK_DOLBY_DIGITAL,
    INFO_BLACK_SECOND_AUDIO_PROGRAM,
    INFO_BLACK_SECOND_CLOSED_CAPTION,
    INFO_BLACK_SECOND_LIVE,
    INFO_BLACK_SECOND_OMU,
    INFO_BLACK_SECOND_FILM,
    INFO_BLACK_SECOND_SERIES,
    INFO_BLACK_SECOND_NEW,
    INFO_BLACK_SECOND_AUDIO_DESCRIPTION,
    INFO_BLACK_SECOND_NEWS,
    INFO_BLACK_SECOND_SHOW,
    INFO_BLACK_SECOND_MAGAZINE,
    INFO_BLACK_SECOND_HD,
    INFO_BLACK_SECOND_DOCU,
    INFO_BLACK_SECOND_ART,
    INFO_BLACK_SECOND_SPORT,
    INFO_BLACK_SECOND_CHILDREN,
    INFO_BLACK_SECOND_OTHER,
    INFO_BLACK_SECOND_SIGN_LANGUAGE,
  };
  
  public static String[] getInfoStringArrayNames(Resources res) {
    return new String[]{
        res.getString(R.string.info_black_and_white),
        res.getString(R.string.info_four_to_three),
        res.getString(R.string.info_sixteen_to_nine),
        res.getString(R.string.info_mono),
        res.getString(R.string.info_stereo),
        res.getString(R.string.info_dolby_sourround),
        res.getString(R.string.info_dolby_digital),
        res.getString(R.string.info_second_audio_program),
        res.getString(R.string.info_closed_caption),
        res.getString(R.string.info_live),
        res.getString(R.string.info_omu),
        res.getString(R.string.info_film),
        res.getString(R.string.info_series),
        res.getString(R.string.info_new),
        res.getString(R.string.info_audio_description),
        res.getString(R.string.info_news),
        res.getString(R.string.info_show),
        res.getString(R.string.info_magazin),
        res.getString(R.string.info_hd),
        res.getString(R.string.info_docu),
        res.getString(R.string.info_art),
        res.getString(R.string.info_sport),
        res.getString(R.string.info_children),
        res.getString(R.string.info_other),
        res.getString(R.string.info_sign_language)
    };
  }
  
  public static boolean infoSet(int categories, int info) {
    return ((categories & info) == info);
  }
  
  public static Spannable getInfoString(int value, Resources res) {
    return getInfoString(value, res, true);
  }
  
  private static int getDefaultCategoryColorKeyForColorKey(int colorKey) {
    int defaultColorCategoryKey = R.string.pref_color_categories_default;

    switch (colorKey) {
      case R.string.PREF_COLOR_CATEGORY_FILM:
        defaultColorCategoryKey = R.string.pref_color_category_film_default;
        break;
      case R.string.PREF_COLOR_CATEGORY_SERIES:
        defaultColorCategoryKey = R.string.pref_color_category_series_default;
        break;
      case R.string.PREF_COLOR_CATEGORY_NEW:
        defaultColorCategoryKey = R.string.pref_color_category_new_default;
        break;
      case R.string.PREF_COLOR_CATEGORY_DOCU:
      case R.string.PREF_COLOR_CATEGORY_MAGAZIN:
        defaultColorCategoryKey = R.string.pref_color_category_docu_default;
        break;
      case R.string.PREF_COLOR_CATEGORY_CHILDREN:
        defaultColorCategoryKey = R.string.pref_color_category_children_default;
        break;
      case R.string.PREF_COLOR_CATEGORY_SHOW:
        defaultColorCategoryKey = R.string.pref_color_category_show_default;
        break;
    }
    
    return defaultColorCategoryKey;
  }
  
  public static HashMap<String,Integer> loadCategoryColorMap(Context context) {
    HashMap<String, Integer> categoryColorMap = new HashMap<>();
    String[] names = getInfoStringArrayNames(context.getResources());
    
    for(int i = 0; i < SettingConstants.CATEGORY_COLOR_PREF_KEY_ARR.length; i++) {
      int colorKey = SettingConstants.CATEGORY_COLOR_PREF_KEY_ARR[i];
      
      int[] colorCategory = getActivatedColorFor(PrefUtils.getStringValue(colorKey, getDefaultCategoryColorKeyForColorKey(colorKey)));
      
      if(colorCategory[0] == 1) {
        categoryColorMap.put(names[i], colorCategory[1]);
      }
    }
        
    return categoryColorMap;
  }
  
  public static Spannable getInfoString(int value, Resources res, boolean colored) {
    return getInfoString(value, res, colored, null);
  }
  
  public static Spannable getInfoString(int value, Resources res, boolean colored, Integer defaultColor) {
    String[] valueArr = {"",
        res.getString(R.string.info_black_and_white),
        res.getString(R.string.info_four_to_three),
        res.getString(R.string.info_sixteen_to_nine),
        res.getString(R.string.info_mono),
        res.getString(R.string.info_stereo),
        res.getString(R.string.info_dolby_sourround),
        res.getString(R.string.info_dolby_digital),
        res.getString(R.string.info_second_audio_program),
        res.getString(R.string.info_closed_caption),
        res.getString(R.string.info_live),
        res.getString(R.string.info_omu),
        res.getString(R.string.info_film),
        res.getString(R.string.info_series),
        res.getString(R.string.info_new),
        res.getString(R.string.info_audio_description),
        res.getString(R.string.info_news),
        res.getString(R.string.info_show),
        res.getString(R.string.info_magazin),
        res.getString(R.string.info_hd),
        res.getString(R.string.info_docu),
        res.getString(R.string.info_art),
        res.getString(R.string.info_sport),
        res.getString(R.string.info_children),
        res.getString(R.string.info_other),
        res.getString(R.string.info_sign_language)
        };
    
    int[] prefKeyArr = SettingConstants.CATEGORY_PREF_KEY_ARR;
    int[] colorPrefKeyArr = SettingConstants.CATEGORY_COLOR_PREF_KEY_ARR;
        
    SpannableStringBuilder infoString = new SpannableStringBuilder();
    
    for(int i = 1; i <= 25; i++) {
      if((value & (1 << i)) == (1 << i) && PrefUtils.getBooleanValue(prefKeyArr[i-1], R.bool.pref_info_show_default)) {
        if(infoString.length() > 0) {
          infoString.append(", ");
          
          if(defaultColor != null) {
            infoString.setSpan(new ForegroundColorSpan(defaultColor), infoString.length()-2, infoString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        }
        infoString.append(valueArr[i]);
        
        if(colored) {
          int[] colorCategory = getActivatedColorFor(PrefUtils.getStringValue(colorPrefKeyArr[i-1], getDefaultCategoryColorKeyForColorKey(colorPrefKeyArr[i-1])));
          
          Integer color = defaultColor;
          
          if(colorCategory[0] == 1) {
            color = colorCategory[1];
          }
          
          if(color != null) {
            infoString.setSpan(new ForegroundColorSpan(color), infoString.length()-valueArr[i].length(), infoString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }          
        }
      }
    }
    
    if(infoString.length() > 0) {
      return infoString;
    }
    
    return null;
  }
  /*
  public static SpannableString setColor(SpannableString categories, String value, int color, boolean foreground) {
    int index = categories.toString().indexOf(value);
    
    if(index != -1) {
      categories.setSpan(foreground ? new ForegroundColorSpan(color) : new BackgroundColorSpan(color), index, index+value.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    
    return categories;
  }*/
  
  public static byte[] loadUrl(String urlString) throws TimeoutException {
    return loadUrl(urlString, 30000);
  }
  
  public static byte[] loadUrl(final String urlString, final int timeout) throws TimeoutException {
    final AtomicInteger count = new AtomicInteger(0);
    final AtomicReference<byte[]> loadData = new AtomicReference<>(null);
    
    new Thread("LOAD URL THREAD") {
      public void run() {
        FileOutputStream fout = null;
        
        try {
          byte[] byteArr = loadUrl(urlString, count);
          
          loadData.set(byteArr);
        }
        catch(IOException ignored) {
        }
        finally {
          close(fout);
        }
      }
    }.start();
    
    Thread wait = new Thread("SAVE URL WAITING THREAD") {
      public void run() {
        while(loadData.get() == null && count.getAndIncrement() < (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException ignored) {}
        }
      }
    };
    wait.start();
    
    try {
      wait.join();
    } catch (InterruptedException ignored) {}
    
    if(loadData.get() == null) {
      throw new TimeoutException("URL '"+urlString+"' could not be saved.");
    }
    
    return loadData.get();
  }
  
  private static byte[] loadUrl(final String urlString, final AtomicInteger timeoutCount) throws IOException {
    BufferedInputStream in = null;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    URLConnection connection = null;
    try {
      connection = new URL(urlString).openConnection();
      IOUtils.setConnectionTimeout(connection,15000);
      
      if(urlString.toLowerCase(Locale.US).endsWith(".gz")) {
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
      }
      
      in = new BufferedInputStream(connection.getInputStream());
      
      byte temp[] = new byte[1024];
      int count;
      
      while ((count = in.read(temp, 0, 1024)) != -1) {
        if(temp != null && count > 0) {
          out.write(temp, 0, count);
          
          if(timeoutCount != null) {
            timeoutCount.set(0);
          }
        }
      }
    } 
    finally {
      close(in);
      disconnect(connection);
    }

    return out.toByteArray();
  }
  
  /**
   * Save given URL to filename with a default timeout of 30 seconds.
   * <p>
   * @param filename The file to save to.
   * @param urlString The URL to load from.
   * <p> 
   * @return <code>true</code> if the file was downloaded successfully, <code>false</code> otherwise.
   */
  public static boolean saveUrl(final String filename, final String urlString) {
    return saveUrl(filename, urlString, 30000);
  }
  
  /**
   * Save given URL to filename.
   * <p>
   * @param filename The file to save to.
   * @param urlString The URL to load from.
   * @param timeout The timeout of the download in milliseconds.
   * <p> 
   * @return <code>true</code> if the file was downloaded successfully, <code>false</code> otherwise.
   */
  public static boolean saveUrl(final String filename, final String urlString, final int timeout) {
    final AtomicBoolean wasSaved = new AtomicBoolean(false);
    final AtomicInteger count = new AtomicInteger(0);
    
    new Thread("SAVE URL THREAD") {
      public void run() {
        FileOutputStream fout = null;
        
        try {
          byte[] byteArr = loadUrl(urlString, count);
          
          fout = new FileOutputStream(filename);
          fout.getChannel().truncate(0);
          fout.write(byteArr, 0, byteArr.length);
          fout.flush();
          
          wasSaved.set(true);
        }
        catch(IOException ignored) {
        }
        finally {
          close(fout);
        }
      }
    }.start();
    
    Thread wait = new Thread("SAVE URL WAITING THREAD") {
      public void run() {
        while(!wasSaved.get() && count.getAndIncrement() < (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException ignored) {}
        }
      }
    };
    wait.start();
        
    try {
      wait.join();
    } catch (InterruptedException e) {
      Log.d("info51", "INTERRUPTED", e);
    }
    
    return wasSaved.get();
  }
  
  /**
   * Save given URL to filename with a default timeout of 30 seconds.
   * <p>
   * @param filename The file to save to.
   * @param in The input stream to load from.
   * <p> 
   * @return <code>true</code> if the file was downloaded successfully, <code>false</code> otherwise.
   */
  public static boolean saveStream(final String filename, final InputStream in) {
    return saveStream(filename, in, 30000);
  }
  
  /**
   * Save given URL to filename.
   * <p>
   * @param filename The file to save to.
   * @param in The input stream to load from.
   * @param timeout The timeout of the download in milliseconds.
   * <p> 
   * @return <code>true</code> if the file was downloaded successfully, <code>false</code> otherwise.
   */
  private static boolean saveStream(final String filename, final InputStream in, final int timeout) {
    final AtomicBoolean wasSaved = new AtomicBoolean(false);
    final AtomicInteger count = new AtomicInteger(0);
    
    new Thread("SAVE URL THREAD") {
      public void run() {
        FileOutputStream fout = null;
        
        try {
          fout = new FileOutputStream(filename);
          fout.getChannel().truncate(0);
          
          byte[] buffer = new byte[10240];
          int len = 0;
          
          while((len = in.read(buffer)) >= 0) {
            fout.write(buffer, 0, len);
          }
          
          fout.flush();
          
          wasSaved.set(true);
        }
        catch(IOException ignored) {
        }
        finally {
          close(fout);
        }
      }
    }.start();
    
    Thread wait = new Thread("SAVE URL WAITING THREAD") {
      public void run() {
        while(!wasSaved.get() && count.getAndIncrement() < (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException ignored) {}
        }
      }
    };
    wait.start();
        
    try {
      wait.join();
    } catch (InterruptedException e) {
      Log.d("info51", "INTERRUPTED", e);
    }
    
    return wasSaved.get();
  }
  
  /*
   * Copied from http://stackoverflow.com/questions/4818468/how-to-check-if-inputstream-is-gzipped and changed.
   * No license given on page.
   */
  public static InputStream decompressStream(InputStream input) throws IOException {
    PushbackInputStream pb = new PushbackInputStream( input, 2 ); //we need a pushbackstream to look ahead
    
    byte [] signature = new byte[2];
    int read = pb.read( signature ); //read the signature
    
    if(read == 2) {
      pb.unread( signature ); //push back the signature to the stream
    }
    else if(read == 1) {
      pb.unread(signature[0]);
    }
    
    if(signature[ 0 ] == (byte) (GZIPInputStream.GZIP_MAGIC & 0xFF) && signature[ 1 ] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8) ) {//check if matches standard gzip magic number
      return decompressStream(new GZIPInputStream(pb));
    }
    else {
      return pb;
    }
  }
  
  public static byte[] getCompressedData(byte[] uncompressed) {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    GZIPOutputStream out = null;
    try {
      out = new GZIPOutputStream(bytesOut);
      
      // SEND THE IMAGE
      int index = 0;
      int size = 1024;
      do {
          if ((index + size) > uncompressed.length) {
              size = uncompressed.length - index;
          }
          out.write(uncompressed, index, size);
          index += size;
      } while (index < uncompressed.length);
      
      out.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    finally {
    	close(out);
    }

    return bytesOut.toByteArray();
  }
  
  public static synchronized void setDataUpdateTime(Context context, long time, SharedPreferences pref) {
    JobDataUpdateAuto.scheduleJob(context,true);
    /*AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent dataUpdate = new Intent(context, AutoDataUpdateReceiver.class);
    dataUpdate.putExtra(SettingConstants.TIME_DATA_UPDATE_EXTRA, true);
    
    if(time != 0) {
      PendingIntent pending = PendingIntent.getBroadcast(context, DATA_UPDATE_KEY, dataUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
      
      CompatUtils.setAlarmInexact(alarmManager,AlarmManager.RTC_WAKEUP, Math.max(System.currentTimeMillis()+28*60000,time), pending);
    }*/
  }
  
  public static synchronized void removeDataUpdateTime(Context context, SharedPreferences pref) {
    JobDataUpdateAuto.cancelJob(context);
    /*AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent dataUpdate = new Intent(context, AutoDataUpdateReceiver.class);
    
    PendingIntent pending = PendingIntent.getBroadcast(context, DATA_UPDATE_KEY, dataUpdate, PendingIntent.FLAG_NO_CREATE);
    
    if(pending != null) {
      alarmManager.cancel(pending);
    }*/
  }
  
  public static void setDataTableRefreshTime(Context context) {
    Calendar now = Calendar.getInstance();
    
    now.add(Calendar.DAY_OF_YEAR, 1);
    now.set(Calendar.HOUR_OF_DAY, 0);
    now.set(Calendar.MINUTE, 0);
    now.set(Calendar.SECOND, 5);
    now.set(Calendar.MILLISECOND, 0);
    
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent dataUpdate = new Intent(context, ServiceUpdateDataTable.class);
    
    if(now.getTimeInMillis() < System.currentTimeMillis()) {
      now.add(Calendar.SECOND, 10);
    }
    
    PendingIntent pending = PendingIntent.getService(context, REQUEST_CODE_DATA_TABLE_UPDATE, dataUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
    
    CompatUtils.setExactAlarmAndAllowWhileIdle(alarmManager,AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pending);
  }
  
  public static synchronized void handleDataUpdatePreferences(Context context) {
    handleDataUpdatePreferences(context,false);
  }
  
  public static synchronized void handleDataUpdatePreferences(Context context, boolean fromNow) {
    JobDataUpdateAuto.scheduleJob(context);
    /*SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    IOUtils.removeDataUpdateTime(context, pref);
    
    if(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("2")) {
      int days = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default)) + 1;
      int time = PrefUtils.getIntValue(R.string.PREF_AUTO_UPDATE_START_TIME, R.integer.pref_auto_update_start_time_default);
      long current = PrefUtils.getLongValue(R.string.AUTO_UPDATE_CURRENT_START_TIME, R.integer.auto_update_current_start_time_default);

      if(current < System.currentTimeMillis()) {
        long lastDate = PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE, R.integer.last_data_update_default);
             
        if(lastDate == 0) {
          lastDate = (System.currentTimeMillis() - (24 * 60 * 60000));
          
          Editor edit = pref.edit();
          edit.putLong(context.getString(R.string.LAST_DATA_UPDATE), lastDate);
          edit.commit();
        }
        
        if(fromNow) {
          current = System.currentTimeMillis();
        }
        
        if(PrefUtils.getStringValue(R.string.PREF_EPGPAID_USER, "").trim().length() > 0 && 
            PrefUtils.getStringValue(R.string.PREF_EPGPAID_PASSWORD, "").trim().length() > 0) {
          Calendar test = Calendar.getInstance(TimeZone.getTimeZone("CET"));
          test.set(Calendar.SECOND, 0);
          test.set(Calendar.MILLISECOND, 0);
          
          int timeTest = time;
          
          do {
            timeTest = time + ((int)(Math.random() * 6 * 60));
            test.set(Calendar.HOUR_OF_DAY, timeTest/60);
            test.set(Calendar.MINUTE, timeTest%60);
          }while(test.get(Calendar.HOUR_OF_DAY) >= 23 || test.get(Calendar.HOUR_OF_DAY) < 4 ||
              (test.get(Calendar.HOUR_OF_DAY) >= 15 && test.get(Calendar.HOUR_OF_DAY) < 17));
          
          time = timeTest;
        }
        else {
          time += ((int)(Math.random() * 6 * 60));
        }
        
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastDate);
        
        last.add(Calendar.DAY_OF_YEAR, days);
        last.set(Calendar.HOUR_OF_DAY, time/60);
        last.set(Calendar.MINUTE, time%60);
        last.set(Calendar.SECOND, 0);
        last.set(Calendar.MILLISECOND, 0);
        
        if(last.getTimeInMillis() < System.currentTimeMillis()) {
          last.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
          
          if(last.get(Calendar.YEAR) < Calendar.getInstance().get(Calendar.YEAR)) {
            last.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
          }
        }
        
        current = last.getTimeInMillis();
        
        if(current < System.currentTimeMillis()) {
          current += (24 * 60 * 60000);
        }
        
        current = Math.max(System.currentTimeMillis()+30*60000, current);
        
        Editor currentTime = PreferenceManager.getDefaultSharedPreferences(context).edit();
        currentTime.putLong(context.getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), current);
        currentTime.commit();
      }
      
      IOUtils.setDataUpdateTime(context, current, pref);
    }*/
  }
  
  public static String[] getStringArrayFromList(ArrayList<String> list) {
    if(list != null) {
      return list.toArray(new String[0]);
    }
    
    return null;
  }
  
  public static boolean isConnectedToServer(final String url, final int timeout) {
    final AtomicBoolean isConnected = new AtomicBoolean(false);
    
    new Thread("NETWORK CONNECTION CHECK THREAD") {
      public void run() {
    	  URLConnection connection = null;
        try {
          URL myUrl = new URL(url);
          
          connection = myUrl.openConnection();
          IOUtils.setConnectionTimeout(connection,timeout);
          
          HttpURLConnection httpConnection = (HttpURLConnection)connection;
          
          if(httpConnection != null) {
            int responseCode = httpConnection.getResponseCode();
          
            isConnected.set(responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED
                || responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpsURLConnection.HTTP_SEE_OTHER);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        finally  {
          disconnect(connection);
        }
      }
    }.start();
    
    Thread check = new Thread("WAITING FOR NETWORK CONNECTION THREAD") {
      @Override
      public void run() {
        int count = 0;
        while(!isConnected.get() && count++ <= (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException ignored) {}
        }
      }
    };
    check.start();
        
    try {
      check.join();
    } catch (InterruptedException ignored) {}
    
    return isConnected.get();
  }
  
  public static void setConnectionTimeoutDefault(URLConnection connection) {
    setConnectionTimeout(connection, 10000);
  }
  
  public static void setConnectionTimeout(URLConnection connection, int timeout) {
    connection.setReadTimeout(timeout);
    connection.setConnectTimeout(timeout);
  }
  
  /**
   * Normalize time of given Calendar to 2014-12-31 with the given time.
   * <p>
   * @param cal The Calendar to normalize.
   * @param minutesAfterMidnight The minutes after midnight to use.
   * @return The normalized Calendar.
   */
  public static Calendar normalizeTime(Calendar cal, int minutesAfterMidnight) {
    return normalizeTime(cal, minutesAfterMidnight, 0);
  }
  
  /**
   * Normalize time of given Calendar to 2014-12-31 with the given time.
   * <p>
   * @param cal The Calendar to normalize.
   * @param minutesAfterMidnight The minutes after midnight to use.
   * @param seconds The seconds to use
   * @return The normalized Calendar.
   */
  public static Calendar normalizeTime(Calendar cal, int minutesAfterMidnight, int seconds) {
    return normalizeTime(cal, minutesAfterMidnight / 60, minutesAfterMidnight % 60, seconds);
  }
  
  /**
   * Normalize time of given Calendar to 2014-12-31 with the hourOfDay and minutes.
   * <p>
   * @param cal The Calendar to normalize.
   * @param hourOfDay The hour of day to use.
   * @param minutes The minutes to use.
   * @return The normalized Calendar.
   */
  public static Calendar normalizeTime(Calendar cal, int hourOfDay, int minutes, int seconds) {
    cal.set(2014, Calendar.DECEMBER, 31, hourOfDay, minutes, seconds);
    
    return cal;
  }

  /**
   * Closes the given object and releases assigned resources.
   * Calls are <code>null</code>-safe and idempotent.
   * 
   * @param closeable the object to close.
   * @see Closeable
   */
  public static void close(final Closeable closeable) {
    if (closeable != null) {
      try {
        // Needs reflection to be compatible with Android 4.0
        Method close = closeable.getClass().getMethod("close");
        
        if(close != null) {
          close.invoke(closeable);
        }
      }
      catch (final Exception ignored) {
        // intentionally ignored
      }
    }
  }

  /**
   * Closes the given database cursor, releases assigned resources, and makes the cursor invalid.
   * A call to {@link Cursor#requery()} will not make the cursor valid again.
   * Calls are <code>null</code>-safe and idempotent.
   *
   * @param cursor the {@link Cursor} to close.
   * @see Cursor#close()
   */
  public static void close(final Cursor cursor) {
    if (cursor!=null && !cursor.isClosed()) {
      cursor.close();
    }
  }

  /**
   * Closes the given content provider client and releases all assigned resources.
   * <p>
   * Calls are <code>null</code>-safe and idempotent.
   *
   * @param client the {@link ContentProviderClient} to close.
   * @see ContentProviderClient#close()
   */
  @SuppressWarnings("deprecation")
  public static void close(@Nullable final ContentProviderClient client) {
    if (client != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        client.release();
      } else {
        client.close();
      }
    }
  }

  /**
   * Disconnects the given connection and releases or reuses associated resources.
   * Calls are <code>null</code>-safe and idempotent.
   * <p/>
   * Note: the underlying connection must be inherited from {@link HttpURLConnection}.
   * 
   * @param connection the connection to release.
   * @see HttpURLConnection#disconnect()
   */
  public static void disconnect(final URLConnection connection) {
    if (connection instanceof HttpURLConnection) {
      try {
        ((HttpURLConnection) connection).disconnect();
      } catch (final Exception ignored) {
        // intentionally ignored
      }
    }
  }

  public static List<Channel> getChannelList(Context context) {
    ArrayList<Channel> channelList = new ArrayList<>();
    
    if(IOUtils.isDatabaseAccessible(context)) {
      final long token = Binder.clearCallingIdentity();
      final Cursor channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.KEY_ID);
      
      try {
        if(IOUtils.prepareAccess(channels)) {
          int keyColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
          int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
          int iconColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
          
          while(channels.moveToNext()) {
            channelList.add(new Channel(channels.getInt(keyColumn), channels.getString(nameColumn), channels.getBlob(iconColumn)));
          }
        }
      }finally {
        IOUtils.close(channels);
        Binder.restoreCallingIdentity(token);
      }
    }
    return channelList;
  }

  private static final int TYPE_DOWNLOAD_DIRECTORY_DATA = 0;
  public static final int TYPE_DOWNLOAD_DIRECTORY_OTHER = 1;
  public static final int TYPE_DOWNLOAD_DIRECTORY_LOG = 2;

  public static File getDownloadDirectory(Context context) {
    return getDownloadDirectory(context,TYPE_DOWNLOAD_DIRECTORY_DATA);
  }

  public static File getDownloadDirectory(Context context, int type) {
    File parent = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    boolean external = true;
    
    if(parent == null || !parent.isDirectory()) {
      external = false;
      parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
    }

    String subdirectory = null;

    switch (type) {
      case TYPE_DOWNLOAD_DIRECTORY_OTHER:
        subdirectory = "other";
        break;
      case TYPE_DOWNLOAD_DIRECTORY_LOG:
        subdirectory = "log";
        break;

      default: subdirectory = "tvbrowserdata";
    }

    File path = new File(parent,subdirectory);
    File nomedia = new File(path,".nomedia");
    
    if(!path.isDirectory()) {
      if(!path.mkdirs() && external) {
        parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
        external = false;

        path = new File(parent,subdirectory);
        nomedia = new File(path,".nomedia");
        
        if(!path.isDirectory()) {
          path.mkdirs();
        }
      }
    }
    
    if(!nomedia.isFile()) {
      try {
        nomedia.createNewFile();
      } catch (IOException ignored) {}
    }
    
    return path;
  }
  
  /**
   * Gets the color for the given encoded category as array with index 0
   * containing the activated state of the color category 0 means disabled,
   * 1 means activated. The value with index 1 contains the color.
   * <p>
   * @param encodedColor The color category preference value.
   * @return An int array with the result.
   */
  public static int[] getActivatedColorFor(String encodedColor) {
    int[] result = new int[] {0,0};
    
    if(encodedColor != null) {
      if(encodedColor.contains(";")) {
        String[] parts = encodedColor.split(";");
      
        result[0] = Boolean.parseBoolean(parts[0]) ? 1 : 0;
        result[1] = Integer.parseInt(parts[1]);
      }
      else {
        result[0] = 1;
        
        if(encodedColor.startsWith("#")) {
          result[1] = (int)Long.parseLong(encodedColor.substring(1), 16);
        }
        else {
          result[1] = Integer.parseInt(encodedColor);
        }
      }
    }
    
    return result;
  }
  
  public static String getUniqueChannelKey(String groupKey, String channelKey) {
    return groupKey.trim() + "_##_" + channelKey.trim();
  }
  
  public static String[] getUniqueChannelKeyParts(String uniqueKey) {
    return uniqueKey.split("_##_");
  }
  
  public static boolean isInteractive(Context context) {
    return CompatUtils.isInteractive((PowerManager)context.getSystemService(Context.POWER_SERVICE));
  }
  
  /**
   * Decode the given value into an array of episode numbers.
   * <p>
   * @param fieldValue The field value to decode.
   * @return An array with the contained episode numbers.
   * @since 0.5.7.3
   */
  private static Integer[] decodeSingleFieldValueToMultipleEpisodeNumbers(int fieldValue) {
    int encodingMask = (fieldValue >> 30) & 0x3;
    
    if(encodingMask == 0) {
      if(((fieldValue >> 29) & 0x1) == 0x1) {
        int first = fieldValue & 0x3FFF;
        int second = (fieldValue >> 14) & 0x7FFF;
        
        return new Integer[] {first,second};
      }
      else {
        return new Integer[] {fieldValue};
      }
    }
    else {
      int andMask = 0xFF;
      int valueMask = 0x7F;
      int shiftMask = 8;
      int num = 2;
              
      if(encodingMask == 2) {          
        andMask = 0x1F;
        valueMask = 0xF;
        shiftMask = 5;
        num = 3;
      }
      else if(encodingMask == 3) {
        andMask = 0xF;
        valueMask = 0x7;
        shiftMask = 4;
        num = 4;
      }
      
      int last = fieldValue & 0x3FFF;
      
      ArrayList<Integer> valueList = new ArrayList<>();
      valueList.add(last);
      
      for(int i = 0; i < num; i++) {
        int testValue = (fieldValue >> (14 + i * shiftMask)) & andMask;
        int absValue = (testValue & valueMask) + 1;
        
        if(((testValue >> shiftMask -1) & 0x1) == 0x1) {
          absValue = absValue * -1;
        }
        
        last += absValue;
        valueList.add(last);
      }
      
      return valueList.toArray(new Integer[0]);
    }
  }
  
  /**
   * Decode the given value into a String of episode numbers.
   * <p>
   * @param fieldValue The field value to decode.
   * @return A String of episode numbers.
   * @since 0.5.7.3
   */
  public static String decodeSingleFieldValueToMultipleEpisodeString(int fieldValue) {
    Integer[] episodes = decodeSingleFieldValueToMultipleEpisodeNumbers(fieldValue);
    
    StringBuilder epis = new StringBuilder();
    
    for(int episode : episodes) {
      if(epis.length() > 0) {
        epis.append("|");
      }
      
      epis.append(episode);
    }
    
    return epis.toString();
  }
  
  public static void deleteOldData(Context context) {
    Calendar cal2 = Calendar.getInstance();
    cal2.add(Calendar.DAY_OF_YEAR, -2);
    cal2.set(Calendar.HOUR_OF_DAY, 0);
    cal2.set(Calendar.MINUTE, 0);
    cal2.set(Calendar.SECOND, 0);
    cal2.set(Calendar.MILLISECOND, 0);
    
    long daysSince1970 = cal2.getTimeInMillis() / 24 / 60 / 60000;

    try {
      context.getContentResolver().delete(
          TvBrowserContentProvider.CONTENT_URI_DATA,
          TvBrowserContentProvider.DATA_KEY_STARTTIME + "<"
              + cal2.getTimeInMillis(), null);
    } catch (IllegalArgumentException ignored) {
    }

    try {
      context.getContentResolver().delete(
          TvBrowserContentProvider.CONTENT_URI_DATA_VERSION,
          TvBrowserContentProvider.VERSION_KEY_DAYS_SINCE_1970 + "<"
              + daysSince1970, null);
    } catch (IllegalArgumentException ignored) {
    }
    
    final File pathBase = getDownloadDirectory(context);
    
    if(pathBase.isDirectory()) {
      final File epgPaidPath = new File(pathBase, "epgPaidData");
      
      if(epgPaidPath.isDirectory()) {
        final File fileSummaryCurrent = new File(epgPaidPath,"summary.gz");
        final Properties currentProperties = readPropertiesFile(fileSummaryCurrent);
        
        if(!currentProperties.isEmpty()) {
          final long startMinute = cal2.getTimeInMillis() / 60000;
          
          final File[] toDelete = pathBase.listFiles(file -> {
            boolean result = false;
            int index = file.getName().indexOf("_");

            if(index > 0) {
              try {
                result = Long.parseLong(file.getName().substring(0,index)) < startMinute;
              }catch(NumberFormatException ignored) {}
            }

            return result;
          });
          
          for(final File file : toDelete) {
            if(file.getName().endsWith("_base.gz")) {
              int index = file.getName().lastIndexOf("base.gz");
              
              currentProperties.remove(file.getName().substring(0, index));
            }
            
            if(!file.delete()) {
              file.deleteOnExit();
            }
          }
          
          storeProperties(currentProperties, fileSummaryCurrent, "Properties of EPGpaid");
        }
      }
    }
  }
  
  /**
    * Sets the cursor to the first entry if it isn't <code>null</code> or has no rows.
    * <p>
    * @param cursor The cursor to move to the first entry, <code>null</code> value is possible.
    * @return <code>true</code> if the cursor could be moved to the first entry,
    * <code>false</code> otherwise.
    */
  public static boolean prepareAccessFirst(Cursor cursor) {
    boolean result = false;
    
    if(cursor != null && cursor.getCount() > 0 && !cursor.isClosed()) {
      cursor.moveToFirst();
      result = true;
    }
    
    return result;
  }

  public static boolean isCursorAccessable(Cursor cursor) {
    boolean result = false;

    if(cursor != null && cursor.getCount() > 0 && !cursor.isClosed()) {
      result = true;
    }

    return result;
  }

  public static boolean prepareAccess(Cursor cursor) {
    boolean result = isCursorAccessable(cursor);
    
    if(result) {
      cursor.moveToPosition(-1);
    }
    
    return result;
  }
  
  public static void removeReminder(Context context, long programID) {
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent remind = new Intent(context,ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_NO_CREATE);
    Logging.log(ReminderBroadcastReceiver.tag, " Delete reminder for programID '" + programID + "' with pending intent '" + pending + "'", Logging.TYPE_REMINDER, context);
    if(pending != null) {
      alarmManager.cancel(pending);
    }
    
    pending = PendingIntent.getBroadcast(context, (int)-programID, remind, PendingIntent.FLAG_NO_CREATE);
    Logging.log(ReminderBroadcastReceiver.tag, " Delete reminder for programID '-" + programID + "' with pending intent '" + pending + "'", Logging.TYPE_REMINDER, context);
    if(pending != null) {
      alarmManager.cancel(pending);
    }
  }
  
  public static boolean isDatabaseAccessible(Context context) {
    boolean result = true;
    
    if(context != null) {
      String path = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context).getString(context.getString(R.string.PREF_DATABASE_PATH), context.getString(R.string.pref_database_path_default));
      
      if(!path.equals(context.getString(R.string.pref_database_path_default))) {
        result = new File(path).canWrite();
      }
    }
    
    return result;
  }
  
  /**
   * Copies the source file to the target. An existing target file
   * will be overwritten.
   * 
   * @param source The source file to copy
   * @param target The target file.
   * @return <code>true</code> if the file could be copied, <code>false</code> otherwise.
   */
  public static boolean copyFile(File source, File target) {
    boolean result = false;
    
    if(source.isFile()) {
      BufferedInputStream in = null;
      BufferedOutputStream out = null;
      
      try {
        in = new BufferedInputStream(new FileInputStream(source));
        
        FileOutputStream fOut = new FileOutputStream(target);
        fOut.getChannel().truncate(0);
        
        out = new BufferedOutputStream(fOut);
        
        byte temp[] = new byte[1024];
        int count;
        
        while ((count = in.read(temp, 0, 1024)) != -1) {
          if(temp != null && count > 0) {
            out.write(temp, 0, count);
          }
        }
        
        out.flush();
        
        result = target.length() == source.length();
      } catch(IOException ioe) {
        Log.d("info12", "", ioe);
      } finally {
        close(in);
        close(out);
      }
    }
    
    return result;
  }
  
  public static boolean isBatterySufficient(Context context) {
    boolean result = false;
    
    if(context != null && context.getApplicationContext() != null) {
      IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      Intent batteryStatus = context.getApplicationContext().registerReceiver(null, filter);
      
      int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
      result = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL;
      
      if(!result) {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        
        result = MIN_BATTERY_LEVEL <= (level / (float)scale);
      }
    }
    
    return result;
  }
  
  public static void storeProperties(Properties prop, File propertiesFile, String comment) {
    if(propertiesFile.getParentFile().isDirectory()) {
      GZIPOutputStream out = null;
      
      try {
        out = new GZIPOutputStream(new FileOutputStream(propertiesFile));
        prop.store(out, comment);
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        close(out);
      }
    }
  }
  
  public static Properties readPropertiesFile(File propertiesFile) {
    final Properties properties = new Properties();
    
    if(propertiesFile.isFile()) {
      GZIPInputStream in = null;
      
      try {
        in = new GZIPInputStream(new FileInputStream(propertiesFile));
        properties.load(in);
      } catch(IOException ignored) {
        
      } finally {
        close(in);
      }
    }
    
    return properties;
  }

  /**
   * Will wait a maximum of waitInMilliseconds in a separate Thread before running
   * the Runnable delayed. (Might not wait the full wait time if the sleeping Thread is interrupted.)
   * <p>
   * @param delayed The runnable to run after the wait time.
   * @param waitInMilliseconds The time in milliseconds to wait.
   */
  public static void postDelayedInSeparateThread(final Runnable delayed, final long waitInMilliseconds) {
    postDelayedInSeparateThread("postDelayedInSeparateThread", delayed, waitInMilliseconds);
  }
  
  /**
   * Will wait a maximum of waitInMilliseconds in a separate Thread before running
   * the Runnable delayed. (Might not wait the full wait time if the sleeping Thread is interrupted.)
   * <p>
   * @param threadName The name of the waiting thread.
   * @param delayed The runnable to run after the wait time.
   * @param waitInMilliseconds The time in milliseconds to wait.
   */
  public static void postDelayedInSeparateThread(String threadName, final Runnable delayed, final long waitInMilliseconds) {
    new Thread(threadName) {
      @Override
      public void run() {
        try {
          sleep(waitInMilliseconds);
        } catch (InterruptedException e) {
          // simply ignore
        }
        
        delayed.run();
      }
    }.start();
  }
}