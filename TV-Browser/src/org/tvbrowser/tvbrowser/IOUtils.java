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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;
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
  
  public static String getInfoString(int value, Resources res) {
    StringBuilder infoString = new StringBuilder();
    
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
    
    int[] prefKeyArr = SettingConstants.INFO_PREF_KEY_ARR;
    
    for(int i = 1; i <= 25; i++) {
      if((value & (1 << i)) == (1 << i) && PrefUtils.getBooleanValue(prefKeyArr[i-1], R.bool.pref_info_show_default)) {
        if(infoString.length() > 0) {
          infoString.append(", ");
        }
        
        infoString.append(valueArr[i]);
      }
    }
    
    return infoString.toString().trim();
  }
  
  public static byte[] loadUrl(String urlString) throws MalformedURLException, IOException {
    BufferedInputStream in = null;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    try {
      URLConnection connection;
      
      connection = new URL(urlString).openConnection();
      connection.setConnectTimeout(10000);
      
      if(urlString.toLowerCase().endsWith(".gz")) {
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
      }
      
      in = new BufferedInputStream(connection.getInputStream());
      
      byte temp[] = new byte[1024];
      int count;
      
      while ((count = in.read(temp, 0, 1024)) != -1) {
        out.write(temp, 0, count);
      }
    } 
    finally {
      if (in != null) {
        in.close();
      }
    }

    return out.toByteArray();

  }
  
  public static void saveUrl(String filename, String urlString) throws MalformedURLException, IOException {
    FileOutputStream fout = null;
    
    try {
      byte[] byteArr = loadUrl(urlString);
      
      fout = new FileOutputStream(filename);
      fout.getChannel().truncate(0);
      fout.write(byteArr, 0, byteArr.length);
    }
    finally {
      if (fout != null) {
        fout.close();
      }
    }
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
    
    Log.d("info", "time  " + new Date(time));
    if(time > System.currentTimeMillis()) {
      PendingIntent pending = PendingIntent.getBroadcast(context, DATA_UPDATE_KEY, dataUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
      Log.d("info", "" + pending);
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
      
      long updateTime = last.getTimeInMillis();
      
      if(updateTime < System.currentTimeMillis()) {
        updateTime += (24 * 60 * 60000);
      }
      
      Log.d("info", "xxx " + new Date(updateTime));
      IOUtils.setDataUpdateTime(context, updateTime, pref);
    }
  }
  
  public static final String[] getStringArrayFromList(ArrayList<String> list) {
    if(list != null) {
      return list.toArray(new String[list.size()]);
    }
    
    return null;
  }
}
