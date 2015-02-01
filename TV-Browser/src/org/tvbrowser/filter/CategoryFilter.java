package org.tvbrowser.filter;

public interface CategoryFilter {
  public int[] getCategoriyIndicies();
  public String getName();
  public String getOperation();
  public void setFilterValues(String name, String operation, int[] categoryIndicies);
}
