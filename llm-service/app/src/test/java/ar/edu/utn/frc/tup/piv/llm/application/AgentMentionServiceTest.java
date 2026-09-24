package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagQueryGuardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.agent.AgentMentionService;
import ar.edu.utn.frc.tup.piv.llm.application.service.agent.QuotaRegistry;
import ar.edu.utn.frc.tup.piv.llm.application.service.agent.RagQueryService;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.AgentSelfMentionSanitizer;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationDecisionUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests unitarios de AgentMentionService (HU LLM-S18-H01 · T2 + T3).
 * Cubre: recuperación RAG aislada por cohorte, abstención BLOCKED_NO_SOURCE,
 * citas de documento/página y moderación de salida.
 */
class AgentMentionServiceTest {

  private RagQueryService ragQueryService;
  private ModelInvocationService modelInvocationService;
  private ModerationDecisionUseCase moderationDecisionUseCase;
  private QuotaRegistry quotaRegistry;
  private AgentSelfMentionSanitizer sanitizer;
  private RagQueryGuardrail ragQueryGuardrail;
  private AgentMentionService service;

  private static final CallerIdentity ACTOR =
      new CallerIdentity("chat-service", UUID.randomUUID(), "req-1", "trace-1");

  private static final String COHORT_ID = UUID.randomUUID().toString();
  private static final String STUDENT_ID = "s-001";
  private static final String THREAD_ID = "thread-abc";

  @BeforeEach
  void setUp() {
    ragQueryService = mock(RagQueryService.class);
    modelInvocationService = mock(ModelInvocationService.class);
    moderationDecisionUseCase = mock(ModerationDecisionUseCase.class);
    quotaRegistry = mock(QuotaRegistry.class);
    sanitizer = new AgentSelfMentionSanitizer();
    ragQueryGuardrail = mock(RagQueryGuardrail.class);

    // Por defecto el guardrail aprueba todo
    when(ragQueryGuardrail.validate(anyString(), anyString()))
        .thenReturn(RagQueryGuardrail.ValidationResult.ok());

    // Por defecto la moderación aprueba
    when(moderationDecisionUseCase.decide(any(ModerationDecisionCommand.class)))
        .thenReturn(ModerationDecision.allow("msg-default", "CLEAN", "mock-classifier", 10, "hash"));

    service = new AgentMentionService(
        ragQueryService, modelInvocationService, moderationDecisionUseCase,
        quotaRegistry, sanitizer, ragQueryGuardrail);
  }

  // ─────────────────────────────────────────────────────────────
  // CA1 + Escenario 1: mención con fuentes → respuesta con citas
  // ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("CA1 — Mención con respaldo en el material")
  class MentionWithSources {

