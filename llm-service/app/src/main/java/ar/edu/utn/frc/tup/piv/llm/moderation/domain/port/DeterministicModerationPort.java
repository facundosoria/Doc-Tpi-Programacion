package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;

/**
 * Puerto de dominio para el motor de moderación determinista orquestado.
 */
public interface DeterministicModerationPort {

    /**
     * Evalúa el contenido mediante detectores deterministas con corte temprano en la primera falla grave.
     *
     * @param messageId identificador del mensaje
     * @param text texto a moderar
     * @param courseId identificador del curso (para umbrales específicos)
     * @return decisión de moderación (ALLOW o BLOCK)
     */
    ModerationDecision evaluate(String messageId, String text, String courseId);

    default ModerationDecision evaluate(String messageId, String text) {
        return evaluate(messageId, text, null);
    }
}
