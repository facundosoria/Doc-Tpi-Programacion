package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Se agotó el presupuesto (llamadas o costo) de una función de IA en el período vigente. */
public class BudgetExceededException extends RuntimeException {
  private final ModelFunction function;

  public BudgetExceededException(ModelFunction function, String message) {
    super(message);
    this.function = function;
  }

  public ModelFunction function() { return function; }
}
