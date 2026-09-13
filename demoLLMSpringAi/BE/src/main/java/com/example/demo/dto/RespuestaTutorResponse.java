package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta del tutor ante un mensaje del alumno")
public record RespuestaTutorResponse(
        @Schema(description = "Texto de la respuesta del tutor")
        String respuesta,

        @Schema(description = "Estado del procesamiento: OK, BLOCKED (jailbreak) o DEGRADED", example = "OK")
        String estado,

        @Schema(description = "Modelo de LLM utilizado", example = "llama-3.3-70b-versatile")
        String modelo,

        @Schema(description = "Identificador de la conversación")
        String conversacionId
) {
}
