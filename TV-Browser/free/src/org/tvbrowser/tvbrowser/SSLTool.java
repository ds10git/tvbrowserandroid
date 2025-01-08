package org.tvbrowser.tvbrowser;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLTool {
  private static SSLSocketFactory mOriginalSSLSocketFactory;
  private static HostnameVerifier mOriginalHostnameVerifier;
  
  public static void disableCertificateValidation() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { 
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0]; 
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
          // ignored intentionally
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
          // ignored intentionally
        }
    }};

    // Ignore differences between given hostname and certificate hostname
    HostnameVerifier hv = (hostname, session) -> hostname.equals(session.getPeerHost());
    
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
    } catch (Exception e) {
      // ignore
    }
  }
  
  public static void resetCertificateValidation() {
    if(mOriginalSSLSocketFactory != null) {
      HttpsURLConnection.setDefaultSSLSocketFactory(mOriginalSSLSocketFactory);
    }
    if(mOriginalHostnameVerifier != null) {
      HttpsURLConnection.setDefaultHostnameVerifier(mOriginalHostnameVerifier);
    }
  }
}
