package futures;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class FutureToCompletableFutureMain {
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    AsyncHttpClient asyncHttpClient = asyncHttpClient();

    Future<Response> resF =
        asyncHttpClient.executeRequest(
            new RequestBuilder().setUrl("https://news.ycombinator.com/").build());

    CompletableFuture<Response> res2F = FutureToCompletableFuture.poll(resF);

    res2F.whenCompleteAsync(
        (response, error) -> {
          System.out.println(response);
        });
  }
}
