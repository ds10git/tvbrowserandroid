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

import org.tvbrowser.content.TvBrowserContentProvider;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;

/**
 * A class with bulk data base operations that takes the
 * memory size on the system into consideration.
 * 
 * @author René Mach
 */
public class MemorySizeConstrictedDatabaseOperation {
  private static final int TABLE_OPERATION_MIN_SIZE = Math.max(100, (int)(Runtime.getRuntime().maxMemory()/1000000));
  private Context mContext;
  
  private ArrayList<ContentValues> mInsertList;
  private ArrayList<ContentProviderOperation> mUpdateList;
  
  private Uri mInsertUri;
  private int mMinOperationDivider;
  
  private boolean mOperationsAvailable;
  
  public MemorySizeConstrictedDatabaseOperation(Context context, Uri insertUri) {
    this(context,insertUri,1);
  }
  
  /**
   * @param context The Context to use.
   * @param insertUri The content URI for the insert operations
   * @param minOperationDivider The divider for the number of entries before starting database opertaion-
   */
  public MemorySizeConstrictedDatabaseOperation(Context context, Uri insertUri, int minOperationDivider) {
    mContext = context;
    mInsertUri = insertUri;
    
    if(mMinOperationDivider > 0) {
      mMinOperationDivider = minOperationDivider;
    }
    else {
      mMinOperationDivider = 1;
    }
    
    if(insertUri != null) {
      mInsertList = new ArrayList<ContentValues>();
    }
    
    mUpdateList = new ArrayList<ContentProviderOperation>();
    
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
      
      if(mInsertList.size() > TABLE_OPERATION_MIN_SIZE/mMinOperationDivider) {
        insert();
      }
    }
  }
  
  public synchronized void addUpdate(ContentProviderOperation update) {
    if(mUpdateList != null) {
      mOperationsAvailable = true;
      mUpdateList.add(update);
      
      if(mUpdateList.size() > TABLE_OPERATION_MIN_SIZE/mMinOperationDivider) {
        update();
      }
    }
  }
  
  private synchronized void insert() {
    if(mInsertUri != null && mInsertList != null && !mInsertList.isEmpty() && mContext != null) {
      mContext.getContentResolver().bulkInsert(mInsertUri, mInsertList.toArray(new ContentValues[mInsertList.size()]));
      mInsertList.clear();
    }
  }
  
  private synchronized void update() {
    if(mUpdateList != null && !mUpdateList.isEmpty() && mContext != null) {
      try {
        mContext.getContentResolver().applyBatch(TvBrowserContentProvider.AUTHORITY, mUpdateList);
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (OperationApplicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      mUpdateList.clear();
    }
  }
  
  public boolean operationsAvailable() {
    return mOperationsAvailable;
  }
}
