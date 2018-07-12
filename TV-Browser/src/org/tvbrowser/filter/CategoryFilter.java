package org.tvbrowser.filter;

public interface CategoryFilter {
  int[] getCategoryIndices();
  String getName();
  String getOperation();
  void setFilterValues(String name, String operation, int[] categoryIndices);
}