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

import android.content.ComponentName;
import android.content.Context;
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
  private String mId;
  private Context mContext;
  
  public PluginServiceConnection(String id, Context context) {
    mId = id;
    mContext = context;
  }
  
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    mPlugin = Plugin.Stub.asInterface(service);
    
    if(isActivated()) {
      callOnActivation();
    }

    Log.d("info23","onServiceConnected " + name );
  }
  
  public void callOnActivation() {
    Log.d("info23", "callOnActivation " + isActivated());
    if(isConnected()) {
      try {
        mPlugin.onActivation(PluginHandler.getPluginManager());
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  public void callOnDeactivation() {
    Log.d("info23", "callOnActivation " + isActivated());
    if(isConnected()) {
      try {
        mPlugin.onActivation(PluginHandler.getPluginManager());
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    mPlugin = null;
    
    Log.d("info23","onServiceDisconnected " + name );
  }
  
  public boolean isConnected() {
    return mPlugin != null;
  }
  
  public boolean isActivated() {
    return isConnected() && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mId+"_ACTIVATED", true);
  }
  
  public Plugin getPlugin() {
    return mPlugin;
  }
  
  public String getId() {
    return mId;
  }

  @Override
  public int compareTo(PluginServiceConnection another) {
    if(isConnected() && another.isConnected()) {
      try {
        return getPlugin().getName().compareToIgnoreCase(another.getPlugin().getName());
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    return isConnected() ? -1 : 1;
  }
}
