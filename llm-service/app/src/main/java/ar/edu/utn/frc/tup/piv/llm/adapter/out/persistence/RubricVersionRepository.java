package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionCustomInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchors;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator.DimensionDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistence boundary for course-scoped rubric versions. */
@Repository
public class RubricVersionRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  public RubricVersionRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

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
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.scope = 'COURSE' order by v.name, v.version_no desc
        """, (rs, row) -> versionRow(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class), rs.getInt("version_no"),
        rs.getString("name"), rs.getString("state"), rs.getLong("revision"), rs.getObject("template_origin_version_id", UUID.class)), courseId);
  }

  public List<RubricVersion> listTemplates() {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where f.scope = 'PLATFORM' and f.course_id is null order by v.name, v.version_no desc
        """, (rs, row) -> versionRow(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class), rs.getInt("version_no"),
        rs.getString("name"), rs.getString("state"), rs.getLong("revision"), rs.getObject("template_origin_version_id", UUID.class)));
  }

  public Optional<RubricVersion> findTemplate(UUID versionId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where f.scope = 'PLATFORM' and f.course_id is null and v.id = ?
        """, (rs, row) -> versionRow(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class), rs.getInt("version_no"),
        rs.getString("name"), rs.getString("state"), rs.getLong("revision"), rs.getObject("template_origin_version_id", UUID.class)), versionId).stream().findFirst();
  }

  public RubricVersion createTemplateDraft(RubricInput input, UUID actorId) {
    UUID familyId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    jdbc.update("insert into llm.rubric_families (id, scope, next_version, created_by_user_id) values (?, 'PLATFORM', 2, ?)", familyId, actorId);
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, name, created_by_user_id) values (?, ?, 1, ?, ?)", versionId, familyId, input.name(), actorId);
    replaceDimensions(versionId, input.dimensions());
    return versionRow(versionId, familyId, 1, input.name(), "DRAFT", 1, null);
  }

  public boolean advanceTemplateRevision(UUID versionId, long expectedRevision) {
    return jdbc.update("""
        update llm.rubric_version_v2 v set revision = revision + 1 from llm.rubric_families f
        where v.family_id = f.id and f.scope = 'PLATFORM' and v.id = ? and v.state = 'DRAFT' and v.revision = ?
        """, versionId, expectedRevision) == 1;
  }

  public boolean publishTemplateDraft(UUID versionId) {
    return jdbc.update("""
        update llm.rubric_version_v2 v set state = 'PUBLISHED', published_at = now() from llm.rubric_families f
        where v.family_id = f.id and f.scope = 'PLATFORM' and v.id = ? and v.state = 'DRAFT'
        """, versionId) == 1;
  }

  public Optional<RubricVersion> createNextTemplateDraft(UUID publishedVersionId, UUID actorId) {
    var source = findTemplate(publishedVersionId).filter(version -> "PUBLISHED".equals(version.state()));
    if (source.isEmpty()) return Optional.empty();
    var current = source.get(); UUID versionId = UUID.randomUUID();
    int version = jdbc.queryForObject("select coalesce(max(version_no), 0) + 1 from llm.rubric_version_v2 where family_id = ?", Integer.class, current.familyId());
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, name, based_on_version_id, created_by_user_id) values (?, ?, ?, ?, ?, ?)", versionId, current.familyId(), version, current.name(), publishedVersionId, actorId);
    jdbc.update("""
        insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, weight)
        select ?, dimension_key, label, criterion, anchors, weight from llm.rubric_dimension_v2 where rubric_version_id = ?
        """, versionId, publishedVersionId);
    return Optional.of(versionRow(versionId, current.familyId(), version, current.name(), "DRAFT", 1, null));
  }

  public Optional<RubricVersion> find(UUID courseId, UUID versionId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.scope = 'COURSE' and v.id = ?
        """, (rs, row) -> versionRow(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class), rs.getInt("version_no"),
        rs.getString("name"), rs.getString("state"), rs.getLong("revision"), rs.getObject("template_origin_version_id", UUID.class)), courseId, versionId).stream().findFirst();
  }

  public Optional<RubricVersion> createDraftFromPublishedTemplate(UUID courseId, UUID templateVersionId, String name, UUID actorId) {
    var template = jdbc.query("""
        select v.id from llm.rubric_version_v2 v join llm.rubric_families f on f.id = v.family_id
        where v.id = ? and v.state = 'PUBLISHED' and f.scope = 'PLATFORM' and f.course_id is null
        """, (rs, row) -> rs.getObject("id", UUID.class), templateVersionId);
    if (template.isEmpty()) return Optional.empty();
    var source = template.getFirst(); UUID familyId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    jdbc.update("insert into llm.rubric_families (id, scope, course_id, next_version, created_by_user_id) values (?, 'COURSE', ?, 2, ?)", familyId, courseId, actorId);
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, name, template_origin_version_id, created_by_user_id) values (?, ?, 1, ?, ?, ?)", versionId, familyId, name, source, actorId);
    jdbc.update("""
        insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, weight)
        select ?, dimension_key, label, criterion, anchors, weight from llm.rubric_dimension_v2 where rubric_version_id = ?
        """, versionId, source);
    return Optional.of(versionRow(versionId, familyId, 1, name, "DRAFT", 1, source));
  }

  /** Copies only a published version, reserves the family sequence atomically, and returns a mutable draft. */
  public Optional<RubricVersion> createNextDraft(UUID courseId, UUID publishedVersionId, UUID actorId) {
    var reservation = jdbc.query("""
        update llm.rubric_families f set next_version = f.next_version + 1
        from llm.rubric_version_v2 source
        where source.family_id = f.id and source.id = ? and source.state = 'PUBLISHED'
          and f.course_id = ? and f.scope = 'COURSE'
        returning f.id, f.next_version - 1 as version_no
        """, (rs, row) -> new FamilyVersion(rs.getObject("id", UUID.class), rs.getInt("version_no")), publishedVersionId, courseId);
    if (reservation.isEmpty()) return Optional.empty();
    FamilyVersion family = reservation.getFirst(); UUID newVersionId = UUID.randomUUID();
    var source = find(courseId, publishedVersionId).orElseThrow();
    jdbc.update("insert into llm.rubric_version_v2 (id, family_id, version_no, name, based_on_version_id, created_by_user_id) values (?, ?, ?, ?, ?, ?)",
        newVersionId, family.id(), family.version(), source.name(), publishedVersionId, actorId);
    jdbc.update("""
        insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, weight)
        select ?, dimension_key, label, criterion, anchors, weight
        from llm.rubric_dimension_v2 where rubric_version_id = ?
        """, newVersionId, publishedVersionId);
    return Optional.of(versionRow(newVersionId, family.id(), family.version(), source.name(), "DRAFT", 1, null));
  }

  public boolean advanceRevision(UUID courseId, UUID versionId, long expectedRevision) {
    return jdbc.update("""
        update llm.rubric_version_v2 v set revision = revision + 1 from llm.rubric_families f
        where v.family_id = f.id and f.course_id = ? and f.scope = 'COURSE' and v.id = ? and v.state = 'DRAFT' and v.revision = ?
        """, courseId, versionId, expectedRevision) == 1;
  }

  public void updateVersionName(UUID courseId, UUID versionId, String name) {
    jdbc.update("""
        update llm.rubric_version_v2 v set name = ? from llm.rubric_families f
        where v.family_id = f.id and v.id = ? and f.course_id = ? and f.scope = 'COURSE' and v.state = 'DRAFT'
        """, name, versionId, courseId);
  }

  public void updateTemplateVersionName(UUID versionId, String name) {
    jdbc.update("""
        update llm.rubric_version_v2 v set name = ? from llm.rubric_families f
        where v.family_id = f.id and v.id = ? and f.scope = 'PLATFORM' and v.state = 'DRAFT'
        """, name, versionId);
  }

  public void replaceDimensions(UUID versionId, List<DimensionInput> dimensions) {
    jdbc.update("delete from llm.rubric_dimension_v2 where rubric_version_id = ?", versionId);
    for (var dimension : dimensions) jdbc.update("""
        insert into llm.rubric_dimension_v2 (rubric_version_id, dimension_key, label, criterion, anchors, weight)
        values (?, ?, ?, ?, cast(? as jsonb), ?)
        """, versionId, dimension.key().name(), dimension.label(), dimension.criterion(), serialize(dimension.anchors()), dimension.weight());
  }

  /** Pesos + evaluator_prompt de las 5 dimensiones de una versión de rúbrica — la calibración
   * siempre referencia una versión ya PUBLICADA (constraint FK), así que a diferencia de
   * {@link #dimensionsOfDraft} esto no filtra por curso ni por estado DRAFT. */
  public List<DimensionInput> weightsAndPrompts(UUID rubricVersionId) { return dimensions(rubricVersionId); }

  // === Métodos para overlays de desafío (Parte A) ===

  /** Lista todas las versiones de overlay para un desafío específico. */
  public List<RubricVersion> listByChallenge(UUID courseId, UUID challengeId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v
        join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.challenge_id = ? and f.scope = 'CHALLENGE'
        order by v.version_no desc
        """, (rs, row) -> versionRow(
            rs.getObject("id", UUID.class),
            rs.getObject("family_id", UUID.class),
            rs.getInt("version_no"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getLong("revision"),
            rs.getObject("template_origin_version_id", UUID.class)
        ), courseId, challengeId);
  }

  /** Lista todos los overlays (scope CHALLENGE) del curso, con su challenge_id. */
  public List<ChallengeRow> listOverlaysByCourse(UUID courseId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id,
               f.challenge_id
        from llm.rubric_version_v2 v
        join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.scope = 'CHALLENGE'
        order by f.challenge_id, v.version_no desc
        """, (rs, row) -> new ChallengeRow(
            rs.getObject("challenge_id", UUID.class),
            versionRow(
                rs.getObject("id", UUID.class),
                rs.getObject("family_id", UUID.class),
                rs.getInt("version_no"),
                rs.getString("name"),
                rs.getString("state"),
                rs.getLong("revision"),
                rs.getObject("template_origin_version_id", UUID.class)
            )
        ), courseId);
  }

  public record ChallengeRow(UUID challengeId, RubricVersion version) {}

  /** Busca una versión específica de overlay para un desafío. */
  public Optional<RubricVersion> findChallenge(UUID courseId, UUID challengeId, UUID versionId) {
    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v
        join llm.rubric_families f on f.id = v.family_id
        where f.course_id = ? and f.challenge_id = ? and f.scope = 'CHALLENGE' and v.id = ?
        """, (rs, row) -> versionRow(
            rs.getObject("id", UUID.class),
            rs.getObject("family_id", UUID.class),
            rs.getInt("version_no"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getLong("revision"),
            rs.getObject("template_origin_version_id", UUID.class)
        ), courseId, challengeId, versionId).stream().findFirst();
  }

  /** Crea un borrador de overlay para un desafío, vinculado a una rúbrica base del curso. */
  public Optional<RubricVersion> createDraftForChallenge(UUID courseId, UUID challengeId, String name,
      UUID baselineVersionId, UUID actorId) {
    UUID familyId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();

    // Crear la familia con scope CHALLENGE
    jdbc.update("""
        insert into llm.rubric_families (id, scope, course_id, challenge_id, next_version, created_by_user_id)
        values (?, 'CHALLENGE', ?, ?, 2, ?)
        """, familyId, courseId, challengeId, actorId);

    // Crear la versión del overlay con rubric_kind MODULAR_CUSTOM y baseline_version_id
    jdbc.update("""
        insert into llm.rubric_version_v2 (id, family_id, version_no, name, rubric_kind, user_prompt,
            baseline_version_id, created_by_user_id)
        values (?, ?, 1, ?, 'MODULAR_CUSTOM', '', ?, ?)
        """, versionId, familyId, name, baselineVersionId, actorId);

    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v where v.id = ?
        """, (rs, row) -> versionRow(
            rs.getObject("id", UUID.class),
            rs.getObject("family_id", UUID.class),
            rs.getInt("version_no"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getLong("revision"),
            rs.getObject("template_origin_version_id", UUID.class)
        ), versionId).stream().findFirst();
  }

  /** Avanza la revisión de un overlay (optimistic locking). */
  public boolean advanceChallengeRevision(UUID courseId, UUID challengeId, UUID versionId, long expectedRevision) {
    return jdbc.update("""
        update llm.rubric_version_v2 v
        set revision = revision + 1
        from llm.rubric_families f
        where v.family_id = f.id and f.course_id = ? and f.challenge_id = ? and f.scope = 'CHALLENGE'
          and v.id = ? and v.state = 'DRAFT' and v.revision = ?
        """, courseId, challengeId, versionId, expectedRevision) == 1;
  }

  /** Actualiza el nombre de una versión de overlay. */
  public void updateChallengeVersionName(UUID courseId, UUID challengeId, UUID versionId, String name) {
    jdbc.update("""
        update llm.rubric_version_v2 v
        set name = ?
        from llm.rubric_families f
        where v.family_id = f.id and f.course_id = ? and f.challenge_id = ? and f.scope = 'CHALLENGE'
          and v.id = ? and v.state = 'DRAFT'
        """, name, courseId, challengeId, versionId);
  }

  /** Actualiza el user_prompt de una versión de overlay. */
  public void updateChallengePrompt(UUID courseId, UUID challengeId, UUID versionId, String userPrompt) {
    jdbc.update("""
        update llm.rubric_version_v2 v
        set user_prompt = ?
        from llm.rubric_families f
        where v.family_id = f.id and f.course_id = ? and f.challenge_id = ? and f.scope = 'CHALLENGE'
          and v.id = ? and v.state = 'DRAFT'
        """, userPrompt, courseId, challengeId, versionId);
  }

  /** Reemplaza las dimensiones custom de un overlay. */
  public void replaceChallengeCustomDimensions(UUID versionId, List<DimensionCustomInput> dimensions) {
    jdbc.update("delete from llm.rubric_custom_dimensions where rubric_version_id = ?", versionId);
    for (int i = 0; i < dimensions.size(); i++) {
      var dimension = dimensions.get(i);
      jdbc.update("""
          insert into llm.rubric_custom_dimensions
              (rubric_version_id, dimension_key, label, criterion, anchors, weight, display_order)
          values (?, ?, ?, ?, cast(? as jsonb), ?, ?)
          """, versionId, dimension.key(), dimension.label(), dimension.criterion(),
          serialize(dimension.anchors()), dimension.weight(), i);
    }
  }

  /** Publica un borrador de overlay. */
  public boolean publishChallengeDraft(UUID courseId, UUID challengeId, UUID versionId) {
    return jdbc.update("""
        update llm.rubric_version_v2 v
        set state = 'PUBLISHED', published_at = now()
        from llm.rubric_families f
        where v.family_id = f.id and f.course_id = ? and f.challenge_id = ? and f.scope = 'CHALLENGE'
          and v.id = ? and v.state = 'DRAFT'
        """, courseId, challengeId, versionId) == 1;
  }

  /** Crea una nueva versión de overlay basada en una versión publicada. */
  public Optional<RubricVersion> createNextChallengeDraft(UUID courseId, UUID challengeId,
      UUID publishedVersionId, UUID actorId) {
    var published = findChallenge(courseId, challengeId, publishedVersionId);
    if (published.isEmpty() || !"PUBLISHED".equals(published.get().state())) {
      return Optional.empty();
    }

    UUID familyId = published.get().familyId();
    int nextVersion = jdbc.queryForObject("""
        select coalesce(max(version_no), 0) + 1
        from llm.rubric_version_v2 where family_id = ?
        """, Integer.class, familyId);

    UUID newVersionId = UUID.randomUUID();
    UUID baseline = baselineVersionIdOf(publishedVersionId);

    // Crear la nueva versión
    jdbc.update("""
        insert into llm.rubric_version_v2 (id, family_id, version_no, name, rubric_kind, user_prompt,
            baseline_version_id, based_on_version_id, created_by_user_id)
        values (?, ?, ?, ?, 'MODULAR_CUSTOM', '', ?, ?, ?)
        """, newVersionId, familyId, nextVersion, published.get().name(), baseline, publishedVersionId, actorId);

    // Copiar las dimensiones custom de la versión publicada
    jdbc.update("""
        insert into llm.rubric_custom_dimensions
            (rubric_version_id, dimension_key, label, criterion, anchors, weight, display_order)
        select ?, dimension_key, label, criterion, anchors, weight, display_order
        from llm.rubric_custom_dimensions
        where rubric_version_id = ?
        """, newVersionId, publishedVersionId);

    return jdbc.query("""
        select v.id, v.family_id, v.version_no, v.name, v.state::text as state, v.revision, v.template_origin_version_id
        from llm.rubric_version_v2 v where v.id = ?
        """, (rs, row) -> versionRow(
            rs.getObject("id", UUID.class),
            rs.getObject("family_id", UUID.class),
            rs.getInt("version_no"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getLong("revision"),
            rs.getObject("template_origin_version_id", UUID.class)
        ), newVersionId).stream().findFirst();
  }

  /** Obtiene las dimensiones custom de una versión de overlay. */
  public List<DimensionCustomInput> challengeCustomDimensions(UUID versionId) {
    return jdbc.query("""
        select dimension_key, label, criterion, anchors::text as anchors, weight
        from llm.rubric_custom_dimensions
        where rubric_version_id = ?
        order by display_order
""", (rs, row) -> new DimensionCustomInput(
            rs.getString("dimension_key"),
            rs.getString("label"),
            rs.getString("criterion"),
            deserialize(rs.getString("anchors")),
            rs.getObject("weight", BigDecimal.class)
        ), versionId);
  }

  private RubricVersion versionRow(UUID id, UUID familyId, int version, String name, String state, long revision,
      UUID templateOriginVersionId) { return new RubricVersion(id, familyId, version, name, state, revision, templateOriginVersionId, dimensions(id)); }
  /** Public accessor: 5 institutional dimensions of a rubric version (used by EffectiveRubricResolver). */
  public List<DimensionInput> dimensions(UUID versionId) {
    return jdbc.query("select dimension_key, label, criterion, anchors::text as anchors, weight from llm.rubric_dimension_v2 where rubric_version_id = ? order by dimension_key",
        (rs, row) -> new DimensionInput(Dimension.valueOf(rs.getString("dimension_key")), rs.getString("label"), rs.getString("criterion"), deserialize(rs.getString("anchors")), rs.getObject("weight", BigDecimal.class)), versionId);
  }

  /** Public accessor: baseline_version_id of a rubric version (used by ChallengeRubricOverlayService). */
  public UUID baselineVersionIdOf(UUID versionId) {
    return jdbc.query("select baseline_version_id from llm.rubric_version_v2 where id = ?",
        (rs, row) -> rs.getObject("baseline_version_id", UUID.class), versionId)
        .stream().filter(value -> value != null).findFirst().orElse(null);
  }

  /** Public accessor: rubric_kind of a rubric version. */
  public String rubricKindOf(UUID versionId) {
    return jdbc.query("select rubric_kind from llm.rubric_version_v2 where id = ?",
        (rs, row) -> rs.getString("rubric_kind"), versionId)
        .stream().findFirst().orElse("DEFAULT_INSTITUTIONAL");
  }

  /** Public accessor: user_prompt of a rubric version. */
  public String userPromptOf(UUID versionId) {
    return jdbc.query("select user_prompt from llm.rubric_version_v2 where id = ?",
        (rs, row) -> rs.getString("user_prompt"), versionId)
        .stream().findFirst().orElse("");
  }
  private String serialize(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new IllegalArgumentException("Anclas inválidas", exception); } }
  private Anchors deserialize(String value) { try { return mapper.readValue(value, Anchors.class); } catch (JsonProcessingException exception) { throw new IllegalStateException("Anclas almacenadas inválidas", exception); } }
  private record FamilyVersion(UUID id, int version) {}
}

