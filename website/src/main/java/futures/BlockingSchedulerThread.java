package futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BlockingSchedulerThread {

  private static long startReference = System.currentTimeMillis();

  private static int lapsedS() {
    return (int) ((System.currentTimeMillis() - startReference) / 1000L);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void printResultAndThread(String result) {
    System.out.printf("%s # %s # %ds\n", result, Thread.currentThread().getName(), lapsedS());
  }

  public static void main(String[] args) {

    Future<String> pF1 = Utils.sleepAsync2("Concurrency Deep Dives - 1", 10_000L);
    Future<String> pF2 = Utils.sleepAsync2("Concurrency Deep Dives - 2", 12_000L);
    Future<String> pF3 = Utils.sleepAsync2("Concurrency Deep Dives - 3", 13_000L);

    CompletableFuture<String> cF1 = FutureToCompletableFuture.poll(pF1);
    CompletableFuture<String> cF2 = FutureToCompletableFuture.poll(pF2);
    CompletableFuture<String> cF3 = FutureToCompletableFuture.poll(pF3);

    cF1.whenComplete(
        (result, exception) -> {
          printResultAndThread(result);
          sleep(21_000L);
        });

    cF2.whenCompleteAsync((result, exception) -> printResultAndThread(result));
    cF3.whenCompleteAsync((result, exception) -> printResultAndThread(result));
  }
}
