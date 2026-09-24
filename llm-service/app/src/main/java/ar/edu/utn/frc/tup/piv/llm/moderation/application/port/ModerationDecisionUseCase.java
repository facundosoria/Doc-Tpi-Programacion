package ar.edu.utn.frc.tup.piv.llm.moderation.application.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;

/**
 * Puerto de entrada primario para ejecutar la decisión de moderación.
 */
public interface ModerationDecisionUseCase {

    /**
     * Evalúa o recupera de forma idempotente la decisión de moderación para el comando provisto.
     */
    ModerationDecision decide(ModerationDecisionCommand command);

    /**
     * Retira (invalida) una decisión de moderación previamente emitida.
     * CA_negativo_1 (LLM-S11-H01): un mensaje con decisión ALLOW jamás puede retirarse sin haber
     * pasado por una revisión explícita de moderación (incidente + resolución docente). Ese flujo
     * no debe existir: el intento se rechaza y se registra como error de protocolo.
     */
    void retireDecision(String messageId, String requestedBy);
}
