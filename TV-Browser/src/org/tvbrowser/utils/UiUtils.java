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
package org.tvbrowser.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.tvbrowser.App;
import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.filter.CategoryFilter;
import org.tvbrowser.filter.ChannelFilter;
import org.tvbrowser.filter.KeywordFilter;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.ActivityFavoriteEdit;
import org.tvbrowser.tvbrowser.ActivityTvBrowserSearchResults;
import org.tvbrowser.tvbrowser.DontWantToSeeExclusion;
import org.tvbrowser.tvbrowser.Favorite;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.NamedFields;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.ServiceUpdateRemindersAndAutoUpdate;
import org.tvbrowser.tvbrowser.TvBrowser;
import org.tvbrowser.view.SwipeScrollView;
import org.tvbrowser.widgets.ImportantProgramsListWidget;
import org.tvbrowser.widgets.RunningProgramsListWidget;
import org.tvbrowser.widgets.WidgetToggleReminderState;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UiUtils {

  UiUtils() {
  }

  /**
   * A thread specific {@link Collator} instance for locale sensitive sorting.
   *
   * @see #getCollator()
   */
  private static final ThreadLocal<Collator> collatorThreadLocal = new ThreadLocal<Collator>() {
    @Override
    protected Collator initialValue() {
      final Collator collator = Collator.getInstance();
      collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
      collator.setStrength(Collator.SECONDARY);
      return collator;
    }
  };

  private static final HashMap<String, Integer> VALUE_MAP;

  public static final int EXPIRED_COLOR_KEY = 0;
  public static final int MARKED_COLOR_KEY = 1;
  public static final int MARKED_FAVORITE_COLOR_KEY = 2;
  public static final int MARKED_REMINDER_COLOR_KEY = 3;
  public static final int MARKED_SYNC_COLOR_KEY = 4;
  private static final int ON_AIR_BACKGROUND_KEY = 5;
  private static final int ON_AIR_PROGRESS_KEY = 6;
  public static final int RUNNING_TIME_SELECTION_KEY = 7;
  public static final int I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR_KEY = 8;

  static {
    VALUE_MAP = new HashMap<>();
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
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST, R.id.detail_year_production_first);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, R.id.detail_last_production_year);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, R.id.detail_additional_info);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SERIES, R.id.detail_series);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_RATING, R.id.detail_rating);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_GENRE_SUB_LIST, R.id.detail_subgenre_list);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_VOD_INFO, R.id.detail_vod_info);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_VOD_LINK, R.id.detail_vod_link);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_KEYWORD_LIST, R.id.detail_keyword_list);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PART_NUMBER, R.id.detail_part_numer);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PART_NUMBER_TOTAL, R.id.detail_parts_total);
    VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_DURATION_ORIGINAL, R.id.detail_duration_original);
  }

  public static void showProgramInfo(final Context context, long id, View parent, Handler handler) {
    showProgramInfo(context, id, null, parent, handler);
  }

  private static boolean mShowingProgramInfo = false;

  public static synchronized void showProgramInfo(final Context context, final long id, final Activity finish, final View parent, final Handler handler) {
    if (!mShowingProgramInfo && IOUtils.isDatabaseAccessible(context)) {
      mShowingProgramInfo = true;

      handler.post(() -> {
        AsyncTask<Void, Void, Boolean> createInfoTask = new AsyncInfo(context,id,finish,parent,handler);
        /*
        AsyncTask<Void, Void, Boolean> createInfoTask = new AsyncTask<Void, Void, Boolean>() {
          private View mLayout;
          private ProgressDialog mProgress;
          private Handler mHandler;
          private Runnable mShowWaiting;
          private boolean mShowWaitingDialog;
          private boolean mHasSpannableActors;

          private long mPreviousId;
          private long mNextId;

          @Override
          protected void onPreExecute() {
            mPreviousId = -1;
            mNextId = -1;

            mShowWaitingDialog = true;
            mHandler = new Handler();
            mShowWaiting = () -> {
              mProgress = new ProgressDialog(context);
              mProgress.setMessage(context.getString(R.string.waiting_detail_show));

              if (mShowWaitingDialog) {
                try {
                  mProgress.show();
                } catch (BadTokenException ignored) {
                }
              }
            };

            int layout = PrefUtils.getBooleanValue(R.string.PREF_DETAIL_ALLOW_SWIPE, R.bool.pref_detail_allow_swipe) ? R.layout.dialog_detail_swipe : R.layout.dialog_detail;

            mLayout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, parent instanceof ViewGroup ? (ViewGroup) parent : null, false);
            mHandler.postDelayed(mShowWaiting, 700);
          }

          @Override
          protected Boolean doInBackground(Void... params) {
            Boolean result = Boolean.FALSE;

            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

            final Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, TvBrowserContentProvider.KEY_ID + " ASC LIMIT 1");

            try {
              if (IOUtils.prepareAccessFirst(c)) {
                mHasSpannableActors = false;
                result = Boolean.TRUE;
                float textScale = Float.parseFloat(PrefUtils.getStringValue(R.string.DETAIL_TEXT_SCALE, R.string.detail_text_scale_default));

                final long startTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
                final long endTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
                final ImageButton handleReminder = mLayout.findViewById(R.id.detail_handle_reminder);

                if (startTime <= System.currentTimeMillis()) {
                  handleReminder.setVisibility(View.GONE);
                } else {
                  final AtomicBoolean userRemind = new AtomicBoolean(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER)) == 1);
                  final AtomicBoolean favoriteRemind = new AtomicBoolean(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) == 1);

                  if (userRemind.get() || favoriteRemind.get()) {
                    handleReminder.setImageResource(R.drawable.ic_action_remove_alarm);
                    CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_remove));
                  } else {
                    handleReminder.setImageResource(R.drawable.ic_action_add_alarm);
                    CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_add));
                  }

                  handleReminder.setOnClickListener(v -> {
                    ContentValues values = new ContentValues();
                    boolean add = true;

                    if (userRemind.get() || favoriteRemind.get()) {
                      add = false;
                      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, false);
                      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, false);

                    } else {
                      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, true);
                    }

                    if (context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), values, null, null) == 1) {
                      if (add) {
                        userRemind.set(true);

                        ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context);
                        ProgramUtils.addReminderId(context, id);

                        mHandler.post(() -> {
                          handleReminder.setImageResource(R.drawable.ic_action_remove_alarm);
                          CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_remove));
                          handleReminder.invalidate();
                        });
                      } else {
                        userRemind.set(false);
                        favoriteRemind.set(false);

                        IOUtils.removeReminder(context, id);
                        ProgramUtils.removeReminderId(context, id);
                        ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context);

                        mHandler.post(() -> {
                          handleReminder.setImageResource(R.drawable.ic_action_add_alarm);
                          CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_add));
                          handleReminder.invalidate();
                        });
                      }

                      Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
                      intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, id);

                      LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);

                      UiUtils.updateImportantProgramsWidget(context);
                      UiUtils.updateRunningProgramsWidget(context);
                    }
                  });
                }

                final int favoriteMatchHighlightColor = PrefUtils.getIntValue(R.string.PREF_DETAIL_HIGHLIGHT_COLOR, ContextCompat.getColor(context, R.color.pref_detail_highlight_color_default));

                final Favorite[] markedFromFavorites = Favorite.getFavoritesForUniqueId(context, id);
                final ArrayList<FavoriteTypePattern> patternList = new ArrayList<>();
                final BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(favoriteMatchHighlightColor);

                if (markedFromFavorites.length > 0) {
                  for (Favorite favorite : markedFromFavorites) {
                    if (favorite.getType() != Favorite.RESTRICTION_RULES_TYPE) {
                      patternList.add(new FavoriteTypePattern(favorite.getType(), Pattern.compile(".*?(" + favorite.getSearchValue().replace("%", ".*?") + ").*?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
                    }
                  }
                }

                TextView date = mLayout.findViewById(R.id.detail_date_channel);
                TextView title = mLayout.findViewById(R.id.detail_title);
                TextView genre = mLayout.findViewById(R.id.detail_genre);
                TextView info = mLayout.findViewById(R.id.detail_info);
                TextView episode = mLayout.findViewById(R.id.detail_episode_title);
                TextView shortDescription = mLayout.findViewById(R.id.detail_short_description);
                TextView description = mLayout.findViewById(R.id.detail_description);
                TextView link = mLayout.findViewById(R.id.detail_link);
                TextView pictureDescription = mLayout.findViewById(R.id.detail_picture_description);
                TextView pictureCopyright = mLayout.findViewById(R.id.detail_picture_copyright);

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

                final Resources resources = context.getResources();

                if (!PrefUtils.isDarkTheme() && !(finish instanceof InfoActivity)) {
                  date.setTextColor(ContextCompat.getColor(context, R.color.detail_date_channel_color_light));
                }

                final int channelID = c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));

                Cursor channel = null;

                try {
                  channel = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[]{TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, null, null, null);

                  if (IOUtils.prepareAccessFirst(channel)) {
                    final StringBuilder channelName = new StringBuilder();
                    if (PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_DETAILS, R.bool.show_sort_number_in_details_default)) {
                      final int columnIndex = channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
                      if (!channel.isNull(columnIndex)) {
                        channelName.append(channel.getString(columnIndex)).append(". ");
                      }
                    }
                    channelName.append(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));

                    final Date start = new Date(startTime);
                    final java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault());
                    final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
                    date.setText(resources.getString(R.string.detail_date_format, start, dateFormat.format(start), timeFormat.format(start), timeFormat.format(new Date(endTime)), channelName));

                    Bitmap logo = UiUtils.createBitmapFromByteArray(channel.getBlob(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));

                    if (logo != null) {
                      float scale = resources.getDisplayMetrics().density;

                      int width = (int) (logo.getWidth() * scale);
                      int height = (int) (logo.getHeight() * scale);

                      BitmapDrawable l = new BitmapDrawable(resources, logo);

                      int color = PrefUtils.getIntValue(R.string.PREF_LOGO_BACKGROUND_COLOR, ContextCompat.getColor(context, R.color.pref_logo_background_color_default));

                      GradientDrawable background = new GradientDrawable(Orientation.BOTTOM_TOP, new int[]{color, color});

                      int add = 2;

                      if (PrefUtils.getBooleanValue(R.string.PREF_LOGO_BORDER, R.bool.pref_logo_border_default)) {
                        add = 3;
                        background.setStroke(1, PrefUtils.getIntValue(R.string.PREF_LOGO_BORDER_COLOR, ContextCompat.getColor(context, R.color.pref_logo_border_color_default)));
                      }

                      background.setBounds(0, 0, width + add, height + add);

                      LayerDrawable logoDrawable = new LayerDrawable(new Drawable[]{background, l});
                      logoDrawable.setBounds(0, 0, width + add, height + add);

                      l.setBounds(2, 2, width, height);

                      date.setCompoundDrawables(logoDrawable, null, null, null);
                    }
                  }
                } finally {
                  IOUtils.close(channel);
                }

                final StringBuilder year = new StringBuilder();

                int yearInt = -1;
                int yearLast = -1;
                int yearFirst = -1;
                int originLength = 0;

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN))) {
                  year.append(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN)));
                  originLength = year.length();
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR))) {
                  yearInt = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR));
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST))) {
                  yearFirst = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST));
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR))) {
                  yearLast = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR));
                }

                if (yearLast < yearInt) {
                  yearLast = -1;
                }

                if (yearInt != -1 && ((yearLast != -1 && yearFirst == -1) || (yearLast == -1 && yearFirst < yearInt && yearFirst != -1))) {
                  yearFirst = yearInt;
                }

                if (yearInt != -1) {
                  if (year.length() >= 0) {
                    year.append(" ");
                  }

                  year.append(yearInt);
                }

                if (yearLast != -1 && yearLast != yearInt) {
                  if (yearFirst != -1) {
                    if (year.length() >= 0) {
                      if (yearFirst == yearInt) {
                        year.append(" \u2013 ");
                      } else {
                        year.append(" (");
                      }
                    }

                    if (yearFirst == yearInt) {
                      year.append(yearLast);
                    } else {
                      year.append(yearFirst).append(" \u2013 ").append(yearLast).append(")");
                    }
                  } else {
                    year.append(yearLast);
                  }
                } else if (yearFirst != -1 && yearFirst != yearInt) {
                  if (yearFirst < yearInt) {
                    year.insert(originLength, " \u2013 ");
                    year.insert(originLength, yearFirst);

                    if (originLength != 0) {
                      year.insert(originLength, " ");
                    }
                  } else if (yearInt == -1) {
                    if (originLength > 0) {
                      year.append(" ");
                    }

                    year.append(yearFirst);
                  }
                }

                String originalTitle = null;
                String titleTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL))) {
                  originalTitle = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL));
                }

                if (originalTitle == null || originalTitle.equals(titleTest)) {
                  checkAndAddHiglightingForFavorites(title, titleTest, patternList, true, backgroundColorSpan);
                  //title.setText(titleTest);
                } else {
                  checkAndAddHiglightingForFavorites(title, titleTest + "/" + originalTitle, patternList, true, backgroundColorSpan);
                  //title.setText(titleTest + "/" + originalTitle);
                }

                if (!PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_DETAILS, R.bool.show_picture_in_details_default) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT))) {
                  pictureCopyright.setVisibility(View.GONE);
                  pictureDescription.setVisibility(View.GONE);
                } else {
                  if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION))) {
                    pictureDescription.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION)));
                  }

                  pictureCopyright.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));

                  Bitmap image = UiUtils.createBitmapFromByteArray(c.getBlob(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)));

                  if (image != null) {
                    BitmapDrawable b = new BitmapDrawable(resources, image);

                    float zoom = Float.parseFloat(PrefUtils.getStringValue(R.string.DETAIL_PICTURE_ZOOM, R.string.detail_picture_zoom_default)) * resources.getDisplayMetrics().density;

                    b.setBounds(0, 0, (int) (image.getWidth() * zoom), (int) (image.getHeight() * zoom));

                    if (PrefUtils.getStringValue(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION, R.string.detail_picture_description_position_default).equals("0")) {
                      pictureDescription.setCompoundDrawables(b, null, null, null);
                    } else {
                      pictureDescription.setCompoundDrawables(null, b, null, null);
                    }
                  }
                }

                if (PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_DETAILS, R.bool.show_genre_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
                  checkAndAddHiglightingForFavorites(genre, c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) + (year.length() > 0 ? " - " + year : ""), patternList, false, backgroundColorSpan);
//                    genre.setText();
                } else if (year.length() > 0) {
                  checkAndAddHiglightingForFavorites(genre, year.toString(), patternList, false, backgroundColorSpan);
                  //genre.setText(year);
                } else {
                  genre.setVisibility(View.GONE);
                }

                Spannable infoValue = IOUtils.getInfoString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)), resources);

                if (PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_DETAILS, R.bool.show_info_in_details_default) && infoValue != null) {
                  info.setText(infoValue);
                } else {
                  info.setVisibility(View.GONE);
                }

                String number = "";

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) {
                  if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT))) {
                    number = IOUtils.decodeSingleFieldValueToMultipleEpisodeString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) + "/" + c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT)) + " ";
                  } else {
                    number = IOUtils.decodeSingleFieldValueToMultipleEpisodeString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) + " ";
                  }
                }

                String originalEpisode = null;

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL))) {
                  originalEpisode = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL));
                }

                if (PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
                  String episodeTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));

                  if (originalEpisode == null || episodeTest.equals(originalEpisode)) {
                    checkAndAddHiglightingForFavorites(episode, number + episodeTest, patternList, false, backgroundColorSpan);
                    //episode.setText(number + episodeTest);
                  } else {
                    checkAndAddHiglightingForFavorites(episode, number + episodeTest + "/" + originalEpisode, patternList, false, backgroundColorSpan);
                    //episode.setText(number + episodeTest + "/" + originalEpisode);
                  }
                } else if (PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && number.trim().length() > 0) {
                  checkAndAddHiglightingForFavorites(episode, number, patternList, false, backgroundColorSpan);
                  //episode.setText(number);
                } else {
                  episode.setVisibility(View.GONE);
                }

                String shortDescriptionValue = null;
                String descriptionValue = null;
                boolean showShortDescription = true;

                boolean showEpgPaidInfo = PrefUtils.getBooleanValue(R.string.PREF_EPGPAID_DESCRIPTION_MISSING_INFO, R.bool.pref_epgpaid_description_missing_default);
                long epgPaidUntil = PrefUtils.getLongValueWithDefaultKey(R.string.PREF_EPGPAID_ACCESS_UNTIL,R.integer.pref_epgpaid_access_until_default);

                final boolean wasActive = epgPaidUntil < System.currentTimeMillis() && epgPaidUntil != context.getResources().getInteger(R.integer.pref_epgpaid_access_until_default);

                if (showEpgPaidInfo) {
                  final Set<String> channelIds = PrefUtils.getStringSetValue(R.string.PREF_EPGPAID_DATABASE_CHANNEL_IDS, new HashSet<>());

                  showEpgPaidInfo = channelIds.contains(String.valueOf(channelID));

                  if (showEpgPaidInfo && !wasActive) {
                    final File epgPaidDir = new File(IOUtils.getDownloadDirectory(context), "epgPaidData");

                    if (epgPaidDir.isDirectory()) {
                      final File[] test = epgPaidDir.listFiles();

                      showEpgPaidInfo = (test.length <= 2);
                    }
                  }
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
                  shortDescriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)).trim();
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION))) {
                  descriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));

                  if (shortDescriptionValue != null) {
                    String test = shortDescriptionValue;

                    if (test.endsWith("...")) {
                      test = test.substring(0, test.length() - 3);
                    } else if (test.endsWith("\u2026")) {
                      test = test.substring(0, test.length() - 1);
                    }

                    test = test.replaceAll("\\s+", " ").trim();

                    showShortDescription = !descriptionValue.replaceAll("\\s+", " ").trim().startsWith(test);

                    if (showShortDescription) {
                      shortDescriptionValue = shortDescriptionValue.trim() + "\n";
                    }

                    String[] paragraphs = descriptionValue.split("\n+");

                    if (paragraphs.length > 1) {
                      String para0 = paragraphs[0].replaceAll("\\s+", " ").trim();
                      String para1 = paragraphs[1].replaceAll("\\s+", " ").trim();

                      if (para1.startsWith(para0) || para0.startsWith(para1)) {
                        descriptionValue = descriptionValue.substring(paragraphs[0].length() + 1).trim();
                      } else {
                        int testIndex = para0.indexOf(".");

                        if (testIndex > 0) {
                          para0 = para0.substring(testIndex + 1).trim();

                          if (!para0.isEmpty() && para1.endsWith(para0)) {
                            descriptionValue = descriptionValue.substring(paragraphs[0].length() + 1).trim();
                          }
                        }
                      }
                    }
                  }
                }

                if (showEpgPaidInfo) {
                  if(shortDescriptionValue == null && descriptionValue == null) {
                    if(wasActive) {
                      descriptionValue = context.getString(R.string.EPGpaidData_info_expiredAccess).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
                    }
                    else {
                      descriptionValue = context.getString(R.string.detail_missing_description_text);
                    }
                  }
                  else if(wasActive) {
                    if((shortDescriptionValue != null && shortDescriptionValue.contains("omdb.org")) ||
                      (descriptionValue != null && descriptionValue.contains("omdb.org"))) {
                      descriptionValue += context.getString(R.string.EPGpaidData_info_expiredAccessExtend).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));;
                    }
                    else if((shortDescriptionValue != null && shortDescriptionValue.contains("wiki.tvbrowser.org/index.php/WirSchauen")) ||
                      (descriptionValue != null && descriptionValue.contains("wiki.tvbrowser.org/index.php/WirSchauen"))) {
                      descriptionValue = context.getString(R.string.EPGpaidData_info_expiredAccess).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));;
                    }
                  }
                }

                if (shortDescriptionValue == null || !showShortDescription) {
                  shortDescription.setVisibility(View.GONE);
                } else {
                  checkAndAddHiglightingForFavorites(shortDescription, shortDescriptionValue.replaceAll("\\s{4,}", "\n\n").replaceAll("\\r", "").replaceAll("\\n{2,}", "\n\n"), patternList, false, backgroundColorSpan);
                }

                if (descriptionValue != null) {
                  checkAndAddHiglightingForFavorites(description, descriptionValue.replaceAll("\\s{4,}", "\n\n").replaceAll("\\r", "").replaceAll("\\n{2,}", "\n\n"), patternList, false, backgroundColorSpan);
                } else {
                  description.setVisibility(View.GONE);
                }

                if (PrefUtils.getBooleanValue(R.string.SHOW_LINK_IN_DETAILS, R.bool.show_link_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK))) {
                  String linkText = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK));
                  link.setText(linkText);
                  link.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                  link.setVisibility(View.GONE);
                }

                Set<String> keys = VALUE_MAP.keySet();

                for (String key : keys) {
                  boolean enabled = pref.getBoolean("details_" + key, true);
                  TextView textView = mLayout.findViewById(VALUE_MAP.get(key));
                  textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() * textScale);

                  if (textView != null && enabled && !c.isNull(c.getColumnIndex(key))) {
                    String text = c.getString(c.getColumnIndex(key));

                    if (key.equals(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO) && descriptionValue != null && text.equals(descriptionValue)) {
                      text = "";
                    }

                    if (text.trim().length() > 0) {
                      try {
                        String name = resources.getString((Integer) R.string.class.getField(key).get(null));

                        boolean endWith = false;

                        if (name.endsWith(":")) {
                          endWith = true;
                        }

                        if (key.equals(TvBrowserContentProvider.DATA_KEY_ACTORS)) {
                          String separator = null;

                          if (text.contains("\n")) {
                            separator = "\n+";
                          } else if (text.contains(",")) {
                            separator = ",\\s*";
                          }

                          SpannableStringBuilder actors = new SpannableStringBuilder(name);
                          actors.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                          actors.setSpan(new UnderlineSpan(), 0, name.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                          int screenSizeHalf = getScreenSize(context).x / 2;

                          if (separator != null) {
                            String[] lines = text.split(separator);

                            float maxLength = 0;
                            TextPaint actorPaint = textView.getPaint();

                            for (int i = 0; i < lines.length; i++) {
                              if (lines[i].contains("\t\t-\t\t")) {
                                separator = "\t\t-\t\t";
                              } else if (lines[i].contains("(")) {
                                separator = "\\s*\\(";
                              } else {
                                separator = null;
                              }

                              if (separator != null) {
                                String[] parts = lines[i].split(separator);

                                if (parts.length > 1) {
                                  float test = actorPaint.measureText(parts[0]);

                                  if (test > screenSizeHalf) {
                                    String secondSeparator = null;

                                    if (parts[0].contains("(")) {
                                      secondSeparator = "(";
                                    } else if (parts[0].contains(" ")) {
                                      secondSeparator = " ";
                                    } else if (parts[0].contains("-")) {
                                      secondSeparator = "-";
                                    }

                                    if (secondSeparator != null) {
                                      int index = parts[0].indexOf(secondSeparator);

                                      String firstPart = parts[0].substring(0, index).trim();
                                      String secondPart = parts[0].substring(index).trim();

                                      float firstLength = actorPaint.measureText(firstPart);
                                      float secondLength = actorPaint.measureText(secondPart);

                                      if (secondLength > screenSizeHalf) {
                                        int whiteSpaceIndex = secondPart.indexOf(" ");

                                        if (whiteSpaceIndex >= 0) {
                                          String thirdPart = secondPart.substring(0, whiteSpaceIndex).trim();
                                          String fifthPart = secondPart.substring(whiteSpaceIndex).trim();

                                          secondLength = Math.max(actorPaint.measureText(thirdPart), actorPaint.measureText(fifthPart));

                                          if (secondLength < screenSizeHalf) {
                                            secondPart = thirdPart + "\n" + fifthPart;
                                          }
                                        }
                                      }

                                      test = Math.max(firstLength, secondLength);

                                      if (test < screenSizeHalf) {
                                        lines[i] = lines[i].replace(parts[0], firstPart + "\n" + secondPart);
                                      }
                                    }
                                  }

                                  maxLength = Math.max(maxLength, test);
                                }
                              }
                            }

                            if (maxLength > 0 && maxLength <= screenSizeHalf) {
                              for (int i = 0; i < lines.length; i++) {
                                String[] parts = lines[i].split(separator);

                                if (i > 0) {
                                  actors.append("\n");
                                }

                                if (parts.length > 1) {
                                  if (parts[1].trim().endsWith(")") || parts[1].trim().endsWith(",")) {
                                    parts[1] = parts[1].substring(0, parts[1].length() - 1);
                                  }

                                  if (parts[0].contains("\n")) {
                                    if (parts[0].indexOf("\n") != parts[0].lastIndexOf("\n")) {
                                      parts[1] += "\n\n";
                                    } else {
                                      parts[1] += "\n";
                                    }
                                  }

                                  actors.append(parts[1]);

                                  actors.setSpan(new ActorColumnSpan(parts[0], (int) maxLength + 10, patternList, favoriteMatchHighlightColor), actors.length() - parts[1].length(), actors.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                  mHasSpannableActors = true;
                                } else {
                                  actors.append(lines[i]);
                                }
                              }
                            } else {
                              actors.append(text);
                            }
                          } else {
                            actors.append(text);
                          }

                          checkAndAddHiglightingForFavorites(textView, actors, patternList, false, backgroundColorSpan);
                          //textView.setText(actors);
                        } else {
                          if (key.equals(TvBrowserContentProvider.DATA_KEY_VPS)) {
                            Calendar temp = Calendar.getInstance();
                            temp.set(Calendar.HOUR_OF_DAY, Integer.parseInt(text) / 60);
                            temp.set(Calendar.MINUTE, Integer.parseInt(text) % 60);

                            text = DateFormat.getTimeFormat(context).format(temp.getTime());
                          }

                          text = text.replace("\n", "<br>");

                          name = "<b><u>" + name.replace("\n", "<br>") + "</u></b>" + (endWith ? " " : "");

                          checkAndAddHiglightingForFavorites(textView, CompatUtils.fromHtml(name + text), patternList, false, backgroundColorSpan);
                        }
                      } catch (Exception e) {
                        textView.setVisibility(View.GONE);
                      }
                    } else {
                      textView.setVisibility(View.GONE);
                    }
                  } else if (textView != null) {
                    textView.setVisibility(View.GONE);
                  }
                }

                final String[] projection = {TvBrowserContentProvider.KEY_ID};

                Cursor prev = null;
                try {
                  prev = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + startTime + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + channelID + " AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " DESC LIMIT 1");
                  if (IOUtils.prepareAccessFirst(prev)) {
                    mPreviousId = prev.getLong(prev.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                  }
                } finally {
                  IOUtils.close(prev);
                }

                Cursor next = null;
                try {
                  next = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + startTime + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + channelID + " AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC LIMIT 1");
                  if (IOUtils.prepareAccessFirst(next)) {
                    mNextId = next.getLong(next.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                  }
                } finally {
                  IOUtils.close(next);
                }
              }
            } finally {
              IOUtils.close(c);
            }

            return result;
          }

          @Override
          protected void onPostExecute(Boolean result) {
            mHandler.removeCallbacks(mShowWaiting);
            mShowWaitingDialog = false;

            if (mProgress != null && mProgress.isShowing()) {
              try {
                mProgress.dismiss();
              } catch (IllegalArgumentException ignored) {
              }
            }

            if (result) {
              AlertDialog.Builder builder = new AlertDialog.Builder(context);

              if (finish != null) {
                builder.setOnCancelListener(dialog -> finish.finish());
              }

              builder.setView(mLayout);


              final AlertDialog test = builder.create();

             // Stupid hack for wrong measured dialog size
              if (mHasSpannableActors && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                final OnPreDrawListener preDrawListener = new OnPreDrawListener() {
                  @Override
                  public boolean onPreDraw() {
                    mLayout.getViewTreeObserver().removeOnPreDrawListener(this);

                    int scrollHeight = mLayout.findViewById(R.id.test_test_test).getMeasuredHeight();

                    if (scrollHeight < mLayout.getMeasuredHeight()) {
                      test.getWindow().setLayout(mLayout.getMeasuredWidth() + 40, scrollHeight + 100);
                    }

                    return true;
                  }
                };
                mLayout.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
              }

              if (mLayout instanceof SwipeScrollView) {
                mLayout.setOnTouchListener((v, event) -> {
                  boolean result1 = false;

                  if (((SwipeScrollView) mLayout).isSwipeLeft()) {
                    ((SwipeScrollView) mLayout).resetSwipe();

                    if (mNextId != -1) {
                      test.dismiss();
                      UiUtils.showProgramInfo(context, mNextId, finish, parent, handler);
                      result1 = true;
                    }
                  } else if (((SwipeScrollView) mLayout).isSwipeRight()) {
                    ((SwipeScrollView) mLayout).resetSwipe();

                    if (mPreviousId != -1) {
                      test.dismiss();
                      UiUtils.showProgramInfo(context, mPreviousId, finish, parent, handler);
                      result1 = true;
                    }
                  }

                  return result1;
                });
              }

              try {
                test.show();
                test.setOnKeyListener((dialog, keyCode, event) -> {
                  boolean result1 = false;

                  if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    test.dismiss();
                    result1 = true;
                  }

                  return result1;
                });
              } catch (BadTokenException ignored) {
              }
            }

            new Thread("SHOW PROGRAM INFO ALLOWING THREAD") {
              @Override
              public void run() {
                try {
                  sleep(500);
                } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                mShowingProgramInfo = false;
              }
            }.start();
          }
        };
*/
        createInfoTask.execute();
      });
    }
  }

  public static void createContextMenu(final Context context, ContextMenu menu, final long id) {
    new MenuInflater(context).inflate(R.menu.program_context, menu);

    String[] projection = TvBrowserContentProvider.getColumnArrayWithMarkingColumns(ProgramUtils.DATA_CHANNEL_PROJECTION);

    if (IOUtils.isDatabaseAccessible(context)) {
      Cursor cursor = null;

      try {
        cursor = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, id), projection, null, null, null);

        if (IOUtils.prepareAccessFirst(cursor)) {
          final Program pluginProgram = ProgramUtils.createProgramFromDataCursor(context, cursor);/*new Program(id, startTime, endTime, title, shortDescription, description, episodeTitle, channel);*/

          boolean showReminder = true;
          boolean isFavoriteReminder = false;
          boolean createFavorite = true;

          for (String column : TvBrowserContentProvider.MARKING_COLUMNS) {
            int index = cursor.getColumnIndex(column);

            if (index >= 0) {
              if (column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER)) {
                showReminder = column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER) && cursor.getInt(index) == 0;
              } else if (column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) {
                isFavoriteReminder = column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER) && cursor.getInt(index) == 1;
              }

              if (column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE)) {
                createFavorite = column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE) && cursor.getInt(index) == 0;
              }
            }
          }

          showReminder = showReminder && !isFavoriteReminder;

          boolean showDontWantToSee = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE)) == 0;
          boolean isFutureReminder = pluginProgram.getStartTimeInUTC() > System.currentTimeMillis() - 5 * 60000;
          boolean showSyncRemove = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC)) == 1;

          menu.findItem(R.id.prog_add_reminder).setVisible(showReminder && isFutureReminder);
          menu.findItem(R.id.prog_remove_reminder).setVisible(!showReminder);
          menu.findItem(R.id.create_favorite_item).setVisible(createFavorite);
          menu.findItem(R.id.edit_favorite_item).setVisible(!createFavorite);

          menu.findItem(R.id.program_popup_dont_want_to_see).setVisible(showDontWantToSee && !SettingConstants.UPDATING_FILTER);
          menu.findItem(R.id.program_popup_want_to_see).setVisible(!showDontWantToSee && !SettingConstants.UPDATING_FILTER);
          menu.findItem(R.id.program_popup_remove_sync).setVisible(showSyncRemove);

          if (PluginHandler.hasPlugins()) {
            PluginServiceConnection[] connections = PluginHandler.getAvailablePlugins();

            for (final PluginServiceConnection pluginService : connections) {
              final Plugin plugin = pluginService.getPlugin();

              if (plugin != null && pluginService.isActivated()) {
                try {
                  PluginMenu[] actions = plugin.getContextMenuActionsForProgram(pluginProgram);

                  if (actions != null) {
                    for (final PluginMenu pluginMenu : actions) {
                      MenuItem item = menu.add(-1, Menu.NONE, Menu.NONE, pluginMenu.getTitle());

                      item.setOnMenuItemClickListener(item1 -> {
                        try {
                          if (plugin.onProgramContextMenuSelected(pluginProgram, pluginMenu)) {
                            if (pluginService.getPluginMarkIcon() != null) {
                              ProgramUtils.markProgram(context, pluginProgram, pluginService.getId());
                            } else {
                              ProgramUtils.markProgram(context, pluginProgram, null);
                            }
                          }
                        } catch (Throwable t) {
                          // catch all possible exceptions and errors of plugins
                          Log.d("info23", "", t);
                        }

                        return true;
                      });
                    }
                  }
                } catch (Throwable t) {
                  // catch all possible exceptions and errors of plugins
                  Log.d("info23", "", t);
                }
              }
            }
          }
        }

        if (context instanceof ActivityTvBrowserSearchResults) {
          menu.findItem(R.id.program_popup_search_repetition).setVisible(false);
        }
      } finally {
        IOUtils.close(cursor);
      }
    }
  }

  private static void searchForRepetition(final Context activity, String title, String episode, View parent) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    final RelativeLayout layout = (RelativeLayout) ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.search_repetition_layout, parent instanceof ViewGroup ? (ViewGroup) parent : null, false);
    final Resources resources = activity.getResources();
    final EditText titleText = layout.findViewById(R.id.search_repetition_title);
    final EditText episodeText = layout.findViewById(R.id.search_repetition_episode);

    if (title != null) {
      titleText.setText(title);
    }
    if (episode != null) {
      episodeText.setText(episode);
    }

    builder.setTitle(resources.getString(R.string.program_popup_search_repetition));
    builder.setView(layout);

    builder.setPositiveButton(resources.getString(android.R.string.search_go), (dialog, which) -> {
      Intent search = new Intent(activity, ActivityTvBrowserSearchResults.class);

      search.putExtra(SearchManager.QUERY, String.valueOf(titleText.getText()));

      String episode1 = String.valueOf(episodeText.getText()).trim();

      if (episode1.length() > 0) {
        search.putExtra(ActivityTvBrowserSearchResults.QUERY_EXTRA_EPISODE_KEY, episode1);
      }

      activity.startActivity(search);
    });

    builder.setNegativeButton(resources.getString(android.R.string.cancel), (dialog, which) -> {

    });

    builder.show();
  }

  @SuppressLint("NewApi")
  public static boolean handleContextMenuSelection(final Context activity, MenuItem item, long programID, final View menuView, View parent) {
    if (IOUtils.isDatabaseAccessible(activity)) {
      String title = null;
      String episode = null;
      ArrayList<String> markedColumns = new ArrayList<>();
      Cursor info = null;

      try {
        info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), TvBrowserContentProvider.getColumnArrayWithMarkingColumns(TvBrowserContentProvider.DATA_KEY_TITLE, TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE), null, null, null);

        if (IOUtils.prepareAccessFirst(info)) {
          for (String column : TvBrowserContentProvider.MARKING_COLUMNS) {
            int index = info.getColumnIndex(column);

            if (index >= 0 && info.getInt(index) == 1) {
              markedColumns.add(column);
            }
          }

          title = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));

          if (!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
            episode = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
          }
        }
      } finally {
        IOUtils.close(info);
      }
      ContentValues values = new ContentValues();

      switch (item.getItemId()) {
        case R.id.create_favorite_item:
          UiUtils.editFavorite(null, activity, title);
          return true;
        case R.id.edit_favorite_item:
          final Favorite[] exludeFavorites = Favorite.getFavoritesForUniqueId(activity, programID);

          if (exludeFavorites.length == 1) {
            UiUtils.editFavorite(exludeFavorites[0], activity, null);
          } else if (exludeFavorites.length > 1) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.dialog_edit_favorite_select_title);

            final ArrayList<String> items = new ArrayList<>();

            for (Favorite favorite : exludeFavorites) {
              items.add(favorite.getName());
            }

            builder.setItems(items.toArray(new String[items.size()]), (dialog, which) -> UiUtils.editFavorite(exludeFavorites[which], activity, null));

            builder.setNegativeButton(android.R.string.cancel, null);

            final AlertDialog d = builder.create();
            d.show();
          }

          return true;
        case R.id.program_popup_search_repetition:
          searchForRepetition(activity, title, episode, parent);
          break;
        case R.id.prog_add_reminder:
          if (markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER) || markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) {
            return true;
          } else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, true);
          }

          ProgramUtils.addReminderId(activity, programID);
          //addReminder(activity.getApplicationContext(),programID,0,UiUtils.class,true);
          break;
        case R.id.prog_remove_reminder:
          if (!(markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER) || markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER))) {
            return true;
          }

          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, false);
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, 0);
          values.put(TvBrowserContentProvider.DATA_KEY_REMOVED_REMINDER, true);

          ProgramUtils.removeReminderId(activity, programID);

          if (menuView != null) {
            markedColumns.remove(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER);
            markedColumns.remove(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER);

            if (markedColumns.isEmpty()) {
              menuView.setBackgroundResource(android.R.drawable.list_selector_background);
            } else {
              handleMarkings(activity, null, menuView, IOUtils.getStringArrayFromList(markedColumns));
            }
          }

          IOUtils.removeReminder(activity.getApplicationContext(), programID);
          break;
        case R.id.program_popup_dont_want_to_see:
          if (title != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.action_dont_want_to_see);

            View view = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dont_want_to_see_edit, parent instanceof ViewGroup ? (ViewGroup) parent : null, false);

            final TextView exclusion = view.findViewById(R.id.dont_want_to_see_value);
            final CheckBox caseSensitive = view.findViewById(R.id.dont_want_to_see_case_sensitve);
            exclusion.setText(title);

            builder.setView(view);

            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
              if (!SettingConstants.UPDATING_FILTER) {
                SettingConstants.UPDATING_FILTER = true;

                String key = activity.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES);
                Set<String> dontWantToSeeSet = PreferenceManager.getDefaultSharedPreferences(activity).getStringSet(key, null);

                HashSet<String> newDontWantToSeeSet = new HashSet<>();

                String exclusionText = exclusion.getText().toString().trim();
                boolean caseSensitiveValue = caseSensitive.isChecked();

                if (exclusionText.length() > 0) {
                  if (dontWantToSeeSet != null) {
                    newDontWantToSeeSet.addAll(dontWantToSeeSet);
                  }

                  final String exclusion1 = exclusionText + ";;" + (caseSensitiveValue ? "1" : "0");

                  new Thread() {
                    public void run() {
                      if (activity instanceof TvBrowser) {
                        ((TvBrowser) activity).updateProgressIcon(true);
                      }

                      Context applicationContext = activity.getApplicationContext();

                      NotificationCompat.Builder builder1 = new NotificationCompat.Builder(applicationContext, App.getNotificationChannelIdDefault(activity));
                      builder1.setSmallIcon(R.drawable.ic_stat_notify);
                      builder1.setOngoing(true);
                      builder1.setContentTitle(activity.getResources().getText(R.string.action_dont_want_to_see));
                      builder1.setContentText(activity.getResources().getText(R.string.dont_want_to_see_refresh_notification_text));

                      int notifyID = 4;

                      NotificationManager notification = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
                      notification.notify(notifyID, builder1.build());

                      ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<>();
                      String title1 = null;

                      Cursor c = null;

                      try {
                        c = activity.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[]{TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.DATA_KEY_TITLE}, " NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.KEY_ID);

                        if (IOUtils.prepareAccess(c)) {
                          int size = c.getCount();
                          int count = 0;

                          builder1.setProgress(100, 0, true);
                          notification.notify(notifyID, builder1.build());

                          int keyColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                          int titleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);
                          DontWantToSeeExclusion exclusionValue = new DontWantToSeeExclusion(exclusion1);

                          int lastPercent = 0;

                          while (c.moveToNext()) {
                            int percent = (int) (count++ / (float) size * 100);

                            if (lastPercent != percent) {
                              lastPercent = percent;
                              builder1.setProgress(100, percent, false);
                              notification.notify(notifyID, builder1.build());
                            }

                            title1 = c.getString(titleColumn);

                            if (UiUtils.filter(title1, exclusionValue)) {
                              ContentValues values1 = new ContentValues();
                              values1.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, 1);

                              ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, c.getLong(keyColumn)));
                              opBuilder.withValues(values1);

                              updateValuesList.add(opBuilder.build());
                            } else {
                              try {
                                sleep(1);
                              } catch (InterruptedException e) {
                                e.printStackTrace();
                              }
                            }
                          }
                        }
                      } finally {
                        IOUtils.close(c);
                      }
                      if (!updateValuesList.isEmpty()) {
                        try {
                          activity.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                          sendDontWantToSeeChangedBroadcast(applicationContext, true);
                        } catch (RemoteException e) {
                          e.printStackTrace();
                        } catch (OperationApplicationException e) {
                          e.printStackTrace();
                        }
                      }

                      notification.cancel(notifyID);

                      if (activity instanceof TvBrowser) {
                        ((TvBrowser) activity).updateProgressIcon(false);
                      }

                      SettingConstants.UPDATING_FILTER = false;
                    }
                  }.start();

                  newDontWantToSeeSet.add(exclusion1);

                  Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();

                  edit.putStringSet(key, newDontWantToSeeSet);
                  edit.commit();
                }
              }
            });

            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            });

            builder.show();
          }

          return true;
        case R.id.program_popup_want_to_see:
          if (title != null && !SettingConstants.UPDATING_FILTER) {
            SettingConstants.UPDATING_FILTER = true;

            Set<String> exclusionValues = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);
            //ArrayList<>
            HashSet<String> newExclusionSet = new HashSet<>();
            final ArrayList<DontWantToSeeExclusion> exclusionList = new ArrayList<>();

            for (String exclusion : exclusionValues) {
              if (!filter(title, new DontWantToSeeExclusion(exclusion))) {
                newExclusionSet.add(exclusion);
                exclusionList.add(new DontWantToSeeExclusion(exclusion));
              }
            }

            new Thread() {
              public void run() {
                if (activity instanceof TvBrowser) {
                  ((TvBrowser) activity).updateProgressIcon(true);
                }

                final Context applicationContext = activity.getApplicationContext();

                final NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext, App.getNotificationChannelIdDefault(activity));
                builder.setSmallIcon(R.drawable.ic_stat_notify);
                builder.setOngoing(true);
                builder.setContentTitle(activity.getResources().getText(R.string.action_dont_want_to_see));
                builder.setContentText(activity.getResources().getText(R.string.dont_want_to_see_refresh_notification_text));

                int notifyID = 3;

                NotificationManager notification = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notification.notify(notifyID, builder.build());

                ArrayList<ContentProviderOperation> updateValuesList = new ArrayList<>();
                Cursor c = null;

                try {
                  c = activity.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, new String[]{TvBrowserContentProvider.KEY_ID, TvBrowserContentProvider.DATA_KEY_TITLE}, TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.KEY_ID);

                  if (IOUtils.prepareAccess(c)) {
                    int size = c.getCount();
                    int count = 0;

                    builder.setProgress(size, 0, true);
                    notification.notify(notifyID, builder.build());

                    int keyColumn = c.getColumnIndex(TvBrowserContentProvider.KEY_ID);
                    int titleColumn = c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE);

                    DontWantToSeeExclusion[] exclusionArr = exclusionList.toArray(new DontWantToSeeExclusion[exclusionList.size()]);

                    while (c.moveToNext()) {
                      builder.setProgress(size, count++, false);
                      notification.notify(notifyID, builder.build());

                      String title = c.getString(titleColumn);

                      ContentValues values = new ContentValues();
                      values.put(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, (UiUtils.filter(title, exclusionArr) ? 1 : 0));

                      ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA_UPDATE, c.getLong(keyColumn)));
                      opBuilder.withValues(values);

                      updateValuesList.add(opBuilder.build());
                    }

                    notification.cancel(notifyID);
                  }
                } finally {
                  IOUtils.close(c);
                }
                if (!updateValuesList.isEmpty()) {
                  try {
                    activity.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, updateValuesList);
                    sendDontWantToSeeChangedBroadcast(applicationContext, false);
                  } catch (RemoteException e) {
                    e.printStackTrace();
                  } catch (OperationApplicationException e) {
                    e.printStackTrace();
                  }
                }

                if (activity instanceof TvBrowser) {
                  ((TvBrowser) activity).updateProgressIcon(false);
                }

                SettingConstants.UPDATING_FILTER = false;
              }
            }.start();

            Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();

            edit.putStringSet(activity.getResources().getString(R.string.I_DONT_WANT_TO_SEE_ENTRIES), newExclusionSet);
            edit.commit();
          }

          return true;
        default:
          if (!markedColumns.contains(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC)) {
            return true;
          }

          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC, false);
          values.put(TvBrowserContentProvider.DATA_KEY_REMOVED_SYNC, true);

          ProgramUtils.removeSyncId(activity, programID);

          if (menuView != null) {
            markedColumns.remove(TvBrowserContentProvider.DATA_KEY_MARKING_SYNC);

            if (markedColumns.isEmpty()) {
              menuView.setBackgroundResource(android.R.drawable.list_selector_background);
            } else {
              handleMarkings(activity, null, menuView, IOUtils.getStringArrayFromList(markedColumns));
            }
          }
          break;
      }

      if (values.size() > 0) {
        if (menuView != null) {
          menuView.invalidate();
        }

        activity.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);

        sendMarkingChangedBroadcast(activity, programID, true);
        ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(activity);
      }
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

  public static void sendMarkingChangedBroadcast(Context context, long programID, boolean onlyUpdate) {
    Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
    intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, programID);
    intent.putExtra(SettingConstants.EXTRA_MARKINGS_ONLY_UPDATE, onlyUpdate);

    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    updateImportantProgramsWidget(context.getApplicationContext());
  }

  private static class RunningDrawable extends Drawable {
    private final Paint mBase;
    private final Paint mSecond;
    private final long mStartTime;
    private final long mEndTime;
    private final boolean mVertical;

    RunningDrawable(Paint base, Paint second, long startTime, long endTime, boolean vertical) {
      mBase = base;
      mSecond = second;
      mStartTime = startTime;
      mEndTime = endTime;
      mVertical = vertical;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
      if (mStartTime <= System.currentTimeMillis() && System.currentTimeMillis() < mEndTime) {
        long expiredSeconds = System.currentTimeMillis() - mStartTime;
        float percent = expiredSeconds / (float) (mEndTime - mStartTime);

        if (mVertical) {
          int topHeight = (int) (getBounds().height() * percent);

          canvas.drawRect(0, 0, getBounds().width(), topHeight, mBase);
          canvas.drawRect(0, topHeight, getBounds().width(), getBounds().height(), mSecond);
        } else {
          int leftWidth = (int) (getBounds().width() * percent);

          canvas.drawRect(0, 0, leftWidth, getBounds().height(), mBase);
          canvas.drawRect(leftWidth, 0, getBounds().width(), getBounds().height(), mSecond);
        }
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.UNKNOWN;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
  }

  public static void handleMarkings(Context activity, Cursor cursor, View view, String[] markingValues) {
    handleMarkings(activity, cursor, view, markingValues, null, false);
  }

  public static void handleMarkings(Context context, Cursor cursor, View view, String[] markingValues, Handler handler, boolean vertical) {
    long startTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) : 0;
    long endTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) : 0;

    handleMarkings(context, cursor, startTime, endTime, view, markingValues, handler, vertical);
  }

  public static void handleMarkings(Context context, Cursor cursor, long startTime, long endTime, final View view, String[] markingValues, Handler handler) {
    handleMarkings(context, cursor, startTime, endTime, view, markingValues, handler, false);
  }

  public static void handleMarkings(Context context, Cursor cursor, long startTime, long endTime, final View view, String[] markedColumns, Handler handler, boolean vertical) {
    final LayerDrawable draw = getMarkingsDrawable(context, cursor, startTime, endTime, markedColumns, vertical);

    if (handler == null) {
      view.invalidate();
      CompatUtils.setBackground(view, draw);
    } else {
      handler.post(() -> {
        view.invalidate();
        CompatUtils.setBackground(view, draw);
      });
    }
  }

  private static LayerDrawable getMarkingsDrawable(Context context, Cursor cursor, long startTime, long endTime, String[] markedColumns, boolean vertical) {
    if (markedColumns == null && cursor != null) {
      ArrayList<String> markedColumnList = new ArrayList<>();

      for (String column : TvBrowserContentProvider.MARKING_COLUMNS) {
        int index = cursor.getColumnIndex(column);
        int keyIndex = cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID);

        if (index >= 0 && cursor.getInt(index) >= 1) {
          markedColumnList.add(column);
        } else if (keyIndex >= 0 && column.equals(TvBrowserContentProvider.DATA_KEY_MARKING_MARKING) && ProgramUtils.isMarkedWithIcon(context, cursor.getLong(keyIndex))) {
          markedColumnList.add(column);
        }
      }

      markedColumns = IOUtils.getStringArrayFromList(markedColumnList);
    }

    Paint base = new Paint();
    base.setStyle(Paint.Style.FILL_AND_STROKE);

    Paint second = null;

    if (PrefUtils.getBooleanValue(R.string.PREF_SHOW_PROGRESS, R.bool.pref_show_progress_default) && startTime <= System.currentTimeMillis() && System.currentTimeMillis() <= endTime) {
      base.setColor(getColor(ON_AIR_PROGRESS_KEY, context));
      second = new Paint();
      second.setStyle(Paint.Style.FILL_AND_STROKE);
      second.setColor(getColor(ON_AIR_BACKGROUND_KEY, context));
    } else {
      base = null;
    }

    final ArrayList<Drawable> draw = new ArrayList<>();

    if (base != null) {
      RunningDrawable running = new RunningDrawable(base, second, startTime, endTime, vertical);
      draw.add(running);
    }

    if (markedColumns != null && markedColumns.length > 0) {
      if (markedColumns.length > 1) {
        int[] colors = new int[markedColumns.length];

        for (int i = 0; i < markedColumns.length; i++) {
          Integer color = SettingConstants.MARK_COLOR_KEY_MAP.get(markedColumns[i]);

          if (markedColumns[i].equals(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE) && PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(context.getResources().getStringArray(R.array.pref_simple_string_value_array2)[0])) {
            color = null;
          }

          if (color != null) {
            colors[i] = getColor(color, context);
          }
        }

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        gd.setCornerRadius(0f);

        draw.add(gd);
      } else {
        Integer color = SettingConstants.MARK_COLOR_KEY_MAP.get(markedColumns[0]);

        if (markedColumns[0].equals(TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE) && PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(context.getResources().getStringArray(R.array.pref_simple_string_value_array2)[0])) {
          color = null;
        }

        if (color != null) {
          draw.add(new ColorDrawable(getColor(color, context)));
        }
      }
    }

    draw.add(ContextCompat.getDrawable(context, android.R.drawable.list_selector_background));

    return new LayerDrawable(draw.toArray(new Drawable[draw.size()]));
  }

  public static void editFavorite(final Favorite fav, final Context activity, String searchString) {
    Intent startEditFavorite = new Intent(activity, ActivityFavoriteEdit.class);
    startEditFavorite.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    if (fav != null) {
      startEditFavorite.putExtra(Favorite.FAVORITE_EXTRA, fav);
    } else if (searchString != null) {
      startEditFavorite.putExtra(Favorite.SEARCH_EXTRA, searchString);
    }

    activity.startActivity(startEditFavorite);
  }

  private static String formatDate(long date, Context context) {
    return formatDate(date, context, true, false);
  }

  public static String formatDate(long date, Context context, boolean onlyDays, boolean withDayString) {
    return formatDate(date, context, onlyDays, withDayString, false);
  }

  public static String formatDate(long date, Context context, boolean onlyDays, boolean withDayString, boolean withDate) {
    return formatDate(date, context, onlyDays, withDayString, withDate, java.text.DateFormat.SHORT, false);
  }

  @SuppressLint("SimpleDateFormat")
  public static String formatDate(long date, Context context, boolean onlyDays, boolean withDayString, boolean withDate, int typeDateFormat, boolean onlyDayString) {
    Calendar progDate = Calendar.getInstance();
    progDate.setTimeInMillis(date);

    Calendar today = Calendar.getInstance();

    String value = null;

    if(!onlyDayString) {
      if (progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
        value = context.getText(R.string.today).toString();
      } else {
        today.add(Calendar.DAY_OF_YEAR, 1);

        if (progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
          value = context.getText(R.string.tomorrow).toString();
        } else {
          today.add(Calendar.DAY_OF_YEAR, -2);

          if (progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            value = context.getText(R.string.yesterday).toString();
          }
        }
      }
    }

    if (value == null || withDate) {
      if (value == null) {
        value = "";
      } else if (value.trim().length() > 0) {
        value += ", ";
      }

      if (!onlyDays) {
        SimpleDateFormat df = (SimpleDateFormat) java.text.DateFormat.getDateInstance(typeDateFormat);
        String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");
Log.d("info22", pattern);
        if (withDayString) {
          pattern = "EEE " + pattern;
        }

        if (pattern.contains(".") && typeDateFormat != java.text.DateFormat.LONG) {
          pattern += ".";
        }

        SimpleDateFormat mdf = new SimpleDateFormat(pattern);

        value += mdf.format(progDate.getTime());
      } else if (withDayString) {
        SimpleDateFormat mdf = new SimpleDateFormat("EEE ");

        value += mdf.format(progDate.getTime());
      }
    }

    return value;
  }

  @SuppressLint("SimpleDateFormat")
  public static void formatDayView(Activity activity, Cursor cursor, View view, int startDayLabelID) {
    try {
      long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));

      TextView text = (TextView) view;

      SimpleDateFormat day = new SimpleDateFormat("EEE", Locale.getDefault());

      TextView startDay = ((View) view.getParent()).findViewById(/*R.id.startDayLabelPL*/startDayLabelID);
      startDay.setText(day.format(new Date(date)));

      CharSequence startDayValue = formatDate(date, activity);

      if (startDayValue != null && startDayValue.toString().trim().length() > 0) {
        startDay.setText(String.format(Locale.getDefault(), "%s, %s", startDayValue, startDay.getText()));
      }

      Calendar progDate = Calendar.getInstance();
      progDate.setTimeInMillis(date);

      SimpleDateFormat df = (SimpleDateFormat) java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
      String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");

      pattern = pattern.replace(".MM", ". MMM");
      pattern = pattern.replace("MM.", "MMM. ");

      SimpleDateFormat mdf = new SimpleDateFormat(pattern);

      text.setText(mdf.format(progDate.getTime()));

      UiUtils.handleMarkings(activity, cursor, ((RelativeLayout) view.getParent()), null);
    } catch (Throwable ignored) {
    }
  }

  public static int convertDpToPixel(float dp, Resources res) {
    // Get the screen's density scale
    final float scale = res.getDisplayMetrics().density;
    // Convert the dps to pixels, based on density scale

    return (int) (dp * scale + 0.5f);
  }

  public static float convertPixelsToSp(float px, Context context) {
    float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
    return px / scaledDensity;
  }

  private static boolean filter(String title, DontWantToSeeExclusion exclusion) {
    return exclusion.matches(title);
  }

  public static boolean filter(String title, DontWantToSeeExclusion[] values) {
    boolean found = false;

    if (title != null) {
      if (values == null) {
        Set<String> exclusionValues = PrefUtils.getStringSetValue(R.string.I_DONT_WANT_TO_SEE_ENTRIES, null);

        values = new DontWantToSeeExclusion[exclusionValues.size()];

        int i = 0;

        for (String exclusion : exclusionValues) {
          values[i++] = new DontWantToSeeExclusion(exclusion);
        }
      }

        for (DontWantToSeeExclusion value : values) {
          if (filter(title, value)) {
            found = true;
            break;
          }
        }
      }

    return found;
  }

  public static int getColor(int key, Context context) throws NullPointerException {
    if (context == null) {
      throw new NullPointerException("Context parameter is null.");
    }

    return getColor(key, PreferenceManager.getDefaultSharedPreferences(context), context);
  }

  private static int getColor(int key, SharedPreferences pref, Context context) throws NullPointerException {
    if (pref == null) {
      throw new NullPointerException("Preferences parameter is null.");
    } else if (context == null) {
      throw new NullPointerException("Context parameter is null.");
    }

    int color = 0;

    switch (key) {
      case EXPIRED_COLOR_KEY:
        if (PrefUtils.isDarkTheme()) {
          color = SettingConstants.EXPIRED_DARK_COLOR;
        } else {
          color = SettingConstants.EXPIRED_LIGHT_COLOR;
        }
        break;
      case MARKED_COLOR_KEY:
        color = pref.getInt(context.getString(R.string.PREF_COLOR_MARKED), ContextCompat.getColor(context, R.color.pref_color_mark_tvb_style_default));
        break;
      case MARKED_FAVORITE_COLOR_KEY:
        color = pref.getInt(context.getString(R.string.PREF_COLOR_FAVORITE), ContextCompat.getColor(context, R.color.pref_color_mark_favorite_tvb_style_default));
        break;
      case MARKED_REMINDER_COLOR_KEY:
        color = pref.getInt(context.getString(R.string.PREF_COLOR_REMINDER), ContextCompat.getColor(context, R.color.pref_color_mark_reminder_tvb_style_default));
        break;
      case MARKED_SYNC_COLOR_KEY:
        color = pref.getInt(context.getString(R.string.PREF_COLOR_SYNC), ContextCompat.getColor(context, R.color.pref_color_mark_sync_tvb_style_favorite_default));
        break;
      case ON_AIR_BACKGROUND_KEY:
        color = pref.getInt(context.getString(R.string.PREF_COLOR_ON_AIR_BACKGROUND), ContextCompat.getColor(context, R.color.pref_color_on_air_background_tvb_style_default));
        break;
      case ON_AIR_PROGRESS_KEY:
        color = pref.getInt(context.getString(R.string.PREF_COLOR_ON_AIR_PROGRESS), ContextCompat.getColor(context, R.color.pref_color_on_air_progress_tvb_style_default));
        break;
      case RUNNING_TIME_SELECTION_KEY:
        color = pref.getInt(context.getString(R.string.PREF_RUNNING_TIME_SELECTION), ContextCompat.getColor(context, R.color.pref_color_running_time_selection_background_tvb_style_default));
        break;
      case I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR_KEY:
        color = pref.getInt(context.getString(R.string.PREF_I_DONT_WANT_TO_SEE_HIGHLIGHT_COLOR), ContextCompat.getColor(context, R.color.i_dont_want_to_see_highlight));
        break;
    }

    return color;
  }

  public static Bitmap drawableToBitmap(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      return ((BitmapDrawable) drawable).getBitmap();
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

    if (colorValues.length == 4) {
      color = color | (colorValues[index++] << 24);
    }

    if (colorValues.length >= 3) {
      color = color | (colorValues[index++] << 16);
      color = color | (colorValues[index++] << 8);
      color = color | (colorValues[index++]);
    }

    return color;
  }

  public static void scaleTextViews(View view, float scale) {
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;

      for (int i = 0; i < group.getChildCount(); i++) {
        scaleTextViews(group.getChildAt(i), scale);
      }
    } else if (view instanceof TextView) {
      TextView text = (TextView) view;

      text.setTextSize(TypedValue.COMPLEX_UNIT_PX, text.getTextSize() * scale);
    }
  }

  public static Bitmap createBitmapFromByteArray(byte[] data) {
    Bitmap logoBitmap = null;

    if (data != null && data.length > 0) {
      try {
        logoBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
      } catch (NullPointerException ignored) {
      }
    }

    return logoBitmap;
  }

  public static String getDontWantToSeeFilterString(Context context) {
    String returnValue = "";
    if (PrefUtils.getStringValue(R.string.PREF_I_DONT_WANT_TO_SEE_FILTER_TYPE, R.string.pref_i_dont_want_to_see_filter_type_default).equals(context.getResources().getStringArray(R.array.pref_simple_string_value_array2)[0])) {
      returnValue = " AND ( NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE + " ) ";
    }

    return returnValue;
  }

  public static void updateImportantProgramsWidget(Context context) {
    Log.d("info2", "updateImportantProgramsWidget");
    try {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

      ComponentName importantProgramsWidget = new ComponentName(context, ImportantProgramsListWidget.class);
      int[] appWidgetIds = appWidgetManager.getAppWidgetIds(importantProgramsWidget);
      Log.d("info2", "appWidgetIds " + Arrays.toString(appWidgetIds));
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.important_widget_list_view);
    } catch (Throwable t) {
      Log.d("info2", "", t);
    }
  }

  public static void updateRunningProgramsWidget(Context context) {
    Log.d("info2", "updateRunningProgramsWidget");
    try {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

      ComponentName importantProgramsWidget = new ComponentName(context, RunningProgramsListWidget.class);
      int[] appWidgetIds = appWidgetManager.getAppWidgetIds(importantProgramsWidget);
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.running_widget_list_view);
      Log.d("info2", "appWidgetIds " + Arrays.toString(appWidgetIds));
    } catch (Throwable t) {
      Log.d("info2", "", t);
    }
  }

  public static void reloadWidgets(Context context) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

    ComponentName programsWidget = new ComponentName(context.getApplicationContext(), ImportantProgramsListWidget.class);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(programsWidget);

    for (int appWidgetId : appWidgetIds) {
      Intent update = new Intent(SettingConstants.UPDATE_IMPORTANT_APP_WIDGET);
      update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

      context.sendBroadcast(update);
    }

    programsWidget = new ComponentName(context.getApplicationContext(), RunningProgramsListWidget.class);
    appWidgetIds = appWidgetManager.getAppWidgetIds(programsWidget);

    for (int appWidgetId : appWidgetIds) {
      Intent update = new Intent(SettingConstants.UPDATE_RUNNING_APP_WIDGET);
      update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

      context.sendBroadcast(update);
    }
  }

  public static void updateToggleReminderStateWidget(Context context) {
    Log.d("info2", "updateToggleReminderStateWidget");
    try {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

      ComponentName importantProgramsWidget = new ComponentName(context, WidgetToggleReminderState.class);
      int[] appWidgetIds = appWidgetManager.getAppWidgetIds(importantProgramsWidget);

      for (int appWidgetId : appWidgetIds) {
        WidgetToggleReminderState.updateAppWidget(context, appWidgetManager, appWidgetId);
      }
      //appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.running_widget_list_view);
      Log.d("info2", "appWidgetIds " + Arrays.toString(appWidgetIds));
    } catch (Throwable t) {
      Log.d("info2", "", t);
    }
  }


  public static void showChannelFilterSelection(Context context, final ChannelFilter channelFilter, ViewGroup parent) {
    showChannelFilterSelection(context, channelFilter, parent, null);
  }

  public static void showChannelFilterSelection(Context context, final ChannelFilter channelFilter, ViewGroup parent, final Runnable cancelCallBack) {
    if (IOUtils.isDatabaseAccessible(context)) {
      String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER
      };

      int[] channelRestriction = channelFilter.getFilteredChannelIds();

      final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      // inflate channel selection view
      View channelSelectionView = inflater.inflate(R.layout.dialog_channel_selection_list, parent, false);
      channelSelectionView.findViewById(R.id.channel_country_label).setVisibility(View.GONE);
      channelSelectionView.findViewById(R.id.channel_country_value).setVisibility(View.GONE);
      channelSelectionView.findViewById(R.id.channel_category_label).setVisibility(View.GONE);
      channelSelectionView.findViewById(R.id.channel_category_value).setVisibility(View.GONE);

      final EditText filterName = channelSelectionView.findViewById(R.id.channel_selection_input_id_name);

      if (channelFilter.getName() == null) {
        channelSelectionView.findViewById(R.id.channel_selection_label_id_name).setVisibility(View.GONE);
        filterName.setVisibility(View.GONE);
      } else {
        filterName.setText(channelFilter.getName());
      }

      final ListView list = channelSelectionView.findViewById(R.id.channel_selection_list);

      final Bitmap defaultLogo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);

      final ArrayAdapter<AdapterChannel> channelAdapter = new ArrayAdapter<AdapterChannel>(context, android.R.layout.simple_list_item_1) {
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
          AdapterChannel value = getItem(position);
          ChannelViewHolder holder = null;

          if (convertView == null) {
            holder = new ChannelViewHolder();

            convertView = inflater.inflate(R.layout.channel_row, parent, false);

            holder.mTextView = convertView.findViewById(R.id.row_of_channel_text);
            holder.mCheckBox = convertView.findViewById(R.id.row_of_channel_selection);
            holder.mLogo = convertView.findViewById(R.id.row_of_channel_icon);

            convertView.setTag(holder);

          } else {
            holder = (ChannelViewHolder) convertView.getTag();
          }

          holder.mTextView.setText(value.mName);
          holder.mCheckBox.setChecked(value.mSelected);
          list.setItemChecked(position, value.mSelected);

          Bitmap logo = value.mChannelLogo;

          if (logo != null) {
            holder.mLogo.setImageBitmap(logo);
          } else {
            holder.mLogo.setImageBitmap(defaultLogo);
          }

          return convertView;
        }
      };

      Cursor channels = null;

      try {
        channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);

        if (IOUtils.prepareAccess(channels)) {
          final int idColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
          final int logoColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
          final int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
          final int orderNumberColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);

          while (channels.moveToNext()) {
            int channelID = channels.getInt(idColumn);
            String name = channels.getString(nameColumn);
            int orderNumber = channels.getInt(orderNumberColumn);
            byte[] logo = channels.getBlob(logoColumn);

            if (orderNumber < 1) {
              name = "-. " + name;
            } else {
              name = orderNumber + ". " + name;
            }

            Bitmap channelLogo = UiUtils.createBitmapFromByteArray(logo);

            if (channelLogo != null) {
              BitmapDrawable l = new BitmapDrawable(context.getResources(), channelLogo);

              ColorDrawable background = new ColorDrawable(SettingConstants.LOGO_BACKGROUND_COLOR);
              background.setBounds(0, 0, channelLogo.getWidth() + 2, channelLogo.getHeight() + 2);

              LayerDrawable logoDrawable = new LayerDrawable(new Drawable[]{background, l});
              logoDrawable.setBounds(background.getBounds());

              l.setBounds(2, 2, channelLogo.getWidth(), channelLogo.getHeight());

              channelLogo = UiUtils.drawableToBitmap(logoDrawable);
            }

            channelAdapter.add(new AdapterChannel(channelID, name, channelLogo, isRestricted(channelRestriction, channelID)));
          }
        }
      } finally {
        IOUtils.close(channels);
      }
      list.setAdapter(channelAdapter);

      channelSelectionView.findViewById(R.id.channel_selection_select_all).setOnClickListener(v -> {
        for (int i = 0; i < channelAdapter.getCount(); i++) {
          channelAdapter.getItem(i).mSelected = true;
        }

        list.invalidateViews();
      });

      channelSelectionView.findViewById(R.id.channel_selection_remove_selection).setOnClickListener(v -> {
        for (int i = 0; i < channelAdapter.getCount(); i++) {
          channelAdapter.getItem(i).mSelected = false;
        }

        list.invalidateViews();
      });

      list.setOnItemClickListener((parent1, view, position, id) -> {
        CheckBox check = view.findViewById(R.id.row_of_channel_selection);

        if (check != null) {
          check.setChecked(!check.isChecked());
          channelAdapter.getItem(position).mSelected = check.isChecked();
          list.setItemChecked(position, check.isChecked());
        }
      });

      AlertDialog.Builder builder = new AlertDialog.Builder(context);

      builder.setView(channelSelectionView);
      builder.setCancelable(false);

      builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
        ArrayList<Integer> channelIDList = new ArrayList<>();
        boolean allSelected = true;

        for (int i = 0; i < channelAdapter.getCount(); i++) {
          AdapterChannel item = channelAdapter.getItem(i);

          if (item.mSelected) {
            channelIDList.add(item.mChannelID);
          } else {
            allSelected = false;
          }
        }

        if (allSelected || channelIDList.isEmpty()) {
          channelFilter.setFilterValues(null, null);
        } else {
          int[] ids = new int[channelIDList.size()];

          for (int i = 0; i < ids.length; i++) {
            ids[i] = channelIDList.get(i);
          }

          String name = null;

          if (filterName.getVisibility() == View.VISIBLE) {
            name = filterName.getText().toString().trim();
          }

          channelFilter.setFilterValues(name, ids);
        }
      });

      if (cancelCallBack != null) {
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> cancelCallBack.run());
      } else {
        builder.setNegativeButton(android.R.string.cancel, null);
      }

      AlertDialog dialog = builder.create();
      dialog.show();

      if (filterName.getVisibility() == View.VISIBLE) {
        final Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setEnabled(filterName.getText().toString().trim().length() > 0);

        filterName.addTextChangedListener(new TextWatcher() {

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          }

          @Override
          public void afterTextChanged(Editable s) {
            ok.setEnabled(filterName.getText().toString().trim().length() > 0);
          }
        });
      }
    }
  }

  public static void showCategorySelection(Context context, final CategoryFilter categoryFilter, ViewGroup parent, final Runnable cancelCallBack) {
    String[] names = IOUtils.getInfoStringArrayNames(context.getResources());
    int[] restrictionIndices = categoryFilter.getCategoryIndices();

    final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View selectionView = inflater.inflate(R.layout.dialog_filter_categories, parent, false);

    final EditText nameInput = selectionView.findViewById(R.id.dialog_filter_categories_input_name);
    final Spinner operationSelection = selectionView.findViewById(R.id.dialog_filter_categories_selection_operation);
    final ListView listView = selectionView.findViewById(R.id.dialog_filter_categories_list);

    UiUtils.createAdapterForSpinner(context, operationSelection, R.array.dialog_categories_operation);

    if (categoryFilter.getName() == null) {
      selectionView.findViewById(R.id.dialog_filter_categories_label_name).setVisibility(View.GONE);
      nameInput.setVisibility(View.GONE);
      operationSelection.setVisibility(View.GONE);
      selectionView.findViewById(R.id.dialog_filter_categories_label_operation).setVisibility(View.GONE);
    } else {
      operationSelection.setSelection(categoryFilter.getOperation().equals("AND") ? 0 : 1);
      nameInput.setText(categoryFilter.getName());
    }

    final ArrayAdapter<AdapterCategory> categoryAdapter = new ArrayAdapter<AdapterCategory>(context, android.R.layout.simple_list_item_1) {
      @NonNull
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        AdapterCategory value = getItem(position);
        CategoryViewHolder holder = null;

        if (convertView == null) {
          holder = new CategoryViewHolder();

          convertView = inflater.inflate(R.layout.list_row_selection, parent, false);

          holder.mTextView = convertView.findViewById(R.id.row_selection_text);
          holder.mCheckBox = convertView.findViewById(R.id.row_selection_selection);

          convertView.setTag(holder);
        } else {
          holder = (CategoryViewHolder) convertView.getTag();
        }

        holder.mTextView.setText(value.mName);
        holder.mCheckBox.setChecked(value.mSelected);
        listView.setItemChecked(position, value.mSelected);

        return convertView;
      }
    };

    for (int i = 0; i < names.length; i++) {
      boolean selected = false;

      if (restrictionIndices != null) {
        for (int index : restrictionIndices) {
          if (i == index) {
            selected = true;
            break;
          }
        }
      }

      int[] color = IOUtils.getActivatedColorFor(PrefUtils.getStringValue(SettingConstants.CATEGORY_COLOR_PREF_KEY_ARR[i], null));

      SpannableString name = new SpannableString(names[i]);

      if (color[0] == 1) {
        name.setSpan(new ForegroundColorSpan(color[1]), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }

      AdapterCategory category = new AdapterCategory(i, name, selected);

      categoryAdapter.add(category);
    }

    listView.setAdapter(categoryAdapter);

    listView.setOnItemClickListener((parent1, view, position, id) -> {
      CheckBox check = view.findViewById(R.id.row_selection_selection);

      if (check != null) {
        check.setChecked(!check.isChecked());
        categoryAdapter.getItem(position).mSelected = check.isChecked();
        listView.setItemChecked(position, check.isChecked());
      }
    });

    AlertDialog.Builder builder = new AlertDialog.Builder(context);

    builder.setView(selectionView);
    builder.setCancelable(false);

    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
      ArrayList<Integer> channelIDList = new ArrayList<>();
      boolean allSelected = true;

      for (int i = 0; i < categoryAdapter.getCount(); i++) {
        AdapterCategory item = categoryAdapter.getItem(i);

        if (item.mSelected) {
          channelIDList.add(item.mIndex);
        } else {
          allSelected = false;
        }
      }

      if (allSelected || channelIDList.isEmpty()) {
        categoryFilter.setFilterValues(null, null, null);
      } else {
        int[] indicies = new int[channelIDList.size()];

        for (int i = 0; i < indicies.length; i++) {
          indicies[i] = channelIDList.get(i);
        }

        String name = null;

        if (nameInput.getVisibility() == View.VISIBLE) {
          name = nameInput.getText().toString().trim();
        }

        String operation = "AND";

        if (operationSelection.getVisibility() == View.VISIBLE) {
          if (operationSelection.getSelectedItemPosition() == 1) {
            operation = "OR";
          }
        }

        categoryFilter.setFilterValues(name, operation, indicies);
      }
    });

    if (cancelCallBack != null) {
      builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> cancelCallBack.run());
    } else {
      builder.setNegativeButton(android.R.string.cancel, null);
    }

    AlertDialog dialog = builder.create();
    dialog.show();

    if (nameInput.getVisibility() == View.VISIBLE) {
      final Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
      ok.setEnabled(nameInput.getText().toString().trim().length() > 0);

      nameInput.addTextChangedListener(new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
          ok.setEnabled(nameInput.getText().toString().trim().length() > 0);
        }
      });
    }
  }

  public static void showKeywordSelection(Context context, final KeywordFilter keywordFilter, ViewGroup parent, final Runnable cancelCallBack) {
    final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View selectionView = inflater.inflate(R.layout.dialog_filter_keyword, parent, false);

    final EditText nameInput = selectionView.findViewById(R.id.dialog_filter_keyword_input_name);
    final EditText keywordInput = selectionView.findViewById(R.id.dialog_filter_keyword_input_keyword);
    final Spinner columnSelection = selectionView.findViewById(R.id.dialog_filter_keyword_selection_column);

    nameInput.setText(keywordFilter.getName());
    keywordInput.setText(keywordFilter.getKeyword());

    final ArrayAdapter<NamedFields> columnSelectionAdatper = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
    columnSelection.setAdapter(columnSelectionAdatper);

    for (int i = 0; i < TvBrowserContentProvider.TEXT_SEARCHABLE_COLUMN_ARRAY.length; i++) {
      final String column = TvBrowserContentProvider.TEXT_SEARCHABLE_COLUMN_ARRAY[i];

      columnSelectionAdatper.add(new NamedFields(context, column));
    }

    columnSelectionAdatper.sort(NamedFields.COMPARATOR);

    for (int i = 0; i < columnSelectionAdatper.getCount(); i++) {
      if (columnSelectionAdatper.getItem(i).getColumn().equals(keywordFilter.getColumn())) {
        columnSelection.setSelection(i);
      }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(context);

    builder.setView(selectionView);
    builder.setCancelable(false);

    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> keywordFilter.setFilterValues(nameInput.getText().toString().trim(), keywordInput.getText().toString(), ((NamedFields) columnSelection.getSelectedItem()).getColumn()));

    if (cancelCallBack != null) {
      builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> cancelCallBack.run());
    } else {
      builder.setNegativeButton(android.R.string.cancel, null);
    }

    AlertDialog dialog = builder.create();
    dialog.show();

    if (nameInput.getVisibility() == View.VISIBLE) {
      final Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
      ok.setEnabled(nameInput.getText().toString().trim().length() > 0 && keywordInput.getText().toString().trim().length() > 0);

      final TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
          ok.setEnabled(nameInput.getText().toString().trim().length() > 0 && keywordInput.getText().toString().trim().length() > 0);
        }
      };

      nameInput.addTextChangedListener(textWatcher);
      keywordInput.addTextChangedListener(textWatcher);
    }
  }


  private static final class AdapterCategory {
    final int mIndex;
    final Spannable mName;
    boolean mSelected;

    AdapterCategory(int index, Spannable name, boolean selected) {
      mIndex = index;
      mName = name;
      mSelected = selected;
    }
  }

  private static final class CategoryViewHolder {
    CheckBox mCheckBox;
    TextView mTextView;
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

    if (!returnValue) {
      for (int channelID : channelIDs) {
        if (id == channelID) {
          returnValue = true;
          break;
        }
      }
    }

    return returnValue;
  }

  private static final class AdapterChannel {
    final int mChannelID;
    final String mName;
    final Bitmap mChannelLogo;
    boolean mSelected;

    AdapterChannel(int channelID, String name, Bitmap channelLogo, boolean selected) {
      mChannelID = channelID;
      mName = name;
      mChannelLogo = channelLogo;
      mSelected = selected;
    }
  }

  public static final int TYPE_THEME_DEFAULT = 1;
  public static final int TYPE_THEME_TOOLBAR = 2;
  public static final int TYPE_THEME_TRANSLUCENT = 3;

  public static int getThemeResourceId(final int type, final boolean useDarkTheme) {
    int style = R.style.Theme_App;

    switch (type) {
      case TYPE_THEME_DEFAULT: if(useDarkTheme) {style = R.style.Theme_App_Dark;}
        break;
      case TYPE_THEME_TOOLBAR: style = useDarkTheme ? R.style.Theme_Pref_Dark : R.style.Theme_Pref_Light;break;
      case TYPE_THEME_TRANSLUCENT: style = useDarkTheme ? R.style.Theme_TvBrowser_Translucent : R.style.Theme_TvBrowser_Translucent_Light;break;
    }

    return style;
  }

  public static ImageSpan createImageSpan(Context context, int drawable) {
    Drawable icon = ContextCompat.getDrawable(context, drawable);

    float zoom = 16f / icon.getIntrinsicHeight() * context.getResources().getDisplayMetrics().density;

    icon.setBounds(0, 0, (int) (icon.getIntrinsicWidth() * zoom), (int) (icon.getIntrinsicHeight() * zoom));

    if (!PrefUtils.isDarkTheme()) {
      icon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
    } else {
      icon.setColorFilter(null);
    }

    return new ImageSpan(icon, ImageSpan.ALIGN_BASELINE);
  }

  private static final class ActorColumnSpan implements LeadingMarginSpan {
    private final String[] mActorParts;
    private final int mMargin;
    private int mFirstBaseline;
    private boolean mHighlight;
    private final int mHighlightColor;

    ActorColumnSpan(String actor, int leadingMargin, ArrayList<FavoriteTypePattern> patternList, int color) {
      mHighlight = false;

      for (FavoriteTypePattern pattern : patternList) {
        if (pattern.isNotOnlyTitleType() && pattern.mPattern.matcher(actor.replaceAll("\\n+", " ")).find()) {
          mHighlight = true;
          break;
        }
      }

      mActorParts = actor.split("\n");
      mMargin = leadingMargin;
      mFirstBaseline = -1;
      mHighlightColor = color;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top,
                                  int baseline, int bottom, CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
      if (first && (mFirstBaseline == -1 || mFirstBaseline == baseline)) {
        mFirstBaseline = baseline;

        if (mHighlight) {
          c.save();
          c.clipRect(x + dir, top, x + dir + p.measureText(mActorParts[0]), bottom);
          c.drawColor(mHighlightColor);
          c.restore();
        }

        c.drawText(mActorParts[0], x + dir, baseline, p);

        if (mActorParts.length > 1) {
          if (mHighlight) {
            c.save();
            c.clipRect(x + dir, 2 * bottom - top, x + dir + p.measureText(mActorParts[1]), bottom);
            c.drawColor(mHighlightColor);
            c.restore();
          }

          c.drawText(mActorParts[1], x + dir, baseline + (bottom - top), p);
        }
        if (mActorParts.length > 2) {
          if (mHighlight) {
            c.save();
            c.clipRect(x + dir, 3 * bottom - top, x + dir + p.measureText(mActorParts[2]), bottom);
            c.drawColor(mHighlightColor);
            c.restore();
          }

          c.drawText(mActorParts[2], x + dir, baseline + 2 * (bottom - top), p);
        }
      }
    }

    @Override
    public int getLeadingMargin(boolean first) {
      return mMargin;
    }
  }

  private static void checkAndAddHiglightingForFavorites(TextView view, CharSequence text, ArrayList<FavoriteTypePattern> patternList, boolean allPatterns, BackgroundColorSpan backgroundColorSpan) {
    SpannableStringBuilder string = new SpannableStringBuilder(text);

    for (FavoriteTypePattern typePattern : patternList) {
      if (allPatterns || typePattern.isNotOnlyTitleType()) {
        string = typePattern.addHighlighting(string, backgroundColorSpan);
      }
    }

    view.setText(string);
  }

  static final class FavoriteTypePattern {
    private final int mType;
    private final Pattern mPattern;

    FavoriteTypePattern(int type, Pattern pattern) {
      mType = type;
      mPattern = pattern;
    }

    SpannableStringBuilder addHighlighting(SpannableStringBuilder text, BackgroundColorSpan backgroundColorSpan) {
      final Matcher m = mPattern.matcher(text);

      int pos = 0;

      while (m.find(pos)) {
        text.setSpan(backgroundColorSpan, m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        pos = m.end();
      }

      return text;
    }

    boolean isNotOnlyTitleType() {
      return mType != Favorite.KEYWORD_ONLY_TITLE_TYPE;
    }
  }

  /**
   * Returns a thread-safe {@link Collator}, that can be used with {@link Comparator} instances
   * to sort strings in a locale sensitive way; i.e. to apply a rule-based sort while sorting
   * strings containing umlauts (Dänemark < Deutschland, Norwegen < Österreich < Polen; a < ä = ae).
   *
   * @return Collator for the default locale (see {@link Locale#getDefault()}).
   */
  public static Collator getCollator() {
    return collatorThreadLocal.get();
  }

  @SuppressWarnings("WeakerAccess")
  @NonNull
  public static Point getScreenSize(@NonNull final Context context) {
    final Point size = new Point();
    final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    if (wm != null) {
      wm.getDefaultDisplay().getSize(size);
    }
    return size;
  }

  private static SimpleDateFormat TIME_FORMAT = null;

  public static SimpleDateFormat getTimeFormat(final Context context) {
    if(TIME_FORMAT == null) {
      createTimeFormat(context);
    }

    return TIME_FORMAT;
  }

  private static synchronized void createTimeFormat(final Context context) {
    if(TIME_FORMAT == null) {
      java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(context);
      String value = ((SimpleDateFormat) mTimeFormat).toLocalizedPattern();

      if ((value.charAt(0) == 'H' && value.charAt(1) != 'H') || (value.charAt(0) == 'h' && value.charAt(1) != 'h')) {
        value = value.charAt(0) + value;
      }

      TIME_FORMAT = new SimpleDateFormat(value, Locale.getDefault());
    }
  }

  public static final void createAdapterForSpinner(final Context context, final Spinner spinner, final int arrayResId) {
    final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    adapter.addAll(context.getResources().getStringArray(arrayResId));

    spinner.setAdapter(adapter);
  }

  private static final class AsyncInfo extends AsyncTask<Void, Void, Boolean> {
    private View mLayout;
    private ProgressDialog mProgress;
    private Handler mHandler;
    private Runnable mShowWaiting;
    private boolean mShowWaitingDialog;
    private boolean mHasSpannableActors;

    private long mPreviousId;
    private long mNextId;

    private final Context context;
    private final long id;
    private final WeakReference<Activity> finish;
    private final View parent;
    private final Handler handler;

    private AsyncInfo(final Context context, final long id, final Activity finish, final View parent, final Handler handler) {
      this.context = context;
      this.id = id;
      this.finish = new WeakReference<>(finish);
      this.parent = parent;
      this.handler = handler;
    }

    @Override
    protected void onPreExecute() {
      mPreviousId = -1;
      mNextId = -1;

      mShowWaitingDialog = true;
      mHandler = new Handler();
      mShowWaiting = () -> {
        mProgress = new ProgressDialog(context);
        mProgress.setMessage(context.getString(R.string.waiting_detail_show));

        if (mShowWaitingDialog) {
          try {
            mProgress.show();
          } catch (BadTokenException ignored) {
          }
        }
      };

      int layout = PrefUtils.getBooleanValue(R.string.PREF_DETAIL_ALLOW_SWIPE, R.bool.pref_detail_allow_swipe) ? R.layout.dialog_detail_swipe : R.layout.dialog_detail;

      mLayout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, parent instanceof ViewGroup ? (ViewGroup) parent : null, false);
      mHandler.postDelayed(mShowWaiting, 700);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
      Boolean result = Boolean.FALSE;

      final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

      final Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, TvBrowserContentProvider.KEY_ID + " ASC LIMIT 1");

      try {
        if (IOUtils.prepareAccessFirst(c)) {
          mHasSpannableActors = false;
          result = Boolean.TRUE;
          float textScale = Float.parseFloat(PrefUtils.getStringValue(R.string.DETAIL_TEXT_SCALE, R.string.detail_text_scale_default));

          final long startTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          final long endTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          final ImageButton handleReminder = mLayout.findViewById(R.id.detail_handle_reminder);

          if (startTime <= System.currentTimeMillis()) {
            handleReminder.setVisibility(View.GONE);
          } else {
            final AtomicBoolean userRemind = new AtomicBoolean(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER)) == 1);
            final AtomicBoolean favoriteRemind = new AtomicBoolean(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) == 1);

            if (userRemind.get() || favoriteRemind.get()) {
              handleReminder.setImageResource(R.drawable.ic_action_remove_alarm);
              CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_remove));
            } else {
              handleReminder.setImageResource(R.drawable.ic_action_add_alarm);
              CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_add));
            }

            handleReminder.setOnClickListener(v -> {
              ContentValues values = new ContentValues();
              boolean add = true;

              if (userRemind.get() || favoriteRemind.get()) {
                add = false;
                values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, false);
                values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, false);

              } else {
                values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, true);
              }

              if (context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), values, null, null) == 1) {
                if (add) {
                  userRemind.set(true);

                  ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context);
                  ProgramUtils.addReminderId(context, id);

                  mHandler.post(() -> {
                    handleReminder.setImageResource(R.drawable.ic_action_remove_alarm);
                    CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_remove));
                    handleReminder.invalidate();
                  });
                } else {
                  userRemind.set(false);
                  favoriteRemind.set(false);

                  IOUtils.removeReminder(context, id);
                  ProgramUtils.removeReminderId(context, id);
                  ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context);

                  mHandler.post(() -> {
                    handleReminder.setImageResource(R.drawable.ic_action_add_alarm);
                    CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_add));
                    handleReminder.invalidate();
                  });
                }

                Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
                intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, id);

                LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);

                UiUtils.updateImportantProgramsWidget(context);
                UiUtils.updateRunningProgramsWidget(context);
              }
            });
          }

          final int favoriteMatchHighlightColor = PrefUtils.getIntValue(R.string.PREF_DETAIL_HIGHLIGHT_COLOR, ContextCompat.getColor(context, R.color.pref_detail_highlight_color_default));

          final Favorite[] markedFromFavorites = Favorite.getFavoritesForUniqueId(context, id);
          final ArrayList<FavoriteTypePattern> patternList = new ArrayList<>();
          final BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(favoriteMatchHighlightColor);

          if (markedFromFavorites.length > 0) {
            for (Favorite favorite : markedFromFavorites) {
              if (favorite.getType() != Favorite.RESTRICTION_RULES_TYPE) {
                patternList.add(new FavoriteTypePattern(favorite.getType(), Pattern.compile(".*?(" + favorite.getSearchValue().replace("%", ".*?") + ").*?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
              }
            }
          }

          TextView date = mLayout.findViewById(R.id.detail_date_channel);
          TextView title = mLayout.findViewById(R.id.detail_title);
          TextView genre = mLayout.findViewById(R.id.detail_genre);
          TextView info = mLayout.findViewById(R.id.detail_info);
          TextView episode = mLayout.findViewById(R.id.detail_episode_title);
          TextView shortDescription = mLayout.findViewById(R.id.detail_short_description);
          TextView description = mLayout.findViewById(R.id.detail_description);
          TextView link = mLayout.findViewById(R.id.detail_link);
          TextView pictureDescription = mLayout.findViewById(R.id.detail_picture_description);
          TextView pictureCopyright = mLayout.findViewById(R.id.detail_picture_copyright);

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

          final Resources resources = context.getResources();

          if (!PrefUtils.isDarkTheme() && !(finish.get() instanceof InfoActivity)) {
            date.setTextColor(ContextCompat.getColor(context, R.color.detail_date_channel_color_light));
          }

          final int channelID = c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));

          Cursor channel = null;

          try {
            channel = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[]{TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, null, null, null);

            if (IOUtils.prepareAccessFirst(channel)) {
              final StringBuilder channelName = new StringBuilder();
              if (PrefUtils.getBooleanValue(R.string.SHOW_SORT_NUMBER_IN_DETAILS, R.bool.show_sort_number_in_details_default)) {
                final int columnIndex = channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
                if (!channel.isNull(columnIndex)) {
                  channelName.append(channel.getString(columnIndex)).append(". ");
                }
              }
              channelName.append(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));

              final Date start = new Date(startTime);
              final java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault());
              final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
              date.setText(resources.getString(R.string.detail_date_format, start, dateFormat.format(start), timeFormat.format(start), timeFormat.format(new Date(endTime)), channelName));

              Bitmap logo = UiUtils.createBitmapFromByteArray(channel.getBlob(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));

              if (logo != null) {
                float scale = resources.getDisplayMetrics().density;

                int width = (int) (logo.getWidth() * scale);
                int height = (int) (logo.getHeight() * scale);

                BitmapDrawable l = new BitmapDrawable(resources, logo);

                int color = PrefUtils.getIntValue(R.string.PREF_LOGO_BACKGROUND_COLOR, ContextCompat.getColor(context, R.color.pref_logo_background_color_default));

                GradientDrawable background = new GradientDrawable(Orientation.BOTTOM_TOP, new int[]{color, color});

                int add = 2;

                if (PrefUtils.getBooleanValue(R.string.PREF_LOGO_BORDER, R.bool.pref_logo_border_default)) {
                  add = 3;
                  background.setStroke(1, PrefUtils.getIntValue(R.string.PREF_LOGO_BORDER_COLOR, ContextCompat.getColor(context, R.color.pref_logo_border_color_default)));
                }

                background.setBounds(0, 0, width + add, height + add);

                LayerDrawable logoDrawable = new LayerDrawable(new Drawable[]{background, l});
                logoDrawable.setBounds(0, 0, width + add, height + add);

                l.setBounds(2, 2, width, height);

                date.setCompoundDrawables(logoDrawable, null, null, null);
              }
            }
          } finally {
            IOUtils.close(channel);
          }

          final StringBuilder year = new StringBuilder();

          int yearInt = -1;
          int yearLast = -1;
          int yearFirst = -1;
          int originLength = 0;

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN))) {
            year.append(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN)));
            originLength = year.length();
          }

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR))) {
            yearInt = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR));
          }

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST))) {
            yearFirst = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST));
          }

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR))) {
            yearLast = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR));
          }

          if (yearLast < yearInt) {
            yearLast = -1;
          }

          if (yearInt != -1 && ((yearLast != -1 && yearFirst == -1) || (yearLast == -1 && yearFirst < yearInt && yearFirst != -1))) {
            yearFirst = yearInt;
          }

          if (yearInt != -1) {
            if (year.length() >= 0) {
              year.append(" ");
            }

            year.append(yearInt);
          }

          if (yearLast != -1 && yearLast != yearInt) {
            if (yearFirst != -1) {
              if (year.length() >= 0) {
                if (yearFirst == yearInt) {
                  year.append(" \u2013 ");
                } else {
                  year.append(" (");
                }
              }

              if (yearFirst == yearInt) {
                year.append(yearLast);
              } else {
                year.append(yearFirst).append(" \u2013 ").append(yearLast).append(")");
              }
            } else {
              year.append(yearLast);
            }
          } else if (yearFirst != -1 && yearFirst != yearInt) {
            if (yearFirst < yearInt) {
              year.insert(originLength, " \u2013 ");
              year.insert(originLength, yearFirst);

              if (originLength != 0) {
                year.insert(originLength, " ");
              }
            } else if (yearInt == -1) {
              if (originLength > 0) {
                year.append(" ");
              }

              year.append(yearFirst);
            }
          }

          String originalTitle = null;
          String titleTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL))) {
            originalTitle = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL));
          }

          if (originalTitle == null || originalTitle.equals(titleTest)) {
            checkAndAddHiglightingForFavorites(title, titleTest, patternList, true, backgroundColorSpan);
            //title.setText(titleTest);
          } else {
            checkAndAddHiglightingForFavorites(title, titleTest + "/" + originalTitle, patternList, true, backgroundColorSpan);
            //title.setText(titleTest + "/" + originalTitle);
          }

          if (!PrefUtils.getBooleanValue(R.string.SHOW_PICTURE_IN_DETAILS, R.bool.show_picture_in_details_default) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT))) {
            pictureCopyright.setVisibility(View.GONE);
            pictureDescription.setVisibility(View.GONE);
          } else {
            if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION))) {
              pictureDescription.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION)));
            }

            pictureCopyright.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));

            Bitmap image = UiUtils.createBitmapFromByteArray(c.getBlob(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)));

            if (image != null) {
              BitmapDrawable b = new BitmapDrawable(resources, image);

              float zoom = Float.parseFloat(PrefUtils.getStringValue(R.string.DETAIL_PICTURE_ZOOM, R.string.detail_picture_zoom_default)) * resources.getDisplayMetrics().density;

              b.setBounds(0, 0, (int) (image.getWidth() * zoom), (int) (image.getHeight() * zoom));

              if (PrefUtils.getStringValue(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION, R.string.detail_picture_description_position_default).equals("0")) {
                pictureDescription.setCompoundDrawables(b, null, null, null);
              } else {
                pictureDescription.setCompoundDrawables(null, b, null, null);
              }
            }
          }

          if (PrefUtils.getBooleanValue(R.string.SHOW_GENRE_IN_DETAILS, R.bool.show_genre_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
            checkAndAddHiglightingForFavorites(genre, c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) + (year.length() > 0 ? " - " + year : ""), patternList, false, backgroundColorSpan);
//                    genre.setText();
          } else if (year.length() > 0) {
            checkAndAddHiglightingForFavorites(genre, year.toString(), patternList, false, backgroundColorSpan);
            //genre.setText(year);
          } else {
            genre.setVisibility(View.GONE);
          }

          Spannable infoValue = IOUtils.getInfoString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)), resources);

          if (PrefUtils.getBooleanValue(R.string.SHOW_INFO_IN_DETAILS, R.bool.show_info_in_details_default) && infoValue != null) {
            info.setText(infoValue);
          } else {
            info.setVisibility(View.GONE);
          }

          String number = "";

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) {
            if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT))) {
              number = IOUtils.decodeSingleFieldValueToMultipleEpisodeString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) + "/" + c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT)) + " ";
            } else {
              number = IOUtils.decodeSingleFieldValueToMultipleEpisodeString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) + " ";
            }
          }

          String originalEpisode = null;

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL))) {
            originalEpisode = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL));
          }

          if (PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
            String episodeTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));

            if (originalEpisode == null || episodeTest.equals(originalEpisode)) {
              checkAndAddHiglightingForFavorites(episode, number + episodeTest, patternList, false, backgroundColorSpan);
              //episode.setText(number + episodeTest);
            } else {
              checkAndAddHiglightingForFavorites(episode, number + episodeTest + "/" + originalEpisode, patternList, false, backgroundColorSpan);
              //episode.setText(number + episodeTest + "/" + originalEpisode);
            }
          } else if (PrefUtils.getBooleanValue(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && number.trim().length() > 0) {
            checkAndAddHiglightingForFavorites(episode, number, patternList, false, backgroundColorSpan);
            //episode.setText(number);
          } else {
            episode.setVisibility(View.GONE);
          }

          String shortDescriptionValue = null;
          String descriptionValue = null;
          boolean showShortDescription = true;

          boolean showEpgPaidInfo = PrefUtils.getBooleanValue(R.string.PREF_EPGPAID_DESCRIPTION_MISSING_INFO, R.bool.pref_epgpaid_description_missing_default);
          long epgPaidUntil = PrefUtils.getLongValueWithDefaultKey(R.string.PREF_EPGPAID_ACCESS_UNTIL,R.integer.pref_epgpaid_access_until_default);

          final boolean wasActive = epgPaidUntil < System.currentTimeMillis() && epgPaidUntil != context.getResources().getInteger(R.integer.pref_epgpaid_access_until_default);

          if (showEpgPaidInfo) {
            final Set<String> channelIds = PrefUtils.getStringSetValue(R.string.PREF_EPGPAID_DATABASE_CHANNEL_IDS, new HashSet<>());

            showEpgPaidInfo = channelIds.contains(String.valueOf(channelID));

            if (showEpgPaidInfo && !wasActive) {
              final File epgPaidDir = new File(IOUtils.getDownloadDirectory(context), "epgPaidData");

              if (epgPaidDir.isDirectory()) {
                final File[] test = epgPaidDir.listFiles();

                showEpgPaidInfo = (test.length <= 2);
              }
            }
          }

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
            shortDescriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)).trim();
          }

          if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION))) {
            descriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));

            if (shortDescriptionValue != null) {
              String test = shortDescriptionValue;

              if (test.endsWith("...")) {
                test = test.substring(0, test.length() - 3);
              } else if (test.endsWith("\u2026")) {
                test = test.substring(0, test.length() - 1);
              }

              test = test.replaceAll("\\s+", " ").trim();

              showShortDescription = !descriptionValue.replaceAll("\\s+", " ").trim().startsWith(test);

              if (showShortDescription) {
                shortDescriptionValue = shortDescriptionValue.trim() + "\n";
              }

              String[] paragraphs = descriptionValue.split("\n+");

              if (paragraphs.length > 1) {
                String para0 = paragraphs[0].replaceAll("\\s+", " ").trim();
                String para1 = paragraphs[1].replaceAll("\\s+", " ").trim();

                if (para1.startsWith(para0) || para0.startsWith(para1)) {
                  descriptionValue = descriptionValue.substring(paragraphs[0].length() + 1).trim();
                } else {
                  int testIndex = para0.indexOf(".");

                  if (testIndex > 0) {
                    para0 = para0.substring(testIndex + 1).trim();

                    if (!para0.isEmpty() && para1.endsWith(para0)) {
                      descriptionValue = descriptionValue.substring(paragraphs[0].length() + 1).trim();
                    }
                  }
                }
              }
            }
          }

          if (showEpgPaidInfo) {
            if(shortDescriptionValue == null && descriptionValue == null) {
              if(wasActive) {
                descriptionValue = context.getString(R.string.EPGpaidData_info_expiredAccess).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
              }
              else {
                descriptionValue = context.getString(R.string.detail_missing_description_text);
              }
            }
            else if(wasActive) {
              if((shortDescriptionValue != null && shortDescriptionValue.contains("omdb.org")) ||
                (descriptionValue != null && descriptionValue.contains("omdb.org"))) {
                descriptionValue += context.getString(R.string.EPGpaidData_info_expiredAccessExtend).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
              }
              else if((shortDescriptionValue != null && shortDescriptionValue.contains("wiki.tvbrowser.org/index.php/WirSchauen")) ||
                (descriptionValue != null && descriptionValue.contains("wiki.tvbrowser.org/index.php/WirSchauen"))) {
                descriptionValue = context.getString(R.string.EPGpaidData_info_expiredAccess).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
              }
            }
          }

          if (shortDescriptionValue == null || !showShortDescription) {
            shortDescription.setVisibility(View.GONE);
          } else {
            checkAndAddHiglightingForFavorites(shortDescription, shortDescriptionValue.replaceAll("\\s{4,}", "\n\n").replaceAll("\\r", "").replaceAll("\\n{2,}", "\n\n"), patternList, false, backgroundColorSpan);
          }

          if (descriptionValue != null) {
            checkAndAddHiglightingForFavorites(description, descriptionValue.replaceAll("\\s{4,}", "\n\n").replaceAll("\\r", "").replaceAll("\\n{2,}", "\n\n"), patternList, false, backgroundColorSpan);
          } else {
            description.setVisibility(View.GONE);
          }

          if (PrefUtils.getBooleanValue(R.string.SHOW_LINK_IN_DETAILS, R.bool.show_link_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK))) {
            String linkText = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK));
            link.setText(linkText);
            link.setMovementMethod(LinkMovementMethod.getInstance());
          } else {
            link.setVisibility(View.GONE);
          }

          Set<String> keys = VALUE_MAP.keySet();

          for (String key : keys) {
            boolean enabled = pref.getBoolean("details_" + key, true);
            TextView textView = mLayout.findViewById(VALUE_MAP.get(key));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() * textScale);

            if (textView != null && enabled && !c.isNull(c.getColumnIndex(key))) {
              String text = c.getString(c.getColumnIndex(key));

              if (key.equals(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO) && text.equals(descriptionValue)) {
                text = "";
              }

              if (text.trim().length() > 0) {
                try {
                  String name = resources.getString((Integer) R.string.class.getField(key).get(null));

                  boolean endWith = false;

                  if (name.endsWith(":")) {
                    endWith = true;
                  }

                  if (key.equals(TvBrowserContentProvider.DATA_KEY_ACTORS)) {
                    String separator = null;

                    if (text.contains("\n")) {
                      separator = "\n+";
                    } else if (text.contains(",")) {
                      separator = ",\\s*";
                    }

                    SpannableStringBuilder actors = new SpannableStringBuilder(name);
                    actors.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    actors.setSpan(new UnderlineSpan(), 0, name.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    int screenSizeHalf = getScreenSize(context).x / 2;

                    if (separator != null) {
                      String[] lines = text.split(separator);

                      float maxLength = 0;
                      TextPaint actorPaint = textView.getPaint();

                      for (int i = 0; i < lines.length; i++) {
                        if (lines[i].contains("\t\t-\t\t")) {
                          separator = "\t\t-\t\t";
                        } else if (lines[i].contains("(")) {
                          separator = "\\s*\\(";
                        } else {
                          separator = null;
                        }

                        if (separator != null) {
                          String[] parts = lines[i].split(separator);

                          if (parts.length > 1) {
                            float test = actorPaint.measureText(parts[0]);

                            if (test > screenSizeHalf) {
                              String secondSeparator = null;

                              if (parts[0].contains("(")) {
                                secondSeparator = "(";
                              } else if (parts[0].contains(" ")) {
                                secondSeparator = " ";
                              } else if (parts[0].contains("-")) {
                                secondSeparator = "-";
                              }

                              if (secondSeparator != null) {
                                int index = parts[0].indexOf(secondSeparator);

                                String firstPart = parts[0].substring(0, index).trim();
                                String secondPart = parts[0].substring(index).trim();

                                float firstLength = actorPaint.measureText(firstPart);
                                float secondLength = actorPaint.measureText(secondPart);

                                if (secondLength > screenSizeHalf) {
                                  int whiteSpaceIndex = secondPart.indexOf(" ");

                                  if (whiteSpaceIndex >= 0) {
                                    String thirdPart = secondPart.substring(0, whiteSpaceIndex).trim();
                                    String fifthPart = secondPart.substring(whiteSpaceIndex).trim();

                                    secondLength = Math.max(actorPaint.measureText(thirdPart), actorPaint.measureText(fifthPart));

                                    if (secondLength < screenSizeHalf) {
                                      secondPart = thirdPart + "\n" + fifthPart;
                                    }
                                  }
                                }

                                test = Math.max(firstLength, secondLength);

                                if (test < screenSizeHalf) {
                                  lines[i] = lines[i].replace(parts[0], firstPart + "\n" + secondPart);
                                }
                              }
                            }

                            maxLength = Math.max(maxLength, test);
                          }
                        }
                      }

                      if (maxLength > 0 && maxLength <= screenSizeHalf) {
                        for (int i = 0; i < lines.length; i++) {
                          String[] parts = lines[i].split(separator);

                          if (i > 0) {
                            actors.append("\n");
                          }

                          if (parts.length > 1) {
                            if (parts[1].trim().endsWith(")") || parts[1].trim().endsWith(",")) {
                              parts[1] = parts[1].substring(0, parts[1].length() - 1);
                            }

                            if (parts[0].contains("\n")) {
                              if (parts[0].indexOf("\n") != parts[0].lastIndexOf("\n")) {
                                parts[1] += "\n\n";
                              } else {
                                parts[1] += "\n";
                              }
                            }

                            actors.append(parts[1]);

                            actors.setSpan(new ActorColumnSpan(parts[0], (int) maxLength + 10, patternList, favoriteMatchHighlightColor), actors.length() - parts[1].length(), actors.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            mHasSpannableActors = true;
                          } else {
                            actors.append(lines[i]);
                          }
                        }
                      } else {
                        actors.append(text);
                      }
                    } else {
                      actors.append(text);
                    }

                    checkAndAddHiglightingForFavorites(textView, actors, patternList, false, backgroundColorSpan);
                    //textView.setText(actors);
                  } else {
                    if (key.equals(TvBrowserContentProvider.DATA_KEY_VPS)) {
                      Calendar temp = Calendar.getInstance();
                      temp.set(Calendar.HOUR_OF_DAY, Integer.parseInt(text) / 60);
                      temp.set(Calendar.MINUTE, Integer.parseInt(text) % 60);

                      text = DateFormat.getTimeFormat(context).format(temp.getTime());
                    }

                    text = text.replace("\n", "<br>");

                    name = "<b><u>" + name.replace("\n", "<br>") + "</u></b>" + (endWith ? " " : "");

                    checkAndAddHiglightingForFavorites(textView, CompatUtils.fromHtml(name + text), patternList, false, backgroundColorSpan);
                  }
                } catch (Exception e) {
                  textView.setVisibility(View.GONE);
                }
              } else {
                textView.setVisibility(View.GONE);
              }
            } else if (textView != null) {
              textView.setVisibility(View.GONE);
            }
          }

          final String[] projection = {TvBrowserContentProvider.KEY_ID};

          Cursor prev = null;
          try {
            prev = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + startTime + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + channelID + " AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " DESC LIMIT 1");
            if (IOUtils.prepareAccessFirst(prev)) {
              mPreviousId = prev.getLong(prev.getColumnIndex(TvBrowserContentProvider.KEY_ID));
            }
          } finally {
            IOUtils.close(prev);
          }

          Cursor next = null;
          try {
            next = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + startTime + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + channelID + " AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC LIMIT 1");
            if (IOUtils.prepareAccessFirst(next)) {
              mNextId = next.getLong(next.getColumnIndex(TvBrowserContentProvider.KEY_ID));
            }
          } finally {
            IOUtils.close(next);
          }
        }
      } finally {
        IOUtils.close(c);
      }

      return result;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onPostExecute(Boolean result) {
      mHandler.removeCallbacks(mShowWaiting);
      mShowWaitingDialog = false;

      if (mProgress != null && mProgress.isShowing()) {
        try {
          mProgress.dismiss();
        } catch (IllegalArgumentException ignored) {
        }
      }

      if (result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if (finish.get() != null) {
          builder.setOnCancelListener(dialog -> finish.get().finish());
        }

        builder.setView(mLayout);


        final AlertDialog test = builder.create();

        /* Stupid hack for wrong measured dialog size */
        if (mHasSpannableActors && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          final OnPreDrawListener preDrawListener = new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
              mLayout.getViewTreeObserver().removeOnPreDrawListener(this);

              int scrollHeight = mLayout.findViewById(R.id.test_test_test).getMeasuredHeight();

              if (scrollHeight < mLayout.getMeasuredHeight()) {
                final Window w = test.getWindow();

                if(w != null) {
                  w.setLayout(mLayout.getMeasuredWidth() + 40, scrollHeight + 100);
                }
              }

              return true;
            }
          };
          mLayout.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        }

        if (mLayout instanceof SwipeScrollView) {
          mLayout.setOnTouchListener((v, event) -> {
            boolean result1 = false;

            if (((SwipeScrollView) mLayout).isSwipeLeft()) {
              ((SwipeScrollView) mLayout).resetSwipe();

              if (mNextId != -1) {
                test.dismiss();
                UiUtils.showProgramInfo(context, mNextId, finish.get(), parent, handler);
                result1 = true;
              }
            } else if (((SwipeScrollView) mLayout).isSwipeRight()) {
              ((SwipeScrollView) mLayout).resetSwipe();

              if (mPreviousId != -1) {
                test.dismiss();
                UiUtils.showProgramInfo(context, mPreviousId, finish.get(), parent, handler);
                result1 = true;
              }
            }

            return result1;
          });
        }

        try {
          test.show();
        } catch (BadTokenException ignored) {
        }
      }

      new Thread("SHOW PROGRAM INFO ALLOWING THREAD") {
        @Override
        public void run() {
          try {
            sleep(500);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          mShowingProgramInfo = false;
        }
      }.start();
    }
  }
}