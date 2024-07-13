package futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CreationRunner {
  public static void main(String[] args) throws InterruptedException, ExecutionException {

    //    CompletableFuture<String> cF1 =
    //        CompletableFuture.supplyAsync(
    //            () -> {
    //              throw new IllegalStateException("kabOOmm!");
    //            });
    //
    //    CompletableFuture<String> cF4 =
    //        CompletableFuture.supplyAsync(
    //            () -> {
    //              try {
    //                Thread.sleep(10_000L);
    //              } catch (InterruptedException e) {
    //                throw new RuntimeException(e);
    //              }
    //              return "Concurrency Deep Dives";
    //            });
    //
    //    CompletableFuture<String> cF3 =
    //        CompletableFuture.supplyAsync(
    //            () -> {
    //              try {
    //                Thread.sleep(10_000L);
    //              } catch (InterruptedException e) {
    //                throw new RuntimeException(e);
    //              }
    //              return "Concurrency Deep Dives";
    //            });
    //
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

    //    boolean wasCompleted = cF3.completeExceptionally(new RuntimeException("kabOOM!"));

    var bar = cF2.thenRunAsync(() -> System.out.println("Running second"));

    bar.cancel(true);

    bar.whenComplete(
        (ignored, ex) -> {
          if (ex != null) System.out.println("Error: " + ex);
          else System.out.println("Completed naturally");
        });

    Thread.sleep(100_000L);
  }
}
