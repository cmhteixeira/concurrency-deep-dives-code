package com.cmhteixeira.sockets.nonblocking.httpproxy;

import com.cmhteixeira.sockets.nonblocking.crawler.GET;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

public class TestingApp {

  private SSLContext sslContext;

  private TestingApp(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  public static TestingApp make()
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
    return new TestingApp(sslContext);
  }

  public SSLEngine sslEngine() {
    return sslContext.createSSLEngine();
  }

  public static void main(String[] args)
      throws CertificateException,
          NoSuchAlgorithmException,
          KeyStoreException,
          IOException,
          KeyManagementException {
    TestingApp testingApp = TestingApp.make();
    SSLEngine sslEngine = testingApp.sslEngine();
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(true);
    URL url = new URL("https://nytimes.com");
    socketChannel.connect(new InetSocketAddress(url.getHost(), 443));
    TlsNegotiation tlsNegotiation = new TlsNegotiation(sslEngine, socketChannel);

    GET get = new GET(url.getHost(), "/");
    String result = tlsNegotiation.send(get);
    System.out.println(result);
  }
}
