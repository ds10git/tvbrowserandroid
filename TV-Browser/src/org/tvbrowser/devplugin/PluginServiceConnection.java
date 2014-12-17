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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A class with a service connection to a specific TV-Browser Plugin.
 * 
 * @author René Mach
 */
public class PluginServiceConnection implements ServiceConnection, Comparable<PluginServiceConnection> {
  private Plugin mPlugin;
  private String mPackageId;
  private String mPluginId;
  private Context mContext;
  
  private String mPluginName;
  private String mPluginVersion;
  private String mPluginDescription;
  private String mPluginAuthor;
  private String mPluginLicense;
  private boolean mHasPreferences;
  
  private Runnable mBindCallback;
  
  private ArrayList<Context> mBindContextList;
  
  public PluginServiceConnection(String packageId, String id, Context context) {
    mPackageId = packageId;
    mPluginId = id;
    mContext = context;
    mHasPreferences = false;
    
    mBindContextList = new ArrayList<Context>();
  }
  
  
  public boolean bindPlugin(Context context, Runnable bindCallback) {
    boolean bound = false;
    mBindCallback = bindCallback;
    
    if(!mBindContextList.contains(context)) {
      Intent intent = new Intent();
      intent.setClassName(mPackageId, mPluginId);
      Log.d("info23", mPackageId + " " + mPluginId);try {
      bound = context.bindService( intent, this, Context.BIND_AUTO_CREATE);
      Log.d("info23", "BOUND23 " + bound);
      if(bound) {
        mBindContextList.add(context);
      }
      
      }catch(Throwable t) {Log.d("info23", "" , t);}
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
  
  public boolean hasPreferences() {
    return mHasPreferences;
  }
  
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    Log.d("info23","onServiceConnected " + name );
    
    mPlugin = Plugin.Stub.asInterface(service);
    
    if(isActivated()) {
      callOnActivation();
    }
    
    readPluginMetaData();
  }
  
  public void callOnActivation() {
    Log.d("info23", "callOnActivation " + isActivated());
    if(isConnected()) {
      try {
        if(PluginHandler.getPluginManager() != null) {
          mPlugin.onActivation(PluginHandler.getPluginManager());
          
          long firstProgramId = PluginHandler.getFirstProgramId();
          
          if(firstProgramId != PluginHandler.FIRST_PROGRAM_ALREADY_HANDLED_ID) {
            mPlugin.handleFirstKnownProgramId(firstProgramId);
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
  
  public void readPluginMetaData() {
    if(isConnected()) {
      try {
        mPluginName = mPlugin.getName();
        mPluginVersion = mPlugin.getVersion();
        mPluginDescription = mPlugin.getDescription();
        mPluginAuthor = mPlugin.getAuthor();
        mPluginLicense = mPlugin.getLicense();
        mHasPreferences = mPlugin.hasPreferences();
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
    Log.d("info23", "callOnActivation " + isActivated());
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
    
    for(Context context : mBindContextList) {
      context.unbindService(this);
    }
    
    mBindContextList.clear();
    
    PluginHandler.removePluginServiceConnection(this);
    
    Log.d("info23","onServiceDisconnected " + name + " " + mPlugin);
  }
  
 /* public boolean isConnected() {
    return mPlugin != null;
  }*/
  
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

  @Override
  public int compareTo(PluginServiceConnection another) {
    if(mPluginName != null && another.mPluginName != null) {
      return mPluginName.compareToIgnoreCase(another.mPluginName);
    }
    
    return isConnected() ? -1 : 1;
  }
}
