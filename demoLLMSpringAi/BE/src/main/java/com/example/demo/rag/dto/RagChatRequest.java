package com.example.demo.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.List;

public record RagChatRequest(
        String documentId,

        List<String> documentIds,

        @NotBlank(message = "La pregunta no puede estar vacía")
        @Size(min = 4, max = 600, message = "La pregunta debe contener entre 4 y 600 caracteres")
        String pregunta,

        String conversacionId
) {
    public RagChatRequest(String documentId, String pregunta, String conversacionId) {
        this(documentId, null, pregunta, conversacionId);
    }

    /**
     * Retorna la lista consolidada de documentos a consultar (soporta tanto un solo ID como lista multi-fuente).
     */
    public List<String> getEffectiveDocumentIds() {
        if (documentIds != null && !documentIds.isEmpty()) {
            return documentIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
        }
        if (documentId != null && !documentId.isBlank()) {
            return List.of(documentId.trim());
        }
        return Collections.emptyList();
    }
}
