package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de correlación transversal (CA2 / H03·T7).
 * Garantiza que toda respuesta (éxito o error) devuelva la cabecera X-Request-Id:
 * preservando la recibida o generando un UUID si no fue provista.
 * Además, si se recibe traceparent (W3C Trace Context), inyecta el traceId en el MDC para logging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RequestCorrelationFilter.class);
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String REQUEST_ID_MDC_KEY = "requestId";
  public static final String TRACEPARENT_HEADER = "traceparent";
  public static final String TRACE_ID_MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long inicio = System.nanoTime();
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    response.setHeader(REQUEST_ID_HEADER, requestId);
    request.setAttribute(REQUEST_ID_HEADER, requestId);
    MDC.put(REQUEST_ID_MDC_KEY, requestId);

    String traceparent = request.getHeader(TRACEPARENT_HEADER);
    boolean traceIdSet = false;
    if (traceparent != null && !traceparent.isBlank()) {
      String traceId = extractTraceId(traceparent);
      MDC.put(TRACE_ID_MDC_KEY, traceId);
      traceIdSet = true;
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
          response.getStatus(), (System.nanoTime() - inicio) / 1_000_000);
      MDC.remove(REQUEST_ID_MDC_KEY);
      if (traceIdSet) {
        MDC.remove(TRACE_ID_MDC_KEY);
      }
    }
  }

  private String extractTraceId(String traceparent) {
    String[] parts = traceparent.split("-");
    if (parts.length >= 4 && parts[0].length() == 2) {
      return parts[1];
    }
    return traceparent;
  }
}
