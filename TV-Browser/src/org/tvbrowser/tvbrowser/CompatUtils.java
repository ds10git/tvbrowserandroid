package org.tvbrowser.tvbrowser;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;

public final class CompatUtils {

  private CompatUtils() {}

  @SuppressWarnings("deprecation")
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  public static int getHeight(final Display display) {
    int result = 0;
    if (display!=null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
        final Point size = new Point();
        display.getSize(size);
        result = size.y;
      }
      else {
        result = display.getHeight();
      }
    }
    return result;
  }

  @SuppressWarnings("deprecation")
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public static void setBackground(final View view, final Drawable drawable) {
    if (view != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        view.setBackground(drawable);
      } else {
        view.setBackgroundDrawable(drawable);
      }
    }
  }
}