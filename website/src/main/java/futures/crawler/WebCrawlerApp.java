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
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawlerApp {
  private static Executor executor = Executors.newWorkStealingPool(10);

  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          //          .executor(executor)
          .build();

  private final Set<String> UnwantedExtensions =
      Set.of(
          ".jpg",
          ".woff",
          ".png",
          ".svg",
          ".woff2",
          ".ttf",
          ".js",
          ".ico",
          ".json",
          ".jpeg",
          ".xml",
          ".zip",
          ".tar.gz",
          "tar.gz.sha256",
          ".rpm",
          ".deb",
          ".exe",
          ".msi",
          ".mp4",
          ".pdf",
          ".git",
          ".gif",
          ".dmg");

  private final int maxConcurrency;
  private final String destination;
  private final AtomicInteger currentConcurrency = new AtomicInteger(0);
  private final ConcurrentLinkedQueue<CompletableFuture<Void>> queueFutures =
      new ConcurrentLinkedQueue<>();
  private final Set<URI> urlsSeen = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final FileOutputStream outputStream;
  private final CompletableFuture<LinkHops> resGraph = new CompletableFuture<>();

  public WebCrawlerApp(int maxConcurrency, String targetUrl) throws IOException {
    this(maxConcurrency, targetUrl, Path.of("./crawling-output.txt"));
  }

  public WebCrawlerApp(int maxConcurrency, String targetUrl, Path out) throws IOException {
    this.maxConcurrency = maxConcurrency;
    this.destination = targetUrl;
    this.outputStream = new FileOutputStream(out.toFile());
  }

  private Set<String> extractLinks(String htmlDoc) {
    Document doc = Jsoup.parse(htmlDoc);
    Elements elements = doc.select("a[href]");
    Set<String> links = new HashSet<>();
    for (Element link : elements) {
      links.add(link.attr("abs:href"));
    }
    return Set.copyOf(links);
  }

  private Set<String> filterOutFiles(Set<String> urls) {
    return urls.stream()
        .filter(url -> UnwantedExtensions.stream().noneMatch(url::endsWith))
        .collect(Collectors.toUnmodifiableSet());
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
        .thenApplyAsync(this::filterOutFiles)
        .thenApplyAsync(this::removeFragmentsAndQueryString)
        .exceptionally(ignored -> Set.of());
  }

  private Set<URI> removeFragmentsAndQueryString(Set<String> strUris) {
    Set<URI> uris = new HashSet<>(strUris.size());
    for (String uriStr : strUris) {
      try {
        URI uri = URI.create(uriStr);
        if (!Objects.equals(uri.getScheme(), "https")) break;
        URI uriNoFragmentOrQueryString =
            new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null);
        uris.add(uriNoFragmentOrQueryString);
      } catch (Exception e) {
        throw new CompletionException("Parsing uri: " + uriStr, e);
      }
    }
    return Set.copyOf(uris);
  }

  private Optional<LinkHops> foo(Set<LinkHops.Hop> hops) {
    if (hops != null) {
      for (LinkHops hop : hops) {
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

  public CompletableFuture<LinkHops> crawl(URI startUrl) {
    recursive(LinkHops.empty(startUrl));
    return resGraph;
  }

  private CompletableFuture<Set<LinkHops>> recursive(LinkHops current) {
    URI currentUri = current.get();
    if (resGraph.isDone()) {
      return CompletableFuture.completedFuture(Set.of(current));
    }

    return getPermission(() -> scrapeExternalLinks(currentUri))
        .whenCompleteAsync((ignoredL, ignoredR) -> write(currentUri))
        .exceptionally(ignore -> Set.of()) // address this.
        .thenApplyAsync(
            childLinks ->
                childLinks.stream()
                    .filter(uri -> !uri.toString().isBlank())
                    .filter(uri -> !urlsSeen.contains(uri))
                    .collect(Collectors.toSet()))
        .whenCompleteAsync(
            (childLinks, ignored) -> {
              if (childLinks != null) urlsSeen.addAll(childLinks);
            })
        .thenApplyAsync(
            childLinks -> childLinks.stream().map(current::add).collect(Collectors.toSet()))
        .whenCompleteAsync(
            (hops, throwable) -> {
              //              System.out.printf(
              //                  "Current requests: %d. Queue size: %d\n",
              //                  currentConcurrency.get(), queueFutures.size());
              foo(hops).ifPresent(resGraph::complete);
            })
        .thenComposeAsync(
            externalLink -> {
              List<CompletableFuture<Set<LinkHops>>> children =
                  externalLink.stream().map(this::recursive).toList();

              return CompletableFuture.allOf(children.toArray(new CompletableFuture[0]))
                  .thenApplyAsync(
                      unused -> {
                        Set<LinkHops> linkHops = new HashSet<>();
                        for (CompletableFuture<Set<LinkHops>> child : children) {
                          linkHops.addAll(child.join());
                        }
                        return Set.copyOf(linkHops);
                      });
            });
  }

  public static void main(String[] args) throws IOException {
    WebCrawlerApp app =
        new WebCrawlerApp(50, "bbc.co.uk", Path.of("/home/cmhteixeira/Desktop/output-temp4.txt"));

    String urlRoot = "https://www.dev.java";

    CompletableFuture<LinkHops> linksCf = app.crawl(URI.create(urlRoot));

    LinkHops links = linksCf.join();
    System.out.println("Done.");
    links.forEach(System.out::println);
  }
}
