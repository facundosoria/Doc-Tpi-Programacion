package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.JdbcCalibrationWorkflowStore;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowEvaluationRunner;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowRunStore;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import ar.edu.utn.frc.tup.piv.llm.shadow.infrastructure.GoldenSetShadowSource;
import ar.edu.utn.frc.tup.piv.llm.shadow.infrastructure.TutorConversationShadowSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** E-31 contra Postgres real: persistencia, corrida completa con la base y el endpoint. El proveedor de
 * LLM se simula (un mock de {@link ModelInvocationService}); todo lo demás es real. */
class ShadowPersistenceIT extends AbstractIntegrationIT {
  static final UUID PLATFORM_RUBRIC = UUID.fromString("10000000-0000-0000-0000-000000000002");
  static final String HUMAN = "{\"AUTONOMY\":80,\"CLARITY\":70,\"PROGRESSION\":60,\"COMPLIANCE\":90,\"EFFICIENCY\":50}";

  @Autowired ShadowRunStore store;
  @Autowired RubricVersionRepository rubrics;
  @Autowired GoldenSetShadowSource goldenSource;
  @Autowired TutorConversationShadowSource tutorSource;
  @Autowired CourseGoldenSetRepository golden;
  @Autowired CalibrationRunRepository runs;
  @Autowired JdbcCalibrationWorkflowStore workflow;
  @Autowired JdbcTemplate jdbc;

  /** El claim es global (toma la corrida QUEUED más vieja de cualquier curso): cada test parte sin corridas. */
  @BeforeEach
  void cleanShadowRuns() {
    jdbc.update("delete from llm.shadow_runs");
  }

