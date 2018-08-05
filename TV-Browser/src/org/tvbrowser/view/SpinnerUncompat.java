/*
 * TV-Browser for Android
 * Copyright (C) 2018 René Mach (rene@tvbrowser.org)
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Stupid workaround class for AppCompatSpinner issues on APIs since 21
 *
 * Prevents Spinner being replaced with AppCompatSpinner
 * in xml layout file, to use Spinner provided by API.
 */
@SuppressLint("AppCompatCustomView")
public class SpinnerUncompat extends Spinner {
  public SpinnerUncompat(Context context) {
    super(context);
  }

  public SpinnerUncompat(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SpinnerUncompat(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SpinnerUncompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public SpinnerUncompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, int mode) {
    super(context, attrs, defStyleAttr, defStyleRes, mode);
  }
}
