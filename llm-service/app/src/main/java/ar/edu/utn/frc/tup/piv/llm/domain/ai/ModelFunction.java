package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Las funciones de IA del producto ([04](../../../../../../../../../../../../docs/04-funciones-de-ia.md)).
 * Solo {@link #TUTOR} tiene un adaptador cableado hoy (LLM-S01-H10); el resto queda declarado
 * para que la tabla función→proveedor+modelo (function_model_config) no tenga que migrarse de
 * nuevo cuando se sumen. {@link #EMBEDDING} se agrega para EP-09 (RAG) — no es una función de
 * generación de texto sino de vectorización, por eso se resuelve por su propio puerto
 * ({@link EmbeddingPort}), no por {@link ModelInvocationPort}. */
public enum ModelFunction {
  TUTOR, EVALUATOR, MODERATOR, GENERATOR, EMBEDDING
}
