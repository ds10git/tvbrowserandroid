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

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class UpdateAlarmValue extends BroadcastReceiver {
  @Override
  public void onReceive(final Context context, Intent intent) {
    new Thread() {
      public void run() {
        IOUtils.handleDataUpdatePreferences(context);
        
        Cursor alarms = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.DATA_KEY_STARTTIME, TvBrowserContentProvider.DATA_KEY_ENDTIME, TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, " ( " + TvBrowserContentProvider.DATA_KEY_MARKING_VALUES + " LIKE '%" + SettingConstants.MARK_VALUE_REMINDER + "%' ) AND ( " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis() + " ) ", null, TvBrowserContentProvider.KEY_ID);
        
        while(alarms.moveToNext()) {
          long id = alarms.getLong(alarms.getColumnIndex(TvBrowserContentProvider.KEY_ID));
          long startTime = alarms.getLong(alarms.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
                    
          UiUtils.removeReminder(context, id);
          UiUtils.addReminder(context, id, startTime);
        }
        
        alarms.close();
      }
    }.start();
  }
}
