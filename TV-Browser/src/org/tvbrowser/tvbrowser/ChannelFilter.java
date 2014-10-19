package org.tvbrowser.tvbrowser;

public interface ChannelFilter {
  public int[] getFilteredChannelIds();
  public void setFilteredChannels(int[] filteredChannelIds);
}
