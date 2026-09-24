package ar.edu.utn.frc.tup.piv.llm.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HttpClientConfigTest {
  private HttpServer server;

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    if (server != null) server.stop(0);
  }

  private RestClient client(AtomicReference<com.sun.net.httpserver.Headers> seen) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/x", ex -> {
      seen.set(ex.getRequestHeaders());
      ex.sendResponseHeaders(204, -1);
      ex.close();
    });
    server.start();
    var config = new HttpClientConfig();
    return config.gatewayRestClient(config.restClientBuilder(), "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @Test
  void propagatesTraceHeadersFromIncomingRequest() throws Exception {
    var seen = new AtomicReference<com.sun.net.httpserver.Headers>();
    var rc = client(seen);
    var req = new MockHttpServletRequest();
    req.addHeader("traceparent", "00-abc-def-01");
    req.addHeader(IdentityHeaders.REQUEST_ID, "req-1");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

    rc.get().uri("/x").retrieve().toBodilessEntity();

    assertThat(seen.get().getFirst("traceparent")).isEqualTo("00-abc-def-01");
    assertThat(seen.get().getFirst(IdentityHeaders.REQUEST_ID)).isEqualTo("req-1");
  }

  @Test
  void skipsBlankOrMissingHeaders() throws Exception {
    var seen = new AtomicReference<com.sun.net.httpserver.Headers>();
    var rc = client(seen);
    var req = new MockHttpServletRequest();
    req.addHeader("traceparent", "  ");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

    rc.get().uri("/x").retrieve().toBodilessEntity();

    assertThat(seen.get().getFirst("traceparent")).isNull();
    assertThat(seen.get().getFirst(IdentityHeaders.REQUEST_ID)).isNull();
  }

  @Test
  void worksWithoutIncomingRequestContext() throws Exception {
    var seen = new AtomicReference<com.sun.net.httpserver.Headers>();
    var rc = client(seen);
    rc.get().uri("/x").retrieve().toBodilessEntity();
    assertThat(seen.get().getFirst("traceparent")).isNull();
  }
}
