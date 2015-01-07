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
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.settings.PrefUtils;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.widgets.ImportantProgramsListWidget;
import org.tvbrowser.widgets.RunningProgramsListWidget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
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
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
  public static final int ON_AIR_BACKGROUND_KEY = 5;
  public static final int ON_AIR_PROGRESS_KEY = 6;
  public static final int RUNNING_TIME_SELECTION_KEY = 7;
  public static final int I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR_KEY = 8;
  
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
  
  public static void showProgramInfo(final Context context, long id, View parent) {
    showProgramInfo(context, id, null, parent);
  }
  
  public static void showProgramInfo(final Context context, final long id, final Activity finish, final View parent) {
    AsyncTask<Void, Void, Boolean> createInfoTask = new AsyncTask<Void, Void, Boolean>() {
      private View mLayout;
      private ProgressDialog mProgress;
      private Handler mHandler;
      private Runnable mShowWaiting;
      private boolean mShowWaitingDialog;
      
      @Override
      protected void onPreExecute() {
        mShowWaitingDialog = true;
        mHandler = new Handler();
        mShowWaiting = new Runnable() {
          @Override
          public void run() {
            mProgress = new ProgressDialog(context);
            mProgress.setMessage(context.getString(R.string.waiting_detail_show));
            
            if(mShowWaitingDialog) {
              mProgress.show();
            }
          }
        };
        mLayout = ((LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.detail_layout, parent instanceof ViewGroup ? (ViewGroup)parent : null, false);
        mHandler.postDelayed(mShowWaiting, 700);
      }
      
      @Override
      protected Boolean doInBackground(Void... params) {
        Boolean result = Boolean.valueOf(false);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        
        Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
        
        try {
          if(c.moveToFirst()) {
            result = Boolean.valueOf(true);
            float textScale = Float.parseFloat(PrefUtils.getStringValue(R.string.DETAIL_TEXT_SCALE, R.string.detail_text_scale_default));
            
            TextView date = (TextView)mLayout.findViewById(R.id.detail_date_channel);
            TextView title = (TextView)mLayout.findViewById(R.id.detail_title);
            TextView genre = (TextView)mLayout.findViewById(R.id.detail_genre);
            TextView info = (TextView)mLayout.findViewById(R.id.detail_info);
            TextView episode = (TextView)mLayout.findViewById(R.id.detail_episode_title);
            TextView shortDescription = (TextView)mLayout.findViewById(R.id.detail_short_description);
            TextView description = (TextView)mLayout.findViewById(R.id.detail_description);
            TextView link = (TextView)mLayout.findViewById(R.id.detail_link);
                
            TextView pictureDescription = (TextView)mLayout.findViewById(R.id.detail_picture_description);
            TextView pictureCopyright = (TextView)mLayout.findViewById(R.id.detail_picture_copyright);
            
            date.setTextSize(TypedValue.COMPLEX_UNIT_PX, date.getTextSize() * textScale);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, date.getTextSize() * textScale);
            genre.setTextSize(TypedValue.COMPLEX_UNIT_PX, genre.getTextSize() * textScale);
            info.setTextSize(TypedValue.COMPLEX_UNIT_PX, info.getTextSize() * textScale);
            episode.setTextSize(TypedValue.COMPLEX_UNIT_PX, episode.getTextSize() * textScale);
            shortDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, shortDescription.getTextSize() * textScale);
            description.setTextSize(TypedValue.COMPLEX_UNIT_PX, description.getTextSize() * textScale);
            link.setTextSize(TypedValue.COMPLEX_UNIT_PX, link.getTextSize() * textScale);
            pictureDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, pictureDescription.getTextSize() * textScale);
            pictureCopyright.setTextSize(TypedValue.COMPLEX_UNIT_PX, pictureCopyright.getTextSize() * textScale);
            
            Date start = new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
            SimpleDateFormat day = new SimpleDateFormat("EEE",Locale.getDefault());
            
            int channelID = c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
            
            Cursor channel = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME,TvBrowserContentProvider.CHANNEL_KEY_LOGO,TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, null, null, null);
            
            channel.moveToFirst();
            
            String channelName = "";
            
            if(PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_DETAILS, R.bool.show_sort_number_in_details_default)) {
              channelName = channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER)) + ". ";
            }
            
            channelName += channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME));
            
            date.setText(day.format(start) + " " + java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(start) + " " + DateFormat.getTimeFormat(context).format(start) + " - " + DateFormat.getTimeFormat(context)/*.getTimeInstance(java.text.DateFormat.SHORT)*/.format(new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)))) + ", " + channelName);
               
            Bitmap logo = UiUtils.createBitmapFromByteArray(channel.getBlob(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));
            
            if(logo != null) {
              float scale = context.getResources().getDisplayMetrics().density;
              
              int width = (int)(logo.getWidth() * scale);
              int height = (int)(logo.getHeight() * scale);
  
              BitmapDrawable l = new BitmapDrawable(context.getResources(), logo);
              
              int color = PrefUtils.getIntValue(R.string.PREF_LOGO_BACKGROUND_COLOR, context.getResources().getColor(R.color.pref_logo_background_color_default));
              
              GradientDrawable background = new GradientDrawable(Orientation.BOTTOM_TOP,new int[] {color,color});
              
              int add = 2;
              
              if(PrefUtils.getBooleanValue(R.string.PREF_LOGO_BORDER, R.bool.pref_logo_border_default)) {
                add = 3;
                background.setStroke(1, PrefUtils.getIntValue(R.string.PREF_LOGO_BORDER_COLOR, context.getResources().getColor(R.color.pref_logo_border_color_default)));
              }
              
              background.setBounds(0, 0, width + add, height + add);
              
              LayerDrawable logoDrawable = new LayerDrawable(new Drawable[] {background,l});
              logoDrawable.setBounds(0, 0, width + add, height + add);
              
              l.setBounds(2, 2, width, height);
              
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
            
            if(!PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_DETAILS, R.bool.show_picture_in_details_default) ||  c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT))) {
              pictureCopyright.setVisibility(View.GONE);
              pictureDescription.setVisibility(View.GONE);
            }
            else {
              if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION))) {
                pictureDescription.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION)));
              }
              
              pictureCopyright.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));
              
              Bitmap image = UiUtils.createBitmapFromByteArray(c.getBlob(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)));
              
              if(image != null) {        
                BitmapDrawable b = new BitmapDrawable(context.getResources(),image);
                
                float zoom = Float.parseFloat(PrefUtils.getStringValue(R.string.DETAIL_PICTURE_ZOOM, R.string.detail_picture_zoom_default)) * context.getResources().getDisplayMetrics().density;
                
                b.setBounds(0, 0, (int)(image.getWidth() * zoom), (int)(image.getHeight() * zoom));
                
                if(PrefUtils.getStringValue(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION, R.string.detail_picture_description_position_default).equals("0")) {
                  pictureDescription.setCompoundDrawables(b, null, null, null);
                }
                else {
                  pictureDescription.setCompoundDrawables(null, b, null, null);
                }
              }
            }
            
            if(PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_DETAILS, R.bool.show_genre_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
              genre.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) + (year.length() > 0 ? " - " + year : ""));
            }
            else if(year.length() > 0) {
              genre.setText(year);
            }
            else {
              genre.setVisibility(View.GONE);
            }
            
            Spannable infoValue = IOUtils.getInfoString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)), context.getResources());
            
            if(PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_DETAILS, R.bool.show_info_in_details_default) && infoValue != null) {
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
            
            if(PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
              String episodeTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
              
              if(originalEpisode == null || episodeTest.equals(originalEpisode)) {
                episode.setText(number + episodeTest);
              }
              else {
                episode.setText(number + episodeTest + "/" + originalEpisode);
              }
            }
            else if(PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && number.trim().length() > 0) {
              episode.setText(number);
            }
            else {
              episode.setVisibility(View.GONE);
            }
            
            String shortDescriptionValue = null;
            String descriptionValue = null;
            boolean showShortDescription = true;
                
            if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
              shortDescriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)).trim();
            }
            
            if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION))) {
              descriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));
              
              if(shortDescriptionValue != null) {
                String test = shortDescriptionValue;
                
                if(test.endsWith("...")) {
                  test = test.substring(0,test.length()-3);
                }
                else if(test.endsWith("\u2026")) {
                  test = test.substring(0,test.length()-1);
                }
                
                test = test.replaceAll("\\s+", " ").trim();
                
                showShortDescription = !descriptionValue.replaceAll("\\s+", " ").trim().startsWith(test);
                
                String[] paragraphs = descriptionValue.split("\n+");
                
                if(paragraphs.length > 1) {
                  String para0 = paragraphs[0].replaceAll("\\s+", " ").trim();
                  String para1 = paragraphs[1].replaceAll("\\s+", " ").trim();
                  
                  if(para1.startsWith(para0) || para0.startsWith(para1)) {
                    descriptionValue = descriptionValue.substring(paragraphs[0].length()+1).trim();
                  }
                }
              }
            }
            
            if(shortDescriptionValue == null || !showShortDescription) {
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
            
            if(PrefUtils.getBooleanValue(R.string.SHOW_LINK_IN_DETAILS, R.bool.show_link_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK))) {
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
              TextView textView = (TextView)mLayout.findViewById(VALUE_MAP.get(key));
              textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() * textScale);
              
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
          }
        }finally {
          IOUtils.closeCursor(c);
        }
        
        return result;
      }
      
      @Override
      protected void onPostExecute(Boolean result) {
        mHandler.removeCallbacks(mShowWaiting);
        mShowWaitingDialog = false;
        
        if(mProgress != null) {
          mProgress.dismiss();
        }
        
        if(result.booleanValue()) {
          AlertDialog.Builder builder = new AlertDialog.Builder(context);
          
          if(finish != null) {
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                finish.finish();
              }
            });
          }
          
          builder.setView(mLayout);
          builder.show();
        }
      }
    };
    
    createInfoTask.execute();
  }
  
  public static void createContextMenu(final Context context, ContextMenu menu, final long id) {
    new MenuInflater(context).inflate(R.menu.program_context, menu);
    
    String[] projection = TvBrowserContentProvider.getColumnArrayWithMarkingColums(ProgramUtils.DATA_CHANNEL_PROJECTION);
    
    Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, id), projection, null, null, null);
    
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      final Program pluginProgram = ProgramUtils.createProgramFromDataCursor(context, cursor);/*new Program(id, startTime, endTime, title, shortDescription, description, episodeTitle, channel);*/
      
      boolean showReminder = true;
      boolean isFavoriteReminder = false;
      boolean createFavorite = true;
      
      for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        int index = cursor.getColumnIndex(column);
        
        if(index >= 0) {
          if(column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER)) {
            showReminder = column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER) && cursor.getInt(index) == 0;
          }
          else if(column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) {
            isFavoriteReminder = column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER) && cursor.getInt(index) == 1;
          }
          
          if(column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE)) {
            createFavorite = column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE) && cursor.getInt(index) == 0;
          }
        }
      }
      
      showReminder = showReminder && !isFavoriteReminder;
            
      boolean showDontWantToSee = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE)) == 0;
      boolean isFutureReminder = pluginProgram.getStartTimeInUTC() > System.currentTimeMillis() - 5 * 60000;
      
      menu.findItem(R.id.prog_add_reminder).setVisible(showReminder && isFutureReminder);
      menu.findItem(R.id.prog_remove_reminder).setVisible(!showReminder);
      menu.findItem(R.id.create_favorite_item).setVisible(createFavorite);
      
      menu.findItem(R.id.program_popup_dont_want_to_see).setVisible(showDontWantToSee && !SettingConstants.UPDATING_FILTER);
      menu.findItem(R.id.program_popup_want_to_see).setVisible(!showDontWantToSee && !SettingConstants.UPDATING_FILTER);
      
      if(PluginHandler.hasPlugins()) {
        PluginServiceConnection[] connections = PluginHandler.getAvailablePlugins();
        
        for(PluginServiceConnection pluginService : connections) {
          final Plugin plugin = pluginService.getPlugin();
          
          if(plugin != null && pluginService.isActivated()) {
            try {
              PluginMenu[] actions = plugin.getContextMenuActionsForProgram(pluginProgram);
              
              if(actions != null) {
                for(final PluginMenu pluginMenu : actions) {
                  MenuItem item = menu.add(-1,Menu.NONE,Menu.NONE,pluginMenu.getTitle());
                  
                  item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                      try {
                        if(plugin.onProgramContextMenuSelected(pluginProgram, pluginMenu)) {
                          ProgramUtils.markProgram(context, pluginProgram);
                        }
                      } catch (RemoteException e) {
                        Log.d("info23", "", e);
                      }
                      
                      return true;
                    }
                  });
                }
              }
            } catch (RemoteException e) {
              Log.d("info23", "", e);
            }
          }
        }
      }
    }
    
    if(context != null && context instanceof ActivityTvBrowserSearchResults) {
      menu.findItem(R.id.program_popup_search_repetition).setVisible(false);
    }
    
    cursor.close();
  }
  
  public static void searchForRepetition(final Context activity, String title, String episode, View parent) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    
    RelativeLayout layout = (RelativeLayout)((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.search_repetition_layout, parent instanceof ViewGroup ? (ViewGroup)parent : null, false);
    
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
        Intent search = new Intent(activity,ActivityTvBrowserSearchResults.class);
        
        search.putExtra(SearchManager.QUERY, String.valueOf(titleText.getText()));
        
        String episode = String.valueOf(episodeText.getText()).trim();
        
        if(episode.length() > 0) {
          search.putExtra(ActivityTvBrowserSearchResults.QUERY_EXTRA_EPISODE_KEY, episode);
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
  public static boolean handleContextMenuSelection(final Context activity, MenuItem item, long programID, final View menuView, View parent) {
    Cursor info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), TvBrowserContentProvider.getColumnArrayWithMarkingColums(TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE), null, null,null);
    
    String title = null;
    String episode = null;
    
    ArrayList<String> markedColumns = new ArrayList<String>();
    
    if(info.getCount() > 0) {
      info.moveToFirst();
      
      for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        int index = info.getColumnIndex(column);
        
        if(index >= 0 && info.getInt(index) == 1) {
          markedColumns.add(column);
        }
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
    else if(item.getItemId() == R.id.program_popup_search_repetition) {
      searchForRepetition(activity,title,episode,parent);
    }
    else if(item.getItemId() == R.id.prog_add_reminder) {
      if(markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER) || markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) {
        return true;
      }
      else {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, true);
      }
      
      addReminder(activity.getApplicationContext(),programID,0,UiUtils.class,true);
    }
    else if(item.getItemId() == R.id.prog_remove_reminder) {
      if(!(markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER) || markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER))) {
        return true;
      }
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, false);
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, false);
      values.put(TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER, true);
      
      if(menuView != null) {
        markedColumns.remove(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
        markedColumns.remove(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);
        
        if(markedColumns.isEmpty()) {
          menuView.setBackgroundResource(android.R.drawable.list_selector_background);  
        }
        else {
          handleMarkings(activity, null, menuView, IOUtils.getStringArrayFromList(markedColumns));
        }
      }
      
      removeReminder(activity.getApplicationContext(),programID);
    }
    else if(item.getItemId() == R.id.program_popup_dont_want_to_see) {
      if(title != null) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
        builder.setTitle(R.string.action_dont_want_to_see);
        
        View view = ((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dont_want_to_see_edit, parent instanceof ViewGroup ? (ViewGroup)parent : null, false);
        
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
                    
                    Context applicationContext = activity.getApplicationContext();
                    
                    NotificationCompat.Builder builder;
                    
                    builder = new NotificationCompat.Builder(activity);
                    builder.setSmallIcon(R.drawable.ic_stat_notify);
                    builder.setOngoing(true);
                    builder.setContentTitle(activity.getResources().getText(R.string.action_dont_want_to_see));
                    builder.setContentText(activity.getResources().getText(R.string.dont_want_to_see_refresh_notification_text));
                    
                    int notifyID = 4;
                    
                    NotificationManager notification = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
                    notification.notify(notifyID, builder.build());
                    
                    Cursor c = activity.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE}, " NOT " +TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.KEY_ID);
                    c.moveToPosition(-1);
                    
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
                          e.printStackTrace();
                        }
                      }
                    }
                    
                    c.close();
                    
                    if(!updateValuesList.isEmpty()) {
                      try {
                        activity.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                        sendDontWantToSeeChangedBroadcast(applicationContext,true);
                      } catch (RemoteException e) {
                        e.printStackTrace();
                      } catch (OperationApplicationException e) {
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
        
        Set<String> exclusionValues = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);
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
            
            Context applicationContext = activity.getApplicationContext();
            
            NotificationCompat.Builder builder;
            
            builder = new NotificationCompat.Builder(activity);
            builder.setSmallIcon(R.drawable.ic_stat_notify);
            builder.setOngoing(true);
            builder.setContentTitle(activity.getResources().getText(R.string.action_dont_want_to_see));
            builder.setContentText(activity.getResources().getText(R.string.dont_want_to_see_refresh_notification_text));
            
            int notifyID = 3;
            
            NotificationManager notification = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
            notification.notify(notifyID, builder.build());
            
            Cursor c = activity.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_TITLE}, TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.KEY_ID);
            c.moveToPosition(-1);
            
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
                sendDontWantToSeeChangedBroadcast(applicationContext,false);
              } catch (RemoteException e) {
                e.printStackTrace();
              } catch (OperationApplicationException e) {
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
    
    updateImportantProgramsWidget(context);
    updateRunningProgramsWidget(context);
  }
  
  public static void sendMarkingChangedBroadcast(Context context, long programID) {
    Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
    intent.putExtra(SettingConstants.MARKINGS_ID, programID);
    
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    updateImportantProgramsWidget(context.getApplicationContext());
  }
  
  public static void addReminder(Context context, long programID, long startTime, Class<?> caller, boolean firstCreation) {try {
    Logging.log(ReminderBroadcastReceiver.tag, "addReminder called from: " + caller + " for programID: '" + programID + "' with start time: " + new Date(startTime), Logging.REMINDER_TYPE, context);
    
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    int reminderTime = PrefUtils.getStringValueAsInt(R.string.PREF_REMINDER_TIME, R.string.pref_reminder_time_default) * 60000;
    int reminderTimeSecond = PrefUtils.getStringValueAsInt(R.string.PREF_REMINDER_TIME_SECOND, R.string.pref_reminder_time_default) * 60000;
    
    boolean remindAgain = reminderTimeSecond >= 0 && reminderTime != reminderTimeSecond;
    
    Intent remind = new Intent(context,ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    if(startTime <= 0) {
      Cursor time = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_STARTTIME}, null, null, null);
      
      if(time.moveToFirst()) {
        startTime = time.getLong(0);
      }
      
      time.close();
    }
    
    if(startTime >= System.currentTimeMillis()) {
      PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
      
      if(startTime-reminderTime > System.currentTimeMillis()) {
        Logging.log(ReminderBroadcastReceiver.tag, "Create Reminder at " + new Date(startTime-reminderTime) + " with programID: '" + programID + "' " + pending.toString(), Logging.REMINDER_TYPE, context);
        alarmManager.set(AlarmManager.RTC_WAKEUP, startTime-reminderTime, pending);
      }
      else if(firstCreation) {
        Logging.log(ReminderBroadcastReceiver.tag, "Create Reminder at " + new Date(System.currentTimeMillis()) + " with programID: '" + programID + "' " + pending.toString(), Logging.REMINDER_TYPE, context);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pending);
      }
      
      if(remindAgain && startTime-reminderTimeSecond > System.currentTimeMillis()) {
        pending = PendingIntent.getBroadcast(context, (int)-programID, remind, PendingIntent.FLAG_UPDATE_CURRENT);
        
        Logging.log(ReminderBroadcastReceiver.tag, "Create Reminder at " + new Date(startTime-reminderTimeSecond) + " with programID: '-" + programID + "' " + pending.toString(), Logging.REMINDER_TYPE, context);
        alarmManager.set(AlarmManager.RTC_WAKEUP, startTime-reminderTimeSecond, pending);
      }
    }
    else {
      Logging.log(ReminderBroadcastReceiver.tag, "Reminder for programID: '" + programID + "' not created, starttime in past: " + new Date(startTime) + " of now: " + new Date(System.currentTimeMillis()), Logging.REMINDER_TYPE, context);
    }
  }catch(Throwable t) {t.printStackTrace();}
  }
  
  public static void removeReminder(Context context, long programID) {
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    
    Intent remind = new Intent(context,ReminderBroadcastReceiver.class);
    remind.putExtra(SettingConstants.REMINDER_PROGRAM_ID_EXTRA, programID);
    
    PendingIntent pending = PendingIntent.getBroadcast(context, (int)programID, remind, PendingIntent.FLAG_NO_CREATE);
    Logging.log(ReminderBroadcastReceiver.tag, " Delete reminder for programID '" + programID + "' with pending intent '" + pending + "'", Logging.REMINDER_TYPE, context);
    if(pending != null) {
      alarmManager.cancel(pending);
    }
    
    pending = PendingIntent.getBroadcast(context, (int)-programID, remind, PendingIntent.FLAG_NO_CREATE);
    Logging.log(ReminderBroadcastReceiver.tag, " Delete reminder for programID '-" + programID + "' with pending intent '" + pending + "'", Logging.REMINDER_TYPE, context);
    if(pending != null) {
      alarmManager.cancel(pending);
    }
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
      return 0;
    }

    @Override
    public void setAlpha(int alpha) {}
    @Override
    public void setColorFilter(ColorFilter cf) {}
  }
  
  public static void handleMarkings(Context activity, Cursor cursor, View view, String[] markingValues) {
    handleMarkings(activity, cursor, view, markingValues, null, false);
  }
  
  public static void handleMarkings(Context context, Cursor cursor, View view, String[] markingValues, Handler handler, boolean vertical) {
    long startTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) : 0;
    long endTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) : 0;
    
    handleMarkings(context, cursor, startTime, endTime, view, markingValues,handler,vertical);
  }
  
  public static void handleMarkings(Context context, Cursor cursor, long startTime, long endTime, View view, String[] markingValues) {
    handleMarkings(context, cursor, startTime, endTime, view, markingValues, null);
  }
  
  public static void handleMarkings(Context context, Cursor cursor, long startTime, long endTime, final View view, String[] markingValues, Handler handler) {
    handleMarkings(context, cursor, startTime, endTime, view, markingValues, handler, false);
  }
  
  public static void handleMarkings(Context context, Cursor cursor, long startTime, long endTime, final View view, String[] markedColumns, Handler handler, boolean vertical) {
    final LayerDrawable draw = getMarkingsDrawable(context, cursor, startTime, endTime, markedColumns, vertical);
    
    if(handler == null) {
      CompatUtils.setBackground(view, draw);
    }
    else{
      handler.post(new Runnable() {
        @Override
        public void run() {
          CompatUtils.setBackground(view, draw);
        }
      });
    }
  }
  
  public static LayerDrawable getMarkingsDrawable(Context context, Cursor cursor, long startTime, long endTime, String[] markedColumns,boolean vertical) {
    if(markedColumns == null && cursor != null) {
      ArrayList<String> markedColumnList = new ArrayList<String>();
      
      for(String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        int index = cursor.getColumnIndex(column);
        
        if(index >= 0 && cursor.getInt(index) == 1) {
          markedColumnList.add(column);
        }
      }
      
      markedColumns = IOUtils.getStringArrayFromList(markedColumnList);
    }
    
    Paint base = new Paint();
    base.setStyle(Paint.Style.FILL_AND_STROKE);
    
    Paint second = null;
        
    if(PrefUtils.getBooleanValue(R.string.PREF_SHOW_PROGRESS, R.bool.pref_show_progress_default) && startTime <= System.currentTimeMillis() && System.currentTimeMillis() <= endTime) {
      base.setColor(getColor(ON_AIR_PROGRESS_KEY, context));
      second = new Paint();
      second.setStyle(Paint.Style.FILL_AND_STROKE);
      second.setColor(getColor(ON_AIR_BACKGROUND_KEY, context));
    }
    else {
      base = null;
    }
    
    final ArrayList<Drawable> draw = new ArrayList<Drawable>();
    
    if(base != null) {
      RunningDrawable running = new RunningDrawable(base, second, startTime, endTime, vertical);
      draw.add(running);
    }
    
    if(markedColumns != null && markedColumns.length > 0) {
      if(markedColumns.length > 1) {
        int[] colors = new int[markedColumns.length];
        
        for(int i = 0; i < markedColumns.length; i++) {
          Integer color = SettingConstants.MARK_COLOR_KEY_MAP.get(markedColumns[i]);
          
          if(markedColumns[i].equals(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE) && PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(context.getResources().getStringArray(R.array.pref_simple_string_value_array2)[0])) {
            color = null;
          }
          
          if(color != null) {
            colors[i] = getColor(color.intValue(), context);
          }
        }
        
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors);
        gd.setCornerRadius(0f);
        
        draw.add(gd);
      }
      else {
        Integer color = SettingConstants.MARK_COLOR_KEY_MAP.get(markedColumns[0]);
        
        if(markedColumns[0].equals(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE) && PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(context.getResources().getStringArray(R.array.pref_simple_string_value_array2)[0])) {
          color = null;
        }
        
        if(color != null) {
          draw.add(new ColorDrawable(getColor(color.intValue(), context)));
        }
      }
    }
    
    draw.add(context.getResources().getDrawable(android.R.drawable.list_selector_background));
    
    return new LayerDrawable(draw.toArray(new Drawable[draw.size()]));
  }
  
  public static void editFavorite(final Favorite fav, final Context activity, String searchString) {
    Intent startEditFavorite = new Intent(activity, ActivityFavoriteEdit.class);
    
    if(fav != null) {
      startEditFavorite.putExtra(Favorite.FAVORITE_EXTRA, fav);
    }
    else if(searchString != null) {
      startEditFavorite.putExtra(Favorite.SEARCH_EXTRA, searchString);
    }
    
    activity.startActivity(startEditFavorite);
  }
  
  public static String formatDate(long date, Context context, boolean onlyDays) {
    return formatDate(date, context, onlyDays, false);
  }
  
  public static String formatDate(long date, Context context, boolean onlyDays, boolean withDayString) {
    return formatDate(date, context, onlyDays, withDayString, false);
  }
  
  @SuppressLint("SimpleDateFormat")
  public static String formatDate(long date, Context context, boolean onlyDays, boolean withDayString, boolean withDate) {
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
    
    if(value == null || withDate) {
      if(value == null) {
        value = "";
      }
      else if(value.trim().length() > 0) {
        value += ", ";
      }
      
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
        
        value += mdf.format(progDate.getTime());
      }
      else if(withDayString) {
        SimpleDateFormat mdf = new SimpleDateFormat("EEE ");
        
        value += mdf.format(progDate.getTime());
      }
    }
    
    return value;
  }
  
  @SuppressLint("SimpleDateFormat")
  public static void formatDayView(Activity activity, Cursor cursor, View view, int startDayLabelID) {try {
    long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
    
    TextView text = (TextView)view;
    
    SimpleDateFormat day = new SimpleDateFormat("EEE", Locale.getDefault());
    
    TextView startDay = (TextView)((View)view.getParent()).findViewById(/*R.id.startDayLabelPL*/startDayLabelID);
    startDay.setText(day.format(new Date(date)));
    
    CharSequence startDayValue = formatDate(date, activity, true);
    
    if(startDayValue != null && startDayValue.toString().trim().length() > 0) {
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
  
  public static float convertPixelsToSp(float px, Context context) {
    float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
    return px/scaledDensity;
  }
  
  public static boolean filter(String title, DontWantToSeeExclusion exclusion) {
    return exclusion.matches(title);
  }
  
  public static boolean filter(Context context, String title, DontWantToSeeExclusion[] values) {
    boolean found = false;
    
    if(title != null) {
      if(values == null) {
        Set<String> exclusionValues = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);
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
      case MARKED_COLOR_KEY: color = pref.getInt(context.getString(R.string.PREF_COLOR_MARKED), context.getResources().getColor(R.color.pref_color_mark_tvb_style_default));break;
      case MARKED_FAVORITE_COLOR_KEY: color = pref.getInt(context.getString(R.string.PREF_COLOR_FAVORITE), context.getResources().getColor(R.color.pref_color_mark_favorite_tvb_style_default));break;
      case MARKED_REMINDER_COLOR_KEY: color = pref.getInt(context.getString(R.string.PREF_COLOR_REMINDER), context.getResources().getColor(R.color.pref_color_mark_reminder_tvb_style_default));break;
      case MARKED_SYNC_COLOR_KEY: color = pref.getInt(context.getString(R.string.PREF_COLOR_SYNC), context.getResources().getColor(R.color.pref_color_mark_sync_tvb_style_favorite_default));break;
      case ON_AIR_BACKGROUND_KEY: color = pref.getInt(context.getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND), context.getResources().getColor(R.color.pref_color_on_air_background_tvb_style_default));break;
      case ON_AIR_PROGRESS_KEY: color = pref.getInt(context.getString(R.string.PREF_COLOR_ON_AIR_PROGRESS), context.getResources().getColor(R.color.pref_color_on_air_progress_tvb_style_default));break;
      case RUNNING_TIME_SELECTION_KEY: color = pref.getInt(context.getString(R.string.PREF_RUNNING_TIME_SELECTION), context.getResources().getColor(R.color.pref_color_running_time_selection_background_tvb_style_default));break;
      case I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR_KEY: color = pref.getInt(context.getString(R.string.PREF_I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR), context.getResources().getColor(R.color.i_dont_want_to_see_highlight));break;
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
  

  public static int[] getColorValues(int color) {
    int[] colorValues = new int[4];
    
    colorValues[0] = (color >> 24) & 0xFF;
    colorValues[1] = (color >> 16) & 0xFF;
    colorValues[2] = (color >> 8) & 0xFF;
    colorValues[3] = color & 0xFF;
    
    return colorValues;
  }
  
  public static int getColorForValues(int[] colorValues) {
    int color = 0;
    int index = 0;
    
    if(colorValues.length == 4) {
      color = color | (colorValues[index++] << 24);
    }
    
    if(colorValues.length >= 3) {
      color = color | (colorValues[index++] << 16);
      color = color | (colorValues[index++] << 8);
      color = color | (colorValues[index++]);
    }
    
    return color;
  }
  
  public static void scaleTextViews(View view, float scale) {
    if(view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup)view;
      
      for(int i = 0; i < group.getChildCount(); i++) {
        scaleTextViews(group.getChildAt(i), scale);
      }
    }
    else if(view instanceof TextView) {
      TextView text = (TextView)view;
      
      text.setTextSize(TypedValue.COMPLEX_UNIT_PX, text.getTextSize() * scale);
    }
  }
  
  public static Bitmap createBitmapFromByteArray(byte[] data) {
    Bitmap logoBitmap = null;
    
    if(data != null && data.length > 0) {
      try {
        logoBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
      }catch(NullPointerException e) {}
    }
    
    return logoBitmap;
  }
  
  public static String getDontWantToSeeFilterString(Context context) {
    String returnValue = "";
    if(PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(context.getResources().getStringArray(R.array.pref_simple_string_value_array2)[0])) {
      returnValue = " AND ( NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE + " ) ";
    }
    
    return returnValue;
  }
    
  public static void updateImportantProgramsWidget(Context context) {
    try {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

      ComponentName importantProgramsWidget = new ComponentName(context, ImportantProgramsListWidget.class);
      int[] appWidgetIds = appWidgetManager.getAppWidgetIds(importantProgramsWidget);
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.important_widget_list_view);
    }catch(Throwable t) {}
  }
  
  public static void updateRunningProgramsWidget(Context context) {
    try {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

      ComponentName importantProgramsWidget = new ComponentName(context, RunningProgramsListWidget.class);
      int[] appWidgetIds = appWidgetManager.getAppWidgetIds(importantProgramsWidget);
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.running_widget_list_view);
    }catch(Throwable t) {}
  }
  
  public static void showChannelFilterSelection(Context context, final ChannelFilter channelFilter, ViewGroup parent) {
    showChannelFilterSelection(context, channelFilter, parent, null);
  }
  
  public static void showChannelFilterSelection(Context context, final ChannelFilter channelFilter, ViewGroup parent, final Runnable cancelCallBack) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER
        };
    
    ContentResolver cr = context.getContentResolver();
    final Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);
    
    final int idColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
    final int logoColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
    final int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
    final int orderNumberColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    int[] channelRestriction = channelFilter.getFilteredChannelIds();
    
    channels.moveToPosition(-1);
    
    final LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
    
    // inflate channel selection view
    View channelSelectionView = inflater.inflate(R.layout.dialog_channel_selection_list, parent, false);
    channelSelectionView.findViewById(R.id.channel_country_label).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_country_value).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_category_label).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_category_value).setVisibility(View.GONE);
    
    final EditText filterName = (EditText)channelSelectionView.findViewById(R.id.channel_selection_input_id_name);
    
    if(channelFilter.getName() == null) {
      channelSelectionView.findViewById(R.id.channel_selection_label_id_name).setVisibility(View.GONE);
      filterName.setVisibility(View.GONE);
    }
    else {
      filterName.setText(channelFilter.getName());
    }
    
    final ListView list = (ListView)channelSelectionView.findViewById(R.id.channel_selection_list);
    
    final Bitmap defaultLogo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
    
    final ArrayAdapter<AdapterChannel> channelAdapter = new ArrayAdapter<AdapterChannel>(context, android.R.layout.simple_list_item_1) {
      public View getView(int position, View convertView, ViewGroup parent) {
        AdapterChannel value = getItem(position);
        ChannelViewHolder holder = null;
        
        if (convertView == null) {
          holder = new ChannelViewHolder();
          
          convertView = inflater.inflate(R.layout.channel_row, parent, false);
          
          holder.mTextView = (TextView)convertView.findViewById(R.id.row_of_channel_text);
          holder.mCheckBox = (CheckBox)convertView.findViewById(R.id.row_of_channel_selection);
          holder.mLogo = (ImageView)convertView.findViewById(R.id.row_of_channel_icon);
          
          convertView.setTag(holder);
          
        }
        else {
          holder = (ChannelViewHolder)convertView.getTag();
        }
        
        holder.mTextView.setText(value.mName);
        holder.mCheckBox.setChecked(value.mSelected);
        list.setItemChecked(position, value.mSelected);
        
        Bitmap logo = value.mChannelLogo;
        
        if(logo != null) {
          holder.mLogo.setImageBitmap(logo);
        }
        else {
          holder.mLogo.setImageBitmap(defaultLogo);
        }
        
        return convertView;
      }
    };
    
    while(channels.moveToNext()) {
      int channelID = channels.getInt(idColumn);
      String name = channels.getString(nameColumn);
      int orderNumber = channels.getInt(orderNumberColumn);
      byte[] logo = channels.getBlob(logoColumn);
      
      if(orderNumber < 1) {
        name = "-. " + name;
      }
      else {
        name = orderNumber + ". " + name;
      }
      
      Bitmap channelLogo = UiUtils.createBitmapFromByteArray(logo);
      
      if(channelLogo != null) {
        BitmapDrawable l = new BitmapDrawable(context.getResources(), channelLogo);
        
        ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
        background.setBounds(0, 0, channelLogo.getWidth()+2,channelLogo.getHeight()+2);
        
        LayerDrawable logoDrawable = new LayerDrawable(new Drawable[] {background,l});
        logoDrawable.setBounds(background.getBounds());
        
        l.setBounds(2, 2, channelLogo.getWidth(), channelLogo.getHeight());
        
        channelLogo = UiUtils.drawableToBitmap(logoDrawable);
      }
      
      channelAdapter.add(new AdapterChannel(channelID, name, channelLogo, isRestricted(channelRestriction, channelID)));
    }
    
    channels.close();
    
    list.setAdapter(channelAdapter);
    
    channelSelectionView.findViewById(R.id.channel_selection_select_all).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        for(int i = 0; i < channelAdapter.getCount(); i++) {
          channelAdapter.getItem(i).mSelected = true;
        }
        
        list.invalidateViews();
      }
    });
    
    channelSelectionView.findViewById(R.id.channel_selection_remove_selection).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        for(int i = 0; i < channelAdapter.getCount(); i++) {
          channelAdapter.getItem(i).mSelected = false;
        }
        
        list.invalidateViews();
      }
    });
    
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        CheckBox check = (CheckBox)view.findViewById(R.id.row_of_channel_selection);
        
        if(check != null) {
          check.setChecked(!check.isChecked());
          channelAdapter.getItem(position).mSelected = check.isChecked();
          list.setItemChecked(position, check.isChecked());
        }
      }
    });
    
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    
    builder.setView(channelSelectionView);
    builder.setCancelable(false);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        ArrayList<Integer> channelIDList = new ArrayList<Integer>();
        boolean allSelected = true;
        
        for(int i = 0; i < channelAdapter.getCount(); i++) {
          AdapterChannel item = channelAdapter.getItem(i);
          
          if(item.mSelected) {
            channelIDList.add(Integer.valueOf(item.mChannelID));
          }
          else {
            allSelected = false;
          }
        }
        
        if(allSelected || channelIDList.isEmpty()) {
          channelFilter.setFilterValues(null, null);
        }
        else {
          int[] ids = new int[channelIDList.size()];
          
          for(int i = 0; i < ids.length; i++) {
            ids[i] = channelIDList.get(i);
          }
          
          String name = null;
          
          if(filterName.getVisibility() == View.VISIBLE) {
            name = filterName.getText().toString().trim();
          }
          
          channelFilter.setFilterValues(name,ids);
        }
      }
    });
    
    if(cancelCallBack != null) {
      builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          cancelCallBack.run();
        }
      });
    }
    else {
      builder.setNegativeButton(android.R.string.cancel, null);
    }
    
    AlertDialog dialog = builder.create();
    dialog.show();
    
    if(filterName.getVisibility() == View.VISIBLE) {
      final Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
      ok.setEnabled(filterName.getText().toString().trim().length() > 0);
      
      filterName.addTextChangedListener(new TextWatcher() {
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void afterTextChanged(Editable s) {
          ok.setEnabled(filterName.getText().toString().trim().length() > 0);
        }
      });
    }
  }
  
  /**
   * View holder for custom cursor adapter of channel selection.
   * 
   * @author René Mach
   */
  private static final class ChannelViewHolder {
    CheckBox mCheckBox;
    TextView mTextView;
    ImageView mLogo;
  }
  
  private static boolean isRestricted(int[] channelIDs, int id) {
    boolean returnValue = channelIDs == null;
    
    if(!returnValue) {
      for(int channelID : channelIDs) {
        if(id == channelID) {
          returnValue = true;
          break;
        }
      }
    }
    
    return returnValue;
  }
  
  private static final class AdapterChannel {
    int mChannelID;
    String mName;
    Bitmap mChannelLogo;
    boolean mSelected; 
    
    public AdapterChannel(int channelID, String name, Bitmap channelLogo, boolean selected) {
      mChannelID = channelID;
      mName = name;
      mChannelLogo = channelLogo;
      mSelected = selected;
    }
  }
  
  public static final int getThemeResourceId() {
    if(SettingConstants.IS_DARK_THEME) {
      return R.style.AppDarkTheme;
    }
    
    return R.style.AppTheme;
  }
}
