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
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Log;

/**
 * A class that handles TV-Browser Plugins.
 * 
 * @author René Mach
 */
public final class PluginHandler {
  public static final String PLUGIN_ACTION = "org.tvbrowser.intent.action.PLUGIN";
  public static ArrayList<PluginServiceConnection> PLUGIN_LIST;
  
  public static final boolean pluginsAvailable() {
    return PLUGIN_LIST != null && !PLUGIN_LIST.isEmpty();
  }
  
  public static final void loadPlugins(Context context) {
    if(PLUGIN_LIST == null) {
      PLUGIN_LIST = new ArrayList<PluginServiceConnection>();
      
      PackageManager packageManager = context.getPackageManager();
      Intent baseIntent = new Intent( PluginHandler.PLUGIN_ACTION );
      baseIntent.setFlags( Intent.FLAG_DEBUG_LOG_RESOLUTION );
      List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER );
      
      for( int i = 0 ; i < list.size() ; ++i ) {
        ResolveInfo info = list.get( i );
        ServiceInfo sinfo = info.serviceInfo;
        IntentFilter filter1 = info.filter;

        Log.d( "info23", "fillPluginList: i: "+i+"; sinfo: "+sinfo+";filter: "+filter1 + " " );
        if(sinfo != null) {
          Log.d( "info23", "hier " + filter1.countCategories() + " " + filter1.getAction(0));
          if( filter1 != null ) {
            StringBuilder categories = new StringBuilder();
            String firstCategory = null;
            
            for( Iterator<String> categoryIterator = filter1.categoriesIterator() ;
                categoryIterator.hasNext() ; ) {
              String category = categoryIterator.next();
              if( firstCategory == null )
                firstCategory = category;
              if( categories.length() > 0 )
                categories.append( "," );
              categories.append( category );
            }
            
            if(firstCategory != null) {
              PluginServiceConnection plugin = new PluginServiceConnection(sinfo.name, context);
              
              Intent intent = new Intent( PluginHandler.PLUGIN_ACTION );
              intent.addCategory( categories.toString() );
              context.bindService( intent, plugin, Context.BIND_AUTO_CREATE);
              
              PLUGIN_LIST.add(plugin);
            }
            
            Log.d( "info23", "categories: " + categories.toString());
          }
        }
      }
    }
  }
  
  public static final void shutdownPlugins(Context context) {
    if(PLUGIN_LIST != null) {
      for(PluginServiceConnection plugin : PLUGIN_LIST) {
        if(plugin.isActivated()) {
          plugin.callOnDeactivation();
        }
        
        context.unbindService(plugin);
      }
    
      PLUGIN_LIST.clear();
      PLUGIN_LIST = null;
    }
    
    PluginHandler.PLUGIN_LIST = null;
  }
}
