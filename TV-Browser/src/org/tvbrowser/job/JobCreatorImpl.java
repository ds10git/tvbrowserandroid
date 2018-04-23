package org.tvbrowser.job;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class JobCreatorImpl implements JobCreator {
  @Override
  @Nullable
  public Job create(@NonNull String tag) {
    switch (tag) {
      case JobDataUpdateAuto.TAG:
        Log.d("info9","createJob ");return new JobDataUpdateAuto();
    }

    return null;
  }
}
