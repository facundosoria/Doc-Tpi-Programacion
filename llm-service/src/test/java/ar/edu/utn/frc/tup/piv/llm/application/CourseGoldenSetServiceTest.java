package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseGoldenSetServiceTest {
  @Test void createsAnIndependentCourseDraftFromAPublishedBase() {
    var repository = mock(CourseGoldenSetRepository.class); var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), base = UUID.randomUUID(), user = UUID.randomUUID();
    CourseGoldenSetVersion copy = new CourseGoldenSetVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "DRAFT", base);
    when(repository.copyPublishedPlatformVersion(course, base, user)).thenReturn(Optional.of(copy));

    assertThat(service.copyFromPublishedBase(course, base, new CallerIdentity("gateway", user, null, null))).isEqualTo(copy);
    verify(repository).copyPublishedPlatformVersion(course, base, user);
  }

  @Test void refusesAnUnpublishedOrNonPlatformSource() {
    var repository = mock(CourseGoldenSetRepository.class); var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), base = UUID.randomUUID(), user = UUID.randomUUID();
    when(repository.copyPublishedPlatformVersion(course, base, user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.copyFromPublishedBase(course, base, new CallerIdentity("gateway", user, null, null)))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("base publicado");
  }

  @Test void rejectsReferenceScoresOutsideTheInclusiveZeroToOneHundredRange() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class); var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var mapper = new ObjectMapper();
    var invalid = new CourseGoldenSetRepository.GoldenSetCaseInput(
        mapper.readTree("[]"), mapper.readTree("{}"), mapper.readTree("{}"), "Docente",
        mapper.readTree("{\"AUTONOMY\":101,\"CLARITY\":0,\"PROGRESSION\":0,\"COMPLIANCE\":0,\"EFFICIENCY\":0}"), mapper.readTree("{}"));

    assertThatThrownBy(() -> service.addCase(course, version, invalid))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Las cinco puntuaciones deben estar entre 0 y 100");
    verify(repository, org.mockito.Mockito.never()).addDraftCase(course, version, invalid);
  }

  @Test void acceptsTheZeroAndOneHundredScoreBoundaries() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class); var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var mapper = new ObjectMapper();
    var valid = new CourseGoldenSetRepository.GoldenSetCaseInput(
        mapper.readTree("[]"), mapper.readTree("{}"), mapper.readTree("{}"), "Docente",
        mapper.readTree("{\"AUTONOMY\":0,\"CLARITY\":100,\"PROGRESSION\":0,\"COMPLIANCE\":100,\"EFFICIENCY\":0}"), mapper.readTree("{}"));
    var created = new CourseGoldenSetRepository.GoldenSetCaseSummary(UUID.randomUUID(), 0, "Docente", "DRAFT");
    when(repository.addDraftCase(course, version, valid)).thenReturn(Optional.of(created));

    assertThat(service.addCase(course, version, valid)).isEqualTo(created);
  }

  @Test void publishesValidDraftAndRecordsAudit() {
    var repository = mock(CourseGoldenSetRepository.class);
    var audit = mock(ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository.class);
    var service = new CourseGoldenSetService(repository, audit);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    when(repository.publishDraft(course, version)).thenReturn(true);

    service.publish(course, version, actor);

    verify(repository).publishDraft(course, version);
    verify(audit).record(org.mockito.ArgumentMatchers.eq("golden_set.published"),
        org.mockito.ArgumentMatchers.eq("golden-set-version"),
        org.mockito.ArgumentMatchers.eq(version),
        org.mockito.ArgumentMatchers.eq(actor),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test void createsNextVersionFromPublishedVersion() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), user = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("gateway", user, null, null);
    CourseGoldenSetVersion next = new CourseGoldenSetVersion(UUID.randomUUID(), UUID.randomUUID(), 2, "DRAFT", version);
    when(repository.createNextDraft(course, version, user)).thenReturn(Optional.of(next));

    var result = service.createNextVersion(course, version, actor);

    assertThat(result).isEqualTo(next);
    verify(repository).createNextDraft(course, version, user);
  }

  @Test void logicallyDeletesOnlyWhenTheRepositoryConfirmsThereIsNoUse() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), user = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("gateway", user, null, null);
    when(repository.softDeleteUnusedDraft(course, version, user)).thenReturn(true);

    service.deleteUnusedDraft(course, version, actor);

    verify(repository).softDeleteUnusedDraft(course, version, user);
  }

  @Test void rejectsLogicalDeletionWhenAnyUseWasDetected() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), user = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("gateway", user, null, null);
    when(repository.softDeleteUnusedDraft(course, version, user)).thenReturn(false);

    assertThatThrownBy(() -> service.deleteUnusedDraft(course, version, actor))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("fue publicado, calibrado o está vinculado");
  }

  @Test void rejectsAddingCaseToPublishedVersionPreservingImmutability() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var mapper = new ObjectMapper();
    var valid = new CourseGoldenSetRepository.GoldenSetCaseInput(
        mapper.readTree("[]"), mapper.readTree("{}"), mapper.readTree("{}"), "Docente",
        mapper.readTree("{\"AUTONOMY\":80,\"CLARITY\":80,\"PROGRESSION\":80,\"COMPLIANCE\":80,\"EFFICIENCY\":80}"), mapper.readTree("{}"));
    when(repository.addDraftCase(course, version, valid)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.addCase(course, version, valid))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sólo puede crearse en un Golden Set borrador");
  }
}
