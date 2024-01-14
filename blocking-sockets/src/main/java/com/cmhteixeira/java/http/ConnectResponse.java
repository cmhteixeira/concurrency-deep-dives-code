package com.cmhteixeira.java.http;

import java.nio.charset.StandardCharsets;

public record ConnectResponse(int statusCode, String reasonPhrase) {
  public String toHttpFormat() {
    StringBuilder stringBuilder = new StringBuilder();
    StatusLine statusLine = new StatusLine(new HttpVersion.Http1_1(), statusCode, reasonPhrase);
    stringBuilder.append(statusLine);
    stringBuilder.append("\r\n");
    stringBuilder.append("\r\n");
    return stringBuilder.toString();
  }

  public byte[] toBytes() {
    return toHttpFormat().getBytes(StandardCharsets.US_ASCII);
  }
}
