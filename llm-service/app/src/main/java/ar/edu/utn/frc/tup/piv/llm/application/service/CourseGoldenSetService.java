package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;

import ar.edu.utn.frc.tup.piv.llm.application.service.RealCaseAnonymizer;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetDetail;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetView;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseSummary;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetView;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseSummary;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetDetail;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseGoldenSetService {
  private final CourseGoldenSetRepository goldenSets;
  private final AuditRepository audit;
  private final CalibrationExpirationService expirations;

  @Autowired
  public CourseGoldenSetService(CourseGoldenSetRepository goldenSets, AuditRepository audit, CalibrationExpirationService expirations) {
    this.goldenSets = goldenSets;
    this.audit = audit;
    this.expirations = expirations;
  }

  public CourseGoldenSetService(CourseGoldenSetRepository goldenSets) {
    this(goldenSets, null, null);
  }

  @Transactional(readOnly = true)
  public List<CourseGoldenSetView> list(UUID courseId) { return goldenSets.list(courseId); }



  /** The copied family belongs exclusively to the course; subsequent edits cannot affect the platform base. */
  @Transactional
  public CourseGoldenSetVersion copyFromPublishedBase(UUID courseId, UUID baseVersionId, CallerIdentity actor) {
    try {
      return goldenSets.copyPublishedPlatformVersion(courseId, baseVersionId, actor.delegatedUserId())
          .orElseThrow(() -> new IllegalStateException("La versión base no existe o no está publicada"));
    } catch (DuplicateKeyException e) {
      throw new IllegalStateException("El curso ya posee un Golden Set", e);
    }
  }

  @Transactional
  public CourseGoldenSetVersion createDraft(UUID courseId, String name, CallerIdentity actor) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("El Golden Set debe tener un nombre");
    return goldenSets.createDraft(courseId, name.trim(), actor.delegatedUserId());
  }

  @Transactional
  public GoldenSetCaseSummary addCase(UUID courseId, UUID versionId, GoldenSetCaseInput input, CallerIdentity actor) {
    validateScores(input.referenceScores());
    if (goldenSets.countCases(versionId) >= 5) {
      throw new GoldenSetSizeException("Un Golden Set admite como máximo cinco casos");
    }
    GoldenSetCaseInput sanitized = new GoldenSetCaseInput(
        RealCaseAnonymizer.anonymize(input.transcript()),
        input.challengeContext(),
        input.metadata() != null ? RealCaseAnonymizer.anonymize(input.metadata()) : input.metadata(),
        actor == null ? input.author() : actor.delegatedUserId().toString(), input.referenceScores(), input.scoreJustifications());
    return goldenSets.addDraftCase(courseId, versionId, sanitized)
        .orElseThrow(() -> new IllegalStateException("El caso sólo puede crearse en un Golden Set borrador del curso"));
  }

  /** Compatibility entry point for existing application callers; HTTP requests always provide an actor. */
  @Transactional
  public GoldenSetCaseSummary addCase(UUID courseId, UUID versionId, GoldenSetCaseInput input) {
    return addCase(courseId, versionId, input, null);
  }

  @Transactional
  public void publish(UUID courseId, UUID versionId, CallerIdentity actor) {
    int cases = goldenSets.countCases(versionId);
    if (cases < 3 || cases > 5) {
      throw new GoldenSetSizeException("Para publicar el Golden Set necesitás entre tres y cinco casos");
    }
    var version = goldenSets.findDetail(courseId, versionId)
        .orElseThrow(() -> new ResourceNotFoundException("El Golden Set no existe"));
    if (!goldenSets.publishDraft(courseId, versionId)) {
      throw new IllegalStateException("El Golden Set no existe en el curso o ya no es un borrador");
    }
    if (expirations != null) {
      expirations.expireByGoldenSet(version.familyId());
    }
    if (audit != null) {
      audit.record("golden_set.published", "golden-set-version", versionId, actor,
          "{\"courseId\":\"" + courseId + "\"}");
    }
  }

  @Transactional(readOnly = true)
  public GoldenSetDetail get(UUID courseId, UUID versionId) {
    return goldenSets.findDetail(courseId, versionId)
        .orElseThrow(() -> new ResourceNotFoundException("El Golden Set no existe en el curso"));
  }

  @Transactional
  public GoldenSetCaseSummary updateCase(UUID courseId, UUID versionId, UUID caseId, GoldenSetCaseInput input,
      CallerIdentity actor) {
    validateScores(input.referenceScores());
    GoldenSetCaseInput sanitized = new GoldenSetCaseInput(
        RealCaseAnonymizer.anonymize(input.transcript()), input.challengeContext(),
        input.metadata() != null ? RealCaseAnonymizer.anonymize(input.metadata()) : input.metadata(),
        actor.delegatedUserId().toString(), input.referenceScores(), input.scoreJustifications());
    return goldenSets.updateDraftCase(courseId, versionId, caseId, sanitized)
        .orElseThrow(() -> new IllegalStateException("El caso sólo puede editarse en un Golden Set borrador del curso"));
  }

  @Transactional
  public CourseGoldenSetVersion createNextVersion(UUID courseId, UUID publishedVersionId, CallerIdentity actor) {
    return goldenSets.createNextDraft(courseId, publishedVersionId, actor.delegatedUserId())
        .orElseThrow(() -> new IllegalStateException("Solo una versión publicada del curso puede originar una nueva versión"));
  }

  @Transactional
  public void deleteUnusedDraft(UUID courseId, UUID versionId, CallerIdentity actor) {
    if (!goldenSets.softDeleteUnusedDraft(courseId, versionId, actor.delegatedUserId())) {
      throw new IllegalStateException("No se puede eliminar: el Golden Set fue publicado, calibrado o está vinculado a un desafío, examen o intento.");
    }
    if (audit != null) audit.record("golden_set.deleted", "golden-set-version", versionId, actor, "{\"courseId\":\"" + courseId + "\"}");
  }

  private void validateScores(JsonNode scores) {
    for (String key : List.of("AUTONOMY", "CLARITY", "PROGRESSION", "COMPLIANCE", "EFFICIENCY")) {
      if (!scores.path(key).canConvertToInt() || scores.path(key).asInt() < 0 || scores.path(key).asInt() > 100) {
        throw new IllegalArgumentException("Las cinco puntuaciones deben estar entre 0 y 100");
      }
    }
  }

  public static class GoldenSetSizeException extends RuntimeException {
    public GoldenSetSizeException(String message) { super(message); }
  }
}
