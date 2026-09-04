package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para crear una nueva conversación de tutoría")
public record CrearConversacionRequest(
        @Schema(description = "Título o tema de la conversación", example = "Bucles en Java")
        String titulo
) {
}
