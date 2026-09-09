package com.example.demo.rag.service;

import com.example.demo.rag.model.DocumentChunk;
import com.example.demo.rag.model.RagDocumentInfo;
import com.pgvector.PGvector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PgVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreService.class);

    private final JdbcTemplate jdbcTemplate;
    private final InMemoryRagVectorStore fallbackMemoryStore;
    private final boolean isPostgres;

    public PgVectorStoreService(JdbcTemplate jdbcTemplate,
                                DataSource dataSource,
                                InMemoryRagVectorStore fallbackMemoryStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.fallbackMemoryStore = fallbackMemoryStore;
        this.isPostgres = checkIsPostgres(dataSource);

        if (this.isPostgres) {
            log.info("🐘 PgVectorStoreService inicializado con PostgreSQL / pgvector (Nube o local).");
            ensureTablesAndExtension();
        } else {
            log.info("💾 Base de datos no-Postgres detectada. PgVectorStoreService operará en modo memoria sincronizada.");
        }
    }

    private boolean checkIsPostgres(DataSource ds) {
        try (Connection conn = ds.getConnection()) {
            String prod = conn.getMetaData().getDatabaseProductName();
            return prod != null && prod.toLowerCase().contains("postgres");
        } catch (SQLException e) {
            log.warn("No se pudo determinar el tipo de base de datos: {}", e.getMessage());
            return false;
        }
    }

    private void ensureTablesAndExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rag_documentos (
                    document_id VARCHAR(64) PRIMARY KEY,
                    file_name VARCHAR(255) NOT NULL,
                    file_size_bytes BIGINT NOT NULL,
                    page_count INT NOT NULL,
                    chunk_count INT NOT NULL,
                    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    preview_text TEXT
                );
            """);
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rag_chunks (
                    id VARCHAR(64) PRIMARY KEY,
                    document_id VARCHAR(64) NOT NULL REFERENCES rag_documentos(document_id) ON DELETE CASCADE,
                    document_name VARCHAR(255) NOT NULL,
                    page_number INT NOT NULL,
                    chunk_index INT NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(768)
                );
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_document_id ON rag_chunks(document_id);");
            log.info("Tablas de RAG y extensión vector verificadas exitosamente en PostgreSQL.");
        } catch (Exception e) {
            log.warn("Aviso al verificar extensión/tablas en PostgreSQL: {}", e.getMessage());
        }
    }

    public boolean isPostgresActive() {
        return isPostgres;
    }

    /**
     * Indexa un documento y sus chunks con vectores densos en pgvector.
     */
    public void indexDocumentWithVectors(RagDocumentInfo doc, List<DocumentChunk> chunks, List<float[]> embeddings) {
        // Guardar siempre en memoria como espejo
        fallbackMemoryStore.indexDocument(doc, chunks);

        if (!isPostgres) {
            return;
        }

        try {
            // 1. Guardar o actualizar documento
            String sqlDoc = """
                INSERT INTO rag_documentos (document_id, file_name, file_size_bytes, page_count, chunk_count, uploaded_at, preview_text)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (document_id) DO UPDATE SET
                    file_name = EXCLUDED.file_name,
                    file_size_bytes = EXCLUDED.file_size_bytes,
                    page_count = EXCLUDED.page_count,
                    chunk_count = EXCLUDED.chunk_count,
                    preview_text = EXCLUDED.preview_text;
            """;

            jdbcTemplate.update(sqlDoc,
                    doc.getDocumentId(),
                    doc.getFileName(),
                    doc.getFileSizeBytes(),
                    doc.getPageCount(),
                    doc.getChunkCount(),
                    Timestamp.valueOf(doc.getUploadedAt() != null ? doc.getUploadedAt() : LocalDateTime.now()),
                    doc.getPreviewText()
            );

            // 2. Eliminar chunks previos si existieran
            jdbcTemplate.update("DELETE FROM rag_chunks WHERE document_id = ?", doc.getDocumentId());

            // 3. Insertar chunks con sus vectores pgvector
            String sqlChunk = """
                INSERT INTO rag_chunks (id, document_id, document_name, page_number, chunk_index, content, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            List<Object[]> batchArgs = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] emb = (embeddings != null && i < embeddings.size()) ? embeddings.get(i) : null;
                PGvector pgVector = (emb != null) ? new PGvector(emb) : null;

                batchArgs.add(new Object[]{
                        chunk.getId(),
                        doc.getDocumentId(),
                        doc.getFileName(),
                        chunk.getPageNumber(),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        pgVector
                });
            }

            jdbcTemplate.batchUpdate(sqlChunk, batchArgs);
            log.info("✅ {} chunks indexados en pgvector para documento: {}", chunks.size(), doc.getFileName());

        } catch (Exception e) {
            log.error("Error al persistir chunks en pgvector: {}", e.getMessage(), e);
        }
    }

    /**
     * Búsqueda semántica cruzada multi-documento estilo NotebookLM.
     * Filtra estrictamente por los documentIds seleccionados.
     */
    public List<DocumentChunk> searchTopKMultiDoc(List<String> documentIds, float[] queryVector, String queryText, int topK) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Si tenemos pgvector y vector de consulta, ejecutamos la búsqueda en PostgreSQL
        if (isPostgres && queryVector != null) {
            try {
                PGvector pgQueryVector = new PGvector(queryVector);
                String inSql = String.join(",", Collections.nCopies(documentIds.size(), "?"));

                String sql = String.format("""
                    SELECT id, document_id, document_name, page_number, chunk_index, content,
                           (1 - (embedding <=> ?)) AS similarity
                    FROM rag_chunks
                    WHERE document_id IN (%s)
                      AND embedding IS NOT NULL
                    ORDER BY embedding <=> ?
                    LIMIT ?
                """, inSql);

                List<Object> params = new ArrayList<>();
                params.add(pgQueryVector);
                params.addAll(documentIds);
                params.add(pgQueryVector);
                params.add(topK);

                List<DocumentChunk> results = jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) ->
                        DocumentChunk.builder()
                                .id(rs.getString("id"))
                                .documentId(rs.getString("document_id"))
                                .documentName(rs.getString("document_name"))
                                .pageNumber(rs.getInt("page_number"))
                                .chunkIndex(rs.getInt("chunk_index"))
                                .content(rs.getString("content"))
                                .similarityScore(rs.getDouble("similarity"))
                                .build()
                );

                if (!results.isEmpty()) {
                    return results;
                }
            } catch (Exception e) {
                log.warn("Fallo búsqueda vectorial en pgvector, recurriendo a fallback en memoria: {}", e.getMessage());
            }
        }

        // Fallback en memoria multi-documento (TF-IDF cross-retrieval)
        return searchInMemoryMultiDoc(documentIds, queryText, topK);
    }

    private List<DocumentChunk> searchInMemoryMultiDoc(List<String> documentIds, String queryText, int topK) {
        List<DocumentChunk> combined = new ArrayList<>();
        for (String docId : documentIds) {
            combined.addAll(fallbackMemoryStore.searchTopK(docId, queryText, topK));
        }
        // Ordenar por similitud y tomar los topK globales
        return combined.stream()
                .sorted(Comparator.comparingDouble(DocumentChunk::getSimilarityScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * Lista todos los documentos registrados.
     */
    public List<RagDocumentInfo> getAllDocuments() {
        if (isPostgres) {
            try {
                String sql = """
                    SELECT document_id, file_name, file_size_bytes, page_count, chunk_count, uploaded_at, preview_text
                    FROM rag_documentos
                    ORDER BY uploaded_at DESC
                """;
                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("uploaded_at");
                    LocalDateTime dt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    return RagDocumentInfo.builder()
                            .documentId(rs.getString("document_id"))
                            .fileName(rs.getString("file_name"))
                            .fileSizeBytes(rs.getLong("file_size_bytes"))
                            .pageCount(rs.getInt("page_count"))
                            .chunkCount(rs.getInt("chunk_count"))
                            .uploadedAt(dt)
                            .previewText(rs.getString("preview_text"))
                            .build();
                });
            } catch (Exception e) {
                log.warn("Error leyendo documentos de PostgreSQL: {}", e.getMessage());
            }
        }
        return fallbackMemoryStore.getAllDocuments();
    }

    public Optional<RagDocumentInfo> getDocument(String documentId) {
        if (isPostgres) {
            try {
                String sql = """
                    SELECT document_id, file_name, file_size_bytes, page_count, chunk_count, uploaded_at, preview_text
                    FROM rag_documentos
                    WHERE document_id = ?
                """;
                List<RagDocumentInfo> list = jdbcTemplate.query(sql, new Object[]{documentId}, (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("uploaded_at");
                    LocalDateTime dt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    return RagDocumentInfo.builder()
                            .documentId(rs.getString("document_id"))
                            .fileName(rs.getString("file_name"))
                            .fileSizeBytes(rs.getLong("file_size_bytes"))
                            .pageCount(rs.getInt("page_count"))
                            .chunkCount(rs.getInt("chunk_count"))
                            .uploadedAt(dt)
                            .previewText(rs.getString("preview_text"))
                            .build();
                });
                if (!list.isEmpty()) {
                    return Optional.of(list.get(0));
                }
            } catch (Exception e) {
                log.warn("Error leyendo documento de PostgreSQL: {}", e.getMessage());
            }
        }
        return fallbackMemoryStore.getDocument(documentId);
    }

    public List<DocumentChunk> getAllChunks(String documentId) {
        if (isPostgres) {
            try {
                String sql = """
                    SELECT id, document_id, document_name, page_number, chunk_index, content
                    FROM rag_chunks
                    WHERE document_id = ?
                    ORDER BY chunk_index ASC
                """;
                return jdbcTemplate.query(sql, new Object[]{documentId}, (rs, rowNum) ->
                        DocumentChunk.builder()
                                .id(rs.getString("id"))
                                .documentId(rs.getString("document_id"))
                                .documentName(rs.getString("document_name"))
                                .pageNumber(rs.getInt("page_number"))
                                .chunkIndex(rs.getInt("chunk_index"))
                                .content(rs.getString("content"))
                                .build()
                );
            } catch (Exception e) {
                log.warn("Error leyendo chunks de PostgreSQL: {}", e.getMessage());
            }
        }
        return fallbackMemoryStore.getAllChunks(documentId);
    }

    /**
     * Elimina un documento y todos sus fragmentos.
     */
    public void deleteDocument(String documentId) {
        fallbackMemoryStore.removeDocument(documentId);
        if (isPostgres) {
            try {
                jdbcTemplate.update("DELETE FROM rag_documentos WHERE document_id = ?", documentId);
                log.info("Documento {} eliminado de PostgreSQL.", documentId);
            } catch (Exception e) {
                log.error("Error al eliminar documento de PostgreSQL: {}", e.getMessage());
            }
        }
    }
}
