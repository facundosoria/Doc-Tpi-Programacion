package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado asíncrono para la purga periódica de evidencia de moderación (LLM-S13-H02 / T2).
 * Corre típicamente en horario nocturno (ej. diario a las 03:00 AM) configurable por propiedad cron.
 */
@Component
@ConditionalOnProperty(name = "moderation.retention.enabled", havingValue = "true", matchIfMissing = true)
public class ModerationEvidencePurgeJob {

    private static final Logger log = LoggerFactory.getLogger(ModerationEvidencePurgeJob.class);

    private final ModerationPurgeService purgeService;

    public ModerationEvidencePurgeJob(ModerationPurgeService purgeService) {
        this.purgeService = purgeService;
    }

    @Scheduled(cron = "${moderation.retention.cron:${llm.moderation.retention.cron:0 0 3 * * *}}")
    public void executePurge() {
        log.info("Starting scheduled moderation evidence purge job...");
        try {
            ModerationPurgeService.PurgeExecutionSummary summary = purgeService.purgeExpiredEvidence();
            log.info("Scheduled moderation evidence purge job finished successfully: {} incidents, {} decisions",
                    summary.incidentsPurged(), summary.decisionsPurged());
        } catch (Exception e) {
            log.error("Error executing scheduled moderation evidence purge job", e);
        }
    }
}
