package ar.edu.utn.frc.tup.piv.llm.agent;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionRequest;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionResponse;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.validation.MentionSenderValidator;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.AgentSelfMentionSanitizer;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.MentionLoopCircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LLM-S18-H02 — Suite de Protección contra Bucles y Menciones No Válidas")
class MentionLoopProtectionTest {

    private MentionSenderValidator senderValidator;
    private AgentSelfMentionSanitizer sanitizer;
    private MentionLoopCircuitBreaker circuitBreaker;
    private AgentMentionController controller;

    @BeforeEach
    void setUp() {
        senderValidator = new MentionSenderValidator();
        sanitizer = new AgentSelfMentionSanitizer();
        circuitBreaker = new MentionLoopCircuitBreaker();
        controller = new AgentMentionController(senderValidator, circuitBreaker, sanitizer);
    }

    @Nested
    @DisplayName("T1 & BDD Escenario 1: Mención de Persona Real (Camino Feliz - CA1)")
    class Scenario1HappyPathTests {

        @Test
        @DisplayName("Mención con sender_role 'student' es autorizada con normalidad")
        void testStudentRoleAuthorized() {
            assertTrue(senderValidator.isAuthorized("student"));
            assertTrue(senderValidator.isAuthorized("STUDENT"));
            assertTrue(senderValidator.isAuthorized(" student "));
            assertFalse(senderValidator.shouldDrop("student"));
            assertTrue(senderValidator.validate("student").isEmpty());

            AgentMentionRequest request = new AgentMentionRequest(
                    "msg-101", "cohort-01", "th-1", "student", "st-99", "@agente ¿qué es un puntero?"
            );
            assertTrue(senderValidator.validate(request).isEmpty());

            ResponseEntity<?> response = controller.handleMention(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(AgentMentionResponse.class, response.getBody());
        }

        @Test
        @DisplayName("Mención con sender_role 'teacher' es autorizada con normalidad")
        void testTeacherRoleAuthorized() {
            assertTrue(senderValidator.isAuthorized("teacher"));
            assertTrue(senderValidator.isAuthorized("Teacher"));
            assertTrue(senderValidator.validate("teacher").isEmpty());

            AgentMentionRequest request = new AgentMentionRequest(
                    "msg-102", "cohort-01", "th-2", "teacher", "prof-01", "@agente resumen de la clase"
            );
            ResponseEntity<?> response = controller.handleMention(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("T1 & BDD Escenario 2: Descarte Silencioso de Bots, System y Roles Desconocidos (CA2, CA5)")
    class Scenario2SilentDropTests {

        @Test
        @DisplayName("Mención con sender_role 'bot' se descarta con HTTP 204 No Content")
        void testBotRoleSilentDrop() {
            assertFalse(senderValidator.isAuthorized("bot"));
            assertTrue(senderValidator.shouldDrop("bot"));

            var result = senderValidator.validate("bot");
            assertTrue(result.isPresent());
            assertEquals(HttpStatus.NO_CONTENT, result.get().getStatusCode());

            AgentMentionRequest request = new AgentMentionRequest(
                    "msg-bot-01", "cohort-01", "th-bot", "bot", "bot-id", "@agente hola bot"
            );
            ResponseEntity<?> response = controller.handleMention(request);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Mención con sender_role 'system' se descarta silenciosamente con 204")
        void testSystemRoleSilentDrop() {
            assertFalse(senderValidator.isAuthorized("system"));
            assertTrue(senderValidator.shouldDrop("system"));

            AgentMentionRequest request = new AgentMentionRequest(
                    "msg-sys-01", "cohort-01", "th-sys", "system", "sys-01", "@agente trigger interno"
            );
            ResponseEntity<?> response = controller.handleMention(request);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Mención sin sender_role (nulo, vacío, blanco o desconocido) se descarta sin asumir ningún rol (CA5)")
        void testMissingOrUnknownRoleSilentDrop() {
            assertFalse(senderValidator.isAuthorized(null));
            assertFalse(senderValidator.isAuthorized(""));
            assertFalse(senderValidator.isAuthorized("   "));
            assertFalse(senderValidator.isAuthorized("admin"));
            assertFalse(senderValidator.isAuthorized("unknown"));

            AgentMentionRequest nullRoleRequest = new AgentMentionRequest(
                    "msg-null", "cohort-01", "th-null", null, "st-01", "@agente pregunta"
            );
            assertEquals(HttpStatus.NO_CONTENT, controller.handleMention(nullRoleRequest).getStatusCode());

            AgentMentionRequest emptyRoleRequest = new AgentMentionRequest(
                    "msg-empty", "cohort-01", "th-empty", "   ", "st-01", "@agente pregunta"
            );
            assertEquals(HttpStatus.NO_CONTENT, controller.handleMention(emptyRoleRequest).getStatusCode());

            AgentMentionRequest unknownRoleRequest = new AgentMentionRequest(
                    "msg-unk", "cohort-01", "th-unk", "moderator", "st-01", "@agente pregunta"
            );
            assertEquals(HttpStatus.NO_CONTENT, controller.handleMention(unknownRoleRequest).getStatusCode());

            // Null request
            assertTrue(senderValidator.validate((AgentMentionRequest) null).isPresent());
            assertEquals(HttpStatus.NO_CONTENT, senderValidator.validate((AgentMentionRequest) null).get().getStatusCode());
        }
    }

    @Nested
    @DisplayName("T2: Sanitización de Auto-Menciones en la Respuesta (CA3)")
    class ScenarioSelfMentionSanitizerTests {

        @Test
        @DisplayName("Si el modelo genera mención a @agente, sanitiza y adjunta suppress_reply_events: true")
        void testSanitizesAgenteAndAttachesSuppression() {
            String generatedOutput = "El usuario @agente preguntó cómo resolver el ejercicio.";
            var result = sanitizer.sanitize(generatedOutput);

            assertTrue(result.selfMentionDetected());
            assertTrue(result.suppressReplyEvents());
            assertEquals("El usuario agente preguntó cómo resolver el ejercicio.", result.sanitizedText());
            assertEquals(Boolean.TRUE, result.metadata().get("suppress_reply_events"));
        }

        @Test
        @DisplayName("Si el modelo genera mención a @agent, sanitiza y neutraliza el prefijo @")
        void testSanitizesAgentInEnglish() {
            String text = "Contact @agent for additional support.";
            var result = sanitizer.sanitize(text);

            assertTrue(result.selfMentionDetected());
            assertTrue(result.suppressReplyEvents());
            assertEquals("Contact agent for additional support.", result.sanitizedText());
        }

        @Test
        @DisplayName("Si la respuesta no contiene auto-mención, mantiene el texto y suppress_reply_events es false")
        void testLeavesCleanOutputUntouched() {
            String text = "Para ordenar un arreglo en Java podés usar Arrays.sort().";
            var result = sanitizer.sanitize(text);

            assertFalse(result.selfMentionDetected());
            assertFalse(result.suppressReplyEvents());
            assertEquals(text, result.sanitizedText());
            assertNull(result.metadata().get("suppress_reply_events"));
        }
    }

    @Nested
    @DisplayName("T3 & BDD Escenario 3: Disyuntor de Ráfagas y Detección de Bucle (CA4)")
    class Scenario3LoopCircuitBreakerTests {

        @Test
        @DisplayName("Cuarta mención en menos de 10s en el mismo hilo abre el circuito y emite LOOP_SUSPECTED")
        void testFourthMentionTripsCircuit() {
            String threadId = "thread-77";

            // 1ª mención: permitida
            assertTrue(circuitBreaker.allowMention(threadId));
            assertFalse(circuitBreaker.isCircuitOpen(threadId));

            // 2ª mención: permitida
            assertTrue(circuitBreaker.allowMention(threadId));
            assertFalse(circuitBreaker.isCircuitOpen(threadId));

            // 3ª mención: permitida
            assertTrue(circuitBreaker.allowMention(threadId));
            assertFalse(circuitBreaker.isCircuitOpen(threadId));

            // 4ª mención en < 10s: CORTA el circuito
            MentionLoopCircuitBreaker.EvaluationResult fourthResult = circuitBreaker.evaluateMention(threadId);
            assertFalse(fourthResult.allowed());
            assertTrue(fourthResult.circuitOpen());
            assertTrue(fourthResult.loopSuspected());
            assertEquals(MentionLoopCircuitBreaker.CODE_LOOP_SUSPECTED, "LOOP_SUSPECTED");

            // 5ª mención: sigue rechazada porque el circuito está abierto
            assertFalse(circuitBreaker.allowMention(threadId));
            assertTrue(circuitBreaker.isCircuitOpen(threadId));

            // Tras reinicio explícito del hilo (docente/sistema), vuelve a permitir
            circuitBreaker.reset(threadId);
            assertFalse(circuitBreaker.isCircuitOpen(threadId));
            assertTrue(circuitBreaker.allowMention(threadId));
        }

        @Test
        @DisplayName("Controlador responde HTTP 429 con LOOP_SUSPECTED al dispararse el disyuntor")
        void testControllerRejectionOnLoopSuspected() {
            String threadId = "thread-burst";

            for (int i = 1; i <= 3; i++) {
                AgentMentionRequest req = new AgentMentionRequest(
                        "msg-" + i, "c-1", threadId, "student", "st-1", "@agente hola " + i
                );
                ResponseEntity<?> res = controller.handleMention(req);
                assertEquals(HttpStatus.OK, res.getStatusCode());
            }

            // 4ª petición: disyuntor salta
            AgentMentionRequest fourthReq = new AgentMentionRequest(
                    "msg-4", "c-1", threadId, "student", "st-1", "@agente hola 4"
            );
            ResponseEntity<?> fourthRes = controller.handleMention(fourthReq);
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, fourthRes.getStatusCode());

            assertInstanceOf(ProblemDetail.class, fourthRes.getBody());
            ProblemDetail detail = (ProblemDetail) fourthRes.getBody();
            assertEquals("LOOP_SUSPECTED", detail.getProperties().get("code"));
            assertEquals(threadId, detail.getProperties().get("thread_id"));
        }

        @Test
        @DisplayName("Ventana deslizante con Reloj Virtual expira menciones tras 10 segundos")
        void testSlidingWindowWithVirtualClock() {
            TestClock virtualClock = new TestClock(Instant.parse("2026-09-18T10:00:00Z"));
            MentionLoopCircuitBreaker cb = new MentionLoopCircuitBreaker(
                    virtualClock, 3, Duration.ofSeconds(10)
            );

            String threadId = "thread-virtual-time";

            // Mención 1 en t=0s
            assertTrue(cb.allowMention(threadId));
            assertEquals(1, cb.getRecentMentionCount(threadId));

            // Mención 2 en t=3s
            virtualClock.advance(Duration.ofSeconds(3));
            assertTrue(cb.allowMention(threadId));
            assertEquals(2, cb.getRecentMentionCount(threadId));

            // El reloj avanza 11 segundos: las menciones de t=0s y t=3s ya salieron de la ventana (10s)
            virtualClock.advance(Duration.ofSeconds(11));
            assertEquals(0, cb.getRecentMentionCount(threadId));

            // Mención 3, 4 y 5 en la nueva ventana
            assertTrue(cb.allowMention(threadId)); // 1ª en nueva ventana
            assertTrue(cb.allowMention(threadId)); // 2ª en nueva ventana
            assertTrue(cb.allowMention(threadId)); // 3ª en nueva ventana
            assertEquals(3, cb.getRecentMentionCount(threadId));

            // 4ª mención en la nueva ventana: ahora sí debe disparar el corte
            assertFalse(cb.allowMention(threadId));
            assertTrue(cb.isCircuitOpen(threadId));
        }

        @Test
        @DisplayName("Hilos distintos son independientes y no interfieren entre sí")
        void testDifferentThreadsAreIndependent() {
            String threadA = "thread-A";
            String threadB = "thread-B";

            // Thread A recibe 3 menciones
            assertTrue(circuitBreaker.allowMention(threadA));
            assertTrue(circuitBreaker.allowMention(threadA));
            assertTrue(circuitBreaker.allowMention(threadA));

            // Thread B recibe 2 menciones
            assertTrue(circuitBreaker.allowMention(threadB));
            assertTrue(circuitBreaker.allowMention(threadB));

            // Thread A recibe la 4ta -> se corta
            assertFalse(circuitBreaker.allowMention(threadA));
            assertTrue(circuitBreaker.isCircuitOpen(threadA));

            // Thread B aún no se cortó y permite su 3ra mención
            assertFalse(circuitBreaker.isCircuitOpen(threadB));
            assertTrue(circuitBreaker.allowMention(threadB));

            // Thread B recibe la 4ta -> se corta
            assertFalse(circuitBreaker.allowMention(threadB));
            assertTrue(circuitBreaker.isCircuitOpen(threadB));

            // resetAll limpia todo
            circuitBreaker.resetAll();
            assertFalse(circuitBreaker.isCircuitOpen(threadA));
            assertFalse(circuitBreaker.isCircuitOpen(threadB));
        }
    }

    @Nested
    @DisplayName("Pruebas de Concurrencia y Resiliencia Multihilo")
    class ConcurrencyTests {

        @Test
        @DisplayName("Ráfaga concurrente de 30 solicitudes en el mismo hilo permite exactamente 3 y rechaza 27")
        void testConcurrentBurstOnSameThread() throws InterruptedException {
            String threadId = "concurrent-burst-thread";
            int totalThreads = 30;
            ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(totalThreads);

            AtomicInteger allowedCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);

            for (int i = 0; i < totalThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (circuitBreaker.allowMention(threadId)) {
                            allowedCount.incrementAndGet();
                        } else {
                            rejectedCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Disparo sincronizado de todos los hilos simultáneamente
            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertEquals(3, allowedCount.get(), "Exactamente 3 menciones deben ser autorizadas");
            assertEquals(27, rejectedCount.get(), "El resto de menciones deben ser rechazadas");
            assertTrue(circuitBreaker.isCircuitOpen(threadId));
        }

        @Test
        @DisplayName("Ráfaga concurrente en 10 hilos diferentes procesa 3 menciones por hilo sin interferencia")
        void testConcurrentMultiThreadIsolation() throws InterruptedException {
            int numberOfDistinctThreads = 10;
            int mentionsPerThread = 3;
            int totalRequests = numberOfDistinctThreads * mentionsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(16);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(totalRequests);

            List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

            for (int t = 0; t < numberOfDistinctThreads; t++) {
                final String threadId = "thread-iso-" + t;
                for (int m = 0; m < mentionsPerThread; m++) {
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            results.add(circuitBreaker.allowMention(threadId));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertEquals(totalRequests, results.size());
            long accepted = results.stream().filter(Boolean::booleanValue).count();
            assertEquals(totalRequests, accepted, "Las 30 menciones (3 por cada uno de los 10 hilos) deben ser autorizadas");

            for (int t = 0; t < numberOfDistinctThreads; t++) {
                assertFalse(circuitBreaker.isCircuitOpen("thread-iso-" + t));
            }
        }
    }

    /**
     * Reloj virtual mutable para control determinista del tiempo en pruebas de resiliencia.
     */
    private static class TestClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone = ZoneId.of("UTC");

        TestClock(Instant start) {
            this.currentInstant = start;
        }

        void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
