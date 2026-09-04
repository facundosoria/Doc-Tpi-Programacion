package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Información de una conversación de tutoría")
public record ConversacionResponse(
        @Schema(description = "Identificador de la conversación")
        UUID id,

        @Schema(description = "Título de la conversación")
        String titulo,

        @Schema(description = "Fecha de creación")
        LocalDateTime fechaCreacion,

        @Schema(description = "Estado de la conversación")
        String estado
) {
}
