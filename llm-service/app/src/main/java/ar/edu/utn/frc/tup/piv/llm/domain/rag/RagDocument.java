package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una fuente (PDF) subida por un curso/cohorte (EP-09). Portado de
 * `demoLLMSpringAi/.../rag/model/RagDocumentInfo.java`, agregando `courseCohortId` (partición
 * obligatoria, `AGENTS.md` §2) y `active` — retirar una fuente es borrado lógico: deja de usarse
 * en búsquedas pero conserva su historial de auditoría (`docs/epicas/ep-09.md`).
 *
 * <p>Los bytes del PDF NO viven en este record (se cargan aparte, bajo demanda, desde
 * `RagDocumentRepository.getPdfBytes`) para no traer un `BYTEA` potencialmente pesado cada vez que
 * se lista o se consulta un documento. */
public record RagDocument(
    UUID id,
    UUID courseCohortId,
    String fileName,
    long fileSizeBytes,
    int pageCount,
    int chunkCount,
    OffsetDateTime uploadedAt,
    String previewText,
    boolean active) {

  /** Retiro lógico (CA5 de `docs/historias/ep-09/h01.md`): la fuente deja de participar en
   * listados y búsquedas nuevas, pero su registro y sus chunks se conservan para auditoría —
   * nunca se traduce en un `DELETE`. Idempotente: retirar una fuente ya retirada devuelve el mismo
   * estado, no es un error. */
  public RagDocument retire() {
    if (!active) {
      return this;
    }
    return new RagDocument(id, courseCohortId, fileName, fileSizeBytes, pageCount, chunkCount, uploadedAt, previewText, false);
  }

  /** Aislamiento por cohorte (`AGENTS.md` §2): una fuente solo es visible/administrable desde su
   * propio `courseCohortId`. */
  public boolean belongsTo(UUID courseCohortId) {
    return this.courseCohortId != null && this.courseCohortId.equals(courseCohortId);
  }
}
