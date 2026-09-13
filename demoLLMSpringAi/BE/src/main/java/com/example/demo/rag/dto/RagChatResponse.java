package com.example.demo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatResponse {
    private String respuesta;
    private String estado;               // "OK", "BLOCKED_PROFANITY", "BLOCKED_INJECTION", "BLOCKED_VALIDATION", "OUT_OF_CONTEXT"
    private String mensajeValidacion;     // Detalle amigable del motivo si fue bloqueado o validado
    private int tokensGastados;           // 0 si fue bloqueado o en caché
    private boolean cached;               // true si provino de caché en memoria
    private String rolTutor;              // Rol pedagógico del tutor
    private List<RagFuenteDto> fuentes;   // Páginas y fragmentos citados
    private String conversacionId;        // ID de la conversación para seguimiento multi-turno
}
