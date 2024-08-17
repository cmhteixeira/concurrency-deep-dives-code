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
import java.util.concurrent.*;
import java.util.regex.Pattern;

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

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    FutureTask<String> fT =
        new FutureTask<>(
            () -> {
              Thread.sleep(10_000L);
              return "Concurrency Deep Dives";
            });

    ForkJoinPool.commonPool().submit(fT);

    while (!fT.isDone()) {
      System.out.println("Foo bar");
    }

    Path path = null;
    UserProfile defaultProfile = null;

    CompletableFuture<Void> cF1 =
        CompletableFuture.supplyAsync(() -> readFile(path))
            .thenApplyAsync(str -> extractUserId(str))
            .thenCompose(userId -> fetchUserProfile(userId))
            .exceptionally(
                exception -> {
                  if (exception instanceof InvalidUserId) return defaultProfile;
                  else throw new CompletionException(exception);
                })
            .thenCombine(
                fetchUserPermissions(),
                (userProfile, allPermissions) ->
                    downloadRestrictedData(userProfile, allPermissions))
            .thenAcceptAsync(accessData -> logAccessOnDB(accessData));

    String res = fT.get();
    System.out.println("The result was: " + res);
  }
}
