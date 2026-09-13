package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RubricPublicationService {
  private final RubricVersionRepository rubrics;
  private final AuditRepository audit;

  public RubricPublicationService(RubricVersionRepository rubrics, AuditRepository audit) {
    this.rubrics = rubrics;
    this.audit = audit;
  }

  /** Publishes only a complete valid draft. Published versions are immutable in the database. */
  @Transactional
  public void publish(UUID courseId, UUID versionId, CallerIdentity actor) {
    var dimensions = rubrics.dimensionsOfDraft(courseId, versionId);
    if (dimensions.isEmpty()) {
      throw new IllegalStateException("La rúbrica no existe en el curso o ya no es un borrador");
    }
    RubricValidator.validateForPublication(dimensions);
    validateAnchors(rubrics.find(courseId, versionId)
        .orElseThrow(() -> new IllegalStateException("La rúbrica no existe en el curso")).dimensions());
    if (!rubrics.publishDraft(courseId, versionId)) {
      throw new IllegalStateException("La rúbrica fue modificada mientras se publicaba");
    }
    audit.record("rubric.published", "rubric-version", versionId, actor,
        "{\"courseId\":\"" + courseId + "\"}");
  }

  static void validateAnchors(List<RubricDraftService.DimensionInput> dimensions) {
    for (var dimension : dimensions) {
      var anchors = dimension.anchors();
      if (anchors == null || !valid(anchors.low()) || !valid(anchors.medium()) || !valid(anchors.high())) {
        throw new IllegalArgumentException("Cada dimensión debe definir anclas baja, media y alta completas");
      }
      if (!(anchors.low().referenceScore() < anchors.medium().referenceScore()
          && anchors.medium().referenceScore() < anchors.high().referenceScore())) {
        throw new IllegalArgumentException("Los puntajes de ancla deben ser crecientes: bajo, medio y alto");
      }
    }
  }

  private static boolean valid(RubricDraftService.Anchor anchor) {
    return anchor != null && anchor.behavior() != null && !anchor.behavior().isBlank()
        && anchor.example() != null && !anchor.example().isBlank()
        && anchor.referenceScore() != null && anchor.referenceScore() >= 0 && anchor.referenceScore() <= 100;
  }
}
