package org.tvbrowser.filter;

public interface ChannelFilter {
  int[] getFilteredChannelIds();
  String getName();
  void setFilterValues(String name, int[] filteredChannelIds);
}
