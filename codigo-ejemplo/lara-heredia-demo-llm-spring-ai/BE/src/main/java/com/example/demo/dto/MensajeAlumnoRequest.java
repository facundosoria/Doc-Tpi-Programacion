package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de un mensaje del alumno dentro de una conversación")
public record MensajeAlumnoRequest(
        @Schema(description = "Pregunta o texto del alumno", example = "¿Cómo sumo todos los números de un array?")
        String contenido
) {
}