    @Test
    @DisplayName("Debe retornar respuesta con al menos una cita de documento y página")
    void mentionWithSourcesReturnsAnswerWithCitations() {
      // Dado: hay chunks disponibles para la cohorte
      DocumentChunk chunk = DocumentChunk.nuevo(
          UUID.randomUUID(), "Apunte de Sistemas", 47, 1, "Un puntero almacena dirección de memoria.")
          .conSimilitud(0.85);
      when(ragQueryService.queryCohortContext(any(UUID.class), anyString(), anyInt()))
          .thenReturn(List.of(chunk));
      when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult("Un puntero es una variable que guarda la dirección.", "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any()))
          .thenReturn(ModerationDecision.allow("msg-1", "CLEAN", "mock", 5, "h1"));

      // Cuando: se procesa la mención
      Optional<AgentMentionService.Response> result = service.processMention(request("¿qué es un puntero?"), ACTOR);

      // Entonces: hay respuesta con citas
      assertThat(result).isPresent();
      AgentMentionService.Response resp = result.get();
      assertThat(resp.status()).isEqualTo(AgentMentionService.STATUS_OK);
      assertThat(resp.citations()).hasSize(1);
      assertThat(resp.citations().get(0).documentName()).isEqualTo("Apunte de Sistemas");
      assertThat(resp.citations().get(0).pageNumber()).isEqualTo(47);
      assertThat(resp.replyText()).isNotBlank();
    }

    @Test
    @DisplayName("Las citas deben incluir nombre de documento y número de página exacto del chunk")
    void citationsIncludeDocumentNameAndPage() {
      DocumentChunk chunk1 = DocumentChunk.nuevo(UUID.randomUUID(), "Manual C", 12, 0, "Texto sobre punteros").conSimilitud(0.9);
      DocumentChunk chunk2 = DocumentChunk.nuevo(UUID.randomUUID(), "Guía Práctica", 5, 0, "Ejemplo de uso").conSimilitud(0.75);
      when(ragQueryService.queryCohortContext(any(UUID.class), anyString(), anyInt()))
          .thenReturn(List.of(chunk1, chunk2));
      when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult("Respuesta combinada", "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any()))
          .thenReturn(ModerationDecision.allow("msg-2", "CLEAN", "mock", 5, "h2"));

      Optional<AgentMentionService.Response> result = service.processMention(request("¿cómo uso punteros?"), ACTOR);

      assertThat(result).isPresent();
      assertThat(result.get().citations()).hasSize(2);
      assertThat(result.get().citations()).extracting(AgentMentionService.Citation::documentName)
          .containsExactly("Manual C", "Guía Práctica");
      assertThat(result.get().citations()).extracting(AgentMentionService.Citation::pageNumber)
          .containsExactly(12, 5);
    }

    @Test
    @DisplayName("El RAG filtra estrictamente por cohortId: nunca mezcla documentos de otra cohorte")
    void ragFiltersAreStrictlyByCohort() {
      UUID specificCohort = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
      String cohortStr = specificCohort.toString();
      DocumentChunk chunk = DocumentChunk.nuevo(UUID.randomUUID(), "Doc cohorte A", 1, 0, "contenido").conSimilitud(0.8);
      when(ragQueryService.queryCohortContext(eq(specificCohort), anyString(), anyInt()))
          .thenReturn(List.of(chunk));
      when(modelInvocationService.invoke(any(), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult("Respuesta", "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any()))
          .thenReturn(ModerationDecision.allow("msg-3", "CLEAN", "mock", 5, "h3"));

      AgentMentionService.Request req = new AgentMentionService.Request(
          UUID.randomUUID().toString(), cohortStr, THREAD_ID, "student", STUDENT_ID, "pregunta");
      Optional<AgentMentionService.Response> result = service.processMention(req, ACTOR);

      assertThat(result).isPresent();
      // El RAG fue invocado exactamente con el cohortId correcto (UUID derivado de la cadena)
      verify(ragQueryService).queryCohortContext(eq(specificCohort), anyString(), anyInt());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // CA2 + Escenario 2: abstención BLOCKED_NO_SOURCE (T3)
  // ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("CA2 — Abstención explícita cuando no hay respaldo suficiente")
  class AbstentionWhenNoSources {

    @Test
    @DisplayName("Sin fuentes en el material retorna BLOCKED_NO_SOURCE sin llamar al modelo")
    void mentionWithoutSourcesReturnsAbstentionWithoutCallingModel() {
      when(ragQueryService.queryCohortContext(any(UUID.class), anyString(), anyInt()))
          .thenReturn(List.of());

      Optional<AgentMentionService.Response> result = service.processMention(request("¿cuál es la capital de Francia?"), ACTOR);

      assertThat(result).isPresent();
      assertThat(result.get().status()).isEqualTo(AgentMentionService.STATUS_BLOCKED_NO_SOURCE);
      assertThat(result.get().replyText()).isEqualTo(AgentMentionService.ABSTENTION_MESSAGE);
      assertThat(result.get().citations()).isEmpty();
      // El modelo nunca fue invocado (0 tokens consumidos)
      verify(modelInvocationService, never()).invoke(any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("La respuesta de abstención no es silencio: incluye mensaje explicativo")
    void abstentionResponseIsExplicitNotSilence() {
      when(ragQueryService.queryCohortContext(any(UUID.class), anyString(), anyInt()))
          .thenReturn(List.of());

      Optional<AgentMentionService.Response> result = service.processMention(request("tema no cubierto"), ACTOR);

      assertThat(result).isPresent();
      assertThat(result.get().replyText()).isNotBlank();
      assertThat(result.get().replyText()).containsIgnoringCase("material");
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Helper
  // ─────────────────────────────────────────────────────────────

  private AgentMentionService.Request request(String text) {
    return new AgentMentionService.Request(
        UUID.randomUUID().toString(), COHORT_ID, THREAD_ID, "student", STUDENT_ID, text);
  }
}
