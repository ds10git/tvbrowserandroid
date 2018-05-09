package org.tvbrowser.filter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
  
  FilterValues(String name) {
    mId = String.valueOf(System.currentTimeMillis());
    mName = name;
  }
    
  public static final Comparator<FilterValues> COMPARATOR_FILTER_VALUES = new Comparator<FilterValues>() {
    @Override
    public int compare(FilterValues lhs, FilterValues rhs) {
      return lhs.toString().compareToIgnoreCase(rhs.toString());
    }
  };
  
  public final String getId() {
    return getClass().getCanonicalName() + SEPARATOR_CLASS + mId;
  }
  
  public String getName() {
    return mName;
  }
  
  public abstract WhereClause getWhereClause(Context context);
  protected abstract String getSaveString();
  public abstract void edit(Context context, Runnable callback, ViewGroup parent);
  
  @Override
  public String toString() {
    return getName();
  }
  
  public static FilterValues load(String id, String values) {
    FilterValues result = null;

    try {
      if (id.contains(SEPARATOR_CLASS) && values != null) {
        String[] parts = id.split(SEPARATOR_CLASS);
        String[] valueParts = values.split(SEPARATOR);

        if (parts[0].equals(FilterValuesCategories.class.getCanonicalName())) {
          result = new FilterValuesCategories(valueParts[0], valueParts[1]);
          result.mId = parts[1];
        } else if (parts[0].equals(FilterValuesChannels.class.getCanonicalName())) {
          result = new FilterValuesChannels(valueParts[0], valueParts[1]);
          result.mId = parts[1];
        } else if (parts[0].equals(FilterValuesKeyword.class.getCanonicalName())) {
          result = new FilterValuesKeyword(valueParts[0], valueParts[1]);
          result.mId = parts[1];
        }
      } else if (values != null) {
        String[] valueParts = values.split(SEPARATOR);

        if (valueParts.length == 2) {
          result = new FilterValuesChannels(valueParts[0], valueParts[1]);
          result.mId = id;
        }
      }
    }catch(Exception ignored) {}
    
    return result;
  }
  
  public static FilterValues load(String id, Context context) {
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
  
  public static void deleteFilter(Context context, FilterValues filter) {
    final String filterId = filter.getClass().getCanonicalName() + SEPARATOR_CLASS + filter.getId();
    
    PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_FILTERS, context).edit().remove(filterId).remove(filter.getId()).commit();
    
    final SharedPreferences pref = PrefUtils.getSharedPreferences(PrefUtils.TYPE_PREFERENCES_SHARED_GLOBAL, context);
    
    final Set<String> test = pref.getStringSet(context.getString(R.string.CURRENT_FILTER_ID), new HashSet<>());
    
    final String[] idValues = test.toArray(new String[test.size()]);
    boolean removed = false;
    
    for(String id : idValues) {
      if(id.equals(filter.getId())) {
        test.remove(id);
        removed = true;
      }
    }
    
    if(removed) {
      pref.edit().putStringSet(context.getString(R.string.CURRENT_FILTER_ID), test).commit();
    }
  }
  
  @Override
  public int hashCode() {
    return Arrays.hashCode(getId().getBytes());
  }
  
  @Override
  public boolean equals(Object o) {
    Log.d("info4", this + " equals " + o);
    if(o instanceof FilterValues) {
      return getId().equals(((FilterValues) o).getId());
    }
    
    return super.equals(o);
  }
}