  private CallerIdentity actor() { return new CallerIdentity("admin-service", TEACHER, "req", null); }

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
      golden.addDraftCase(course, draft.id(), new GoldenSetCaseInput(json.readTree("[{\"role\":\"STUDENT\",\"content\":\"h" + i + "\"}]"),
          json.readTree("{\"s\":1}"), null, "Doc", json.readTree(HUMAN), null)).orElseThrow();
    }
    assertThat(golden.publishDraft(course, draft.id())).isTrue();
    return draft.id();
  }

  /** Deja a {@code rubric} como la rúbrica de la calibración activa del curso (por el mismo camino real). */
  private void activate(UUID course, UUID rubric, UUID goldenVersion) {
    UUID adapter = UUID.randomUUID();
    jdbc.update("insert into llm.model_adapters (id, provider, created_by_user_id) values (?,?,?)", adapter, "adp-" + adapter, TEACHER);
    UUID deployment = UUID.randomUUID();
    jdbc.update("insert into llm.model_deployments (id, adapter_id, model_id, model_version, provider_key) values (?,?,?,?,'openai-compatible')", deployment, adapter, "m-" + deployment, "v1");
    var run = runs.createCourse(course, rubric, goldenVersion, deployment, UUID.randomUUID(), TEACHER);
    jdbc.update("update llm.calibration_runs set state='RUNNING', started_at=now() where id=?", run.id());
    runs.finish(run.id(), true, BigDecimal.ONE, 1);
    workflow.activate(course, run.id(), actor());
  }

  private ShadowRun enqueue(UUID course, UUID baseline, UUID candidate, ShadowRun.Source source, UUID goldenVersion, int size) {
    return store.create(new ShadowRunStore.NewRun(course, baseline, candidate, source, goldenVersion, size, 10, UUID.randomUUID(), TEACHER));
  }

  @Test
  void runsAreIdempotentAndTheDatabaseRejectsInconsistentRuns() throws Exception {
    UUID course = UUID.randomUUID();
    UUID baseline = courseRubric(course);
    UUID candidate = courseRubric(course);
    UUID key = UUID.randomUUID();
    var first = store.create(new ShadowRunStore.NewRun(course, baseline, candidate, ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5, 10, key, TEACHER));
    var again = store.create(new ShadowRunStore.NewRun(course, baseline, candidate, ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5, 10, key, TEACHER));

    assertThat(again.id()).as("misma Idempotency-Key").isEqualTo(first.id());
    assertThat(first.state()).isEqualTo(ShadowRun.State.QUEUED);
    assertThat(store.list(course)).hasSize(1);

    assertThatThrownBy(() -> enqueue(course, baseline, baseline, ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5))
        .as("candidata = baseline").isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> enqueue(course, baseline, candidate, ShadowRun.Source.GOLDEN_SET, null, 5))
        .as("GOLDEN_SET sin golden set").isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> enqueue(course, baseline, candidate, ShadowRun.Source.TUTOR_CONVERSATIONS, null, 0))
        .as("muestra vacía").isInstanceOf(RuntimeException.class);
  }

  @Test
  void resolvesTheBaselineAndScopesVisibilityToTheCourse() throws Exception {
    UUID course = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    UUID rubric = courseRubric(course);
    UUID goldenVersion = courseGolden(course, 1);

    assertThat(store.activeRubricVersion(course)).as("sin calibración activa").isEmpty();
    activate(course, rubric, goldenVersion);
    assertThat(store.activeRubricVersion(course)).contains(rubric);

    assertThat(store.rubricVersionVisibleToCourse(course, rubric)).isTrue();
    assertThat(store.rubricVersionVisibleToCourse(other, rubric)).as("rúbrica de otro curso").isFalse();
    assertThat(store.rubricVersionVisibleToCourse(other, PLATFORM_RUBRIC)).as("de la plataforma, publicada").isTrue();
    assertThat(store.goldenSetVersionVisibleToCourse(course, goldenVersion)).isTrue();
    assertThat(store.goldenSetVersionVisibleToCourse(other, goldenVersion)).as("golden set de otro curso").isFalse();
  }

  @Test
  void aWholeRunOverTheGoldenSetIsComparedStoredAndTouchesNothingReal() throws Exception {
    UUID course = UUID.randomUUID();
    UUID baseline = courseRubric(course);
    UUID candidate = courseRubric(course);
    UUID goldenVersion = courseGolden(course, 2);
    var run = enqueue(course, baseline, candidate, ShadowRun.Source.GOLDEN_SET, goldenVersion, 10);

    int outbox = count("event_outbox"), calibrationResults = count("calibration_case_results"), pending = count("pending_evaluations");
    var models = mock(ModelInvocationService.class);
    // por caso: primero la baseline (70), después la candidata (78)
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(result(70), result(78), result(70), result(78));
    var runner = new ShadowEvaluationRunner(store, rubrics, models, List.of(goldenSource, tutorSource), json, 1000, 5, 3, 0.2);

    var claimed = store.claimNextQueued().orElseThrow();
    assertThat(claimed.id()).isEqualTo(run.id());
    assertThat(claimed.state()).isEqualTo(ShadowRun.State.RUNNING);
    runner.execute(run.id());

    var done = store.find(course, run.id()).orElseThrow();
    assertThat(done.state()).isEqualTo(ShadowRun.State.COMPLETED);
    assertThat(done.progress()).isEqualTo(100);
    assertThat(done.finishedAt()).isNotNull();
    var summary = done.summary();
    assertThat(summary.comparedCases()).isEqualTo(2);
    assertThat(summary.bias()).isEqualTo(8.0);
    assertThat(summary.mae()).isEqualTo(8.0);
    assertThat(summary.recommendation()).isEqualTo(ShadowMetrics.Recommendation.REVIEW);
    assertThat(summary.baselineMaeVsHuman()).as("la fuente es el golden set: hay nota humana").isNotNull();
    assertThat(jdbc.queryForObject("select count(*) from llm.shadow_case_results where shadow_run_id=? and error_code is null", Integer.class, run.id())).isEqualTo(2);

    assertThat(count("event_outbox")).as("no emitió eventos").isEqualTo(outbox);
    assertThat(count("calibration_case_results")).as("no tocó la calibración").isEqualTo(calibrationResults);
    assertThat(count("pending_evaluations")).as("no tocó evaluaciones reales").isEqualTo(pending);
  }

  @Test
  void aFailedCaseIsStoredWithItsErrorCodeAndTheRunStillCompletes() throws Exception {
    UUID course = UUID.randomUUID();
    UUID baseline = courseRubric(course);
    UUID candidate = courseRubric(course);
    UUID goldenVersion = courseGolden(course, 2);
    var run = enqueue(course, baseline, candidate, ShadowRun.Source.GOLDEN_SET, goldenVersion, 10);
    var models = mock(ModelInvocationService.class);
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult("no es json", "fake", "m"), result(70), result(70), result(70));
    var runner = new ShadowEvaluationRunner(store, rubrics, models, List.of(goldenSource), json, 1000, 5, 3, 0.2);

    store.claimNextQueued();
    runner.execute(run.id());

    var done = store.find(course, run.id()).orElseThrow();
    assertThat(done.state()).isEqualTo(ShadowRun.State.COMPLETED);
    assertThat(done.summary().failedCases()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from llm.shadow_case_results where shadow_run_id=? and error_code='EVALUATION_FAILED'", Integer.class, run.id())).isEqualTo(1);
  }

  @Test
  void claimingIsExclusiveAndAStuckRunIsReclaimedWithoutDuplicatingCases() throws Exception {
    UUID course = UUID.randomUUID();
    var run = enqueue(course, courseRubric(course), courseRubric(course), ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5);

    assertThat(store.claimNextQueued().orElseThrow().id()).isEqualTo(run.id());
    assertThat(store.claimNextQueued()).as("ya está RUNNING y reciente: nadie más la toma").isEmpty();

    store.recordCase(run.id(), new ShadowMetrics.CaseOutcome("c1", null, null, null, "EVALUATION_FAILED"));
    jdbc.update("update llm.shadow_runs set started_at = now() - interval '20 minutes' where id = ?", run.id());
    assertThat(store.claimNextQueued().orElseThrow().id()).as("RUNNING viejo: se reclama").isEqualTo(run.id());
    store.recordCase(run.id(), new ShadowMetrics.CaseOutcome("c1", null, null, null, "EVALUATION_FAILED"));
    assertThat(jdbc.queryForObject("select count(*) from llm.shadow_case_results where shadow_run_id=?", Integer.class, run.id()))
        .as("upsert: no duplica").isEqualTo(1);
  }

  @Test
  void finishedRunsArePurgedWithTheirCasesAfterTheRetentionPeriod() throws Exception {
    UUID course = UUID.randomUUID();
    var old = enqueue(course, courseRubric(course), courseRubric(course), ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5);
    var recent = enqueue(course, courseRubric(course), courseRubric(course), ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5);
    var queued = enqueue(course, courseRubric(course), courseRubric(course), ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5);
    for (var r : List.of(old, recent)) {
      jdbc.update("update llm.shadow_runs set state='RUNNING', started_at=now() where id=?", r.id());
      store.recordCase(r.id(), new ShadowMetrics.CaseOutcome("c", null, null, null, "EVALUATION_FAILED"));
      store.fail(r.id(), "X");
    }
    jdbc.update("update llm.shadow_runs set finished_at = now() - interval '40 days' where id = ?", old.id());

    assertThat(store.purgeFinishedBefore(30)).isEqualTo(1);

    assertThat(store.findById(old.id())).isEmpty();
    assertThat(jdbc.queryForObject("select count(*) from llm.shadow_case_results where shadow_run_id=?", Integer.class, old.id())).isZero();
    assertThat(store.findById(recent.id())).isPresent();
    assertThat(store.findById(queued.id())).as("sin terminar: no se purga").isPresent();
  }

  @Test
  void theTutorSourceReplaysRealConversationsOfTheCourse() {
    UUID course = UUID.randomUUID();
    UUID learner = UUID.randomUUID();
    UUID conversation = UUID.randomUUID();
    UUID tooShort = UUID.randomUUID();
    for (var c : List.of(conversation, tooShort)) {
      jdbc.update("insert into llm.conversations (id, course_cohort_id, learner_id, titulo) values (?,?,?,?)", c, course, learner, "Duda");
    }
    jdbc.update("insert into llm.messages (conversation_id, rol, contenido, created_at) values (?, 'alumno', 'no entiendo el push', now() - interval '2 minutes')", conversation);
    jdbc.update("insert into llm.messages (conversation_id, rol, contenido, created_at) values (?, 'tutor', 'mirá el tope', now() - interval '1 minute')", conversation);
    jdbc.update("insert into llm.messages (conversation_id, rol, contenido) values (?, 'alumno', 'hola')", tooShort);
    var run = enqueue(course, courseRubric(course), courseRubric(course), ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5);

    var samples = tutorSource.load(run);

    assertThat(samples).hasSize(1);
    assertThat(samples.get(0).sourceRef()).isEqualTo(conversation.toString());
    assertThat(samples.get(0).transcript().get(0).get("role").asText()).isEqualTo("STUDENT");
    assertThat(samples.get(0).transcript().get(1).get("role").asText()).isEqualTo("TUTOR");
    assertThat(samples.get(0).humanScores()).as("sin nota humana").isNull();
  }

  @Test
  void theEndpointQueuesAndReadsRunsForTheCourseTeacherOnly() throws Exception {
    UUID course = UUID.randomUUID();
    UUID rubric = courseRubric(course);
    UUID candidate = courseRubric(course);
    activate(course, rubric, courseGolden(course, 1));
    String path = "/api/llm/courses/" + course + "/shadow-runs";
    String requestBody = "{\"candidateRubricVersionId\":\"" + candidate + "\",\"source\":\"TUTOR_CONVERSATIONS\",\"sampleSize\":5}";
    UUID key = UUID.randomUUID();

    var created = mvc.perform(asTeacher(post(path), course).header("Idempotency-Key", key.toString()).content(requestBody))
        .andExpect(status().isAccepted());
    var run = body(created);
    assertThat(run.get("state").asText()).isEqualTo("QUEUED");
    assertThat(run.get("baselineRubricVersionId").asText()).isEqualTo(rubric.toString());

    var read = body(mvc.perform(asTeacher(get(path + "/" + run.get("id").asText()), course)).andExpect(status().isOk()));
    assertThat(read.get("id").asText()).isEqualTo(run.get("id").asText());
    assertThat(body(mvc.perform(asTeacher(get(path), course)).andExpect(status().isOk())).get("items")).hasSize(1);

    UUID stranger = UUID.randomUUID();
    mvc.perform(asTeacher(get(path), stranger)).andExpect(status().isForbidden());
  }

  private ModelInvocationResult result(int score) {
    return new ModelInvocationResult("{\"autonomy\":" + score + ",\"clarity\":" + score + ",\"progression\":" + score
        + ",\"compliance\":" + score + ",\"efficiency\":" + score + "}", "fake", "m");
  }

  private int count(String table) {
    return jdbc.queryForObject("select count(*) from llm." + table, Integer.class);
  }
}
