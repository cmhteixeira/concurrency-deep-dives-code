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

  private final Set<String> UNWANTED_EXTENSIONS =
      io.vavr.collection.HashSet.of(
          ".jpg", ".woff", ".png", ".svg", ".woff2", ".ttf", ".js", ".ico", ".json", ".jpeg",
          ".xml", ".zip", ".tar.gz", ".rpm", ".deb", ".exe", ".msi", ".mp4", ".pdf", ".git", ".gif",
          ".dmg");

  private final int maxConcurrency;
  private final String destination;
  private final AtomicInteger currentConcurrency = new AtomicInteger(0);
  private final ConcurrentLinkedQueue<CompletableFuture<Void>> queueFutures =
      new ConcurrentLinkedQueue<>();
  private final java.util.Set<URI> urlsSeen = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final FileOutputStream outputStream;
  private final Path outputFolder;
  private final CompletableFuture<List<URI>> resGraph = new CompletableFuture<>();

  public WebCrawlerApp(int maxConcurrency, String targetUrl) throws IOException {
    this(maxConcurrency, targetUrl, Path.of("./crawling-output.txt"));
  }

  public WebCrawlerApp(int maxConcurrency, String targetUrl, Path out) throws IOException {
    this.maxConcurrency = maxConcurrency;
    this.destination = targetUrl;
    this.outputFolder = out;
    this.outputStream = new FileOutputStream(out.resolve("debug-crawler.csv").toFile());
  }

  private Set<String> extractLinks(String htmlDoc) {
    Document doc = Jsoup.parse(htmlDoc);
    Elements elements = doc.select("a[href]");
    return HashSet.ofAll(elements).map(k -> k.attr("abs:href"));
  }

  private CompletableFuture<Set<URI>> scrapeExternalLinks(URI url) {
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
        .thenApplyAsync(
            children -> children.filter(child -> !UNWANTED_EXTENSIONS.exists(child::endsWith)))
        .thenApplyAsync(this::removeFragmentsAndQueryString)
        .exceptionally(ignored -> HashSet.empty());
  }

  private Set<URI> removeFragmentsAndQueryString(Set<String> strUris) {

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
        .filter(uri -> Objects.equals(uri.getScheme(), "https"))
        .filter(uri -> uri.getHost() != null && !uri.getHost().isBlank());
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
    CompletableFuture<Set<List<URI>>> cF = childLinks(List.of(startUrl));
    return resGraph.whenCompleteAsync(
        (res, ex) -> {
          String res2 = cF.join().map(k -> k.mkString(", ")).mkString("\n");
          try (FileOutputStream oS =
              new FileOutputStream(outputFolder.resolve("crawl-paths.csv").toFile())) {
            oS.write(res2.getBytes(StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private CompletableFuture<Set<List<URI>>> childLinks(List<URI> current) {
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
        .whenCompleteAsync(
            (childUris, throwable) -> {
              if (childUris != null) {
                childUris
                    .find(child -> Objects.equals(child.getHost(), destination))
                    .toJavaOptional()
                    .ifPresent(destLink -> resGraph.complete(current.prepend(destLink)));
              }
            })
        .thenApplyAsync(childLinks -> childLinks.map(current::prepend))
        .thenComposeAsync(
            externalLinks -> {
              Set<CompletableFuture<Set<List<URI>>>> children = externalLinks.map(this::childLinks);

              return CompletableFuture.allOf(children.toJavaArray(i -> new CompletableFuture[0]))
                  .thenApplyAsync(unused -> children.flatMap(CompletableFuture::join));
            });
  }

  public static void main(String[] args) throws IOException {
    WebCrawlerApp app = new WebCrawlerApp(300, "bbc.co.uk", Path.of("/home/cmhteixeira/Desktop"));

    String urlRoot = "https://www.dev.java";

    CompletableFuture<List<URI>> linksCf = app.crawl(URI.create(urlRoot));

    List<URI> links = linksCf.join();
    System.out.println("Done:");
    links.forEach(System.out::println);
  }
}
