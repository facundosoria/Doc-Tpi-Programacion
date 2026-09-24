package ar.edu.utn.frc.tup.piv.llm.moderation.api.mapper;

import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.CreateModerationAppealRequest;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationAppealResponse;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.CreateModerationAppealCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;

/**
 * Mapper estático entre DTOs de transporte HTTP y modelos de aplicación/dominio.
 */
public final class ModerationAppealApiMapper {

    private ModerationAppealApiMapper() {
    }

    public static CreateModerationAppealCommand toCommand(CreateModerationAppealRequest request, String userId) {
        return new CreateModerationAppealCommand(
                request.incidentId(),
                userId,
                request.appealReason()
        );
    }

    public static ModerationAppealResponse toResponse(ModerationAppeal appeal) {
        return new ModerationAppealResponse(
                appeal.getId(),
                appeal.getIncidentId(),
                appeal.getUserId(),
                appeal.getAppealReason(),
                appeal.getStatus(),
                appeal.getCreatedAt(),
                appeal.getUpdatedAt()
        );
    }
}
