package futures;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

public class FutureToCompletableFuture {

  private FutureToCompletableFuture() {}

  // this class uses only one thread and can handle tens of thousands of
  // the polling tasks we require
  private static Timer timer = new Timer("future-to-completablefuture-thread", false);
  private static long pollPeriodMillisDefault = 50L;

  static <T> CompletableFuture<T> poll(Future<T> original) {
    return poll(original, pollPeriodMillisDefault);
  }

  static <T> CompletableFuture<T> poll(Future<T> original, long pollPeriodMillis) {
    // two threads will keep a reference to this future
    // the calling thread (calling the method), and the thread of the timer.
    CompletableFuture<T> cF1 = new CompletableFuture<>();

    TimerTask timerTask =
        new TimerTask() {
          @Override
          public void run() {

            // this operation is non-blocking.
            // this means we are not preventing "polling operations" on other future conversions.
            if (original.isDone()) {
              try {
                T t = original.get();
                cF1.complete(t);
              } catch (ExecutionException e) {
                cF1.completeExceptionally(new CompletionException(e.getCause()));
              } catch (CancellationException | InterruptedException e) {
                cF1.completeExceptionally(e);
              }
              this.cancel(); // after its completed polling is unnecessary and wasteful
            }
          }
        };

    // run the task at a given frequency.
    timer.schedule(timerTask, pollPeriodMillis, pollPeriodMillis);

    return cF1;
  }

  static <T> CompletableFuture<T> toCompletableFuture(Future<T> original, Executor executor) {
    CompletableFuture<T> cF1 = new CompletableFuture<>();
    executor.execute(
        () -> {
          try {
            T result = original.get();
            cF1.complete(result);
          } catch (ExecutionException e) {
            cF1.completeExceptionally(new CompletionException(e.getCause()));
          } catch (CancellationException | InterruptedException e) {
            cF1.completeExceptionally(e);
          }
        });

    return cF1;
  }

  static <T> CompletableFuture<T> toCompletableFutureViaSupply(
      Future<T> original, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return original.get();
          } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
          } catch (InterruptedException e) {
            throw new CompletionException(e);
          }
        },
        executor);
  }

  static <T> CompletableFuture<T> toCompletableFutureViaSupply(Future<T> original) {
    return toCompletableFutureViaSupply(original, ForkJoinPool.commonPool());
  }
}
