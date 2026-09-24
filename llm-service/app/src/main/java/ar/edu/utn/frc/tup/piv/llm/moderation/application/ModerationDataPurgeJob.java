package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import org.springframework.stereotype.Component;

/**
 * Delegado/Alias de {@link ModerationEvidencePurgeJob} conforme a la traza formal de la Tarea SMART T2 (LLM-S13-H02).
 */
@Component
public class ModerationDataPurgeJob {

    private final ModerationEvidencePurgeJob delegate;
    private final ModerationPurgeService purgeService;

    public ModerationDataPurgeJob(ModerationEvidencePurgeJob delegate, ModerationPurgeService purgeService) {
        this.delegate = delegate;
        this.purgeService = purgeService;
    }

    public void run() {
        delegate.executePurge();
    }

    public ModerationPurgeService.PurgeExecutionSummary purgeNow() {
        return purgeService.purgeExpiredEvidence();
    }
}
