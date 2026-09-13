package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Mensaje de una conversación")
public record MensajeResponse(
        @Schema(description = "Identificador del mensaje")
        UUID id,

        @Schema(description = "Rol del mensaje: alumno o tutor")
        String rol,

        @Schema(description = "Contenido del mensaje")
        String contenido,

        @Schema(description = "Timestamp del mensaje")
        LocalDateTime timestamp
) {
}
