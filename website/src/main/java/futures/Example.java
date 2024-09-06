package futures;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Example {

  private static final HttpClient httpClient = HttpClient.newHttpClient();

  private static CompletableFuture<String> httpGetRequest(String url) {
    return httpClient
        .sendAsync(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body);
  }

  private static CompletableFuture<Void> writeFile(String content) {
    CompletableFuture<Void> cF = new CompletableFuture<>();
    AsynchronousFileChannel fC;
    try {
      fC =
          AsynchronousFileChannel.open(
              Path.of("results.txt"),
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.WRITE);
    } catch (IOException e) {
      return CompletableFuture.failedFuture(e);
    }

    fC.write(
        ByteBuffer.wrap(content.getBytes()),
        0,
        null,
        new CompletionHandler<>() {
          public void completed(Integer result, Object attachment) {
            cF.complete(null);
          }

          public void failed(Throwable exc, Object attachment) {
            cF.completeExceptionally(exc);
          }
        });

    // we should be closing the file channel
    return cF;
  }

  private static int countPrefix(String contents) {
    return (int) Pattern.compile("money").matcher(contents).results().count();
  }

  private static String readFile(Path path) {
    return null;
  }

  private static String extractUserId(String rawString) {
    return null;
  }

  private static CompletableFuture<UserProfile> fetchUserProfile(String rawString) {
    return null;
  }

  private static void logAccessOnDB(Data accessData) {
    return;
  }

  private static Data downloadRestrictedData(UserProfile user, UserPermissions permissions) {
    return null;
  }

  record Data() {}

  record UserProfile() {}

  class InvalidUserId extends RuntimeException {}

  static CompletableFuture<UserPermissions> fetchUserPermissions() {
    return null;
  }

  record UserPermissions() {}

  public static long generateRandomLong(long min, long max) {
    if (min >= max) {
      throw new IllegalArgumentException("max must be greater than min");
    }
    Random random = new Random();
    return min + (long) (random.nextDouble() * (max - min));
  }

  private static int lapsedS(long startReference) {
    return (int) ((System.currentTimeMillis() - startReference) / 1000L);
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    Thread.sleep(10_000L);

    List<Future<Integer>> foo =
        IntStream.range(0, 10_000)
            .boxed()
            .map(
                i ->
                    (Future<Integer>)
                        CompletableFuture.supplyAsync(
                            () -> i,
                            CompletableFuture.delayedExecutor(
                                generateRandomLong(1_000, 30_000), TimeUnit.MILLISECONDS)))
            .toList();

    AtomicInteger counter = new AtomicInteger(0);
    AtomicInteger errorCounter = new AtomicInteger(0);
    for (Future<Integer> fut : foo) {
      FutureToCompletableFuture.poll(fut)
          .whenCompleteAsync(
              (res, ex) -> {
                if (ex != null)
                  System.out.printf(
                      "Erroed future. %s.%d\n",
                      ex.getMessage().replace('\n', ' '), errorCounter.incrementAndGet());
                else System.out.printf("Completed future %d. %d\n", res, counter.incrementAndGet());
              });
    }
  }
}
