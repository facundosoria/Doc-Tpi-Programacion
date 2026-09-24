package ar.edu.utn.frc.tup.piv.llm.moderation.application.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.CreateModerationAppealCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import java.util.UUID;

/**
 * Puerto de entrada (caso de uso) para gestión de apelaciones de moderación (LLM-S12-H01).
 */
public interface ModerationAppealUseCase {

    ModerationAppeal createAppeal(CreateModerationAppealCommand command);

    ModerationAppeal getAppeal(UUID appealId, String authenticatedUserId);
}
