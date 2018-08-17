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
package org.tvbrowser.tvbrowser;

import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class LoaderUpdater {
  private static final int TIME_UPDATE_DELAY = 400;
  
  private Fragment mFragment;
  
  private boolean mIsRunning;
  private long mLastUpdateStart;
  
  private Thread mUpdateWaitingThread;
  
  private Handler mHandler;
  
  public LoaderUpdater(Fragment fragment, Handler handler) throws UnsupportedFragmentException, NullPointerException {
    if(fragment instanceof LoaderManager.LoaderCallbacks<?>) {
      mFragment = fragment;
    }
    else {
      throw new UnsupportedFragmentException();
    }

    if(handler == null) {
      throw new NullPointerException("Handler cannot be null");
    }
    
    mHandler = handler;
    mLastUpdateStart = 0;
    
    /*final Class<?>[] interfaces = mFragment.getClass().getInterfaces();
    final String interfaceClassName = LoaderManager.LoaderCallbacks.class.getCanonicalName();
    
     * 
     * for(Class<?> test : interfaces) {
      if(test.getCanonicalName().equals(interfaceClassName)) {
        mFragment = fragment;
        mLoaderCallback = (LoaderManager.LoaderCallbacks<?>)fragment;
        break;
      }
    }*/
  }

  public void setIsRunning() {
    mIsRunning = true;
  }
  
  public void setIsNotRunning() {
    mIsRunning = false;
  }
  
  public boolean isRunning() {
    return mIsRunning;
  }
  
  public void startUpdate() {
    startUpdate(0, null);
  }
  
  public void startUpdate(final CallbackObjects callbackObjects) {
    startUpdate(0, callbackObjects);
  }
  
  public void startUpdate(long nextUpdate) {
    startUpdate(nextUpdate, null);
  }
  
  private synchronized void startUpdate(long nextUpdate, final CallbackObjects callbackObjects) {
    mLastUpdateStart = System.currentTimeMillis();
    if((nextUpdate == 0 || nextUpdate >= System.currentTimeMillis()) && 
        (mUpdateWaitingThread == null || !mUpdateWaitingThread.isAlive())) {
      mUpdateWaitingThread = new Thread("LoaderUpdate UPDATE WAITING THREAD") {
        @Override
        public void run() {
          while(mLastUpdateStart + TIME_UPDATE_DELAY > System.currentTimeMillis()) {
            try {
              sleep(TIME_UPDATE_DELAY);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          
          if(!mFragment.isDetached() && mFragment.getActivity() != null && mIsRunning) {
            if(mFragment instanceof Callback) {
              ((Callback) mFragment).handleCallback(callbackObjects);
            }
            
            if(mFragment.getLoaderManager().hasRunningLoaders()) {
              try {
                mFragment.getLoaderManager().getLoader(0).stopLoading();
              }catch(Throwable ignored) {}
            }

            mHandler.post(() -> {
              if(!mFragment.isDetached() && mFragment.getActivity() != null && mIsRunning) {
                mFragment.getLoaderManager().restartLoader(0, null, (LoaderManager.LoaderCallbacks<?>)mFragment);
              }
            });
          }
        }
      };
      mUpdateWaitingThread.start();
    }
  }
  
  
  public static final class UnsupportedFragmentException extends Exception {
    UnsupportedFragmentException() {
      super("Fragment not supported, must implement android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>");
    }
  }
  
  public static final class CallbackObject<T> {
    private String mName;
    private T mValue;
    
    public CallbackObject(String name, T value) throws NullPointerException {
      if(name == null) {
        throw new NullPointerException("No null values for name allowed.");
      }
      
      mName = name;
      mValue = value;
    }
    
    public String getName() {
      return mName;
    }
    
    public T getValue() {
      return mValue;
    }
    
    @Override
    public int hashCode() {
      return Arrays.hashCode(mName.getBytes());
    }
    
    @Override
    public boolean equals(Object o) {
      if(o instanceof CallbackObject) {
        return mName.equals(((CallbackObject<?>) o).mName);
      }
      
      return super.equals(o);
    }
  }
  
  public static final class CallbackObjects {
    private final HashSet<CallbackObject<?>> mCallbackObjects;
    
    public CallbackObjects(CallbackObject<?>... initialObjects) {
      mCallbackObjects = new HashSet<>();
      
      if(initialObjects != null) {
        Collections.addAll(mCallbackObjects, initialObjects);
      }
    }
    
    public void addOrReplace(CallbackObject<?> callbackObject) {
      synchronized (mCallbackObjects) {
        mCallbackObjects.remove(callbackObject);
        mCallbackObjects.add(callbackObject);
      }
    }
    
    public CallbackObject<?>[] getCallbackObjects() {
      synchronized (mCallbackObjects) {
        return mCallbackObjects.toArray(new CallbackObject[0]);
      }
    }
    
    public Object getCallbackObjectValue(String name, Object defaultValue) {
      final CallbackObject<?> result = getCallbackObject(name, null);
      
      if(result != null) {
        defaultValue = result.mValue;
      }
      
      return defaultValue;
    }
    
    CallbackObject<?> getCallbackObject(String name, CallbackObject<?> defaultValue) {
      synchronized (mCallbackObjects) {
        for(CallbackObject<?> test : mCallbackObjects) {
          if(name.equals(test.mName)) {
            defaultValue = test;
            break;
          }
        }
      }
      
      return defaultValue;
    }
  }
  
  public interface Callback {
    void handleCallback(CallbackObjects callbackObjects);
  }
}
