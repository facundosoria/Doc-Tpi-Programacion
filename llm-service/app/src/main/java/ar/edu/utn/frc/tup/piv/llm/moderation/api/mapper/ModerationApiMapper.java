package ar.edu.utn.frc.tup.piv.llm.moderation.api.mapper;

import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationDecisionRequest;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationDecisionResponse;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;

/**
 * Mapeador entre DTOs de API y modelos de aplicación/dominio.
 */
public final class ModerationApiMapper {

    private ModerationApiMapper() {
    }

    public static ModerationDecisionCommand toCommand(ModerationDecisionRequest request) {
        return new ModerationDecisionCommand(
                request.messageId(),
                request.courseId(),
                request.senderRole(),
                request.text(),
                request.contextFlags(),
                request.senderId().trim()
        );
    }

    public static ModerationDecisionResponse toResponse(ModerationDecision decision) {
        return new ModerationDecisionResponse(
                decision.getMessageId(),
                decision.getDecision(),
                decision.getReasonCode(),
                decision.getClassifierUsed(),
                decision.getLatencyMs(),
                decision.getIncidentId(),
                decision.getDegradationReason()
        );
    }
}
