package com.example.demo.rag;

import com.example.demo.rag.model.DocumentChunk;
import com.example.demo.rag.model.RagDocumentInfo;
import com.example.demo.rag.service.InMemoryRagVectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRagVectorStoreTest {

    private InMemoryRagVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new InMemoryRagVectorStore();
    }

    @Test
    void testIndexAndSearchTopK() {
        String docId = "doc_test_1";
        RagDocumentInfo docInfo = RagDocumentInfo.builder()
                .documentId(docId)
                .fileName("guia_java.pdf")
                .fileSizeBytes(1024)
                .pageCount(2)
                .chunkCount(2)
                .uploadedAt(LocalDateTime.now())
                .previewText("Introducción a Java y programación orientada a objetos.")
                .build();

        DocumentChunk chunk1 = DocumentChunk.builder()
                .id("c1")
                .documentId(docId)
                .documentName("guia_java.pdf")
                .pageNumber(1)
                .chunkIndex(0)
                .content("En Java, un bucle for tradicional permite iterar sobre arreglos y colecciones con un índice explícito.")
                .build();

        DocumentChunk chunk2 = DocumentChunk.builder()
                .id("c2")
                .documentId(docId)
                .documentName("guia_java.pdf")
                .pageNumber(2)
                .chunkIndex(1)
                .content("La herencia en Java se implementa con la palabra clave extends, permitiendo reutilizar código de clases base.")
                .build();

        vectorStore.indexDocument(docInfo, List.of(chunk1, chunk2));

        assertTrue(vectorStore.hasDocument(docId));
        assertEquals(2, vectorStore.getAllChunks(docId).size());

        // Consulta sobre bucles
        List<DocumentChunk> resultadosBucle = vectorStore.searchTopK(docId, "¿Cómo funciona un bucle for en arreglos?", 1);
        assertFalse(resultadosBucle.isEmpty());
        assertEquals("c1", resultadosBucle.get(0).getId());
        assertEquals(1, resultadosBucle.get(0).getPageNumber());
        assertTrue(resultadosBucle.get(0).getSimilarityScore() > 0);

        // Consulta sobre herencia
        List<DocumentChunk> resultadosHerencia = vectorStore.searchTopK(docId, "¿Qué palabra clave se usa para la herencia y clases base?", 1);
        assertFalse(resultadosHerencia.isEmpty());
        assertEquals("c2", resultadosHerencia.get(0).getId());
        assertEquals(2, resultadosHerencia.get(0).getPageNumber());
    }

    @Test
    void testSearchInNonExistentDocReturnsEmpty() {
        List<DocumentChunk> results = vectorStore.searchTopK("doc_inexistente", "pregunta cualquiera", 3);
        assertTrue(results.isEmpty());
    }
}
