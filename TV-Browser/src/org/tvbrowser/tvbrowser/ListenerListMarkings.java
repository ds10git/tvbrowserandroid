package org.tvbrowser.tvbrowser;

import android.os.Handler;
import android.support.v4.widget.CursorAdapter;
import android.widget.ListView;

public class ListenerListMarkings {
  private ListView mListView;
  private Handler mHandler;
  
  public ListenerListMarkings(ListView view, Handler handler) {
    mListView = view;
    mHandler = handler;
    refresh();
    /*handler.post(new Runnable() {
      @Override
      public void run() {
        ((OrientationHandlingCursorAdapter)mListView.getAdapter()).notifyDataSetChanged();
      }
    });*/
  }
  
  public void refresh() {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        //((OrientationHandlingCursorAdapter)mListView.getAdapter()).notifyDataSetChanged();
        mListView.invalidateViews();
      }
    });
  }
}
