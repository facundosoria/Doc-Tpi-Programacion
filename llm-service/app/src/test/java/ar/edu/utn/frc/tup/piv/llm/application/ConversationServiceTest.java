package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.ConversationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeNotActiveException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatus;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatusPort;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationOwnershipException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.MessageRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationServiceTest {
  // findAndRegisterModules() trae JavaTimeModule (necesario para OffsetDateTime en Conversation),
  // igual que hace el ObjectMapper autoconfigurado por Spring Boot en producción.
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final CallerIdentity actor = new CallerIdentity("practice-service", UUID.randomUUID(), "req-1", null);

  @Test
  void requiresCourseCohortAndLearner() {
    var service = new ConversationService(mock(ConversationRepository.class), mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    assertThatThrownBy(() -> service.create(null, UUID.randomUUID(), null, "t", UUID.randomUUID(), actor))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createsAndPersistsANewConversation() {
    var conversations = mock(ConversationRepository.class);
    when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var audit = mock(AuditRepository.class);
    var service = new ConversationService(conversations, mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), audit, mapper);

    var conversation = service.create(UUID.randomUUID(), UUID.randomUUID(), null, "Mi conversación", UUID.randomUUID(), actor);

    assertThat(conversation.titulo()).isEqualTo("Mi conversación");
    verify(audit).record(org.mockito.ArgumentMatchers.eq("tutor.conversation.create"), any(), any(), any(), any());
  }

  @Test
  void retryingWithTheSameIdempotencyKeyDoesNotCreateASecondConversation() throws Exception {
    var conversations = mock(ConversationRepository.class);
    var idempotency = mock(IdempotencyRepository.class);
    UUID courseCohortId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    var stored = Conversation.nueva(courseCohortId, learnerId, null, "Reintento");
    when(idempotency.replay(org.mockito.ArgumentMatchers.eq("tutor.conversation.create"), any(), any(), any()))
        .thenReturn(Optional.of(mapper.valueToTree(stored)));
    var service = new ConversationService(conversations, mock(MessageRepository.class), idempotency, mock(AuditRepository.class), mapper);

    var conversation = service.create(courseCohortId, learnerId, null, "Reintento", UUID.randomUUID(), actor);

    assertThat(conversation.titulo()).isEqualTo("Reintento");
    verify(conversations, never()).save(any());
  }

  @Test
  void messagesThrowsWhenTheConversationDoesNotExist() {
    var conversations = mock(ConversationRepository.class);
    when(conversations.findById(any())).thenReturn(Optional.empty());
    var service = new ConversationService(conversations, mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    assertThatThrownBy(() -> service.messages(UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void messagesReturnsHistoryWhenLearnerIsOwner() {
    var conversations = mock(ConversationRepository.class);
    var messages = mock(MessageRepository.class);
    UUID conversationId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    var conversation = Conversation.nueva(UUID.randomUUID(), learnerId, null, "Mi conversación");
    when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
    var expected = List.of(new Message(UUID.randomUUID(), conversationId, "alumno", "hola", java.time.OffsetDateTime.now()));
    when(messages.findByConversationId(conversationId)).thenReturn(expected);
    var service = new ConversationService(conversations, messages, idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    var result = service.messages(conversationId, learnerId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void messagesThrowsOwnershipExceptionWhenLearnerIsNotOwner() {
    var conversations = mock(ConversationRepository.class);
    UUID conversationId = UUID.randomUUID();
    UUID ownerLearnerId = UUID.randomUUID();
    UUID strangerLearnerId = UUID.randomUUID();
    var conversation = Conversation.nueva(UUID.randomUUID(), ownerLearnerId, null, "Mi conversación");
    when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
    var service = new ConversationService(conversations, mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    assertThatThrownBy(() -> service.messages(conversationId, strangerLearnerId))
        .isInstanceOf(ConversationOwnershipException.class)
        .hasMessage("Conversación no encontrada: " + conversationId);
  }

  @Test
  void listRequiresLearnerId() {
    var service = new ConversationService(mock(ConversationRepository.class), mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    assertThatThrownBy(() -> service.list(null, UUID.randomUUID(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("learnerId es obligatorio");
  }

  @Test
  void listFiltersByLearnerAndChallengeId() {
    var conversations = mock(ConversationRepository.class);
    UUID learnerId = UUID.randomUUID();
    UUID courseCohortId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    var expected = List.of(Conversation.nueva(courseCohortId, learnerId, challengeId, "Desafío"));
    when(conversations.find(learnerId, courseCohortId, challengeId)).thenReturn(expected);
    var service = new ConversationService(conversations, mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    var result = service.list(learnerId, courseCohortId, challengeId);

    assertThat(result).isEqualTo(expected);
    verify(conversations).find(learnerId, courseCohortId, challengeId);
  }

  @Test
  void appendMessageSavesAlumnoMessageWhenLearnerIsOwner() {
    var conversations = mock(ConversationRepository.class);
    var messages = mock(MessageRepository.class);
    UUID conversationId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    var conversation = new Conversation(conversationId, UUID.randomUUID(), learnerId, null, "Mi conversación",
        Conversation.ESTADO_ABIERTA, java.time.OffsetDateTime.now());
    when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messages.save(any())).thenAnswer(inv -> inv.getArgument(0));
    var service = new ConversationService(conversations, messages, idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    Message result = service.appendMessage(conversationId, learnerId, "Mi nueva duda");

    assertThat(result.conversationId()).isEqualTo(conversationId);
    assertThat(result.rol()).isEqualTo("alumno");
    assertThat(result.contenido()).isEqualTo("Mi nueva duda");
    assertThat(result.timestamp()).isNotNull();
    verify(messages).save(any());
  }

  @Test
  void appendMessageThrowsWhenLearnerIsNotOwner() {
    var conversations = mock(ConversationRepository.class);
    UUID conversationId = UUID.randomUUID();
    UUID ownerLearnerId = UUID.randomUUID();
    UUID strangerLearnerId = UUID.randomUUID();
    var conversation = Conversation.nueva(UUID.randomUUID(), ownerLearnerId, null, "Mi conversación");
    when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
    var service = new ConversationService(conversations, mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper);

    assertThatThrownBy(() -> service.appendMessage(conversationId, strangerLearnerId, "Duda ajena"))
        .isInstanceOf(ConversationOwnershipException.class);
  }

  @Test
  void createThrowsWhenChallengeIsNotOpen() {
    var challengeStatusPort = mock(ChallengeStatusPort.class);
    UUID challengeId = UUID.randomUUID();
    when(challengeStatusPort.consultar(challengeId)).thenReturn(ChallengeStatus.CERRADO);
    var service = new ConversationService(mock(ConversationRepository.class), mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper, challengeStatusPort);

    assertThatThrownBy(() -> service.create(UUID.randomUUID(), UUID.randomUUID(), challengeId, "T", UUID.randomUUID(), actor))
        .isInstanceOf(ChallengeNotActiveException.class)
        .hasMessage("Desafío no encontrado o no disponible: " + challengeId);
  }

  @Test
  void appendMessageThrowsWhenChallengeIsNotOpen() {
    var conversations = mock(ConversationRepository.class);
    var challengeStatusPort = mock(ChallengeStatusPort.class);
    UUID conversationId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    var conversation = Conversation.nueva(UUID.randomUUID(), learnerId, challengeId, "Mi conversación");
    when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(challengeStatusPort.consultar(challengeId)).thenReturn(ChallengeStatus.NO_EXISTE);

    var service = new ConversationService(conversations, mock(MessageRepository.class),
        idempotencyThatAlwaysProceeds(), mock(AuditRepository.class), mapper, challengeStatusPort);

    assertThatThrownBy(() -> service.appendMessage(conversationId, learnerId, "Mensaje"))
        .isInstanceOf(ChallengeNotActiveException.class)
        .hasMessage("Desafío no encontrado o no disponible: " + challengeId);
  }

  private IdempotencyRepository idempotencyThatAlwaysProceeds() {
    var idempotency = mock(IdempotencyRepository.class);
    when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
    return idempotency;
  }
}
