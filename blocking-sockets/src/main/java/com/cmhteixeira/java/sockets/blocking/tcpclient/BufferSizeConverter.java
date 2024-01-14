package com.cmhteixeira.java.sockets.blocking.tcpclient;

import picocli.CommandLine;

public class BufferSizeConverter implements CommandLine.ITypeConverter<Integer> {

  public BufferSizeConverter() {}

  @Override
  public Integer convert(String value) {
    if (value.endsWith("k")) {
      return Integer.parseInt(value.substring(0, value.length() - 1)) * 1024;
    } else if (value.endsWith("m")) {
      return Integer.parseInt(value.substring(0, value.length() - 1)) * 1024 * 1024;
    } else {
      return Integer.parseInt(value);
    }
  }
}
