package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable rubric drafts. Every autosave uses the version revision as an optimistic lock. */
@Service
public class RubricDraftService {
  private final RubricVersionRepository rubrics;

  public RubricDraftService(RubricVersionRepository rubrics) { this.rubrics = rubrics; }

  @Transactional(readOnly = true)
  public List<RubricVersion> list(UUID courseId) { return rubrics.list(courseId); }

  @Transactional(readOnly = true)
  public RubricVersion get(UUID courseId, UUID versionId) { return rubrics.find(courseId, versionId)
      .orElseThrow(() -> new IllegalStateException("La rúbrica no existe en el curso")); }

  @Transactional
  public RubricVersion createFromTemplate(UUID courseId, UUID templateVersionId, String name, CallerIdentity actor) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre de la rúbrica es obligatorio");
    return rubrics.createDraftFromPublishedTemplate(courseId, templateVersionId, name.trim(), actor.delegatedUserId())
        .orElseThrow(() -> new IllegalArgumentException("La plantilla institucional publicada no existe"));
  }

  @Transactional
  public RubricVersion createNextVersion(UUID courseId, UUID publishedVersionId, CallerIdentity actor) {
    return rubrics.createNextDraft(courseId, publishedVersionId, actor.delegatedUserId())
        .orElseThrow(() -> new IllegalStateException("Solo una versión publicada del curso puede originar una nueva versión"));
  }

  @Transactional
  public RubricVersion autosave(UUID courseId, UUID versionId, long expectedRevision, RubricInput input, CallerIdentity actor) {
    validate(input);
    if (!rubrics.advanceRevision(courseId, versionId, expectedRevision)) {
      throw new OptimisticLockException("El borrador fue actualizado en otro dispositivo; recargá antes de guardar");
    }
    rubrics.updateVersionName(courseId, versionId, input.name().trim());
    rubrics.replaceDimensions(versionId, input.dimensions());
    return get(courseId, versionId);
  }

  private void validate(RubricInput input) {
    if (input == null || input.name() == null || input.name().isBlank() || input.dimensions() == null || input.dimensions().size() != 5
        || input.dimensions().stream().map(DimensionInput::key).distinct().count() != 5) {
      throw new IllegalArgumentException("El borrador debe incluir nombre y exactamente las cinco dimensiones");
    }
  }

  public record RubricInput(String name, List<DimensionInput> dimensions) {}
  public record Anchor(String behavior, Integer referenceScore, String example) {}
  public record Anchors(Anchor low, Anchor medium, Anchor high) {}
  public record DimensionInput(Dimension key, String label, String criterion, Anchors anchors, BigDecimal weight) {}
  public record RubricVersion(UUID id, UUID familyId, int version, String name, String state, long revision,
      UUID templateOriginVersionId, List<DimensionInput> dimensions) {}
  public static class OptimisticLockException extends RuntimeException { public OptimisticLockException(String message) { super(message); } }
}
