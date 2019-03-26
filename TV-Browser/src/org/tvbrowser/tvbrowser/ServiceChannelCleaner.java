/*
 * TV-Browser for Android
 * Copyright (C) 2018 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software free of charge without modifications and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowser;

import java.util.ArrayList;
import java.util.HashSet;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.UiUtils;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ServiceChannelCleaner extends Service {
  public ServiceChannelCleaner() {
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    new Thread("CLEAN CHANNELS") {
      @Override
      public void run() {
        if(IOUtils.isDatabaseAccessible(ServiceChannelCleaner.this)) {
          final ContentResolver cr = getContentResolver();
          
          final String[] projection = {
              TvBrowserContentProvider.KEY_ID,
              TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
              TvBrowserContentProvider.GROUP_KEY_GROUP_ID
          };
          
          final Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, null, null, TvBrowserContentProvider.KEY_ID);
          
          final HashSet<String> knownChannels = new HashSet<>();
          final ArrayList<String> toDelete = new ArrayList<>();
          
          try {
            if(IOUtils.prepareAccess(channels)) {
              final int keyIndex = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
              final int channelKeyIndex = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
              final int groupKeyIndex = channels.getColumnIndex(TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
              
              while(channels.moveToNext()) {
                final int key = channels.getInt(keyIndex);
                final String channelGroupKey = getChannelGroupKey(channels.getString(channelKeyIndex),channels.getInt(groupKeyIndex));
                
                if(knownChannels.contains(channelGroupKey)) {
                  toDelete.add(String.valueOf(key));
                }
                else {
                  knownChannels.add(channelGroupKey);
                }
              }
            }
          }finally{
            IOUtils.close(channels);
          }
          
          if(!toDelete.isEmpty()) {
            int rows = cr.delete(TvBrowserContentProvider.CONTENT_URI_DATA, TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IN ( " + TextUtils.join(", ", toDelete) + " ) ", null);
            
            Log.d("info4", "DELETED DATA ROWS " + rows);
            
            rows = cr.delete(TvBrowserContentProvider.CONTENT_URI_CHANNELS, TvBrowserContentProvider.KEY_ID + " IN ( " + TextUtils.join(", ", toDelete) + " ) ", null);
            
            Log.d("info4", "DELETED CHANNEL ROWS " + rows);
          }
          
          UiUtils.updateRunningProgramsWidget(getApplicationContext());
          UiUtils.updateImportantProgramsWidget(getApplicationContext());
          
          stopSelf();
        }
      }
    }.start();
    
    return Service.START_NOT_STICKY;
  }
  
  private static String getChannelGroupKey(String channelKey, int groupKey) {
    return channelKey + ";" + groupKey;
  }
}
