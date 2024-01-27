package com.cmhteixeira.java.sockets.blocking.tcpclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

  private Helper() {}

  private static final Pattern regex = Pattern.compile("^read=([1-9][0-9]*)(kb|mb)?$");

  static int bytesToRead(String input) {
    Matcher matcher = regex.matcher(input);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Wrong input: " + input);
    }
    int value = Integer.parseInt(matcher.group(1));
    if (matcher.group(2) == null) {
      return value;
    }

    if (matcher.group(2).equalsIgnoreCase("mb")) {
      return value * 1024 * 1024;
    } else if (matcher.group(2).equalsIgnoreCase("kb")) {
      return value * 1024;
    } else {
      throw new IllegalArgumentException("Not valid: " + value);
    }
  }

  private static final Pattern regex2 = Pattern.compile("^\\+([1-9][0-9]*)$");

  public static int bytesToRead2(String input) {
    Matcher matcher = regex2.matcher(input);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Wrong input: " + input);
    }
    return Integer.parseInt(matcher.group(1));
  }
}
