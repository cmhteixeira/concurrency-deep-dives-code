package com.cmhteixeira.sockets.nonblocking.httpclient;

import com.cmhteixeira.sockets.nonblocking.httpproxy.TestingApp;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        "www.foreignaffairs.com",
        "www.foreignpolicy.com",
        "www.observador.pt",
        "www.latimes.com",
        "www.nytimes.com",
        "www.sapo.pt",
        "www.ft.com",
        "www.economist.com",
        "www.wsj.com",
        "www.reuters.com",
        "www.bloomberg.com",
      })
  void httpsRequests(String host) throws Exception {
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

  @ParameterizedTest
  @MethodSource("hostsAndBufferSizes")
  void httpsRequests2(String host, int a, int b, int c) throws Exception {
    URI uri = new URI("https", null, host, 443, "/", null, null);
    HttpClient httpClient2 =
        new HttpClientImpl(
            TestingApp.defaultSslContext(), a * 16 * 1024, b * 16 * 1024, c * 16 * 1024);
    RawHttpResponse<Void> response =
        httpClient2.send(
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

  private static Stream<Arguments> hostsAndBufferSizes() {
    List<String> hosts =
        List.of(
            "www.foreignaffairs.com",
            "www.foreignpolicy.com",
            "www.observador.pt",
            "www.latimes.com",
            "www.nytimes.com",
            "www.sapo.pt",
            "www.ft.com",
            "www.economist.com",
            "www.wsj.com",
            "www.reuters.com",
            "www.bloomberg.com");

    List<Integer> oneToTen = IntStream.range(1, 4).boxed().toList();

    return hosts.stream()
        .flatMap(
            host ->
                oneToTen.stream()
                    .flatMap(
                        a ->
                            oneToTen.stream()
                                .flatMap(
                                    b -> oneToTen.stream().map(c -> Arguments.of(host, a, b, c)))));
  }
}
