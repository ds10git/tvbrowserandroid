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
package org.tvbrowser.devplugin;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * A class that contains information about a certain Plugin.
 * <p>
 * @author René Mach
 */
public class PluginDefinition implements Comparable<PluginDefinition> {
  private static final String PLUGIN_INFO_URL = SettingConstants.URL_SYNC_BASE;
  
  private static final String XML_ELEMENT_ROOT = "plugin";
  private static final String XML_ATTRIBUTE_PACKAGE = "package";
  private static final String XML_ATTRIBUTE_MIN_API = "minApi";
  private static final String XML_ATTRIBUTE_VERSION = "version";
  private static final String XML_ATTRIBUTE_AUTHOR = "author";
  private static final String XML_ATTRIBUTE_ON_GOOGLE_PLAY = "isOnGooglePlay";
  
  private static final String XML_ELEMENT_NAME_DE = "name-de";
  private static final String XML_ELEMENT_NAME_EN = "name-en";
  
  private static final String XML_ELEMENT_DESCRIPTION_DE = "description-de";
  private static final String XML_ELEMENT_DESCRIPTION_EN = "description-en";

  private static final String XML_ELEMENT_SERVICES = "servicelist";
  private static final String XML_ELEMENT_SERVICE = "service";
  
  private final String mPackageName;
  private final String mVersion;
  private final String mAuthor;
  
  private String mNameDe;
  private String mNameEn;
  
  private String mDescriptionDe;
  private String mDescriptionEn;
  
  private final HashMap<String,String> mUnknownValues;
  
  private final int mMinApiVersion;
  
  private final boolean mIsOnGooglePlay;
  private boolean mIsUpdate;
  
  private String[] mServices;

  private static final Comparator<PluginDefinition> COMPARATOR_DOWN = (o1, o2) -> {
    if (Locale.getDefault().getLanguage().equals(new Locale("de", "", "").getLanguage())) {
      return o2.mNameDe.compareToIgnoreCase(o1.mNameDe);
    }

    return o2.mNameEn.compareTo(o1.mNameEn);
  };

  public static Comparator<PluginDefinition> getComparatorDown() {
    return COMPARATOR_DOWN;
  }

  @SuppressWarnings("WeakerAccess")
  public PluginDefinition(String packageName, int minApiVersion, String version, String author, boolean isOnGooglePlay) {
    this(packageName, minApiVersion, version, author, isOnGooglePlay, null, null, null, null, null);
  }
  
  @SuppressWarnings("WeakerAccess")
  public PluginDefinition(String packageName, int minApiVersion, String version, String author, boolean isOnGooglePlay, String nameDe, String nameEn, String descriptionDe, String descriptionEn, String[] services) {
    mPackageName = packageName;
    mMinApiVersion = minApiVersion;
    mVersion = version;
    mAuthor = author;
    mIsOnGooglePlay = isOnGooglePlay;
    mNameDe = nameDe;
    mNameEn = nameEn;
    mDescriptionDe = descriptionDe;
    mDescriptionEn = descriptionEn;
    mServices = services;

    mUnknownValues = new HashMap<>();
  }
  
  private String getValue(String value) {
    return value != null ? value : "";
  }
  
  public String getPackageName() {
    return getValue(mPackageName);
  }
  
  public int getMinApiVersion() {
    return mMinApiVersion;
  }
  
  public String getVersion() {
    return getValue(mVersion);
  }
  
  public String getAuthor() {
    return mAuthor;
  }
  
  public boolean isOnGooglePlay() {
    return mIsOnGooglePlay;
  }
  
  public String getName() {
    if (Locale.getDefault().getLanguage().equals(new Locale("de", "", "").getLanguage())) {
      return getValue(mNameDe);
    }
    
    return getValue(mNameEn);
  }
  
  public String getDescription() {
    if (Locale.getDefault().getLanguage().equals(new Locale("de", "", "").getLanguage())) {
      return getValue(mDescriptionDe);
    }
    
    return getValue(mDescriptionEn);
  }
    
  public String getUnknownValueForName(final String name) {
    return getValue(mUnknownValues.get(name));
  }

  public String[] getServices() {
    return mServices;
  }
    
