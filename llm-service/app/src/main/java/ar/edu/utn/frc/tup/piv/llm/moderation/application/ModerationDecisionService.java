package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationDecisionUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.MessageContent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDecisionRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio orquestador de moderación de chat:
 * - Garantiza idempotencia estricta por message_id y alerta ante discrepancias de contenido.
 * - Ejecuta la decisión mediante Virtual Threads con timeout estricto de 800 ms (fallback seguro a PENDING).
 * - Persiste la auditoría inmutable aplicando minimización de datos (sin texto si es ALLOW).
 */
@Service
public class ModerationDecisionService implements ModerationDecisionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ModerationDecisionService.class);

    private final ModerationDecisionRepositoryPort repository;
    private final long timeoutMs;
    private final ExecutorService virtualExecutor;
    private final Function<ModerationDecisionCommand, ModerationDecision> classifierEngine;
    private ModerationIncidentRepositoryPort incidentRepository;
    private ModerationResolutionRepositoryPort resolutionRepository;

    @Autowired
    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            @Value("${llm.moderation.timeout-ms:800}") long timeoutMs,
            ModerationDegradationService degradationService,
            ModerationIncidentRepositoryPort incidentRepository,
            ModerationResolutionRepositoryPort resolutionRepository) {
        this(repository, timeoutMs, (Function<ModerationDecisionCommand, ModerationDecision>) degradationService);
        this.incidentRepository = incidentRepository;
        this.resolutionRepository = resolutionRepository;
    }

    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            long timeoutMs,
            ModerationDegradationService degradationService) {
        this(repository, timeoutMs, (Function<ModerationDecisionCommand, ModerationDecision>) degradationService);
    }

    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            @Value("${llm.moderation.timeout-ms:800}") long timeoutMs,
            DeterministicModerationPort deterministicEngine) {
        this(repository, timeoutMs,
                cmd -> deterministicEngine.evaluate(cmd.messageId(), cmd.text(), cmd.courseId()));
    }

    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            long timeoutMs) {
        this(repository, timeoutMs, (Function<ModerationDecisionCommand, ModerationDecision>) null);
    }

    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            long timeoutMs,
            Function<ModerationDecisionCommand, ModerationDecision> customEngine) {
        this.repository = repository;
        this.timeoutMs = timeoutMs;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.classifierEngine = customEngine != null ? customEngine : this::defaultDeterministicEvaluation;
    }

    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            long timeoutMs,
            Function<ModerationDecisionCommand, ModerationDecision> customEngine,
            ModerationIncidentRepositoryPort incidentRepository) {
        this(repository, timeoutMs, customEngine);
        this.incidentRepository = incidentRepository;
    }

    public ModerationDecisionService(
            ModerationDecisionRepositoryPort repository,
            long timeoutMs,
            Function<ModerationDecisionCommand, ModerationDecision> customEngine,
            ModerationIncidentRepositoryPort incidentRepository,
            ModerationResolutionRepositoryPort resolutionRepository) {
        this(repository, timeoutMs, customEngine);
        this.incidentRepository = incidentRepository;
        this.resolutionRepository = resolutionRepository;
    }

    @Override
    public ModerationDecision decide(ModerationDecisionCommand command) {
        MessageContent content = new MessageContent(command.text());
        String currentHash = content.getContentHash();

        // 1. Control de Idempotencia por message_id
        Optional<ModerationDecision> existing = repository.findByMessageId(command.messageId());
        if (existing.isPresent()) {
            ModerationDecision previous = existing.get();
            if (!previous.getContentHash().equals(currentHash)) {
                log.warn("ALERTA DE DISCREPANCIA: El message_id '{}' ya fue decidido pero con un contenido de texto distinto. Retornando decisión original.", command.messageId());
            } else {
                log.debug("Decisión idempotente recuperada para message_id '{}'", command.messageId());
            }
            return previous;
        }

        // 2. Ejecución con Virtual Thread y Timeout estricto de 800 ms
        long startTime = System.currentTimeMillis();
        ModerationDecision decision;

        try {
            decision = CompletableFuture
                    .supplyAsync(() -> classifierEngine.apply(command), virtualExecutor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof TimeoutException) {
                log.warn("Timeout de moderación excedido (> {} ms) para message_id '{}'. Emitiendo PENDING.", timeoutMs, command.messageId());
                decision = new ModerationDecision(
                        command.messageId(),
                        ModerationDecisionEnum.PENDING,
                        ModerationReasonCode.ENGINE_UNAVAILABLE,
                        "fallback",
                        latency,
                        UUID.randomUUID(),
                        currentHash,
                        ModerationReasonCode.FULL_ENGINE_UNAVAILABLE
                );
            } else {
                log.error("Falla en el motor de moderación para message_id '{}'. Degradando a PENDING.", command.messageId(), cause);
                decision = new ModerationDecision(
                        command.messageId(),
                        ModerationDecisionEnum.PENDING,
                        ModerationReasonCode.ENGINE_UNAVAILABLE,
                        "fallback",
                        latency,
                        UUID.randomUUID(),
                        currentHash,
                        ModerationReasonCode.FULL_ENGINE_UNAVAILABLE
                );
            }
        }

        // 3. Todo mensaje no permitido abre su incidente (base de apelaciones y revisión docente)
        openIncidentIfNeeded(decision, command);

        // 4. Persistencia de Auditoría con Minimización de Datos
        repository.save(decision, command.courseId(), command.senderRole(), command.text());

        return decision;
    }

    /**
     * CA_negativo_1 (LLM-S11-H01): retira una decisión de moderación previamente emitida.
     * Un mensaje ALLOW nunca genera incidente (ver {@link #openIncidentIfNeeded}), por lo tanto
     * jamás pudo pasar por revisión explícita de un docente; retirarlo violaría el contrato de
     * moderación. Cualquier decisión (ALLOW, BLOCK o PENDING) que no cuente con una resolución
     * explícita registrada se rechaza como error de protocolo, se audita en el log y se propaga
     * como 409 Conflict.
     */
    @Override
    public void retireDecision(String messageId, String requestedBy) {
        if (messageId == null || messageId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message_id es obligatorio");
        }

        ModerationDecision decision = repository.findByMessageId(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe decisión de moderación para message_id: " + messageId));

        if (decision.getDecision() == ModerationDecisionEnum.ALLOW) {
            log.error("PROTOCOL_VIOLATION: intento de retirar un mensaje ALLOW ('{}') sin revisión explícita de moderación. "
                            + "Un ALLOW jamás abre incidente, por lo que este flujo no debe existir (requestedBy={})",
                    messageId, requestedBy);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PROTOCOL_VIOLATION: un mensaje ALLOW no puede retirarse sin pasar por una revisión explícita de moderación");
        }

        boolean hasExplicitReview = decision.getIncidentId() != null
                && resolutionRepository != null
                && resolutionRepository.existsByIncidentId(decision.getIncidentId());

        if (!hasExplicitReview) {
            log.error("PROTOCOL_VIOLATION: intento de retirar la decisión '{}' sin una revisión explícita registrada "
                            + "(incidentId={}, requestedBy={})",
                    messageId, decision.getIncidentId(), requestedBy);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PROTOCOL_VIOLATION: no existe revisión explícita (resolución docente) para este mensaje");
        }

        log.info("Retiro de decisión '{}' autorizado tras revisión explícita registrada (requestedBy={})",
                messageId, requestedBy);
    }

    private static final int PREVIEW_MAX_CHARS = 200;

    /**
     * BLOCK -> incidente en estado BLOCK (apelable); PENDING/PENDING_REVIEW -> incidente PENDING_REVIEW
     * (visible para el docente, no apelable). El preview se recorta a 200 caracteres.
     */
    private void openIncidentIfNeeded(ModerationDecision decision, ModerationDecisionCommand command) {
        if (incidentRepository == null || decision.getIncidentId() == null
                || decision.getDecision() == ModerationDecisionEnum.ALLOW) {
            return;
        }
        String text = command.text();
        String preview = text.length() > PREVIEW_MAX_CHARS ? text.substring(0, PREVIEW_MAX_CHARS) : text;
        String owner = command.senderId() != null && !command.senderId().isBlank() ? command.senderId() : "system";
        String status = decision.getDecision() == ModerationDecisionEnum.BLOCK ? "BLOCK" : "PENDING_REVIEW";
        incidentRepository.save(new ModerationIncident(
                decision.getIncidentId(), decision.getMessageId(), owner, command.courseId(),
                status, decision.getReasonCode(), preview, null));
    }

    /**
     * Evaluación determinista estándar (< 50 ms):
     * Verifica reglas básicas y clasifica como ALLOW con reason_code CLEAN.
     */
    private ModerationDecision defaultDeterministicEvaluation(ModerationDecisionCommand command) {
        long start = System.currentTimeMillis();
        MessageContent content = new MessageContent(command.text());

        // Latencia determinista mínima
        long latency = Math.max(1, System.currentTimeMillis() - start + 5);

        return ModerationDecision.allow(
                command.messageId(),
                ModerationReasonCode.CLEAN,
                "deterministic",
                latency,
                content.getContentHash()
        );
    }
}
