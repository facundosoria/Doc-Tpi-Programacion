package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ContextualClassificationResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ContextualModerationPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.DeterministicModerationEngine;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationDegradationServiceTest {

    private DeterministicModerationEngine deterministicEngine;
    private ContextualModerationPort contextualClient;
    private CircuitBreaker circuitBreaker;
    private MeterRegistry meterRegistry;
    private ModerationDegradationService degradationService;

    @BeforeEach
    void setUp() {
        SpamDetector spamDetector = new SpamDetector();
        CodeObfuscationDetector obfuscationDetector = new CodeObfuscationDetector();
        TextNormalizationPipeline pipeline = new TextNormalizationPipeline();
        ProfanityDetector profanityDetector = new ProfanityDetector(pipeline, 0.70);

        CompositeModerationDetector composite = new CompositeModerationDetector(
                spamDetector, obfuscationDetector, profanityDetector
        );
        deterministicEngine = new DeterministicModerationEngine(composite);

        contextualClient = mock(ContextualModerationPort.class);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();
        circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("test-cb", config);

        meterRegistry = new SimpleMeterRegistry();

        degradationService = new ModerationDegradationService(
                deterministicEngine,
                contextualClient,
                circuitBreaker,
                meterRegistry
        );
    }

    @Test
    void cleanMessageBypassesContextualModelAndIsAllowedDeterministically() {
        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-clean-1", "curso-10", "student", "Hola profe, buenas tardes.", Map.of()
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getIncidentId()).isNull();

        verify(contextualClient, never()).classify(anyString());
    }

    @Test
    void spamMessageIsBlockedDeterministicallyInRamWithoutInvokingContextualModel() {
        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-spam-1", "curso-10", "student",
                "Entra a https://link1.com y https://link2.com o https://link3.com ya", Map.of()
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getIncidentId()).isNotNull();

        verify(contextualClient, never()).classify(anyString());
    }

    @Test
    void suspiciousMessageInvokesContextualModelAndAllowsIfClean() {
        when(contextualClient.classify(anyString()))
                .thenReturn(ContextualClassificationResult.allow(45L));

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-susp-ok", "curso-10", "student",
                "Este mensaje tiene un contenido dudoso que necesita revision contextual", Map.of()
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getClassifierUsed()).isEqualTo("contextual");

        verify(contextualClient, times(1)).classify(anyString());
    }

    @Test
    void suspiciousMessageInvokesContextualModelAndBlocksIfFlagged() {
        when(contextualClient.classify(anyString()))
                .thenReturn(ContextualClassificationResult.block("HARASSMENT", 0.92, 60L));

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-susp-block", "curso-10", "student",
                "Un texto sospechoso con tono amenazante", Map.of()
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo("HARASSMENT");
        assertThat(decision.getClassifierUsed()).isEqualTo("contextual");
        assertThat(decision.getIncidentId()).isNotNull();

        verify(contextualClient, times(1)).classify(anyString());
    }

    @Test
    void suspiciousMessageWhenContextualModelFailsDegradesSafelyToPendingReviewAndNeverAllows() {
        // Simulamos falla del clasificador externo (500 / timeout)
        when(contextualClient.classify(anyString()))
                .thenThrow(new RuntimeException("External model 500 error / Connection timed out"));

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-susp-fail", "curso-10", "student",
                "Mensaje sospechoso que no puede verificarse por caida del modelo", Map.of()
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        // POLÍTICA DE SEGURIDAD ESTRICTA:
        // Ante caída del modelo externo, NUNCA se responde ALLOW a mensajes dudosos.
        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING_REVIEW);
        assertThat(decision.getDecision()).isNotEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.NEEDS_REVIEW);
        assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
        assertThat(decision.getDegradationReason()).isEqualTo(ModerationReasonCode.CONTEXTUAL_UNAVAILABLE);
        assertThat(decision.getIncidentId()).isNotNull();

        // Métrica Micrometer registrada
        double fallbacks = meterRegistry.get("moderation_fallback_total").counter().count();
        assertThat(fallbacks).isEqualTo(1.0);
    }

    @Test
    void openCircuitBreakerShortCircuitsSuspiciousMessageToPendingReviewWithoutCallingModel() {
        // Forzamos apertura del Circuit Breaker
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-cb-open", "curso-10", "student",
                "Texto sospechoso mientras el circuito esta abierto", Map.of("suspicious", true)
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING_REVIEW);
        assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
        assertThat(decision.getDegradationReason()).isEqualTo(ModerationReasonCode.CONTEXTUAL_UNAVAILABLE);

        // Jamás se intentó invocar el cliente externo porque el circuito está ABIERTO
        verify(contextualClient, never()).classify(anyString());

        double fallbacks = meterRegistry.get("moderation_fallback_total").counter().count();
        assertThat(fallbacks).isGreaterThan(0.0);
    }

    @Test
    void openCircuitBreakerStillBlocksSpamDeterministicallyInRam() {
        // Con Circuit Breaker abierto, los detectores deterministas siguen protegiendo
        circuitBreaker.transitionToOpenState();

        ModerationDecisionCommand command = new ModerationDecisionCommand(
                "msg-cb-spam", "curso-10", "student",
                "Spam https://a.com y https://b.com y https://c.com en ram", Map.of()
        );

        ModerationDecision decision = degradationService.evaluateWithResilience(command);

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");

        verify(contextualClient, never()).classify(anyString());
    }
}
