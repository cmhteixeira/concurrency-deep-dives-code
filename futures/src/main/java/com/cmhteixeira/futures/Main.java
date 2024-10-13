package com.cmhteixeira.futures;

import java.util.concurrent.CompletableFuture;

public class Main {
  public static void main(String[] args) {
    CompletableFuture<String> failedFuture =
        CompletableFuture.failedFuture(new IllegalAccessException("kaBoOm!"));
    CompletableFuture<String> successFuture =
        CompletableFuture.completedFuture("Concurrency Deep Dives");

    CompletableFuture<String> cF1 = CompletableFuture.completedFuture("Concurrency Deep Dives");

    cF1.cancel(true);

    CompletableFuture<String> cF40 = cF1.exceptionallyCompose(null);
    CompletableFuture<String> cF41 = cF1.exceptionallyComposeAsync(null);
    CompletableFuture<String> cF42 = cF1.exceptionallyComposeAsync(null, null);

    CompletableFuture<String> cF37 = cF1.exceptionally(null);
    CompletableFuture<String> cF38 = cF1.exceptionallyAsync(null);
    CompletableFuture<String> cF39 = cF1.exceptionallyAsync(null);

    CompletableFuture<Void> cF2 = cF1.thenAccept(String::length);
    CompletableFuture<Void> cF3 = cF1.thenAcceptAsync(String::length);
    CompletableFuture<Void> cF4 = cF1.thenAcceptAsync(String::length, null);

    CompletableFuture<Void> cF5 = cF1.thenCombine(cF1, null);
    CompletableFuture<Void> cF6 = cF1.thenCombineAsync(cF1, null);
    CompletableFuture<Void> cF7 = cF1.thenCombineAsync(cF1, null, null);

    CompletableFuture<Integer> cF8 = cF1.handle((a, b) -> 3);
    CompletableFuture<Integer> cF9 = cF1.handleAsync((a, b) -> 3);
    CompletableFuture<Integer> cF10 = cF1.handleAsync((a, b) -> 3, null);

    CompletableFuture<Integer> cF11 = cF1.thenApply(String::length);
    CompletableFuture<Integer> cF12 = cF1.thenApplyAsync(String::length);
    CompletableFuture<Integer> cF13 = cF1.thenApplyAsync(String::length, null);

    CompletableFuture<Integer> cF14 = cF1.applyToEither(cF1, String::length);
    CompletableFuture<Integer> cF15 = cF1.applyToEitherAsync(cF1, String::length);
    CompletableFuture<Integer> cF16 = cF1.applyToEitherAsync(cF1, String::length, null);

    CompletableFuture<Integer> cF17 = cF1.thenCompose(null);
    CompletableFuture<Integer> cF18 = cF1.thenComposeAsync(null);
    CompletableFuture<Integer> cF19 = cF1.thenComposeAsync(null, null);

    CompletableFuture<Integer> cF23 = cF1.thenCompose(null);
    CompletableFuture<Integer> cF24 = cF1.thenComposeAsync(null);
    CompletableFuture<Integer> cF25 = cF1.thenComposeAsync(null, null);

    CompletableFuture<String> cF26 = cF1.completeAsync(null);
    CompletableFuture<String> cF27 = cF1.completeAsync(null, null);

    CompletableFuture<String> cF28 = CompletableFuture.supplyAsync(null);
    CompletableFuture<String> cF29 = CompletableFuture.supplyAsync(null, null);

    CompletableFuture<Void> cF30 = CompletableFuture.runAsync(null);
    CompletableFuture<Void> cF31 = CompletableFuture.runAsync(null, null);

    CompletableFuture<Void> cF32 = CompletableFuture.allOf();
    CompletableFuture<Object> cF33 = CompletableFuture.anyOf(cF1, cF1, cF3);

    CompletableFuture<String> cF34 = cF1.whenComplete(null);
    CompletableFuture<String> cF35 = cF1.whenCompleteAsync(null);
    CompletableFuture<String> cF36 = cF1.whenCompleteAsync(null, null);

    //    CompletableFuture<Object> cF34 = cF1.unipush();
  }
}
