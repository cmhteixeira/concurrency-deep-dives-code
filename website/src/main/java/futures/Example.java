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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

  public static void main(String[] args) {
    CompletableFuture<Path> cF1 = FileSystemMonitor.monitorFolder("/home/cmhteixeira/Desktop", "*password*");
    System.out.println(cF1.join());
  }
}
