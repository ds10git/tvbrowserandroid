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
/*
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.test.MoreAsserts;
import android.util.Log;
*/
public class SSLTool {
  /*private static SSLSocketFactory mOriginalSSLSocketFactory;
  private static HostnameVerifier mOriginalHostnameVerifier;
  
  public static void disableCertificateValidation() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { 
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0]; 
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }};

    // Ignore differences between given hostname and certificate hostname
    HostnameVerifier hv = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return hostname.equals(session.getPeerHost());
		}
	};
    
    // Install the all-trusting trust manager
    try {
      if(mOriginalSSLSocketFactory == null) {
        mOriginalSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
      }
      if(mOriginalHostnameVerifier == null) {
        mOriginalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
      }
      
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(hv);
    } catch (Exception e) {}
  }
  
  public static void resetCertificateValidation() {
    if(mOriginalSSLSocketFactory != null) {
      HttpsURLConnection.setDefaultSSLSocketFactory(mOriginalSSLSocketFactory);
    }
    if(mOriginalHostnameVerifier != null) {
      HttpsURLConnection.setDefaultHostnameVerifier(mOriginalHostnameVerifier);
    }
  }*/
}
