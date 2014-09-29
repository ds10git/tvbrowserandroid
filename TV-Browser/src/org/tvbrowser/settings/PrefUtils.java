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
package org.tvbrowser.settings;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PrefUtils {
  private static Context mContext;
  private static SharedPreferences mPref;
  
  private PrefUtils() {}
  
  public static void initialize(Context context) {
    if(mContext == null) {
      mContext = context.getApplicationContext();
      mPref = PreferenceManager.getDefaultSharedPreferences(context);
      
      String installerSouce = context.getPackageManager().getInstallerPackageName(context.getPackageName());
      
      SettingConstants.GOOGLE_PLAY = installerSouce != null && installerSouce.equals("com.android.vending");
      
      Log.d("info", "fromGooglePlay: " + SettingConstants.GOOGLE_PLAY);
    }
  }
  
  public static int getIntValue(int prefKey, int defaultValue) {
    if(mPref != null) {
      return mPref.getInt(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static int getIntValueWithDefaultKey(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getIntValue(prefKey,mContext.getResources().getInteger(defaultKey));
    }
    
    return -1;
  }
  
  public static long getLongValue(int prefKey, long defaultValue) {
    if(mPref != null) {
      return mPref.getLong(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static long getLongValueWithDefaultKey(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getLongValue(prefKey,mContext.getResources().getInteger(defaultKey));
    }
    
    return -1;
  }
  
  public static boolean getBooleanValue(int prefKey, boolean defaultValue) {
    if(mPref != null) {
      return mPref.getBoolean(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static boolean getBooleanValue(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getBooleanValue(prefKey,mContext.getResources().getBoolean(defaultKey));
    }
    
    return false;
  }
  
  public static String getStringValue(int prefKey, String defaultValue) {
    if(mPref != null) {
      return mPref.getString(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static String getStringValue(int prefKey, int defaultKey) {
    if(mContext != null) {
      return getStringValue(prefKey,mContext.getResources().getString(defaultKey));
    }
    
    return null;
  }
  
  public static int getStringValueAsInt(int prefKey, String defaultValue) throws NumberFormatException {
    if(mPref != null) {
      String value = mPref.getString(mContext.getString(prefKey), defaultValue);
      
      if(value != null) {
        return Integer.parseInt(value);
      }
    }
    else if(defaultValue != null) {
      return Integer.parseInt(defaultValue);
    }
    
    return Integer.MIN_VALUE;
  }
  
  public static int getStringValueAsInt(int prefKey, int defaultKey) throws NumberFormatException {
    if(mContext != null) {
      String value = getStringValue(prefKey,mContext.getResources().getString(defaultKey));
      
      if(value != null) {
        return Integer.parseInt(value);
      }
    }
    
    return Integer.MIN_VALUE;
  }

  public static Set<String> getStringSetValue(int prefKey, Set<String> defaultValue) {
    if(mPref != null) {
      return mPref.getStringSet(mContext.getString(prefKey), defaultValue);
    }
    
    return defaultValue;
  }
  
  public static Set<String> getStringSetValue(int prefKey, int defaultKey) {
    if(mContext != null) {
      String[] tempValues = mContext.getResources().getStringArray(defaultKey);
      
      HashSet<String> defaultValues = new HashSet<String>();
      
      if(tempValues != null) {
        for(String value : tempValues) {
          defaultValues.add(value);
        }
      }
      
      return getStringSetValue(prefKey,defaultValues);
    }
    
    return null;
  }
}
