package com.cmhteixeira.java.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class HttpParsing {
  private HttpParsing() {
    throw new IllegalCallerException("Static class. Cannot be instantiated");
  }

  public static RequestLine parseRequestLine(InputStreamReader inputStream) throws IOException {
    HttpVerb verb = parseMethod(inputStream);
    String uri = parseUri(inputStream);
    HttpVersion httpVersion = parseVersion(inputStream);
    return new RequestLine(verb, uri, httpVersion);
  }

  static String untilSeparator(InputStreamReader dataInputStream, char sep) throws IOException {
    StringBuilder maybeVerb = new StringBuilder();
    while (true) {
      int res = dataInputStream.read();
      if (res != -1) {
        char maybeChar = (char) res;
        if (maybeChar == sep) {
          break;
        } else {
          maybeVerb.append(maybeChar);
        }
      } else {
        throw new IllegalArgumentException("End of stream");
      }
    }

    return maybeVerb.toString();
  }

  static String untilCRLF(InputStreamReader dataInputStream) throws IOException {
    StringBuilder maybeVerb = new StringBuilder();
    while (true) {
      int res = dataInputStream.read();
      if (res != -1) {
        char maybeChar = (char) res;
        if (maybeChar == '\r') {
          int res2 = dataInputStream.read();
          if (res2 == -1) {
            throw new IllegalArgumentException("End of stream");
          } else {
            char maybeChar2 = (char) res2;
            if (maybeChar2 == '\n') {
              break;
            } else {
              throw new IllegalArgumentException("LF not followed by CR.");
            }
          }
        } else {
          maybeVerb.append(maybeChar);
        }
      } else {
        throw new IllegalArgumentException("End of stream");
      }
    }

    return maybeVerb.toString();
  }

  static HttpVerb parseMethod(InputStreamReader dataInputStream) throws IOException {
    return HttpVerb.fromString(untilSeparator(dataInputStream, ' '));
  }

  static String parseUri(InputStreamReader dataInputStream) throws IOException {
    return untilSeparator(dataInputStream, ' ');
  }

  static HttpVersion parseVersion(InputStreamReader dataInputStream) throws IOException {
    String res = untilSeparator(dataInputStream, '\r');
    int maybeNewLine = dataInputStream.read();
    if (maybeNewLine != -1) {
      if (maybeNewLine == '\n') {
        return HttpVersion.fromString(res);
      } else {
        throw new IllegalArgumentException("Received CR not followed by LF.");
      }
    } else {
      throw new IllegalArgumentException("End of stream");
    }
  }

  static List<String> parseHeaderSection() {
    return null;
  }

  public static Map.Entry<String, String> parseHeader(InputStreamReader dataInputStream)
      throws IOException {
    String name = untilSeparator(dataInputStream, ':');
    String value = untilCRLF(dataInputStream);
    String valueStripped = value.strip();
    return Map.entry(name, valueStripped);
  }

  public static Map.Entry<String, String> parseHeader2(String fieldLine) {
    int index = fieldLine.indexOf(':');
    if (index == -1) {
      throw new IllegalArgumentException(
          String.format("Field Line does not have a colon. '%s'", fieldLine));
    }
    String name = fieldLine.substring(0, index);
    if (name.isBlank()) {
      throw new IllegalArgumentException("Field name is blank.");
    }
    String valueStripped = fieldLine.substring(index + 1).strip();
    return Map.entry(name, valueStripped);
  }

  public static Map<String, String> parseHeaders(InputStreamReader dataInputStream)
      throws IOException {
    HashMap<String, String> headers = new HashMap<>();
    while (true) {
      String line = untilCRLF(dataInputStream);
      if (line.isEmpty()) {
        return headers;
      } else {
        var entry = parseHeader2(line);
        headers.put(entry.getKey(), entry.getValue());
      }
    }
  }
}
