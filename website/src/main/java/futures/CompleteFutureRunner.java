package futures;

import java.util.concurrent.CompletableFuture;

public class CompleteFutureRunner {

  private static final long start = System.currentTimeMillis();

  private static long duration() {
    return (System.currentTimeMillis() - start) / 1000L;
  }

  private static void sleepAndLog(String name, long sleepSeconds) {
    System.out.printf("%d s. Starting %s.\n", duration(), name);
    try {
      Thread.sleep(sleepSeconds*1000L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    System.out.printf("%d s. Ending %s.\n", duration(), name);
  }

  public static void main(String[] args) throws InterruptedException {
    long cF1Sleep = Long.parseLong(args[0]);
    long cF2Sleep = Long.parseLong(args[1]);
    long mainThreadSleep = Long.parseLong(args[2]);

    CompletableFuture<Void> cF1 = CompletableFuture.runAsync(() -> sleepAndLog("cF1", cF1Sleep));

    CompletableFuture<Void> cF2 = cF1.thenRun(() -> sleepAndLog("cF2", cF2Sleep));

    cF2.whenCompleteAsync(
        (ignored, ex) -> {
          long elapsed = duration();
          if (ex != null) System.out.printf("%d s. cF2 failed with: %s\n", elapsed, ex);
          else System.out.printf("%d s. cF2 succeeded\n", elapsed);
        });

    Thread.sleep(mainThreadSleep*1000L);
    boolean isCancelled = cF2.cancel(true);
    System.out.printf("%d s. Was cF2 cancelled: %s\n", duration(), isCancelled);
    Thread.sleep(100_000L);
  }
}
