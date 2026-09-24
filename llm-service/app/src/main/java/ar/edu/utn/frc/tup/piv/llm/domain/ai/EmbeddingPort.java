package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.util.List;

/** El puerto de vectorización que pide EP-09 (RAG): convertir texto en un vector denso sin que la
 * capa de aplicación conozca el proveedor concreto. Separado de {@link ModelInvocationPort}
 * porque este puerto no genera texto — devuelve un vector, no tiene sentido forzarlo dentro de
 * {@link ModelInvocationResult}. Vive en `domain`, sin dependencias de Spring (regla doc 37 §1). */
public interface EmbeddingPort {
  EmbeddingResult embed(String text);

  List<EmbeddingResult> embedBatch(List<String> texts);

  String provider();

  String model();
}
