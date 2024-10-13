package com.cmhteixeira.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StdInputMonitor {

  private final ConcurrentHashMap<String, CompletableFuture<String>> theMap =
      new ConcurrentHashMap<>();

  public StdInputMonitor() {
    Thread threadHandle = new Thread(this::run, "StdInputMonitor");
    threadHandle.start();
  }

  private void run() {
    while (true) {
      String line = System.console().readLine();
      theMap.forEach(
          (prefix, future) -> {
            if (line.startsWith(prefix)) future.complete(line.substring(prefix.length()));
          });
    }
  }

  public CompletableFuture<String> monitor(String prefix) {
    CompletableFuture<String> cF = new CompletableFuture<>();
    var previousFut = theMap.computeIfAbsent(prefix, s -> cF);
    if (previousFut != cF)
      return CompletableFuture.failedFuture(new IllegalArgumentException("Already registered."));

    return cF;
  }
}
