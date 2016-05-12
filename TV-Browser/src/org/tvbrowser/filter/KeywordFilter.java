package org.tvbrowser.filter;

public interface KeywordFilter {
  public String getColumn();
  public String getKeyword();
  public String getName();
  public void setFilterValues(String name, String keyword, String column);
}
