package org.tvbrowser.filter;

import java.util.Comparator;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.WhereClause;
import org.tvbrowser.utils.PrefUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.ViewGroup;

public abstract class FilterValues {
  public static final String SEPARATOR_CLASS = "§§_§§";
  private static final String SEPARATOR = "##_##";
  
  private String mId;
  protected String mName;
  
  public FilterValues(String name) {
    mId = String.valueOf(System.currentTimeMillis());
    mName = name;
  }
    
  public static final Comparator<FilterValues> COMPARATOR_FILTER_VALUES = new Comparator<FilterValues>() {
    @Override
    public int compare(FilterValues lhs, FilterValues rhs) {
      Log.d("info2", "lhs " + lhs + " rhs " + rhs);
      
      return lhs.toString().compareToIgnoreCase(rhs.toString());
    }
  };
  
  public final String getId() {
    return getClass().getCanonicalName() + SEPARATOR_CLASS + mId;
  }
  
  public final String getName() {
    return mName;
  }
  
  public abstract WhereClause getWhereClause(Context context);
  protected abstract String getSaveString();
  public abstract void edit(Context context, Runnable callback, ViewGroup parent);
  
  @Override
  public String toString() {
    return getName();
  }
  
  public static final FilterValues load(String id, String values) {
    FilterValues result = null;
    Log.d("info2", "load id " + id + " values " + values + " " + id.contains(SEPARATOR_CLASS));
    if(id.contains(SEPARATOR_CLASS) && values != null) {
      String[] parts = id.split(SEPARATOR_CLASS);
      String[] valueParts = values.split(SEPARATOR);
      Log.d("info2", "parts[0] " + parts[0]);
      if(parts[0].equals(FilterValuesCategories.class.getCanonicalName())) {
        result = new FilterValuesCategories(valueParts[0], valueParts[1]);
        result.mId = parts[1];
      }
      else if(parts[0].equals(FilterValuesChannels.class.getCanonicalName())) {        
        result = new FilterValuesChannels(valueParts[0], valueParts[1]);
        result.mId = parts[1];
      }
    }
    else if(values != null) {
      String[] valueParts = values.split(SEPARATOR);
      
      result = new FilterValuesChannels(valueParts[0], valueParts[1]);
      result.mId = id;
    }
    
    return result;
  }
  
  public static final FilterValues load(String id, Context context) {
    FilterValues result = null;

    SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, context);
    
    String values = pref.getString(id, null);
    
    if(id.equals(SettingConstants.ALL_FILTER_ID)) {
      result = new FilterValuesChannels(context.getString(R.string.activity_edit_filter_list_text_all),new int[0]);
      result.mId = SettingConstants.ALL_FILTER_ID;
    }
    else {
      result = load(id, values);
    }
    
    return result;
  }
  
  public final void save(Context context) {
    Editor edit = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, context).edit();
    edit.putString(getId(), mName + SEPARATOR + getSaveString());
    edit.commit();
  }
  
  public static final void deleteFilter(Context context, FilterValues filter) {
    Editor edit = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, context).edit();
    
    String filterId = filter.getClass().getCanonicalName() + SEPARATOR_CLASS + filter.getId();
    
    edit.remove(filterId);
    edit.remove(filter.getId());
    edit.commit();
    
    SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context);
    
    String test = pref.getString(context.getString(R.string.CURRENT_FILTER_ID), SettingConstants.ALL_FILTER_ID);
    
    if(test.equals(filter.getId())) {
      edit = pref.edit();
      edit.putString(context.getString(R.string.CURRENT_FILTER_ID), SettingConstants.ALL_FILTER_ID);
      edit.commit();
    }
  }
}
