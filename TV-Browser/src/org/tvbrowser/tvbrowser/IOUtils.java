package org.tvbrowser.tvbrowser;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpInetConnection;
import org.apache.http.client.methods.HttpOptions;

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
  
  public static void saveUrl(String filename, String urlString) throws MalformedURLException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try
        {
            URLConnection connection;
          
            connection = new URL(urlString).openConnection();
            
            if(filename.toLowerCase().endsWith(".gz")) {
              connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
            }
            
            in = new BufferedInputStream(connection.getInputStream());
            fout = new FileOutputStream(filename);

            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1)
            {
                fout.write(data, 0, count);
            }
        }
        finally
        {
            if (in != null)
                in.close();
            if (fout != null)
                fout.close();
        }
    }
}
