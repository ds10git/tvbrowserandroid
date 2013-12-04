package org.tvbrowser.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public abstract class ProgramTableLayout extends ViewGroup {

  public ProgramTableLayout(Context context) {
    super(context);
  }

  public void clear() {
    for(int i = getChildCount()-1; i >= 0; i--) {
      View view = getChildAt(i);
      removeView(view);
      ((ProgramPanel)view).clear();
      view = null;
    }
  }
}
