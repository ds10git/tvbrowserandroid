/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.tvbrowser;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A class that contains filter information for title filtering of programs.
 * 
 * @author René Mach
 */
public class DontWantToSeeExclusion {
  private String mExclusion;
  private final Pattern mPrecompiledPattern;
  private final boolean mIsCaseSensitive;
  
  public DontWantToSeeExclusion(String exclusion) {
    String[] parts = exclusion.split(";;");
    
    mExclusion = parts[0];
    mIsCaseSensitive = parts[1].equals("1");
    
    if(!mIsCaseSensitive) {
      mExclusion = mExclusion.toLowerCase(Locale.getDefault());
    }
    
    if(parts[0].contains("*")) {
      String pattern = Pattern.quote(mExclusion);
      pattern = pattern.replace("*", "\\E.*\\Q");
      mPrecompiledPattern = Pattern.compile(pattern);
    }
    else {
      mPrecompiledPattern = null;
    }
  }
  
  public boolean matches(String title) {
    boolean matches;
    
    if(!mIsCaseSensitive) {
      title = title.toLowerCase(Locale.getDefault());
    }
    
    if(mPrecompiledPattern != null) {
      matches = mPrecompiledPattern.matcher(title).matches();
    }
    else {
      matches = title.equals(mExclusion);
    }
    
    return matches;
  }
}