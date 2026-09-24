package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationBranchesTest {
  private final UUID course = UUID.randomUUID(), learner = UUID.randomUUID(), challenge = UUID.randomUUID();

  @Test
  void blankOrNullTitleFallsBackToDefaultAndTitleIsTrimmed() {
    assertThat(Conversation.nueva(course, learner, challenge, null).titulo()).isEqualTo("Nueva conversación");
    assertThat(Conversation.nueva(course, learner, challenge, "   ").titulo()).isEqualTo("Nueva conversación");
    assertThat(Conversation.nueva(course, learner, challenge, "  Pilas ").titulo()).isEqualTo("Pilas");
  }

  @Test
  void newConversationIsOpenAndCreatesMessagesWithRoles() {
    var c = Conversation.nueva(course, learner, challenge, "t");
    assertThat(c.isAbierta()).isTrue();
    assertThat(c.crearMensajeAlumno("hola").rol()).isEqualTo(Message.ROL_ALUMNO);
    var m = c.crearMensajeTutor("respuesta");
    assertThat(m.rol()).isEqualTo(Message.ROL_TUTOR);
    assertThat(m.conversationId()).isEqualTo(c.id());
    assertThat(m.contenido()).isEqualTo("respuesta");
  }

  @Test
  void closedConversationKeepsIdentityAndRejectsMessages() {
    var c = Conversation.nueva(course, learner, challenge, "t");
    var closed = c.cerrar();
    assertThat(closed.isAbierta()).isFalse();
    assertThat(closed.id()).isEqualTo(c.id());
    assertThat(closed.estado()).isEqualTo(Conversation.ESTADO_CERRADA);
    assertThatThrownBy(() -> closed.crearMensajeAlumno("x")).isInstanceOf(IllegalStateException.class).hasMessageContaining("cerrada");
    assertThatThrownBy(() -> closed.crearMensajeTutor("x")).isInstanceOf(IllegalStateException.class).hasMessageContaining("cerrada");
  }

  @Test
  void stateComparisonIsCaseInsensitive() {
    var c = new Conversation(UUID.randomUUID(), course, learner, challenge, "t", "abierta", null);
    assertThat(c.isAbierta()).isTrue();
  }
}
