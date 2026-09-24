package ar.edu.utn.frc.tup.piv.llm.moderation;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationDecisionService;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationDegradationService;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDecisionRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.client.ContextualModerationClientAdapter;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CodeObfuscationDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.CompositeModerationDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.DeterministicModerationEngine;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.ProfanityDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.SpamDetector;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector.TextNormalizationPipeline;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.resilience.ModerationResilienceConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Suite de integración de resiliencia y verificación de modo degradado (LLM-S13-H01 / T5):
 * - Simula falla 500 y timeout de 2 segundos en el clasificador externo mediante WireMock.
 * - Verifica apertura automática del Circuit Breaker (umbral > 50%).
 * - Verifica que un mensaje sospechoso reciba PENDING_REVIEW y JAMÁS ALLOW (política estricta).
 * - Verifica que el detector determinista en RAM siga bloqueando spam aún con la IA caída.
 * - Ejecuta ráfaga de 50 mensajes variados confirmando tolerancia a fallos total.
 */
class ModerationResilienceIT {

    private WireMockServer wireMockServer;
    private CircuitBreaker circuitBreaker;
    private MeterRegistry meterRegistry;
    private ContextualModerationClientAdapter clientAdapter;
    private ModerationDecisionService decisionService;
    private InMemoryModerationDecisionRepository repository;

