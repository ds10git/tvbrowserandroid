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
  private int[] mColumnIndices;
  private String mOperation;
  
  public FilterValuesCategories() {
    this("",new int[0],"AND");
  }
  
  public FilterValuesCategories(String name, int[] columnIndices, String operation) {
    super(name);
    mColumnIndices = columnIndices;
    mOperation = operation;
  }
    
  FilterValuesCategories(String name, String values) {
    super(name);
    String[] parts = values.split(";");
    
    mOperation = parts[0].toUpperCase();
    
    mColumnIndices = new int[parts.length-1];
    
    for(int i = 1; i < parts.length; i++) {
      mColumnIndices[i-1] = Integer.parseInt(parts[i]);
    }
  }
  
  public WhereClause getWhereClause(Context context) {
    StringBuilder whereClause = new StringBuilder();
    
    if(mColumnIndices.length > 0) {
      whereClause.append(" AND ( ");
      
      String[] columnNames = TvBrowserContentProvider.INFO_CATEGORIES_COLUMNS_ARRAY;
      
      for(int i = 0; i < mColumnIndices.length-1; i++) {
        whereClause.append(columnNames[mColumnIndices[i]]).append(" ").append(mOperation).append(" ");
      }
      
      whereClause.append(columnNames[mColumnIndices[mColumnIndices.length-1]]).append(" ) ");
    }
    
    return new WhereClause(whereClause.toString(),null);
  }
    
  @Override
  protected String getSaveString() {
    StringBuilder save = new StringBuilder();
    
    save.append(mOperation).append(";");
    
    for(int i = 0; i < mColumnIndices.length-1; i++) {
      save.append(mColumnIndices[i]).append(";");
    }
    
    if(mColumnIndices.length > 0) {
      save.append(mColumnIndices[mColumnIndices.length-1]);
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
  public int[] getCategoryIndices() {
    return mColumnIndices;
  }

  @Override
  public String getOperation() {
    return mOperation;
  }

  @Override
  public void setFilterValues(String name, String operation, int[] categoryIndices) {
    if(name != null && categoryIndices != null && operation != null) {
      mName = name;
      mOperation = operation;
      mColumnIndices = categoryIndices;
      
      if(mCallback != null) {
        mCallback.run();
      }
      
      mCallback = null;
    }
  }
}
