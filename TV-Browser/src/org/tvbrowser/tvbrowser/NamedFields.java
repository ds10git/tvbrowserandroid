package org.tvbrowser.tvbrowser;

import java.util.Comparator;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.content.Context;

public final class NamedFields {
  public static final Comparator<NamedFields> COMPARATOR = new Comparator<NamedFields>() {
    @Override
    public int compare(NamedFields lhs, NamedFields rhs) {
      return lhs.mName.compareToIgnoreCase(rhs.mName);
    }
  }; 
  
  private String mName;
  private final String mColumn;
  
  public NamedFields(Context context, String column) {
    mColumn = column;
    
    if(TvBrowserContentProvider.DATA_KEY_ACTORS.equals(column)) {
      mName = context.getString(R.string.actors);
    }
    else if(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO.equals(column)) {
      mName = context.getString(R.string.additionalInfo);
    }
    else if(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT.equals(column)) {
      mName = context.getString(R.string.ageLimit);
    }
    else if(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING.equals(column)) {
      mName = context.getString(R.string.ageLimitString);
    }
    else if(TvBrowserContentProvider.DATA_KEY_CAMERA.equals(column)) {
      mName = context.getString(R.string.camera);
    }
    else if(TvBrowserContentProvider.DATA_KEY_CATEGORIES.equals(column)) {
      mName = context.getString(R.string.categories);
    }
    else if(TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO.equals(column)) {
      mName = context.getString(R.string.customInfo);
    }
    else if(TvBrowserContentProvider.DATA_KEY_CUT.equals(column)) {
      mName = context.getString(R.string.cut);
    }
    else if(TvBrowserContentProvider.DATA_KEY_DESCRIPTION.equals(column)) {
      mName = context.getString(R.string.description);
    }
    else if(TvBrowserContentProvider.DATA_KEY_DURATION_IN_MINUTES.equals(column)) {
      mName = context.getString(R.string.duration);
    }
    else if(TvBrowserContentProvider.DATA_KEY_ENDTIME.equals(column)) {
      mName = context.getString(R.string.endtime);
    }
    else if(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT.equals(column)) {
      mName = context.getString(R.string.episodeCount);
    }
    else if(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER.equals(column)) {
      mName = context.getString(R.string.episodeNumber);
    }
    else if(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE.equals(column)) {
      mName = context.getString(R.string.episodeTitle);
    }
    else if(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL.equals(column)) {
      mName = context.getString(R.string.episodeTitleOriginal);
    }
    else if(TvBrowserContentProvider.DATA_KEY_GENRE.equals(column)) {
      mName = context.getString(R.string.genre);
    }
    else if(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR.equals(column)) {
      mName = context.getString(R.string.lastProductionYear);
    }
    else if(TvBrowserContentProvider.DATA_KEY_MODERATION.equals(column)) {
      mName = context.getString(R.string.moderation);
    }
    else if(TvBrowserContentProvider.DATA_KEY_MUSIC.equals(column)) {
      mName = context.getString(R.string.music);
    }
    else if(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME.equals(column)) {
      mName = context.getString(R.string.nettoPlayTime);
    }
    else if(TvBrowserContentProvider.DATA_KEY_ORIGIN.equals(column)) {
      mName = context.getString(R.string.origin);
    }
    else if(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS.equals(column)) {
      mName = context.getString(R.string.otherPersons);
    }
    else if(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION.equals(column)) {
      mName = context.getString(R.string.pictureDescription);
    }
    else if(TvBrowserContentProvider.DATA_KEY_PRODUCER.equals(column)) {
      mName = context.getString(R.string.producer);
    }
    else if(TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM.equals(column)) {
      mName = context.getString(R.string.productionFirm);
    }
    else if(TvBrowserContentProvider.DATA_KEY_RATING.equals(column)) {
      mName = context.getString(R.string.rating);
    }
    else if(TvBrowserContentProvider.DATA_KEY_REGIE.equals(column)) {
      mName = context.getString(R.string.regie);
    }
    else if(TvBrowserContentProvider.DATA_KEY_REPETITION_FROM.equals(column)) {
      mName = context.getString(R.string.repetitionFrom);
    }
    else if(TvBrowserContentProvider.DATA_KEY_REPETITION_ON.equals(column)) {
      mName = context.getString(R.string.repetitionOn);
    }
    else if(TvBrowserContentProvider.DATA_KEY_SCRIPT.equals(column)) {
      mName = context.getString(R.string.script);
    }
    else if(TvBrowserContentProvider.DATA_KEY_SERIES.equals(column)) {
      mName = context.getString(R.string.series);
    }
    else if(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION.equals(column)) {
      mName = context.getString(R.string.shortDescription);
    }
    else if(TvBrowserContentProvider.DATA_KEY_STARTTIME.equals(column)) {
      mName = context.getString(R.string.startTime);
    }
    else if(TvBrowserContentProvider.DATA_KEY_TITLE.equals(column)) {
      mName = context.getString(R.string.title);
    }
    else if(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL.equals(column)) {
      mName = context.getString(R.string.titleOrginal);
    }
    else if(TvBrowserContentProvider.DATA_KEY_YEAR.equals(column)) {
      mName = context.getString(R.string.year);
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
