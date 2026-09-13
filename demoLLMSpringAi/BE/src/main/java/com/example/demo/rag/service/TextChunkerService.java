package com.example.demo.rag.service;

import com.example.demo.rag.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TextChunkerService {

    private static final Logger log = LoggerFactory.getLogger(TextChunkerService.class);

    // Parámetros de chunking balanceados para RAG con contexto completo y pedagógico
    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;
    private static final int MIN_CHUNK_SIZE = 100;

    /**
     * Divide las páginas extraídas de un PDF en chunks contextuales respetando fronteras de oraciones.
     */
    public List<DocumentChunk> createChunks(String documentId,
                                            String documentName,
                                            List<PdfTextExtractorService.ExtractedPage> pages) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int globalChunkIndex = 0;

        for (PdfTextExtractorService.ExtractedPage page : pages) {
            String pageText = page.text();
            if (pageText == null || pageText.isBlank()) {
                continue;
            }

            List<String> textPieces = splitIntoPieces(pageText, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);

            for (String piece : textPieces) {
                if (piece.length() >= MIN_CHUNK_SIZE) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .id(documentId + "_chunk_" + globalChunkIndex)
                            .documentId(documentId)
                            .documentName(documentName)
                            .pageNumber(page.pageNumber())
                            .chunkIndex(globalChunkIndex)
                            .content(piece)
                            .build();

                    chunks.add(chunk);
                    globalChunkIndex++;
                }
            }
        }

        log.info("Documento '{}' dividido en {} fragmentos (chunks) contextuales", documentName, chunks.size());
        return chunks;
    }

    private List<String> splitIntoPieces(String text, int targetSize, int overlap) {
        List<String> pieces = new ArrayList<>();
        int textLength = text.length();

        if (textLength <= targetSize) {
            pieces.add(text.trim());
            return pieces;
        }

        int start = 0;
        while (start < textLength) {
            int end = Math.min(start + targetSize, textLength);

            // Intentar cortar en límite natural de oración o párrafo
            if (end < textLength) {
                int naturalBoundary = findNaturalBoundary(text, start, end);
                if (naturalBoundary > start + (targetSize / 2)) {
                    end = naturalBoundary;
                }
            }

            String chunkContent = text.substring(start, end).trim();
            if (!chunkContent.isEmpty()) {
                pieces.add(chunkContent);
            }

            if (end >= textLength) {
                break;
            }

            // Aplicar solapamiento (overlap) para mantener coherencia semántica
            start = Math.max(end - overlap, start + 1);
        }

        return pieces;
    }

    private int findNaturalBoundary(String text, int start, int end) {
        // 1. Prioridad: Punto y espacio o salto de párrafo
        int paragraphBreak = text.lastIndexOf("\n\n", end);
        if (paragraphBreak > start + 150) return paragraphBreak + 2;

        int sentenceEnd = text.lastIndexOf(". ", end);
        if (sentenceEnd > start + 150) return sentenceEnd + 2;

        int questionEnd = text.lastIndexOf("? ", end);
        if (questionEnd > start + 150) return questionEnd + 2;

        int exclamationEnd = text.lastIndexOf("! ", end);
        if (exclamationEnd > start + 150) return exclamationEnd + 2;

        int lineBreak = text.lastIndexOf("\n", end);
        if (lineBreak > start + 100) return lineBreak + 1;

        int space = text.lastIndexOf(" ", end);
        if (space > start + 50) return space + 1;

        return end;
    }
}
