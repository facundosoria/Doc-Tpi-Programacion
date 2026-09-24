package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ContextualClassificationResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.MessageContent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ContextualModerationPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Orquestador de degradación y resiliencia de moderación (T3 de LLM-S13-H01):
 * - Ejecuta primero los detectores deterministas en RAM (< 5 ms).
 * - Si hay infracción clara (spam, ofuscación, insulto evidente), emite BLOCK inmediato.
 * - Si el caso es dudoso/sospechoso, invoca al clasificador contextual protegido por Resilience4j Circuit Breaker.
 * - Ante fallo o circuito abierto, aplica Fallback Fail-Safe:
 *   - Registra métrica Micrometer 'moderation_fallback_total'.
 *   - Reasegura con filtro determinista en RAM.
 *   - Si el mensaje dudoso no pudo ser analizado contextualmente: emite decision: PENDING_REVIEW con classifier_used: fallback.
 *   - POLÍTICA ESTRICTA: ¡JAMÁS emite ALLOW de manera automática a mensajes no verificados!
 */
@Service
public class ModerationDegradationService implements Function<ModerationDecisionCommand, ModerationDecision> {

    private static final Logger log = LoggerFactory.getLogger(ModerationDegradationService.class);

    private static final List<String> SUSPICIOUS_PATTERNS = List.of(
            "sospechoso", "dudoso", "revisar", "amenaza", "te voy a buscar",
            "odio", "peligro", "acoso", "idiota", "tarado", "inutil", "gil", "morite"
    );

    private final DeterministicModerationPort deterministicEngine;
    private final ContextualModerationPort contextualClient;
    private final CircuitBreaker circuitBreaker;
    private final Counter fallbackCounter;

    @Autowired
    public ModerationDegradationService(
            DeterministicModerationPort deterministicEngine,
            ContextualModerationPort contextualClient,
            CircuitBreaker contextualCircuitBreaker,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.deterministicEngine = deterministicEngine;
        this.contextualClient = contextualClient;
        this.circuitBreaker = contextualCircuitBreaker;

        MeterRegistry registry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.fallbackCounter = Counter.builder("moderation_fallback_total")
                .description("Total de caidas a modo degradado en moderacion ante fallas del clasificador contextual")
                .tag("reason", ModerationReasonCode.CONTEXTUAL_UNAVAILABLE)
                .register(registry);
    }

    @Override
    public ModerationDecision apply(ModerationDecisionCommand command) {
        return evaluateWithResilience(command);
    }

    public ModerationDecision evaluateWithResilience(ModerationDecisionCommand command) {
        long startTime = System.currentTimeMillis();
        MessageContent content = new MessageContent(command.text());
        String contentHash = content.getContentHash();

        // 1. Fase determinista en RAM (< 5 ms): corte temprano en infracciones evidentes
        ModerationDecision deterministicDecision = deterministicEngine.evaluate(
                command.messageId(), command.text(), command.courseId()
        );

        if (deterministicDecision.getDecision() == ModerationDecisionEnum.BLOCK) {
            log.info("Mensaje '{}' bloqueado inmediatamente por detector determinista '{}' en RAM",
                    command.messageId(), deterministicDecision.getReasonCode());
            return deterministicDecision;
        }

        // 2. Discriminación de caso dudoso / sospechoso vs mensaje evidentemente limpio
        boolean suspicious = isSuspicious(command.text(), command.contextFlags());

        if (!suspicious) {
            // Mensaje limpio y sin sospecha: emitir ALLOW determinista
            long latency = Math.max(1, System.currentTimeMillis() - startTime);
            return ModerationDecision.allow(
                    command.messageId(),
                    ModerationReasonCode.CLEAN,
                    "deterministic",
                    latency,
                    contentHash
            );
        }

        // 3. Caso dudoso: requiere clasificación contextual externa con Circuit Breaker
        log.info("Mensaje '{}' marcado como caso dudoso/sospechoso. Consultando clasificador contextual externo...",
                command.messageId());

        try {
            ContextualClassificationResult contextualResult = circuitBreaker.executeSupplier(
                    () -> contextualClient.classify(command.text())
            );

            long totalLatency = Math.max(1, System.currentTimeMillis() - startTime);

            if (contextualResult.flagged()) {
                log.info("Mensaje '{}' bloqueado por clasificador contextual (categoria='{}', score={})",
                        command.messageId(), contextualResult.category(), contextualResult.score());
                return ModerationDecision.block(
                        command.messageId(),
                        contextualResult.category() != null ? contextualResult.category() : ModerationReasonCode.CONTEXTUAL_BLOCK,
                        "contextual",
                        totalLatency,
                        contentHash,
                        UUID.randomUUID()
                );
            } else {
                log.info("Mensaje sospechoso '{}' verificado y aprobado por clasificador contextual", command.messageId());
                return ModerationDecision.allow(
                        command.messageId(),
                        ModerationReasonCode.CLEAN,
                        "contextual",
                        totalLatency,
                        contentHash
                );
            }

        } catch (Throwable t) {
            // 4. Fallback Fail-Safe: Circuit Breaker OPEN, Timeout (> 300 ms) o Falla HTTP 5xx
            return executeFailSafeFallback(command, contentHash, startTime, t);
        }
    }

