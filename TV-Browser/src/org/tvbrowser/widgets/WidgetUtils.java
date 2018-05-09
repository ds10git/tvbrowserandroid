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
package org.tvbrowser.widgets;

import org.tvbrowser.utils.UiUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

final class WidgetUtils {
  @SuppressLint("UseSparseArrays")
  public static Spannable getMarkings(Context context, Cursor cursor, boolean showMarkings, int pluginMarkingIndex, int favoriteMarkingIndex, int reminderMarkingIndex, int favoriteReminderMarkingIndex, int syncMarkingIndex) {
    SpannableStringBuilder markings = new SpannableStringBuilder();
    
    if(showMarkings) {
      if(pluginMarkingIndex != -1 && cursor.getInt(pluginMarkingIndex) == 1) {
        addMarkingInfo(context,markings,UiUtils.MARKED_COLOR_KEY);
      }
      if(cursor.getInt(favoriteMarkingIndex) >= 1) {
        addMarkingInfo(context,markings,UiUtils.MARKED_FAVORITE_COLOR_KEY);
      }
      if((reminderMarkingIndex != -1 && cursor.getInt(reminderMarkingIndex) == 1) || 
          (favoriteReminderMarkingIndex != -1 && cursor.getInt(favoriteReminderMarkingIndex) >= 1)) {
        addMarkingInfo(context,markings,UiUtils.MARKED_REMINDER_COLOR_KEY);
      }
      if(syncMarkingIndex != -1 && cursor.getInt(syncMarkingIndex) == 1) {
        addMarkingInfo(context,markings,UiUtils.MARKED_SYNC_COLOR_KEY);
      }
    }
    
    if(markings.length() > 0) {
      markings.insert(0, " ");
      
      return markings;
    }
        
    return null;
  }
  
  private static void addMarkingInfo(Context context, SpannableStringBuilder markings, int colorKey) {
    markings.append("\u2593\u2593");
    markings.setSpan(new ForegroundColorSpan(UiUtils.getColor(colorKey,context)), markings.length()-2, markings.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    markings.setSpan(new RelativeSizeSpan(0.85f), markings.length()-2, markings.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
  }
  
  public static CharSequence getColoredString(CharSequence original, int[] encodedColorValue) {
    if(encodedColorValue != null && encodedColorValue[0] == 1) {
      original = new SpannableString(original);
      ((SpannableString)original).setSpan(new ForegroundColorSpan(encodedColorValue[1]), 0, original.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    
    return original;
  }
}
