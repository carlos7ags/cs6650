import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

public class HttpClientAsync {

  private static final HttpClient httpClient = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  @lombok.SneakyThrows
  public void main() {

    HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("http://localhost:8080/hello"))
        .setHeader("User-Agent", "Testing CS6650 client")
        .build();

    CompletableFuture<HttpResponse<String>> response =
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

    String result = response.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);

  }

}