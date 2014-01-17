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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UiUtils {
  public static final SimpleDateFormat LONG_DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.getDefault());
  
  private static final HashMap<String, Integer> VALUE_MAP;
  
  public static final int EXPIRED_COLOR_KEY = 0;
  public static final int MARKED_COLOR_KEY = 1;
  public static final int MARKED_FAVORITE_COLOR_KEY = 2;
  public static final int MARKED_REMINDER_COLOR_KEY = 3;
  public static final int MARKED_SYNC_COLOR_KEY = 4;
  
  static {
    VALUE_MAP = new HashMap<String, Integer>();
    
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_ACTORS, R.id.detail_actors);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_REGIE, R.id.detail_regie);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO, R.id.detail_custom);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT, R.id.detail_age_limit);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME, R.id.detail_netto_playtime);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_VPS, R.id.detail_vps);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SCRIPT, R.id.detail_script);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_REPETITION_FROM, R.id.detail_repetition_from);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_MUSIC, R.id.detail_music);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_MODERATION, R.id.detail_moderation);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_REPETITION_ON, R.id.detail_repetition_on);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER, R.id.detail_season);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PRODUCER, R.id.detail_producer);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_CAMERA, R.id.detail_camera);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_CUT, R.id.detail_cut);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS, R.id.detail_other_persons);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM, R.id.detail_production_firm);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING, R.id.detail_age_limit_string);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, R.id.detail_last_production_year);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, R.id.detail_additional_info);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SERIES, R.id.detail_series);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_RATING, R.id.detail_rating);
  }
  
  public static void showProgramInfo(final Context context, long id) {
    showProgramInfo(context, id, null);
  }
  
  public static void showProgramInfo(final Context context, long id, final Activity finish) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    
    Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
    
    if(c.moveToFirst()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      
      View layout =((LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.detail_layout, null);
      
      TextView date = (TextView)layout.findViewById(R.id.detail_date_channel);
      TextView title = (TextView)layout.findViewById(R.id.detail_title);
      TextView genre = (TextView)layout.findViewById(R.id.detail_genre);
      TextView info = (TextView)layout.findViewById(R.id.detail_info);
      TextView episode = (TextView)layout.findViewById(R.id.detail_episode_title);
      TextView shortDescription = (TextView)layout.findViewById(R.id.detail_short_description);
      TextView description = (TextView)layout.findViewById(R.id.detail_description);
      TextView link = (TextView)layout.findViewById(R.id.detail_link);
          
      TextView pictureDescription = (TextView)layout.findViewById(R.id.detail_picture_description);
      TextView pictureCopyright = (TextView)layout.findViewById(R.id.detail_picture_copyright);
      
      Date start = new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
      SimpleDateFormat day = new SimpleDateFormat("EEE",Locale.getDefault());
      
      long channelID = c.getLong(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
      
      Cursor channel = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_LOGO,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, null, null, null);
      
      channel.moveToFirst();
      
      String channelName = "";
      
      if(pref.getBoolean(context.getResources().getString(R.string.SHOW_SORT_NUMBER_IN_DETAILS), true)) {
        channelName = channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER)) + ". ";
      }
      
      channelName += channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
      
      date.setText(day.format(start) + " " + java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(start) + " " + DateFormat.getTimeFormat(context).format(start) + " - " + DateFormat.getTimeFormat(context)/*.getTimeInstance(java.text.DateFormat.SHORT)*/.format(new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)))) + ", " + channelName);
      
      if(!channel.isNull(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO))) {
        byte[] logoData = channel.getBlob(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO));
        Bitmap logo = BitmapFactory.decodeByteArray(logoData, 0, logoData.length);
        BitmapDrawable l = new BitmapDrawable(context.getResources(), logo);
        
        ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
        background.setBounds(0, 0, logo.getWidth() + 2, logo.getHeight() + 2);
        
        LayerDrawable logoDrawable = new LayerDrawable(new Drawable[] {background,l});
        logoDrawable.setBounds(0, 0, logo.getWidth() + 2, logo.getHeight() + 2);
        
        l.setBounds(2, 2, logo.getWidth(), logo.getHeight());
        
        date.setCompoundDrawables(logoDrawable, null, null, null);
      }
      
      channel.close();
      
      String year = "";
          
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN))) {
        year = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN));
      }
      
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR))) {
        if(year.length() > 0) {
          year += " ";
        }
        
        year += c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR));
      }
      
      String originalTitle = null;
      String titleTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
      
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL))) {
        originalTitle = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL));
      }
      
      if(originalTitle == null || originalTitle.equals(titleTest)) {
        title.setText(titleTest);
      }
      else {
        title.setText(titleTest + "/" + originalTitle);
      }
      
      if(!pref.getBoolean(context.getResources().getString(R.string.SHOW_PICTURE_IN_DETAILS), true) ||  c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT))) {
        pictureCopyright.setVisibility(View.GONE);
        pictureDescription.setVisibility(View.GONE);
      }
      else {
        if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION))) {
          pictureDescription.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION)));
        }
        
        pictureCopyright.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));
        
        byte[] pictureData = c.getBlob(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE));
        
        Bitmap image = BitmapFactory.decodeByteArray(pictureData,0,pictureData.length);
        
        BitmapDrawable b = new BitmapDrawable(context.getResources(),image);
        
        float zoom = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.DETAIL_PICTURE_ZOOM), "1.4"));
        
        b.setBounds(0, 0, (int)(image.getWidth() * zoom), (int)(image.getHeight() * zoom));
        
        pictureDescription.setCompoundDrawables(b, null, null, null);
      }
      
      if(pref.getBoolean(context.getResources().getString(R.string.SHOW_GENRE_IN_DETAILS), true) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
        genre.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) + (year.length() > 0 ? " - " + year : ""));
      }
      else if(year.length() > 0) {
        genre.setText(year);
      }
      else {
        genre.setVisibility(View.GONE);
      }
      
      String infoValue = IOUtils.getInfoString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)), context.getResources());
      
      if(pref.getBoolean(context.getResources().getString(R.string.SHOW_INFO_IN_DETAILS), true) && infoValue.length() > 0) {
        info.setText(infoValue);
      }
      else {
        info.setVisibility(View.GONE);
      }
      
      String number = "";
      
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) {
        if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT))) {
          number = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER)) + "/" + c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT)) + " ";
        }
        else {
          number = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER)) + " ";
        }
      }
      
      String originalEpisode = null;
      
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL))) {
        originalEpisode = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL));
      }
      
      if(pref.getBoolean(context.getResources().getString(R.string.SHOW_EPISODE_IN_DETAILS), true) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
        String episodeTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
        
        if(originalEpisode == null || episodeTest.equals(originalEpisode)) {
          episode.setText(number + episodeTest);
        }
        else {
          episode.setText(number + episodeTest + "/" + originalEpisode);
        }
      }
      else if(pref.getBoolean(context.getResources().getString(R.string.SHOW_EPISODE_IN_DETAILS), true) && number.trim().length() > 0) {
        episode.setText(number);
      }
      else {
        episode.setVisibility(View.GONE);
      }
      
      String shortDescriptionValue = null;
      String descriptionValue = null;
      
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
        shortDescriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
      }
      
      if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION))) {
        descriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));
      }
      
      if(shortDescriptionValue == null || (descriptionValue != null && descriptionValue.startsWith(shortDescriptionValue))) {
        shortDescription.setVisibility(View.GONE);
      }
      else {
        shortDescription.setText(shortDescriptionValue);
      }
      
      if(descriptionValue != null) {
        description.setText(descriptionValue);
      }
      else {
        description.setVisibility(View.GONE);
      }
      
      if(pref.getBoolean(context.getResources().getString(R.string.SHOW_LINK_IN_DETAILS), true) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK))) {
        String linkText = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK));
        link.setText(linkText);
        link.setMovementMethod(LinkMovementMethod.getInstance());
      }
      else {
        link.setVisibility(View.GONE);
      }
      
      Set<String> keys = VALUE_MAP.keySet();
      
      for(String key : keys) {
        boolean enabled = pref.getBoolean("details_" + key, true);
        TextView textView = (TextView)layout.findViewById(VALUE_MAP.get(key));
        
        if(textView != null && enabled && !c.isNull(c.getColumnIndex(key))) {
          String text = c.getString(c.getColumnIndex(key));
          
          if(text.trim().length() > 0) {
            try {
              String name = context.getResources().getString((Integer)R.string.class.getField(key).get(null));
              
              boolean endWith = false;
              
              if(name.endsWith(":")) {
                endWith = true;
              }
              
              if(key.equals(TvBrowserContentProvider.DATA_KEY_VPS)) {
                Calendar temp = Calendar.getInstance();
                temp.set(Calendar.HOUR_OF_DAY, Integer.parseInt(text) / 60);
                temp.set(Calendar.MINUTE, Integer.parseInt(text) % 60);
                
                text = DateFormat.getTimeFormat(context).format(temp.getTime());
              }
              
              text = text.replace("\n", "<br>");
              
              name = "<b><u>" + name.replace("\n", "<br>") + "</u></b>" + (endWith ? " " : "");
              
              textView.setText(Html.fromHtml(name + text));
            } catch (Exception e) {
              textView.setVisibility(View.GONE);
            }
          }
          else {
            textView.setVisibility(View.GONE);
          }
        }
        else if(textView != null) {
          textView.setVisibility(View.GONE);
        }
      }
      
      c.close();
      
      if(finish != null) {
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish.finish();
          }
        });/*.setOnDismissListener(new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
            activity.finish();
          }
        });*/
      }
      
      builder.setView(layout);
      builder.show();
    }
  }
  
  public static void createContextMenu(Context context, ContextMenu menu, long id) {
    new MenuInflater(context).inflate(R.menu.program_context, menu);
    
    Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE}, null, null, null);
    
    if(Build.VERSION.SDK_INT < 14) {
      menu.findItem(R.id.prog_create_calendar_entry).setVisible(false);
    }
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      boolean showMark = true;
      boolean showUnMark = false;
      boolean showReminder = true;
      boolean createFavorite = true;
            
      boolean showDontWantToSee = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE)) == 0;
      
      if(!cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
        String markValue = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES)).trim();
        
        showUnMark = markValue.length() > 0 && !markValue.trim().equals(SettingConstants.MARK_VALUE_REMINDER);
        showMark = !markValue.trim().contains(SettingConstants.MARK_VALUE);
        showReminder = !markValue.contains(SettingConstants.MARK_VALUE_REMINDER);
        createFavorite = !markValue.contains(SettingConstants.MARK_VALUE_FAVORITE);
      }
      
      long startTime = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
      
      boolean isFutureReminder = startTime > System.currentTimeMillis() - 5 * 60000;
      boolean isFutureCalendar = startTime > System.currentTimeMillis();
      
      menu.findItem(R.id.prog_mark_item).setVisible(showMark);
      menu.findItem(R.id.prog_unmark_item).setVisible(showUnMark);
      menu.findItem(R.id.prog_add_reminder).setVisible(showReminder && isFutureReminder);
      menu.findItem(R.id.prog_remove_reminder).setVisible(!showReminder);
      menu.findItem(R.id.prog_create_calendar_entry).setVisible(isFutureCalendar);
      menu.findItem(R.id.create_favorite_item).setVisible(createFavorite);
      
      menu.findItem(R.id.program_popup_dont_want_to_see).setVisible(showDontWantToSee && !SettingConstants.UPDATING_FILTER);
      menu.findItem(R.id.program_popup_want_to_see).setVisible(!showDontWantToSee && !SettingConstants.UPDATING_FILTER);
    }
    
    if(context != null && context instanceof TvBrowserSearchResults) {
      menu.findItem(R.id.program_popup_search_repetition).setVisible(false);
    }
    
    cursor.close();
  }
  
  public static void searchForRepetition(final Context activity, String title, String episode) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    
    RelativeLayout layout = (RelativeLayout)((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.search_repetition_layout, null);
    
    final EditText titleText = (EditText)layout.findViewById(R.id.search_repetition_title);
    final EditText episodeText = (EditText)layout.findViewById(R.id.search_repetition_episode);
    
    if(title != null) {
      titleText.setText(title);
    }
    if(episode != null) {
      episodeText.setText(episode);
    }
    
    builder.setTitle(activity.getResources().getString(R.string.program_popup_search_repetition));
    builder.setView(layout);
    
    builder.setPositiveButton(activity.getResources().getString(android.R.string.search_go), new DialogInterface.OnClickListener() {      
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Intent search = new Intent(activity,TvBrowserSearchResults.class);
        
        search.putExtra(SearchManager.QUERY, String.valueOf(titleText.getText()));
        
        String episode = String.valueOf(episodeText.getText()).trim();
        
        if(episode.length() > 0) {
          search.putExtra(TvBrowserSearchResults.QUERY_EXTRA_EPISODE_KEY, episode);
        }
        
        activity.startActivity(search);
      }
    });
    
    builder.setNegativeButton(activity.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {      
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    
    builder.show();
  }
  
  @SuppressLint("NewApi")
  public static boolean handleContextMenuSelection(final Context activity, MenuItem item, long programID, final View menuView) {
    Cursor info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
    
    String current = null;
    String title = null;
    String episode = null;
    
    if(info.getCount() > 0) {
      info.moveToFirst();
      
      if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
        current = info.getString(0);
      }
      
      title = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
      
      if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
        episode = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
      }
    }
    
    info.close();
    
    ContentValues values = new ContentValues();
    
    if(item.getItemId() == R.id.create_favorite_item) {
      UiUtils.editFavorite(null, activity, title);
      return true;
    }
    else if(item.getItemId() == R.id.prog_send_email) {
      info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        
        if(channel.getCount() > 0) {
          channel.moveToFirst();
          Intent sendMail = new Intent(Intent.ACTION_SEND);
          
          StringBuilder message = new StringBuilder();
          StringBuilder subject = new StringBuilder();
          
          String desc = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
            desc = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
            
            if(desc != null && desc.trim().toLowerCase().equals("null")) {
              desc = null;
            }
          }
          
          String startDate = DateFormat.getLongDateFormat(activity).format(new Date(info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))));
          String startTime = DateFormat.getTimeFormat(activity).format(new Date(info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME))));
          String endTime = DateFormat.getTimeFormat(activity).format(new Date(info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME))));
          
          subject.append(startDate).append(", ").append(startTime).append(" - ").append(endTime).append(" ").append(channel.getString(0)).append(": ");
          subject.append(info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
          
          message.append(info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
          
          if(episode != null) {
            subject.append(" - ").append(episode);
            message.append(" - ").append(episode);
          }
          
          if(desc != null) {
            message.append("\n\n").append(desc);
          }
          
          String mail = PreferenceManager.getDefaultSharedPreferences(activity).getString(activity.getString(R.string.PREF_EMAIL_TARGET_ADDRESS), null);
          
          if(mail != null) {
            sendMail.putExtra(Intent.EXTRA_EMAIL, new String[]{mail});
          }
          
          sendMail.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
          sendMail.putExtra(Intent.EXTRA_TEXT, message.toString());
          sendMail.setType("text/rtf");
          activity.startActivity(Intent.createChooser(sendMail, activity.getResources().getString(R.string.log_send_mail)));
        }
        
        channel.close();
      }
      
      info.close();
      
      return true;
    }
    else if(item.getItemId() == R.id.program_popup_search_repetition) {
      searchForRepetition(activity,title,episode);
    }
    else if(item.getItemId() == R.id.prog_mark_item) {
      if(current != null && current.contains(SettingConstants.MARK_VALUE)) {
        return true;
      }
      else if(current == null || current.trim().length() == 0) {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, SettingConstants.MARK_VALUE);
      }
      else {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";"+SettingConstants.MARK_VALUE);
      }
    }
    else if(item.getItemId() == R.id.prog_unmark_item){
      if(current == null || current.trim().length() == 0) {
        return true;
      }
      
      if(current.contains(SettingConstants.MARK_VALUE_REMINDER)) {
        current = SettingConstants.MARK_VALUE_REMINDER;
      }
      else {
        current = "";
      }
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
      
      if(menuView != null) {
        if(current.trim().length() == 0) {
          menuView.setBackgroundResource(android.R.drawable.list_selector_background);  
        }
        else {
          handleMarkings(activity, null, menuView, current);
        }
      }
    }
    else if(item.getItemId() == R.id.prog_create_calendar_entry) {
      info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        
        if(channel.getCount() > 0) {
          channel.moveToFirst();
          
          // Create a new insertion Intent.
          Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT);
          
          addCalendarEntry.setType(activity.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
          
          String desc = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
            desc = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
            
            if(desc != null && desc.trim().toLowerCase().equals("null")) {
              desc = null;
            }
          }
          
          addCalendarEntry.putExtra(Events.EVENT_LOCATION, channel.getString(0));
          // Add the calendar event details
          addCalendarEntry.putExtra(Events.TITLE, info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
          
          String description = null;
          
          if(episode != null) {
            description = episode;
          }
          
          if(desc != null) {
            if(description != null) {
              description += "\n\n" + desc;
            }
            else {
              description = desc;
            }
          }
          
          if(description != null) {
            addCalendarEntry.putExtra(Events.DESCRIPTION, description);
          }
          
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)));
          
          // Use the Calendar app to add the new event.
          activity.startActivity(addCalendarEntry);
          
          if(current != null && current.contains(SettingConstants.MARK_VALUE_CALENDAR)) {
            return true;
          }
          else if(current == null || current.trim().length() == 0) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, SettingConstants.MARK_VALUE_CALENDAR);
          }
          else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";"+SettingConstants.MARK_VALUE_CALENDAR);
          }
          
          if(menuView != null) {
            menuView.setBackgroundColor(getColor(MARKED_REMINDER_COLOR_KEY, activity));
          }
        }
        
        channel.close();
      }
      
      info.close();
    }
    else if(item.getItemId() == R.id.prog_add_reminder) {
      if(current != null && current.contains(SettingConstants.MARK_VALUE_REMINDER)) {
        return true;
      }
      else if(current == null || current.trim().length() == 0) {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, SettingConstants.MARK_VALUE_REMINDER);
      }
      else {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";"+SettingConstants.MARK_VALUE_REMINDER);
      }
      
      addReminder(activity.getApplicationContext(),programID,0);
    }
    else if(item.getItemId() == R.id.prog_remove_reminder) {
      if(current == null || current.trim().length() == 0 || !current.contains(SettingConstants.MARK_VALUE_REMINDER)) {
        return true;
      }
      
      current = current.replace(SettingConstants.MARK_VALUE_REMINDER, "").replace(";;", ";");
      
      if(current.endsWith(";")) {
        current = current.substring(0,current.length()-1);
      }
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
      values.put(TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER, 1);
      
      if(menuView != null) {
        if(current.trim().length() == 0) {
          menuView.setBackgroundResource(android.R.drawable.list_selector_background);  
        }
        else {
          handleMarkings(activity, null, menuView, current);
        }
      }
      
      removeReminder(activity.getApplicationContext(),programID);
    }
    else if(item.getItemId() == R.id.program_popup_dont_want_to_see) {
      if(title != null) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
        builder.setTitle(R.string.action_dont_want_to_see);
        
        View view = ((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dont_want_to_see_edit, null);
        
        final TextView exclusion = (TextView)view.findViewById(R.id.dont_want_to_see_value);
        final CheckBox caseSensitive = (CheckBox)view.findViewById(R.id.dont_want_to_see_case_sensitve);
        exclusion.setText(title);
        
        builder.setView(view);
        
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if(!SettingConstants.UPDATING_FILTER) {
              SettingConstants.UPDATING_FILTER = true;
              
              String key = activity.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES);
              Set<String> dontWantToSeeSet = PreferenceManager.getDefaultSharedPreferences(activity).getStringSet(key, null);
              
              HashSet<String> newDontWantToSeeSet = new HashSet<String>();
                          
              String exclusionText = exclusion.getText().toString().trim();
              boolean caseSensitiveValue = caseSensitive.isChecked();
              
              if(exclusionText.length() > 0) {
                if(dontWantToSeeSet != null) {
                  newDontWantToSeeSet.addAll(dontWantToSeeSet);
                }
                
                final String exclusion = exclusionText + ";;" + (caseSensitiveValue ? "1" : "0");
                
                new Thread() {
                  public void run() {
                    if(activity instanceof TvBrowser) {
                      ((TvBrowser)activity).updateProgressIcon(true);
                    }
                    
                    NotificationCompat.Builder builder;
                    
                    builder = new NotificationCompat.Builder(activity);
                    builder.setSmallIcon(R.drawable.ic_launcher);
                    builder.setOngoing(true);
                    builder.setContentTitle(activity.getResources().getText(R.string.action_dont_want_to_see));
                    builder.setContentText(activity.getResources().getText(R.string.dont_want_to_see_refresh_notification_text));
                    
                    int notifyID = 4;
                    
                    NotificationManager notification = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
                    notification.notify(notifyID, builder.build());
                    
                    Cursor c = activity.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE}, " NOT " +TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.KEY_ID);
  
                    int size = c.getCount();
                    int count = 0;
                    
                    builder.setProgress(100, 0, true);
                    notification.notify(notifyID, builder.build());
                    
                    ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
                    
                    int keyColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                    int titleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
                    DontWantToSeeExclusion exclusionValue = new DontWantToSeeExclusion(exclusion);
                    
                    int lastPercent = 0;
                    
                    while(c.moveToNext()) {
                      int percent = (int)(count++/(float)size * 100);
                      
                      if(lastPercent != percent) {
                        lastPercent = percent;
                        builder.setProgress(100,percent, false);
                        notification.notify(notifyID, builder.build());
                      }
                      
                      String title = c.getString(titleColumn);
                      
                      if(UiUtils.filter(title, exclusionValue)) {
                        ContentValues values = new ContentValues();
                        values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, 1);
                        
                        ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, c.getLong(keyColumn)));
                        opBuilder.withValues(values);
                        
                        updateValuesList.add(opBuilder.build());
                      }
                      else {
                        try {
                          sleep(1);
                        } catch (InterruptedException e) {
                          // TODO Auto-generated catch block
                          e.printStackTrace();
                        }
                      }
                    }
                    
                    c.close();
                    
                    if(!updateValuesList.isEmpty()) {
                      try {
                        activity.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                        sendDontWantToSeeChangedBroadcast(activity,true);
                      } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                      } catch (OperationApplicationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                      }
                    }
                    
                    notification.cancel(notifyID);
                    
                    if(activity instanceof TvBrowser) {
                      ((TvBrowser)activity).updateProgressIcon(false);
                    }
                    
                    SettingConstants.UPDATING_FILTER = false;
                  }
                }.start();
                
                newDontWantToSeeSet.add(exclusion);
                
                Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();
                
                edit.putStringSet(key, newDontWantToSeeSet);
                edit.commit();
              }
            }
          }
        });
        
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {}
        });
        
        builder.show();
      }
      
      return true;
    }
    else if(item.getItemId() == R.id.program_popup_want_to_see) {
      if(title != null && !SettingConstants.UPDATING_FILTER) {
        SettingConstants.UPDATING_FILTER = true;
        
        Set<String> exclusionValues = PreferenceManager.getDefaultSharedPreferences(activity).getStringSet(activity.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), null);
        //ArrayList<>
        HashSet<String> newExclusionSet = new HashSet<String>();
        final ArrayList<DontWantToSeeExclusion> exclusionList = new ArrayList<DontWantToSeeExclusion>();
        
        for(String exclusion : exclusionValues) {
          if(!filter(title, new DontWantToSeeExclusion(exclusion))) {
            newExclusionSet.add(exclusion);
            exclusionList.add(new DontWantToSeeExclusion(exclusion));
          }
        }
        
        new Thread() {
          public void run() {
            if(activity instanceof TvBrowser) {
              ((TvBrowser)activity).updateProgressIcon(true);
            }
            
            NotificationCompat.Builder builder;
            
            builder = new NotificationCompat.Builder(activity);
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setOngoing(true);
            builder.setContentTitle(activity.getResources().getText(R.string.action_dont_want_to_see));
            builder.setContentText(activity.getResources().getText(R.string.dont_want_to_see_refresh_notification_text));
            
            int notifyID = 3;
            
            NotificationManager notification = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
            notification.notify(notifyID, builder.build());
            
            Cursor c = activity.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE}, TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.KEY_ID);

            int size = c.getCount();
            int count = 0;
            
            builder.setProgress(size, 0, true);
            notification.notify(notifyID, builder.build());
            
            ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<ContentProviderOperation>();
            
            int keyColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
            int titleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
            
            DontWantToSeeExclusion[] exclusionArr = exclusionList.toArray(new DontWantToSeeExclusion[exclusionList.size()]);
            
            while(c.moveToNext()) {
              builder.setProgress(size, count++, false);
              notification.notify(notifyID, builder.build());
              
              String title = c.getString(titleColumn);
              
              ContentValues values = new ContentValues();
              values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, (UiUtils.filter(activity, title, exclusionArr) ? 1 : 0));
              
              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, c.getLong(keyColumn)));
              opBuilder.withValues(values);
              
              updateValuesList.add(opBuilder.build());
            }
            
            notification.cancel(notifyID);
            
            c.close();
            
            if(!updateValuesList.isEmpty()) {
              try {
                activity.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                sendDontWantToSeeChangedBroadcast(activity,false);
              } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              } catch (OperationApplicationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
            
            if(activity instanceof TvBrowser) {
              ((TvBrowser)activity).updateProgressIcon(false);
            }
            
            SettingConstants.UPDATING_FILTER = false;
          }
        }.start();
        
        Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        
        edit.putStringSet(activity.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), newExclusionSet);
        edit.commit();
      }
      
      return true;
    }
    
    if(values.size() > 0) {
      if(menuView != null) {
        menuView.invalidate();
      }
      
      activity.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
      
      sendMarkingChangedBroadcast(activity, programID);
    }
    
    return true;
  }
  
  public static void sendDontWantToSeeChangedBroadcast(Context context, boolean added) {
    Intent intent = new Intent(SettingConstants.DONT_WANT_TO_SEE_CHANGED);
    intent.putExtra(SettingConstants.DONT_WANT_TO_SEE_ADDED_EXTRA, added);
    
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
  }
  
  private static void sendMarkingChangedBroadcast(Context context, long programID) {
    Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
    intent.putExtra(SettingConstants.MARKINGS_ID, programID);
    
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
  }
  
  //TODO
  public static void addReminder(Context context, long programID, long startTime) {try {
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    
    int reminderTime = Integer.parseInt(pref.getString(context.getString(R.string.PREF_REMINDER_TIME), "0")) * 60000;
    boolean remindAgain = pref.getBoolean(context.getString(R.string.PREF_REMIND_AGAIN_AT_START), true);
    
    Intent remind = new Intent(context,ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    if(startTime <= 0) {
      Cursor time = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME}, null, null, null);
      
      if(time.moveToFirst()) {
        startTime = time.getLong(0);
      }
      
      time.close();
    }
    
    if(startTime > System.currentTimeMillis()) {
      PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
      
      if(startTime-reminderTime > System.currentTimeMillis()) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, startTime-reminderTime, pending);
        
        if(remindAgain && reminderTime > 0) {
          pending = PendingIntent.getBroadcast(context, (int)-programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
          
          alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, pending);
        }
      }
      else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, pending);
      }
    }
  }catch(Throwable t) {t.printStackTrace();}
  }
  //TODO
  public static void removeReminder(Context context, long programID) {
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent remind = new Intent(context,ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
    
    alarmManager.cancel(pending);
    
    pending = PendingIntent.getBroadcast(context, (int)-programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
    
    alarmManager.cancel(pending);
  }
  
  private static class RunningDrawable extends Drawable {
    private Paint mBase;
    private Paint mSecond;
    private long mStartTime;
    private long mEndTime;
    private boolean mVertical;
    
    public RunningDrawable(Paint base, Paint second, long startTime, long endTime, boolean vertical) {
      mBase = base;
      mSecond = second;
      mStartTime = startTime;
      mEndTime = endTime;
      mVertical = vertical;
    }
    
    @Override
    public void draw(Canvas canvas) {
      if(mStartTime <= System.currentTimeMillis() && System.currentTimeMillis() < mEndTime) {
        long expiredSeconds = System.currentTimeMillis() - mStartTime;
        float percent = expiredSeconds/(float)(mEndTime - mStartTime);
        
        if(mVertical) {
          int topHeight = (int)(getBounds().height() * percent);
          
          canvas.drawRect(0, 0, getBounds().width(), topHeight, mBase);
          canvas.drawRect(0, topHeight, getBounds().width(), getBounds().height(), mSecond);
        }
        else {
          int leftWidth = (int)(getBounds().width() * percent);
        
          canvas.drawRect(0, 0, leftWidth, getBounds().height(), mBase);
          canvas.drawRect(leftWidth, 0, getBounds().width(), getBounds().height(), mSecond);
        }
      }
    }

    @Override
    public int getOpacity() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void setAlpha(int alpha) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
      // TODO Auto-generated method stub
      
    }
    
  }
  
  public static void handleMarkings(Context activity, Cursor cursor, View view, String markingValues) {
    handleMarkings(activity, cursor, view, markingValues, null, false);
  }
  
  public static void handleMarkings(Context activity, Cursor cursor, View view, String markingValues, Handler handler, boolean vertical) {
    long startTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) : 0;
    long endTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) : 0;
    
    handleMarkings(activity, cursor, startTime, endTime, view, markingValues,handler,vertical);
  }
  
  public static void handleMarkings(Context activity, Cursor cursor, long startTime, long endTime, View view, String markingValues) {
    handleMarkings(activity, cursor, startTime, endTime, view, markingValues, null);
  }
  
  public static void handleMarkings(Context activity, Cursor cursor, long startTime, long endTime, final View view, String markingValues, Handler handler) {
    handleMarkings(activity, cursor, startTime, endTime, view, markingValues, handler, false);
  }
  
  public static void handleMarkings(Context activity, Cursor cursor, long startTime, long endTime, final View view, String markingValues, Handler handler, boolean vertical) {
    String value = markingValues;
    
    if(value == null && cursor != null && !cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
      value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
    }
    
    Paint base = new Paint();
    base.setStyle(Paint.Style.FILL_AND_STROKE);
    
    Paint second = null;
        
    if(startTime <= System.currentTimeMillis() && System.currentTimeMillis() <= endTime) {
      base.setColor(0x400000FF);
      second = new Paint();
      second.setStyle(Paint.Style.FILL_AND_STROKE);
      second.setColor(0x150000FF);
    }
    else {
      base = null;
    }
    
    final ArrayList<Drawable> draw = new ArrayList<Drawable>();
    
    if(base != null) {
      RunningDrawable running = new RunningDrawable(base, second, startTime, endTime, vertical);
      draw.add(running);
    }
    
    if(value != null && value.trim().length() > 0) {
      if(value.startsWith(";")) {
        value = value.substring(1);
      }
      
      String[] markings = value.split(";");
      
      if(markings.length > 1) {
        int[] colors = new int[markings.length];
        
        for(int i = 0; i < markings.length; i++) {
          Integer color = SettingConstants.MARK_COLOR_KEY_MAP.get(markings[i]);
          
          if(color != null) {
            colors[i] = getColor(color.intValue(), activity);
          }
        }
        
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors);
        gd.setCornerRadius(0f);
        
        draw.add(gd);
      }
      else {
        Integer color = SettingConstants.MARK_COLOR_KEY_MAP.get(value);
        
        if(color != null) {
          draw.add(new ColorDrawable(getColor(color.intValue(), activity)));
        }
      }
    }
    
    draw.add(activity.getResources().getDrawable(android.R.drawable.list_selector_background));
    
    if(handler == null) {
      view.setBackgroundDrawable(new LayerDrawable(draw.toArray(new Drawable[draw.size()])));
    }
    else{
      handler.post(new Runnable() {
        @Override
        public void run() {
          view.setBackgroundDrawable(new LayerDrawable(draw.toArray(new Drawable[draw.size()])));
        }
      });
    }
  }
  
  public static void editFavorite(final Favorite fav, final Context activity, String searchString) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    
    final View input = ((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.add_favorite_layout, null);
    
    if(fav != null) {
      ((EditText)input.findViewById(R.id.favorite_name)).setText(fav.getName());
      ((EditText)input.findViewById(R.id.favorite_search_value)).setText(fav.getSearchValue());
      ((CheckBox)input.findViewById(R.id.favorite_only_title)).setChecked(fav.searchOnlyTitle());
      ((CheckBox)input.findViewById(R.id.favorite_remind)).setChecked(fav.remind());
    }
    else if(searchString != null) {
      ((EditText)input.findViewById(R.id.favorite_search_value)).setText(searchString);
    }
    
    builder.setView(input);
    
    builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        new Thread() {
          public void run() {
            String name = ((EditText)input.findViewById(R.id.favorite_name)).getText().toString();
            String search = ((EditText)input.findViewById(R.id.favorite_search_value)).getText().toString();
            boolean onlyTitle = ((CheckBox)input.findViewById(R.id.favorite_only_title)).isChecked();
            boolean remind = ((CheckBox)input.findViewById(R.id.favorite_remind)).isChecked();
            
            if(name == null || name.trim().length() == 0) {
              name = search;
            }
            
            if(search != null) {
              Intent intent = new Intent(SettingConstants.FAVORITES_CHANGED);
                       
              if(fav != null) {
                intent.putExtra(Favorite.OLD_NAME_KEY, fav.getName());
              }
              
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
              
              Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
              HashSet<String> newFavoriteList = new HashSet<String>();
              
              Favorite dummy = new Favorite(name, search, onlyTitle, remind);
              boolean added = false;
              
              for(String favorite : favoritesSet) {
                String[] values = favorite.split(";;");
                
                if(fav != null && values[0].equals(fav.getName())) {                
                  newFavoriteList.add(dummy.getSaveString());
                  added = true;
                }
                else {
                  newFavoriteList.add(favorite);
                }
              }
                
              if(!added) {
                newFavoriteList.add(dummy.getSaveString());
              }
              
              Editor edit = prefs.edit();
              edit.putStringSet(SettingConstants.FAVORITE_LIST, newFavoriteList);
              edit.commit();
              
              intent.putExtra(Favorite.NAME_KEY, name);
              intent.putExtra(Favorite.SEARCH_KEY, search);
              intent.putExtra(Favorite.ONLY_TITLE_KEY, onlyTitle);
              intent.putExtra(Favorite.REMIND_KEY, remind);
              
              Favorite.updateFavoriteMarking(activity.getApplicationContext(), activity.getContentResolver(), dummy);
              
              LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
            }
          }
        }.start();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    
    AlertDialog dialog = builder.create();
    dialog.show();
  }
  
  public static String formatDate(long date, Context context, boolean onlyDays) {
    return formatDate(date, context, onlyDays, false);
  }
  
  public static String formatDate(long date, Context context, boolean onlyDays, boolean withDayString) {
    Calendar progDate = Calendar.getInstance();
    progDate.setTimeInMillis(date);
    
    Calendar today = Calendar.getInstance();
    
    String value = null;
    
    if(progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
      value = context.getText(R.string.today).toString();
    }
    else {
      today.add(Calendar.DAY_OF_YEAR, 1);
      
      if(progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
        value = context.getText(R.string.tomorrow).toString();
      }
      else {
        today.add(Calendar.DAY_OF_YEAR, -2);
        
        if(progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
          value = context.getText(R.string.yesterday).toString();
        }
      }
    }
    
    if(value == null) {
      if(!onlyDays) {
        SimpleDateFormat df = (SimpleDateFormat)java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
        String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");
        
        if(withDayString) {
          pattern = "EEE " + pattern;
        }
        
        if(pattern.contains(".")) {
          pattern += ".";
        }
        
        SimpleDateFormat mdf = new SimpleDateFormat(pattern);
        
        value = mdf.format(progDate.getTime());
      }
      else if(withDayString) {
        SimpleDateFormat mdf = new SimpleDateFormat("EEE ");
        
        value = mdf.format(progDate.getTime());
      }
    }
    
    return value;
  }
  
  public static void formatDayView(Activity activity, Cursor cursor, View view, int startDayLabelID) {try {
    long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
    
    TextView text = (TextView)view;
    
    SimpleDateFormat day = new SimpleDateFormat("EEE", Locale.getDefault());
    
    TextView startDay = (TextView)((View)view.getParent()).findViewById(/*R.id.startDayLabelPL*/startDayLabelID);
    startDay.setText(day.format(new Date(date)));
    
    CharSequence startDayValue = formatDate(date, activity, true);
    
    if(startDayValue != null) {
      startDay.setText(startDayValue + ", " + startDay.getText());
    }
    
    Calendar progDate = Calendar.getInstance();
    progDate.setTimeInMillis(date);

    SimpleDateFormat df = (SimpleDateFormat)java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
    String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");
    
    pattern = pattern.replace(".MM", ". MMM");
    pattern = pattern.replace("MM.", "MMM. ");
    
    SimpleDateFormat mdf = new SimpleDateFormat(pattern);
    
    text.setText(mdf.format(progDate.getTime()));
    
    UiUtils.handleMarkings(activity, cursor, ((RelativeLayout)view.getParent()), null);}catch(Throwable t) {}
  }
  
  public static int convertDpToPixel(float dp, Resources res) {
    // Get the screen's density scale
    final float scale = res.getDisplayMetrics().density;
    // Convert the dps to pixels, based on density scale
    
    return (int) (dp * scale + 0.5f);
  }
  
  public static boolean filter(String title, DontWantToSeeExclusion exclusion) {
    return exclusion.matches(title);
  }
  
  public static boolean filter(Context context, String title, DontWantToSeeExclusion[] values) {
    boolean found = false;
    
    if(title != null) {
      if(values == null) {
        Set<String> exclusionValues = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(context.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), null);
        //ArrayList<>
        values = new DontWantToSeeExclusion[exclusionValues.size()];
        
        int i = 0;
        
        for(String exclusion : exclusionValues) {
          values[i++] = new DontWantToSeeExclusion(exclusion);
        }
      }
      
      
      if(values != null) {
        for(DontWantToSeeExclusion value : values) {
          if(filter(title, value)) {
            found = true;
            break;
          }
        }
      }
        /*    
      if(clear) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        
        edit.putStringSet(context.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), cleared);
        edit.commit();
      }*/
    }
    
    return found;
  }
  
  public static final int getColor(int key, Context context) throws NullPointerException {
    if(context == null) {
      throw new NullPointerException("Context parameter is null.");
    }
    
    return getColor(key, PreferenceManager.getDefaultSharedPreferences(context),context);
  }
  
  public static final int getColor(int key, SharedPreferences pref, Context context) throws NullPointerException {
    if(pref == null) {
      throw new NullPointerException("Preferences parameter is null.");
    }
    else if(context == null) {
      throw new NullPointerException("Context parameter is null.");
    }
    
    int color = 0;
    
    switch(key) {
      case EXPIRED_COLOR_KEY:
          if(SettingConstants.IS_DARK_THEME) {
            color = SettingConstants.EXPIRED_DARK_COLOR;
          }
          else {
            color = SettingConstants.EXPIRED_LIGHT_COLOR;
          }
          break;
      case MARKED_COLOR_KEY: color = context.getResources().getColor(R.color.mark_color);break;
      case MARKED_FAVORITE_COLOR_KEY: color = context.getResources().getColor(R.color.mark_color_favorite);break;
      case MARKED_REMINDER_COLOR_KEY: color = context.getResources().getColor(R.color.mark_color_calendar);break;
      case MARKED_SYNC_COLOR_KEY: color = context.getResources().getColor(R.color.mark_color_sync_favorite);break;
    }
    
    return color;
  }
  
  public static void handleConfigurationChange(Handler handler, final BaseAdapter adapter, Configuration newConfig) {
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          adapter.notifyDataSetChanged();
        }
      });
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
      handler.post(new Runnable() {
        @Override
        public void run() {
          adapter.notifyDataSetChanged();
        }
      });
    }
  }
  
  public static Bitmap drawableToBitmap (Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
        return ((BitmapDrawable)drawable).getBitmap();
    }

    Bitmap bitmap = Bitmap.createBitmap(drawable.getBounds().width(), drawable.getBounds().height(), Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap); 
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
  }
}
