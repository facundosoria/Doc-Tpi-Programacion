package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService.QueuedEvaluation;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcCalibrationWorkflowStoreCoverageTest {

  @Test void transitionsARunBetweenStatesWhenTheRowIsUpdated() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID id = UUID.randomUUID();
    when(jdbc.update(anyString(), eq("RUNNING"), eq("RUNNING"), eq("RUNNING"), eq(id), eq("QUEUED")))
        .thenReturn(1);

    assertThat(store.transition(id, CalibrationState.QUEUED, CalibrationState.RUNNING)).isTrue();
    verify(jdbc).update(contains("update llm.calibration_runs set state"), eq("RUNNING"),
        eq("RUNNING"), eq("RUNNING"), eq(id), eq("QUEUED"));
  }

  @Test void reportsARejectedTransition() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(0);
    assertThat(store.transition(UUID.randomUUID(), CalibrationState.RUNNING, CalibrationState.PASSED))
        .isFalse();
  }

  @Test void answersWhetherABareRunIsPassed() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID id = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(id))).thenReturn(true);
    assertThat(store.isPassed(id)).isTrue();
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(id))).thenReturn(false);
    assertThat(store.isPassed(id)).isFalse();
  }

  @Test void answersWhetherARunIsPassedWithinACourse() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID course = UUID.randomUUID(), id = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(id), eq(course))).thenReturn(true);
    assertThat(store.isPassed(course, id)).isTrue();
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(id), eq(course))).thenReturn(false);
    assertThat(store.isPassed(course, id)).isFalse();
  }

  @Test void activatesACalibrationAndAuditsIt() {
    var jdbc = mock(JdbcTemplate.class);
    var audit = mock(AuditRepository.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, audit);
    UUID course = UUID.randomUUID(), run = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req-3", "trace-3");
    store.activate(course, run, actor);
    verify(jdbc).update(anyString(), eq(course), eq(run), eq(actor.delegatedUserId()));
    verify(audit).record(eq("calibration.activated"), eq("calibration-run"), eq(run), eq(actor), anyString());
  }

  @Test void answersWhetherAChallengeHasAValidCalibration() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID challenge = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(challenge))).thenReturn(true);
    assertThat(store.hasValidCalibration(challenge)).isTrue();
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(challenge))).thenReturn(false);
    assertThat(store.hasValidCalibration(challenge)).isFalse();
  }

  @Test void enqueuesTheSnapshottedAssignedCalibration() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID attempt = UUID.randomUUID(), challenge = UUID.randomUUID(), key = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);
    store.enqueue(attempt, challenge, key);
    verify(jdbc).update(contains("pending_evaluations"), eq(attempt), eq(key), eq(challenge));
  }

  @Test void claimsTheNextQueuedEvaluation() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID id = UUID.randomUUID(), challenge = UUID.randomUUID(), run = UUID.randomUUID();
    when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
      RowMapper<?> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getString(1)).thenReturn(id.toString());
      when(rs.getString(2)).thenReturn(challenge.toString());
      when(rs.getString(3)).thenReturn(run.toString());
      return List.of(mapper.mapRow(rs, 0));
    });

    Optional<QueuedEvaluation> claimed = store.claimNextQueued();

    assertThat(claimed).isPresent();
    assertThat(claimed.get().id()).isEqualTo(id);
    assertThat(claimed.get().challengeId()).isEqualTo(challenge);
    assertThat(claimed.get().calibrationRunId()).isEqualTo(run);
  }

  @Test void returnsEmptyWhenNothingIsQueued() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
    assertThat(store.claimNextQueued()).isEmpty();
  }

  @Test void marksARunningEvaluationOnlyWhenQueued() {
    var jdbc = mock(JdbcTemplate.class);
    var store = new JdbcCalibrationWorkflowStore(jdbc, mock(AuditRepository.class));
    UUID id = UUID.randomUUID();
    when(jdbc.update(anyString(), eq(id))).thenReturn(1);
    assertThat(store.markRunning(id)).isTrue();
    when(jdbc.update(anyString(), eq(id))).thenReturn(0);
    assertThat(store.markRunning(id)).isFalse();
  }
}