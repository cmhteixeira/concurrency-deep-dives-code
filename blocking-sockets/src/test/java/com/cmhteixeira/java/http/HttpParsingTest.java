package com.cmhteixeira.java.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class HttpParsingTest {
  @Test
  void doThis() throws IOException {
    assertEquals(
        HttpParsing.parseRequestLine(
            new InputStreamReader(
                new ByteArrayInputStream(
                    "CONNECT foo:80 HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8)))),
        new RequestLine(new HttpVerb.CONNECT(), "foo:80", new HttpVersion.Http1_1()));
  }
}
