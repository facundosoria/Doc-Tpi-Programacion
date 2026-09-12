package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Las cinco funciones de IA del producto ([04](../../../../../../../../../../../docs/04-funciones-de-ia.md)).
 * Solo {@link #TUTOR} tiene un adaptador cableado hoy (LLM-S01-H10); el resto queda declarado
 * para que la tabla función→proveedor+modelo (function_model_config) no tenga que migrarse de
 * nuevo cuando se sumen. */
public enum ModelFunction {
  TUTOR, EVALUATOR, MODERATOR, GENERATOR
}
