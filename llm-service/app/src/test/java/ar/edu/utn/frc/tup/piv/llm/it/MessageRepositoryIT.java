package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.MessageRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Integración real (Postgres + Flyway) del repositorio JDBC de mensajes (EP-05/EP-09). */
class MessageRepositoryIT extends AbstractIntegrationIT {
  @Autowired MessageRepository messageRepository;
  @Autowired ConversationRepository conversationRepository;

  private UUID setupConversation() {
    Conversation conversation = Conversation.nueva(UUID.randomUUID(), UUID.randomUUID(), null, "Test");
    conversationRepository.save(conversation);
    return conversation.id();
  }

  private Message message(UUID conversationId, String role, String content, OffsetDateTime timestamp) {
    return new Message(UUID.randomUUID(), conversationId, role, content, timestamp);
  }

  @Test
  void savesAndReadsBackAMessage() {
    UUID conversationId = setupConversation();
    Message message = message(conversationId, Message.ROL_ALUMNO, "Hola tutor", OffsetDateTime.now());

    messageRepository.save(message);

    var messages = messageRepository.findByConversationId(conversationId);
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).contenido()).isEqualTo("Hola tutor");
    assertThat(messages.get(0).rol()).isEqualTo(Message.ROL_ALUMNO);
  }

  @Test
  void findByConversationIdOrdersByCreatedAtAscending() {
    UUID conversationId = setupConversation();
    OffsetDateTime base = OffsetDateTime.now();
    messageRepository.save(message(conversationId, Message.ROL_ALUMNO, "primero", base.minusMinutes(3)));
    messageRepository.save(message(conversationId, Message.ROL_TUTOR, "segundo", base.minusMinutes(2)));
    messageRepository.save(message(conversationId, Message.ROL_ALUMNO, "tercero", base));

    var messages = messageRepository.findByConversationId(conversationId);

    assertThat(messages).extracting(Message::contenido).containsExactly("primero", "segundo", "tercero");
  }

  @Test
  void findByConversationIdIsEmptyForAnUnknownConversation() {
    assertThat(messageRepository.findByConversationId(UUID.randomUUID())).isEmpty();
  }

  @Test
  void messagesFromDifferentConversationsDoNotMix() {
    UUID conversationA = setupConversation();
    UUID conversationB = setupConversation();
    messageRepository.save(message(conversationA, Message.ROL_ALUMNO, "mensaje A", OffsetDateTime.now()));
    messageRepository.save(message(conversationB, Message.ROL_ALUMNO, "mensaje B", OffsetDateTime.now()));

    assertThat(messageRepository.findByConversationId(conversationA)).extracting(Message::contenido)
        .containsExactly("mensaje A");
    assertThat(messageRepository.findByConversationId(conversationB)).extracting(Message::contenido)
        .containsExactly("mensaje B");
  }

  @Test
  void savingAMessageForAnUnknownConversationViolatesTheForeignKey() {
    Message orphan = message(UUID.randomUUID(), Message.ROL_ALUMNO, "huerfano", OffsetDateTime.now());

    assertThatThrownBy(() -> messageRepository.save(orphan))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
