package com.example.demo.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    private String id;
    private String documentId;
    private String documentName;
    private int pageNumber;
    private String content;
    private int chunkIndex;
    
    // Vector de términos (palabra -> peso TF-IDF o frecuencia normalizada)
    private Map<String, Double> termVector;
    
    // Puntuación de relevancia calculada durante la búsqueda
    private double similarityScore;
}
