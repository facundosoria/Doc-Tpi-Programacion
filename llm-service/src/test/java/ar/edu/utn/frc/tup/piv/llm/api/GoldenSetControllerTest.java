package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.GoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.domain.GoldenSet;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldenSetControllerTest {
  private final GoldenSetService service = mock(GoldenSetService.class);
  private final GoldenSetAuthorization authorization = mock(GoldenSetAuthorization.class);
  private final GoldenSetController controller = new GoldenSetController(service, authorization);
  private final ObjectMapper mapper = new ObjectMapper();
  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), "req-1", null);

  @Test void createReturns201AndResourceLocation() throws Exception {
    UUID id = UUID.randomUUID(); UUID key = UUID.randomUUID(); HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    when(service.create("1.0", "es", key, actor)).thenReturn(mapper.readTree("{\"id\":\"" + id + "\"}"));

    var response = controller.create(new CreateGoldenSetRequest("1.0", "es"), key, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/llm/golden-sets/" + id);
    assertThat(response.getBody()).isNotNull();
  }

  @Test void listAuthorizesCallerAndForwardsPagination() {
    HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    GoldenSet set = new GoldenSet(UUID.randomUUID(), 1, "1.0", "es", OffsetDateTime.now(), List.of());
    when(service.list("1.0", 1, 10)).thenReturn(List.of(set));

    assertThat(controller.list("1.0", 1, 10, headers)).containsExactly(set);

    verify(authorization).require(headers);
    verify(service).list("1.0", 1, 10);
  }

  @Test void getAuthorizesCallerBeforeLoadingResource() {
    UUID id = UUID.randomUUID(); HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    GoldenSet set = new GoldenSet(id, 1, "1.0", "es", OffsetDateTime.now(), List.of());
    when(service.get(id)).thenReturn(set);

    assertThat(controller.get(id, headers)).isEqualTo(set);
    verify(authorization).require(headers);
    verify(service).get(id);
  }
}
