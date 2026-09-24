package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

/**
 * Estados posibles del ciclo de vida de una apelación de moderación (LLM-S12-H01 / EP-08).
 */
public enum ModerationAppealStatus {
    PENDING_REVIEW,
    CONFIRMED,
    REVERSED
}
