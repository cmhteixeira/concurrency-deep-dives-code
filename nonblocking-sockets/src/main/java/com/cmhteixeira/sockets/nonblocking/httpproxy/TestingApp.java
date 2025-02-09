package com.cmhteixeira.sockets.nonblocking.httpproxy;

import com.cmhteixeira.sockets.nonblocking.httpclient.HttpClient;
import com.cmhteixeira.sockets.nonblocking.httpclient.HttpClientImpl;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Scanner;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import rawhttp.core.*;

public class TestingApp {

  public static SSLContext defaultSslContext()
      throws NoSuchAlgorithmException,
          KeyManagementException,
          KeyStoreException,
          IOException,
          CertificateException {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    SecureRandom secureRandom = new SecureRandom();

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(
        new FileInputStream(
            Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts").toFile()),
        "changeit".toCharArray());
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
    tmf.init(keyStore);

    sslContext.init(null, tmf.getTrustManagers(), secureRandom);
    return sslContext;
  }

  public static void main(String[] args)
      throws CertificateException,
          NoSuchAlgorithmException,
          KeyStoreException,
          IOException,
          KeyManagementException,
          URISyntaxException,
          InterruptedException {
    Scanner scanner = new Scanner(System.in);
    String host = scanner.nextLine();
    URI uri = new URI("https", null, host, 443, "/", null, null);
    HttpClient httpClient = new HttpClientImpl(TestingApp.defaultSslContext());

    RawHttpResponse<Void> res =
        httpClient.send(
            new RawHttpRequest(
                new RequestLine("GET", uri, HttpVersion.HTTP_1_1),
                RawHttpHeaders.newBuilder()
                    .with("User-Agent", "curl/7.68.0")
                    .with("Accept", "*/*")
                    .with("Connection", "close")
                    .with("Host", uri.getHost())
                    .build(),
                null,
                null));

    System.out.println(res);
  }
}
