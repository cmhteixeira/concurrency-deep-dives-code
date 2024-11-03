package com.cmhteixeira.sockets.nonblocking.crawler;

public record GET(String host, String path) {

  private static final String newLine = "\r\n";

  public String encodeHttpRequest() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("GET ").append(path).append(" HTTP/1.1").append(newLine);

    stringBuilder.append("Host: ").append(host).append(newLine);
    stringBuilder.append("User-Agent: ").append("curl/7.68.0").append(newLine);
    stringBuilder.append("Accept: */*").append(newLine);
    stringBuilder.append("Connection: close").append(newLine);
    stringBuilder.append(newLine);
    return stringBuilder.toString();
  }
}
