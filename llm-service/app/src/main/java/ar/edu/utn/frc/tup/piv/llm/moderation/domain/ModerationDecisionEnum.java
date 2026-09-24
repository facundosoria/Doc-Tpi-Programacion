package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

/**
 * Estado canónico de la decisión de moderación según contrato RF-CHT-09 y HU LLM-S11-H01.
 */
public enum ModerationDecisionEnum {
    ALLOW,
    BLOCK,
    PENDING,
    PENDING_REVIEW
}
