/*
 * TV-Browser for Android
 * Copyright (C) 2015 René Mach (rene@tvbrowser.org)
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

/**
 * A class that stores the selection and selection arguments for a SQL query.
 * 
 * @author René Mach
 */
public class WhereClause {
  private String mWhere;
  private String[] mSelectionArgs;
  
  public WhereClause() {
    this(null,null);
  }
  
  public WhereClause(String where, String[] selectionArgs) {
    mWhere = where;
    mSelectionArgs = selectionArgs;
  }
  
  public String getWhere() {
    return mWhere;
  }
  
  public String[] getSelectionArgs() {
    return mSelectionArgs;
  }
}