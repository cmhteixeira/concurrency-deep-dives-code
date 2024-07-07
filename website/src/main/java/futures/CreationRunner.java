package futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CreationRunner {
  public static void main(String[] args) throws InterruptedException, ExecutionException {

    CompletableFuture<String> cF1 =
        CompletableFuture.supplyAsync(
            () -> {
              throw new IllegalStateException("kabOOmm!");
            });

    CompletableFuture<String> cF4 =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                Thread.sleep(10_000L);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              return "Concurrency Deep Dives";
            });

    CompletableFuture<String> cF3 =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                Thread.sleep(10_000L);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              return "Concurrency Deep Dives";
            });

    CompletableFuture<Void> cF2 =
        CompletableFuture.runAsync(
            () -> {
              // do something useful
              try {
                Thread.sleep(10_000L);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              System.out.println("Running");
            });

    boolean wasCompleted = cF3.completeExceptionally(new RuntimeException("kabOOM!"));

    String result = cF3.get();
    System.out.printf("Completed from outside: %s. Result: %s", wasCompleted, result);
  }
}
