package com.cmhteixeira.futures;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SortList {
  public static List<Integer> generateRandomList(long size) {
    Random random = new Random();
    return random.ints(size, 0, 50_000_000).boxed().collect(Collectors.toList());
  }

  public static void main(String[] args) {
    long now = System.currentTimeMillis();
    List<Integer> unsorterList = generateRandomList(10_000_000L);
    CompletableFuture<Integer> cF1 =
        CompletableFuture.supplyAsync(() -> unsorterList.stream().sorted().toList().get(0));

    System.out.printf("Here ... %d.\n", (System.currentTimeMillis() - now) / 1000L);
    var fo = cF1.join();
    System.out.printf("Result: %d ... %d.\n", fo, (System.currentTimeMillis() - now) / 1000L);
    System.out.println(unsorterList.subList(0, 10));
  }
}
