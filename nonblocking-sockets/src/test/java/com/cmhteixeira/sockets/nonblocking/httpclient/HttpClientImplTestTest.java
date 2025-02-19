package com.cmhteixeira.sockets.nonblocking.httpclient;

import com.cmhteixeira.sockets.nonblocking.httpproxy.TestingApp;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import rawhttp.core.*;

public class HttpClientImplTestTest {

  private static HttpClient httpClient;

  @BeforeAll
  static void setUp()
      throws CertificateException,
          NoSuchAlgorithmException,
          KeyStoreException,
          IOException,
          KeyManagementException {
    httpClient = new HttpClientImpl(TestingApp.defaultSslContext());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        //        "www.foreignaffairs.com",
        //        "www.foreignpolicy.com",
        //        "www.observador.pt",
        //        "www.latimes.com",
        "www.nytimes.com",
        "www.sapo.pt",
        "www.ft.com",
        //        "www.economist.com",
        //        "www.wsj.com",
        //        "www.reuters.com",
        "www.bloomberg.com",
        //        "www.washingtonpost.com",
      })
  void httpsRequests(String host) throws IOException, InterruptedException, URISyntaxException {
    URI uri = new URI("https", null, host, 443, "/", null, null);
    RawHttpResponse<Void> response =
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
    System.out.println(response);
  }
}
