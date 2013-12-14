package org.tvbrowser.view;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public abstract class ProgramTableLayout extends ViewGroup {
  private ArrayList<Integer> mChannelIDsOrdered;
  
  public ProgramTableLayout(Context context, ArrayList<Integer> channelIDsOrdered) {
    super(context);
    
    mChannelIDsOrdered = channelIDsOrdered;
  }
  
  final int getIndexForChannelID(int channelID) {
    for(int i = 0; i < mChannelIDsOrdered.size(); i++) {
      if(mChannelIDsOrdered.get(i).intValue() == channelID) {
        return i;
      }
    }
    
    return -1;
  }

  public void clear() {
    for(int i = getChildCount()-1; i >= 0; i--) {
      View view = getChildAt(i);
      removeView(view);
      ((ProgramPanel)view).clear();
      view = null;
    }
  }
  
  final int getColumnCount() {
    return mChannelIDsOrdered.size();
  }
}
