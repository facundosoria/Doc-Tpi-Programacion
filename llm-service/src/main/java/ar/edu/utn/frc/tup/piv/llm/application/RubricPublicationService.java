package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.UUID;
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
    if (!rubrics.publishDraft(courseId, versionId)) {
      throw new IllegalStateException("La rúbrica fue modificada mientras se publicaba");
    }
    audit.record("rubric.published", "rubric-version", versionId, actor,
        "{\"courseId\":\"" + courseId + "\"}");
  }
}
