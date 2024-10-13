package com.cmhteixeira.futures;

import java.util.concurrent.CompletableFuture;

public class ExceptionHandling {
  public static void main(String[] args) {
    CompletableFuture<String> cF1 =
        CompletableFuture.supplyAsync(() -> "ConcurrencyDeepDives.com")
            .thenCompose(
                p -> {
                  if (p.length() > 4)
                    return CompletableFuture.failedFuture(new RuntimeException("kaBoom!"));
                  return CompletableFuture.completedFuture(p);
                });

    cF1.whenComplete(
        (value, ex) -> {
          if (ex != null) ex.printStackTrace();
          else System.out.println("Success!");
        });
  }
}
