/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software free of charge without modifications and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.view;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public abstract class ProgramTableLayout extends ViewGroup {
  private ArrayList<Integer> mChannelIDsOrdered;
  
  ProgramTableLayout(Context context, ArrayList<Integer> channelIDsOrdered) {
    super(context);
    
    mChannelIDsOrdered = channelIDsOrdered;
  }
  
  final int getIndexForChannelID(int channelID) {
    for(int i = 0; i < mChannelIDsOrdered.size(); i++) {
      if(mChannelIDsOrdered.get(i) == channelID) {
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
  
  /*
   * Possible bug fix for bug:
   * https://code.google.com/p/android/issues/detail?id=55933
   */
  @Override
  public void addChildrenForAccessibility(ArrayList<View> childrenForAccessibility) {}
}
