package ar.edu.utn.frc.tup.piv.llm.adapter.out.http;

import ar.edu.utn.frc.tup.piv.llm.application.port.out.CourseMembershipPort;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Synchronous M2M query to Courses through the platform Gateway. */
@Component
public class GatewayCoursesMembershipClient implements CourseMembershipPort {
  private final RestClient client;
  private final String serviceToken;

  public GatewayCoursesMembershipClient(RestClient.Builder builder,
      @Value("${llm.courses.base-url}") String gatewayBaseUrl,
      @Value("${llm.courses.service-token}") String serviceToken) {
    this.client = builder.baseUrl(gatewayBaseUrl).build();
    this.serviceToken = serviceToken;
  }

  @Override
  public Membership membership(UUID courseCohortId, UUID userId, CallerIdentity caller) {
    if (courseCohortId == null || userId == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cohorte o usuario delegado inválido");
    }
    try {
      MembershipResponse response = client.get()
          .uri("/api/courses/{courseCohortId}/members/{userId}", courseCohortId, userId)
          .headers(headers -> propagateCorrelation(headers, caller))
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(MembershipResponse.class);
      if (response == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Matrícula no encontrada");
      return new Membership(response.role(), response.status());
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().is4xxClientError()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El actor no pertenece a la cohorte", exception);
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Courses no está disponible", exception);
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Courses no está disponible", exception);
    }
  }

  private void propagateCorrelation(HttpHeaders headers, CallerIdentity caller) {
    headers.setBearerAuth(serviceToken);
    headers.set("X-Request-Id", caller != null && caller.requestId() != null ? caller.requestId() : UUID.randomUUID().toString());
    if (caller != null && caller.traceparent() != null && !caller.traceparent().isBlank()) {
      headers.set("traceparent", caller.traceparent());
    }
  }

  private record MembershipResponse(String role, String status) {}
}
