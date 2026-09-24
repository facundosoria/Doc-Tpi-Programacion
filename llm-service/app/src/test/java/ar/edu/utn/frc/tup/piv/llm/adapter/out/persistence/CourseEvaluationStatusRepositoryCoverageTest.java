package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ActiveCalibration;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ChallengeAssignment;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.PendingEvaluation;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CourseEvaluationStatusRepositoryCoverageTest {
  private final UUID courseId = UUID.randomUUID();

  @Test
  void readsTheActiveCalibrationThroughTheRowMapper() {
    var jdbc = mock(JdbcTemplate.class);
    UUID calibrationRunId = UUID.randomUUID();
    OffsetDateTime activatedAt = OffsetDateTime.now();
    when(jdbc.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation -> {
      RowMapper<ActiveCalibration> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getObject("course_id", UUID.class)).thenReturn(courseId);
      when(rs.getObject("calibration_run_id", UUID.class)).thenReturn(calibrationRunId);
      when(rs.getObject("activated_at", OffsetDateTime.class)).thenReturn(activatedAt);
      return List.of(mapper.mapRow(rs, 0));
    });
    var repository = new CourseEvaluationStatusRepository(jdbc);

    var result = repository.activeCalibration(courseId);

    assertThat(result)
        .contains(new ActiveCalibration(courseId, calibrationRunId, activatedAt));
  }

  @Test
  void activeCalibrationIsEmptyWhenNoRowComesBack() {
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());
    var repository = new CourseEvaluationStatusRepository(jdbc);

    assertThat(repository.activeCalibration(courseId)).isEmpty();
  }

  @Test
  void mapsChallengeAssignmentsAndTheirLockState() {
    var jdbc = mock(JdbcTemplate.class);
    UUID challengeId = UUID.randomUUID();
    UUID calibrationRunId = UUID.randomUUID();
    OffsetDateTime lockedAt = OffsetDateTime.now();
    when(jdbc.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation -> {
      RowMapper<ChallengeAssignment> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getObject("challenge_id", UUID.class)).thenReturn(challengeId);
      when(rs.getObject("calibration_run_id", UUID.class)).thenReturn(calibrationRunId);
      when(rs.getObject("locked_at", OffsetDateTime.class)).thenReturn(lockedAt);
      return List.of(mapper.mapRow(rs, 0));
    });
    var repository = new CourseEvaluationStatusRepository(jdbc);

    var assignments = repository.assignments(courseId);

    assertThat(assignments).hasSize(1);
    assertThat(assignments.getFirst().locked()).isTrue();
  }

  @Test
  void anUnlockedAssignmentReportsFalse() {
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation -> {
      RowMapper<ChallengeAssignment> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getObject("challenge_id", UUID.class)).thenReturn(UUID.randomUUID());
      when(rs.getObject("calibration_run_id", UUID.class)).thenReturn(UUID.randomUUID());
      when(rs.getObject("locked_at", OffsetDateTime.class)).thenReturn(null);
      return List.of(mapper.mapRow(rs, 0));
    });
    var repository = new CourseEvaluationStatusRepository(jdbc);

    assertThat(repository.assignments(courseId).getFirst().locked()).isFalse();
  }

  @Test
  void mapsPendingEvaluationsIncludingStateAndReason() {
    var jdbc = mock(JdbcTemplate.class);
    UUID id = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID calibrationRunId = UUID.randomUUID();
    OffsetDateTime queuedAt = OffsetDateTime.now();
    when(jdbc.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation -> {
      RowMapper<PendingEvaluation> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getObject("id", UUID.class)).thenReturn(id);
      when(rs.getObject("attempt_id", UUID.class)).thenReturn(attemptId);
      when(rs.getObject("assignment_challenge_id", UUID.class)).thenReturn(assignmentId);
      when(rs.getObject("calibration_run_id", UUID.class)).thenReturn(calibrationRunId);
      when(rs.getString("state")).thenReturn("QUEUED");
      when(rs.getString("reason")).thenReturn("sin calibración");
      when(rs.getObject("queued_at", OffsetDateTime.class)).thenReturn(queuedAt);
      return List.of(mapper.mapRow(rs, 0));
    });
    var repository = new CourseEvaluationStatusRepository(jdbc);

    var pending = repository.pendingEvaluations(courseId).getFirst();

    assertThat(pending.state()).isEqualTo("QUEUED");
    assertThat(pending.reason()).isEqualTo("sin calibración");
    assertThat(pending.queuedAt()).isEqualTo(queuedAt);
  }
}