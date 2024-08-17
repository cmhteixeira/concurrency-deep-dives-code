package futures;

import java.util.concurrent.*;

public class Utils {

  public static <T> CompletableFuture<T> sleepAsync(T t, long sleepMillis) {
    return CompletableFuture.supplyAsync(
        () -> {
          return t;
        });
  }
}
