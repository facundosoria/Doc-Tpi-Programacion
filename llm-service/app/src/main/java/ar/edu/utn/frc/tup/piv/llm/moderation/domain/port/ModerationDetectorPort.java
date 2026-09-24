package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;

/**
 * Puerto de dominio para detectores individuales deterministas.
 */
public interface ModerationDetectorPort {

    /**
     * Nombre identificador del detector (ej. 'spam', 'code_obfuscation', 'offensive').
     */
    String getName();

    /**
     * Ejecuta la evaluación determinista sobre el texto suministrado.
     *
     * @param text texto a analizar
     * @param courseId identificador de curso para políticas o umbrales específicos (opcional)
     * @return resultado inmutable de la evaluación
     */
    DetectionResult detect(String text, String courseId);

    /**
     * Sobrecarga sin contexto de curso.
     */
    default DetectionResult detect(String text) {
        return detect(text, null);
    }
}
