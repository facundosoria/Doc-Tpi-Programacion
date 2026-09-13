package com.example.demo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que transporta el resultado de la decodificación determinista de una imagen del PDF.
 * Proporciona el título, la interpretación contextual del documento y su representación en Mermaid.js,
 * sin capas de arquitectura forzadas ni datos hardcodeados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramDecodedResultDto {
    private int imageIndex;
    private int pageNumber;
    private String tituloDetectado;
    private String tipoDiagrama;
    private String interpretacion;
    private String mermaidCode;
    private List<String> elementosEncontrados;
}
