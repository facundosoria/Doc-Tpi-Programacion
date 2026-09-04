package com.example.demo.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagChatRequest(
        @NotBlank(message = "El ID del documento es obligatorio")
        String documentId,

        @NotBlank(message = "La pregunta no puede estar vacía")
        @Size(min = 4, max = 600, message = "La pregunta debe contener entre 4 y 600 caracteres")
        String pregunta,

        String conversacionId
) {}
