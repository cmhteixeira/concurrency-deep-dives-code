package com.cmhteixeira.java.http;

record StatusLine(HttpVersion httpVersion, int statusCode, String reasonPhrase) {
  @Override
  public String toString() {
    return httpFormat();
  }

  public String httpFormat() {
    if (reasonPhrase == null || reasonPhrase.isBlank()) {
      return String.format("%s %d ", httpVersion.toString(), statusCode);
    } else {
      return String.format("%s %d %s", httpVersion.toString(), statusCode, reasonPhrase);
    }
  }
}
