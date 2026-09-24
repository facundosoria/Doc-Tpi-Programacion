package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagQueryGuardrail;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * Tests del interceptor de moderación de salida de AgentMentionService (HU LLM-S18-H01 · T4).
 * Verifica que la respuesta del agente pase siempre por ModerationDecisionUseCase antes de publicarse.
 */
class AgentModerationInterceptorTest {

  private RagQueryService ragQueryService;
  private ModelInvocationService modelInvocationService;
  private ModerationDecisionUseCase moderationDecisionUseCase;
  private QuotaRegistry quotaRegistry;
  private AgentSelfMentionSanitizer sanitizer;
  private RagQueryGuardrail ragQueryGuardrail;
  private AgentMentionService service;

  private static final CallerIdentity ACTOR =
      new CallerIdentity("chat-service", UUID.randomUUID(), "req-mod", "trace-mod");
  private static final String COHORT_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    ragQueryService = mock(RagQueryService.class);
    modelInvocationService = mock(ModelInvocationService.class);
    moderationDecisionUseCase = mock(ModerationDecisionUseCase.class);
    quotaRegistry = mock(QuotaRegistry.class);
    sanitizer = new AgentSelfMentionSanitizer();
    ragQueryGuardrail = mock(RagQueryGuardrail.class);

    when(ragQueryGuardrail.validate(anyString(), anyString()))
        .thenReturn(RagQueryGuardrail.ValidationResult.ok());

    // Por defecto: hay chunks disponibles
    DocumentChunk chunk = DocumentChunk.nuevo(UUID.randomUUID(), "Apunte", 10, 0, "Contenido académico").conSimilitud(0.8);
    when(ragQueryService.queryCohortContext(any(UUID.class), anyString(), anyInt()))
        .thenReturn(List.of(chunk));

    service = new AgentMentionService(
        ragQueryService, modelInvocationService, moderationDecisionUseCase,
        quotaRegistry, sanitizer, ragQueryGuardrail);
  }

  // ─────────────────────────────────────────────────────────────
  // CA3 + Escenario 3: moderación de salida antes de publicar
  // ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("CA3 — La respuesta del agente pasa siempre por moderación")
  class ModerationInterceptor {

    @Test
    @DisplayName("Respuesta permitida por moderación retorna status OK con texto generado")
    void allowedModerationYieldsOkResponse() {
      String generatedText = "Los punteros almacenan direcciones de memoria.";
      when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult(generatedText, "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any(ModerationDecisionCommand.class)))
          .thenReturn(ModerationDecision.allow("msg-allow", "CLEAN", "classifier", 5, "hash"));

      Optional<AgentMentionService.Response> result = service.processMention(request("¿qué es un puntero?"), ACTOR);

      assertThat(result).isPresent();
      assertThat(result.get().status()).isEqualTo(AgentMentionService.STATUS_OK);
      // La moderación fue consultada exactamente una vez
      verify(moderationDecisionUseCase).decide(any(ModerationDecisionCommand.class));
    }

    @Test
    @DisplayName("Respuesta bloqueada por moderación retorna BLOCKED y nunca el texto original")
    void blockedModerationYieldsBlockedStatusWithSafeMessage() {
      String sensitiveText = "Esta es una solución completa inapropiada.";
      when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult(sensitiveText, "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any(ModerationDecisionCommand.class)))
          .thenReturn(ModerationDecision.block("msg-block", "POLICY_VIOLATION",
              "classifier", 7, "hash", UUID.randomUUID()));

      Optional<AgentMentionService.Response> result = service.processMention(request("pregunta cualquiera"), ACTOR);

      assertThat(result).isPresent();
      assertThat(result.get().status()).isEqualTo(AgentMentionService.STATUS_BLOCKED);
      assertThat(result.get().replyText()).isEqualTo(AgentMentionService.MODERATION_BLOCKED_MESSAGE);
      // El texto original no llega al alumno (Escenario 3)
      assertThat(result.get().replyText()).doesNotContain(sensitiveText);
    }

    @Test
    @DisplayName("El texto original generado nunca es visible cuando moderación dice BLOCK (Escenario 3)")
    void blockedResponseTextIsNeverTheOriginalGeneratedText() {
      String originalReply = "Texto generado con contenido bloqueado XYZ123";
      when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult(originalReply, "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any()))
          .thenReturn(ModerationDecision.block("msg-b2", "HARMFUL", "classifier", 3, "h", UUID.randomUUID()));

      Optional<AgentMentionService.Response> result = service.processMention(request("pregunta test"), ACTOR);

      assertThat(result).isPresent();
      // El contenido original NUNCA aparece en la respuesta visible
      assertThat(result.get().replyText()).doesNotContain("XYZ123");
      assertThat(result.get().citations()).isEmpty();
    }

    @Test
    @DisplayName("La moderación siempre se invoca incluso cuando la respuesta es limpia")
    void moderationIsAlwaysInvokedForEveryAgentReply() {
      when(modelInvocationService.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any()))
          .thenReturn(new ModelInvocationResult("Respuesta académica válida.", "tok", "model-v1"));
      when(moderationDecisionUseCase.decide(any()))
          .thenReturn(ModerationDecision.allow("msg-ok", "CLEAN", "mock", 5, "h"));

      service.processMention(request("pregunta válida"), ACTOR);

      // La moderación SIEMPRE se llama, no es opcional
      verify(moderationDecisionUseCase).decide(any(ModerationDecisionCommand.class));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Helper
  // ─────────────────────────────────────────────────────────────

  private AgentMentionService.Request request(String text) {
    return new AgentMentionService.Request(
        UUID.randomUUID().toString(), COHORT_ID, "thread-mod", "student", "s-001", text);
  }
}
