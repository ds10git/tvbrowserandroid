package org.tvbrowser.tvbrowser;

import android.content.ContentValues;
import android.util.Log;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.utils.PrefUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DegenderPlugin {
  private static final String[] FIELD_TYPES = {
      TvBrowserContentProvider.DATA_KEY_TITLE,
      TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO,
      TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION,
      TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
      TvBrowserContentProvider.DATA_KEY_SERIES,
      TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS
  };
  
  private LinkedHashMap<String, String> mSingularReplacement;
  private LinkedHashMap<String, String> mPluralReplacement;
  
  private static final Pattern GENDERED = Pattern.compile("(\\b(?i)(die\\s){0,1}(?-i)\\b(?!Mc)(\\w+?)(?:\\s*[\\*\\:_](?i:i)|I)n(nen){0,1})", Pattern.DOTALL);
  private static final Pattern GENDERED_LONG = Pattern.compile("(\\b([\\w\\-]+?)innen\\b\\s+(?:und|oder)\\s+\\-{0,1}\\b(\\w+?)\\b)|(\\b([\\w\\-]+?)\\b\\s+(?:und|oder)\\s+\\-{0,1}\\b(\\w+?)innen\\b)", Pattern.DOTALL);
  private static final Pattern GENDERED_PARTIZIP = Pattern.compile("(\\b(?i)(?:(\\w*eine|der|die|bei|mit|\\w+en)\\s){0,1}(?-i)(\\b\\w+\\b\\s){0,1}\\b((\\p{Upper}\\w+)ende(n|r){0,1}(\\w*)\\b))", Pattern.DOTALL);

  private boolean mRemoveLongForm = false;
  private boolean mReplacePartizip = false;
  
  private int mCountShort;
  private int mCountLong;
  private int mCountPartizip;

  private boolean mEnabled;
  
  public DegenderPlugin() {
    mSingularReplacement = new LinkedHashMap<String, String>();
    
    mSingularReplacement.put("\u00c4rzt", "Arzt");
    mSingularReplacement.put("Anw\u00e4lt", "Anwalt");
    mSingularReplacement.put("B\u00e4uer", "Bauer");
    mSingularReplacement.put("Beamt", "Beamter");
    mSingularReplacement.put("G\u00c4st", "Gast");
    mSingularReplacement.put("log", "loge");
    mSingularReplacement.put("r\u00e4t", "rat");
    
    mPluralReplacement = new LinkedHashMap<String, String>();
    
    mPluralReplacement.put("\u00c4rzt","\u00c4rzte");
    mPluralReplacement.put("Anw\u00e4lt", "Anw\u00e4lte");
    mPluralReplacement.put("B\u00e4uer", "Bauern");
    mPluralReplacement.put("Beamt","Beamte");
    mPluralReplacement.put("G\u00c4st","G\u00c4ste");
    mPluralReplacement.put("Freund", "Freunde");
    
    mPluralReplacement.put("ling","linge");
    mPluralReplacement.put("eur","eure");
    mPluralReplacement.put("ich","iche");
    mPluralReplacement.put("ier", "iere"); 
    mPluralReplacement.put("ig", "ige");
    mPluralReplacement.put("\u00f6r", "\u00f6re");
    mPluralReplacement.put("r\u00e4t", "r\u00e4te");
  }

  public void handleTvDataUpdateStarted() {
    mCountShort = 0;
    mCountLong = 0;
    mCountPartizip = 0;

    mEnabled = PrefUtils.getBooleanValue(R.string.PREF_DEGENDER_ENABLED,R.bool.pref_degender_enabled_default);
    mRemoveLongForm = PrefUtils.getBooleanValue(R.string.PREF_DEGENDER_ENABLED,R.bool.pref_degender_enabled_default);
    mReplacePartizip = PrefUtils.getBooleanValue(R.string.PREF_DEGENDER_ENABLED,R.bool.pref_degender_enabled_default);

    Log.d("info22","DEGENDER: " + mEnabled);
  }

  public void handleData(ContentValues values) {
    if(mEnabled) {
      for(String field : FIELD_TYPES) {
        String text = values.getAsString(field);
        text = deGender(text);
        values.put(field, text);
      }
    }
  }

  public void handleTvDataUpdateFinished() {
    Log.d("info22","DEGENDER: " + mCountShort + " " + mCountLong + " " + mCountPartizip);
    PrefUtils.setIntValue(R.string.PREF_DEGENDER_COUNT_SHORT, mCountShort);
    PrefUtils.setIntValue(R.string.PREF_DEGENDER_COUNT_LONG, mCountLong);
    PrefUtils.setIntValue(R.string.PREF_DEGENDER_COUNT_PARTIZIP, mCountPartizip);
  }

  private String deGenderShort(String text) {
    Matcher m = GENDERED.matcher(text);
    StringBuilder result = new StringBuilder();
    
    do {
      int pos = 0;
      
      while(m.find(pos)) {
       /*for(int i = 1; i <= m.groupCount(); i++) {
          System.out.println(i+": " + m.group(i));
        }
        */
        boolean wrongArticle = (m.group(2) != null && !m.group(2).trim().isEmpty());
        String replace = m.group(3);
        String test = replace.toLowerCase();
        boolean singular = (m.group(4) == null || m.group(4).trim().isEmpty());
        
        if(!singular) {
          Set<String> keys = mPluralReplacement.keySet();
          boolean found = false;
          for(String key : keys) {
            if(test.endsWith(key.toLowerCase())) {
              found = true;
              String replacement = mPluralReplacement.get(key);
              
              if(replace.charAt(replace.length()-key.length()) != key.charAt(0)) {
                replacement = replacement.toLowerCase();
              }
              
              replace = replace.substring(0,replace.length()-key.length()) + replacement;
              
              break;
            }
          }
          
          if(!found && !test.endsWith("er") && !test.endsWith("el")) {
            replace += "en";
          }
        }
        else {
          Set<String> keys = mSingularReplacement.keySet();
          for(String key : keys) {
            if(test.endsWith(key.toLowerCase())) {
              String replacement = mSingularReplacement.get(key);
              
              if(replace.charAt(replace.length()-key.length()) != key.charAt(0)) {
                replacement = replacement.toLowerCase();
              }
              
              replace = replace.substring(0,replace.length()-key.length()) + replacement;
              
              break;
            }
          }
          
          if(wrongArticle) {
            if(m.group(2).startsWith("D")) {
              replace = "Der "+replace;
            }
            else {
              replace = "der "+replace;
            }
          }
        }
     //   System.out.println("    "+replace+"\n");
        mCountShort++;
        result.append(text.substring(pos,m.start(1))).append(replace);
        pos = m.end();
      }
      
      if(pos < text.length()) {
        result.append(text.substring(pos,text.length()));
      }
      
      text = result.toString();
      
      m = GENDERED.matcher(text);
    }while(m.find());
    
    text = text.replace("jede:r", "jeder");
    
    return text;
  }
  
  private String deGenderLong(String text) {
    if(mRemoveLongForm && text != null) {
      int pos = 0;
      StringBuilder result = new StringBuilder();
      Matcher m = GENDERED_LONG.matcher(text);
      
      while(m.find(pos)) {
        /*for(int i = 1; i <= m.groupCount(); i++) {
        System.out.println(i+": " + m.group(i));
      }System.out.println();*/
        String needle = null;
        String replace = null;
        String male = null;
        String female = null;
        int index = -1;
        
        if(m.group(1) != null && m.group(2) != null && m.group(3) != null) {
          needle = m.group(1);
          female = m.group(2).toLowerCase().replace("ä", "a").replace("ö", "o").replace("ü", "u");
          replace = m.group(3);
          male = replace.toLowerCase().replace("ä", "a").replace("ö", "o").replace("ü", "u");
          
          if(female.contains("-")) {
            female = female.substring(female.lastIndexOf("-")+1);
            replace = m.group(2).substring(0,m.group(2).lastIndexOf("-")+1)+replace;
          }
          
          index = m.start(1);
        }
        else if(m.group(4) != null && m.group(5) != null && m.group(6) != null) {
          needle = m.group(4);
          female = m.group(6).toLowerCase().replace("ä", "a").replace("ö", "o").replace("ü", "u");
          replace = m.group(5);
          male = replace.toLowerCase().replace("ä", "a").replace("ö", "o").replace("ü", "u");
          
          if(male.contains("-")) {
            male = male.substring(male.lastIndexOf("-")+1);
          }
          
          index = m.start(4);
        }
        
        if(needle != null && male != null && female != null && male.startsWith(female)) {
          result.append(text.substring(pos,index)).append(replace);
          mCountLong++;
        }
        else {
          result.append(text.substring(pos,index)).append(needle);
        }
        
        pos = m.end();
      }
      
      if(pos < text.length()) {
        result.append(text.substring(pos,text.length()));
      }
      
      text = result.toString();
    }
    
    return text;
  }
  
  private String deGenderPartizip(String text) {
    if(mReplacePartizip && text != null) {
      StringBuilder result = new StringBuilder();
      Matcher m = GENDERED_PARTIZIP.matcher(text);
      int pos = 0;
      
      while(m.find(pos)) {
        String replace = m.group(1);
        
        if(m.group(5).toLowerCase().endsWith("studier") || m.group(5).toLowerCase().endsWith("dozier")) {
          replace = m.group(1).substring(0,m.group(1).length()-m.group(4).length()+1) + m.group(5).substring(1,m.group(5).length()-3)+"ent";
          
          if((m.group(2) == null && (m.group(6) == null || (m.group(6) != null && m.group(6).equals("n")))) || 
              (m.group(2) != null && (m.group(6) != null && m.group(6).equals("n"))) ||
              (m.group(2) != null && m.group(6) == null && m.group(2).endsWith("en"))) {
            replace += "en";
          }
        }
        else if(m.group(5).toLowerCase().endsWith("kunstschaff")) {
          replace = m.group(1).substring(0,m.group(1).length()-m.group(4).length()+1) + m.group(5).substring(1,m.group(5).length()-10)+"ünstler";
        }
        else if(m.group(5).toLowerCase().endsWith("forsch") || m.group(5).toLowerCase().endsWith("lehr") 
            || m.group(5).toLowerCase().endsWith("bewohn") || m.group(5).toLowerCase().endsWith("besuch")
            || m.group(5).toLowerCase().endsWith("eit") || m.group(5).toLowerCase().endsWith("ütz")
            || m.group(5).toLowerCase().endsWith("fahr") || m.group(5).toLowerCase().endsWith("arbeitnehm")
            || m.group(5).toLowerCase().endsWith("helf") || m.group(5).toLowerCase().endsWith("schau")
            || m.group(5).toLowerCase().endsWith("trink") || m.group(5).toLowerCase().endsWith("teilnehm")
            || m.group(5).toLowerCase().endsWith("deal")) {
          replace = m.group(1).substring(0,m.group(1).length()-m.group(4).length()) + m.group(5)+"er";
        }
        else if(m.group(5).toLowerCase().endsWith("zufußgeh")) {
          replace = m.group(5).substring(0,m.group(5).length()-8);
          
          if(Character.isUpperCase(m.group(5).charAt(m.group(5).length()-8))) {
            replace += "Fußgänger";
          }
          else {
            replace += "fußgänger";
          }
        }
        
        if(!replace.equals(m.group(1))) {
          if(m.group(7) != null && !m.group(7).trim().isEmpty()) {
            replace += m.group(7);
          }
     
          if(m.group(6) == null && m.group(2) != null && (m.group(2).toLowerCase().equals("die") || m.group(2).toLowerCase().endsWith("eine"))) {
            replace += "in";
          }
          else if(m.group(6) != null && m.group(2) != null && (m.group(2).toLowerCase().equals("mit") || m.group(2).toLowerCase().equals("bei") 
              || m.group(2).endsWith("en")) && replace.toLowerCase().endsWith("er")
              && (m.group(3) == null || !m.group(3).trim().equals("die"))) {
            replace += "n";
          }
         // System.out.println(m.group(1) + " " + m.group(7) + " " + replace);
          mCountPartizip++;
        }

        result.append(text.substring(pos,m.start(1))).append(replace);
        
        pos = m.end();
      }
      
      
      
      if(pos < text.length()) {
        result.append(text.substring(pos,text.length()));
      }
      
      text = result.toString();
    }
    
    return text;
  }
  
  private String deGender(String text) {
    if(text != null) {
      text = deGenderShort(text);
      text = deGenderLong(text);
      text = deGenderPartizip(text);
    }
    
    return text;
  }
}
