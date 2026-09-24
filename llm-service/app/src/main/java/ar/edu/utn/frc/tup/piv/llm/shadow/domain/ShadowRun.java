package ar.edu.utn.frc.tup.piv.llm.shadow.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una corrida de shadow: evalúa una rúbrica candidata y la baseline sobre las mismas transcripciones
 * y guarda la comparación. {@code summary} queda nulo hasta que la corrida termina. */
public record ShadowRun(UUID id, UUID courseId, UUID baselineRubricVersionId, UUID candidateRubricVersionId,
    Source source, UUID goldenSetVersionId, int sampleSize, double divergenceThreshold, State state, int progress,
    ShadowMetrics.Summary summary, String failureCode, UUID createdByUserId, OffsetDateTime createdAt,
    OffsetDateTime startedAt, OffsetDateTime finishedAt) {

  public enum Source { GOLDEN_SET, TUTOR_CONVERSATIONS }

  public enum State { QUEUED, RUNNING, COMPLETED, FAILED }
}
