package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read models for the teacher's course-scoped evaluator overview. */
@Repository
public class CourseEvaluationStatusRepository {
  private final JdbcTemplate jdbc;
  public CourseEvaluationStatusRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public Optional<ActiveCalibration> activeCalibration(UUID courseId) {
    return jdbc.query("select course_id, calibration_run_id, activated_at from llm.active_calibrations where course_id = ?", (rs, row) -> new ActiveCalibration(rs.getObject("course_id", UUID.class), rs.getObject("calibration_run_id", UUID.class), rs.getObject("activated_at", OffsetDateTime.class)), courseId).stream().findFirst();
  }
  public List<ChallengeAssignment> assignments(UUID courseId) {
    return jdbc.query("select challenge_id, calibration_run_id, locked_at from llm.challenge_calibration_assignments where course_id = ? order by assigned_at desc", (rs, row) -> new ChallengeAssignment(rs.getObject("challenge_id", UUID.class), rs.getObject("calibration_run_id", UUID.class), rs.getObject("locked_at", OffsetDateTime.class)), courseId);
  }
  public List<PendingEvaluation> pendingEvaluations(UUID courseId) {
    return jdbc.query("""
        select pending.id, pending.attempt_id, pending.assignment_challenge_id, pending.calibration_run_id,
               pending.state::text as state, pending.reason, pending.queued_at
        from llm.pending_evaluations pending
        join llm.challenge_calibration_assignments assignment on assignment.challenge_id = pending.assignment_challenge_id
        where assignment.course_id = ? order by pending.queued_at desc
        """, (rs, row) -> new PendingEvaluation(rs.getObject("id", UUID.class), rs.getObject("attempt_id", UUID.class), rs.getObject("assignment_challenge_id", UUID.class), rs.getObject("calibration_run_id", UUID.class), rs.getString("state"), rs.getString("reason"), rs.getObject("queued_at", OffsetDateTime.class)), courseId);
  }
  public record ActiveCalibration(UUID courseId, UUID calibrationRunId, OffsetDateTime activatedAt) {}
  public record ChallengeAssignment(UUID challengeId, UUID calibrationRunId, OffsetDateTime lockedAt) { public boolean locked() { return lockedAt != null; } }
  public record PendingEvaluation(UUID id, UUID attemptId, UUID assignmentId, UUID calibrationRunId, String state, String reason, OffsetDateTime queuedAt) {}
}
