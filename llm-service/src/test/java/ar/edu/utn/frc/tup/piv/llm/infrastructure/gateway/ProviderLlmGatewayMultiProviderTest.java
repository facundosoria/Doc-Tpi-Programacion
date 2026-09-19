package ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway.Provider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ejercita Anthropic/Gemini redirigiendo el HttpClient interno (hosts fijos) a un servidor local. */
class ProviderLlmGatewayMultiProviderTest {
  private HttpServer server;
  private final List<String> seen = new CopyOnWriteArrayList<>();
  private final List<String> headersSeen = new CopyOnWriteArrayList<>();
  private ProviderLlmGateway gateway;

  /** Reescribe host/puerto/esquema del request hacia el servidor local, conservando path y query. */
  static class RedirectingClient extends HttpClient {
    private final HttpClient delegate = HttpClient.newHttpClient();
    private final int port;
    RedirectingClient(int port) { this.port = port; }

    private HttpRequest rewrite(HttpRequest r) {
      URI o = r.uri();
      String q = o.getRawQuery() == null ? "" : "?" + o.getRawQuery();
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + o.getRawPath() + q))
          .method(r.method(), r.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
      r.headers().map().forEach((k, vs) -> vs.forEach(v -> b.header(k, v)));
      return b.build();
    }

    @Override public <T> HttpResponse<T> send(HttpRequest r, HttpResponse.BodyHandler<T> h) throws IOException, InterruptedException { return delegate.send(rewrite(r), h); }
    @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { return delegate.sendAsync(rewrite(r), h); }
    @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { return delegate.sendAsync(rewrite(r), h, p); }
    @Override public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }
    @Override public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }
    @Override public Redirect followRedirects() { return delegate.followRedirects(); }
    @Override public Optional<ProxySelector> proxy() { return delegate.proxy(); }
    @Override public SSLContext sslContext() { return delegate.sslContext(); }
    @Override public SSLParameters sslParameters() { return delegate.sslParameters(); }
    @Override public Optional<Authenticator> authenticator() { return delegate.authenticator(); }
    @Override public Version version() { return delegate.version(); }
    @Override public Optional<Executor> executor() { return delegate.executor(); }
  }

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/models", ex -> {
      headersSeen.add("anthropic-key=" + ex.getRequestHeaders().getFirst("x-api-key"));
      reply(ex, 200, "{\"data\":[{\"id\":\"claude-b\"},{\"id\":\"claude-a\"}]}");
    });
    server.createContext("/v1/messages", ex -> {
      headersSeen.add("version=" + ex.getRequestHeaders().getFirst("anthropic-version"));
      reply(ex, 200, "{\"content\":[{\"text\":\"hola claude\"}],\"usage\":{\"input_tokens\":7,\"output_tokens\":9}}");
    });
    server.createContext("/v1beta/models", ex -> {
      String path = ex.getRequestURI().toString();
      seen.add(path);
      if (path.contains(":generateContent")) {
        reply(ex, 200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hola gemini\"}]}}],\"usageMetadata\":{\"promptTokenCount\":2,\"candidatesTokenCount\":4}}");
      } else {
        headersSeen.add("goog=" + ex.getRequestHeaders().getFirst("x-goog-api-key"));
        reply(ex, 200, "{\"models\":[{\"name\":\"models/gemini-z\"},{\"name\":\"models/gemini-a\"}]}");
      }
    });
    server.start();
    gateway = new ProviderLlmGateway(new ObjectMapper());
    Field f = ProviderLlmGateway.class.getDeclaredField("client");
    f.setAccessible(true);
    f.set(gateway, new RedirectingClient(server.getAddress().getPort()));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private static void reply(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.sendResponseHeaders(status, bytes.length);
    ex.getResponseBody().write(bytes);
    ex.close();
  }

  @Test
  void anthropicListModelsUsesApiKeyHeaderAndSortsIds() {
    assertThat(gateway.listModels(Provider.ANTHROPIC, "ignored", "sk-ant")).containsExactly("claude-a", "claude-b");
    assertThat(headersSeen).contains("anthropic-key=sk-ant");
  }

  @Test
  void geminiListModelsStripsModelsPrefix() {
    assertThat(gateway.listModels(Provider.GEMINI, "ignored", "g-key")).containsExactly("gemini-a", "gemini-z");
    assertThat(headersSeen).contains("goog=g-key");
  }

  @Test
  void anthropicChatParsesTextAndTokens() {
    var reply = gateway.chat(Provider.ANTHROPIC, "ignored", "sk", "claude", "hi");
    assertThat(reply.text()).isEqualTo("hola claude");
    assertThat(reply.inputTokens()).isEqualTo(7);
    assertThat(reply.outputTokens()).isEqualTo(9);
    assertThat(headersSeen).contains("version=2023-06-01");
  }

  @Test
  void geminiChatParsesTextTokensAndEncodesKeyInQuery() {
    var reply = gateway.chat(Provider.GEMINI, "ignored", "k e/y", "gemini-a", "hi");
    assertThat(reply.text()).isEqualTo("hola gemini");
    assertThat(reply.inputTokens()).isEqualTo(2);
    assertThat(reply.outputTokens()).isEqualTo(4);
    assertThat(seen.get(0)).contains("gemini-a:generateContent").contains("key=k+e%2Fy");
  }

  @Test
  void streamChatOnNonOpenAiProviderEmitsSingleDeltaWithFullText() {
    List<String> deltas = new java.util.ArrayList<>();
    var reply = gateway.streamChat(Provider.ANTHROPIC, "ignored", "sk", "claude", "hi", deltas::add);
    assertThat(deltas).containsExactly("hola claude");
    assertThat(reply.outputTokens()).isEqualTo(9);
  }

  @Test
  void chatWrapsErrorWhenProviderEndpointIsMissing() {
    server.removeContext("/v1/messages");
    assertThatThrownBy(() -> gateway.chat(Provider.ANTHROPIC, "ignored", "sk", "claude", "hi"))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no pudo responder");
  }

  @Test
  void listModelsWithUnreachableServerFailsWithConnectionMessage() {
    server.stop(0);
    assertThatThrownBy(() -> gateway.listModels(Provider.GEMINI, "x", "k"))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("conectar");
  }
}
