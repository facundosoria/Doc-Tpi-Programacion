package ar.edu.utn.frc.tup.piv.llm.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class JwksRefreshJobTest {
  private HttpServer server;
  private volatile String body = "{}";
  private volatile int status = 200;
  private String url;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/jwks", ex -> {
      byte[] b = body.getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().add("Content-Type", "application/json");
      ex.sendResponseHeaders(status, b.length);
      ex.getResponseBody().write(b);
      ex.close();
    });
    server.start();
    url = "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void initialStateIsNunca() {
    var job = new JwksRefreshJob(RestClient.builder(), url);
    assertThat(job.estado().resultado()).isEqualTo("nunca");
    assertThat(job.estado().cantidad()).isZero();
  }

  @Test
  void refreshReadsSortedKidsAndIgnoresNonObjectEntries() {
    body = "{\"keys\":[{\"kid\":\"b\"},{\"kid\":\"a\"},\"raro\"]}";
    var job = new JwksRefreshJob(RestClient.builder(), url);
    job.refrescar();
    var e = job.estado();
    assertThat(e.resultado()).isEqualTo("ok");
    assertThat(e.cantidad()).isEqualTo(3);
    assertThat(e.kids()).containsExactly("a", "b");
    assertThat(e.actualizadoEn()).isNotNull();
  }

  @Test
  void refreshWithoutKeysYieldsEmptyOkState() {
    body = "{}";
    var job = new JwksRefreshJob(RestClient.builder(), url);
    job.refrescar();
    assertThat(job.estado().resultado()).isEqualTo("ok");
    assertThat(job.estado().kids()).isEmpty();
  }

  @Test
  void failureKeepsPreviousKeysAndReportsError() {
    body = "{\"keys\":[{\"kid\":\"k1\"}]}";
    var job = new JwksRefreshJob(RestClient.builder(), url);
    job.refrescar();
    status = 500;
    job.refrescar();
    var e = job.estado();
    assertThat(e.resultado()).startsWith("fallo:");
    assertThat(e.kids()).containsExactly("k1");
    assertThat(e.cantidad()).isEqualTo(1);
    assertThat(e.actualizadoEn()).isNotNull();
  }
}
