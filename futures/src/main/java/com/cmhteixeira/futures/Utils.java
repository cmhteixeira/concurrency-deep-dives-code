package com.cmhteixeira.futures;

import java.util.concurrent.*;

public class Utils {

  public static <T> CompletableFuture<T> sleepAsync(T t, long sleepMillis) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Thread.sleep(sleepMillis);
            return t;
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        });
  }

  public static <T> CompletableFuture<T> sleepAsync2(T t, long sleepMillis) {
    return CompletableFuture.supplyAsync(
        () -> t, CompletableFuture.delayedExecutor(sleepMillis, TimeUnit.MILLISECONDS));
  }
}
