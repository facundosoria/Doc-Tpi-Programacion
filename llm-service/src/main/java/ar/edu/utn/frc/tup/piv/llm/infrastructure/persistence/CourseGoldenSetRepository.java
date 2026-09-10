package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Creates independent course Golden Sets from immutable platform versions. */
@Repository
public class CourseGoldenSetRepository {
  private final JdbcTemplate jdbc;
  public CourseGoldenSetRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Optional<CourseGoldenSetVersion> copyPublishedPlatformVersion(UUID courseId, UUID baseVersionId, UUID actorId) {
    var source = jdbc.query("""
        select v.id, f.name from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where v.id = ? and v.state = 'PUBLISHED' and f.scope = 'PLATFORM' and f.course_id is null
        """, (rs, row) -> new Source(rs.getObject("id", UUID.class), rs.getString("name")), baseVersionId);
    if (source.isEmpty()) return Optional.empty();
    Source base = source.getFirst(); UUID familyId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    jdbc.update("""
        insert into llm.golden_set_families (id, scope, course_id, name, next_version, created_by_user_id)
        values (?, 'COURSE', ?, ?, 2, ?)
        """, familyId, courseId, base.name(), actorId);
    jdbc.update("""
        insert into llm.golden_set_versions (id, family_id, version_no, based_on_version_id, created_by_user_id)
        values (?, ?, 1, ?, ?)
        """, versionId, familyId, base.id(), actorId);
    jdbc.update("""
        insert into llm.golden_set_cases
          (golden_set_version_id, case_order, review_state, transcript, challenge_context, safe_metadata, author, reference_scores, score_justifications)
        select ?, case_order, review_state, transcript, challenge_context, safe_metadata, author, reference_scores, score_justifications
        from llm.golden_set_cases where golden_set_version_id = ? order by case_order
        """, versionId, base.id());
    return Optional.of(new CourseGoldenSetVersion(versionId, familyId, 1, "DRAFT", base.id()));
  }

  public CourseGoldenSetVersion createDraft(UUID courseId, String name, UUID actorId) {
    UUID familyId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    jdbc.update("""
        insert into llm.golden_set_families (id, scope, course_id, name, next_version, created_by_user_id)
        values (?, 'COURSE', ?, ?, 2, ?)
        """, familyId, courseId, name, actorId);
    jdbc.update("""
        insert into llm.golden_set_versions (id, family_id, version_no, state, created_by_user_id)
        values (?, ?, 1, 'DRAFT', ?)
        """, versionId, familyId, actorId);
    return new CourseGoldenSetVersion(versionId, familyId, 1, "DRAFT", null);
  }

