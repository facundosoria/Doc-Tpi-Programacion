package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository.Profile;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository.Run;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository.StabilityGroup;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.CalibrationMigrationPreview;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ActiveCalibration;
import ar.edu.utn.frc.tup.piv.llm.application.service.CourseEvaluationStatusService;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetView;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseSummary;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetDetail;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Deployment;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationActivationPreviewService;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationActivationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationMigrationConfirmation;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationRunService;
import ar.edu.utn.frc.tup.piv.llm.application.service.CourseGoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricPublicationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricTemplateService;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CourseGoldenSetController.CreateGoldenSetDraft;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CalibrationRunController.Request;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CalibrationActivationController.ActivationInput;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.InstitutionalCalibrationController.ProfileRequest;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CourseGoldenSetController.GoldenSetPage;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.RubricController.CreateFromTemplateRequest;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.RubricController.RubricPage;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.RubricTemplateController.TemplatePage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class AdminControllersCoverageTest {

  private static CallerIdentity actor() {
    return new CallerIdentity("lms", UUID.randomUUID(), "req-1", null);
  }

  private static HttpHeaders headers() {
    return new HttpHeaders();
  }

  private static RubricVersion version() {
    return new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "Rúbrica", "DRAFT", 1L, null, List.of());
  }

  private static Run run() {
    return new Run(UUID.randomUUID(), "RUNNING", 0);
  }

  // ---------------------------------------------------------------------------
  // CourseGoldenSetController
  // ---------------------------------------------------------------------------

  @Test
  void courseGoldenSetCopyFromBaseReturnsCreatedWithUri() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    UUID course = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    when(service.copyFromPublishedBase(any(UUID.class), any(UUID.class), any(CallerIdentity.class)))
        .thenReturn(new CourseGoldenSetVersion(versionId, UUID.randomUUID(), 1, "DRAFT", null));

    var response = controller.copyFromBase(course, UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().id()).isEqualTo(versionId);
    assertThat(response.getHeaders().getLocation().toString())
        .contains("/courses/" + course + "/golden-sets/" + versionId);
  }

  @Test
  void courseGoldenSetListReturnsThePage() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    when(service.list(any(UUID.class)))
        .thenReturn(List.of(new CourseGoldenSetView(UUID.randomUUID(), UUID.randomUUID(), "Golden", 1, "DRAFT",
            null, List.of())));

    GoldenSetPage page = controller.list(UUID.randomUUID(), headers());

    assertThat(page.items()).hasSize(1);
  }

  @Test
  void courseGoldenSetCreateReturnsCreated() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    UUID versionId = UUID.randomUUID();
    when(service.createDraft(any(UUID.class), any(String.class), any(CallerIdentity.class)))
        .thenReturn(new CourseGoldenSetVersion(versionId, UUID.randomUUID(), 1, "DRAFT", null));

    var response = controller.create(UUID.randomUUID(), new CreateGoldenSetDraft("Borrador"), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().id()).isEqualTo(versionId);
  }

  @Test
  void courseGoldenSetAddCaseReturnsCreated() throws Exception {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    UUID caseId = UUID.randomUUID();
    when(service.addCase(any(UUID.class), any(UUID.class), any(GoldenSetCaseInput.class),
        any(CallerIdentity.class)))
        .thenReturn(new GoldenSetCaseSummary(caseId, 3, "alumno", "DRAFT"));

    var response = controller.addCase(UUID.randomUUID(), UUID.randomUUID(), caseInput(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().id()).isEqualTo(caseId);
  }

  @Test
  void courseGoldenSetGetReturnsTheDetail() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    when(service.get(any(UUID.class), any(UUID.class)))
        .thenReturn(new GoldenSetDetail(UUID.randomUUID(), UUID.randomUUID(), "Golden", 1, "DRAFT", null, List.of()));

    GoldenSetDetail detail = controller.get(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(detail.name()).isEqualTo("Golden");
  }

  @Test
  void courseGoldenSetUpdateCaseReturnsTheSummary() throws Exception {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    UUID caseId = UUID.randomUUID();
    when(service.updateCase(any(UUID.class), any(UUID.class), any(UUID.class), any(GoldenSetCaseInput.class),
        any(CallerIdentity.class)))
        .thenReturn(new GoldenSetCaseSummary(caseId, 2, "alumno", "DRAFT"));

    var summary = controller.updateCase(UUID.randomUUID(), UUID.randomUUID(), caseId, caseInput(), headers());

    assertThat(summary.id()).isEqualTo(caseId);
    assertThat(summary.order()).isEqualTo(2);
  }

  @Test
  void courseGoldenSetPublishReturnsOk() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());

    var response = controller.publish(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void courseGoldenSetCreateNextVersionReturnsCreated() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());
    UUID versionId = UUID.randomUUID();
    when(service.createNextVersion(any(UUID.class), any(UUID.class), any(CallerIdentity.class)))
        .thenReturn(new CourseGoldenSetVersion(versionId, UUID.randomUUID(), 2, "DRAFT", null));

    var response = controller.createNextVersion(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().version()).isEqualTo(2);
  }

  @Test
  void courseGoldenSetDeleteUnusedDraftReturnsNoContent() {
    var service = mock(CourseGoldenSetService.class);
    var controller = new CourseGoldenSetController(service, auth(), courseAuth());

    var response = controller.deleteUnusedDraft(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  // ---------------------------------------------------------------------------
  // InstitutionalCalibrationController
  // ---------------------------------------------------------------------------

  @Test
  void institutionalProfileReturnsItWhenConfigured() {
    var runs = mock(CalibrationRunRepository.class);
    var controller = new InstitutionalCalibrationController(runs, mock(ProviderCredentialRepository.class),
        auth());
    when(runs.profile()).thenReturn(Optional.of(new Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));

    Profile profile = controller.profile(headers());

    assertThat(profile.goldenSetVersionId()).isNotNull();
  }

  @Test
  void institutionalProfileThrowsWhenMissing() {
    var runs = mock(CalibrationRunRepository.class);
    var controller = new InstitutionalCalibrationController(runs, mock(ProviderCredentialRepository.class),
        auth());
    when(runs.profile()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.profile(headers()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No hay perfil institucional");
  }

  @Test
  void institutionalConfigurePersistsTheProfile() {
    var runs = mock(CalibrationRunRepository.class);
    var controller = new InstitutionalCalibrationController(runs, mock(ProviderCredentialRepository.class),
        auth());
    UUID golden = UUID.randomUUID();
    UUID rubric = UUID.randomUUID();

    controller.configure(new ProfileRequest(golden, rubric), headers());
  }

  @Test
  void institutionalListReturnsThePlatformRuns() {
    var runs = mock(CalibrationRunRepository.class);
    var controller = new InstitutionalCalibrationController(runs, mock(ProviderCredentialRepository.class),
        auth());
    when(runs.listPlatform()).thenReturn(List.of(run()));

    var page = controller.list(headers());

    assertThat(page.items()).hasSize(1);
  }

  @Test
  void institutionalCreateReturnsAccepted() {
    var runs = mock(CalibrationRunRepository.class);
    var deployments = mock(ProviderCredentialRepository.class);
    var controller = new InstitutionalCalibrationController(runs, deployments, auth());
    when(runs.profile()).thenReturn(Optional.of(new Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));
    when(deployments.calibrationTarget()).thenReturn(Optional.of(deployment()));
    when(runs.createPlatform(any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(run());

    var response = controller.create(headers(), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }

  @Test
  void institutionalCreateThrowsWithoutConfiguredProfile() {
    var runs = mock(CalibrationRunRepository.class);
    var controller = new InstitutionalCalibrationController(runs, mock(ProviderCredentialRepository.class),
        auth());
    when(runs.profile()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.create(headers(), UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Configure el perfil institucional");
  }

  @Test
  void institutionalCreateThrowsWithoutCandidateModel() {
    var runs = mock(CalibrationRunRepository.class);
    var deployments = mock(ProviderCredentialRepository.class);
    var controller = new InstitutionalCalibrationController(runs, deployments, auth());
    when(runs.profile()).thenReturn(Optional.of(new Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));
    when(deployments.calibrationTarget()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.create(headers(), UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("modelo candidato");
  }

  @Test
  void institutionalGetReturnsRunDetail() {
    var runs = mock(CalibrationRunRepository.class);
    var controller = new InstitutionalCalibrationController(runs, mock(ProviderCredentialRepository.class),
        auth());
    UUID runId = UUID.randomUUID();
    var theRun = run();
    when(runs.byId(runId)).thenReturn(Optional.of(new CalibrationRunRepository.Run(runId, theRun.courseId(), "PLATFORM", theRun.state(), theRun.progress(), theRun.rubricVersionId(), theRun.goldenSetVersionId(), theRun.modelDeploymentId(), theRun.maeFinal(), theRun.maxIndividualError(), theRun.reason(), theRun.createdAt(), theRun.finishedAt(), theRun.failureCode(), theRun.failureDetail(), theRun.expirationReason())));
    when(runs.dimensionErrors(runId)).thenReturn(Map.of());

    var detail = controller.get(runId, headers());

    assertThat(detail.run().id()).isEqualTo(runId);
  }

  // ---------------------------------------------------------------------------
  // ProviderCredentialController
  // ---------------------------------------------------------------------------

  @Test
  void rubricTemplateListAndGet() {
    var templates = mock(RubricTemplateService.class);
    var controller = new RubricTemplateController(templates, auth());
    when(templates.list()).thenReturn(List.of(version()));
    when(templates.get(any(UUID.class))).thenReturn(version());

    TemplatePage page = controller.list(headers());
    RubricVersion fetched = controller.get(UUID.randomUUID(), headers());

    assertThat(page.items()).hasSize(1);
    assertThat(fetched).isNotNull();
  }

  @Test
  void rubricTemplateCreateReturnsCreated() {
    var templates = mock(RubricTemplateService.class);
    var controller = new RubricTemplateController(templates, auth());
    when(templates.create(any(RubricInput.class), any(CallerIdentity.class))).thenReturn(version());

    var response = controller.create(new RubricInput("Plantilla", List.of()), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void rubricTemplateUpdateAndPublish() {
    var templates = mock(RubricTemplateService.class);
    var controller = new RubricTemplateController(templates, auth());
    when(templates.update(any(UUID.class), any(Long.class), any(RubricInput.class))).thenReturn(version());

    RubricVersion updated = controller.update(UUID.randomUUID(), 1L, new RubricInput("Plantilla", List.of()), headers());
    controller.publish(UUID.randomUUID(), headers());

    assertThat(updated).isNotNull();
  }

  @Test
  void rubricTemplateNextReturnsCreated() {
    var templates = mock(RubricTemplateService.class);
    var controller = new RubricTemplateController(templates, auth());
    when(templates.next(any(UUID.class), any(CallerIdentity.class))).thenReturn(version());

    var response = controller.next(UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  // ---------------------------------------------------------------------------
  // RubricController
  // ---------------------------------------------------------------------------

  @Test
  void rubricListAndGet() {
    var drafts = mock(RubricDraftService.class);
    var controller = new RubricController(mock(RubricPublicationService.class), drafts, auth(), courseAuth());
    when(drafts.list(any(UUID.class))).thenReturn(List.of(version()));
    when(drafts.get(any(UUID.class), any(UUID.class))).thenReturn(version());

    RubricPage page = controller.list(UUID.randomUUID(), headers());
    RubricVersion fetched = controller.get(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(page.items()).hasSize(1);
    assertThat(fetched).isNotNull();
  }

  @Test
  void rubricCreateFromTemplateReturnsCreated() {
    var drafts = mock(RubricDraftService.class);
    var controller = new RubricController(mock(RubricPublicationService.class), drafts, auth(), courseAuth());
    when(drafts.createFromTemplate(any(UUID.class), any(UUID.class), any(String.class), any(CallerIdentity.class)))
        .thenReturn(version());

    var response = controller.create(UUID.randomUUID(),
        new CreateFromTemplateRequest(UUID.randomUUID(), "Rúbrica"), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void rubricAutosaveReturnsTheSavedVersion() {
    var drafts = mock(RubricDraftService.class);
    var controller = new RubricController(mock(RubricPublicationService.class), drafts, auth(), courseAuth());
    when(drafts.autosave(any(UUID.class), any(UUID.class), any(Long.class), any(RubricInput.class),
        any(CallerIdentity.class)))
        .thenReturn(version());

    RubricVersion saved = controller.autosave(UUID.randomUUID(), UUID.randomUUID(), 2L,
        new RubricInput("Rúbrica", List.of()), headers());

    assertThat(saved).isNotNull();
  }

  @Test
  void rubricCreateNextVersionAndPublish() {
    var drafts = mock(RubricDraftService.class);
    var publications = mock(RubricPublicationService.class);
    var controller = new RubricController(publications, drafts, auth(), courseAuth());
    when(drafts.createNextVersion(any(UUID.class), any(UUID.class), any(CallerIdentity.class)))
        .thenReturn(version());

    var response = controller.createNextVersion(UUID.randomUUID(), UUID.randomUUID(), headers());
    var published = controller.publish(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(published.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  // ---------------------------------------------------------------------------
  // CalibrationActivationController
  // ---------------------------------------------------------------------------

  @Test
  void activationPreviewReturnsTokenAndLists() {
    var previews = mock(CalibrationActivationPreviewService.class);
    var confirmations = mock(CalibrationMigrationConfirmation.class);
    var controller = new CalibrationActivationController(previews, confirmations,
        mock(CalibrationActivationService.class), mock(CourseEvaluationStatusService.class), auth(), courseAuth());
    UUID migrable = UUID.randomUUID();
    UUID locked = UUID.randomUUID();
    when(previews.preview(any(UUID.class), any(UUID.class)))
        .thenReturn(new CalibrationMigrationPreview(List.of(migrable), List.of(locked)));
    when(confirmations.issue(any(UUID.class), any(UUID.class), any(Set.class))).thenReturn("tok-abc");

    var preview = controller.preview(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(preview.previewToken()).isEqualTo("tok-abc");
    assertThat(preview.migrableChallengeIds()).containsExactly(migrable);
    assertThat(preview.lockedChallengeIds()).containsExactly(locked);
  }

  @Test
  void activationActivateReturnsTheActiveCalibration() {
    var activation = mock(CalibrationActivationService.class);
    var status = mock(CourseEvaluationStatusService.class);
    var controller = new CalibrationActivationController(mock(CalibrationActivationPreviewService.class),
        mock(CalibrationMigrationConfirmation.class), activation, status, auth(), courseAuth());
    UUID course = UUID.randomUUID();
    UUID runId = UUID.randomUUID();
    when(status.activeCalibration(any(UUID.class))).thenReturn(Optional.of(
        new ActiveCalibration(course, runId, OffsetDateTime.now())));

    var response = controller.activate(course, runId,
        new ActivationInput("tok-abc", Set.of(UUID.randomUUID())), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().calibrationRunId()).isEqualTo(runId);
  }

  @Test
  void activationActivateThrowsWhenTheCalibrationCannotBeRecovered() {
    var status = mock(CourseEvaluationStatusService.class);
    var controller = new CalibrationActivationController(mock(CalibrationActivationPreviewService.class),
        mock(CalibrationMigrationConfirmation.class), mock(CalibrationActivationService.class), status, auth(),
        courseAuth());
    when(status.activeCalibration(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.activate(UUID.randomUUID(), UUID.randomUUID(),
        new ActivationInput("tok-abc", Set.of()), headers()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No se pudo recuperar la calibración activada");
  }

  // ---------------------------------------------------------------------------
  // RubricTemplateCatalogController
  // ---------------------------------------------------------------------------

  @Test
  void catalogListsOnlyPublishedTemplates() {
    var templates = mock(RubricTemplateService.class);
    var controller = new RubricTemplateCatalogController(templates, auth());
    when(templates.list()).thenReturn(List.of(
        new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "Plantilla A", "PUBLISHED", 1L, null, List.of()),
        new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "Plantilla B", "DRAFT", 1L, null, List.of())));

    var page = controller.list(headers());

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).name()).isEqualTo("Plantilla A");
  }

  // ---------------------------------------------------------------------------
  // CalibrationRunController
  // ---------------------------------------------------------------------------

  @Test
  void calibrationRunsListGroupsAndGet() {
    var service = mock(CalibrationRunService.class);
    var controller = new CalibrationRunController(service, auth(), courseAuth(),
        mock(ProviderCredentialRepository.class));
    when(service.list(any(UUID.class))).thenReturn(List.of(run()));
    when(service.stabilityGroups(any(UUID.class))).thenReturn(List.of(
        new StabilityGroup(UUID.randomUUID(), "STABLE", BigDecimal.ONE, Instant.now(), Instant.now(),
            List.of(run()))));
    when(service.get(any(UUID.class), any(UUID.class))).thenReturn(run());

    var page = controller.list(UUID.randomUUID(), headers());
    var groups = controller.groups(UUID.randomUUID(), headers());
    Run fetched = controller.get(UUID.randomUUID(), UUID.randomUUID(), headers());

    assertThat(page.items()).hasSize(1);
    assertThat(groups.items()).hasSize(1);
    assertThat(fetched).isNotNull();
  }

  @Test
  void calibrationRunCreateReturnsAccepted() {
    var service = mock(CalibrationRunService.class);
    var deployments = mock(ProviderCredentialRepository.class);
    var controller = new CalibrationRunController(service, auth(), courseAuth(), deployments);
    when(deployments.calibrationTarget()).thenReturn(Optional.of(deployment()));
    when(service.enqueue(any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(CallerIdentity.class)))
        .thenReturn(run());

    var response = controller.create(UUID.randomUUID(),
        new Request(UUID.randomUUID(), UUID.randomUUID()), UUID.randomUUID(), headers());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }

  @Test
  void calibrationRunCreateThrowsWithoutCandidateModel() {
    var deployments = mock(ProviderCredentialRepository.class);
    var controller = new CalibrationRunController(mock(CalibrationRunService.class), auth(), courseAuth(),
        deployments);
    when(deployments.calibrationTarget()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.create(UUID.randomUUID(),
        new Request(UUID.randomUUID(), UUID.randomUUID()), UUID.randomUUID(), headers()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("El administrador debe seleccionar un modelo candidato");
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private static GoldenSetAuthorization auth() {
    var auth = mock(GoldenSetAuthorization.class);
    when(auth.require(any(HttpHeaders.class))).thenReturn(actor());
    when(auth.requireInstitutionalManager(any(HttpHeaders.class))).thenReturn(actor());
    when(auth.requireTemplateManager(any(HttpHeaders.class))).thenReturn(actor());
    return auth;
  }

  private static CourseAuthorization courseAuth() {
    return mock(CourseAuthorization.class);
  }

  private static Deployment deployment() {
    return new Deployment(UUID.randomUUID(), UUID.randomUUID(), "openai-compatible", "credencial",
        "gpt-4o-mini", "ACTIVE", Instant.now(), 0, Instant.now(), Map.of());
  }

  private static GoldenSetCaseInput caseInput() throws Exception {
    return new GoldenSetCaseInput(
        new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"role\":\"TUTOR\"}"),
        null, null, "alumno",
        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(), null);
  }

  private static Dimension anyDimension() {
    return Dimension.AUTONOMY;
  }
}