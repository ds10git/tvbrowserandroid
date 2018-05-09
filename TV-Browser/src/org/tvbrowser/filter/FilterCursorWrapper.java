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
package org.tvbrowser.filter;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.CursorWrapper;

class FilterCursorWrapper extends CursorWrapper {
  private Integer[] filterMap = null;
  private int mPos = -1;
  private Cursor mCursor;

  @Override
  public int getCount() { 
    return filterMap != null ? filterMap.length : super.getCount();
  }

  @Override
  public boolean moveToPosition(int pos) {
      boolean moved = false;
      
      if(filterMap == null) {
        moved = super.moveToPosition(pos);
      }
      else if(filterMap.length > pos) {
        moved = super.moveToPosition(filterMap[pos]);
      }
      
      if (moved) mPos = pos;
      return moved;
  }

  @Override
  public final boolean move(int offset) {
      return moveToPosition(mPos + offset);
  }

  @Override
  public final boolean moveToFirst() {
      return moveToPosition(0);
  }

  @Override
  public final boolean moveToLast() {
      return moveToPosition(getCount() - 1);
  }

  @Override
  public final boolean moveToNext() {
      return moveToPosition(mPos + 1);
  }

  @Override
  public final boolean moveToPrevious() {
      return moveToPosition(mPos - 1);
  }

  @Override
  public final boolean isFirst() {
      return mPos == 0 && getCount() != 0;
  }

  @Override
  public final boolean isLast() {
      int cnt = getCount();
      return mPos == (cnt - 1) && cnt != 0;
  }

  @Override
  public final boolean isBeforeFirst() {
	  return getCount() == 0 || mPos == -1;
  }

  @Override
  public final boolean isAfterLast() {
	  return getCount() == 0 || mPos == getCount();
  }

  @Override
  public int getPosition() {
      return mPos;
  }  
  
  public FilterCursorWrapper(Cursor cursor) {
    super(cursor);
    
    mCursor = cursor;
    filterMap = null;
    mPos = -1;
  }

  public void updateFilter(CursorFilter filter) {
    ArrayList<Integer> newFilterMap = new ArrayList<Integer>();
    
    for(int i = 0; i < super.getCount(); i++) {
      if(mCursor.moveToNext()) {
        if(filter.accept(mCursor)) {
          newFilterMap.add(i);
        }
      }
    }
    
    mPos = -1;
    filterMap = newFilterMap.toArray(new Integer[newFilterMap.size()]);
  }
}
