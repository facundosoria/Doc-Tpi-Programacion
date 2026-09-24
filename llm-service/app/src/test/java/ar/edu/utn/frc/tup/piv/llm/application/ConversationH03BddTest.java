package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.ConversationService;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeNotActiveException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatus;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationOwnershipException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.MessageRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.external.FakeChallengeStatusAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los cuatro escenarios BDD de la historia LLM-EP05-H03 (T7).
 */
class ConversationH03BddTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private FakeChallengeStatusAdapter challengeStatusAdapter;
  private InMemoryConversationRepository conversationRepo;
  private InMemoryMessageRepository messageRepo;
  private ConversationService conversationService;
  private CallerIdentity actor;

  @BeforeEach
  void setUp() {
    challengeStatusAdapter = new FakeChallengeStatusAdapter();
    conversationRepo = new InMemoryConversationRepository();
    messageRepo = new InMemoryMessageRepository();
    IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
    when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
    AuditRepository audit = mock(AuditRepository.class);

    conversationService = new ConversationService(
        conversationRepo,
        messageRepo,
        idempotency,
        audit,
        mapper,
        challengeStatusAdapter);

    actor = new CallerIdentity("practice-service", UUID.randomUUID(), "req-bdd", null);
  }

  @Test
  @DisplayName("Escenario 1 — Retomar la conversación (camino correcto)")
  void escenario1_retomarConversacionCaminoFeliz() {
    // Dado: un alumno autenticado con una conversación dentro de un desafío activo y 3 mensajes previos
    UUID learnerId = actor.delegatedUserId();
    UUID courseCohortId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    challengeStatusAdapter.setStatus(challengeId, ChallengeStatus.ABIERTO);

    Conversation conversacion = conversationRepo.save(
        Conversation.nueva(courseCohortId, learnerId, challengeId, "Desafío con tutor"));

    Message m1 = messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), Message.ROL_ALUMNO,
        "¿Cómo resuelvo el ejercicio?", OffsetDateTime.now().minusMinutes(10)));
    Message m2 = messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), Message.ROL_TUTOR,
        "¿Qué intentaste hasta ahora?", OffsetDateTime.now().minusMinutes(8)));
    Message m3 = messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), Message.ROL_ALUMNO,
        "Intenté usar un bucle for.", OffsetDateTime.now().minusMinutes(5)));

    // Cuando: pide el historial de esa conversación
    List<Message> historial = conversationService.messages(conversacion.id(), learnerId);

    // Entonces: recibe los 3 mensajes en orden cronológico, identificados por rol
    assertThat(historial).hasSize(3);
    assertThat(historial.get(0).contenido()).isEqualTo(m1.contenido());
    assertThat(historial.get(0).rol()).isEqualTo(Message.ROL_ALUMNO);
    assertThat(historial.get(1).contenido()).isEqualTo(m2.contenido());
    assertThat(historial.get(1).rol()).isEqualTo(Message.ROL_TUTOR);
    assertThat(historial.get(2).contenido()).isEqualTo(m3.contenido());
    assertThat(historial.get(2).rol()).isEqualTo(Message.ROL_ALUMNO);

    // Y: puede enviar un mensaje nuevo que queda registrado como cuarto mensaje
    Message nuevoMensaje = conversationService.appendMessage(conversacion.id(), learnerId,
        "Pensaba sumar cada elemento a una lista.");
    assertThat(nuevoMensaje.rol()).isEqualTo(Message.ROL_ALUMNO);

    List<Message> historialActualizado = conversationService.messages(conversacion.id(), learnerId);
    assertThat(historialActualizado).hasSize(4);
    assertThat(historialActualizado.get(3).contenido()).isEqualTo("Pensaba sumar cada elemento a una lista.");
  }

  @Test
  @DisplayName("Escenario 2 — Intento de acceder a la conversación de otro alumno (caso que debe fallar)")
  void escenario2_intentoDeAccederAConversacionAjena() {
    // Dado: un alumno autenticado que conoce el UUID de la conversación de otro alumno
    UUID alumnoDueno = UUID.randomUUID();
    UUID alumnoAjeno = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    challengeStatusAdapter.setStatus(challengeId, ChallengeStatus.ABIERTO);

    Conversation conversacion = conversationRepo.save(
        Conversation.nueva(UUID.randomUUID(), alumnoDueno, challengeId, "Conversación privada"));
    messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), Message.ROL_ALUMNO, "Mensaje secreto", OffsetDateTime.now()));

    // Cuando: el alumno ajeno solicita el historial
    // Entonces: el sistema responde con ConversationOwnershipException (403), sin revelar datos
    assertThatThrownBy(() -> conversationService.messages(conversacion.id(), alumnoAjeno))
        .isInstanceOf(ConversationOwnershipException.class)
        .hasMessage("Conversación no encontrada: " + conversacion.id());

    // Y: si intenta enviar un mensaje a la conversación ajena también es rechazado
    assertThatThrownBy(() -> conversationService.appendMessage(conversacion.id(), alumnoAjeno, "Intrusión"))
        .isInstanceOf(ConversationOwnershipException.class);
  }

  @Test
  @DisplayName("Escenario 3 — Intento de continuar una conversación con desafío cerrado (caso que debe fallar)")
  void escenario3_intentoDeContinuarConversacionConDesafioCerrado() {
    // Dado: un alumno autenticado con una conversación dentro de un desafío que ya fue cerrado
    UUID learnerId = actor.delegatedUserId();
    UUID challengeId = UUID.randomUUID();
    challengeStatusAdapter.setStatus(challengeId, ChallengeStatus.CERRADO);

    Conversation conversacion = conversationRepo.save(
        Conversation.nueva(UUID.randomUUID(), learnerId, challengeId, "Desafío cerrado"));
    messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), Message.ROL_ALUMNO, "Mensaje inicial", OffsetDateTime.now()));

    // Cuando: intenta enviar un nuevo mensaje a esa conversación
    // Entonces: el sistema responde que no está disponible (404), sin agregar ningún mensaje
    assertThatThrownBy(() -> conversationService.appendMessage(conversacion.id(), learnerId, "Nuevo mensaje en cerrado"))
        .isInstanceOf(ChallengeNotActiveException.class)
        .hasMessage("Desafío no encontrado o no disponible: " + challengeId);

    // Verifica que nada nuevo se haya guardado
    List<Message> mensajes = messageRepo.findByConversationId(conversacion.id());
    assertThat(mensajes).hasSize(1);
    assertThat(mensajes.get(0).contenido()).isEqualTo("Mensaje inicial");
  }

  @Test
  @DisplayName("Escenario 4 — Conversación con desafío inexistente (caso límite)")
  void escenario4_conversacionConDesafioInexistente() {
    // Dado: un desafío que no existe en el sistema
    UUID challengeIdInexistente = UUID.randomUUID();
    challengeStatusAdapter.setStatus(challengeIdInexistente, ChallengeStatus.NO_EXISTE);

    // Cuando: intenta crear una conversación asociada a ese desafío
    // Entonces: el sistema avisa que no se encontró con el MISMO mensaje de error que en desafío cerrado
    assertThatThrownBy(() -> conversationService.create(
        UUID.randomUUID(), actor.delegatedUserId(), challengeIdInexistente, "Nueva charla", UUID.randomUUID(), actor))
        .isInstanceOf(ChallengeNotActiveException.class)
        .hasMessage("Desafío no encontrado o no disponible: " + challengeIdInexistente);

    // Y: no se creó ningún registro en el repositorio de conversaciones
    assertThat(conversationRepo.find(actor.delegatedUserId(), null)).isEmpty();
  }

  // --- Implementaciones en memoria de repositorios para aislamiento total en tests ---

  static class InMemoryConversationRepository implements ConversationRepository {
    private final Map<UUID, Conversation> store = new ConcurrentHashMap<>();

    @Override
    public Conversation save(Conversation conversation) {
      store.put(conversation.id(), conversation);
      return conversation;
    }

    @Override
    public Optional<Conversation> findById(UUID id) {
      return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Conversation> find(UUID learnerId, UUID courseCohortId, UUID challengeId) {
      return store.values().stream()
          .filter(c -> learnerId == null || learnerId.equals(c.learnerId()))
          .filter(c -> courseCohortId == null || courseCohortId.equals(c.courseCohortId()))
          .filter(c -> challengeId == null || challengeId.equals(c.challengeId()))
          .sorted(Comparator.comparing(Conversation::createdAt).reversed())
          .toList();
    }
  }

  static class InMemoryMessageRepository implements MessageRepository {
    private final List<Message> store = new ArrayList<>();

    @Override
    public synchronized Message save(Message message) {
      store.add(message);
      return message;
    }

    @Override
    public synchronized List<Message> findByConversationId(UUID conversationId) {
      return store.stream()
          .filter(m -> m.conversationId().equals(conversationId))
          .sorted(Comparator.comparing(Message::timestamp))
          .toList();
    }
  }
}
