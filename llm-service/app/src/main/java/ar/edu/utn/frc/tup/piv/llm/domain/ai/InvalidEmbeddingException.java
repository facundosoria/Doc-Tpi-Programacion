package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** El adaptador de embeddings devolvió un vector nulo o de dimensión inesperada — nunca se
 * propaga como si fuera válido, mismo criterio que {@link InvalidModelResponseException}. */
public class InvalidEmbeddingException extends RuntimeException {
  public InvalidEmbeddingException(String message) { super(message); }
}
