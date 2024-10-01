package futures.crawler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
  private final CompletableFuture<List<URI>> resGraph = new CompletableFuture<>();

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
                    "con: %d, queue:%d, %d, %d, %s, %s\n",
                    concurrency.get(),
                    queueFutures.size(),
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
        .filter(uri -> uri.getHost() != null && !uri.getHost().isBlank());
  }

  private void returnPermission() {
    int current = concurrency.get();
    if (current <= maxConcurrency) {
      if (!concurrency.compareAndSet(current, current)) returnPermission();
      CompletableFuture<Void> cF = queueFutures.poll();
      if (cF != null) cF.complete(null);
      else concurrency.getAndIncrement();
    } else concurrency.getAndDecrement();
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

  public CompletableFuture<List<URI>> crawl(URI startUri) {
    return recursiveChildren(List.of(startUri))
        .whenCompleteAsync(
            (allPathsExplored, ignored) -> {
              try (FileOutputStream oS =
                  new FileOutputStream(Path.of("crawl-paths.csv").toFile())) {
                String allPathsVisited = allPathsExplored.map(k -> k.mkString(", ")).mkString("\n");
                oS.write(allPathsVisited.getBytes(StandardCharsets.UTF_8));
              } catch (IOException e) {
                throw new CompletionException("Whilst writing all paths explored.", e);
              }
            })
        .thenApplyAsync(
            allPaths -> {
              if (resGraph.isDone()) return resGraph.join();
              else throw new RuntimeException("No path between source and destination");
            });
  }

  private CompletableFuture<Set<List<URI>>> recursiveChildren(List<URI> current) {
    URI currentUri = current.get();
    if (resGraph.isDone()) {
      return CompletableFuture.completedFuture(HashSet.of(current));
    }

    return getPermission(() -> scrapeExternalLinks(currentUri))
        .exceptionally(ignored -> HashSet.empty())
        .thenApplyAsync(
            children ->
                children.filter(child -> !child.getAuthority().equals(currentUri.getAuthority())))
        .thenApplyAsync(
            children ->
                children.filter(
                    child -> child.getPath().endsWith(".html") || !child.getPath().contains(".")))
        .thenApplyAsync(children -> children.filter(uri -> !urisSeen.contains(uri)))
        .whenCompleteAsync(
            (childLinks, ignored) -> {
              if (childLinks != null) childLinks.forEach(urisSeen::add);
            })
        .whenCompleteAsync(
            (childUris, throwable) -> {
              if (childUris != null) {
                childUris
                    .find(child -> child.getHost().contains(destination))
                    .forEach(destLink -> resGraph.complete(current.prepend(destLink)));
              }
            })
        .thenApplyAsync(childLinks -> childLinks.map(current::prepend))
        .thenComposeAsync(
            externalLinks -> {
              Set<CompletableFuture<Set<List<URI>>>> children =
                  externalLinks.map(this::recursiveChildren);

              return CompletableFuture.allOf(children.toJavaArray(i -> new CompletableFuture[0]))
                  .thenApplyAsync(unused -> children.flatMap(CompletableFuture::join));
            });
  }

  //    Best: -Xmx10g -XX:+UseParallelGC
  public static void main(String[] args) {
    WebCrawlerApp app = new WebCrawlerApp(3, "news.ycombinator.com");

    CompletableFuture<List<URI>> possiblePathF = app.crawl(URI.create("https://nytimes.com"));
    List<URI> possiblePath = possiblePathF.join();
    System.out.println("Done:");
    possiblePath.forEach(System.out::println);
  }
}
