package org.tvbrowser.filter;

import org.tvbrowser.tvbrowser.WhereClause;
import org.tvbrowser.utils.UiUtils;

import android.content.Context;
import android.view.ViewGroup;

public class FilterValuesKeyword extends FilterValues implements KeywordFilter {
  private String mKeyword;
  private String mColumn;
  
  FilterValuesKeyword() {
    this("","");
  }
  
  FilterValuesKeyword(String name, String values) {
    super(name);
    
    final String[] parts = values.split(";");
    
    if(parts.length == 2) {
      mKeyword = parts[0];
      mColumn = parts[1];
    }
    else {
      mKeyword = "";
      mColumn = "";
    }
  }
  
  public FilterValuesKeyword(String name, String keyword, String column) {
    super(name);
    mKeyword = keyword;
    mColumn = column;
  }

  @Override
  public String getColumn() {
    return mColumn;
  }
  
  @Override
  public String getKeyword() {
    return mKeyword;
  }

  @Override
  public void setFilterValues(String name, String keyword, String column) {
    if(name != null && keyword != null && mColumn != null) {
      mName = name;
      mKeyword = keyword;
      mColumn = column;
      
      if(mCallback != null) {
        mCallback.run();
      }
      
      mCallback = null;
    }
  }

  @Override
  public WhereClause getWhereClause(Context context) {

    return new WhereClause(" " + " AND ( " + mColumn + " LIKE \"%" + mKeyword.replaceAll("\\s+OR\\s+", "%\" OR " + mColumn + " LIKE \"%").replaceAll("\\s+AND\\s+", "%\" AND " + mColumn + " LIKE \"%") + "%\" ) ", null);
  }

  @Override
  protected String getSaveString() {
    return mKeyword+";"+mColumn;
  }

  private Runnable mCallback; 
  
  @Override
  public void edit(Context context, Runnable callback, ViewGroup parent) {
    mCallback = callback;
    
    UiUtils.showKeywordSelection(context, this, parent, null);
  }
}
