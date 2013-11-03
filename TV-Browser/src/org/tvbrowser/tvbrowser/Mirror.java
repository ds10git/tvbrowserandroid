/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software without modifications and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

/**
 * Class with informations about a webserver for data update.
 * <p>
 * @author René Mach
 */
public class Mirror implements Comparable<Mirror> {
  private String mUrl;
  private int mWeight;
  
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  public Mirror(String url, int weight) {
    mUrl = url;
    mWeight = weight;
  }
  
  public String getUrl() {
    return mUrl;
  }
  
  public int getWeight() {
    return mWeight;
  }
  
  /**
   * Gets the mirror array from the given text.
   * <p>
   * @param value The text containing the mirror information.
   * @return The array with Mirrors.
   */
  public static Mirror[] getMirrorsFor(String value) {
    ArrayList<Mirror> mirrors = new ArrayList<Mirror>();
    String[] mirrorParts = value.split(";");
    
    for(String part : mirrorParts) {
      if(part.contains("#")) {
        String[] mirrorValues = part.split("#");
        
        if(!mirrorValues[0].endsWith("/")) {
          mirrorValues[0] += "/";
        }
        
        mirrors.add(new Mirror(mirrorValues[0], Integer.valueOf(mirrorValues[1])));
      }
      else {
        if(!part.endsWith("/")) {
          part += "/";
        }

        mirrors.add(new Mirror(part, 10));
      }
    }
    
    Mirror[] mirrorArr = new Mirror[mirrors.size()];
    
    return mirrors.toArray(mirrorArr);
  }

  @Override
  public int compareTo(Mirror another) {
    if(mWeight < another.mWeight) {
      return -1;
    }
    else if(mWeight > another.mWeight) {
      return 1;
    }
    
    return 0;
  }
  
  public static Mirror getMirrorToUseForGroup(Mirror[] mirrors, String group) {
    ArrayList<Mirror> toChooseFrom = new ArrayList<Mirror>(Arrays.asList(mirrors));
    
    Mirror choosen = null;
    
    while(choosen == null && !toChooseFrom.isEmpty()) {
      int weightSum = 0;
      
      for(Mirror mirror : toChooseFrom) {
        weightSum += mirror.getWeight();
      }
      
      int limit = new Random().nextInt(weightSum + 1);
          
      for(int i = toChooseFrom.size()-1; i >= 0; i--) {
        if(toChooseFrom.get(i).getWeight() >= limit) {
          if(useMirror(toChooseFrom.get(i),group,5000)) {
            choosen = toChooseFrom.get(i);
            break;
          }
          else {
            toChooseFrom.remove(i);
          }
        }
      }
    }
    
    return choosen;
  }
  
  private static boolean useMirror(Mirror mirror, String group, int timeout) {
    boolean success = false;
    
    try{
      URL myUrl = new URL(mirror.getUrl() + group + "_lastupdate");
      
      
      URLConnection connection;
      connection = myUrl.openConnection();
      connection.setConnectTimeout(timeout);
      
      HttpURLConnection httpConnection = (HttpURLConnection)connection;
      int responseCode = httpConnection.getResponseCode();
      
      if(responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader read = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        String date = read.readLine();
        read.close();
        
        Date serverDate = DATE_FORMAT.parse(date);
        
        // only if update date on server is  
        success = (((System.currentTimeMillis() - serverDate.getTime()) / 1000 / 60 / 60 / 24)) <= 3; 
      }
    } catch (Exception e) {
        // Handle your exceptions
      success = false;
    }
      
    return success;
  }
}
