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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Binder;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

/**
 * Helper class to supper IO.
 * <p>
 * @author René Mach
 */
public class IOUtils {
  private static final int DATA_UPDATE_KEY = 1234;
  
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
  
  public static int INFO_BLACK_AND_WHITE = 1 << 1;
  public static int INFO_BLACK_FOUR_TO_THREE = 1 << 2;
  public static int INFO_BLACK_SIXTEEN_TO_NINE = 1 << 3;
  public static int INFO_BLACK_MONO = 1 << 4;
  public static int INFO_BLACK_STEREO = 1 << 5;
  public static int INFO_BLACK_DOLBY_SOURROUND = 1 << 6;
  public static int INFO_BLACK_DOLBY_DIGITAL = 1 << 7;
  public static int INFO_BLACK_SECOND_AUDIO_PROGRAM = 1 << 8;
  public static int INFO_BLACK_SECOND_CLOSED_CAPTION = 1 << 9;
  public static int INFO_BLACK_SECOND_LIVE = 1 << 10;
  public static int INFO_BLACK_SECOND_OMU = 1 << 11;
  public static int INFO_BLACK_SECOND_FILM = 1 << 12;
  public static int INFO_BLACK_SECOND_SERIES = 1 << 13;
  public static int INFO_BLACK_SECOND_NEW = 1 << 14;
  public static int INFO_BLACK_SECOND_AUDIO_DESCRIPTION = 1 << 15;
  public static int INFO_BLACK_SECOND_NEWS = 1 << 16;
  public static int INFO_BLACK_SECOND_SHOW = 1 << 17;
  public static int INFO_BLACK_SECOND_MAGAZIN = 1 << 18;
  public static int INFO_BLACK_SECOND_HD = 1 << 19;
  public static int INFO_BLACK_SECOND_DOCU = 1 << 20;
  public static int INFO_BLACK_SECOND_ART = 1 << 21;
  public static int INFO_BLACK_SECOND_SPORT = 1 << 22;
  public static int INFO_BLACK_SECOND_CHILDREN = 1 << 23;
  public static int INFO_BLACK_SECOND_OTHER = 1 << 24;
  public static int INFO_BLACK_SECOND_SIGN_LANGUAGE = 1 << 25;
  
  public static final int[] INFO_CATEGORIES_ARRAY = {
    INFO_BLACK_AND_WHITE,
    INFO_BLACK_FOUR_TO_THREE,
    INFO_BLACK_SIXTEEN_TO_NINE,
    INFO_BLACK_MONO,
    INFO_BLACK_STEREO,
    INFO_BLACK_DOLBY_SOURROUND,
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
    INFO_BLACK_SECOND_MAGAZIN,
    INFO_BLACK_SECOND_HD,
    INFO_BLACK_SECOND_DOCU,
    INFO_BLACK_SECOND_ART,
    INFO_BLACK_SECOND_SPORT,
    INFO_BLACK_SECOND_CHILDREN,
    INFO_BLACK_SECOND_OTHER,
    INFO_BLACK_SECOND_SIGN_LANGUAGE,
  };
  
