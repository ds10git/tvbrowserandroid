/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.res.Resources;

/**
 * Helper class to supper IO.
 * <p>
 * @author René Mach
 */
public class IOUtils {
  
  /**
   * Creates an integer value from the given byte array.
   * <p>
   * @param value The byte array to convert (Big-Endian).
   * @return The calculated integer value.
   */
  public static int getIntForBytes(byte[] value) {
    int count = value.length - 1;
    
    int result = 0;
    
    for(byte b : value) {
      result = result | ((((int)b) & 0xFF) << (count * 8));
      
      count--;
    }
    
    return result;
  }
  
  public static String getInfoString(int value, Resources res) {
    StringBuilder infoString = new StringBuilder();
    
    String[] valueArr = {"",
        res.getString(R.string.info_black_and_white),
        res.getString(R.string.info_four_to_three),
        res.getString(R.string.info_sixteen_to_nine),
        res.getString(R.string.info_mono),
        res.getString(R.string.info_stereo),
        res.getString(R.string.info_dolby_sourround),
        res.getString(R.string.info_dolby_digital),
        res.getString(R.string.info_second_audio_program),
        res.getString(R.string.info_closed_caption),
        res.getString(R.string.info_live),
        res.getString(R.string.info_omu),
        res.getString(R.string.info_film),
        res.getString(R.string.info_series),
        res.getString(R.string.info_new),
        res.getString(R.string.info_audio_description),
        res.getString(R.string.info_news),
        res.getString(R.string.info_show),
        res.getString(R.string.info_magazin),
        res.getString(R.string.info_hd),
        res.getString(R.string.info_docu),
        res.getString(R.string.info_art),
        res.getString(R.string.info_sport),
        res.getString(R.string.info_children),
        res.getString(R.string.info_other),
        res.getString(R.string.info_sign_language)
        };
    
    for(int i = 1; i <= 25; i++) {
      if((value & (1 << i)) == (1 << i)) {
        if(infoString.length() > 0) {
          infoString.append(", ");
        }
        
        infoString.append(valueArr[i]);
      }
    }
    
    return infoString.toString().trim();
  }
  
  public static byte[] loadUrl(String urlString) throws MalformedURLException, IOException {
    BufferedInputStream in = null;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    try {
      URLConnection connection;
      
      connection = new URL(urlString).openConnection();
      
      if(urlString.toLowerCase().endsWith(".gz")) {
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
      }
      
      in = new BufferedInputStream(connection.getInputStream());
      
      byte temp[] = new byte[1024];
      int count;
      
      while ((count = in.read(temp, 0, 1024)) != -1) {
        out.write(temp, 0, count);
      }
    } 
    finally {
      if (in != null) {
        in.close();
      }
    }

    return out.toByteArray();

  }
  
  public static void saveUrl(String filename, String urlString) throws MalformedURLException, IOException {
    FileOutputStream fout = null;
    
    try {
      byte[] byteArr = loadUrl(urlString);
      
      fout = new FileOutputStream(filename);
      fout.write(byteArr, 0, byteArr.length);
    }
    finally {
      if (fout != null) {
        fout.close();
      }
    }
  }
}
