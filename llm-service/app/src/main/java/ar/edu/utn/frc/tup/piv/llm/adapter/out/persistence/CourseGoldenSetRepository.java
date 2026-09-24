package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetView;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseSummary;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetDetail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** Creates independent course Golden Sets from immutable platform versions. */
@Repository
public class CourseGoldenSetRepository {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  @Autowired
  public CourseGoldenSetRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
  public CourseGoldenSetRepository(JdbcTemplate jdbc) { this(jdbc, JSON); }

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

  public int countCases(UUID versionId) {
    Integer count = jdbc.queryForObject("select count(*) from llm.golden_set_cases where golden_set_version_id = ?", Integer.class, versionId);
    return count == null ? 0 : count;
  }

  public Optional<CourseGoldenSetView> find(UUID courseId, UUID versionId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.state::text as state, v.based_on_version_id, f.name
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where v.id = ? and f.scope = 'COURSE' and f.course_id = ? and f.deleted_at is null and v.deleted_at is null
        """, (rs, row) -> new CourseGoldenSetView(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
        rs.getString("name"), rs.getInt("version_no"), rs.getString("state"), rs.getObject("based_on_version_id", UUID.class),
        cases(rs.getObject("id", UUID.class))), versionId, courseId).stream().findFirst();
  }

  public Optional<GoldenSetDetail> findDetail(UUID courseId, UUID versionId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.state::text as state, v.based_on_version_id, f.name
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where v.id = ? and f.scope = 'COURSE' and f.course_id = ? and f.deleted_at is null and v.deleted_at is null
        """, (rs, row) -> new GoldenSetDetail(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
        rs.getString("name"), rs.getInt("version_no"), rs.getString("state"), rs.getObject("based_on_version_id", UUID.class),
        caseDetails(rs.getObject("id", UUID.class))), versionId, courseId).stream().findFirst();
  }

  private List<GoldenSetDetail.CaseItem> caseDetails(UUID versionId) {
    return jdbc.query("select id, case_order, transcript, reference_scores from llm.golden_set_cases where golden_set_version_id = ? order by case_order",
        (rs, row) -> new GoldenSetDetail.CaseItem(rs.getObject("id", UUID.class), rs.getInt("case_order"), jsonNode(rs.getString("transcript")), jsonNode(rs.getString("reference_scores"))), versionId);
  }

  public Optional<GoldenSetCaseSummary> addDraftCase(UUID courseId, UUID versionId, GoldenSetCaseInput input) {
    UUID id = UUID.randomUUID();
    String metadataJson = input.metadata() != null ? input.metadata().toString() : "{}";
    String justificationsJson = input.scoreJustifications() != null ? input.scoreJustifications().toString() : "{}";
    int inserted = jdbc.update("""
        insert into llm.golden_set_cases (id, golden_set_version_id, case_order, transcript, challenge_context, safe_metadata, author, reference_scores, score_justifications)
        select ?, v.id, coalesce((select max(case_order) + 1 from llm.golden_set_cases where golden_set_version_id = v.id), 0), cast(? as jsonb), cast(? as jsonb), cast(? as jsonb), ?, cast(? as jsonb), cast(? as jsonb)
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where v.id = ? and v.state = 'DRAFT' and f.scope = 'COURSE' and f.course_id = ?
        """, id, input.transcript().toString(), json(input.challengeContext()), jsonObject(input.metadata()), input.author(), input.referenceScores().toString(), jsonObject(input.scoreJustifications()), versionId, courseId);
    return inserted == 0 ? Optional.empty() : Optional.of(new GoldenSetCaseSummary(id, cases(versionId).size() - 1, input.author(), "DRAFT"));
  }

  public Optional<GoldenSetCaseSummary> updateDraftCase(UUID courseId, UUID versionId, UUID caseId, GoldenSetCaseInput input) {
    int updated = jdbc.update("""
        update llm.golden_set_cases c set transcript = cast(? as jsonb), challenge_context = cast(? as jsonb),
          safe_metadata = cast(? as jsonb), author = ?, reference_scores = cast(? as jsonb), score_justifications = cast(? as jsonb)
        from llm.golden_set_versions v join llm.golden_set_families f on f.id = v.family_id
        where c.id = ? and c.golden_set_version_id = v.id and v.id = ? and v.state = 'DRAFT'
          and f.scope = 'COURSE' and f.course_id = ?
        """, input.transcript().toString(), json(input.challengeContext()), jsonObject(input.metadata()), input.author(),
        input.referenceScores().toString(), jsonObject(input.scoreJustifications()), caseId, versionId, courseId);
    if (updated == 0) return Optional.empty();
    return jdbc.query("select id, case_order, author, review_state from llm.golden_set_cases where id = ?",
        (rs, row) -> new GoldenSetCaseSummary(rs.getObject("id", UUID.class), rs.getInt("case_order"), rs.getString("author"), rs.getString("review_state")), caseId).stream().findFirst();
  }

  private String json(JsonNode value) { return value == null || value.isNull() ? "null" : value.toString(); }
  private String jsonObject(JsonNode value) { return value == null || value.isNull() ? "{}" : value.toString(); }
  private JsonNode jsonNode(String value) { try { return JSON.readTree(value); } catch (Exception exception) { throw new IllegalStateException("No se pudo leer un caso Golden Set", exception); } }

  public boolean publishDraft(UUID courseId, UUID versionId) {
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

  /** Los casos de una versión (publicada o no) para correr una calibración —
   * `transcript`/`challengeContext`/`referenceScores` tal cual quedaron persistidos. */
  public List<ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseDetail> casesOf(UUID goldenSetVersionId) {
    return jdbc.query("""
        select id, transcript, challenge_context, reference_scores from llm.golden_set_cases
        where golden_set_version_id = ? order by case_order
        """, (rs, row) -> new ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseDetail(rs.getObject("id", UUID.class),
        readTree(rs.getString("transcript")), readTree(rs.getString("challenge_context")),
        readTree(rs.getString("reference_scores"))), goldenSetVersionId);
  }

  private JsonNode readTree(String json) {
    try { return mapper.readTree(json); }
    catch (Exception exception) { throw new IllegalStateException("No se pudo leer un caso del golden set", exception); }
  }

  private record Source(UUID id, String name) {}
}
