package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway;

public interface LlmGateway {

    /**
     * Envía una solicitud al proveedor configurado para la función especificada.
     *
     * @param funcion Identificador de la función (ej: "tutor", "evaluador", "moderador").
     * @param systemPrompt Prompt del sistema con las directivas pedagógicas/evaluativas.
     * @param userPrompt Prompt del usuario con el contexto y la consulta.
     * @return Texto de respuesta generado por el modelo.
     */
    String llamar(String funcion, String systemPrompt, String userPrompt);

    /**
     * Retorna el identificador del modelo actualmente en uso para la función dada.
     */
    String obtenerModeloActivo(String funcion);
}
