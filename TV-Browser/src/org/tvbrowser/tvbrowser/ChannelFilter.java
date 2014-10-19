package org.tvbrowser.tvbrowser;

public interface ChannelFilter {
  public int[] getFilteredChannelIds();
  public String getName();
  public void setFilterValues(String name, int[] filteredChannelIds);
}
