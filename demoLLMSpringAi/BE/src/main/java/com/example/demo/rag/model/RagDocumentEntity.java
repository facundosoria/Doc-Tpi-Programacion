package com.example.demo.rag.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rag_documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagDocumentEntity {

    @Id
    @Column(name = "document_id", length = 64, nullable = false)
    private String documentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "preview_text", columnDefinition = "TEXT")
    private String previewText;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
