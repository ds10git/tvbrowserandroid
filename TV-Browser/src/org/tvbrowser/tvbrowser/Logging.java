/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logging {
  public static final int TYPE_DATA_UPDATE = 0;
  public static final int TYPE_REMINDER = 1;
  public static final int TYPE_PLUGIN = 2;
  
  private static RandomAccessFile DATA_UPDATE_LOG;
  @SuppressLint("SimpleDateFormat")
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
  
  public synchronized static void log(String tag, String message, int type, Context context) {
    PrefUtils.initialize(context);
    RandomAccessFile log = getLogFileForType(type, context);

    if(log != null) {
      try {
        log.writeBytes(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ": " + message + "\n");
        
        if(type == TYPE_REMINDER || type == TYPE_PLUGIN) {
          int logKey = R.string.REMINDER_LOG_LAST_POS;
          
          if(type == TYPE_PLUGIN) {
            logKey = R.string.LOG_PLUGIN_LAST_POST;
          }
          
          Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
          edit.putLong(context.getString(logKey), log.getFilePointer());
          edit.commit();
          
          log.writeBytes(" --- NEWEST ENTRY ABOVE THIS LINE --- \n");
        }
      } catch (IOException ignored) {}
      finally {
        if(type == TYPE_REMINDER || type == TYPE_PLUGIN) {
          IOUtils.close(log);
        }
      }
    }
    
    if(tag != null) {
      Log.d(tag, DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ": " + message);
    }
  }
  
  public static synchronized void openLogForDataUpdate(Context context) {
    boolean writeLog = false;
    
    PrefUtils.initialize(context);
    
    try {
      writeLog = PrefUtils.getBooleanValue(R.string.WRITE_DATA_UPDATE_LOG, R.bool.write_data_update_log_default);
    }catch(Exception ignored) {}

    if(DATA_UPDATE_LOG == null && writeLog) {
      try {
        final File path = IOUtils.getDownloadDirectory(context, IOUtils.TYPE_DOWNLOAD_DIRECTORY_LOG);
        
        File logFile = new File(path, SettingConstants.LOG_FILE_NAME_DATA_UPDATE);
        
        DATA_UPDATE_LOG = new RandomAccessFile(logFile, "rw");
        
        if(DATA_UPDATE_LOG.length() < (5 * 1024 * 1024)) {
          DATA_UPDATE_LOG.seek(DATA_UPDATE_LOG.length());
        }
        else {
          DATA_UPDATE_LOG.getChannel().truncate(0);
        }
      }catch(IOException ignored) {}
    }
  }
  
  public static synchronized void closeLogForDataUpdate() {
    if(DATA_UPDATE_LOG != null) {
      IOUtils.close(DATA_UPDATE_LOG);
      DATA_UPDATE_LOG = null;
    }
  }
  
  private static RandomAccessFile getLogFileForType(int type, Context context) {
    RandomAccessFile log = null;
    String tag = null;
    
    if(type == TYPE_DATA_UPDATE) {
      log = DATA_UPDATE_LOG;
      tag = "DataUpdate";
    }
    else {
      String fileName = null;
      int lastPosKey = 0;
      
      if(type == TYPE_REMINDER) {
        tag = "Reminder";
        if(PrefUtils.getBooleanValue(R.string.WRITE_REMINDER_LOG, R.bool.write_reminder_log_default)) {
          fileName = SettingConstants.LOG_FILE_NAME_REMINDER;
          lastPosKey = R.string.REMINDER_LOG_LAST_POS;
        }
      }
      else if(type == TYPE_PLUGIN) {
        tag = "Plugin";
        
        if(PrefUtils.getBooleanValue(R.string.LOG_WRITE_PLUGIN_LOG, R.bool.log_write_plugin_log_default)) {
          fileName = SettingConstants.LOG_FILE_NAME_PLUGINS;
          lastPosKey = R.string.LOG_PLUGIN_LAST_POST;
        }
      }
      
      if(fileName != null) {
        try {
          final File path = IOUtils.getDownloadDirectory(context, IOUtils.TYPE_DOWNLOAD_DIRECTORY_LOG);
          
          File logFile = new File(path,fileName);
          boolean logFileExists = logFile.isFile();
          
          log = new RandomAccessFile(logFile, "rw");
          
          long pos = PrefUtils.getLongValueWithDefaultKey(lastPosKey, R.integer.log_last_pos_default);
          
          if(!logFileExists || pos > (5 * 1024 * 1024)) {
            log.seek(0);
          }
          else {
            log.seek(pos);
          }
        }catch(IOException ignored) {}
      }
    }
    if(log != null && tag != null) {
      try {
        Log.d(tag, "" + log.length());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return log;
  }
}
