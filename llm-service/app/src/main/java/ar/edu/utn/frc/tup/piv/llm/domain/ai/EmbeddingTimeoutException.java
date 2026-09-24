package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** El adaptador de embeddings superó el timeout configurado — mismo criterio que
 * {@link ModelTimeoutException} para el puerto de texto. */
public class EmbeddingTimeoutException extends RuntimeException {
  public EmbeddingTimeoutException(String message) { super(message); }
}
