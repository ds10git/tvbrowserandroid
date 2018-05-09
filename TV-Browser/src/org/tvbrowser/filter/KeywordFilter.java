package org.tvbrowser.filter;

public interface KeywordFilter {
  String getColumn();
  String getKeyword();
  String getName();
  void setFilterValues(String name, String keyword, String column);
}
