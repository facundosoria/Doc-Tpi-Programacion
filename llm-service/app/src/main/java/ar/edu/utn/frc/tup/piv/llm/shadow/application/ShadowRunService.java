package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de E-31: pedir y consultar corridas de shadow. Solo encola; la ejecución la hace
 * {@link ShadowRunWorker} fuera del camino de cualquier request. */
@Service
public class ShadowRunService {
  private final ShadowRunStore store;
  private final AuditRepository audit;
  private final int maxSampleSize;
  private final double defaultThreshold;

  public ShadowRunService(ShadowRunStore store, AuditRepository audit,
      @Value("${llm.shadow.max-sample-size:100}") int maxSampleSize,
      @Value("${llm.shadow.divergence-threshold:10}") double defaultThreshold) {
    this.store = store;
    this.audit = audit;
    this.maxSampleSize = maxSampleSize;
    this.defaultThreshold = defaultThreshold;
  }

  public record Command(UUID candidateRubricVersionId, ShadowRun.Source source, UUID goldenSetVersionId,
      Integer sampleSize, Double divergenceThreshold) {}

  @Transactional
  public ShadowRun enqueue(UUID courseId, Command command, UUID idempotencyKey, CallerIdentity actor) {
    if (command.candidateRubricVersionId() == null || command.source() == null) {
      throw new IllegalArgumentException("candidateRubricVersionId y source son obligatorios");
    }
    if (command.source() == ShadowRun.Source.GOLDEN_SET && command.goldenSetVersionId() == null) {
      throw new IllegalArgumentException("Con source GOLDEN_SET hay que indicar goldenSetVersionId");
    }
    if (command.source() != ShadowRun.Source.GOLDEN_SET && command.goldenSetVersionId() != null) {
      throw new IllegalArgumentException("goldenSetVersionId solo aplica a la fuente GOLDEN_SET");
    }
    int size = command.sampleSize() == null ? maxSampleSize : command.sampleSize();
    if (size < 1 || size > maxSampleSize) {
      throw new IllegalArgumentException("sampleSize debe estar entre 1 y " + maxSampleSize);
    }
    double threshold = command.divergenceThreshold() == null ? defaultThreshold : command.divergenceThreshold();
    if (threshold <= 0) {
      throw new IllegalArgumentException("divergenceThreshold debe ser mayor que 0");
    }
    UUID baseline = store.activeRubricVersion(courseId).orElseThrow(() -> new IllegalStateException(
        "El curso no tiene una calibración activa: no hay rúbrica baseline contra la que comparar"));
    if (baseline.equals(command.candidateRubricVersionId())) {
      throw new IllegalArgumentException("La rúbrica candidata es la misma que la activa del curso");
    }
    if (!store.rubricVersionVisibleToCourse(courseId, command.candidateRubricVersionId())) {
      throw new ResourceNotFoundException("La rúbrica candidata no existe en el curso");
    }
    if (command.source() == ShadowRun.Source.GOLDEN_SET
        && !store.goldenSetVersionVisibleToCourse(courseId, command.goldenSetVersionId())) {
      throw new ResourceNotFoundException("El golden set no existe en el curso");
    }
    var run = store.create(new ShadowRunStore.NewRun(courseId, baseline, command.candidateRubricVersionId(),
        command.source(), command.goldenSetVersionId(), size, threshold, idempotencyKey, actor.delegatedUserId()));
    audit.record("shadow.queued", "shadow-run", run.id(), actor,
        "{\"courseId\":\"" + courseId + "\",\"candidateRubricVersionId\":\"" + command.candidateRubricVersionId()
            + "\",\"source\":\"" + command.source() + "\"}");
    return run;
  }

  @Transactional(readOnly = true)
  public ShadowRun get(UUID courseId, UUID runId) {
    return store.find(courseId, runId)
        .orElseThrow(() -> new ResourceNotFoundException("La corrida de shadow no existe en el curso"));
  }

  @Transactional(readOnly = true)
  public List<ShadowRun> list(UUID courseId) {
    return store.list(courseId);
  }
}
