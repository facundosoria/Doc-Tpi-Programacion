package ar.edu.utn.frc.tup.piv.llm.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.application.service.ConversationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.TutorInteractionService;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
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
import org.mockito.ArgumentCaptor;

/**
 * Pruebas de los Criterios de Aceptación y Escenarios BDD de la historia LLM-EP05-H02.
 * Cubre:
 * - CA1 / Escenario 1: Crear y retomar una conversación considerando turnos previos.
 * - CA2: Idempotencia en la creación (repetir no duplica).
 * - CA3: Listar conversaciones filtradas por alumno o curso/cohorte.
 * - CA4: Historial de mensajes ordenado cronológicamente.
 * - CA5 / Escenario 2: Conversación inexistente responde caso explícito "no encontrada".
 * - CA6 / Escenario 3: Compatibilidad hacia atrás (pedido sin conversacionId funciona igual que antes).
 */
class ConversationH02BddTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private InMemoryConversationRepository conversationRepo;
  private InMemoryMessageRepository messageRepo;
  private AuditRepository auditRepo;
  private IdempotencyRepository idempotencyRepo;
  private FakeChallengeStatusAdapter challengeStatusAdapter;
  private ConversationService conversationService;
  private ModelInvocationService modelInvocationService;
  private TutorInteractionService tutorInteractionService;
  private CallerIdentity actor;

  @BeforeEach
  void setUp() {
    conversationRepo = new InMemoryConversationRepository();
    messageRepo = new InMemoryMessageRepository();
    auditRepo = mock(AuditRepository.class);
    idempotencyRepo = mock(IdempotencyRepository.class);
    when(idempotencyRepo.replay(anyString(), any(), any(), anyString())).thenReturn(Optional.empty());

    challengeStatusAdapter = new FakeChallengeStatusAdapter();
    conversationService = new ConversationService(
        conversationRepo,
        messageRepo,
        idempotencyRepo,
        auditRepo,
        mapper,
        challengeStatusAdapter);

    modelInvocationService = mock(ModelInvocationService.class);
    when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult("¿Qué estructura de datos pensaste?", "fake", "fake-socratic-v1"));

    tutorInteractionService = new TutorInteractionService(
        modelInvocationService,
        idempotencyRepo,
        auditRepo,
        conversationRepo,
        messageRepo,
        mapper,
        2000,
        challengeStatusAdapter);

    actor = new CallerIdentity("practice-service", UUID.randomUUID(), "req-h02", null);
  }

  @Test
  @DisplayName("Escenario 1 & CA1: Alumno crea una conversación y al enviar un mensaje retomándola se incluye el contexto previo")
  void bddEscenario1_crearYRetomarConversacionConContextoPrevio() {
    UUID courseCohortId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    // Dado un alumno que ya creó una conversación con el tutor
    Conversation conversacion = conversationService.create(
        courseCohortId, learnerId, challengeId, "Duda sobre ordenamiento", UUID.randomUUID(), actor);
    assertThat(conversacion.id()).isNotNull();

    // Se registran turnos iniciales en la conversación
    messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), "alumno", "¿Cómo arranco a ordenar?", OffsetDateTime.now().minusMinutes(2)));
    messageRepo.save(new Message(UUID.randomUUID(), conversacion.id(), "tutor", "¿Conocés el algoritmo de burbuja o quicksort?", OffsetDateTime.now().minusMinutes(1)));

    // Cuando envía un nuevo mensaje indicando esa conversación
    var request = new TutorInteractionService.Request(
        UUID.randomUUID(), challengeId, courseCohortId, learnerId, "Me interesa quicksort", "low", conversacion.id());

    var response = tutorInteractionService.respond(request, UUID.randomUUID(), actor);

    // Entonces la respuesta se arma considerando los últimos turnos ya registrados
    assertThat(response.conversacionId()).isEqualTo(conversacion.id());
    assertThat(response.message()).isNotBlank();
    assertThat(response.state()).isEqualTo("completed");

    ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
    verify(modelInvocationService).invoke(eq(ModelFunction.TUTOR), anyString(), userPromptCaptor.capture(), any());

    String userPrompt = userPromptCaptor.getValue();
    assertThat(userPrompt).contains("¿Cómo arranco a ordenar?");
    assertThat(userPrompt).contains("¿Conocés el algoritmo de burbuja o quicksort?");
    assertThat(userPrompt).contains("Me interesa quicksort");
  }

  @Test
  @DisplayName("Escenario 2 & CA5: Historial de una conversación inexistente responde no encontrada de forma explícita")
  void bddEscenario2_historialConversacionInexistenteFallaExplicito() {
    UUID idInexistente = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();

    // Cuando se pide su historial
    // Entonces el sistema responde el caso "no encontrada" explícito, no un error genérico
    assertThatThrownBy(() -> conversationService.messages(idInexistente, learnerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Conversación no encontrada: " + idInexistente);
  }

  @Test
  @DisplayName("Escenario 3 & CA6: Compatibilidad hacia atrás (pedido sin conversacionId funciona exactamente igual que antes)")
  void bddEscenario3_compatibilidadHaciaAtrasSinConversacionId() {
    UUID courseCohortId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    // Dado un pedido al tutor idéntico a como se armaba antes de esta historia, sin indicar conversación
    var requestSinConversacion = new TutorInteractionService.Request(
        UUID.randomUUID(), challengeId, courseCohortId, learnerId, "Primera duda sin hilo", "low", null);

    // Cuando se envía
    var response = tutorInteractionService.respond(requestSinConversacion, UUID.randomUUID(), actor);

    // Entonces el comportamiento y la respuesta son los mismos que antes (se asigna una conversación automática de 1 turno)
    assertThat(response.message()).isNotBlank();
    assertThat(response.state()).isEqualTo("completed");
    assertThat(response.conversacionId()).isNotNull();

    // Y queda persistida la conversación y el mensaje
    Optional<Conversation> convGuardada = conversationRepo.findById(response.conversacionId());
    assertThat(convGuardada).isPresent();
    assertThat(convGuardada.get().learnerId()).isEqualTo(learnerId);

    List<Message> msgs = messageRepo.findByConversationId(response.conversacionId());
    assertThat(msgs).hasSize(2); // alumno y tutor
    assertThat(msgs.get(0).rol()).isEqualTo("alumno");
    assertThat(msgs.get(0).contenido()).isEqualTo("Primera duda sin hilo");
    assertThat(msgs.get(1).rol()).isEqualTo("tutor");
  }

  @Test
  @DisplayName("CA2: Reintentar la creación de una conversación con la misma Idempotency-Key no crea un duplicado")
  void ca2_idempotenciaEnCreacionNoCreaDuplicados() {
    UUID courseCohortId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();

    Conversation primera = conversationService.create(courseCohortId, learnerId, challengeId, "Título", idempotencyKey, actor);

    // Simulamos que idempotency retorna la versión guardada en el reintento
    when(idempotencyRepo.replay(eq("tutor.conversation.create"), any(), eq(idempotencyKey), anyString()))
        .thenReturn(Optional.of(mapper.valueToTree(primera)));

    Conversation segunda = conversationService.create(courseCohortId, learnerId, challengeId, "Título", idempotencyKey, actor);

    assertThat(segunda.id()).isEqualTo(primera.id());
    assertThat(conversationRepo.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("CA3: Listar conversaciones permite filtrar por learnerId y courseCohortId")
  void ca3_listarConversacionesFiltradas() {
    UUID alumnoA = UUID.randomUUID();
    UUID alumnoB = UUID.randomUUID();
    UUID cohorte1 = UUID.randomUUID();
    UUID cohorte2 = UUID.randomUUID();

    conversationRepo.save(Conversation.nueva(cohorte1, alumnoA, null, "Conv A1"));
    conversationRepo.save(Conversation.nueva(cohorte2, alumnoA, null, "Conv A2"));
    conversationRepo.save(Conversation.nueva(cohorte1, alumnoB, null, "Conv B1"));

    List<Conversation> filtroAlumnoA = conversationService.list(alumnoA, null, null);
    assertThat(filtroAlumnoA).extracting(Conversation::titulo).containsExactlyInAnyOrder("Conv A1", "Conv A2");

    List<Conversation> filtroAlumnoACohorte1 = conversationService.list(alumnoA, cohorte1, null);
    assertThat(filtroAlumnoACohorte1).extracting(Conversation::titulo).containsExactly("Conv A1");
  }

  @Test
  @DisplayName("CA4: El historial de una conversación devuelve todos sus mensajes ordenados por fecha ascendente")
  void ca4_historialDeMensajesOrdenadoPorFecha() {
    UUID convId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    conversationRepo.save(new Conversation(convId, UUID.randomUUID(), learnerId, null, "Historial", "ABIERTA", OffsetDateTime.now()));

    OffsetDateTime t1 = OffsetDateTime.now().minusMinutes(10);
    OffsetDateTime t2 = OffsetDateTime.now().minusMinutes(5);
    OffsetDateTime t3 = OffsetDateTime.now().minusMinutes(1);

    // Guardados desordenados intencionalmente
    messageRepo.save(new Message(UUID.randomUUID(), convId, "alumno", "mensaje 2", t2));
    messageRepo.save(new Message(UUID.randomUUID(), convId, "alumno", "mensaje 1", t1));
    messageRepo.save(new Message(UUID.randomUUID(), convId, "tutor", "mensaje 3", t3));

    List<Message> historial = conversationService.messages(convId, learnerId);

    assertThat(historial).extracting(Message::contenido).containsExactly("mensaje 1", "mensaje 2", "mensaje 3");
  }

  // Repositorios en memoria para pruebas de ciclo completo de conversación
  static class InMemoryConversationRepository implements ConversationRepository {
    private final Map<UUID, Conversation> storage = new ConcurrentHashMap<>();

    @Override
    public Conversation save(Conversation conversation) {
      storage.put(conversation.id(), conversation);
      return conversation;
    }

    @Override
    public Optional<Conversation> findById(UUID id) {
      return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Conversation> find(UUID learnerId, UUID courseCohortId, UUID challengeId) {
      return storage.values().stream()
          .filter(c -> learnerId == null || c.learnerId().equals(learnerId))
          .filter(c -> courseCohortId == null || c.courseCohortId().equals(courseCohortId))
          .filter(c -> challengeId == null || (c.challengeId() != null && c.challengeId().equals(challengeId)))
          .sorted(Comparator.comparing(Conversation::createdAt).reversed())
          .toList();
    }

    public List<Conversation> findAll() {
      return new ArrayList<>(storage.values());
    }
  }

  static class InMemoryMessageRepository implements MessageRepository {
    private final List<Message> storage = new ArrayList<>();

    @Override
    public synchronized Message save(Message message) {
      storage.add(message);
      return message;
    }

    @Override
    public synchronized List<Message> findByConversationId(UUID conversationId) {
      return storage.stream()
          .filter(m -> m.conversationId().equals(conversationId))
          .sorted(Comparator.comparing(Message::timestamp))
          .toList();
    }
  }
}
