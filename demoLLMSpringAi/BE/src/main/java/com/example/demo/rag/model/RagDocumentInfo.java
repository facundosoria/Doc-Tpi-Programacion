package com.example.demo.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentInfo {
    private String documentId;
    private String fileName;
    private long fileSizeBytes;
    private int pageCount;
    private int chunkCount;
    private LocalDateTime uploadedAt;
    private String previewText;
}
