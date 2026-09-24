package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationDecisionJdbcRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationIncidentEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationRetentionPolicyEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationAppealRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationIncidentRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationResolutionRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationRetentionPolicyRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación para la purga programada e irreversible de evidencia de moderación (LLM-S13-H02 / T2).
 * Aplica el principio de minimización de datos (GDPR Art. 5.1.c y Art. 17).
 * Destruye de forma irreversible a NULL el contenido textual personal retenido, conservando las métricas agregadas.
 * Garantiza que ningún logger contenga texto sensible o evidencia purgada (Sanitización de logs).
 */
@Service
public class ModerationPurgeService {

    private static final Logger log = LoggerFactory.getLogger(ModerationPurgeService.class);

    private final SpringDataModerationIncidentRepository incidentRepository;
    private final SpringDataModerationAppealRepository appealRepository;
    private final SpringDataModerationResolutionRepository resolutionRepository;
    private final ModerationDecisionJdbcRepository decisionJdbcRepository;
    private final SpringDataModerationRetentionPolicyRepository retentionPolicyRepository;

    @Value("${moderation.retention.days:${llm.moderation.retention.days:30}}")
    private int defaultRetentionDays;

    @Value("${moderation.retention.reversed-days:${llm.moderation.retention.reversed-days:90}}")
    private int defaultReversedDays;

    @Value("${moderation.retention.unresolved-timeout-days:${llm.moderation.retention.unresolved-timeout-days:90}}")
    private int defaultUnresolvedTimeoutDays;

    public ModerationPurgeService(
            SpringDataModerationIncidentRepository incidentRepository,
            SpringDataModerationAppealRepository appealRepository,
            SpringDataModerationResolutionRepository resolutionRepository,
            ModerationDecisionJdbcRepository decisionJdbcRepository,
            SpringDataModerationRetentionPolicyRepository retentionPolicyRepository) {
        this.incidentRepository = incidentRepository;
        this.appealRepository = appealRepository;
        this.resolutionRepository = resolutionRepository;
        this.decisionJdbcRepository = decisionJdbcRepository;
        this.retentionPolicyRepository = retentionPolicyRepository;
    }

    /**
     * Ejecuta el proceso de purga destructiva de evidencia vencida según la política configurada.
     */
    @Transactional
    public PurgeExecutionSummary purgeExpiredEvidence() {
        return purgeExpiredEvidence(OffsetDateTime.now());
    }

    /**
     * Versión determinista para testing y ejecuciones con reloj de referencia.
     */
    @Transactional
    public PurgeExecutionSummary purgeExpiredEvidence(OffsetDateTime referenceTime) {
        long startTime = System.currentTimeMillis();
        OffsetDateTime now = referenceTime != null ? referenceTime : OffsetDateTime.now();

        int blockConfirmedDays = resolveRetentionDays("BLOCK_CONFIRMED", defaultRetentionDays);
        int blockReversedDays = resolveRetentionDays("BLOCK_REVERSED", defaultReversedDays);
        int pendingTimeoutDays = resolveRetentionDays("PENDING_REVIEW", defaultUnresolvedTimeoutDays);

        OffsetDateTime blockCutoff = now.minusDays(blockConfirmedDays);
        OffsetDateTime reversedCutoff = now.minusDays(blockReversedDays);
        OffsetDateTime pendingCutoff = now.minusDays(pendingTimeoutDays);
        OffsetDateTime defaultCutoff = now.minusDays(defaultRetentionDays);

        Set<UUID> candidateIds = new HashSet<>();

        // 1. Incidentes CONFIRMED o BLOCK vencidos según política de BLOCK
        List<ModerationIncidentEntity> confirmedIncidents = incidentRepository.findUnpurgedBefore(blockCutoff, "CONFIRMED");
        for (ModerationIncidentEntity i : confirmedIncidents) {
            candidateIds.add(i.getId());
        }
        List<ModerationIncidentEntity> blockIncidents = incidentRepository.findUnpurgedBefore(blockCutoff, "BLOCK");
        for (ModerationIncidentEntity i : blockIncidents) {
            candidateIds.add(i.getId());
        }

        // 2. Incidentes REVERSED (falsos positivos) con política de retención extendida para calibración
        List<ModerationIncidentEntity> reversedIncidents = incidentRepository.findUnpurgedBefore(reversedCutoff, "REVERSED");
        for (ModerationIncidentEntity i : reversedIncidents) {
            candidateIds.add(i.getId());
        }

        // 3. Incidentes PENDING_REVIEW que hayan superado el timeout de expiración sin resolución
        List<ModerationIncidentEntity> timedOutPending = incidentRepository.findUnpurgedBefore(pendingCutoff, "PENDING_REVIEW");
        for (ModerationIncidentEntity i : timedOutPending) {
            candidateIds.add(i.getId());
        }

        int incidentsPurged = 0;
        int appealsPurged = 0;
        int resolutionsPurged = 0;
        int decisionsPurged = 0;

        if (!candidateIds.isEmpty()) {
            incidentsPurged = incidentRepository.purgeIncidentsByIds(candidateIds, now);
            appealsPurged = appealRepository.purgeAppealsByIncidentIds(candidateIds, now);
            resolutionsPurged = resolutionRepository.purgeResolutionsByIncidentIds(candidateIds);
            decisionsPurged = decisionJdbcRepository.purgeByIncidentIds(candidateIds, now);
        }

        // 4. Decisiones de moderación independientes o huérfanas con antigüedad superior al default
        int standaloneDecisionsPurged = decisionJdbcRepository.purgeStandaloneBefore(defaultCutoff, now);
        decisionsPurged += standaloneDecisionsPurged;

        long durationMs = System.currentTimeMillis() - startTime;

        // Sanitización estricta de logs: solo métricas y conteos, NUNCA texto purgado
        log.info("Moderation evidence purge completed in {} ms: {} incidents purged, {} appeals sanitized, {} resolutions sanitized, {} decision records sanitized",
                durationMs, incidentsPurged, appealsPurged, resolutionsPurged, decisionsPurged);

        return new PurgeExecutionSummary(incidentsPurged, appealsPurged, resolutionsPurged, decisionsPurged, now);
    }

    private int resolveRetentionDays(String incidentType, int fallback) {
        return retentionPolicyRepository.findByIncidentType(incidentType)
                .map(ModerationRetentionPolicyEntity::getRetentionDays)
                .orElse(fallback);
    }

    public record PurgeExecutionSummary(
            int incidentsPurged,
            int appealsPurged,
            int resolutionsPurged,
            int decisionsPurged,
            OffsetDateTime executedAt
    ) {
    }
}
