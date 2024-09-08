package futures.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

public class WebCrawlerAppSync {

  public static void main(String[] args) throws IOException, InterruptedException {
    String urlRoot = "https://www.thetimes.com";
    HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(urlRoot)).GET().build();

    HttpResponse<String> res =
        WebCrawlerApp.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    Set<String> urlsExtracted = WebCrawlerApp.extractLinks(res.body());
    Set<String> urlsFiltered = WebCrawlerApp.filterOutFiles(urlsExtracted);

    urlsFiltered.forEach(System.out::println);
    System.out.printf("Size=%d\n", urlsFiltered.size());
  }
}
