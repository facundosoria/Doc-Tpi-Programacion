package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Vector denso crudo del {@link EmbeddingPort}, antes de cualquier validación de dimensión.
 * `vector` puede ser {@code null} cuando el adaptador no pudo vectorizar un texto puntual (mismo
 * criterio que `EmbeddingService.computeEmbeddings` en la demo: se rellena con null en vez de
 * cortar el lote) — el llamador decide si eso bloquea la indexación o solo ese fragmento. */
public record EmbeddingResult(float[] vector, String provider, String model) {}
