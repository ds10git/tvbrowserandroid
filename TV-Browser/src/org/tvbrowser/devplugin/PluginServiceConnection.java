/*
 * TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.devplugin;

import java.util.ArrayList;
import java.util.Arrays;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Logging;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.UiUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.style.ImageSpan;

/**
 * A class with a service connection to a specific TV-Browser Plugin.
 * 
 * @author René Mach
 */
public class PluginServiceConnection implements ServiceConnection, Comparable<PluginServiceConnection> {
  private Plugin mPlugin;
  private final String mPackageId;
  private final String mPluginId;
  private final Context mContext;
  
  private String mPluginName;
  private String mPluginVersion;
  private String mPluginDescription;
  private String mPluginAuthor;
  private String mPluginLicense;
  private boolean mHasPreferences;
  private ImageSpan mIcon;
  
  private Runnable mBindCallback;
  
  private final ArrayList<Context> mBindContextList;
  
  public PluginServiceConnection(String packageId, String id, Context context) {
    mPackageId = packageId;
    mPluginId = id;
    mContext = context;
    mHasPreferences = false;
    
    mBindContextList = new ArrayList<>();
    
    doLog(mContext, "Plugin connection created: " + packageId + " " + id);
  }
  
  
  public boolean bindPlugin(Context context, Runnable bindCallback) {
    boolean bound = false;
    mBindCallback = bindCallback;
    
    doLog(mContext, "Plugin connection created: " + mPackageId + " " + mPluginId+"\nCONTEXT: " + context + " " + mBindContextList.contains(context) + " " + mBindContextList.size());
    
    if(!mBindContextList.contains(context)) {
      Intent intent = new Intent();
      intent.setClassName(mPackageId, mPluginId);
      
      try {
        bound = context.bindService( intent, this, Context.BIND_AUTO_CREATE);
        if(bound) {
          mBindContextList.add(context);
        }
      }catch(Throwable ignored) {}
    }
    
    return bound;
  }
  
  public void unbindPlugin(Context context) {
    if(mBindContextList.contains(context)) {
      mBindContextList.remove(context);
      
      context.unbindService(this);
    }
  }
  
  public boolean isBound(Context context) {
    return mBindContextList.contains(context);
  }
  
  public String getPluginName() {
    return mPluginName;
  }

  public String getPluginVersion() {
    return mPluginVersion;
  }
  
  public String getPluginDescription() {
    return mPluginDescription;
  }
  
  public String getPluginAuthor() {
    return mPluginAuthor;
  }
  
  public String getPluginLicense() {
    return mPluginLicense;
  }
  
  public ImageSpan getPluginMarkIcon() {
    return mIcon;
  }
  
  public boolean hasPreferences() {
    return mHasPreferences;
  }
  
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    mPlugin = Plugin.Stub.asInterface(service);
    doLog(mContext, "Plugin connected: " + name);
    if(isActivated()) {
      callOnActivation();
    }
    
    readPluginMetaData();
  }
  
  @SuppressWarnings("WeakerAccess")
  public void callOnActivation() {
    if(isConnected()) {
      try {
        if(PluginHandler.getPluginManager() != null) {
          mPlugin.onActivation(PluginHandler.getPluginManager());
          
          if(!PluginHandler.firstAndLastProgramIdAlreadyHandled()) {
            try {
              SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, mContext);
              long value = pref.getLong(mContext.getString(R.string.META_DATA_ID_FIRST_KNOWN), mContext.getResources().getInteger(R.integer.meta_data_id_default));
              mPlugin.handleFirstKnownProgramId(value);
            }catch(Throwable ignored) {}
          }
        }
        
        if(mBindCallback != null) {
          mBindCallback.run();
          mBindCallback = null;
        }
      } catch (Throwable e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  public void loadIcon() {
    mIcon = null;
    
    if(isConnected() && isActivated()) {
      try {
        byte[] iconBytes = mPlugin.getMarkIcon();
        
        if(iconBytes != null) {
          Bitmap iconBitmap = UiUtils.createBitmapFromByteArray(iconBytes);
          
          BitmapDrawable icon = new BitmapDrawable(mContext.getResources(),iconBitmap);
          
          float zoom = 16f/iconBitmap.getHeight() * mContext.getResources().getDisplayMetrics().density;
          
          icon.setBounds(0, 0, (int)(iconBitmap.getWidth() * zoom), (int)(iconBitmap.getHeight() * zoom));
          
          if(!SettingConstants.IS_DARK_THEME) {
            icon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
          }
          
          mIcon = new ImageSpan(icon, ImageSpan.ALIGN_BASELINE);
        }
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  @SuppressWarnings("WeakerAccess")
  public void readPluginMetaData() {
    if(isConnected()) {
      try {
        mPluginName = mPlugin.getName();
        mPluginVersion = mPlugin.getVersion();
        mPluginDescription = mPlugin.getDescription();
        mPluginAuthor = mPlugin.getAuthor();
        mPluginLicense = mPlugin.getLicense();
        mHasPreferences = mPlugin.hasPreferences();
        loadIcon();
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  private boolean isConnected() {
    return mPlugin != null && !mBindContextList.isEmpty();
  }
  
  public void callOnDeactivation() {
    if(isConnected()) {
      try {
        mPlugin.onDeactivation();
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  @Override
  public void onServiceDisconnected(ComponentName name) {
    mPlugin = null;
    doLog(mContext, "Plugin disconnected: " + name);
    for(Context context : mBindContextList) {
      context.unbindService(this);
    }
    
    mBindContextList.clear();
    
    PluginHandler.removePluginServiceConnection(this);
  }
  
  public boolean isActivated() {
    return mPlugin != null && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mPluginId+"_ACTIVATED", true);
  }
  
  public Plugin getPlugin() {
    return mPlugin;
  }
  
  public String getPackageId() {
    return mPackageId;
  }
  
  public String getId() {
    return mPluginId;
  }
  
  private String getCompareId() {
    return mPackageId+"."+mPluginId;
  }
  
  @Override
  public int hashCode() {
    return Arrays.hashCode(getCompareId().getBytes());
  }
  
  @Override
  public boolean equals(Object o) {
    if(o instanceof PluginServiceConnection) {
      return getCompareId().equals(((PluginServiceConnection) o).getCompareId());
    }
    
    return super.equals(o);
  }

  @Override
  public int compareTo(@SuppressWarnings("NullableProblems") PluginServiceConnection another) {
    if(mPluginName != null && another.mPluginName != null) {
      return UiUtils.getCollator().compare(mPluginName, another.mPluginName);
    }
    
    return isConnected() ? -1 : 1;
  }
  
  private static void doLog(Context context, String message) {
    Logging.log(null, message, Logging.TYPE_PLUGIN, context);
  }
}
