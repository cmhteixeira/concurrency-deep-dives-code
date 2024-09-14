package futures.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.HashSet;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WebCrawlerApp {
  private static Executor executor = Executors.newWorkStealingPool(10);

  static final HttpClient httpClient =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .executor(executor)
          .build();

  static final Set<String> UnwantedExtensions =
      Set.of(
          ".jpg", ".woff", ".png", ".svg", ".woff2", ".ttf", ".js", ".ico", ".json", ".jpeg",
          ".xml");

  static Set<String> extractLinks(String htmlDoc) {
    Document doc = Jsoup.parse(htmlDoc);
    Elements elements = doc.select("a[href]");
    Set<String> links = new HashSet<>();
    for (Element link : elements) {
      links.add(link.attr("abs:href"));
    }
    return Set.copyOf(links);
  }

  static Set<String> filterOutFiles(Set<String> urls) {
    return urls.stream()
        .filter(url -> UnwantedExtensions.stream().noneMatch(url::endsWith))
        .collect(Collectors.toUnmodifiableSet());
  }

  public static CompletableFuture<Set<String>> scrapeExternalLinks(String url) {
    HttpRequest httpRequest;

    try {
      httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().build();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    return httpClient
        .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
        .orTimeout(150, TimeUnit.SECONDS)
        .thenApplyAsync(maybeBody -> WebCrawlerApp.extractLinks(maybeBody.body()))
        .thenApplyAsync(WebCrawlerApp::filterOutFiles)
        .exceptionally(ignored -> Set.of());
  }

  private static final int maximumNumberJumps = 10;

  private static final Set<String> urlsSeen = new ConcurrentSkipListSet<>();

  private static CompletableFuture<Set<LinkHops>> recursive(LinkHops current, int numberHops) {
    String currentUri = current.get();
    if (numberHops >= maximumNumberJumps || urlsSeen.contains(currentUri)) {
      return CompletableFuture.completedFuture(Set.of(current));
    }

    return scrapeExternalLinks(currentUri)
        .whenCompleteAsync((ignoredL, ignoredR) -> urlsSeen.add(currentUri))
        .exceptionally(ignore -> Set.of()) // address this.
        .thenApplyAsync(theSet -> theSet.stream().map(current::add).collect(Collectors.toSet()))
        .thenComposeAsync(
            externalLink -> {
              List<CompletableFuture<Set<LinkHops>>> children =
                  externalLink.stream()
                      .map(linkedHops -> recursive(linkedHops, numberHops + 1))
                      .toList();

              return CompletableFuture.allOf(children.toArray(new CompletableFuture[0]))
                  .thenApplyAsync(
                      unused -> {
                        Set<LinkHops> linkHops = new HashSet<>();
                        for (CompletableFuture<Set<LinkHops>> child : children) {
                          linkHops.addAll(child.join());
                        }
                        return Set.copyOf(linkHops);
                      })
                  .whenCompleteAsync(
                      (value, error) -> {
                        if (error != null) error.printStackTrace();
                      });
            });
  }

  public static void main(String[] args) {
    String urlRoot = "https://www.dev.java";

    CompletableFuture<Set<LinkHops>> linksCf = recursive(LinkHops.empty(urlRoot), 0);

    Set<LinkHops> links = linksCf.join();
    links.forEach(System.out::println);
    System.out.printf("Size = %d links\n", links.size());
    var g =
        links.stream()
            .sorted(Comparator.comparingInt(LinkHops::size))
            .toList()
            .get(links.size() - 1);
    System.out.printf("Biggest graph = %d\n", g.size());
    Set<String> flattenedLinks = new HashSet<>();
    links.forEach(
        linkHop -> {
          linkHop.forEach(flattenedLinks::add);
        });

    //    flattenedLinks.forEach(System.out::println);
    System.out.printf("Size Unique= %d links\n", flattenedLinks.size());
  }
}
