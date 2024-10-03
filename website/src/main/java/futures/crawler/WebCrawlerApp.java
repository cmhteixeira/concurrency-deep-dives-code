package futures.crawler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.HashSet;

public class WebCrawlerApp {
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private final int maxConcurrency;
  private final String destination;
  private final AtomicInteger concurrency = new AtomicInteger(0);
  private final ConcurrentLinkedQueue<CompletableFuture<Void>> queueFutures =
      new ConcurrentLinkedQueue<>();
  private final java.util.Set<URI> urisSeen = Collections.newSetFromMap(new ConcurrentHashMap<>());

  public WebCrawlerApp(int maxConcurrency, String targetUri) {
    this.maxConcurrency = maxConcurrency;
    this.destination = targetUri;
  }

  private Set<String> extractLinks(String htmlDoc) {
    Document doc = Jsoup.parse(htmlDoc);
    Elements elements = doc.select("a[href]");
    return HashSet.ofAll(elements).map(element -> element.attr("abs:href"));
  }

  private CompletableFuture<Set<URI>> scrapeExternalLinks(URI uri) {
    HttpRequest httpRequest;

    try {
      httpRequest = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(2)).build();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    long start = System.currentTimeMillis();
    return httpClient
        .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
        .orTimeout(5, TimeUnit.SECONDS)
        .whenCompleteAsync(
            (httpResponse, exception) ->
                System.out.printf(
                    "liveReq: %d, %d, %d, %s, %s\n",
                    concurrency.get(),
                    httpResponse.body().length() / 1024L,
                    System.currentTimeMillis() - start,
                    uri.getAuthority(),
                    uri))
        .thenApplyAsync(httpResponse -> extractLinks(httpResponse.body()))
        .thenApplyAsync(this::parseURI);
  }

  private Set<URI> parseURI(Set<String> strUris) {
    return strUris
        .flatMap(
            uriStr -> {
              try {
                URI uri = URI.create(uriStr);
                return HashSet.of(
                    new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null));
              } catch (Exception e) {
                return HashSet.empty();
              }
            })
        .filter(uri -> Objects.equals(uri.getScheme(), "https"))
        .filter(uri -> uri.getHost() != null && !uri.getHost().isBlank())
        .filter(uri -> uri.getPath().endsWith(".html") || !uri.getPath().contains("."));
  }

  private void returnPermission() {
    CompletableFuture<Void> cF = queueFutures.poll();
    if (cF != null) cF.complete(null);
    else concurrency.getAndDecrement();
  }

  private <V> CompletableFuture<V> getPermission(Supplier<CompletableFuture<V>> cFToRun) {
    int current = concurrency.get();
    if (current < maxConcurrency) {
      if (!concurrency.compareAndSet(current, current + 1)) getPermission(cFToRun);
      return cFToRun.get().whenCompleteAsync((ignoredL, ignoredR) -> returnPermission());
    }
    CompletableFuture<Void> cF = new CompletableFuture<>();
    queueFutures.offer(cF);
    return cF.thenComposeAsync(ignored -> cFToRun.get())
        .whenCompleteAsync((ignoredL, ignoredR) -> returnPermission());
  }

  private <T> CompletableFuture<T> anySuccessOf(Set<CompletableFuture<T>> cFs) {
    if (cFs.isEmpty())
      return CompletableFuture.failedFuture(new IllegalArgumentException("No futures passed in."));
    AtomicInteger counter = new AtomicInteger(cFs.length());
    CompletableFuture<T> cfToReturn = new CompletableFuture<>();
    cFs.forEach(
        cF ->
            cF.whenCompleteAsync(
                (success, error) -> {
                  if (success != null) cfToReturn.complete(success);
                  else {
                    if (counter.decrementAndGet() == 0)
                      cfToReturn.completeExceptionally(
                          new CompletionException("All other futures also failed.", error));
                  }
                }));
    return cfToReturn;
  }

  public CompletableFuture<List<URI>> crawlFrom(URI parent) {
    return getPermission(() -> scrapeExternalLinks(parent))
        .exceptionally(ignored -> HashSet.empty())
        .thenApplyAsync(
            children ->
                children.filter(child -> !child.getAuthority().equals(parent.getAuthority())))
        .thenApplyAsync(children -> children.filter(uri -> !urisSeen.contains(uri)))
        .whenCompleteAsync((childLinks, ignored) -> childLinks.forEach(urisSeen::add))
        .thenComposeAsync(
            children ->
                children
                    .find(child -> child.getAuthority().contains(destination))
                    .fold(
                        () ->
                            anySuccessOf(children.map(this::crawlFrom))
                                .thenApplyAsync(successPath -> successPath.prepend(parent)),
                        destinationURI ->
                            CompletableFuture.completedFuture(List.of(parent, destinationURI))));
  }

  //    Best: -Xmx10g -XX:+UseParallelGC
  public static void main(String[] args) {
    WebCrawlerApp app = new WebCrawlerApp(10, "nytimes.com");

    CompletableFuture<List<URI>> possiblePathF =
        app.crawlFrom(URI.create("https://reddit.com/r/java"));
    List<URI> possiblePath = possiblePathF.join();
    System.out.println("Done:");
    possiblePath.forEach(System.out::println);
  }
}
