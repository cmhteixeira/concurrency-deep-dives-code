package futures;

import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.monitor.*;

public class Utils {
  public static <T> CompletableFuture<T> sleepAsync(T t, long sleepMillis) {
    return CompletableFuture.supplyAsync(
        () -> {
          return t;
        });
  }
}