  public List<CourseGoldenSetView> list(UUID courseId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.state::text as state, v.based_on_version_id, f.name
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where f.scope = 'COURSE' and f.course_id = ? and f.deleted_at is null and v.deleted_at is null order by f.name, v.version_no desc
        """, (rs, row) -> new CourseGoldenSetView(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
        rs.getString("name"), rs.getInt("version_no"), rs.getString("state"), rs.getObject("based_on_version_id", UUID.class),
        cases(rs.getObject("id", UUID.class))), courseId);
  }

  private List<GoldenSetCaseSummary> cases(UUID versionId) {
    return jdbc.query("""
        select id, case_order, author, review_state from llm.golden_set_cases
        where golden_set_version_id = ? order by case_order
        """, (rs, row) -> new GoldenSetCaseSummary(rs.getObject("id", UUID.class), rs.getInt("case_order"),
        rs.getString("author"), rs.getString("review_state")), versionId);
  }

  public Optional<GoldenSetCaseSummary> addDraftCase(UUID courseId, UUID versionId, GoldenSetCaseInput input) {
    UUID id = UUID.randomUUID();
    int inserted = jdbc.update("""
        insert into llm.golden_set_cases (id, golden_set_version_id, case_order, transcript, challenge_context, safe_metadata, author, reference_scores, score_justifications)
        select ?, v.id, coalesce((select max(case_order) + 1 from llm.golden_set_cases where golden_set_version_id = v.id), 0), cast(? as jsonb), cast(? as jsonb), cast(? as jsonb), ?, cast(? as jsonb), cast(? as jsonb)
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where v.id = ? and v.state = 'DRAFT' and f.scope = 'COURSE' and f.course_id = ?
        """, id, input.transcript().toString(), input.challengeContext().toString(), input.metadata().toString(), input.author(), input.referenceScores().toString(), input.scoreJustifications().toString(), versionId, courseId);
    return inserted == 0 ? Optional.empty() : Optional.of(new GoldenSetCaseSummary(id, cases(versionId).size() - 1, input.author(), "DRAFT"));
  }

  public boolean publishDraft(UUID courseId, UUID versionId) {
    Integer casesCount = jdbc.queryForObject(
        "select count(*) from llm.golden_set_cases where golden_set_version_id = ?",
        Integer.class, versionId);
    if (casesCount == null || casesCount == 0) throw new IllegalStateException("Un Golden Set sin casos no puede publicarse");
    jdbc.update("""
        update llm.golden_set_versions set state = 'SUPERSEDED'
        where family_id = (select family_id from llm.golden_set_versions where id = ?)
          and state = 'PUBLISHED'
        """, versionId);
    return jdbc.update("""
        update llm.golden_set_versions v
        set state = 'PUBLISHED', published_at = now()
        from llm.golden_set_families f
        where v.family_id = f.id and v.id = ? and v.state = 'DRAFT' and f.course_id = ? and f.scope = 'COURSE'
        """, versionId, courseId) == 1;
  }

  public Optional<CourseGoldenSetVersion> createNextDraft(UUID courseId, UUID publishedVersionId, UUID actorId) {
    var familyInfo = jdbc.query("""
        select f.id as family_id, f.name, v.version_no
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where v.id = ? and v.state = 'PUBLISHED' and f.course_id = ? and f.scope = 'COURSE'
        """, (rs, row) -> new Source(rs.getObject("family_id", UUID.class), rs.getString("name")), publishedVersionId, courseId);
    if (familyInfo.isEmpty()) return Optional.empty();
    Source info = familyInfo.getFirst();
    Integer nextVersion = jdbc.queryForObject(
        "select coalesce(max(version_no), 0) + 1 from llm.golden_set_versions where family_id = ?",
        Integer.class, info.id());
    UUID newVersionId = UUID.randomUUID();
    jdbc.update("""
        insert into llm.golden_set_versions (id, family_id, version_no, state, based_on_version_id, created_by_user_id)
        values (?, ?, ?, 'DRAFT', ?, ?)
        """, newVersionId, info.id(), nextVersion, publishedVersionId, actorId);
    jdbc.update("""
        insert into llm.golden_set_cases
          (golden_set_version_id, case_order, review_state, transcript, challenge_context, safe_metadata, author, reference_scores, score_justifications)
        select ?, case_order, review_state, transcript, challenge_context, safe_metadata, author, reference_scores, score_justifications
        from llm.golden_set_cases where golden_set_version_id = ? order by case_order
        """, newVersionId, publishedVersionId);
    return Optional.of(new CourseGoldenSetVersion(newVersionId, info.id(), nextVersion, "DRAFT", publishedVersionId));
  }

  /** A conservative logical deletion: the target draft must never have been published or used. */
  public boolean softDeleteUnusedDraft(UUID courseId, UUID versionId, UUID actorId) {
    return jdbc.update("""
        update llm.golden_set_versions version
        set deleted_at = now(), deleted_by_user_id = ?
        from llm.golden_set_families family
        where version.family_id = family.id and version.id = ? and version.state = 'DRAFT'
          and version.deleted_at is null and family.deleted_at is null and family.scope = 'COURSE' and family.course_id = ?
          and not exists (select 1 from llm.calibration_runs run where run.golden_set_version_id = version.id)
          and not exists (select 1 from llm.active_calibrations active join llm.calibration_runs run on run.id = active.calibration_run_id where run.golden_set_version_id = version.id)
          and not exists (select 1 from llm.challenge_calibration_assignments assignment join llm.calibration_runs run on run.id = assignment.calibration_run_id where run.golden_set_version_id = version.id)
          and not exists (select 1 from llm.pending_evaluations pending join llm.calibration_runs run on run.id = pending.calibration_run_id where run.golden_set_version_id = version.id)
        """, actorId, versionId, courseId) == 1;
  }

  public record CourseGoldenSetVersion(UUID id, UUID familyId, int version, String state, UUID basedOnVersionId) {}
  public record CourseGoldenSetView(UUID id, UUID familyId, String name, int version, String state, UUID basedOnVersionId,
      List<GoldenSetCaseSummary> cases) {}
  public record GoldenSetCaseSummary(UUID id, int order, String author, String reviewState) {}
  public record GoldenSetCaseInput(JsonNode transcript, JsonNode challengeContext, JsonNode metadata, String author,
      JsonNode referenceScores, JsonNode scoreJustifications) {}
  private record Source(UUID id, String name) {}
}
