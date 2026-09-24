package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ConversationController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.ConversationService;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.TutorGatewayAuthorization;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

class ConversationControllerTest {

  @Test
  void authorizesBeforeCreatingAConversation() {
    var service = mock(ConversationService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    UUID learnerId = UUID.randomUUID();
    var actor = new CallerIdentity("practice-service", learnerId, null, null);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    var body = new ConversationController.CreateRequest(UUID.randomUUID(), learnerId, null, "Mi conversación");
    var idempotencyKey = UUID.randomUUID();
    var expected = Conversation.nueva(body.courseCohortId(), body.learnerId(), null, body.titulo());
    when(service.create(body.courseCohortId(), body.learnerId(), body.challengeId(), body.titulo(), idempotencyKey, actor))
        .thenReturn(expected);
    var controller = new ConversationController(service, authorization);

    var response = controller.create(body, idempotencyKey, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expected);
    var order = Mockito.inOrder(authorization, service);
    order.verify(authorization).require(headers);
    order.verify(service).create(body.courseCohortId(), body.learnerId(), body.challengeId(), body.titulo(), idempotencyKey, actor);
  }

  @Test
  void authorizesBeforeListingConversations() {
    var service = mock(ConversationService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    var headers = new HttpHeaders();
    UUID learnerId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("practice-service", learnerId, null, null));
    UUID courseCohortId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    var expected = List.of(Conversation.nueva(courseCohortId, learnerId, challengeId, "t"));
    when(service.list(learnerId, courseCohortId, challengeId)).thenReturn(expected);
    var controller = new ConversationController(service, authorization);

    var result = controller.list(learnerId, courseCohortId, challengeId, headers);

    assertThat(result).isEqualTo(expected);
    verify(authorization).require(headers);
    verify(service).list(learnerId, courseCohortId, challengeId);
  }

  @Test
  void authorizesBeforeAppendingMessage() {
    var service = mock(ConversationService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    var headers = new HttpHeaders();
    UUID learnerId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("practice-service", learnerId, null, null));
    UUID conversationId = UUID.randomUUID();
    var expected = new Message(UUID.randomUUID(), conversationId, "alumno", "hola tutor", OffsetDateTime.now());
    var actor = new CallerIdentity("practice-service", learnerId, null, null);
    when(service.appendMessage(conversationId, learnerId, "hola tutor", null, actor)).thenReturn(expected);
    var controller = new ConversationController(service, authorization);

    var response = controller.appendMessage(conversationId, new ConversationController.AppendMessageRequest("hola tutor"), null, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expected);
    verify(authorization).require(headers);
    verify(service).appendMessage(conversationId, learnerId, "hola tutor", null, actor);
  }

  @Test
  void authorizesBeforeReadingMessages() {
    var service = mock(ConversationService.class);
    var authorization = mock(TutorGatewayAuthorization.class);
    var headers = new HttpHeaders();
    UUID learnerId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("practice-service", learnerId, null, null));
    UUID conversationId = UUID.randomUUID();
    var expected = List.of(new Message(UUID.randomUUID(), conversationId, "alumno", "hola", OffsetDateTime.now()));
    when(service.messages(conversationId, learnerId)).thenReturn(expected);
    var controller = new ConversationController(service, authorization);

    var result = controller.messages(conversationId, headers);

    assertThat(result).isEqualTo(expected);
    verify(authorization).require(headers);
    verify(service).messages(conversationId, learnerId);
  }
}
