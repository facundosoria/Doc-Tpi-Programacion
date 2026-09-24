package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.RequestCorrelationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {
  private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

  @Test
  void echoesIncomingRequestId() throws ServletException, IOException {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "client-request-id-42");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("X-Request-Id")).isEqualTo("client-request-id-42");
    assertThat(request.getAttribute("X-Request-Id")).isEqualTo("client-request-id-42");
  }

  @Test
  void generatesRequestIdWhenAbsent() throws ServletException, IOException {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    String generatedId = response.getHeader("X-Request-Id");
    assertThat(generatedId).isNotBlank();
    assertThat(UUID.fromString(generatedId)).isNotNull();
    assertThat(request.getAttribute("X-Request-Id")).isEqualTo(generatedId);
  }

  @Test
  void retainsHeaderEvenWhenFilterChainThrowsException() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "error-flow-id");
    var response = new MockHttpServletResponse();
    FilterChain throwingChain = (req, res) -> {
      throw new RuntimeException("Error simulado en controlador o downstream filter");
    };

    assertThatThrownBy(() -> filter.doFilter(request, response, throwingChain))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Error simulado en controlador o downstream filter");

    assertThat(response.getHeader("X-Request-Id")).isEqualTo("error-flow-id");
  }

  @Test
  void extractsTraceIdToMdcAndCleansUp() throws ServletException, IOException {
    var request = new MockHttpServletRequest();
    request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
    var response = new MockHttpServletResponse();

    AtomicReference<String> mdcTraceIdDuringChain = new AtomicReference<>();
    FilterChain capturingChain = (req, res) -> {
      mdcTraceIdDuringChain.set(MDC.get("traceId"));
    };

    filter.doFilter(request, response, capturingChain);

    assertThat(mdcTraceIdDuringChain.get()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    assertThat(MDC.get("traceId")).isNull();
  }
}
