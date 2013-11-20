package org.tvbrowser.tvbrowser;

import android.database.Cursor;
import android.database.CursorWrapper;

public class RunningCursorWrapper extends CursorWrapper {
  private Integer[] filterMap = null;
  private int mPos = -1;

  @Override
  public int getCount() { return filterMap.length; }

  @Override
  public boolean moveToPosition(int pos) {
      boolean moved = super.moveToPosition(filterMap[pos]);
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
      if (getCount() == 0) {
          return true;
      }
      return mPos == -1;
  }

  @Override
  public final boolean isAfterLast() {
      if (getCount() == 0) {
          return true;
      }
      return mPos == getCount();
  }

  @Override
  public int getPosition() {
      return mPos;
  }  
  
  public RunningCursorWrapper(Cursor cursor) {
    super(cursor);
    
    filterMap = new Integer[cursor.getCount()];
  }

  public void setFilterMap(Integer[] filterMap) {
    this.filterMap = filterMap;
  }
}