  public static PluginDefinition[] loadAvailablePluginDefinitions(String url) {
    ArrayList<PluginDefinition> pluginList = new ArrayList<>();
    
    InputStreamReader in = null;
        
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      Log.d("info6","openURL: "+PLUGIN_INFO_URL+url);
      in = new InputStreamReader(IOUtils.decompressStream(new ByteArrayInputStream(IOUtils.loadUrl(PLUGIN_INFO_URL+url, 15000))),"UTF-8");
      
      XmlPullParser parser = factory.newPullParser();
      parser.setInput(in);
      
      String tagName = null;
      PluginDefinition current = null;
      int event = parser.getEventType();
      ArrayList<String> serviceList = new ArrayList<>();
      
      while(event != XmlPullParser.END_DOCUMENT) {
        switch(parser.getEventType()) {
          case XmlPullParser.START_TAG:
          {
            tagName = parser.getName();

            switch (tagName) {
              case XML_ELEMENT_ROOT:
                String isOnGooglePlay = parser.getAttributeValue(null, XML_ATTRIBUTE_ON_GOOGLE_PLAY);

                if (isOnGooglePlay == null) {
                  isOnGooglePlay = "false";
                }

                String author = parser.getAttributeValue(null, XML_ATTRIBUTE_AUTHOR);

                if (author != null) {
                  author = URLDecoder.decode(author, "UTF-8");
                } else {
                  author = "Unknown";
                }

                int minApi = 11;

                String readApi = parser.getAttributeValue(null, XML_ATTRIBUTE_MIN_API);

                if (readApi != null) {
                  minApi = Integer.parseInt(readApi);
                }

                current = new PluginDefinition(parser.getAttributeValue(null, XML_ATTRIBUTE_PACKAGE), minApi, parser.getAttributeValue(null, XML_ATTRIBUTE_VERSION), author, isOnGooglePlay.equals("true"));
                break;
              case XML_ELEMENT_SERVICES:
                serviceList.clear();
                break;
              case XML_ELEMENT_SERVICE:
                serviceList.add(parser.getAttributeValue(null, XML_ATTRIBUTE_PACKAGE));
                break;
            }
          }break;
          case XmlPullParser.TEXT:
          {
            if(current != null) {
              switch (tagName) {
                case XML_ELEMENT_NAME_EN:
                  current.mNameEn = URLDecoder.decode(parser.getText(), "UTF-8");
                  break;
                case XML_ELEMENT_NAME_DE:
                  current.mNameDe = URLDecoder.decode(parser.getText(), "UTF-8");
                  break;
                case XML_ELEMENT_DESCRIPTION_EN:
                  current.mDescriptionEn = URLDecoder.decode(parser.getText(), "UTF-8");
                  break;
                case XML_ELEMENT_DESCRIPTION_DE:
                  current.mDescriptionDe = URLDecoder.decode(parser.getText(), "UTF-8");
                  break;
                default:
                  current.mUnknownValues.put(tagName, URLDecoder.decode(parser.getText(), "UTF-8"));
                  break;
              }
            }
          }break;
          case XmlPullParser.END_TAG:
          {
            if(parser.getName().equals(XML_ELEMENT_ROOT)) {
              pluginList.add(current);
            }
            else if(parser.getName().equals(XML_ELEMENT_SERVICES)) {
              current.mServices = serviceList.toArray(new String[serviceList.size()]);
              serviceList.clear();
            }
          }break;
        }
        
        event = parser.next();
      }
      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (XmlPullParserException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TimeoutException e) {
      // TODO Auto-generated catch block
      Log.d("info6","",e);
    }
    
    return pluginList.toArray(new PluginDefinition[pluginList.size()]);
  }

  @Override
  public int compareTo(@SuppressWarnings("NullableProblems") PluginDefinition o) {
    if (Locale.getDefault().getLanguage().equals(new Locale("de", "", "").getLanguage())) {
      return mNameDe.compareToIgnoreCase(o.mNameDe);
    }
    
    return mNameEn.compareTo(o.mNameEn);
  }
  
  public void setIsUpdate() {
    mIsUpdate = true;
  }
  
  public boolean isUpdate() {
    return mIsUpdate;
  }
  
  @Override
  public String toString() {
    if (Locale.getDefault().getLanguage().equals(new Locale("de", "", "").getLanguage())) {
      return mNameDe + " " + mVersion;
    }
    
    return mNameEn + " " + mVersion;
  }
}
