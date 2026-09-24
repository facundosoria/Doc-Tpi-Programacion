package ar.edu.utn.frc.tup.piv.llm.moderation.application.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationResolutionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ResolveIncidentCommand;

/**
 * Caso de uso para resolver un incidente de moderación (LLM-S12-H02 / T2).
 */
public interface ModerationIncidentResolveUseCase {

    ModerationResolutionResult resolve(ResolveIncidentCommand command);
}
