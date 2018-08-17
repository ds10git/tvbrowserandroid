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

import java.util.Comparator;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.utils.UiUtils;

import android.content.Context;

public final class NamedFields {
  public static final Comparator<NamedFields> COMPARATOR = (lhs, rhs) -> UiUtils.getCollator().compare(lhs.mName, rhs.mName);
  
  private String mName;
  private final String mColumn;
  
  public NamedFields(Context context, String column) {
    mColumn = column;

    switch (column) {
      case TvBrowserContentProvider.DATA_KEY_ACTORS:
        mName = context.getString(R.string.actors);
        break;
      case TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO:
        mName = context.getString(R.string.additionalInfo);
        break;
      case TvBrowserContentProvider.DATA_KEY_AGE_LIMIT:
        mName = context.getString(R.string.ageLimit);
        break;
      case TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING:
        mName = context.getString(R.string.ageLimitString);
        break;
      case TvBrowserContentProvider.DATA_KEY_CAMERA:
        mName = context.getString(R.string.camera);
        break;
      case TvBrowserContentProvider.DATA_KEY_CATEGORIES:
        mName = context.getString(R.string.categories);
        break;
      case TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO:
        mName = context.getString(R.string.customInfo);
        break;
      case TvBrowserContentProvider.DATA_KEY_CUT:
        mName = context.getString(R.string.cut);
        break;
      case TvBrowserContentProvider.DATA_KEY_DESCRIPTION:
        mName = context.getString(R.string.description);
        break;
      case TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES:
        mName = context.getString(R.string.duration);
        break;
      case TvBrowserContentProvider.DATA_KEY_ENDTIME:
        mName = context.getString(R.string.endtime);
        break;
      case TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT:
        mName = context.getString(R.string.episodeCount);
        break;
      case TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER:
        mName = context.getString(R.string.episodeNumber);
        break;
      case TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE:
        mName = context.getString(R.string.episodeTitle);
        break;
      case TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL:
        mName = context.getString(R.string.episodeTitleOriginal);
        break;
      case TvBrowserContentProvider.DATA_KEY_GENRE:
        mName = context.getString(R.string.genre);
        break;
      case TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR:
        mName = context.getString(R.string.lastProductionYear);
        break;
      case TvBrowserContentProvider.DATA_KEY_MODERATION:
        mName = context.getString(R.string.moderation);
        break;
      case TvBrowserContentProvider.DATA_KEY_MUSIC:
        mName = context.getString(R.string.music);
        break;
      case TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME:
        mName = context.getString(R.string.nettoPlayTime);
        break;
      case TvBrowserContentProvider.DATA_KEY_ORIGIN:
        mName = context.getString(R.string.origin);
        break;
      case TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS:
        mName = context.getString(R.string.otherPersons);
        break;
      case TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION:
        mName = context.getString(R.string.pictureDescription);
        break;
      case TvBrowserContentProvider.DATA_KEY_PRODUCER:
        mName = context.getString(R.string.producer);
        break;
      case TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM:
        mName = context.getString(R.string.productionFirm);
        break;
      case TvBrowserContentProvider.DATA_KEY_RATING:
        mName = context.getString(R.string.rating);
        break;
      case TvBrowserContentProvider.DATA_KEY_REGIE:
        mName = context.getString(R.string.regie);
        break;
      case TvBrowserContentProvider.DATA_KEY_REPETITION_FROM:
        mName = context.getString(R.string.repetitionFrom);
        break;
      case TvBrowserContentProvider.DATA_KEY_REPETITION_ON:
        mName = context.getString(R.string.repetitionOn);
        break;
      case TvBrowserContentProvider.DATA_KEY_SCRIPT:
        mName = context.getString(R.string.script);
        break;
      case TvBrowserContentProvider.DATA_KEY_SERIES:
        mName = context.getString(R.string.series);
        break;
      case TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION:
        mName = context.getString(R.string.shortDescription);
        break;
      case TvBrowserContentProvider.DATA_KEY_STARTTIME:
        mName = context.getString(R.string.startTime);
        break;
      case TvBrowserContentProvider.DATA_KEY_TITLE:
        mName = context.getString(R.string.title);
        break;
      case TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL:
        mName = context.getString(R.string.titleOrginal);
        break;
      case TvBrowserContentProvider.DATA_KEY_YEAR:
        mName = context.getString(R.string.year);
        break;
    }
    
    if(mName == null) {
      mName = "Unknown";
    }
    
    mName = mName.replace(":", "").replace("\n", "");
  }
  
  @Override
  public final String toString() {
    return mName;
  }
  
  public final String getColumn() {
    return mColumn;
  }
}
