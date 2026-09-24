package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.util.UUID;

/** La fuente pedida no existe para la cohorte del llamador. Se usa la misma excepción tanto para
 * "no existe" como para "existe pero es de otra cohorte" (`AGENTS.md` §2): responder distinto
 * filtraría la existencia de material ajeno. La capa `api` la traduce a 404. */
public class RagDocumentNotFoundException extends RuntimeException {
  public RagDocumentNotFoundException(UUID documentId) {
    super("Fuente no encontrada: " + documentId);
  }
}
