package futures.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebCrawlerApp {
  static final HttpClient httpClient =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
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

  public static void main(String[] args) {
    String urlRoot = "https://www.thetimes.com";
    HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(urlRoot)).GET().build();

    CompletableFuture<Set<String>> linksCf =
        httpClient
            .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync(maybeBody -> WebCrawlerApp.extractLinks(maybeBody.body()))
            .thenApplyAsync(links -> WebCrawlerApp.filterOutFiles(links));

    Set<String> links = linksCf.join();
    links.forEach(System.out::println);
    System.out.printf("Size = %d links\n", links.size());
  }
}
