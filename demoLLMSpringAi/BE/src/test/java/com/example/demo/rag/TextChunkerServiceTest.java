package com.example.demo.rag;

import com.example.demo.rag.model.DocumentChunk;
import com.example.demo.rag.service.PdfTextExtractorService;
import com.example.demo.rag.service.TextChunkerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerServiceTest {

    @Test
    void testChunkingWithOverlap() {
        TextChunkerService chunker = new TextChunkerService();

        String longParagraph = "El desarrollo de software con Spring Boot permite crear microservicios de manera ágil. "
                + "La inyección de dependencias facilita el desacoplamiento de componentes y la modularidad del sistema. "
                + "Spring AI complementa este ecosistema ofreciendo integraciones declarativas con modelos de lenguaje. "
                + "A través de ChatClient, los desarrolladores pueden interactuar con LLMs utilizando prompts estructurados. "
                + "Además, el patrón RAG permite enriquecer los prompts con información contextual externa como documentos PDF. "
                + "Esto previene alucinaciones y provee respuestas verificables basadas en fuentes confiables. "
                + "Cada módulo puede ser probado de forma aislada mediante tests unitarios rápidos y deterministas.";

        var page = new PdfTextExtractorService.ExtractedPage(1, longParagraph);
        List<DocumentChunk> chunks = chunker.createChunks("doc123", "spring_guide.pdf", List.of(page));

        assertFalse(chunks.isEmpty());
        for (DocumentChunk chunk : chunks) {
            assertEquals("doc123", chunk.getDocumentId());
            assertEquals("spring_guide.pdf", chunk.getDocumentName());
            assertEquals(1, chunk.getPageNumber());
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isBlank());
        }
    }
}
