package com.cmhteixeira.java.sockets.blocking.staticfileserver;

import java.util.List;
import java.util.stream.Collectors;

public class HtmlTemplating {
  private HtmlTemplating() {}

  public static String getHtml(List<String> fileNames) {
    return """
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
   "http://www.w3.org/TR/html4/strict.dtd">
  <HTML>
     <HEAD>
        <TITLE>Concurrency Deep Dives - Static File Server</TITLE>
     </HEAD>
     <BODY>
        <ul>
          %s
        </ul>
     </BODY>
  </HTML>
"""
        .formatted(
            fileNames.stream()
                .map(fN -> String.format("<li>%s</li>", fN))
                .collect(Collectors.joining("\n")));
  }
}
