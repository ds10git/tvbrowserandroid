/*
 * EPGpaid data: A supplement data plugin for TV-Browser.
 * Copyright: (c) 2015 René Mach
 */
package de.epgpaid;

import android.util.Log;

import org.tvbrowser.utils.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EPGpaidDataConnection {
  private static final String TAG = "EPGpaid";

  public static final String KEY_NO_CONNECTION = "NO_CONNECTION_TO_LOGIN_SITE";
  public static final String KEY_NO_CONNECTION_TO_INTERNET = "NO_CONNECTION_TO_INTERNET";
  
  private static final Pattern PATTERN_FORM = Pattern.compile(".*?name=\"(.*?)\".*?value=\"(.*?)\".*");
  private static final Pattern PATTERN_DATE_UNTIL = Pattern.compile(".*?Der Zugang zu den EPGpaid-Daten, ist noch bis zum (\\d+)\\.(\\d+)\\.(\\d+) aktiv.*?");
  private static final Pattern PATTERN_DATE_EXPIRED = Pattern.compile(".*?Der Nutzungszeitraum für die EPGpaid-Daten, ist am (\\d+)\\.(\\d+)\\.(\\d+) abgelaufen.*?");
  private static final Pattern PATTERN_DATE_UNLOCK = Pattern.compile(".*?Der Zugang für die EPGpaid-Daten wird am (\\d+)\\.(\\d+)\\.(\\d+) freigeschaltet und läuft dann bis zum (\\d+)\\.(\\d+)\\.(\\d+).*?");
  
  private static final String DOMAIN = "https://data.epgpaid.de/";
  private static final String REQUEST_METHOD_GET = "GET";
  private static final String REQUEST_METHOD_POST = "POST";
  
  private HttpURLConnection mHttpConnection;
  private Authenticator mAuthenticator;
  private CookieHandler mCookieHandlerDefault; 
  
  private String mSessionId;
  
  public EPGpaidDataConnection() {
    //if(!(CookieHandler.getDefault() instanceof CookieManager)) {
      //CookieHandler.setDefault(new CookieManager());
    //}
    mSessionId = null;
    
    mAuthenticator = new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("epgpaid_login", "4BFdvBhpx8strj4".toCharArray());
      }
    };
    
   // mCookieList = new ArrayList<String>();
  }
  
  public boolean loginBool(final String userName, final String password) {
    return "true".equals(login(userName,password));
  }
  
  public String login(final String userName, final String password) {
    final StringBuilder result = new StringBuilder();
    
    boolean loggedIn = false;
    
    mSessionId = null;
    mCookieHandlerDefault = CookieHandler.getDefault();
    CookieHandler.setDefault(null);
    Authenticator.setDefault(mAuthenticator);
    
    try {
      int responseCode = openGetConnection(DOMAIN+"login.php");
      
      if(responseCode == HttpURLConnection.HTTP_OK) {
        // Read login form
        String pageContent = readPageContent(mHttpConnection);
        closeHttpConnection();
        
        if(pageContent.contains("<title>data.epgpaid.de: Anmeldung erforderlich</title>")) {
          HashMap<String, String> nameValueMap = new HashMap<String, String>();
          
          String[] lines = pageContent.split("\n");
          
          for(String line : lines) {
            Matcher m = PATTERN_FORM.matcher(line);
            
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
              value = userName;
            }
            else if(key.equals(passwordFieldName)) {
              value = password;
            }
            
            postParameters.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
          }
          
          // post login data
          responseCode = openPostConnection(DOMAIN+"login.php", postParameters.toString());
          
          if(responseCode == HttpURLConnection.HTTP_OK) {
            final String content = readPageContent(mHttpConnection);
            
            Matcher date = PATTERN_DATE_UNTIL.matcher(content);
            
            long until = 0;
            
            if(date.find() && date.groupCount() == 3) {
              until = updateUntilDate(date);
              EPGpaidData.setDateValue(EPGpaidData.TYPE_DATE_FROM,0);
            }
            else {
              date = PATTERN_DATE_EXPIRED.matcher(content);
              
              if(date.find() && date.groupCount() == 3) {
                until = updateUntilDate(date);
                EPGpaidData.setDateValue(EPGpaidData.TYPE_DATE_FROM,0);
              }
              else {
                date = PATTERN_DATE_UNLOCK.matcher(content);
                
                if(date.find() && date.groupCount() == 6) {
                  updateUntilDate(date,0,EPGpaidData.TYPE_DATE_FROM);
                  until = updateUntilDate(date,1,EPGpaidData.TYPE_DATE_UNTIL);
                }
              }
            }
            
            closeHttpConnection();
            
            try {
              Thread.sleep(400);
            }catch(InterruptedException ie) {
              //ignore
            }
            
            // test if access is granted
            responseCode = openGetConnection(DOMAIN+"accessTest.php");
            
            if(responseCode == HttpURLConnection.HTTP_OK || until != 0) {
              result.append("true"); 
              loggedIn = true;
            }
            else {
              Log.d(TAG, "No ACCESS to EPGpaid data with response code: " + responseCode);
            }
            
            closeHttpConnection();
          }
          else {
            Log.d(TAG, "No LOGIN with response code: " + responseCode);
          }
        }
        else {
          result.append(KEY_NO_CONNECTION);
          Log.d(TAG, "Login site not reached");
        }
      }
      else {
        result.append(KEY_NO_CONNECTION);
        Log.d(TAG, "No login connection with response code: " + responseCode);
      }
    }catch(Throwable t) {
      if(t instanceof UnknownHostException) {
        result.append(KEY_NO_CONNECTION_TO_INTERNET);
        Log.d(TAG, "No connection to Internet with UnknownHostException.");
      }
      else if(t instanceof SocketException) {
        result.append(KEY_NO_CONNECTION_TO_INTERNET);
        Log.d(TAG, "No connection to Internet with SocketException.");
      }
      else {
        Log.d(TAG, "EPGpaidDataConnection login error", t);
        
        result.append("ERROR MESSAGE:\n\n");
        result.append(t.getMessage()).append("\n");

          final StackTraceElement[] els = t.getStackTrace();
        
        for(StackTraceElement el : els) {
          result.append(el.toString()).append("\n");
        }
      }
    }
    
    Authenticator.setDefault(null);
    
    if(!loggedIn) {
      CookieHandler.setDefault(mCookieHandlerDefault);
      mSessionId = null;
    }
    
    return result.toString();
  }
  
  private long updateUntilDate(final Matcher date) {
    return updateUntilDate(date, 0, EPGpaidData.TYPE_DATE_UNTIL);
  }
  
  private long updateUntilDate(final Matcher date, final int group, final int dateType) {
    final Calendar cal = Calendar.getInstance();
    cal.set(Integer.parseInt(date.group(3+group*3)), Integer.parseInt(date.group(2+group*3))-1, Integer.parseInt(date.group(1+group*3)));
    cal.set(Calendar.HOUR_OF_DAY, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 999);
    
    if(cal.getTimeInMillis() > (System.currentTimeMillis() - (2*365*24*60*60000L))) {
      EPGpaidData.setDateValue(dateType, cal.getTimeInMillis());
    }
    
    return cal.getTimeInMillis();
  }
  
  public String isLoggedIn() {
    Authenticator.setDefault(mAuthenticator);
    
    final StringBuilder result = new StringBuilder();
    
    try {
      if(openGetConnection(DOMAIN+"loginTest.php") == HttpURLConnection.HTTP_OK) {
        result.append("true");
      }
    } catch (Throwable t) {
      Log.d(TAG, "EPGpaidDataConnection isLoggedIn error", t);
      
      result.append("ERROR MESSAGE:\n\n");
      result.append(t.getMessage()).append("\n");
      
      final StackTraceElement[] els = t.getStackTrace();
      
      for(StackTraceElement el : els) {
        result.append(el.toString()).append("\n");
      }
    }
    
    Authenticator.setDefault(null);
    
    return result.toString();
  }
  
  public boolean download(String file, File target) {
    boolean result = false;
    
    Authenticator.setDefault(mAuthenticator);
    
    try {
      if(openGetConnection(DOMAIN+file) == HttpURLConnection.HTTP_OK) {
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
    }catch(Throwable t) {
      Log.d(TAG, "EPGpaidDataConnection download error", t);
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
    } catch (Throwable t) {
      Log.d(TAG, "EPGpaidDataConnection logout error", t);
    }
    
    closeHttpConnection();
    
    Authenticator.setDefault(null);
    
    CookieHandler.setDefault(mCookieHandlerDefault);
    
    mSessionId = null;
  }
  
  private int openPostConnection(String url, String parameter) throws Throwable {
    return openConnection(url, REQUEST_METHOD_POST, parameter);
  }
  
  private int openGetConnection(String url) throws Throwable {
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
   * @throws Throwable If something goes wrong.
   */
  private int openConnection(String url, String requestMethod, String postParameter) throws Throwable {
    final URL targetUrl = new URL(url);
    
    if(requestMethod == null) {
      requestMethod = REQUEST_METHOD_GET;
    }
    
    mHttpConnection = (HttpURLConnection) targetUrl.openConnection();
    mHttpConnection.setInstanceFollowRedirects(false);
    mHttpConnection.setRequestMethod(requestMethod);
    mHttpConnection.setUseCaches(false);

    if(mSessionId != null) {
      mHttpConnection.setRequestProperty("Cookie", mSessionId);
    }
    
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
    
    mHttpConnection.connect();
    
    int responseCode = mHttpConnection.getResponseCode();
    
    final String location = mHttpConnection.getHeaderField("Location");
    final String cookie = mHttpConnection.getHeaderField("Set-Cookie");
    
    if(cookie != null && cookie.contains(";")) {
	  mSessionId = cookie.substring(0,cookie.indexOf(";"));
		
	  if(mSessionId.endsWith("deleted")) {
	    mSessionId = null;
	  }
	  else {
      Log.d(TAG, "Cookies found");
	  }
	}
	    
    if(responseCode != HttpURLConnection.HTTP_OK) {
      closeHttpConnection();
    }
    
    if((responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
    		responseCode == HttpURLConnection.HTTP_MOVED_TEMP) && location != null) {
      Log.d(TAG, "Redirected");
      responseCode = openGetConnection(location);
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
        e.printStackTrace();
      } finally {
        IOUtils.close(in);
      }
    }
    
    return result.toString();
  }
  
  private boolean saveStream(InputStream in, File target) {
    return IOUtils.saveStream(target.getAbsolutePath(), in);
  }
}
