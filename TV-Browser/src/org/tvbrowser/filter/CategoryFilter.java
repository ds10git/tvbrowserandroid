package org.tvbrowser.filter;

public interface CategoryFilter {
  int[] getCategoriyIndicies();
  String getName();
  String getOperation();
  void setFilterValues(String name, String operation, int[] categoryIndicies);
}
