/*
 * TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.utils.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

/**
 * A class to read news of TV-Browser.
 * <p>
 * @author René Mach
 */
@SuppressLint("SimpleDateFormat")
public class NewsReader {
  private static final String NEWS_URL = "http://www.tvbrowser.org/newsplugin/static-news.xml";
  private static final SimpleDateFormat NEWS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  // after 90 days a news is outdated and is no longer shown
  private static final int NEWS_DAYOUT = 90;
  
  /**
   * Make sure to call this not from UI thread cause this will access the Internet.
   * 
   * @param context The context for the news reader.
   */
  public static void readNews(final Context context, final Handler handler) {
    try {
      XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
      XmlPullParser myParser = xmlPullParserFactory.newPullParser();
      myParser.setInput(new InputStreamReader(new ByteArrayInputStream(IOUtils.loadUrl(NEWS_URL, 10000)),"ISO-8859-15"));
      
      ArrayList<News> newsList = new ArrayList<News>();
      
      int event = myParser.getEventType();
      News currentNews = null;
      String name = null;
      
      long newestNewsDate = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.NEWS_DATE_LAST_KNOWN), 0);
      
      while (event != XmlPullParser.END_DOCUMENT) {
        if(event == XmlPullParser.START_TAG) {
          name = myParser.getName();
          
          if(name.equals("news")) {
            currentNews = new News();
            
            for(int i = 0; i < myParser.getAttributeCount(); i++) {
              String attrName = myParser.getAttributeName(i);
              
              if(attrName.equals("date")) {
                currentNews.mDate = NEWS_DATE_FORMAT.parse(myParser.getAttributeValue(i));
                newestNewsDate = Math.max(newestNewsDate, currentNews.mDate.getTime());
              }
              else if(attrName.equals("author")) {
                currentNews.mAuthor = myParser.getAttributeValue(i);
              }
              else if(attrName.equals("type")) {
                currentNews.mType = myParser.getAttributeValue(i);
              }
            }
          }
        }
        else if(event == XmlPullParser.END_TAG) {
           if(myParser.getName().equals("news")) {
             if(currentNews != null && !currentNews.isOutdated()) {
               newsList.add(currentNews);
             }
           }
         }
         else if(event == XmlPullParser.TEXT) {
           if(name.equals("title")) {
             currentNews.mDeNewsTitle = URLDecoder.decode(myParser.getText(),"ISO-8859-15");
           }
           else if(name.equals("title-en")) {
             currentNews.mEnNewsTitle = URLDecoder.decode(myParser.getText(),"ISO-8859-15");
           }
           else if(name.equals("text")) {
             currentNews.mDeNewsText = URLDecoder.decode(myParser.getText(),"ISO-8859-15");
           }
           else if(name.equals("text-en")) {
             currentNews.mEnNewsText = URLDecoder.decode(myParser.getText(),"ISO-8859-15");
           }
         }
         
         event = myParser.next();
      }
      
      StringBuilder newsText = new StringBuilder();
      
      boolean de = Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage());
      
      String newsType = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.PREF_NEWS_TYPE), context.getString(R.string.pref_news_type_default));
      
      for(News news : newsList) {
        if(news.isAcceptedType(context,newsType)) {
          if(newsText.length() > 0) {
            newsText.append("<line>LINE</line>");
          }
          
          newsText.append("<p>");
          newsText.append("<i><u>");
          newsText.append(DateFormat.getLongDateFormat(context).format(news.mDate)).append(":");
          newsText.append("</u></i>");
          newsText.append("</p>");
          
          newsText.append("<h2>");
          newsText.append(news.getTitle(de));
          newsText.append("</h2>");
          
          newsText.append(news.getText(de));
          
          newsText.append("<p><i><right>");
          newsText.append(news.mAuthor);
          newsText.append("</right></i></p>");
        }
      }
      
      Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
      edit.putString(context.getString(R.string.NEWS_TEXT), newsText.toString());
      edit.putLong(context.getString(R.string.NEWS_DATE_LAST_KNOWN), newestNewsDate);
      edit.putLong(context.getString(R.string.NEWS_DATE_LAST_DOWNLOAD), System.currentTimeMillis());
      
      edit.commit();
    } catch (Exception e) {}
  }
  
  private static final class News {
    Date mDate;
    String mType;
    String mAuthor;
    String mDeNewsTitle;
    String mEnNewsTitle;
    String mDeNewsText;
    String mEnNewsText;
    
    boolean isAcceptedType(Context context, String type) {
      boolean accept = mType == null || type.equals(context.getString(R.string.pref_news_type_all)) || mType.equals(context.getString(R.string.pref_news_type_none));
      
      if(!accept) {
        String tvbType = context.getString(R.string.pref_news_type_tvbrowser);
        String androidType = context.getString(R.string.pref_news_type_android);
        String desktopType = context.getString(R.string.pref_news_type_desktop);
        
        if(type.equals(tvbType)) {
          accept = (mType.equals(tvbType) || mType.equals(androidType) || mType.equals(desktopType));
        }
        else {
          accept = mType.equals(type);
        }
      }
      
      return accept;
    }
    
    boolean isOutdated() {
      boolean outdated = mDate == null || mDate.getTime() < (System.currentTimeMillis() - (NEWS_DAYOUT * 24 * 60 * 60000L));
      
      return outdated;
    }
    
    @Override
    public String toString() {
      return mDate + " " + mAuthor + " " + mDeNewsTitle + " " + mEnNewsTitle + " " + mDeNewsText + " " + mEnNewsText;
    }
    
    public String getTitle(boolean de) {
      if((de || mEnNewsTitle == null) && mDeNewsTitle != null) {
        return mDeNewsTitle;
      }
      
      return mEnNewsTitle;
    }
    
    public String getText(boolean de) {
      if((de || mEnNewsText == null) && mDeNewsText != null) {
        return mDeNewsText;
      }
      
      return mEnNewsText;
    }
  }
}
