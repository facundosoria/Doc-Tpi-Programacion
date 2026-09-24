package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC directa de {@link RagDocument} (metadata de la fuente + bytes del PDF), sin
 * puerto — mismo estilo que {@link ConversationRepository}. El motor de búsqueda vectorial de sus
 * chunks es un puerto aparte ({@link ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort}) por
 * ser un detalle de infraestructura reemplazable; esta tabla no lo es. Portado de
 * `demoLLMSpringAi/.../rag/repository/RagDocumentRepository.java`, agregando `courseCohortId`
 * (partición obligatoria) y `active` (retirar es borrado lógico, EP-09). */
@Repository
public class RagDocumentRepository {
  private final JdbcTemplate jdbc;

  public RagDocumentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public RagDocument save(RagDocument document, byte[] pdfBytes) {
    jdbc.update(
        "insert into llm.rag_documents (id, course_cohort_id, file_name, file_size_bytes, page_count, "
            + "chunk_count, uploaded_at, preview_text, pdf_bytes, active) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        document.id(), document.courseCohortId(), document.fileName(), document.fileSizeBytes(), document.pageCount(),
        document.chunkCount(), document.uploadedAt(), document.previewText(), pdfBytes, document.active());
    return document;
  }

  public Optional<RagDocument> findById(UUID id) {
    return jdbc.query("select " + COLUMNS + " from llm.rag_documents where id = ?", RagDocumentRepository::mapRow, id)
        .stream()
        .findFirst();
  }

  public List<RagDocument> findActiveByCourse(UUID courseCohortId) {
    return jdbc.query(
        "select " + COLUMNS + " from llm.rag_documents where course_cohort_id = ? and active = true order by uploaded_at desc",
        RagDocumentRepository::mapRow, courseCohortId);
  }

  public byte[] getPdfBytes(UUID id) {
    return jdbc.query("select pdf_bytes from llm.rag_documents where id = ?",
            rs -> rs.next() ? rs.getBytes("pdf_bytes") : null, id);
  }

  /** Retiro lógico: deja de usarse en búsquedas (el llamador filtra por `active`) pero conserva
   * la fila y sus chunks para auditoría — no se hace `DELETE` (`docs/epicas/ep-09.md`). */
  public void deactivate(UUID id) {
    jdbc.update("update llm.rag_documents set active = false where id = ?", id);
  }

  private static final String COLUMNS =
      "id, course_cohort_id, file_name, file_size_bytes, page_count, chunk_count, uploaded_at, preview_text, active";

  private static RagDocument mapRow(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new RagDocument(
        rs.getObject("id", UUID.class), rs.getObject("course_cohort_id", UUID.class), rs.getString("file_name"),
        rs.getLong("file_size_bytes"), rs.getInt("page_count"), rs.getInt("chunk_count"),
        rs.getObject("uploaded_at", OffsetDateTime.class), rs.getString("preview_text"), rs.getBoolean("active"));
  }
}
