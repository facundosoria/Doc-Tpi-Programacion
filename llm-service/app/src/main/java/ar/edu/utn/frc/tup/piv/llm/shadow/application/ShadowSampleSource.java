package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** De dónde salen las transcripciones que se reproducen. Una implementación por {@link ShadowRun.Source}. */
public interface ShadowSampleSource {
  ShadowRun.Source source();

  /** Hasta {@code run.sampleSize()} muestras. Sin anonimizar: el runner lo hace antes de enviarlas al proveedor. */
  List<Sample> load(ShadowRun run);

  /** {@code humanScores} es nulo si la fuente no tiene nota humana. */
  record Sample(String sourceRef, JsonNode transcript, JsonNode challengeContext, JsonNode humanScores) {}
}
