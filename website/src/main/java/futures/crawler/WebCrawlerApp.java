package futures.crawler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.asynchttpclient.uri.Uri;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import io.vavr.collection.List;

public class WebCrawlerApp {
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private final io.vavr.collection.Set<String> UNWANTED_EXTENSIONS =
      io.vavr.collection.HashSet.of(
          ".jpg", ".woff", ".png", ".svg", ".woff2", ".ttf", ".js", ".ico", ".json", ".jpeg",
          ".xml", ".zip", ".tar.gz", ".rpm", ".deb", ".exe", ".msi", ".mp4", ".pdf", ".git", ".gif",
          ".dmg");

  private final int maxConcurrency;
  private final String destination;
  private final AtomicInteger currentConcurrency = new AtomicInteger(0);
  private final ConcurrentLinkedQueue<CompletableFuture<Void>> queueFutures =
      new ConcurrentLinkedQueue<>();
  private final Set<URI> urlsSeen = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final FileOutputStream outputStream;
  private final CompletableFuture<List<URI>> resGraph = new CompletableFuture<>();

  public WebCrawlerApp(int maxConcurrency, String targetUrl) throws IOException {
    this(maxConcurrency, targetUrl, Path.of("./crawling-output.txt"));
  }

  public WebCrawlerApp(int maxConcurrency, String targetUrl, Path out) throws IOException {
    this.maxConcurrency = maxConcurrency;
    this.destination = targetUrl;
    this.outputStream = new FileOutputStream(out.toFile());
  }

  private io.vavr.collection.Set<String> extractLinks(String htmlDoc) {
    Document doc = Jsoup.parse(htmlDoc);
    Elements elements = doc.select("a[href]");
    Set<String> links = new HashSet<>();
    for (Element link : elements) {
      links.add(link.attr("abs:href"));
    }
    return io.vavr.collection.HashSet.ofAll(links);
  }

  private io.vavr.collection.Set<String> filterOutFiles(io.vavr.collection.Set<String> urls) {
    return urls.filter(url -> !UNWANTED_EXTENSIONS.exists(url::endsWith));
  }

  private CompletableFuture<io.vavr.collection.Set<URI>> scrapeExternalLinks(URI url) {
    HttpRequest httpRequest;

    try {
      httpRequest = HttpRequest.newBuilder(url).GET().build();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    return httpClient
        .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
        .orTimeout(5, TimeUnit.SECONDS)
        .thenApplyAsync(maybeBody -> extractLinks(maybeBody.body()))
        .thenApplyAsync(this::filterOutFiles)
        .thenApplyAsync(this::removeFragmentsAndQueryString)
        .exceptionally(ignored -> io.vavr.collection.HashSet.empty());
  }

  private io.vavr.collection.Set<URI> removeFragmentsAndQueryString(
      io.vavr.collection.Set<String> strUris) {

    return strUris
        .map(
            uriStr -> {
              try {
                URI uri = URI.create(uriStr);
                return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null);
              } catch (Exception e) {
                throw new CompletionException("Parsing uri: " + uriStr, e);
              }
            })
        .filter(uri -> Objects.equals(uri.getScheme(), "https"));
  }

  private Optional<List<URI>> foo(io.vavr.collection.Set<List<URI>> hops) {
    if (hops != null) {
      for (List<URI> hop : hops) {
        try {
          if (hop.get().getHost().contains(destination)) return Optional.of(hop);
        } catch (Exception e) {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  private void write(URI url) {
    try {

      this.outputStream.write(
          String.format("%s, %s\n", url.getAuthority(), url).getBytes(StandardCharsets.UTF_8));
    } catch (IOException exception) {
      throw new CompletionException(String.format("Error writing url %s to file", url), exception);
    }
  }

  private void returnPermission() {
    int current = currentConcurrency.get();
    if (current <= maxConcurrency) {
      if (!currentConcurrency.compareAndSet(current, current)) returnPermission();
      CompletableFuture<Void> cF = queueFutures.poll();
      if (cF != null) cF.complete(null);
      else currentConcurrency.getAndIncrement();
    } else currentConcurrency.getAndDecrement();
  }

  private <V> CompletableFuture<V> getPermission(Supplier<CompletableFuture<V>> cFToRun) {
    int current = currentConcurrency.get();
    if (current < maxConcurrency) {
      if (!currentConcurrency.compareAndSet(current, current + 1)) getPermission(cFToRun);
      return cFToRun.get().whenCompleteAsync((ignoredL, ignoredR) -> returnPermission());
    }
    CompletableFuture<Void> cF = new CompletableFuture<>();
    queueFutures.offer(cF);
    return cF.thenComposeAsync(ignored -> cFToRun.get())
        .whenCompleteAsync((ignoredL, ignoredR) -> returnPermission());
  }

  public CompletableFuture<List<URI>> crawl(URI startUrl) {
    CompletableFuture<io.vavr.collection.Set<List<URI>>> cF = recursive(List.of(startUrl));
    return resGraph.whenCompleteAsync(
        (res, ex) -> {
          if (res != null) {
            String res2 = cF.join().map(k -> k.mkString(", ")).mkString("\n");
            try {
              this.outputStream.write(
                  ("####################" + res2.length() + "\n").getBytes(StandardCharsets.UTF_8));
              this.outputStream.write("####################\n".getBytes(StandardCharsets.UTF_8));
              this.outputStream.write(res2.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  private CompletableFuture<io.vavr.collection.Set<List<URI>>> recursive(List<URI> current) {
    URI currentUri = current.get();
    if (resGraph.isDone()) {
      return CompletableFuture.completedFuture(io.vavr.collection.HashSet.of(current));
    }

    return getPermission(() -> scrapeExternalLinks(currentUri))
        .whenCompleteAsync((ignoredL, ignoredR) -> write(currentUri))
        .exceptionally(ignore -> io.vavr.collection.HashSet.empty()) // address this.
        .thenApplyAsync(
            childLinks ->
                childLinks
                    .filter(uri -> !uri.toString().isBlank())
                    .filter(uri -> !urlsSeen.contains(uri)))
        .whenCompleteAsync(
            (childLinks, ignored) -> {
              if (childLinks != null) childLinks.forEach(urlsSeen::add);
            })
        .thenApplyAsync(childLinks -> childLinks.map(current::prepend))
        .whenCompleteAsync((hops, throwable) -> foo(hops).ifPresent(resGraph::complete))
        .thenComposeAsync(
            externalLinks -> {
              io.vavr.collection.Set<CompletableFuture<io.vavr.collection.Set<List<URI>>>>
                  children = externalLinks.map(this::recursive);

              return CompletableFuture.allOf(children.toJavaArray(i -> new CompletableFuture[0]))
                  .thenApplyAsync(unused -> children.flatMap(CompletableFuture::join));
            });
  }

  public static void main(String[] args) throws IOException {
    WebCrawlerApp app =
        new WebCrawlerApp(50, "bbc.co.uk", Path.of("/home/cmhteixeira/Desktop/output-temp4.txt"));

    String urlRoot = "https://www.dev.java";

    CompletableFuture<List<URI>> linksCf = app.crawl(URI.create(urlRoot));

    List<URI> links = linksCf.join();
    System.out.println("Done.");
    links.forEach(System.out::println);
  }
}
