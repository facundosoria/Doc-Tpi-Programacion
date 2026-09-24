package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

/**
 * Puerto de dominio para normalización de texto y neutralización de técnicas de evasión.
 */
public interface TextNormalizerPort {

    /**
     * Aplica la cadena de transformaciones canónicas:
     * minúsculas, descomposición Unicode NFKD, descarte de diacríticos, leet speak,
     * colapso de caracteres repetidos y unión de separadores.
     *
     * @param text texto original
     * @return texto canónico normalizado
     */
    String normalize(String text);
}
