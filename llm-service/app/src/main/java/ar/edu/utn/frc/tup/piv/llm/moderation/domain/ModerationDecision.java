package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad raíz de dominio inmutable que representa una decisión de moderación con sus invariantes.
 */
public final class ModerationDecision {

    private final String messageId;
    private final ModerationDecisionEnum decision;
    private final String reasonCode;
    private final String classifierUsed;
    private final long latencyMs;
    private final UUID incidentId;
    private final String contentHash;
    private final String degradationReason;

    public ModerationDecision(String messageId,
                              ModerationDecisionEnum decision,
                              String reasonCode,
                              String classifierUsed,
                              long latencyMs,
                              UUID incidentId,
                              String contentHash) {
        this(messageId, decision, reasonCode, classifierUsed, latencyMs, incidentId, contentHash, null);
    }

    public ModerationDecision(String messageId,
                              ModerationDecisionEnum decision,
                              String reasonCode,
                              String classifierUsed,
                              long latencyMs,
                              UUID incidentId,
                              String contentHash,
                              String degradationReason) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("message_id no puede ser nulo ni vacío.");
        }
        this.messageId = messageId;
        this.decision = Objects.requireNonNull(decision, "decision no puede ser nula.");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reason_code no puede ser nulo.");
        this.classifierUsed = Objects.requireNonNull(classifierUsed, "classifier_used no puede ser nulo.");
        this.latencyMs = Math.max(0, latencyMs);
        this.contentHash = contentHash != null ? contentHash : "";
        this.degradationReason = degradationReason;

        if (decision == ModerationDecisionEnum.ALLOW) {
            this.incidentId = null;
        } else {
            this.incidentId = incidentId != null ? incidentId : UUID.randomUUID();
        }
    }

    public static ModerationDecision allow(String messageId, String reasonCode, String classifierUsed, long latencyMs, String contentHash) {
        return new ModerationDecision(messageId, ModerationDecisionEnum.ALLOW, reasonCode, classifierUsed, latencyMs, null, contentHash, null);
    }

    public static ModerationDecision block(String messageId, String reasonCode, String classifierUsed, long latencyMs, String contentHash, UUID incidentId) {
        return new ModerationDecision(messageId, ModerationDecisionEnum.BLOCK, reasonCode, classifierUsed, latencyMs, incidentId, contentHash, null);
    }

    public static ModerationDecision pending(String messageId, String reasonCode, String classifierUsed, long latencyMs, String contentHash) {
        return pending(messageId, reasonCode, classifierUsed, latencyMs, contentHash, null);
    }

    public static ModerationDecision pending(String messageId, String reasonCode, String classifierUsed, long latencyMs, String contentHash, String degradationReason) {
        return new ModerationDecision(messageId, ModerationDecisionEnum.PENDING, reasonCode, classifierUsed, latencyMs, UUID.randomUUID(), contentHash, degradationReason);
    }

    public static ModerationDecision pendingReview(String messageId, String reasonCode, String classifierUsed, long latencyMs, String contentHash, String degradationReason) {
        return new ModerationDecision(messageId, ModerationDecisionEnum.PENDING_REVIEW, reasonCode, classifierUsed, latencyMs, UUID.randomUUID(), contentHash, degradationReason);
    }

    public boolean shouldPersistText() {
        return this.decision != ModerationDecisionEnum.ALLOW;
    }

    public String getMessageId() {
        return messageId;
    }

    public ModerationDecisionEnum getDecision() {
        return decision;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getClassifierUsed() {
        return classifierUsed;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getDegradationReason() {
        return degradationReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModerationDecision that)) return false;
        return latencyMs == that.latencyMs
                && Objects.equals(messageId, that.messageId)
                && decision == that.decision
                && Objects.equals(reasonCode, that.reasonCode)
                && Objects.equals(classifierUsed, that.classifierUsed)
                && Objects.equals(incidentId, that.incidentId)
                && Objects.equals(contentHash, that.contentHash)
                && Objects.equals(degradationReason, that.degradationReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, decision, reasonCode, classifierUsed, latencyMs, incidentId, contentHash, degradationReason);
    }

    @Override
    public String toString() {
        return "ModerationDecision{" +
                "messageId='" + messageId + '\'' +
                ", decision=" + decision +
                ", reasonCode='" + reasonCode + '\'' +
                ", classifierUsed='" + classifierUsed + '\'' +
                ", latencyMs=" + latencyMs +
                ", incidentId=" + incidentId +
                ", contentHash='" + contentHash + '\'' +
                ", degradationReason='" + degradationReason + '\'' +
                '}';
    }
}
