package futures;

import java.util.concurrent.CompletableFuture;

public class Utils {
  public static <T> CompletableFuture<T> sleepAsync(T t, long sleepMillis) {
    return CompletableFuture.supplyAsync(
        () -> {
          return t;
        });
  }
}
