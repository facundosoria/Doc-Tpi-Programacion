package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ContextualClassificationResult;

/**
 * Puerto de dominio para la invocación del clasificador contextual externo de moderación
 * (OpenAI omni-moderation, Groq Llama Guard 3 o Perspective API).
 */
public interface ContextualModerationPort {

    /**
     * Clasifica el texto mediante el clasificador contextual externo.
     *
     * @param text texto a moderar
     * @return resultado de la clasificación contextual
     */
    ContextualClassificationResult classify(String text);
}
