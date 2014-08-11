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
package org.tvbrowser.tvbrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class EditFavoriteActivity extends Activity {
  private Favorite mFavorite;
  private EditText mSearchValue;
  private EditText mName;
  private CheckBox mOnlyTitle;
  private CheckBox mRemind;
  private TextView mTime;
  private TextView mDays;
  private TextView mChannels;
  private EditText mExclusions;
  
  private int mCheckedCount;
  private Favorite mOldFavorite;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    
    if(SettingConstants.IS_DARK_THEME) {
      setTheme(android.R.style.Theme_Holo);
    }
    
    setContentView(R.layout.add_favorite_layout);
    
    mSearchValue = (EditText)findViewById(R.id.favorite_search_value);
    mName = (EditText)findViewById(R.id.favorite_name);
    mOnlyTitle = (CheckBox)findViewById(R.id.favorite_only_title);
    mRemind = (CheckBox)findViewById(R.id.favorite_remind);
    mTime = (TextView)findViewById(R.id.favorite_time_restriction);
    mDays = (TextView)findViewById(R.id.favorite_day_restriction);
    mChannels = (TextView)findViewById(R.id.favorite_channel_restriction);
    mExclusions = (EditText)findViewById(R.id.favorite_exclusion);
    
    int color = getResources().getColor(android.R.color.primary_text_light);
    
    if(SettingConstants.IS_DARK_THEME) {
      color = getResources().getColor(android.R.color.primary_text_dark);
    }
    
    mTime.setTextColor(color);
    mDays.setTextColor(color);
    mChannels.setTextColor(color);
    
    mSearchValue.requestFocusFromTouch();
    
    mFavorite = (Favorite)getIntent().getSerializableExtra(Favorite.FAVORITE_EXTRA);
    
    if(mFavorite != null) {
      mOldFavorite = mFavorite.copy();
      Log.d("info12", "OUT " + mFavorite.getName() + " " + mFavorite.getSearchValue() + " " + mFavorite.searchOnlyTitle() + " " + mFavorite.remind());
      mSearchValue.setText(mFavorite.getSearchValue());
      mName.setText(mFavorite.getName());
      mOnlyTitle.setChecked(mFavorite.searchOnlyTitle());
      mRemind.setChecked(mFavorite.remind());
      
      if(mFavorite.isHavingExclusions()) {
        mExclusions.setText(TextUtils.join(", ", mFavorite.getExclusions()));
      }
      Log.d("info12", "VALUES " + mName.getText() + " " + mSearchValue.getText() + " " + mOnlyTitle.isSelected() + " " + mRemind.isSelected());
    }
    else {
      mFavorite = new Favorite();
      
      String search = getIntent().getStringExtra(Favorite.SEARCH_EXTRA);
      
      if(search != null) {
        mFavorite.setSearchValue(search);
        mSearchValue.setText(search);
      }
    }
    
    final View ok = findViewById(R.id.favorite_ok);
    
    ok.setEnabled(mFavorite.getSearchValue() != null && mFavorite.getSearchValue().trim().length() > 0);
    
    mSearchValue.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      
      @Override
      public void afterTextChanged(Editable s) {
        ok.setEnabled(mSearchValue.getText().toString().trim().length() > 0);
      }
    });
    
    handleTimeView();
    handleDayView();
    handleChannelView();
  }
  
  public void changeTime(View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(EditFavoriteActivity.this);
    
    View timeSelection = getLayoutInflater().inflate(R.layout.favorite_time_selection, (ViewGroup)mSearchValue.getRootView(), false);
    
    final TimePicker from = (TimePicker)timeSelection.findViewById(R.id.favorite_time_selection_from);
    final TimePicker to = (TimePicker)timeSelection.findViewById(R.id.favorite_time_selection_to);
    
    from.setIs24HourView(DateFormat.is24HourFormat(EditFavoriteActivity.this));
    to.setIs24HourView(DateFormat.is24HourFormat(EditFavoriteActivity.this));
    
    if(mFavorite.isTimeRestricted()) {
      Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionStart() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionStart() % 60);
      
      Calendar current = Calendar.getInstance();
      current.setTime(utc.getTime());
      
      from.setCurrentHour(current.get(Calendar.HOUR_OF_DAY));
      from.setCurrentMinute(current.get(Calendar.MINUTE));
      
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionEnd() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionEnd() % 60);
      
      current.setTime(utc.getTime());
      
      to.setCurrentHour(current.get(Calendar.HOUR_OF_DAY));
      to.setCurrentMinute(current.get(Calendar.MINUTE));
    }
    else {
      from.setCurrentHour(0);
      from.setCurrentMinute(0);
      
      to.setCurrentHour(23);
      to.setCurrentMinute(59);
    }
    
    builder.setView(timeSelection);
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Calendar current = Calendar.getInstance();
        current.set(Calendar.HOUR_OF_DAY, from.getCurrentHour());
        current.set(Calendar.MINUTE, from.getCurrentMinute());
        
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTime(current.getTime());
        Log.d("info12", from.getCurrentHour() + " " + utc.get(Calendar.HOUR_OF_DAY) + " " + from.getCurrentMinute() + " " + utc.get(Calendar.MINUTE));
        
        int start = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE);
        
        current.set(Calendar.HOUR_OF_DAY, to.getCurrentHour());
        current.set(Calendar.MINUTE, to.getCurrentMinute());
        
        utc.setTime(current.getTime());
        
        int end = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE);
        
        if((start == end) || (start == 0 && end == 23*60 + 59)) {
          start = -1;
          end = -1;
        }
        
        mFavorite.setTimeRestrictionStart(start);
        mFavorite.setTimeRestrictionEnd(end);
        handleTimeView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  public void changeDays(View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(EditFavoriteActivity.this);
    
    final Calendar dayCal = Calendar.getInstance();
    final Locale locale = Locale.getDefault();
    
    String[] dayArray = new String[7];
    
    for(int day = Calendar.MONDAY; day <= Calendar.SATURDAY; day++) {
      dayCal.set(Calendar.DAY_OF_WEEK, day);
      dayArray[day-2] = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale);
    }
    
    dayCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    dayArray[6] = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale);
    
    final boolean[] checked = new boolean[7];
    mCheckedCount = 0;
    
    Arrays.fill(checked, false);
    
    if(mFavorite.isDayRestricted()) {
      for(int day : mFavorite.getDayRestriction()) {
        if(day == Calendar.SUNDAY) {
          checked[6] = true;
          mCheckedCount++;
        }
        else {
          checked[day-2] = true;
          mCheckedCount++;
        }
      }
    }
    Log.d("info12", "days " + dayArray.length + " " + checked.length + " " + mFavorite.isDayRestricted());
    
    for(int i = 0; i < dayArray.length; i++) {
      Log.d("info12", dayArray[i] + " " + checked[i]);
    }
    
    builder.setMultiChoiceItems(dayArray, checked, new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        checked[which] = isChecked;
        
        if(isChecked) {
          mCheckedCount++;
        }
        else {
          mCheckedCount--;
        }
      }
    });
    
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if(mCheckedCount < 7 && mCheckedCount > 0) {
          int[] days =  new int[mCheckedCount];
          
          int dayIndex = 0;
          
          ArrayList<String> dayView = new ArrayList<String>();
          
          for(int i = 0; i < checked.length; i++) {
            if(checked[i]) {
              if(i == 6) {
                days[dayIndex++] = Calendar.SUNDAY;
              }
              else {
                days[dayIndex++] = i+2;
              }
            }
          }
          
          mFavorite.setDayRestriction(days);
        }
        else {
          mFavorite.setDayRestriction(null);
        }
        
        handleDayView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  private void handleTimeView() {
    java.text.DateFormat timeFormat = DateFormat.getTimeFormat(EditFavoriteActivity.this);
    
    Date fromFormat = null;
    Date toFormat = null;
    
    if(mFavorite.isTimeRestricted()) {
      Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionStart() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionStart() % 60);
      
      fromFormat = utc.getTime();
      
      utc.set(Calendar.HOUR_OF_DAY, mFavorite.getTimeRestrictionEnd() / 60);
      utc.set(Calendar.MINUTE, mFavorite.getTimeRestrictionEnd() % 60);
      
      toFormat = utc.getTime();
    }
    else {
      Calendar now = Calendar.getInstance();
      now.set(Calendar.HOUR_OF_DAY, 0);
      now.set(Calendar.MINUTE, 0);
      
      fromFormat = now.getTime();
      
      now.set(Calendar.HOUR_OF_DAY, 23);
      now.set(Calendar.MINUTE, 59);
      
      toFormat = now.getTime();
    }
    
    StringBuilder timeString = new StringBuilder();
    Log.d("info12", fromFormat + " " + toFormat + " " + mFavorite.isTimeRestricted());
    timeString.append(timeFormat.format(fromFormat));
    timeString.append(" ");
    timeString.append(getString(R.string.favorite_time_to));
    timeString.append(" ");
    timeString.append(timeFormat.format(toFormat));
    
    mTime.setText(timeString.toString());
  }
  
  private void handleDayView() {
    if(!mFavorite.isDayRestricted()) {
      mDays.setText(R.string.favorite_days_default);
    }
    else {
      Locale locale = Locale.getDefault();
      
      Calendar dayNames = Calendar.getInstance();
      
      ArrayList<String> days = new ArrayList<String>();
      
      String sunday = null;
      
      for(int day : mFavorite.getDayRestriction()) {
        dayNames.set(Calendar.DAY_OF_WEEK, day);
        
        if(day == Calendar.SUNDAY) {
          sunday = dayNames.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale);
        }
        else {
          days.add(dayNames.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale));
        }
      }
      
      if(sunday != null) {
        days.add(sunday);
      }
      
      mDays.setText(TextUtils.join(" ", days));
    }
  }
  
  private void handleChannelView() {
    if(mFavorite.isChannelRestricted()) {
      String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME
      };
      
      int[] ids = mFavorite.getChannelRestrictionIDs();
      
      StringBuilder where = new StringBuilder();
      
      where.append(TvBrowserContentProvider.KEY_ID).append(" IN ( ");
      
      for(int i = 0; i < ids.length-1; i++) {
        where.append(ids[i]).append(", ");
      }
      
      where.append(ids[ids.length-1]).append(" ) ");
      
      Cursor channelNames = getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);
      
      channelNames.move(-1);
      
      ArrayList<String> nameList = new ArrayList<String>();
      
      int nameColumn = channelNames.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
      
      while(channelNames.moveToNext()) {
        nameList.add(channelNames.getString(nameColumn));
      }
      
      channelNames.close();
      
      mChannels.setText(TextUtils.join(", ", nameList));
    }
    else {
      mChannels.setText(R.string.favorite_channels_default);
    }
  }
  
  /**
   * View holder for custom cursor adapter of channel selection.
   * 
   * @author René Mach
   */
  private static final class ViewHolder {
    CheckBox mCheckBox;
    TextView mTextView;
    ImageView mLogo;
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
  
  private boolean isRestricted(int[] channelIDs, int id) {
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
  
  public void changeChannels(View view) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.CHANNEL_KEY_SELECTION,
        TvBrowserContentProvider.CHANNEL_KEY_LOGO,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER
        };
    
    ContentResolver cr = getContentResolver();
    final Cursor channels = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, projection, TvBrowserContentProvider.CHANNEL_KEY_SELECTION, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + ", " + TvBrowserContentProvider.CHANNEL_KEY_NAME);
    
    final int idColumn = channels.getColumnIndex(TvBrowserContentProvider.KEY_ID);
    final int logoColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO);
    final int nameColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME);
    final int orderNumberColumn = channels.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
    
    int[] channelRestriction = mFavorite.getChannelRestrictionIDs();
    
    channels.move(-1);
    
    // inflate channel selection view
    View channelSelectionView = getLayoutInflater().inflate(R.layout.channel_selection_list, null);
    channelSelectionView.findViewById(R.id.channel_country_label).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_country_value).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_category_label).setVisibility(View.GONE);
    channelSelectionView.findViewById(R.id.channel_category_value).setVisibility(View.GONE);
    
    final ListView list = (ListView)channelSelectionView.findViewById(R.id.channel_selection_list);
    
    final Bitmap defaultLogo = BitmapFactory.decodeResource( getResources(), R.drawable.ic_launcher);
    
    final ArrayAdapter<AdapterChannel> channelAdapter = new ArrayAdapter<EditFavoriteActivity.AdapterChannel>(EditFavoriteActivity.this, android.R.layout.simple_list_item_1) {
      public View getView(int position, View convertView, ViewGroup parent) {
        AdapterChannel value = getItem(position);
        ViewHolder holder = null;
        
        if (convertView == null) {
          LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
          
          holder = new ViewHolder();
          
          convertView = mInflater.inflate(R.layout.channel_row, null);
          
          holder.mTextView = (TextView)convertView.findViewById(R.id.row_of_channel_text);
          holder.mCheckBox = (CheckBox)convertView.findViewById(R.id.row_of_channel_selection);
          holder.mLogo = (ImageView)convertView.findViewById(R.id.row_of_channel_icon);
          
          convertView.setTag(holder);
          
        }
        else {
          holder = (ViewHolder)convertView.getTag();
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
        BitmapDrawable l = new BitmapDrawable(getResources(), channelLogo);
        
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
    
    AlertDialog.Builder builder = new AlertDialog.Builder(EditFavoriteActivity.this);
    
    builder.setView(channelSelectionView);
    
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
          mFavorite.setChannelRestrictionIDs(null);
        }
        else {
          int[] ids = new int[channelIDList.size()];
          
          for(int i = 0; i < ids.length; i++) {
            ids[i] = channelIDList.get(i);
          }
          
          mFavorite.setChannelRestrictionIDs(ids);
        }
        
        handleChannelView();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, null);
    
    builder.show();
  }
  
  public void cancel(View view) {
    finish();
  }
  
  public void ok(View view) {
    findViewById(R.id.favorite_ok).setEnabled(false);
    findViewById(R.id.favorite_cancel).setEnabled(false);

    mFavorite.setName(mName.getText().toString());
    mFavorite.setSearchValue(mSearchValue.getText().toString());
    mFavorite.setSearchOnlyTitle(mOnlyTitle.isChecked());
    mFavorite.setRemind(mRemind.isChecked());
    
    String exclusions = mExclusions.getText().toString();
    
    if(exclusions.trim().length() > 0) {
      if(exclusions.contains(",")) {
        mFavorite.setExclusions(exclusions.split(",\\s+"));
      }
      else {
        mFavorite.setExclusions(new String[] {exclusions.trim()});
      }
    }
    else {
      mFavorite.setExclusions(null);
    }
    
    if(mFavorite.getName().trim().length() == 0) {
      mFavorite.setName(mFavorite.getSearchValue());
    }
    
    final Intent intent = new Intent(SettingConstants.FAVORITES_CHANGED);
    
    if(mOldFavorite != null) {
      intent.putExtra(Favorite.OLD_NAME_KEY, mOldFavorite.getName());
    }
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(EditFavoriteActivity.this);
    
    Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
    HashSet<String> newFavoriteList = new HashSet<String>();
    
    boolean added = false;
    
    for(String favorite : favoritesSet) {
      String[] values = favorite.split(";;");
      
      if(mOldFavorite != null && values[0].equals(mOldFavorite.getName())) {                
        newFavoriteList.add(mFavorite.getSaveString());
        added = true;
      }
      else {
        newFavoriteList.add(favorite);
      }
    }
      
    if(!added) {
      newFavoriteList.add(mFavorite.getSaveString());
    }
    
    Editor edit = prefs.edit();
    edit.putStringSet(SettingConstants.FAVORITE_LIST, newFavoriteList);
    edit.commit();
    
    intent.putExtra(Favorite.FAVORITE_EXTRA, mFavorite);
    
    final Context context = getApplicationContext();
    final ContentResolver resolver = getContentResolver();
    
    new Thread() {
      @Override
      public void run() {
        if(mOldFavorite != null) {
          Favorite.removeFavoriteMarking(context, resolver, mOldFavorite);
        }
        
        Favorite.updateFavoriteMarking(context, resolver, mFavorite);
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
      }
    }.start();
        
    finish();
  }
}
