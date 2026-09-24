package ar.edu.utn.frc.tup.piv.llm.application.service.agent;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepción de cuota diaria agotada (HU LLM-S18-H01 · T5).
 * Emite HTTP 429 con cabecera Retry-After.
 */
public class QuotaExceededException extends ResponseStatusException {
  private final String functionKey;
  private final long retryAfterSeconds;

  public QuotaExceededException(String functionKey, long retryAfterSeconds, String message) {
    super(HttpStatus.TOO_MANY_REQUESTS, message);
    this.functionKey = functionKey;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  @Override
  public HttpHeaders getHeaders() {
    return createHeaders(retryAfterSeconds);
  }

  private static HttpHeaders createHeaders(long retryAfterSeconds) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    return headers;
  }

  public String getFunctionKey() {
    return functionKey;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
