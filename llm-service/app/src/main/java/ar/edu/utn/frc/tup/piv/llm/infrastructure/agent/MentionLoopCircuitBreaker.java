package ar.edu.utn.frc.tup.piv.llm.infrastructure.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disyuntor de ráfagas y prevención de bucles de menciones (LLM-S18-H02 / T3).
 * <p>
 * Regla de negocio (CA4 / Escenario BDD 3):
 * Ventana deslizante de tiempo en memoria por `thread_id`.
 * Si se reciben más de 3 menciones al agente en menos de 10 segundos en el mismo hilo,
 * se abre el circuito, se rechazan las siguientes menciones de ese hilo y se emite
 * un log estructurado con código `LOOP_SUSPECTED`. El hilo permanece pausado para el
 * agente hasta que sea reiniciado/revisado.
 */
@Component
public class MentionLoopCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(MentionLoopCircuitBreaker.class);

    public static final String CODE_LOOP_SUSPECTED = "LOOP_SUSPECTED";
    public static final int DEFAULT_MAX_MENTIONS = 3;
    public static final Duration DEFAULT_WINDOW_DURATION = Duration.ofSeconds(10);

    private final Clock clock;
    private final int maxAllowedMentions;
    private final Duration windowDuration;

    private final ConcurrentHashMap<String, ThreadWindowState> threadStates = new ConcurrentHashMap<>();

    @Autowired
    public MentionLoopCircuitBreaker(
            @Value("${llm.agent.circuit-breaker.max-mentions:3}") int maxAllowedMentions,
            @Value("${llm.agent.circuit-breaker.window-seconds:10}") long windowSeconds) {
        this(Clock.systemUTC(), maxAllowedMentions, Duration.ofSeconds(windowSeconds));
    }

    public MentionLoopCircuitBreaker() {
        this(Clock.systemUTC(), DEFAULT_MAX_MENTIONS, DEFAULT_WINDOW_DURATION);
    }

    public MentionLoopCircuitBreaker(Clock clock) {
        this(clock, DEFAULT_MAX_MENTIONS, DEFAULT_WINDOW_DURATION);
    }

    public MentionLoopCircuitBreaker(Clock clock, int maxAllowedMentions, Duration windowDuration) {
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.maxAllowedMentions = maxAllowedMentions > 0 ? maxAllowedMentions : DEFAULT_MAX_MENTIONS;
        this.windowDuration = windowDuration != null ? windowDuration : DEFAULT_WINDOW_DURATION;
    }

    /**
     * Resultado tipificado de la evaluación de una mención por el disyuntor.
     */
    public record EvaluationResult(
            boolean allowed,
            boolean circuitOpen,
            boolean loopSuspected,
            int mentionsInWindow,
            String threadId
    ) {
    }

    /**
     * Evalúa si una mención en un hilo está autorizada o debe ser rechazada por disyuntor abierto.
     *
     * @param threadId identificador del hilo de conversación
     * @return true si la mención es autorizada, false si es rechazada (circuito abierto o disparo de corte)
     */
    public boolean allowMention(String threadId) {
        return evaluateMention(threadId).allowed();
    }

    /**
     * Evalúa de forma exhaustiva la mención, retornando los detalles del estado del circuito.
     *
     * @param threadId identificador del hilo
     * @return EvaluationResult con el veredicto
     */
    public EvaluationResult evaluateMention(String threadId) {
        String key = (threadId != null && !threadId.isBlank()) ? threadId.trim() : "default";

        ThreadWindowState state = threadStates.computeIfAbsent(key, k -> new ThreadWindowState());

        synchronized (state) {
            Instant now = clock.instant();

            // 1. Si el circuito ya estaba abierto, rechaza de inmediato
            if (state.circuitOpen) {
                log.warn("[LOOP_SUSPECTED] Mención rechazada en hilo '{}': circuito abierto.", key);
                return new EvaluationResult(false, true, false, state.timestamps.size(), key);
            }

            // 2. Limpieza de marcas temporales fuera de la ventana deslizante
            Instant windowStart = now.minus(windowDuration);
            while (!state.timestamps.isEmpty() && state.timestamps.peekFirst().isBefore(windowStart)) {
                state.timestamps.pollFirst();
            }

            // 3. Evaluar umbral: si ya hay >= maxAllowedMentions dentro de la ventana,
            // esta nueva mención excede el límite (> 3 en menos de 10s)
            if (state.timestamps.size() >= maxAllowedMentions) {
                state.circuitOpen = true;
                int totalSuspected = state.timestamps.size() + 1;

                // Emisión de log estructurado con código LOOP_SUSPECTED (CA4 / Escenario 3)
                log.warn("[{}] Posible bucle detectado en hilo '{}'. {} menciones en < {}s. Circuito abierto.",
                        CODE_LOOP_SUSPECTED, key, totalSuspected, windowDuration.toSeconds());
                log.warn("{{\"event\":\"LOOP_SUSPECTED\",\"code\":\"{}\",\"thread_id\":\"{}\",\"mentions_in_window\":{},\"window_seconds\":{}}}",
                        CODE_LOOP_SUSPECTED, key, totalSuspected, windowDuration.toSeconds());

                return new EvaluationResult(false, true, true, totalSuspected, key);
            }

            // 4. Mención autorizada: se agrega la marca de tiempo a la ventana
            state.timestamps.addLast(now);
            return new EvaluationResult(true, false, false, state.timestamps.size(), key);
        }
    }

    /**
     * Consulta si el circuito está abierto para un hilo determinado.
     */
    public boolean isCircuitOpen(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return false;
        }
        ThreadWindowState state = threadStates.get(threadId.trim());
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return state.circuitOpen;
        }
    }

    /**
     * Retorna la cantidad de menciones registradas en la ventana deslizante actual.
     */
    public int getRecentMentionCount(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return 0;
        }
        ThreadWindowState state = threadStates.get(threadId.trim());
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            Instant now = clock.instant();
            Instant windowStart = now.minus(windowDuration);
            while (!state.timestamps.isEmpty() && state.timestamps.peekFirst().isBefore(windowStart)) {
                state.timestamps.pollFirst();
            }
            return state.timestamps.size();
        }
    }

    /**
     * Cierra el circuito y restablece el historial para un hilo (revisión de docente/sistema).
     */
    public void reset(String threadId) {
        if (threadId != null && !threadId.isBlank()) {
            ThreadWindowState state = threadStates.get(threadId.trim());
            if (state != null) {
                synchronized (state) {
                    state.circuitOpen = false;
                    state.timestamps.clear();
                }
            }
        }
    }

    /**
     * Restablece el estado de todos los hilos (útil para pruebas o reinicio general).
     */
    public void resetAll() {
        threadStates.clear();
    }

    /**
     * Estado interno por hilo con marcas de tiempo y bandera del disyuntor.
     */
    private static class ThreadWindowState {
        private final Deque<Instant> timestamps = new ArrayDeque<>();
        private boolean circuitOpen = false;
    }
}
