package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseDetail;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseSummary;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetDetail;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseGoldenSetServiceCoverageTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);

  @Test void wrapsDuplicateKeyAsExistingGoldenSetError() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), base = UUID.randomUUID();
    when(repository.copyPublishedPlatformVersion(course, base, actor.delegatedUserId())).thenThrow(new DuplicateKeyException("dup"));
    assertThatThrownBy(() -> service.copyFromPublishedBase(course, base, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("El curso ya posee un Golden Set");
  }

  @Test void copiesPublishedBaseWithDelegatedUser() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), base = UUID.randomUUID(), id = UUID.randomUUID(), family = UUID.randomUUID();
    var copy = new CourseGoldenSetVersion(id, family, 1, "DRAFT", base);
    when(repository.copyPublishedPlatformVersion(course, base, actor.delegatedUserId())).thenReturn(Optional.of(copy));
    assertThat(service.copyFromPublishedBase(course, base, actor)).isEqualTo(copy);
  }

  @Test void rejectsCopyingAProtectedOrMissingBase() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), base = UUID.randomUUID();
    when(repository.copyPublishedPlatformVersion(course, base, actor.delegatedUserId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.copyFromPublishedBase(course, base, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("La versión base no existe o no está publicada");
  }

  @Test void addsCaseWithoutActorKeepsOriginalAuthor() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var created = new GoldenSetCaseSummary(UUID.randomUUID(), 0, "Docente", "DRAFT");
    when(repository.countCases(version)).thenReturn(1);
    when(repository.addDraftCase(eq(course), eq(version), any(GoldenSetCaseInput.class))).thenReturn(Optional.of(created));
    assertThat(service.addCase(course, version, validInput())).isEqualTo(created);
    var captor = org.mockito.ArgumentCaptor.forClass(GoldenSetCaseInput.class);
    verify(repository).addDraftCase(eq(course), eq(version), captor.capture());
    assertThat(captor.getValue().author()).isEqualTo("Docente");
  }

  @Test void rejectsBlankDraftNames() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID();
    assertThatThrownBy(() -> service.createDraft(course, "  ", actor)).isInstanceOf(IllegalArgumentException.class).hasMessage("El Golden Set debe tener un nombre");
    assertThatThrownBy(() -> service.createDraft(course, null, actor)).isInstanceOf(IllegalArgumentException.class);
    verify(repository, never()).createDraft(any(), any(), any());
  }

  @Test void createsDraftWithTrimmedName() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), id = UUID.randomUUID(), family = UUID.randomUUID();
    var created = new CourseGoldenSetVersion(id, family, 1, "DRAFT", null);
    when(repository.createDraft(course, "Golden", actor.delegatedUserId())).thenReturn(created);
    assertThat(service.createDraft(course, " Golden ", actor)).isEqualTo(created);
  }

  @Test void rejectsMoreThanFiveCases() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.countCases(version)).thenReturn(5);
    assertThatThrownBy(() -> service.addCase(course, version, validInput(), actor))
        .isInstanceOf(CourseGoldenSetService.GoldenSetSizeException.class).hasMessage("Un Golden Set admite como máximo cinco casos");
    verify(repository, never()).addDraftCase(any(), any(), any());
  }

  @Test void overwritesAuthorWithDelegatedUserAndAnonymizesMetadata() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var input = new GoldenSetCaseInput(mapper.readTree("[]"), mapper.createObjectNode(),
        mapper.createObjectNode().put("studentId", "s-1"), "Docente",
        mapper.readTree("{\"AUTONOMY\":80,\"CLARITY\":80,\"PROGRESSION\":80,\"COMPLIANCE\":80,\"EFFICIENCY\":80}"),
        mapper.createObjectNode());
    var created = new GoldenSetCaseSummary(UUID.randomUUID(), 0, actor.delegatedUserId().toString(), "DRAFT");
    when(repository.countCases(version)).thenReturn(0);
    when(repository.addDraftCase(eq(course), eq(version), any(GoldenSetCaseInput.class))).thenReturn(Optional.of(created));
    assertThat(service.addCase(course, version, input, actor)).isEqualTo(created);
    var captor = org.mockito.ArgumentCaptor.forClass(GoldenSetCaseInput.class);
    verify(repository).addDraftCase(eq(course), eq(version), captor.capture());
    assertThat(captor.getValue().author()).isEqualTo(actor.delegatedUserId().toString());
    assertThat(captor.getValue().metadata().has("studentId")).isFalse();
  }

  @Test void rejectsPublishWithWrongCaseCounts() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.countCases(version)).thenReturn(2);
    assertThatThrownBy(() -> service.publish(course, version, actor))
        .isInstanceOf(CourseGoldenSetService.GoldenSetSizeException.class).hasMessage("Para publicar el Golden Set necesitás entre tres y cinco casos");
    when(repository.countCases(version)).thenReturn(6);
    assertThatThrownBy(() -> service.publish(course, version, actor))
        .isInstanceOf(CourseGoldenSetService.GoldenSetSizeException.class);
  }

  @Test void rejectsPublishWhenDetailIsMissing() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.countCases(version)).thenReturn(3);
    when(repository.findDetail(course, version)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.publish(course, version, actor)).isInstanceOf(ResourceNotFoundException.class).hasMessage("El Golden Set no existe");
  }

  @Test void publishesWithAuditAndExpirationCollaborators() {
    var repository = mock(CourseGoldenSetRepository.class);
    var audit = mock(ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository.class);
    var expirations = mock(CalibrationExpirationService.class);
    var service = new CourseGoldenSetService(repository, audit, expirations);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), family = UUID.randomUUID();
    var detail = new GoldenSetDetail(version, family, "name", 1, "DRAFT", null, List.of());
    when(repository.countCases(version)).thenReturn(3);
    when(repository.findDetail(course, version)).thenReturn(Optional.of(detail));
    when(repository.publishDraft(course, version)).thenReturn(true);
    service.publish(course, version, actor);
    verify(expirations).expireByGoldenSet(family);
    verify(audit).record(eq("golden_set.published"), eq("golden-set-version"), eq(version), eq(actor), any());
  }

  @Test void rejectsPublishWhenDraftNoLongerExists() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var detail = new GoldenSetDetail(version, UUID.randomUUID(), "name", 1, "DRAFT", null, List.of());
    when(repository.countCases(version)).thenReturn(3);
    when(repository.findDetail(course, version)).thenReturn(Optional.of(detail));
    when(repository.publishDraft(course, version)).thenReturn(false);
    assertThatThrownBy(() -> service.publish(course, version, actor)).isInstanceOf(IllegalStateException.class).hasMessage("El Golden Set no existe en el curso o ya no es un borrador");
  }

  @Test void publishesWithoutExpirationOrAuditCollaborators() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var detail = new GoldenSetDetail(version, UUID.randomUUID(), "name", 1, "DRAFT", null, List.of());
    when(repository.countCases(version)).thenReturn(3);
    when(repository.findDetail(course, version)).thenReturn(Optional.of(detail));
    when(repository.publishDraft(course, version)).thenReturn(true);
    service.publish(course, version, actor);
    verify(repository).publishDraft(course, version);
  }

  @Test void returnsExistingDetailAndRejectsMissingOne() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var detail = new GoldenSetDetail(version, UUID.randomUUID(), "name", 1, "DRAFT", null, List.of());
    when(repository.findDetail(course, version)).thenReturn(Optional.of(detail));
    assertThat(service.get(course, version)).isEqualTo(detail);
    when(repository.findDetail(course, version)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(course, version)).isInstanceOf(ResourceNotFoundException.class).hasMessage("El Golden Set no existe en el curso");
  }

  @Test void updatesDraftCaseWithDelegatedAuthor() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), caseId = UUID.randomUUID();
    var input = validInput();
    var updated = new GoldenSetCaseSummary(caseId, 1, actor.delegatedUserId().toString(), "DRAFT");
    when(repository.updateDraftCase(eq(course), eq(version), eq(caseId), any(GoldenSetCaseInput.class))).thenReturn(Optional.of(updated));
    assertThat(service.updateCase(course, version, caseId, input, actor)).isEqualTo(updated);
    var captor = org.mockito.ArgumentCaptor.forClass(GoldenSetCaseInput.class);
    verify(repository).updateDraftCase(eq(course), eq(version), eq(caseId), captor.capture());
    assertThat(captor.getValue().author()).isEqualTo(actor.delegatedUserId().toString());
  }

  @Test void rejectsInvalidScoresOnUpdate() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    var invalid = new GoldenSetCaseInput(mapper.readTree("[]"), mapper.createObjectNode(), mapper.createObjectNode(), "Docente",
        mapper.readTree("{\"AUTONOMY\":\"alto\",\"CLARITY\":80,\"PROGRESSION\":80,\"COMPLIANCE\":80,\"EFFICIENCY\":80}"), mapper.createObjectNode());
    assertThatThrownBy(() -> service.updateCase(course, version, UUID.randomUUID(), invalid, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Las cinco puntuaciones deben estar entre 0 y 100");
    verify(repository, never()).updateDraftCase(any(), any(), any(), any());
  }

  @Test void rejectsUpdatingNonDraftCase() throws Exception {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.updateDraftCase(course, version, version, validInput())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.updateCase(course, version, version, validInput(), actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("El caso sólo puede editarse en un Golden Set borrador del curso");
  }

  @Test void rejectsNextVersionFromNonPublishedSource() {
    var repository = mock(CourseGoldenSetRepository.class);
    var service = new CourseGoldenSetService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.createNextDraft(course, version, actor.delegatedUserId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.createNextVersion(course, version, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("Solo una versión publicada del curso puede originar una nueva versión");
  }

  @Test void recordsDeletionAuditWhenAuditIsConfigured() {
    var repository = mock(CourseGoldenSetRepository.class);
    var audit = mock(ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository.class);
    var service = new CourseGoldenSetService(repository, audit, mock(CalibrationExpirationService.class));
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.softDeleteUnusedDraft(course, version, actor.delegatedUserId())).thenReturn(true);
    service.deleteUnusedDraft(course, version, actor);
    verify(audit).record(eq("golden_set.deleted"), eq("golden-set-version"), eq(version), eq(actor), any());
  }

  @Test void goldenSetSizeExceptionCarriesItsMessage() {
    assertThat(new CourseGoldenSetService.GoldenSetSizeException("por tamaño").getMessage()).isEqualTo("por tamaño");
  }

  @Test void instantiateCaseDetailRecord() throws Exception {
    var detail = new GoldenSetCaseDetail(UUID.randomUUID(), mapper.readTree("[]"), mapper.readTree("{}"),
        mapper.readTree("{}"));
    assertThat(detail.transcript().isArray()).isTrue();
  }

  private GoldenSetCaseInput validInput() throws Exception {
    return new GoldenSetCaseInput(mapper.readTree("[]"), mapper.createObjectNode(), mapper.createObjectNode(), "Docente",
        mapper.readTree("{\"AUTONOMY\":80,\"CLARITY\":80,\"PROGRESSION\":80,\"COMPLIANCE\":80,\"EFFICIENCY\":80}"), mapper.createObjectNode());
  }
}