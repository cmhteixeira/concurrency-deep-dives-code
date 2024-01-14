package com.cmhteixeira.java.http;

import java.util.Objects;

public sealed interface HttpVersion
    permits HttpVersion.Http1_0, HttpVersion.Http1_1, HttpVersion.Http2_0 {

  static HttpVersion fromString(String input) {
    if (Objects.equals(input, "HTTP/1.0")) return new Http1_0();
    if (Objects.equals(input, "HTTP/1.1")) return new Http1_1();
    if (Objects.equals(input, "HTTP/2.0")) return new Http2_0();
    else
      throw new IllegalArgumentException(
          String.format("The input '%s' is not a valid HTTP version", input));
  }

  record Http1_0() implements HttpVersion {
    @Override
    public String toString() {
      return "HTTP/1.0";
    }
  }

  record Http1_1() implements HttpVersion {
    @Override
    public String toString() {
      return "HTTP/1.1";
    }
  }

  record Http2_0() implements HttpVersion {
    @Override
    public String toString() {
      return "HTTP/2.0";
    }
  }
}
