package com.example.demo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagFuenteDto {
    private int pageNumber;
    private int chunkIndex;
    private double score;
    private String textoExtracto;
}