    @org.junit.jupiter.api.BeforeAll
    static void warmUp() {
        WireMockServer warmServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());
        warmServer.start();
        warmServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"results\":[]}")));
        try {
            new ContextualModerationClientAdapter("http://127.0.0.1:" + warmServer.port(), "k", "m", 2000)
                    .classify("warmup");
        } catch (Exception ignored) {}
        warmServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());
        wireMockServer.start();

        // 1. Cliente HTTP con timeout de 300 ms apuntando a WireMock
        clientAdapter = new ContextualModerationClientAdapter(
                "http://127.0.0.1:" + wireMockServer.port(),
                "test-api-key",
                "omni-moderation-latest",
                300
        );

        // 2. Configuración de Circuit Breaker con ventana deslizante y umbral > 50%
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(6)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofMillis(300))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();
        circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("resilience-it-cb", cbConfig);

        meterRegistry = new SimpleMeterRegistry();

        // 3. Detectores deterministas en RAM
        SpamDetector spamDetector = new SpamDetector();
        CodeObfuscationDetector obfuscationDetector = new CodeObfuscationDetector();
        TextNormalizationPipeline pipeline = new TextNormalizationPipeline();
        ProfanityDetector profanityDetector = new ProfanityDetector(pipeline, 0.70);
        CompositeModerationDetector composite = new CompositeModerationDetector(
                spamDetector, obfuscationDetector, profanityDetector
        );
        DeterministicModerationEngine deterministicEngine = new DeterministicModerationEngine(composite);

        // 4. Servicio de degradación
        ModerationDegradationService degradationService = new ModerationDegradationService(
                deterministicEngine,
                clientAdapter,
                circuitBreaker,
                meterRegistry
        );

        // 5. Repositorio en memoria y orquestador principal
        repository = new InMemoryModerationDecisionRepository();
        decisionService = new ModerationDecisionService(
                repository,
                800L,
                degradationService
        );
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testExternalModelFailureAndTimeoutTriggersCircuitBreakerAndEmitsPendingReviewNeverAllow() {
        // Configuramos WireMock para responder con error 500
        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Enviamos 3 mensajes sospechosos que fallan con 500
        for (int i = 1; i <= 3; i++) {
            ModerationDecisionCommand cmd = new ModerationDecisionCommand(
                    "msg-fail-500-" + i, "curso-1", "student",
                    "Texto sospechoso número " + i, Map.of("suspicious", true)
            );
            ModerationDecision decision = decisionService.decide(cmd);

            // Verificamos que recibe PENDING_REVIEW y JAMÁS ALLOW
            assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING_REVIEW);
            assertThat(decision.getDecision()).isNotEqualTo(ModerationDecisionEnum.ALLOW);
            assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.NEEDS_REVIEW);
            assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
            assertThat(decision.getDegradationReason()).isEqualTo(ModerationReasonCode.CONTEXTUAL_UNAVAILABLE);
        }

        // Configuramos WireMock para simular timeout de 2 segundos (el cliente corta a los 300 ms)
        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse()
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[]}")));

        // Enviamos 2 mensajes adicionales con timeout para superar el umbral de apertura
        for (int i = 4; i <= 5; i++) {
            ModerationDecisionCommand cmd = new ModerationDecisionCommand(
                    "msg-timeout-" + i, "curso-1", "student",
                    "Texto sospechoso demorado " + i, Map.of("suspicious", true)
            );
            ModerationDecision decision = decisionService.decide(cmd);

            assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING_REVIEW);
            assertThat(decision.getDecision()).isNotEqualTo(ModerationDecisionEnum.ALLOW);
            assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
        }

        // VERIFICACIÓN: El Circuit Breaker debe haber cambiado a estado OPEN tras superar el 50% de fallas
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Cuando el circuito está OPEN, una nueva llamada sospechosa se cortocircuita de inmediato sin latencia de red
        long start = System.currentTimeMillis();
        ModerationDecisionCommand cmdOpen = new ModerationDecisionCommand(
                "msg-circuit-open", "curso-1", "student",
                "Texto sospechoso con circuito abierto", Map.of("suspicious", true)
        );
        ModerationDecision decisionOpen = decisionService.decide(cmdOpen);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(decisionOpen.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING_REVIEW);
        assertThat(decisionOpen.getDecision()).isNotEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decisionOpen.getClassifierUsed()).isEqualTo("fallback");
        assertThat(elapsed).isLessThan(500L); // Cortocircuito inmediato en memoria (muy por debajo del delay de red de 2000 ms simulado)

        // Métrica Micrometer registrada
        assertThat(meterRegistry.get("moderation_fallback_total").counter().count()).isGreaterThanOrEqualTo(5.0);
    }

    @Test
    void testDeterministicDetectorInRamContinuesBlockingSpamWhenAiIsDown() {
        // Forzamos Circuit Breaker a estado OPEN (clasificador externo caído)
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Mensaje con spam evidente (3 URLs distintas en texto corto)
        ModerationDecisionCommand spamCmd = new ModerationDecisionCommand(
                "msg-spam-open", "curso-1", "student",
                "Urgente visita https://spam1.org y https://spam2.org o https://spam3.org ahora", Map.of()
        );

        ModerationDecision spamDecision = decisionService.decide(spamCmd);

        // El detector determinista en RAM sigue operando y BLOQUEA
        assertThat(spamDecision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(spamDecision.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
        assertThat(spamDecision.getClassifierUsed()).isEqualTo("deterministic");
    }

    @Test
    void testBurstOf50MessagesHandledGracefullyWithoutUncaughtExceptions() {
        // Simulamos IA caída
        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse().withStatus(500)));

        int allowedCount = 0;
        int blockedCount = 0;
        int pendingReviewCount = 0;

        for (int i = 1; i <= 50; i++) {
            String messageId = "burst-msg-" + i;
            ModerationDecisionCommand cmd;

            if (i % 3 == 0) {
                // Spam evidente
                cmd = new ModerationDecisionCommand(
                        messageId, "curso-42", "student",
                        "Spam repetido https://u1.com y https://u2.com y https://u3.com " + i, Map.of()
                );
            } else if (i % 3 == 1) {
                // Dudoso / sospechoso
                cmd = new ModerationDecisionCommand(
                        messageId, "curso-42", "student",
                        "Mensaje con tono dudoso y sospechoso " + i, Map.of("suspicious", true)
                );
            } else {
                // Limpio evidente
                cmd = new ModerationDecisionCommand(
                        messageId, "curso-42", "student",
                        "Consulta academica legitima sobre la materia " + i, Map.of()
                );
            }

            ModerationDecision decision = decisionService.decide(cmd);
            assertThat(decision).isNotNull();

            if (decision.getDecision() == ModerationDecisionEnum.ALLOW) {
                allowedCount++;
                // Un mensaje sospechoso JAMÁS debe ser aprobado
                assertThat(cmd.text()).doesNotContain("sospechoso");
            } else if (decision.getDecision() == ModerationDecisionEnum.BLOCK) {
                blockedCount++;
            } else if (decision.getDecision() == ModerationDecisionEnum.PENDING_REVIEW) {
                pendingReviewCount++;
                assertThat(decision.getClassifierUsed()).isEqualTo("fallback");
            }
        }

        // Verificamos que el 100% de los 50 mensajes recibió veredicto adecuado
        assertThat(allowedCount + blockedCount + pendingReviewCount).isEqualTo(50);
        assertThat(blockedCount).isGreaterThan(0);
        assertThat(pendingReviewCount).isGreaterThan(0);
        assertThat(allowedCount).isGreaterThan(0);
    }

    private static class InMemoryModerationDecisionRepository implements ModerationDecisionRepositoryPort {
        private final Map<String, ModerationDecision> storage = new ConcurrentHashMap<>();

        @Override
        public Optional<ModerationDecision> findByMessageId(String messageId) {
            return Optional.ofNullable(storage.get(messageId));
        }

        @Override
        public void save(ModerationDecision decision, String courseId, String senderRole, String messageText) {
            storage.putIfAbsent(decision.getMessageId(), decision);
        }
    }
}