  public static final String[] getInfoStringArrayNames(Resources res) {
    String[] valueArr = {
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
    
    return valueArr;
  }
  
  public static boolean infoSet(int categories, int info) {
    return ((categories & info) == info);
  }
  
  public static Spannable getInfoString(int value, Resources res) {
    return getInfoString(value, res, true);
  }
  
  private static int getDefaultCategoryColorKeyForColorKey(int colorKey) {
    int defaultColorCategoryKey = R.string.pref_color_categories_default;
    
    if(colorKey == R.string.PREF_COLOR_CATEGORY_FILM) {
      defaultColorCategoryKey = R.string.pref_color_category_film_default;
    }
    else if(colorKey == R.string.PREF_COLOR_CATEGORY_SERIES) {
      defaultColorCategoryKey = R.string.pref_color_category_series_default;
    }
    else if(colorKey == R.string.PREF_COLOR_CATEGORY_NEW) {
      defaultColorCategoryKey = R.string.pref_color_category_new_default;
    }
    else if(colorKey == R.string.PREF_COLOR_CATEGORY_DOCU || colorKey == R.string.PREF_COLOR_CATEGORY_MAGAZIN) {
      defaultColorCategoryKey = R.string.pref_color_category_docu_default;
    }
    else if(colorKey == R.string.PREF_COLOR_CATEGORY_CHILDREN) {
      defaultColorCategoryKey = R.string.pref_color_category_children_default;
    }
    else if(colorKey == R.string.PREF_COLOR_CATEGORY_SHOW) {
      defaultColorCategoryKey = R.string.pref_color_category_show_default;
    }
    
    return defaultColorCategoryKey;
  }
  
  public static HashMap<String,Integer> loadCategoryColorMap(Context context) {
    HashMap<String, Integer> categoryColorMap = new HashMap<String, Integer>();
    String[] names = getInfoStringArrayNames(context.getResources());
    
    for(int i = 0; i < SettingConstants.CATEGORY_COLOR_PREF_KEY_ARR.length; i++) {
      int colorKey = SettingConstants.CATEGORY_COLOR_PREF_KEY_ARR[i];
      
      int[] colorCategory = getColorForCategory(PrefUtils.getStringValue(colorKey, getDefaultCategoryColorKeyForColorKey(colorKey)));
      
      if(colorCategory[0] == 1) {
        categoryColorMap.put(names[i], Integer.valueOf(colorCategory[1]));
      }
    }
        
    return categoryColorMap;
  }
  
  public static Spannable getInfoString(int value, Resources res, boolean colored) {
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
        }
        infoString.append(valueArr[i]);
        
        if(colored) {
          int[] colorCategory = getColorForCategory(PrefUtils.getStringValue(colorPrefKeyArr[i-1], getDefaultCategoryColorKeyForColorKey(colorPrefKeyArr[i-1])));
          
          if(colorCategory[0] == 1) {
            infoString.setSpan(new ForegroundColorSpan(colorCategory[1]), infoString.length()-valueArr[i].length(), infoString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
  
  public static byte[] loadUrl(String urlString) throws MalformedURLException, IOException, TimeoutException {
    return loadUrl(urlString, 30000);
  }
  
  public static byte[] loadUrl(final String urlString, final int timeout) throws MalformedURLException, IOException, TimeoutException {
    final AtomicInteger count = new AtomicInteger(0);
    final AtomicReference<byte[]> loadData = new AtomicReference<byte[]>(null);
    
    new Thread("LOAD URL THREAD") {
      public void run() {
        FileOutputStream fout = null;
        
        try {
          byte[] byteArr = loadUrl(urlString, count);
          
          loadData.set(byteArr);
        }
        catch(IOException e) {
        }
        finally {
          if (fout != null) {
            try {
              fout.close();
            } catch (IOException e) {}
          }
        }
      };
    }.start();
    
    Thread wait = new Thread("SAVE URL WAITING THREAD") {
      public void run() {
        while(loadData.get() == null && count.getAndIncrement() < (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException e) {}
        }
      };
    };
    wait.start();
    
    try {
      wait.join();
    } catch (InterruptedException e) {}
    
    if(loadData.get() == null) {
      throw new TimeoutException("URL '"+urlString+"' could not be saved.");
    }
    
    return loadData.get();
  }
  
  private static byte[] loadUrl(String urlString, AtomicInteger timeoutCount) throws MalformedURLException, IOException {
    BufferedInputStream in = null;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    try {
      URLConnection connection;
      
      connection = new URL(urlString).openConnection();
      connection.setConnectTimeout(15000);
      
      if(urlString.toLowerCase().endsWith(".gz")) {
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
      if (in != null) {
        in.close();
      }
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
          fout.close();
          
          wasSaved.set(true);
        }
        catch(IOException e) {
        }
        finally {
          if (fout != null) {
            try {
              fout.close();
            } catch (IOException e) {}
          }
        }
      };
    }.start();
    
    Thread wait = new Thread("SAVE URL WAITING THREAD") {
      public void run() {
        while(!wasSaved.get() && count.getAndIncrement() < (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException e) {}
        }
      };
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
    
    try {
      GZIPOutputStream out = new GZIPOutputStream(bytesOut);
      
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
      out.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return bytesOut.toByteArray();
  }
  
  public static final void setDataUpdateTime(Context context, long time, SharedPreferences pref) {
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent dataUpdate = new Intent(context, AutoDataUpdateReceiver.class);
    dataUpdate.putExtra(SettingConstants.TIME_DATA_UPDATE_EXTRA, true);
    
    if(time > System.currentTimeMillis()) {
      PendingIntent pending = PendingIntent.getBroadcast(context, DATA_UPDATE_KEY, dataUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
      
      alarmManager.set(AlarmManager.RTC_WAKEUP, time, pending);
    }
  }
  
  public static final void removeDataUpdateTime(Context context, SharedPreferences pref) {
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent dataUpdate = new Intent(context, AutoDataUpdateReceiver.class);
    
    PendingIntent pending = PendingIntent.getBroadcast(context, DATA_UPDATE_KEY, dataUpdate, PendingIntent.FLAG_NO_CREATE);
    
    if(pending != null) {
      alarmManager.cancel(pending);
    }
  }
  
  public static final void handleDataUpdatePreferences(Context context) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    IOUtils.removeDataUpdateTime(context, pref);
    
    if(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_TYPE, R.string.pref_auto_update_type_default).equals("2")) {
      int days = Integer.parseInt(PrefUtils.getStringValue(R.string.PREF_AUTO_UPDATE_FREQUENCY, R.string.pref_auto_update_frequency_default)) + 1;
      int time = PrefUtils.getIntValue(R.string.PREF_AUTO_UPDATE_START_TIME, R.integer.pref_auto_update_start_time_default);
      long current = PrefUtils.getLongValue(R.string.AUTO_UPDATE_CURRENT_START_TIME, R.integer.auto_update_current_start_time_default);

      if(current < System.currentTimeMillis() - 5000) {
        long lastDate = PrefUtils.getLongValue(R.string.LAST_DATA_UPDATE, R.integer.last_data_update_default);
             
        if(lastDate == 0) {
          lastDate = (System.currentTimeMillis() - (24 * 60 * 60000));
          
          Editor edit = pref.edit();
          edit.putLong(context.getString(R.string.LAST_DATA_UPDATE), lastDate);
          edit.commit();
        }
        
        time += ((int)(Math.random() * 6 * 60));
        
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
        
        Editor currentTime = PreferenceManager.getDefaultSharedPreferences(context).edit();
        currentTime.putLong(context.getString(R.string.AUTO_UPDATE_CURRENT_START_TIME), current);
        currentTime.commit();
      }
      
      IOUtils.setDataUpdateTime(context, current, pref);
    }
  }
  
  public static final String[] getStringArrayFromList(ArrayList<String> list) {
    if(list != null) {
      return list.toArray(new String[list.size()]);
    }
    
    return null;
  }
  
  public static boolean isConnectedToServer(final String url, final int timeout) {
    final AtomicBoolean isConnected = new AtomicBoolean(false);
    
    new Thread("NETWORK CONNECTION CHECK THREAD") {
      public void run() {
        try {
          URL myUrl = new URL(url);
          
          URLConnection connection;
          connection = myUrl.openConnection();
          connection.setConnectTimeout(timeout);
          
          HttpURLConnection httpConnection = (HttpURLConnection)connection;
          
          if(httpConnection != null) {
            int responseCode = httpConnection.getResponseCode();
          
            isConnected.set(responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED
                || responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpsURLConnection.HTTP_SEE_OTHER);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      };
    }.start();
    
    Thread check = new Thread("WAITING FOR NETWORK CONNECTION THREAD") {
      @Override
      public void run() {
        int count = 0;
        while(!isConnected.get() && count++ <= (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException e) {}
        }
      }
    };
    check.start();
        
    try {
      check.join();
    } catch (InterruptedException e) {}
    
    return isConnected.get();
  }
  
  /**
   * Normale time of given Calendar to 2014-12-31 with the given time.
   * <p>
   * @param cal The Calendar to normalize.
   * @param minutesAfterMidnight The minutes after midnight to use.
   * @return The normalized Calendar.
   */
  public static Calendar normalizeTime(Calendar cal, int minutesAfterMidnight) {
    return normalizeTime(cal, minutesAfterMidnight, 0);
  }
  
  /**
   * Normale time of given Calendar to 2014-12-31 with the given time.
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
   * Normale time of given Calendar to 2014-12-31 with the hourOfDay and minutes.
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
   * Closes the given cursor.
   * Checks for <code>null</code> and already closed before closing.
   * 
   * @param cursor The cursor to close.
   */
  public static void closeCursor(Cursor cursor) {
    if(cursor != null) {
      cursor.close();
    }
  }
  
  /**
   * Closes the given input stream, checks for <code>null</code> prehand.
   * <p>
   * @param in The stream to close
   */
  public static void closeInputStream(InputStream in) {
    if(in != null) {
      try {
        in.close();
      } catch (IOException e) {
        // Igonore, nothing to do here
      }
    }
  }
  
  /**
   * Closes the given output stream, checks for <code>null</code> prehand.
   * <p>
   * @param out The stream to close
   */
  public static void closeOutpuStream(OutputStream out) {
    if(out != null) {
      try {
        out.close();
      } catch (IOException e) {
        // Igonore, nothing to do here
      }
    }
  }
  
  public static List<Channel> getChannelList(Context context) {
    ArrayList<Channel> channelList = new ArrayList<Channel>();
    
    final long token = Binder.clearCallingIdentity();
    Cursor channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, new String[] {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO}, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.KEY_ID);
    
    try {
      if(channels != null) {
        channels.moveToPosition(-1);
        
        int keyColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
        int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
        int iconColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
        
        while(channels.moveToNext()) {
          channelList.add(new Channel(channels.getInt(keyColumn), channels.getString(nameColumn), channels.getBlob(iconColumn)));
        }
      }
    }finally {
      IOUtils.closeCursor(channels);
      Binder.restoreCallingIdentity(token);
    }
    
    return channelList;
  }
  
  public static File getDownloadDirectory(Context context) {
    File parent = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    boolean external = true;
    
    if(parent == null || !parent.isDirectory()) {
      external = false;
      parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
    }
    
    File path = new File(parent,"tvbrowserdata");
    File nomedia = new File(path,".nomedia");
    
    if(!path.isDirectory()) {
      if(!path.mkdirs() && external) {
        parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
        
        path = new File(parent,"tvbrowserdata");
        nomedia = new File(path,".nomedia");
        
        if(!path.isDirectory()) {
          path.mkdirs();
        }
      }
    }
    
    if(!nomedia.isFile()) {
      try {
        nomedia.createNewFile();
      } catch (IOException e) {}
    }
    
    return path;
  }
  
  /**
   * Gets the color for the given encoded category as array with index 0
   * containing the activated state of the color category 0 means disabled,
   * 1 means activated. The value with index 1 contains the color.
   * <p>
   * @param encodedColorCategory The color categroy preference value.
   * @return An int array with the result.
   */
  public static int[] getColorForCategory(String encodedColorCategory) {
    int[] result = new int[] {0,0};
    
    if(encodedColorCategory != null) {
      String[] parts = encodedColorCategory.split(";");
      
      result[0] = Boolean.parseBoolean(parts[0]) ? 1 : 0;
      result[1] = Integer.parseInt(parts[1]);
    }
    
    return result;
  }
  
  public static final String getUniqueChannelKey(String groupKey, String channelKey) {
    return new StringBuilder(groupKey.trim()).append("_##_").append(channelKey.trim()).toString();
  }
  
  public static final String[] getUniqueChannelKeyParts(String uniqueKey) {
    return uniqueKey.split("_##_");
  }
  
  public static final boolean isInteractive(Context context) {
    return CompatUtils.isInteractive((PowerManager)context.getSystemService(Context.POWER_SERVICE));
  }
}
