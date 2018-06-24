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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

/**
 * A class with bulk data base operations that takes the
 * memory size on the system into consideration.
 * 
 * @author René Mach
 */
class MemorySizeConstrictedDatabaseOperation {
  private static final int TABLE_OPERATION_MIN_SIZE = Math.max(100, (int)(Runtime.getRuntime().maxMemory()/1000000));
  private Context mContext;
  
  private ArrayList<ContentValues> mInsertList;
  private ArrayList<ContentProviderOperation> mUpdateList;
  
  private final Uri mInsertUri;
  private int mMinOperationDivider;
  private final int mOperationDivider;
  
  private boolean mOperationsAvailable;
  private boolean mOperationsAdded;
  private final AtomicBoolean mSuccess;
  
  public MemorySizeConstrictedDatabaseOperation(Context context, Uri insertUri) {
    this(context,insertUri,1);
  }
  
  /**
   * @param context The Context to use.
   * @param insertUri The content URI for the insert operations
   * @param minOperationDivider The divider for the number of entries before starting database operation.
   */
  public MemorySizeConstrictedDatabaseOperation(Context context, Uri insertUri, int minOperationDivider) {
    mContext = context;
    mInsertUri = insertUri;
    mSuccess = new AtomicBoolean(true);
    mOperationsAdded = false;
    
    if(mMinOperationDivider > 0) {
      mMinOperationDivider = minOperationDivider;
    }
    else {
      mMinOperationDivider = 1;
    }
    
    mOperationDivider = TABLE_OPERATION_MIN_SIZE/mMinOperationDivider;
    
    if(insertUri != null) {
      mInsertList = new ArrayList<>();
    }
    
    mUpdateList = new ArrayList<>();
    
    mOperationsAvailable = false;
  }
  
  public synchronized void cancel() {
    if(mInsertList != null && !mInsertList.isEmpty()) {
      mInsertList.clear();
    }
    
    if(mUpdateList != null && !mUpdateList.isEmpty()) {
      mInsertList.clear();
    }
    
    mInsertList = null;
    mUpdateList = null;
    mContext = null;
  }
  
  public synchronized void finish() {
    if(mInsertList != null && !mInsertList.isEmpty()) {
      insert();
    }
    
    if(mUpdateList != null && !mUpdateList.isEmpty()) {
      update();
    }
    
    mInsertList = null;
    mUpdateList = null;
    mContext = null;
  }
  
  public synchronized void addInsert(ContentValues insert) {
    if(mInsertList != null) {
      mOperationsAvailable = true;
      mInsertList.add(insert);
      mOperationsAdded = true;
      
      if(mInsertList.size() > mOperationDivider) {
        insert();
      }
    }
  }
  
  public synchronized void addUpdate(ContentProviderOperation update) {
    if(mUpdateList != null) {
      mOperationsAvailable = true;
      mUpdateList.add(update);
      mOperationsAdded = true;
      
      if(mUpdateList.size() > mOperationDivider) {
        update();
      }
    }
  }
  
  private synchronized void insert() {
    if(mInsertUri != null && mInsertList != null && !mInsertList.isEmpty() && mContext != null) {
      boolean success = mContext.getContentResolver().bulkInsert(mInsertUri, mInsertList.toArray(new ContentValues[0])) >= mInsertList.size();
      
      mSuccess.compareAndSet(true, success);
      
      mInsertList.clear();
    }
  }
  
  private synchronized void update() {
    Log.d("info9", "update()");
    if(mUpdateList != null && !mUpdateList.isEmpty() && mContext != null) {
      boolean success = false;
      Log.d("info9", " " + mUpdateList.size());
      try {
         success = mContext.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, mUpdateList).length >= mUpdateList.size();
      } catch (RemoteException | OperationApplicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      mSuccess.compareAndSet(true, success);
      
      mUpdateList.clear();
    }
  }
  
  public boolean operationsAvailable() {
    return mOperationsAvailable;
  }
  
  public boolean wasSuccessful() {
    return mSuccess.get() && mOperationsAdded;
  }
}
