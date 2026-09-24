package ar.edu.utn.frc.tup.piv.llm.domain.rag;

/** El archivo subido no es una fuente indexable (#669, CA3 de `docs/historias/ep-09/h01.md`).
 * Cada motivo trae su propio mensaje: el docente tiene que saber qué corregir, no recibir un
 * "archivo inválido" indistinto. La capa `api` la traduce a 422 sin indexar nada. */
public class InvalidPdfSourceException extends RuntimeException {
  public InvalidPdfSourceException(String message) {
    super(message);
  }
}
