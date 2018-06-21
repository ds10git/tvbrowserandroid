package org.tvbrowser.filter;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.tvbrowser.WhereClause;
import org.tvbrowser.utils.UiUtils;

import android.content.Context;
import android.view.ViewGroup;

/**
 * A class with values of a category filter.
 * <p>
 * @author René Mach
 */
public class FilterValuesCategories extends FilterValues implements CategoryFilter {
  private int[] mColumnIndicies;
  private String mOperation;
  
  public FilterValuesCategories() {
    this("",new int[0],"AND");
  }
  
  public FilterValuesCategories(String name, int[] columnIndicies, String operation) {
    super(name);
    mColumnIndicies = columnIndicies;
    mOperation = operation;
  }
    
  FilterValuesCategories(String name, String values) {
    super(name);
    String[] parts = values.split(";");
    
    mOperation = parts[0].toUpperCase();
    
    mColumnIndicies = new int[parts.length-1];
    
    for(int i = 1; i < parts.length; i++) {
      mColumnIndicies[i-1] = Integer.parseInt(parts[i]);
    }
  }
  
  public WhereClause getWhereClause(Context context) {
    StringBuilder whereClause = new StringBuilder();
    
    if(mColumnIndicies.length > 0) {
      whereClause.append(" AND ( ");
      
      String[] columnNames = TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY;
      
      for(int i = 0; i < mColumnIndicies.length-1; i++) {
        whereClause.append(columnNames[mColumnIndicies[i]]).append(" ").append(mOperation).append(" ");
      }
      
      whereClause.append(columnNames[mColumnIndicies[mColumnIndicies.length-1]]).append(" ) ");
    }
    
    return new WhereClause(whereClause.toString(),null);
  }
    
  @Override
  protected String getSaveString() {
    StringBuilder save = new StringBuilder();
    
    save.append(mOperation).append(";");
    
    for(int i = 0; i < mColumnIndicies.length-1; i++) {
      save.append(mColumnIndicies[i]).append(";");
    }
    
    if(mColumnIndicies.length > 0) {
      save.append(mColumnIndicies[mColumnIndicies.length-1]);
    }
    
    return save.toString();
  }

  private Runnable mCallback;
  
  @Override
  public void edit(Context context, Runnable callback, ViewGroup parent) {
    mCallback = callback;
    
    UiUtils.showCategorySelection(context, this, parent, null);
  }

  @Override
  public int[] getCategoriyIndicies() {
    return mColumnIndicies;
  }

  @Override
  public String getOperation() {
    return mOperation;
  }

  @Override
  public void setFilterValues(String name, String operation, int[] categoryIndicies) {
    if(name != null && categoryIndicies != null && operation != null) {
      mName = name;
      mOperation = operation;
      mColumnIndicies = categoryIndicies;
      
      if(mCallback != null) {
        mCallback.run();
      }
      
      mCallback = null;
    }
  }
}
