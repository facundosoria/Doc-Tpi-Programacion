package ar.edu.utn.frc.tup.piv.llm.shadow.infrastructure;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowRunStore;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** JDBC de {@code shadow_runs} / {@code shadow_case_results}: las únicas tablas que escribe el shadow. */
@Repository
public class JdbcShadowRunStore implements ShadowRunStore {
  private static final String COLUMNS = """
      id, course_id, baseline_rubric_version_id, candidate_rubric_version_id, source, golden_set_version_id,
      sample_size, divergence_threshold, state, progress, summary::text as summary, failure_code,
      created_by_user_id, created_at, started_at, finished_at""";
  /** Una corrida RUNNING que no avanza (el servicio se cayó a mitad) se reclama de nuevo pasado este plazo. */
  private static final String STALE_RUNNING = "interval '15 minutes'";

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final RowMapper<ShadowRun> rows = (rs, n) -> new ShadowRun(
      rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
      rs.getObject("baseline_rubric_version_id", UUID.class), rs.getObject("candidate_rubric_version_id", UUID.class),
      ShadowRun.Source.valueOf(rs.getString("source")), rs.getObject("golden_set_version_id", UUID.class),
      rs.getInt("sample_size"), rs.getBigDecimal("divergence_threshold").doubleValue(),
      ShadowRun.State.valueOf(rs.getString("state")), rs.getInt("progress"), readSummary(rs.getString("summary")),
      rs.getString("failure_code"), rs.getObject("created_by_user_id", UUID.class),
      rs.getObject("created_at", OffsetDateTime.class), rs.getObject("started_at", OffsetDateTime.class),
      rs.getObject("finished_at", OffsetDateTime.class));

  public JdbcShadowRunStore(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public ShadowRun create(NewRun run) {
    jdbc.update("""
        insert into llm.shadow_runs (course_id, baseline_rubric_version_id, candidate_rubric_version_id, source,
          golden_set_version_id, sample_size, divergence_threshold, idempotency_key, created_by_user_id)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict (idempotency_key) do nothing
        """, run.courseId(), run.baselineRubricVersionId(), run.candidateRubricVersionId(), run.source().name(),
        run.goldenSetVersionId(), run.sampleSize(), run.divergenceThreshold(), run.idempotencyKey(), run.createdByUserId());
    return jdbc.queryForObject("select " + COLUMNS + " from llm.shadow_runs where idempotency_key = ?", rows, run.idempotencyKey());
  }

  @Override
  public Optional<ShadowRun> find(UUID courseId, UUID runId) {
    return jdbc.query("select " + COLUMNS + " from llm.shadow_runs where course_id = ? and id = ?", rows, courseId, runId)
        .stream().findFirst();
  }

  @Override
  public Optional<ShadowRun> findById(UUID runId) {
    return jdbc.query("select " + COLUMNS + " from llm.shadow_runs where id = ?", rows, runId).stream().findFirst();
  }

  @Override
  public List<ShadowRun> list(UUID courseId) {
    return jdbc.query("select " + COLUMNS + " from llm.shadow_runs where course_id = ? order by created_at desc limit 100",
        rows, courseId);
  }

  @Override
  public Optional<ShadowRun> claimNextQueued() {
    return jdbc.query("""
        update llm.shadow_runs set state = 'RUNNING', started_at = now(), progress = 0
        where id = (
          select id from llm.shadow_runs
          where state = 'QUEUED' or (state = 'RUNNING' and started_at < now() - %s)
          order by created_at for update skip locked limit 1)
        returning %s
        """.formatted(STALE_RUNNING, COLUMNS), rows).stream().findFirst();
  }

  @Override
  public void recordProgress(UUID runId, int progress) {
    jdbc.update("update llm.shadow_runs set progress = ? where id = ? and state = 'RUNNING'", progress, runId);
  }

  @Override
  public void recordCase(UUID runId, ShadowMetrics.CaseOutcome outcome) {
    // upsert: si la corrida se reclama de nuevo tras una caída, no duplica ni rompe la unicidad.
    jdbc.update("""
        insert into llm.shadow_case_results (shadow_run_id, source_ref, baseline_scores, candidate_scores, human_scores, error_code)
        values (?, ?, cast(? as jsonb), cast(? as jsonb), cast(? as jsonb), ?)
        on conflict (shadow_run_id, source_ref) do update set baseline_scores = excluded.baseline_scores,
          candidate_scores = excluded.candidate_scores, human_scores = excluded.human_scores, error_code = excluded.error_code
        """, runId, outcome.sourceRef(), scoresJson(outcome.baseline()), scoresJson(outcome.candidate()),
        scoresJson(outcome.human()), outcome.errorCode());
  }

  @Override
  public void complete(UUID runId, ShadowMetrics.Summary summary) {
    jdbc.update("update llm.shadow_runs set state = 'COMPLETED', progress = 100, finished_at = now(), summary = cast(? as jsonb) "
        + "where id = ? and state = 'RUNNING'", write(summary), runId);
  }

  @Override
  public void fail(UUID runId, String failureCode) {
    jdbc.update("update llm.shadow_runs set state = 'FAILED', finished_at = now(), failure_code = ? "
        + "where id = ? and state = 'RUNNING'", failureCode, runId);
  }

  @Override
  public Optional<UUID> activeRubricVersion(UUID courseId) {
    return jdbc.queryForList("""
        select r.rubric_version_id from llm.active_calibrations a
        join llm.calibration_runs r on r.id = a.calibration_run_id where a.course_id = ?
        """, UUID.class, courseId).stream().findFirst();
  }

  @Override
  public boolean rubricVersionVisibleToCourse(UUID courseId, UUID rubricVersionId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        select exists (select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
          where v.id = ? and ((f.scope = 'PLATFORM' and v.state in ('PUBLISHED', 'SUPERSEDED')) or f.course_id = ?))
        """, Boolean.class, rubricVersionId, courseId));
  }

  @Override
  public boolean goldenSetVersionVisibleToCourse(UUID courseId, UUID goldenSetVersionId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        select exists (select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
          where v.id = ? and ((f.scope = 'PLATFORM' and v.state in ('PUBLISHED', 'SUPERSEDED')) or f.course_id = ?))
        """, Boolean.class, goldenSetVersionId, courseId));
  }

  @Override
  public int purgeFinishedBefore(int days) {
    return jdbc.update("delete from llm.shadow_runs where finished_at is not null and finished_at < now() - make_interval(days => ?)", days);
  }

  private String scoresJson(Map<Dimension, Integer> scores) {
    if (scores == null) {
      return null;
    }
    var node = mapper.createObjectNode();
    scores.forEach((dimension, value) -> node.put(dimension.name().toLowerCase(Locale.ROOT), value));
    return node.toString();
  }

  private String write(ShadowMetrics.Summary summary) {
    try {
      return mapper.writeValueAsString(summary);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo serializar el resumen del shadow", e);
    }
  }

  private ShadowMetrics.Summary readSummary(String json) {
    if (json == null) {
      return null;
    }
    try {
      return mapper.readValue(json, ShadowMetrics.Summary.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Resumen del shadow ilegible", e);
    }
  }
}