    /**
     * Mecanismo de Fallback Fail-Safe ante caída o saturación del clasificador contextual.
     */
    private ModerationDecision executeFailSafeFallback(
            ModerationDecisionCommand command,
            String contentHash,
            long startTime,
            Throwable throwable) {

        long totalLatency = Math.max(1, System.currentTimeMillis() - startTime);

        // A. Registrar métrica de observabilidad Micrometer
        fallbackCounter.increment();

        boolean circuitOpen = throwable instanceof CallNotPermittedException
                || circuitBreaker.getState() == CircuitBreaker.State.OPEN;

        log.warn("DEGRADACION DE MODERACION activada para messageId='{}' (CircuitBreaker State: {}). Causa: {}",
                command.messageId(), circuitBreaker.getState(), throwable.getMessage());

        // B. Reaseguro determinista: si el detector determinista detecta spam o insulto, se BLOQUEA
        ModerationDecision deterministicRecheck = deterministicEngine.evaluate(
                command.messageId(), command.text(), command.courseId()
        );
        if (deterministicRecheck.getDecision() == ModerationDecisionEnum.BLOCK) {
            return deterministicRecheck;
        }

        // C. POLÍTICA DE SEGURIDAD ESTRICTA:
        // Ante caída del modelo externo, NUNCA se responde ALLOW de manera automática a mensajes dudosos.
        // El veredicto es decision: PENDING_REVIEW con classifier_used: fallback y degradation_reason: CONTEXTUAL_UNAVAILABLE.
        log.warn("Mensaje dudoso '{}' no pudo ser verificado por caída del modelo externo. Emitiendo PENDING_REVIEW (anti fail-open)",
                command.messageId());

        return ModerationDecision.pendingReview(
                command.messageId(),
                ModerationReasonCode.NEEDS_REVIEW,
                "fallback",
                totalLatency,
                contentHash,
                ModerationReasonCode.CONTEXTUAL_UNAVAILABLE
        );
    }

    public boolean isSuspicious(String text, Map<String, Object> contextFlags) {
        if (contextFlags != null) {
            if (Boolean.TRUE.equals(contextFlags.get("suspicious"))
                    || Boolean.TRUE.equals(contextFlags.get("needs_review"))
                    || Boolean.TRUE.equals(contextFlags.get("requires_contextual_review"))
                    || "dudoso".equalsIgnoreCase(String.valueOf(contextFlags.get("flag")))) {
                return true;
            }
        }
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public Counter getFallbackCounter() {
        return fallbackCounter;
    }
}
