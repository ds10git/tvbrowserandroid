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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.tvbrowser.settings.PrefUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class Logging {
  public static final int DATA_UPDATE_TYPE = 0;
  public static final int REMINDER_TYPE = 1;
  
  private static RandomAccessFile DATA_UPDATE_LOG;
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
  
  public synchronized static void log(String tag, String message, int type, Context context) {
    RandomAccessFile log = getLogFileForType(type, context);
    
    if(log != null) {
      try {
        log.writeBytes(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ": " + message + "\n");
        
        if(type == REMINDER_TYPE) {
          Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
          edit.putLong(context.getString(R.string.REMINDER_LOG_LAST_POS), log.getFilePointer());
          edit.commit();
          
          log.writeBytes(" --- NEWEST ENTRY ABOVE THIS LINE --- \n");
        }
      } catch (IOException e) {}
      finally {
        if(log != null && type == REMINDER_TYPE) {
          try {
            log.close();
          } catch (IOException e) {}
        }
      }
    }
    
    if(tag != null) {
      Log.d(tag, message);
    }
  }
  
  public static void openLogForDataUpdate(Context context) {
    if(DATA_UPDATE_LOG == null && PrefUtils.getBooleanValue(R.string.WRITE_DATA_UPDATE_LOG, R.bool.write_data_update_log_default)) {
      try {
        File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        if(!parent.isDirectory()) {
          parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
        }
        
        final File path = new File(parent,"tvbrowserdata");
        
        File logFile = new File(path,"data-update-log.txt");
        
        DATA_UPDATE_LOG = new RandomAccessFile(logFile, "rw");
        
        if(DATA_UPDATE_LOG.length() < (5 * 1024 * 1024)) {
          DATA_UPDATE_LOG.seek(DATA_UPDATE_LOG.length());
        }
        else {
          DATA_UPDATE_LOG.getChannel().truncate(0);
        }
      }catch(IOException e) {}
    }
  }
  
  public static void closeLogForDataUpdate() {
    if(DATA_UPDATE_LOG != null) {
      try {
        DATA_UPDATE_LOG.close();
        DATA_UPDATE_LOG = null;
      } catch (IOException e) {}
    }
  }
  
  private static RandomAccessFile getLogFileForType(int type, Context context) {
    RandomAccessFile log = null;
    
    if(type == DATA_UPDATE_TYPE) {
      log = DATA_UPDATE_LOG;
    }
    else if(type == REMINDER_TYPE) {
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
      
      if(PrefUtils.getBooleanValue(R.string.WRITE_REMINDER_LOG, R.bool.write_reminder_log_default)) {
        try {
          File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
          
          if(!parent.isDirectory()) {
            parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
          }
          
          final File path = new File(parent,"tvbrowserdata");
          
          File logFile = new File(path,"reminder-log.txt");
          boolean logFileExists = logFile.isFile();
          
          log = new RandomAccessFile(logFile, "rw");
          
          long pos = PrefUtils.getLongValueWithDefaultKey(R.string.REMINDER_LOG_LAST_POS, R.integer.reminder_log_last_pos_default);
          
          if(!logFileExists || pos > (5 * 1024 * 1024)) {
            log.seek(0);
          }
          else {
            log.seek(pos);
          }
        }catch(IOException e) {}
      }
    }
    if(log != null) {
      try {
        Log.d("Reminder", "" + log.length());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return log;
  }
}
