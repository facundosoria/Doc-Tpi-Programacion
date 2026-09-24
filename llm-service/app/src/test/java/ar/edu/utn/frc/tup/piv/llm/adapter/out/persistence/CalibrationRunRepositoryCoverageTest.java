package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationRunRepositoryCoverageTest {
  private final ObjectMapper json = new ObjectMapper();

  private ResultSet runResultSet(CalibrationRunRepository.Run run) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getObject(1, UUID.class)).thenReturn(run.id());
    when(rs.getObject(2, UUID.class)).thenReturn(run.courseId());
    when(rs.getString(3)).thenReturn(run.stage());
    when(rs.getString(4)).thenReturn(run.state());
    when(rs.getInt(5)).thenReturn(run.progress());
    when(rs.getObject(6, UUID.class)).thenReturn(run.rubricVersionId());
    when(rs.getObject(7, UUID.class)).thenReturn(run.goldenSetVersionId());
    when(rs.getObject(8, UUID.class)).thenReturn(run.modelDeploymentId());
    when(rs.getBigDecimal(9)).thenReturn(run.maeFinal());
    when(rs.getObject(10, Integer.class)).thenReturn(run.maxIndividualError());
    when(rs.getString(11)).thenReturn(run.reason());
    when(rs.getTimestamp(12)).thenReturn(run.createdAt() == null ? null : Timestamp.from(run.createdAt()));
    when(rs.getTimestamp(13)).thenReturn(run.finishedAt() == null ? null : Timestamp.from(run.finishedAt()));
    when(rs.getString(14)).thenReturn(run.failureCode());
    when(rs.getString(15)).thenReturn(run.failureDetail());
    when(rs.getString(16)).thenReturn(run.expirationReason());
    return rs;
  }

  private Answer<List<?>> rows(ResultSet rs, int count) {
    return invocation -> {
      RowMapper<?> mapper = invocation.getArgument(1);
      List<Object> mapped = new ArrayList<>();
      for (int i = 0; i < count; i++) mapped.add(mapper.mapRow(rs, i));
      return mapped;
    };
  }

  /**
   * Same as {@link #rows(ResultSet, int)} but builds and stubs the ResultSet at invocation
   * time, avoiding stubbing inside the outer when(...) (Mockito UnfinishedStubbingException).
   */
  private Answer<List<?>> runRows(CalibrationRunRepository.Run run, int count) {
    return invocation -> {
      RowMapper<?> mapper = invocation.getArgument(1);
      ResultSet rs = runResultSet(run);
      List<Object> mapped = new ArrayList<>();
      for (int i = 0; i < count; i++) mapped.add(mapper.mapRow(rs, i));
      return mapped;
    };
  }

  private CalibrationRunRepository.Run run() {
    return new CalibrationRunRepository.Run(UUID.randomUUID(), UUID.randomUUID(), "COURSE", "QUEUED",
        0, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, 3, "MANUAL",
        Instant.now(), null, null, null, null);
  }

  @Test void createReturnsTheExistingIdempotentRun() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), key = UUID.randomUUID();
    CalibrationRunRepository.Run expected = run();
    when(jdbc.query(contains("r.idempotency_key=?"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(runRows(expected, 1));

    CalibrationRunRepository.Run result = repository.createCourse(course, expected.rubricVersionId(),
        expected.goldenSetVersionId(), expected.modelDeploymentId(), key, UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
    verify(jdbc, never()).update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class));
  }

  @Test void createInsertsAndReloadsANewRunForAnUnknownKey() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), rubric = UUID.randomUUID(), golden = UUID.randomUUID();
    UUID deployment = UUID.randomUUID(), key = UUID.randomUUID(), actor = UUID.randomUUID();
    CalibrationRunRepository.Run expected = new CalibrationRunRepository.Run(UUID.randomUUID(),
        course, "COURSE", "QUEUED", 0, rubric, golden, deployment, null, null, "MANUAL",
        Instant.now(), null, null, null, null);
    when(jdbc.query(contains("r.idempotency_key=?"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());
    when(jdbc.update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class))).thenReturn(1);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(expected, 1));

    CalibrationRunRepository.Run result = repository.createCourse(course, rubric, golden,
        deployment, key, actor);

    assertThat(result).isEqualTo(expected);
    verify(jdbc).update(contains("insert into llm.calibration_runs"), any(UUID.class),
        eq(course), eq(rubric), eq(golden), eq(deployment), eq(actor), eq(key), eq(rubric),
        eq(course), eq(golden), eq(course));
  }

  @Test void createThrowsWhenThePublishedVersionsDoNotBelongToTheCourse() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), key = UUID.randomUUID();
    when(jdbc.query(contains("r.idempotency_key=?"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());
    when(jdbc.update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class))).thenReturn(0);

    assertThatThrownBy(() -> repository.createCourse(course, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), key, UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
  }

  @Test void createStabilityCreatesAGroupWithSeededRuns() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), key = UUID.randomUUID();
    CalibrationRunRepository.Run first = run();
    when(jdbc.query(contains("select id from llm.calibration_stability_groups"),
        any(RowMapper.class), any(UUID.class), any(UUID.class))).thenReturn(List.of());
    when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);
    when(jdbc.update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(Integer.class),
        any(Long.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(1);
    when(jdbc.query(contains("where r.stability_group_id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(first, 3));

    List<CalibrationRunRepository.Run> result = repository.createStability(course,
        first.rubricVersionId(), first.goldenSetVersionId(), first.modelDeploymentId(), key,
        UUID.randomUUID(), List.of(1L, 2L, 3L));

    assertThat(result).hasSize(3);
    verify(jdbc).update(contains("insert into llm.calibration_stability_groups"), any(), any(), any());
    verify(jdbc, times(3)).update(contains("stability_group_id"), any(), any(), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test void createStabilityReusesTheExistingGroup() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), key = UUID.randomUUID(), groupId = UUID.randomUUID();
    CalibrationRunRepository.Run first = run();
    when(jdbc.query(contains("select id from llm.calibration_stability_groups"),
        any(RowMapper.class), any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
      RowMapper<?> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getObject(1, UUID.class)).thenReturn(groupId);
      return List.of(mapper.mapRow(rs, 0));
    });
    when(jdbc.query(contains("where r.stability_group_id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(first, 1));

    List<CalibrationRunRepository.Run> result = repository.createStability(course,
        first.rubricVersionId(), first.goldenSetVersionId(), first.modelDeploymentId(), key,
        UUID.randomUUID(), List.of(1L, 2L, 3L));

    assertThat(result).hasSize(1);
    verify(jdbc, never()).update(contains("insert into llm.calibration_stability_groups"), any(), any(), any());
  }

  @Test void createStabilityThrowsWhenAStableRunCannotBeInserted() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), key = UUID.randomUUID();
    when(jdbc.query(contains("select id from llm.calibration_stability_groups"),
        any(RowMapper.class), any(UUID.class), any(UUID.class))).thenReturn(List.of());
    when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);
    when(jdbc.update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(Integer.class),
        any(Long.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(1).thenReturn(0);

    assertThatThrownBy(() -> repository.createStability(course, UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), key, UUID.randomUUID(), List.of(1L, 2L, 3L)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void createPlatformInsertsAndReloadsTheInstitutionalRun() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = new CalibrationRunRepository.Run(UUID.randomUUID(),
        null, "PLATFORM", "QUEUED", 0, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(), any(UUID.class), any(UUID.class))).thenReturn(1);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(expected, 1));

    CalibrationRunRepository.Run result = repository.createPlatform(expected.rubricVersionId(),
        expected.goldenSetVersionId(), expected.modelDeploymentId(), UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
  }

  @Test void createPlatformThrowsWhenInstitutionalVersionsAreMissing() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    when(jdbc.update(anyString(), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class),
        any(UUID.class), any(), any(UUID.class), any(UUID.class))).thenReturn(0);
    assertThatThrownBy(() -> repository.createPlatform(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
  }

  @Test void createPlatformReturnsExistingRunWhenIdempotencyKeyMatches() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = new CalibrationRunRepository.Run(UUID.randomUUID(),
        null, "PLATFORM", "QUEUED", 0, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        null, null, "MANUAL", Instant.now(), null, null, null, null);
    UUID key = UUID.randomUUID();
    when(jdbc.query(contains("r.idempotency_key=?"), any(RowMapper.class), eq(key)))
        .thenAnswer(runRows(expected, 1));

    CalibrationRunRepository.Run result = repository.createPlatform(expected.rubricVersionId(),
        expected.goldenSetVersionId(), expected.modelDeploymentId(), key, UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
  }

  @Test void claimNextQueuedClaimsTheFirstAvailableRun() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = new CalibrationRunRepository.Run(UUID.randomUUID(),
        UUID.randomUUID(), "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(runRows(expected, 1));
    assertThat(repository.claimNextQueued()).contains(expected);
    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
    assertThat(repository.claimNextQueued()).isEmpty();
  }

  @Test void findLocatesARunByCourseAndId() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = run();
    when(jdbc.query(contains("where r.course_id=? and r.id=?"), any(RowMapper.class),
        any(UUID.class), any(UUID.class))).thenAnswer(runRows(expected, 1));
    assertThat(repository.find(expected.courseId(), expected.id())).contains(expected);
    when(jdbc.query(contains("where r.course_id=? and r.id=?"), any(RowMapper.class),
        any(UUID.class), any(UUID.class))).thenReturn(List.of());
    assertThat(repository.find(expected.courseId(), expected.id())).isEmpty();
  }

  @Test void byIdReturnsARunWhenPresent() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = run();
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(expected, 1));
    assertThat(repository.byId(expected.id())).contains(expected);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());
    assertThat(repository.byId(expected.id())).isEmpty();
  }

  @Test void listsRunsForACourse() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = run();
    when(jdbc.query(contains("where r.course_id=? order by r.created_at desc"), any(RowMapper.class),
        any(UUID.class))).thenAnswer(runRows(expected, 1));
    assertThat(repository.list(expected.courseId())).containsExactly(expected);
  }

  @Test void listsPlatformRuns() throws SQLException {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run expected = run();
    when(jdbc.query(contains("where r.stage='PLATFORM'"), any(RowMapper.class)))
        .thenAnswer(runRows(expected, 1));
    assertThat(repository.listPlatform()).containsExactly(expected);
  }

  @Test void stabilityGroupsMapsGroupsAndTheirNestedRuns() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID course = UUID.randomUUID(), groupId = UUID.randomUUID();
    CalibrationRunRepository.Run nested = run();
    ResultSet groupRs = mock(ResultSet.class);
    when(groupRs.getObject(1, UUID.class)).thenReturn(groupId);
    when(groupRs.getString(2)).thenReturn("RUNNING");
    when(groupRs.getBigDecimal(3)).thenReturn(BigDecimal.ONE);
    when(groupRs.getTimestamp(4)).thenReturn(Timestamp.from(Instant.now()));
    when(groupRs.getTimestamp(5)).thenReturn(null, Timestamp.from(Instant.now()));
    when(jdbc.query(contains("state,mae_spread,created_at,finished_at"), any(RowMapper.class),
        any(UUID.class))).thenAnswer(rows(groupRs, 2));
    when(jdbc.query(contains("where r.stability_group_id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(nested, 1));

    var groups = repository.stabilityGroups(course);

    assertThat(groups).hasSize(2);
    assertThat(groups.get(0).id()).isEqualTo(groupId);
    assertThat(groups.get(0).runs()).hasSize(1);
    assertThat(groups.get(0).finishedAt()).isNull();
    assertThat(groups.get(1).finishedAt()).isNotNull();
  }

  @Test void profileReturnsTheInstitutionalReference() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID golden = UUID.randomUUID(), rubric = UUID.randomUUID();
    ResultSet profileRs = mock(ResultSet.class);
    when(profileRs.getObject(1, UUID.class)).thenReturn(golden);
    when(profileRs.getObject(2, UUID.class)).thenReturn(rubric);
    when(profileRs.getTimestamp(3)).thenReturn(Timestamp.from(Instant.now()));
    when(jdbc.query(contains("from llm.institutional_calibration_profiles"), any(RowMapper.class)))
        .thenAnswer(rows(profileRs, 1));
    var profile = repository.profile();
    assertThat(profile).isPresent();
    assertThat(profile.get().goldenSetVersionId()).isEqualTo(golden);
    assertThat(profile.get().rubricVersionId()).isEqualTo(rubric);

    when(jdbc.query(contains("from llm.institutional_calibration_profiles"), any(RowMapper.class)))
        .thenReturn(List.of());
    assertThat(repository.profile()).isEmpty();
  }

  @Test void profileRequiresPublishedPlatformVersions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID golden = UUID.randomUUID(), rubric = UUID.randomUUID(), actor = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(golden), eq(rubric))).thenReturn(null);
    assertThatThrownBy(() -> repository.profile(golden, rubric, actor))
        .isInstanceOf(IllegalStateException.class);

    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(golden), eq(rubric))).thenReturn(0);
    assertThatThrownBy(() -> repository.profile(golden, rubric, actor))
        .isInstanceOf(IllegalStateException.class);

    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(golden), eq(rubric))).thenReturn(1);
    repository.profile(golden, rubric, actor);
    verify(jdbc).update(anyString(), eq(golden), eq(rubric), eq(actor));
  }

  @Test void executionBuildsTheFullContext() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    UUID runId = UUID.randomUUID(), caseId = UUID.randomUUID();
    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(runId, UUID.randomUUID(),
        "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null,
        "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(run, 1));

    UUID deploymentId = UUID.randomUUID(), credentialId = UUID.randomUUID();
    ResultSet deploymentRs = mock(ResultSet.class);
    when(deploymentRs.getObject(1, UUID.class)).thenReturn(deploymentId);
    when(deploymentRs.getObject(2, UUID.class)).thenReturn(credentialId);
    when(deploymentRs.getString(3)).thenReturn("openai-compatible");
    when(deploymentRs.getString(4)).thenReturn("gpt-4o-mini");
    when(jdbc.query(contains("from llm.model_deployments d"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(deploymentRs, 1));

    doAnswer(invocation -> {
      RowCallbackHandler handler = invocation.getArgument(1);
      ResultSet dimensionRs = mock(ResultSet.class);
      when(dimensionRs.getString(1)).thenReturn("AUTONOMY");
      when(dimensionRs.getBigDecimal(5)).thenReturn(BigDecimal.valueOf(20));
      when(dimensionRs.getString(3)).thenReturn("Evidencia de autonomía");
      when(dimensionRs.getString(4)).thenReturn("alto/medio/bajo");
      handler.processRow(dimensionRs);
      return null;
    }).when(jdbc).query(contains("from llm.rubric_dimension_v2"), any(RowCallbackHandler.class),
        any(UUID.class));

    ResultSet caseRs = mock(ResultSet.class);
    when(caseRs.getObject(1, UUID.class)).thenReturn(caseId);
    when(caseRs.getString(2)).thenReturn("{\"role\":\"TUTOR\"}");
    when(caseRs.getString(3)).thenReturn("{\"topic\":\"colas\"}");
    when(caseRs.getString(4)).thenReturn("{\"AUTONOMY\":85,\"CLARITY\":80,\"PROGRESSION\":90,\"COMPLIANCE\":75,\"EFFICIENCY\":70}");
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(caseRs, 1));

    when(jdbc.query(contains("select calibration_seed"), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of(123L));

    var execution = repository.execution(runId);

    assertThat(execution.run()).isEqualTo(run);
    assertThat(execution.deployment().id()).isEqualTo(deploymentId);
    assertThat(execution.deployment().modelId()).isEqualTo("gpt-4o-mini");
    assertThat(execution.weights()).containsEntry(Dimension.AUTONOMY, 20);
    assertThat(execution.rubric()).contains("AUTONOMY").contains("peso 20");
    assertThat(execution.cases()).hasSize(1);
    assertThat(execution.cases().get(0).id()).isEqualTo(caseId);
    assertThat(execution.cases().get(0).humanScores()).containsEntry(Dimension.AUTONOMY, 85);
    assertThat(execution.seed()).isEqualTo(123L);
  }

  @Test void executionRebuildsWithANullSeedWhenNoneWasPersisted() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(UUID.randomUUID(),
        UUID.randomUUID(), "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(run, 1));
    ResultSet deploymentRs = stubDeployment();
    when(jdbc.query(contains("from llm.model_deployments d"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(deploymentRs, 1));
    doAnswer(invocation -> {
      RowCallbackHandler handler = invocation.getArgument(1);
      ResultSet dimensionRs = mock(ResultSet.class);
      when(dimensionRs.getString(1)).thenReturn("AUTONOMY");
      when(dimensionRs.getBigDecimal(5)).thenReturn(BigDecimal.valueOf(20));
      when(dimensionRs.getString(3)).thenReturn("Evidencia de autonomía");
      when(dimensionRs.getString(4)).thenReturn("alto/medio/bajo");
      handler.processRow(dimensionRs);
      return null;
    }).when(jdbc).query(contains("from llm.rubric_dimension_v2"), any(RowCallbackHandler.class),
        any(UUID.class));
    ResultSet caseRs = mock(ResultSet.class);
    when(caseRs.getObject(1, UUID.class)).thenReturn(UUID.randomUUID());
    when(caseRs.getString(2)).thenReturn("{\"role\":\"TUTOR\"}");
    when(caseRs.getString(3)).thenReturn("{\"topic\":\"colas\"}");
    when(caseRs.getString(4)).thenReturn("{\"AUTONOMY\":85,\"CLARITY\":80,\"PROGRESSION\":90,\"COMPLIANCE\":75,\"EFFICIENCY\":70}");
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(caseRs, 1));
    when(jdbc.query(contains("select calibration_seed"), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.execution(run.id()).seed()).isNull();
  }

  @Test void executionRejectsANonRunningRun() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(UUID.randomUUID(),
        UUID.randomUUID(), "COURSE", "QUEUED", 0, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(run, 1));
    assertThatThrownBy(() -> repository.execution(run.id()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void executionRejectsARunWithoutAnActiveCredential() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(UUID.randomUUID(),
        UUID.randomUUID(), "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(run, 1));
    when(jdbc.query(contains("from llm.model_deployments d"), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());
    assertThatThrownBy(() -> repository.execution(run.id()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void executionRejectsAGoldenSetWithoutCases() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(UUID.randomUUID(),
        UUID.randomUUID(), "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(run, 1));
    ResultSet deploymentRs = stubDeployment();
    when(jdbc.query(contains("from llm.model_deployments d"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(deploymentRs, 1));
    doAnswer(invocation -> {
      RowCallbackHandler handler = invocation.getArgument(1);
      ResultSet dimensionRs = mock(ResultSet.class);
      when(dimensionRs.getString(1)).thenReturn("AUTONOMY");
      when(dimensionRs.getBigDecimal(5)).thenReturn(BigDecimal.valueOf(20));
      when(dimensionRs.getString(3)).thenReturn("Evidencia de autonomía");
      when(dimensionRs.getString(4)).thenReturn("alto/medio/bajo");
      handler.processRow(dimensionRs);
      return null;
    }).when(jdbc).query(contains("from llm.rubric_dimension_v2"), any(RowCallbackHandler.class),
        any(UUID.class));
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThatThrownBy(() -> repository.execution(run.id()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void executionRejectsCorruptedGoldenSetData() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CalibrationRunRepository(jdbc, json);
    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(UUID.randomUUID(),
        UUID.randomUUID(), "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), null, null, "MANUAL", Instant.now(), null, null, null, null);
    when(jdbc.query(contains("where r.id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(runRows(run, 1));
    ResultSet deploymentRs = stubDeployment();
    when(jdbc.query(contains("from llm.model_deployments d"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(deploymentRs, 1));
    doAnswer(invocation -> {
      RowCallbackHandler handler = invocation.getArgument(1);
      ResultSet dimensionRs = mock(ResultSet.class);
      when(dimensionRs.getString(1)).thenReturn("AUTONOMY");
      when(dimensionRs.getBigDecimal(5)).thenReturn(BigDecimal.valueOf(20));
      when(dimensionRs.getString(3)).thenReturn("Evidencia de autonomía");
      when(dimensionRs.getString(4)).thenReturn("alto/medio/bajo");
      handler.processRow(dimensionRs);
      return null;
    }).when(jdbc).query(contains("from llm.rubric_dimension_v2"), any(RowCallbackHandler.class),
        any(UUID.class));
    ResultSet caseRs = mock(ResultSet.class);
    when(caseRs.getObject(1, UUID.class)).thenReturn(UUID.randomUUID());
    when(caseRs.getString(2)).thenReturn("not-json");
    when(caseRs.getString(3)).thenReturn("{\"topic\":\"colas\"}");
    when(caseRs.getString(4)).thenReturn("{\"AUTONOMY\":85,\"CLARITY\":80,\"PROGRESSION\":90,\"COMPLIANCE\":75,\"EFFICIENCY\":70}");
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rows(caseRs, 1));

    assertThatThrownBy(() -> repository.execution(run.id()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Datos de calibración inválidos");
  }

  private ResultSet stubDeployment() throws SQLException {
    ResultSet deploymentRs = mock(ResultSet.class);
    when(deploymentRs.getObject(1, UUID.class)).thenReturn(UUID.randomUUID());
    when(deploymentRs.getObject(2, UUID.class)).thenReturn(UUID.randomUUID());
    when(deploymentRs.getString(3)).thenReturn("openai-compatible");
    when(deploymentRs.getString(4)).thenReturn("gpt-4o-mini");
    return deploymentRs;
  }
}