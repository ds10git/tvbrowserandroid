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
package de.epgpaid;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.PrefUtils;

import android.util.Log;

public class EPGpaidDataConnection {
  private static final Pattern FORM_PATTERN = Pattern.compile(".*?name=\"(.*?)\".*?value=\"(.*?)\".*");
  private static final String DOMAIN = "https://data.epgpaid.de/";
  private static final String REQUEST_METHOD_GET = "GET";
  private static final String REQUEST_METHOD_POST = "POST";
  
  private HttpURLConnection mHttpConnection;
  private Authenticator mAuthenticator;
  
  public EPGpaidDataConnection() {
    if(!(CookieHandler.getDefault() instanceof CookieManager)) {
      CookieHandler.setDefault(new CookieManager());
    }
    
    mAuthenticator = new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("epgpaid_login", "4BFdvBhpx8strj4".toCharArray());
      }
    };
    
   // mCookieList = new ArrayList<String>();
  }
  
  public boolean login(String userName, String password) {
    boolean result = false;
    
    Authenticator.setDefault(mAuthenticator);
    
    if(!PrefUtils.getBooleanValue(R.string.PREF_EPGPAID_CHECK_SSL, R.bool.pref_epgpaid_check_ssl_default)) {
      SSLTool.disableCertificateValidation();
    }
    
    try {
      if(openGetConnection(DOMAIN+"login_android.php") == HttpURLConnection.HTTP_OK) {
        // Read login form
        String pageContent = readPageContent(mHttpConnection);
        closeHttpConnection();
        
        if(!pageContent.isEmpty()) {
          HashMap<String, String> nameValueMap = new HashMap<String, String>();
          
          String[] lines = pageContent.split("\n");
          
          for(String line : lines) {
            Matcher m = FORM_PATTERN.matcher(line);
            
            if(m.find()) {
              nameValueMap.put(m.group(1),m.group(2));
            }
          }
          
          String passwordFieldName = nameValueMap.get("password_field_name");
          Set<String> keys = nameValueMap.keySet();
          
          StringBuilder postParameters = new StringBuilder();
          
          for(String key : keys) {
            String value = nameValueMap.get(key);
            
            if(postParameters.length() > 0) {
              postParameters.append("&");
            }
            if(key.equals("username")) {
              value = userName.trim();
            }
            else if(key.equals(passwordFieldName)) {
              value = password.trim();
            }
            
            postParameters.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
          }
          
          // post login data
          if(openPostConnection(DOMAIN+"login_android.php", postParameters.toString()) == HttpURLConnection.HTTP_OK) {
            closeHttpConnection();
            
            // test if access is granted
            result = openGetConnection(DOMAIN+"accessTest.php") == HttpURLConnection.HTTP_OK;
            closeHttpConnection();
          }
        }
      }
    }catch(Exception e) {
      e.printStackTrace();
    }
    
    Authenticator.setDefault(null);
    
    return result;
  }
  
  public boolean isLoggedIn() {
    Authenticator.setDefault(mAuthenticator);
    
    boolean result = false;
    
    try {
      result = openGetConnection(DOMAIN+"loginTest.php") == HttpURLConnection.HTTP_OK;
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    Authenticator.setDefault(null);
    
    return result;
  }
  
  public boolean download(String file, File target) {
    boolean result = false;
    
    Authenticator.setDefault(mAuthenticator);
    
    try {
      Log.d("info6", "DOWNLOAD: " + DOMAIN+file);
      if(openGetConnection(DOMAIN+file) == HttpURLConnection.HTTP_OK) {
        Log.d("info6", "CONNECTION OPENED");
        InputStream in = null;
        
        try {
          in = mHttpConnection.getInputStream();
        
          result = saveStream(in, target);
        }catch(IOException ioe1) {
          ioe1.printStackTrace();
        }finally {
          if(in != null) {
            try {
              in.close();
            }catch(IOException ioe1) {
              ioe1.printStackTrace();
            }
          }
        }
      }
    }catch(Exception e) {
      e.printStackTrace();
    }
    
    closeHttpConnection();
    
    Authenticator.setDefault(null);
    
    return result;
  }
  
  public void logout() {
    Authenticator.setDefault(mAuthenticator);
    
    try {
      openGetConnection(DOMAIN+"logout.php");
      closeHttpConnection();
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    closeHttpConnection();
    
    Authenticator.setDefault(null);
    SSLTool.resetCertificateValidation();
  }
  
  private int openPostConnection(String url, String parameter) throws Exception {
    return openConnection(url, REQUEST_METHOD_POST, parameter);
  }
  
  private int openGetConnection(String url) throws Exception {
    return openConnection(url, REQUEST_METHOD_GET, null);
  }
  
  /**
   * Based upon http://www.mkyong.com/java/how-to-automate-login-a-website-java-example/
   * <p>
   * @param url The URL to open.
   * @param requestMethod The request method.
   * @param postParameter The post parameter if any.
   * @return The response code.
   * <p>
   * @throws IOException If something goes wrong.
   */
  private int openConnection(String url, String requestMethod, String postParameter) throws Exception {
    final URL targetUrl = new URL(url);
    
    if(requestMethod == null) {
      requestMethod = REQUEST_METHOD_GET;
    }
    
    mHttpConnection = (HttpURLConnection) targetUrl.openConnection();
    mHttpConnection.setRequestMethod(requestMethod);
    mHttpConnection.setUseCaches(false);
    
    IOUtils.setConnectionTimeout(mHttpConnection,20000);
    
    if(REQUEST_METHOD_POST.equals(requestMethod) && postParameter != null) {
      mHttpConnection.setRequestProperty("Connection", "keep-alive");
      mHttpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      mHttpConnection.setRequestProperty("Content-Length", Integer.toString(postParameter.length()));

      mHttpConnection.setDoOutput(true);
      mHttpConnection.setDoInput(true);
      
      //  Send post request
      DataOutputStream post = null;
      
      try {
        post = new DataOutputStream(mHttpConnection.getOutputStream());
        post.writeBytes(postParameter);
        post.flush();
      } catch(IOException ioe) {
        ioe.printStackTrace();
      } finally {
        if(post != null) {
          try {
            post.close();
          }catch(IOException ioe) {
            ioe.printStackTrace();
          }
        }
      }
    }
    
    int responseCode = mHttpConnection.getResponseCode();
    
    if(responseCode != HttpURLConnection.HTTP_OK) {
      closeHttpConnection();
    }
    
    return responseCode;
  }
  
  private void closeHttpConnection() {
    if(mHttpConnection != null) {
      mHttpConnection.disconnect();
      mHttpConnection = null;
    }
  }
  
  /* read the page content of a established connection */
  private String readPageContent(HttpURLConnection connection) {
    StringBuilder result = new StringBuilder();
    
    if(connection != null) {
      BufferedReader in = null;
      
      try {
        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        String line = null;
        
        while((line = in.readLine()) != null) {
          result.append(line).append("\n");
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        if(in != null) {
          try {
            in.close();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
    
    return result.toString();
  }
  
  private boolean saveStream(InputStream in, File target) throws IOException {
    return IOUtils.saveStream(target.getAbsolutePath(), in);
  }
}
