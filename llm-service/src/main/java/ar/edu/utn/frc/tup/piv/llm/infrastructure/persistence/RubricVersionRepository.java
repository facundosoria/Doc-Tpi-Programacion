package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator.DimensionDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistence boundary for course-scoped rubric versions. */
@Repository
public class RubricVersionRepository {
  private final JdbcTemplate jdbc;
  public RubricVersionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<DimensionDefinition> dimensionsOfDraft(UUID courseId, UUID versionId) {
    return jdbc.query("""
        select d.dimension_key, d.weight from llm.rubric_dimension_v2 d
        join llm.rubric_version_v2 v on v.id = d.rubric_version_id join llm.rubric_families f on f.id = v.family_id
        where v.id = ? and f.course_id = ? and f.scope = 'COURSE' and v.state = 'DRAFT' order by d.dimension_key
        """, (rs, row) -> new DimensionDefinition(Dimension.valueOf(rs.getString("dimension_key")), rs.getObject("weight", BigDecimal.class)), versionId, courseId);
  }

  public boolean publishDraft(UUID courseId, UUID versionId) {
    return jdbc.update("""
        update llm.rubric_version_v2 v set state = 'PUBLISHED', published_at = now() from llm.rubric_families f
        where v.family_id = f.id and v.id = ? and f.course_id = ? and f.scope = 'COURSE' and v.state = 'DRAFT'
        """, versionId, courseId) == 1;
  }

  public List<RubricVersion> list(UUID courseId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, f.name, v.state::text as state, v.revision
        from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.scope = 'COURSE' order by f.name, v.version_no desc
        """, (rs, row) -> versionRow(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class), rs.getInt("version_no"),
        rs.getString("name"), rs.getString("state"), rs.getLong("revision")), courseId);
  }

  public Optional<RubricVersion> find(UUID courseId, UUID versionId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, f.name, v.state::text as state, v.revision
        from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.scope = 'COURSE' and v.id = ?
        """, (rs, row) -> versionRow(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class), rs.getInt("version_no"),
        rs.getString("name"), rs.getString("state"), rs.getLong("revision")), courseId, versionId).stream().findFirst();
  }

  public RubricVersion createDraft(UUID courseId, RubricInput input, UUID actorId) {
    UUID familyId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    jdbc.update("insert into llm.rubric_families (id, scope, course_id, name, next_version, created_by_user_id) values (?, 'COURSE', ?, ?, 2, ?)", familyId, courseId, input.name(), actorId);
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, created_by_user_id) values (?, ?, 1, ?)", versionId, familyId, actorId);
    replaceDimensions(versionId, input.dimensions());
    return new RubricVersion(versionId, familyId, 1, input.name(), "DRAFT", 1, input.dimensions());
  }

  /** Copies only a published version, reserves the family sequence atomically, and returns a mutable draft. */
  public Optional<RubricVersion> createNextDraft(UUID courseId, UUID publishedVersionId, UUID actorId) {
    var reservation = jdbc.query("""
        update llm.rubric_families f set next_version = f.next_version + 1
        from llm.rubric_version_v2 source
        where source.family_id = f.id and source.id = ? and source.state = 'PUBLISHED'
          and f.course_id = ? and f.scope = 'COURSE'
        returning f.id, f.name, f.next_version - 1 as version_no
        """, (rs, row) -> new FamilyVersion(rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("version_no")), publishedVersionId, courseId);
    if (reservation.isEmpty()) return Optional.empty();
    FamilyVersion family = reservation.getFirst(); UUID newVersionId = UUID.randomUUID();
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, based_on_version_id, created_by_user_id) values (?, ?, ?, ?, ?)",
        newVersionId, family.id(), family.version(), publishedVersionId, actorId);
    jdbc.update("""
        insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, evaluator_prompt, weight)
        select ?, dimension_key, label, criterion, anchors, evaluator_prompt, weight
        from llm.rubric_dimension_v2 where rubric_version_id = ?
        """, newVersionId, publishedVersionId);
    return Optional.of(new RubricVersion(newVersionId, family.id(), family.version(), family.name(), "DRAFT", 1, dimensions(newVersionId)));
  }

  public boolean advanceRevision(UUID courseId, UUID versionId, long expectedRevision) {
    return jdbc.update("""
        update llm.rubric_version_v2 v set revision = revision + 1 from llm.rubric_families f
        where v.family_id = f.id and f.course_id = ? and f.scope = 'COURSE' and v.id = ? and v.state = 'DRAFT' and v.revision = ?
        """, courseId, versionId, expectedRevision) == 1;
  }

  public void replaceDimensions(UUID versionId, List<DimensionInput> dimensions) {
    jdbc.update("delete from llm.rubric_dimension_v2 where rubric_version_id = ?", versionId);
    for (var dimension : dimensions) jdbc.update("""
        insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, evaluator_prompt, weight)
        values (?, ?, ?, ?, cast(? as jsonb), ?, ?)
        """, versionId, dimension.key().name(), dimension.label(), dimension.criterion(), dimension.anchors(), dimension.evaluatorPrompt(), dimension.weight());
  }

  private RubricVersion versionRow(UUID id, UUID familyId, int version, String name, String state, long revision) { return new RubricVersion(id, familyId, version, name, state, revision, dimensions(id)); }
  private List<DimensionInput> dimensions(UUID versionId) {
    return jdbc.query("select dimension_key, label, criterion, anchors::text as anchors, evaluator_prompt, weight from llm.rubric_dimension_v2 where rubric_version_id = ? order by dimension_key",
        (rs, row) -> new DimensionInput(Dimension.valueOf(rs.getString("dimension_key")), rs.getString("label"), rs.getString("criterion"), rs.getString("anchors"), rs.getString("evaluator_prompt"), rs.getObject("weight", BigDecimal.class)), versionId);
  }
  private record FamilyVersion(UUID id, String name, int version) {}
}
