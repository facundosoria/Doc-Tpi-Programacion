package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;

/**
 * Puerto de dominio para la emisión de eventos de resolución de moderación (LLM-S12-H02 / T4).
 */
public interface ModerationEventPublisherPort {

    void publishMessageUnblocked(ModerationResolutionDomainEvent event);
}
