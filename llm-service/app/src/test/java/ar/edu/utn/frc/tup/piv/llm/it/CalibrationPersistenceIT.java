package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ChallengeCalibrationAssignmentRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseEvaluationStatusRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.JdbcCalibrationWorkflowStore;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CalibrationPersistenceIT extends AbstractIntegrationIT {
  static final UUID PLATFORM_RUBRIC = UUID.fromString("10000000-0000-0000-0000-000000000002");
  static final String SCORES = "{\"AUTONOMY\":80,\"CLARITY\":70,\"PROGRESSION\":60,\"COMPLIANCE\":90,\"EFFICIENCY\":50}";

  @Autowired CalibrationRunRepository runs;
  @Autowired ChallengeCalibrationAssignmentRepository assignments;
  @Autowired JdbcCalibrationWorkflowStore store;
  @Autowired CourseEvaluationStatusRepository status;
  @Autowired CourseGoldenSetRepository golden;
  @Autowired JdbcTemplate jdbc;

  /** El claim es global: drena corridas en cola de otros ITs para reclamar solo las propias. */
  @BeforeEach
  void drainCalibrationQueue() {
    while (runs.claimNextQueued().isPresent()) {
      // se descartan (quedan RUNNING): no interesan a este test
    }
  }

  private CallerIdentity actor() { return new CallerIdentity("admin-service", TEACHER, "req", null); }

  private UUID deployment(boolean withCredential) {
    UUID adapter = UUID.randomUUID();
    jdbc.update("insert into llm.model_adapters (id, provider, created_by_user_id) values (?,?,?)", adapter, "adp-" + adapter, TEACHER);
    UUID cred = null;
    if (withCredential) {
      cred = UUID.randomUUID();
      jdbc.update("insert into llm.provider_credentials (id, provider_key, display_name, public_configuration, encrypted_secrets, secret_nonce, secret_mask, created_by_user_id) values (?, 'openai-compatible', 'x', cast('{\"baseUrl\":\"http://h/v1\"}' as jsonb), ?, ?, 'sk-1234', ?)",
          cred, new byte[] {1, 2}, new byte[] {3}, TEACHER);
    }
    UUID dep = UUID.randomUUID();
    jdbc.update("insert into llm.model_deployments (id, adapter_id, model_id, model_version, credential_id, provider_key) values (?,?,?,?,?,'openai-compatible')", dep, adapter, "m-" + dep, "v1", cred);
    return dep;
  }

  private UUID courseRubric(UUID course) {
    UUID family = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    jdbc.update("insert into llm.rubric_families (id, scope, course_id, name, next_version, created_by_user_id) values (?, 'COURSE', ?, ?, 2, ?)", family, course, "R " + family, TEACHER);
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, state, created_by_user_id, published_at, name) values (?,?,1,'PUBLISHED',?, now(), 'R')", version, family, TEACHER);
    jdbc.update("insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, weight) select ?, dimension_key, label, criterion, anchors, weight from llm.rubric_dimension_v2 where rubric_version_id = ?", version, PLATFORM_RUBRIC);
    return version;
  }

  private UUID courseGolden(UUID course, int cases) throws Exception {
    var draft = golden.createDraft(course, "G " + UUID.randomUUID(), TEACHER);
    for (int i = 0; i < cases; i++) {
      golden.addDraftCase(course, draft.id(), new GoldenSetCaseInput(json.readTree("[{\"role\":\"STUDENT\",\"content\":\"h\"}]"),
          json.readTree("{\"s\":1}"), null, "Doc", json.readTree(SCORES), null)).orElseThrow();
    }
    assertThat(golden.publishDraft(course, draft.id())).isTrue();
    return draft.id();
  }

  private UUID platformGolden() {
    UUID family = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    jdbc.update("insert into llm.golden_set_families (id, scope, name, next_version, created_by_user_id) values (?, 'PLATFORM', ?, 2, ?)", family, "P " + family, TEACHER);
    jdbc.update("insert into llm.golden_set_versions (id, family_id, version_no, state, created_by_user_id, published_at) values (?,?,1,'PUBLISHED',?, now())", version, family, TEACHER);
    return version;
  }

  /** Lleva una corrida en cola a un estado final por el mismo camino que el worker (claim + finish). */
  private void passRun(UUID id) {
    jdbc.update("update llm.calibration_runs set state='RUNNING', started_at=now() where id=? and state='QUEUED'", id);
    runs.finish(id, true, BigDecimal.ONE, 1);
    assertThat(runs.byId(id).orElseThrow().state()).isEqualTo("PASSED");
  }

  @Test
  void courseRunLifecycleIdempotencyAndListing() throws Exception {
    UUID course = UUID.randomUUID();
    UUID rubric = courseRubric(course);
    UUID gold = courseGolden(course, 1);
    UUID dep = deployment(false);
    UUID key = UUID.randomUUID();

    var run = runs.createCourse(course, rubric, gold, dep, key, TEACHER);
    assertThat(run.state()).isEqualTo("QUEUED");
    assertThat(run.stage()).isEqualTo("COURSE");
    assertThat(run.reason()).isEqualTo("MANUAL");
    assertThat(runs.createCourse(course, rubric, gold, dep, key, TEACHER).id()).as("misma clave").isEqualTo(run.id());
    // rúbrica de otro curso: rechazada
    assertThatThrownBy(() -> runs.createCourse(UUID.randomUUID(), rubric, gold, dep, UUID.randomUUID(), TEACHER))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("no pertenecen al curso");
    var domain = runs.create(course, rubric, gold, dep, UUID.randomUUID(), TEACHER);
    assertThat(domain.state()).isEqualTo("QUEUED");
    assertThat(domain.maeFinal()).isNull();

    assertThat(runs.find(course, run.id())).isPresent();
    assertThat(runs.find(UUID.randomUUID(), run.id())).isEmpty();
    assertThat(runs.findById(run.id()).orElseThrow().progress()).isZero();
    assertThat(runs.findById(UUID.randomUUID())).isEmpty();
    assertThat(runs.byId(run.id()).orElseThrow().goldenSetVersionId()).isEqualTo(gold);
    assertThat(runs.list(course)).hasSize(2);

    // claim mueve la corrida en cola más antigua a RUNNING
    var claimed = runs.claimNextQueued().orElseThrow();
    assertThat(claimed.id()).isEqualTo(run.id());
    assertThat(claimed.state()).isEqualTo("RUNNING");
    runs.progress(run.id(), 40);
    assertThat(runs.byId(run.id()).orElseThrow().progress()).isEqualTo(40);
    runs.recordProgress(run.id(), 55, 3.5, 7);
    var progressed = runs.byId(run.id()).orElseThrow();
    assertThat(progressed.progress()).isEqualTo(55);
    assertThat(progressed.maeFinal()).isEqualByComparingTo("3.5");
    assertThat(progressed.maxIndividualError()).isEqualTo(7);
    runs.recordProgress(run.id(), 56, null, null);
    assertThat(runs.byId(run.id()).orElseThrow().maeFinal()).isNull();

    runs.finish(run.id(), true, new BigDecimal("2.5000"), 9);
    var done = runs.byId(run.id()).orElseThrow();
    assertThat(done.state()).isEqualTo("PASSED");
    assertThat(done.progress()).isEqualTo(100);
    assertThat(done.finishedAt()).isNotNull();
    runs.progress(run.id(), 10); // ya no RUNNING: ignorado
    runs.finish(run.id(), false, BigDecimal.ONE, 1); // ya terminada: ignorado
    runs.fail(run.id(), "X", "y");
    assertThat(runs.byId(run.id()).orElseThrow().state()).isEqualTo("PASSED");
    assertThat(runs.byId(run.id()).orElseThrow().failureCode()).isNull();

    // la segunda corrida termina FAILED con diagnóstico
    var second = runs.claimNextQueued().orElseThrow();
    assertThat(second.id()).isEqualTo(domain.id());
    runs.fail(second.id(), "MODEL_ERROR", "sin respuesta");
    var failed = runs.byId(second.id()).orElseThrow();
    assertThat(failed.state()).isEqualTo("FAILED");
    assertThat(failed.failureCode()).isEqualTo("MODEL_ERROR");
    assertThat(failed.failureDetail()).isEqualTo("sin respuesta");

    // sin colas propias: ya no hay nada que reclamar (otros ITs no dejan corridas en cola)
    assertThat(runs.claimNextQueued()).isEmpty();
    assertThat(runs.doubleEvidence(dep)).isFalse();
  }

  @Test
  void platformRunsProfileAndDoubleEvidence() throws Exception {
    UUID course = UUID.randomUUID();
    UUID dep = deployment(false);
    UUID pGold = platformGolden();

    var platform = runs.createPlatform(PLATFORM_RUBRIC, pGold, dep, TEACHER);
    assertThat(platform.stage()).isEqualTo("PLATFORM");
    assertThat(platform.courseId()).isNull();
    assertThat(runs.listPlatform()).extracting("id").contains(platform.id());
    // versiones de curso no sirven como institucionales
    UUID courseGold = courseGolden(course, 1);
    assertThatThrownBy(() -> runs.createPlatform(PLATFORM_RUBRIC, courseGold, dep, TEACHER))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("PLATFORM");
    assertThatThrownBy(() -> runs.createPlatform(courseRubric(course), pGold, dep, TEACHER))
        .isInstanceOf(RuntimeException.class);

    assertThat(runs.claimNextQueued().orElseThrow().id()).isEqualTo(platform.id());
    runs.finish(platform.id(), true, BigDecimal.ONE, 2);
    assertThat(runs.doubleEvidence(dep)).as("falta la etapa de curso").isFalse();

    var courseRun = runs.createCourse(course, courseRubric(course), courseGold, dep, UUID.randomUUID(), TEACHER);
    assertThat(runs.claimNextQueued().orElseThrow().id()).isEqualTo(courseRun.id());
    runs.finish(courseRun.id(), true, BigDecimal.ONE, 2);
    assertThat(runs.doubleEvidence(dep)).isTrue();
    assertThat(runs.doubleEvidence(UUID.randomUUID())).isFalse();

    // perfil institucional (singleton): validaciones y restauración del valor previo
    var previous = runs.profile();
    try {
      assertThatThrownBy(() -> runs.profile(courseGold, PLATFORM_RUBRIC, TEACHER))
          .isInstanceOf(RuntimeException.class).hasMessageContaining("institucionales");
      runs.profile(pGold, PLATFORM_RUBRIC, TEACHER);
      var profile = runs.profile().orElseThrow();
      assertThat(profile.goldenSetVersionId()).isEqualTo(pGold);
      assertThat(profile.rubricVersionId()).isEqualTo(PLATFORM_RUBRIC);
      assertThat(profile.configuredAt()).isNotNull();
      UUID pGold2 = platformGolden();
      runs.profile(pGold2, PLATFORM_RUBRIC, TEACHER);
      assertThat(runs.profile().orElseThrow().goldenSetVersionId()).isEqualTo(pGold2);
    } finally {
      previous.ifPresent(p -> runs.profile(p.goldenSetVersionId(), p.rubricVersionId(), TEACHER));
    }
  }

  @Test
  void executionLoadsRubricWeightsAndCasesAndSavesResultsIdempotently() throws Exception {
    UUID course = UUID.randomUUID();
    UUID rubric = courseRubric(course);
    UUID gold = courseGolden(course, 2);
    UUID dep = deployment(true);
    var run = runs.createCourse(course, rubric, gold, dep, UUID.randomUUID(), TEACHER);

    assertThatThrownBy(() -> runs.execution(run.id())).as("aún en cola")
        .isInstanceOf(RuntimeException.class).hasMessageContaining("no está disponible");
    assertThat(runs.claimNextQueued().orElseThrow().id()).isEqualTo(run.id());

    var exec = runs.execution(run.id());
    assertThat(exec.run().id()).isEqualTo(run.id());
    assertThat(exec.deployment().id()).isEqualTo(dep);
    // Desde la V26 la credencial expone `provider_key` y el secreto ya no viaja en el Deployment:
    // el gateway lo descifra en su frontera (ProviderInvocationGateway).
    assertThat(exec.deployment().providerKey()).isEqualTo("openai-compatible");
    assertThat(exec.weights()).containsEntry(Dimension.AUTONOMY, 30).containsEntry(Dimension.EFFICIENCY, 10).hasSize(5);
    assertThat(exec.rubric()).contains("AUTONOMY (peso 30").contains("Anclas:");
    assertThat(exec.cases()).hasSize(2);
    var first = exec.cases().get(0);
    assertThat(first.humanScores()).containsEntry(Dimension.CLARITY, 70).containsEntry(Dimension.EFFICIENCY, 50);
    assertThat(first.transcript().isArray()).isTrue();

    Map<Dimension, Integer> model = new EnumMap<>(Dimension.class);
    for (var d : Dimension.values()) model.put(d, first.humanScores().get(d) + 10);
    runs.saveCase(run.id(), first, model, exec.weights());
    runs.saveCase(run.id(), first, model, exec.weights()); // conflicto ignorado
    assertThat(jdbc.queryForObject("select count(*) from llm.calibration_case_results where calibration_run_id=?", Integer.class, run.id())).isEqualTo(1);
    var row = jdbc.queryForMap("select human_final_score, model_final_score, final_error, dimension_errors::text as e from llm.calibration_case_results where calibration_run_id=?", run.id());
    // humano: .8*30+.7*25+.6*20+.9*15+.5*10 = 24+17.5+12+13.5+5 = 72 ; modelo +10 en cada dimensión = 82
    assertThat((BigDecimal) row.get("human_final_score")).isEqualByComparingTo("72");
    assertThat((BigDecimal) row.get("model_final_score")).isEqualByComparingTo("82");
    assertThat((BigDecimal) row.get("final_error")).isEqualByComparingTo("10");
    assertThat((String) row.get("e")).contains("\"AUTONOMY\": 10");
    runs.finish(run.id(), false, new BigDecimal("10"), 10);
    assertThat(runs.byId(run.id()).orElseThrow().state()).isEqualTo("FAILED");
  }

  @Test
  void executionFailsWithoutActiveCredentialOrWithoutCases() throws Exception {
    UUID course = UUID.randomUUID();
    UUID rubric = courseRubric(course);
    UUID dep = deployment(false);
    var noCred = runs.createCourse(course, rubric, courseGolden(course, 1), dep, UUID.randomUUID(), TEACHER);
    assertThat(runs.claimNextQueued().orElseThrow().id()).isEqualTo(noCred.id());
    assertThatThrownBy(() -> runs.execution(noCred.id())).isInstanceOf(RuntimeException.class).hasMessageContaining("credencial activa");
    runs.fail(noCred.id(), "NO_CRED", "d");

    var noCases = runs.createCourse(course, rubric, courseGolden(course, 0), deployment(true), UUID.randomUUID(), TEACHER);
    assertThat(runs.claimNextQueued().orElseThrow().id()).isEqualTo(noCases.id());
    assertThatThrownBy(() -> runs.execution(noCases.id())).isInstanceOf(RuntimeException.class).hasMessageContaining("no contiene casos");
    runs.fail(noCases.id(), "EMPTY", "d");
  }

  @Test
  void assignmentsWorkflowStoreAndEvaluationStatusReadModels() throws Exception {
    UUID course = UUID.randomUUID();
    UUID rubric = courseRubric(course);
    UUID gold = courseGolden(course, 1);
    UUID dep = deployment(false);
    var run = runs.createCourse(course, rubric, gold, dep, UUID.randomUUID(), TEACHER);
    var next = runs.createCourse(course, rubric, gold, dep, UUID.randomUUID(), TEACHER);
    UUID challenge = UUID.randomUUID();
    UUID challenge2 = UUID.randomUUID();
    UUID attempt = UUID.randomUUID();

    // sin calibración activa nada se asigna ni se encola
    assertThat(status.activeCalibration(course)).isEmpty();
    assertThat(status.assignments(course)).isEmpty();
    assertThat(assignments.assignActive(challenge, course)).isFalse();
    assertThat(store.hasValidCalibration(challenge)).isFalse();
    store.enqueue(attempt, challenge, UUID.randomUUID());
    assertThat(jdbc.queryForObject("select count(*) from llm.pending_evaluations where attempt_id=?", Integer.class, attempt)).isZero();

    assertThat(store.isPassed(run.id())).isFalse();
    passRun(run.id());
    passRun(next.id());
    assertThat(store.isPassed(run.id())).isTrue();
    assertThat(store.isPassed(course, run.id())).isTrue();
    assertThat(store.isPassed(UUID.randomUUID(), run.id())).isFalse();
    assertThat(runs.byId(run.id()).orElseThrow().finishedAt()).isNotNull();

    store.activate(course, run.id(), actor());
    var active = status.activeCalibration(course).orElseThrow();
    assertThat(active.calibrationRunId()).isEqualTo(run.id());
    assertThat(jdbc.queryForObject("select count(*) from llm.audit_events where resource_id=? and action='calibration.activated'", Integer.class, run.id())).isPositive();

    assertThat(assignments.assignActive(challenge, course)).isTrue();
    assertThat(assignments.assignActive(challenge2, course)).isTrue();
    assertThatThrownBy(() -> assignments.assignActive(challenge, course)).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    assertThat(store.hasValidCalibration(challenge)).isTrue();
    assertThat(assignments.assignActive(UUID.randomUUID(), UUID.randomUUID())).as("otro curso sin activa").isFalse();

    var preview = assignments.preview(course, next.id());
    assertThat(preview.migrable()).containsExactlyInAnyOrder(challenge, challenge2);
    assertThat(preview.locked()).isEmpty();

    assertThat(assignments.lockOnFirstAttempt(challenge, attempt)).isTrue();
    assertThat(assignments.lockOnFirstAttempt(challenge, UUID.randomUUID())).as("ya bloqueado").isFalse();
    assertThat(assignments.lockOnFirstAttempt(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    preview = assignments.preview(course, next.id());
    assertThat(preview.migrable()).containsExactly(challenge2);
    assertThat(preview.locked()).containsExactly(challenge);

    assertThat(assignments.migrate(course, next.id(), Set.of())).isZero();
    // solo migra los no bloqueados
    assertThat(assignments.migrate(course, next.id(), Set.of(challenge, challenge2))).isEqualTo(1);
    var assigned = status.assignments(course);
    assertThat(assigned).hasSize(2);
    assertThat(assigned.stream().filter(a -> a.challengeId().equals(challenge2)).findFirst().orElseThrow().calibrationRunId()).isEqualTo(next.id());
    var lockedOne = assigned.stream().filter(a -> a.challengeId().equals(challenge)).findFirst().orElseThrow();
    assertThat(lockedOne.calibrationRunId()).isEqualTo(run.id());
    assertThat(lockedOne.locked()).isTrue();
    assertThat(assignments.migrate(UUID.randomUUID(), next.id(), Set.of(challenge2))).isZero();

    // re-activar otra corrida actualiza (upsert)
    store.activate(course, next.id(), actor());
    assertThat(status.activeCalibration(course).orElseThrow().calibrationRunId()).isEqualTo(next.id());

    // cola de evaluaciones con snapshot de la calibración asignada
    UUID key = UUID.randomUUID();
    store.enqueue(attempt, challenge2, key);
    store.enqueue(attempt, challenge2, UUID.randomUUID()); // mismo attempt: no duplica
    var pendings = status.pendingEvaluations(course);
    assertThat(pendings).hasSize(1);
    assertThat(pendings.get(0).state()).isEqualTo("QUEUED");
    assertThat(pendings.get(0).reason()).isEqualTo("CALIBRATION_UNAVAILABLE");
    assertThat(pendings.get(0).attemptId()).isEqualTo(attempt);
    assertThat(pendings.get(0).assignmentId()).isEqualTo(challenge2);
    assertThat(pendings.get(0).calibrationRunId()).isEqualTo(next.id());
    assertThat(status.pendingEvaluations(UUID.randomUUID())).isEmpty();

    java.util.Optional<CalibrationWorkflowService.QueuedEvaluation> claimed = store.claimNextQueued();
    assertThat(claimed).isPresent();
    UUID pendingId = pendings.get(0).id();
    assertThat(store.markRunning(pendingId)).isTrue();
    assertThat(store.markRunning(pendingId)).as("ya RUNNING").isFalse();
    assertThat(store.markRunning(UUID.randomUUID())).isFalse();
    assertThat(status.pendingEvaluations(course).get(0).state()).isEqualTo("RUNNING");
  }


  @Test
  void workflowStoreTransitionMovesEnumStateOnlyFromExpectedState() throws Exception {
    UUID course = UUID.randomUUID();
    var run = runs.createCourse(course, courseRubric(course), courseGolden(course, 1), deployment(false), UUID.randomUUID(), TEACHER);

    assertThat(store.transition(run.id(), CalibrationState.RUNNING, CalibrationState.PASSED)).as("estado de origen distinto").isFalse();
    assertThat(store.transition(run.id(), CalibrationState.QUEUED, CalibrationState.RUNNING)).isTrue();
    assertThat(runs.byId(run.id()).orElseThrow().state()).isEqualTo("RUNNING");
    assertThat(store.transition(run.id(), CalibrationState.RUNNING, CalibrationState.PASSED)).isTrue();
    var done = runs.byId(run.id()).orElseThrow();
    assertThat(done.state()).isEqualTo("PASSED");
    assertThat(done.finishedAt()).isNotNull();
  }
}
