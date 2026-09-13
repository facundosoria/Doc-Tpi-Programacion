package ar.edu.utn.frc.tup.piv.llm.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.TutorInteractionService;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.TutorGatewayAuthorization;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

class TutorInteractionControllerTest {

  @Test
  void authorizesBeforeCallingTheService() {
    var service = mock(TutorInteractionService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    var actor = new CallerIdentity("practice-service", UUID.randomUUID(), null, null);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    var expected = new TutorInteractionService.Response("hola", "completed");
    when(service.respond(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(actor)))
        .thenReturn(expected);
    var controller = new TutorInteractionController(service, authorization);
    var body = new TutorInteractionController.Request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hola", "low");

    var response = controller.create(body, UUID.randomUUID(), headers);

    assertThat(response).isEqualTo(expected);
    var order = Mockito.inOrder(authorization, service);
    order.verify(authorization).require(headers);
    order.verify(service).respond(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(actor));
  }

  @Test
  void rejectsARequestWithoutARiskLevel() {
    var service = mock(TutorInteractionService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("practice-service", UUID.randomUUID(), null, null));
    var controller = new TutorInteractionController(service, authorization);
    var body = new TutorInteractionController.Request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hola", null);

    assertThatThrownBy(() -> controller.create(body, UUID.randomUUID(), headers)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsABlankMessage() {
    var service = mock(TutorInteractionService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("practice-service", UUID.randomUUID(), null, null));
    var controller = new TutorInteractionController(service, authorization);
    var body = new TutorInteractionController.Request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "  ", "low");

    assertThatThrownBy(() -> controller.create(body, UUID.randomUUID(), headers)).isInstanceOf(IllegalArgumentException.class);
  }
}
