package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.TutorGatewayAuthorization;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.TutorInteractionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;

class TutorInteractionControllerCoverageTest {
  private final HttpHeaders headers = new HttpHeaders();
  private final CallerIdentity actor = new CallerIdentity("practice-service", UUID.randomUUID(), null, null);

  @Test
  void rejectsRequestMissingMandatoryIds() {
    var controller = controllerWithValidIdentity();
    var body = new TutorInteractionController.Request(null, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), "hola", "low", null);

    assertThatThrownBy(() -> controller.create(body, UUID.randomUUID(), headers))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsAnUnknownRiskLevel() {
    var controller = controllerWithValidIdentity();
    var body = new TutorInteractionController.Request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), "hola", "extremo", null);

    assertThatThrownBy(() -> controller.create(body, UUID.randomUUID(), headers))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void mapsTheRequestBodyIntoTheServiceRequestAndDelegates() {
    var service = mock(TutorInteractionService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    when(authorization.require(headers)).thenReturn(actor);
    var controller = new TutorInteractionController(service, authorization);
    UUID attemptId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseCohortId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    var body = new TutorInteractionController.Request(attemptId, challengeId, courseCohortId, learnerId,
        "¿cómo ordeno?", "medium", null, "return x;");

    controller.create(body, UUID.randomUUID(), headers);

    var captor = ArgumentCaptor.forClass(TutorInteractionService.Request.class);
    verify(service).respond(captor.capture(), any(), eq(actor));
    assertThat(captor.getValue()).isEqualTo(new TutorInteractionService.Request(attemptId, challengeId,
        courseCohortId, learnerId, "¿cómo ordeno?", "medium", null, "return x;"));
  }

  private TutorInteractionController controllerWithValidIdentity() {
    var authorization = mock(TutorGatewayAuthorization.class);
    when(authorization.require(headers)).thenReturn(actor);
    return new TutorInteractionController(mock(TutorInteractionService.class), authorization);
  }
}