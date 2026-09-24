package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.DeterministicModerationEngine;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.MessageContent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDecisionRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationDecisionServiceTest {

    private ModerationDecisionRepositoryPort repository;
    private ModerationDecisionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ModerationDecisionRepositoryPort.class);
        service = new ModerationDecisionService(repository, 800L);
    }

    @Test
    void happyPathDeterministicAllowNeverStoresTextInAudit() {
        when(repository.findByMessageId("msg-1")).thenReturn(Optional.empty());

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-1", "curso-10", "student", "Hola profesor, tengo una duda.", Map.of()
        );

        ModerationDecision decision = service.decide(command);

        assertThat(decision.getMessageId()).isEqualTo("msg-1");
        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getLatencyMs()).isLessThan(800L);
        assertThat(decision.getIncidentId()).isNull();

        verify(repository).save(eq(decision), eq("curso-10"), eq("student"), eq("Hola profesor, tengo una duda."));
    }

    @Test
    void idempotencyReplaysPreviousDecisionWithoutReevaluatingOrSaving() {
        ModerationDecision previous = ModerationDecision.allow(
                "msg-2", ModerationReasonCode.CLEAN, "deterministic", 12L,
                new MessageContent("Mensaje repetido").getContentHash()
        );
        when(repository.findByMessageId("msg-2")).thenReturn(Optional.of(previous));

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-2", "curso-10", "student", "Mensaje repetido", Map.of()
        );

        ModerationDecision decision = service.decide(command);

        assertThat(decision).isSameAs(previous);
        verify(repository, never()).save(any(), any(), any(), any());
    }

    @Test
    void discrepancyWithDifferentTextReturnsOriginalDecisionAndDoesNotResave() {
        ModerationDecision previous = ModerationDecision.block(
                "msg-3", ModerationReasonCode.SPAM, "contextual", 50L,
                new MessageContent("Texto original").getContentHash(), UUID.randomUUID()
        );
        when(repository.findByMessageId("msg-3")).thenReturn(Optional.of(previous));

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-3", "curso-10", "student", "Texto completamente modificado", Map.of()
        );

        ModerationDecision decision = service.decide(command);

        assertThat(decision).isSameAs(previous);
        verify(repository, never()).save(any(), any(), any(), any());
    }

    @Test
    void timeoutExceedingThresholdReturnsPendingAndGeneratesIncidentId() {
        // Configuramos timeout corto de 50 ms para el test
        ModerationDecisionService timeoutService = new ModerationDecisionService(
                repository,
                50L,
                cmd -> {
                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ModerationDecision.allow(cmd.messageId(), "CLEAN", "deterministic", 200L, "hash");
                }
        );

        when(repository.findByMessageId("msg-slow")).thenReturn(Optional.empty());

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-slow", "curso-10", "student", "Mensaje con latencia alta", Map.of()
        );

        ModerationDecision decision = timeoutService.decide(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.ENGINE_UNAVAILABLE);
        assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
        assertThat(decision.getIncidentId()).isNotNull();

        verify(repository).save(eq(decision), eq("curso-10"), eq("student"), eq("Mensaje con latencia alta"));
    }

    @Test
    void engineFailureDegradesSafelyToPending() {
        ModerationDecisionService failingService = new ModerationDecisionService(
                repository,
                500L,
                cmd -> {
                    throw new RuntimeException("Clasificador no disponible / Connection refused");
                }
        );

        when(repository.findByMessageId("msg-err")).thenReturn(Optional.empty());

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-err", "curso-10", "student", "Mensaje con error de motor", Map.of()
        );

        ModerationDecision decision = failingService.decide(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.ENGINE_UNAVAILABLE);
        assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
        assertThat(decision.getIncidentId()).isNotNull();

        verify(repository).save(eq(decision), eq("curso-10"), eq("student"), eq("Mensaje con error de motor"));
    }

    @Test
    void integrationWithDeterministicEngineBlocksSpamAndPersistsAudit() {
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector spamDetector =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector();
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector obfuscationDetector =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector();
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline pipeline =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline();
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector profanityDetector =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector(pipeline, 0.70);

        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector composite =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector(
                        spamDetector, obfuscationDetector, profanityDetector);
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.DeterministicModerationEngine engine =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.DeterministicModerationEngine(composite);

        ModerationDecisionService realEngineService = new ModerationDecisionService(repository, 800L, engine);

        when(repository.findByMessageId("msg-spam-int")).thenReturn(Optional.empty());

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-spam-int", "curso-10", "student",
                "Entra ya a https://link1.com o https://link2.com y tambien https://link3.com", Map.of()
        );

        ModerationDecision decision = realEngineService.decide(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getIncidentId()).isNotNull();

        verify(repository).save(eq(decision), eq("curso-10"), eq("student"),
                eq("Entra ya a https://link1.com o https://link2.com y tambien https://link3.com"));
    }

    @Test
    void integrationWithDeterministicEngineAllowsCleanMessageAndMinimizesAudit() {
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector spamDetector =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector();
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector obfuscationDetector =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector();
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline pipeline =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline();
        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector profanityDetector =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector(pipeline, 0.70);

        ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector composite =
                new ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector(
                        spamDetector, obfuscationDetector, profanityDetector);

        ModerationDecisionService realEngineService = new ModerationDecisionService(repository, 800L, composite);

        when(repository.findByMessageId("msg-clean-int")).thenReturn(Optional.empty());

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-clean-int", "curso-10", "student", "Hola profe, una consulta sobre la clase.", Map.of()
        );

        ModerationDecision decision = realEngineService.decide(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getIncidentId()).isNull();

        verify(repository).save(eq(decision), eq("curso-10"), eq("student"), eq("Hola profe, una consulta sobre la clase."));
    }

    // --- Incidentes: todo mensaje no permitido queda registrado con su emisor (base de apelaciones) ---

    private ModerationDecisionService serviceWithIncidents(
            ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort incidents,
            ModerationDecisionEnum outcome) {
        return new ModerationDecisionService(repository, 800L, cmd -> {
            if (outcome == ModerationDecisionEnum.BLOCK) {
                return ModerationDecision.block(cmd.messageId(), "SPAM", "deterministic", 1L, "h", UUID.randomUUID());
            }
            return ModerationDecision.allow(cmd.messageId(), ModerationReasonCode.CLEAN, "deterministic", 1L, "h");
        }, incidents);
    }

    @Test
    void blockOpensIncidentOwnedBySenderWithTruncatedPreview() {
        var incidents = mock(ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort.class);
        when(repository.findByMessageId("m-blk")).thenReturn(Optional.empty());
        String longText = "x".repeat(500);

        ModerationDecision decision = serviceWithIncidents(incidents, ModerationDecisionEnum.BLOCK).decide(
                new ModerationDecisionCommand("m-blk", "curso-1", "student", longText, Map.of(), "alumno-7"));

        ArgumentCaptor<ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident> captor =
                ArgumentCaptor.forClass(ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident.class);
        verify(incidents).save(captor.capture());
        var incident = captor.getValue();
        assertThat(incident.getId()).isEqualTo(decision.getIncidentId());
        assertThat(incident.getUserId()).isEqualTo("alumno-7");
        assertThat(incident.getCourseId()).isEqualTo("curso-1");
        assertThat(incident.isBlock()).isTrue();
        assertThat(incident.getMessagePreview()).hasSize(200);
    }

    @Test
    void allowDoesNotOpenIncident() {
        var incidents = mock(ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort.class);
        when(repository.findByMessageId("m-ok")).thenReturn(Optional.empty());

        serviceWithIncidents(incidents, ModerationDecisionEnum.ALLOW).decide(
                new ModerationDecisionCommand("m-ok", "curso-1", "student", "hola", Map.of(), "alumno-7"));

        verify(incidents, never()).save(any());
    }

    @Test
    void idempotentRetryDoesNotDuplicateIncident() {
        var incidents = mock(ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort.class);
        ModerationDecision previous = ModerationDecision.block("m-dup", "SPAM", "deterministic", 1L,
                new MessageContent("spam").getContentHash(), UUID.randomUUID());
        when(repository.findByMessageId("m-dup")).thenReturn(Optional.of(previous));

        serviceWithIncidents(incidents, ModerationDecisionEnum.BLOCK).decide(
                new ModerationDecisionCommand("m-dup", "curso-1", "student", "spam", Map.of(), "alumno-7"));

        verify(incidents, never()).save(any());
    }

    // --- CA_negativo_1 (LLM-S11-H01): retiro de decisiones sin revisión explícita ---

    @Test
    void retiringAllowDecisionIsRejectedAsProtocolViolation() {
        ModerationDecision allow = ModerationDecision.allow(
                "m-allow", ModerationReasonCode.CLEAN, "deterministic", 1L, "hash-allow");
        when(repository.findByMessageId("m-allow")).thenReturn(Optional.of(allow));

        assertThatThrownBy(() -> service.retireDecision("m-allow", "docente-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PROTOCOL_VIOLATION");
    }

    @Test
    void retiringBlockDecisionWithoutExplicitReviewIsRejectedAsProtocolViolation() {
        UUID incidentId = UUID.randomUUID();
        ModerationDecision block = ModerationDecision.block(
                "m-block", "SPAM", "deterministic", 1L, "hash-block", incidentId);
        when(repository.findByMessageId("m-block")).thenReturn(Optional.of(block));

        ModerationResolutionRepositoryPort resolutions = mock(ModerationResolutionRepositoryPort.class);
        when(resolutions.existsByIncidentId(incidentId)).thenReturn(false);
        ModerationDecisionService serviceWithResolutions = new ModerationDecisionService(
                repository, 800L, (Function<ModerationDecisionCommand, ModerationDecision>) null,
                mock(ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort.class),
                resolutions);

        assertThatThrownBy(() -> serviceWithResolutions.retireDecision("m-block", "docente-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PROTOCOL_VIOLATION");
    }

    @Test
    void retiringBlockDecisionWithExplicitReviewIsAllowed() {
        UUID incidentId = UUID.randomUUID();
        ModerationDecision block = ModerationDecision.block(
                "m-block-ok", "SPAM", "deterministic", 1L, "hash-block-ok", incidentId);
        when(repository.findByMessageId("m-block-ok")).thenReturn(Optional.of(block));

        ModerationResolutionRepositoryPort resolutions = mock(ModerationResolutionRepositoryPort.class);
        when(resolutions.existsByIncidentId(incidentId)).thenReturn(true);
        ModerationDecisionService serviceWithResolutions = new ModerationDecisionService(
                repository, 800L, (Function<ModerationDecisionCommand, ModerationDecision>) null,
                mock(ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort.class),
                resolutions);

        org.assertj.core.api.Assertions.assertThatCode(
                        () -> serviceWithResolutions.retireDecision("m-block-ok", "docente-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void retiringUnknownMessageIdReturnsNotFound() {
        when(repository.findByMessageId("m-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retireDecision("m-missing", "docente-1"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
