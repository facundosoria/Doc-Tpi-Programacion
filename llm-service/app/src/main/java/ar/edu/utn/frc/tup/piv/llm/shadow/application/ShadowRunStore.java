package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia del shadow. Es el único destino de escritura del módulo: nada de esto llega al
 * outbox, a Kafka ni a las tablas de calibración/evaluaciones reales. */
public interface ShadowRunStore {
  /** Devuelve la corrida existente si la {@code idempotencyKey} ya se usó. */
  ShadowRun create(NewRun run);

  Optional<ShadowRun> find(UUID courseId, UUID runId);

  Optional<ShadowRun> findById(UUID runId);

  List<ShadowRun> list(UUID courseId);

  /** Reclama la corrida QUEUED más vieja pasándola a RUNNING. */
  Optional<ShadowRun> claimNextQueued();

  void recordProgress(UUID runId, int progress);

  void recordCase(UUID runId, ShadowMetrics.CaseOutcome outcome);

  void complete(UUID runId, ShadowMetrics.Summary summary);

  void fail(UUID runId, String failureCode);

  /** La rúbrica de la calibración activa del curso, si tiene una. */
  Optional<UUID> activeRubricVersion(UUID courseId);

  /** Visible = de la plataforma (ya publicada) o del propio curso, en cualquier estado. */
  boolean rubricVersionVisibleToCourse(UUID courseId, UUID rubricVersionId);

  boolean goldenSetVersionVisibleToCourse(UUID courseId, UUID goldenSetVersionId);

  /** Borra corridas terminadas hace más de {@code days} días (sus casos caen en cascada). */
  int purgeFinishedBefore(int days);

  record NewRun(UUID courseId, UUID baselineRubricVersionId, UUID candidateRubricVersionId, ShadowRun.Source source,
      UUID goldenSetVersionId, int sampleSize, double divergenceThreshold, UUID idempotencyKey, UUID createdByUserId) {}
}
