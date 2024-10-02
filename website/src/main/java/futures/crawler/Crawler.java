package futures.crawler;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

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

public class Crawler {
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private final AtomicInteger concurrency = new AtomicInteger(0);
  private final ConcurrentLinkedQueue<CompletableFuture<Void>> queueFutures =
      new ConcurrentLinkedQueue<>();
  private final java.util.Set<URI> urisSeen = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private final int maxConcurrency;
  private final CompletableFuture<URI> targetWebPageF = new CompletableFuture<>();
  private final String destinationHost;

  public Crawler(int maxConcurrency, String destinationHost) {
    this.maxConcurrency = maxConcurrency;
    this.destinationHost = destinationHost;
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
        .whenCompleteAsync(
            (httpResponse, exception) ->
                System.out.printf(
                    "%d, %d, %s, %s\n",
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
                return HashSet.of(uri);
              } catch (Exception e) {
                return HashSet.empty();
              }
            })
        .filter(uri -> Objects.equals(uri.getScheme(), "https"))
        .filter(uri -> uri.getHost() != null && !uri.getHost().isBlank());
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

  private CompletableFuture<Set<URI>> recursiveChildren(URI parent) {
    if (targetWebPageF.isDone()) {
      return CompletableFuture.completedFuture(HashSet.empty());
    }

    return getPermission(() -> scrapeExternalLinks(parent))
        .exceptionallyAsync(ignored -> HashSet.empty())
        .thenApplyAsync(children -> children.filter(uri -> !urisSeen.contains(uri)))
        .whenCompleteAsync((childLinks, ignored) -> childLinks.forEach(urisSeen::add))
        .whenCompleteAsync(
            (childUris, ignored) ->
                childUris
                    .find(child -> child.getHost().contains(destinationHost))
                    .forEach(targetWebPageF::complete))
        .thenComposeAsync(
            children -> {
              Set<CompletableFuture<Set<URI>>> grandChildrenF =
                  children.map(this::recursiveChildren);

              CompletableFuture<Void> allGrandChildrenFound =
                  CompletableFuture.allOf(
                      grandChildrenF.toJavaArray(i -> new CompletableFuture[0]));

              return allGrandChildrenFound.thenApplyAsync(
                  unused -> grandChildrenF.flatMap(CompletableFuture::join));
            });
  }

  public CompletableFuture<URI> crawlFrom(URI startUri) {
    return recursiveChildren(startUri)
        .thenApplyAsync(
            allUris -> {
              if (targetWebPageF.isDone()) return targetWebPageF.join();
              else throw new RuntimeException("No path between source and destination");
            });
  }

  public static void main(String[] args) {
    CompletableFuture<URI> childrenF =
        new Crawler(1, "nytimes.com").crawlFrom(URI.create("https://en.wikisource.org/"));

    URI targetPage = childrenF.join();
    System.out.printf("The page to the target website is: %s\n", targetPage);
  }
}
