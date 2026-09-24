package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

/**
 * Códigos estándar de razón de moderación según contrato y especificación RF-CHT-10.
 */
public final class ModerationReasonCode {

    public static final String CLEAN = "CLEAN";
    public static final String SPAM = "SPAM";
    public static final String OFFENSIVE = "OFFENSIVE";
    public static final String CODE_OBFUSCATION = "CODE_OBFUSCATION";
    public static final String CONTEXTUAL_BLOCK = "CONTEXTUAL_BLOCK";
    public static final String ENGINE_UNAVAILABLE = "ENGINE_UNAVAILABLE";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String CONTEXTUAL_UNAVAILABLE = "CONTEXTUAL_UNAVAILABLE";
    public static final String FULL_ENGINE_UNAVAILABLE = "FULL_ENGINE_UNAVAILABLE";

    private ModerationReasonCode() {
    }
}
