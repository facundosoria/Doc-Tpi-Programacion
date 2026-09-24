package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import com.pgvector.PGvector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Adaptador pgvector de {@link VectorStorePort}, portado de
 * `demoLLMSpringAi/.../rag/service/PgVectorStoreService.java` — sin el fallback automático a
 * memoria (TF-IDF) que tenía la demo: la infraestructura de `llm-service` siempre tiene
 * PostgreSQL+pgvector (regla de plataforma), así que ese fallback no es una rama de producción
 * acá (decisión documentada en `docs/estado-implementacion/ep-09/`). */
@Repository
public class PgVectorStoreAdapter implements VectorStorePort {
  private final JdbcTemplate jdbc;

  public PgVectorStoreAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void indexChunks(UUID documentId, List<DocumentChunk> chunks, List<EmbeddingResult> embeddings) {
    jdbc.update("delete from llm.rag_chunks where document_id = ?", documentId);

    List<Object[]> batchArgs = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      DocumentChunk chunk = chunks.get(i);
      EmbeddingResult embedding = (embeddings != null && i < embeddings.size()) ? embeddings.get(i) : null;
      batchArgs.add(new Object[] {
          chunk.id(), chunk.documentId(), chunk.documentName(), chunk.pageNumber(), chunk.chunkIndex(),
          chunk.content(), toPgVector(embedding)
      });
    }
    jdbc.batchUpdate(
        "insert into llm.rag_chunks (id, document_id, document_name, page_number, chunk_index, content, embedding) "
            + "values (?, ?, ?, ?, ?, ?, ?)",
        batchArgs);
  }

  @Override
  public void addChunk(DocumentChunk chunk, EmbeddingResult embedding) {
    jdbc.update(
        "insert into llm.rag_chunks (id, document_id, document_name, page_number, chunk_index, content, embedding) "
            + "values (?, ?, ?, ?, ?, ?, ?)",
        chunk.id(), chunk.documentId(), chunk.documentName(), chunk.pageNumber(), chunk.chunkIndex(),
        chunk.content(), toPgVector(embedding));
  }

  @Override
  public List<DocumentChunk> searchTopK(UUID courseCohortId, List<UUID> documentIds, float[] queryVector, int topK) {
    if (courseCohortId == null || documentIds == null || documentIds.isEmpty() || queryVector == null) {
      return Collections.emptyList();
    }

    PGvector pgQueryVector = new PGvector(queryVector);
    String inSql = String.join(",", Collections.nCopies(documentIds.size(), "?"));
    // El operador <=> queda en el esquema `llm` (la extensión se crea allí), fuera del search_path por defecto.
    // Cohorte (#675) y retiro lógico (#672) se filtran en el join, antes del ORDER BY/LIMIT: una
    // fuente ajena o retirada no ocupa lugares del top-K aunque el llamador la haya pedido por id.
    String sql = "select c.id, c.document_id, c.document_name, c.page_number, c.chunk_index, c.content, "
        + "(1 - (c.embedding OPERATOR(llm.<=>) ?)) as similarity from llm.rag_chunks c "
        + "join llm.rag_documents d on d.id = c.document_id and d.active = true and d.course_cohort_id = ? "
        + "where c.document_id in (" + inSql + ") and c.embedding is not null "
        + "order by c.embedding OPERATOR(llm.<=>) ? limit ?";

    List<Object> params = new ArrayList<>();
    params.add(pgQueryVector);
    params.add(courseCohortId);
    params.addAll(documentIds);
    params.add(pgQueryVector);
    params.add(topK);

    return jdbc.query(sql, (rs, row) -> new DocumentChunk(
        rs.getObject("id", UUID.class), rs.getObject("document_id", UUID.class), rs.getString("document_name"),
        rs.getInt("page_number"), rs.getInt("chunk_index"), rs.getString("content"), rs.getDouble("similarity")),
        params.toArray());
  }

  @Override
  public List<DocumentChunk> getChunks(UUID documentId) {
    return jdbc.query(
        "select id, document_id, document_name, page_number, chunk_index, content from llm.rag_chunks "
            + "where document_id = ? order by chunk_index asc",
        (rs, row) -> new DocumentChunk(rs.getObject("id", UUID.class), rs.getObject("document_id", UUID.class),
            rs.getString("document_name"), rs.getInt("page_number"), rs.getInt("chunk_index"), rs.getString("content"), 0.0),
        documentId);
  }

  @Override
  public void deleteChunks(UUID documentId) {
    jdbc.update("delete from llm.rag_chunks where document_id = ?", documentId);
  }

  private PGvector toPgVector(EmbeddingResult embedding) {
    return (embedding != null && embedding.vector() != null) ? new PGvector(embedding.vector()) : null;
  }
}
