package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchor;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchors;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionCustomInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator.DimensionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class RubricVersionRepositoryCoverageTest {

  private static final String ANCHORS_JSON =
      "{\"low\":{\"behavior\":\"b\",\"referenceScore\":1,\"example\":\"e\"},"
      + "\"medium\":{\"behavior\":\"m\",\"referenceScore\":2,\"example\":\"m\"},"
      + "\"high\":{\"behavior\":\"h\",\"referenceScore\":3,\"example\":\"h\"}}";

  @Test
  void dimensionsOfDraftListsDimensionDefinitions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("rubric_dimension_v2 d"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getString("dimension_key")).thenReturn("AUTONOMY");
          when(r.getObject("weight", BigDecimal.class)).thenReturn(BigDecimal.valueOf(20));
        }));

    List<DimensionDefinition> result = repository.dimensionsOfDraft(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo(Dimension.AUTONOMY);
    assertThat(result.get(0).weight()).isEqualByComparingTo(BigDecimal.valueOf(20));
  }

  @Test
  void publishDraftReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.publishDraft(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.publishDraft(UUID.randomUUID(), UUID.randomUUID())).isFalse();
  }

  @Test
  void listMapsVersionsWithTheirDimensions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    UUID family = UUID.randomUUID();
    UUID origin = UUID.randomUUID();
    when(jdbc.query(contains("order by v.name, v.version_no desc"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getInt("version_no")).thenReturn(4);
          when(r.getString("name")).thenReturn("Rúbrica");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(2L);
          when(r.getObject("template_origin_version_id", UUID.class)).thenReturn(origin);
        }));
    stubDimensions(jdbc);

    List<RubricVersion> result = repository.list(UUID.randomUUID());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(id);
    assertThat(result.get(0).version()).isEqualTo(4);
    assertThat(result.get(0).revision()).isEqualTo(2L);
    assertThat(result.get(0).templateOriginVersionId()).isEqualTo(origin);
    assertThat(result.get(0).dimensions()).hasSize(1);
    assertThat(result.get(0).dimensions().get(0).key()).isEqualTo(Dimension.AUTONOMY);
  }

  @Test
  void listTemplatesMapsPlatformVersions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("f.scope = 'PLATFORM' and f.course_id is null"), any(RowMapper.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Plantilla");
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getLong("revision")).thenReturn(1L);
          when(r.getObject("template_origin_version_id", UUID.class)).thenReturn(null);
        }));
    stubDimensions(jdbc);

    List<RubricVersion> result = repository.listTemplates();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).state()).isEqualTo("PUBLISHED");
  }

  @Test
  void findTemplateReturnsTheVersionWhenPresent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.query(contains("f.scope = 'PLATFORM' and f.course_id is null and v.id = ?"),
        any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Plantilla");
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getLong("revision")).thenReturn(3L);
          when(r.getObject("template_origin_version_id", UUID.class)).thenReturn(null);
        }));
    stubDimensions(jdbc);

    Optional<RubricVersion> result = repository.findTemplate(id);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(id);
  }

  @Test
  void findTemplateReturnsEmptyWhenMissing() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("f.scope = 'PLATFORM' and f.course_id is null and v.id = ?"),
        any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.findTemplate(UUID.randomUUID())).isEmpty();
  }

  @Test
  void createTemplateDraftInsertsFamilyVersionAndDimensions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    stubDimensions(jdbc);
    RubricInput input = new RubricInput("Plantilla inicial",
        List.of(dimensionInput()));

    RubricVersion result = repository.createTemplateDraft(input, UUID.randomUUID());

    assertThat(result.name()).isEqualTo("Plantilla inicial");
    assertThat(result.state()).isEqualTo("DRAFT");
    assertThat(result.dimensions()).hasSize(1);
    verify(jdbc, times(4)).update(anyString(), any(Object[].class));
  }

  @Test
  void advanceTemplateRevisionReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.advanceTemplateRevision(UUID.randomUUID(), 2)).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.advanceTemplateRevision(UUID.randomUUID(), 2)).isFalse();
  }

  @Test
  void publishTemplateDraftReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.publishTemplateDraft(UUID.randomUUID())).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.publishTemplateDraft(UUID.randomUUID())).isFalse();
  }

  @Test
  void createNextTemplateDraftReturnsEmptyWhenSourceNotPublished() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("f.scope = 'PLATFORM' and f.course_id is null and v.id = ?"),
        any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Plantilla");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    stubDimensions(jdbc);

    assertThat(repository.createNextTemplateDraft(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
  }

  @Test
  void createNextTemplateDraftCreatesTheNextVersion() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID family = UUID.randomUUID();
    when(jdbc.query(contains("f.scope = 'PLATFORM' and f.course_id is null and v.id = ?"),
        any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getInt("version_no")).thenReturn(2);
          when(r.getString("name")).thenReturn("Plantilla");
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(family))).thenReturn(3);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    stubDimensions(jdbc);

    var result = repository.createNextTemplateDraft(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().version()).isEqualTo(3);
    assertThat(result.get().state()).isEqualTo("DRAFT");
  }

  @Test
  void findReturnsTheCourseVersionWhenPresent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.query(contains("f.course_id = ? and f.scope = 'COURSE' and v.id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Rúbrica");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
          when(r.getObject("template_origin_version_id", UUID.class)).thenReturn(UUID.randomUUID());
        }));
    stubDimensions(jdbc);

    Optional<RubricVersion> result = repository.find(UUID.randomUUID(), id);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(id);
  }

  @Test
  void findReturnsEmptyWhenMissing() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("f.course_id = ? and f.scope = 'COURSE' and v.id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.find(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
  }

  @Test
  void createDraftFromPublishedTemplateReturnsEmptyWhenTemplateMissing() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("v.state = 'PUBLISHED' and f.scope = 'PLATFORM'"), any(RowMapper.class),
        any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.createDraftFromPublishedTemplate(UUID.randomUUID(), UUID.randomUUID(), "Nombre",
        UUID.randomUUID())).isEmpty();
  }

  @Test
  void createDraftFromPublishedTemplateCreatesACourseDraft() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID template = UUID.randomUUID();
    when(jdbc.query(contains("v.state = 'PUBLISHED' and f.scope = 'PLATFORM'"), any(RowMapper.class),
        any(UUID.class)))
        .thenAnswer(rowsOf(r -> when(r.getObject("id", UUID.class)).thenReturn(template)));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    stubDimensions(jdbc);

    var result = repository.createDraftFromPublishedTemplate(UUID.randomUUID(), template, "Rúbrica curso",
        UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Rúbrica curso");
    assertThat(result.get().templateOriginVersionId()).isEqualTo(template);
    assertThat(result.get().state()).isEqualTo("DRAFT");
  }

  @Test
  void createNextDraftReturnsEmptyWhenNoPublishedReservation() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("set next_version = f.next_version + 1"), any(RowMapper.class),
        any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.createNextDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        .isEmpty();
  }

  @Test
  void createNextDraftCreatesTheNextCourseDraft() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID family = UUID.randomUUID();
    when(jdbc.query(contains("set next_version = f.next_version + 1"), any(RowMapper.class),
        any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(family);
          when(r.getInt("version_no")).thenReturn(4);
        }));
    when(jdbc.query(contains("f.course_id = ? and f.scope = 'COURSE' and v.id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getInt("version_no")).thenReturn(3);
          when(r.getString("name")).thenReturn("Rúbrica");
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    stubDimensions(jdbc);

    var result = repository.createNextDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().version()).isEqualTo(4);
    assertThat(result.get().state()).isEqualTo("DRAFT");
  }

  @Test
  void advanceRevisionReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.advanceRevision(UUID.randomUUID(), UUID.randomUUID(), 3)).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.advanceRevision(UUID.randomUUID(), UUID.randomUUID(), 3)).isFalse();
  }

  @Test
  void updateVersionNameRenamesTheDraft() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());

    repository.updateVersionName(UUID.randomUUID(), UUID.randomUUID(), "Nuevo nombre");

    verify(jdbc).update(anyString(), any(Object[].class));
  }

  @Test
  void updateTemplateVersionNameRenamesTheTemplateDraft() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());

    repository.updateTemplateVersionName(UUID.randomUUID(), "Nuevo nombre");

    verify(jdbc).update(anyString(), any(Object[].class));
  }

  @Test
  void replaceDimensionsDeletesAndInsertsEachDimension() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    repository.replaceDimensions(UUID.randomUUID(), List.of());
    verify(jdbc, times(1)).update(anyString(), any(Object[].class));

    repository.replaceDimensions(UUID.randomUUID(),
        List.of(dimensionInput(), dimensionInput()));
    verify(jdbc, times(4)).update(anyString(), any(Object[].class));
  }

  @Test
  void listFailsOnCorruptedStoredAnchors() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("order by v.name, v.version_no desc"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Rúbrica");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    when(jdbc.query(contains("anchors::text as anchors"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getString("dimension_key")).thenReturn("AUTONOMY");
          when(r.getString("label")).thenReturn("Autonomía");
          when(r.getString("criterion")).thenReturn("Criterio");
          when(r.getString("anchors")).thenReturn("{ anclas inválidas");
          when(r.getObject("weight", BigDecimal.class)).thenReturn(BigDecimal.valueOf(20));
        }));

    assertThatThrownBy(() -> repository.list(UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class);
  }

  private static DimensionInput dimensionInput() {
    return new DimensionInput(Dimension.AUTONOMY, "Autonomía", "Criterio",
        new Anchors(
            new Anchor("bajo", 1, "ejemplo bajo"),
            new Anchor("medio", 2, "ejemplo medio"),
            new Anchor("alto", 3, "ejemplo alto")),
        BigDecimal.valueOf(20));
  }

  private static void stubDimensions(JdbcTemplate jdbc) {
    when(jdbc.query(contains("anchors::text as anchors"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getString("dimension_key")).thenReturn("AUTONOMY");
          when(r.getString("label")).thenReturn("Autonomía");
          when(r.getString("criterion")).thenReturn("Criterio");
          when(r.getString("anchors")).thenReturn(ANCHORS_JSON);
          when(r.getObject("weight", BigDecimal.class)).thenReturn(BigDecimal.valueOf(20));
        }));
  }

  @Test
  void listByChallengeListsChallengeOverlayVersions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID();
    when(jdbc.query(contains("f.scope = 'CHALLENGE'"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Overlay");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    stubDimensions(jdbc);

    List<RubricVersion> result = repository.listByChallenge(course, challenge);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Overlay");
  }

  @Test
  void listOverlaysByCourseListsCourseWideChallengeVersions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID();
    when(jdbc.query(contains("f.scope = 'CHALLENGE'"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("challenge_id", UUID.class)).thenReturn(challenge);
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Overlay A");
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getLong("revision")).thenReturn(2L);
        }));

    List<RubricVersionRepository.ChallengeRow> result = repository.listOverlaysByCourse(course);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).challengeId()).isEqualTo(challenge);
    assertThat(result.get(0).version().name()).isEqualTo("Overlay A");
    assertThat(result.get(0).version().state()).isEqualTo("PUBLISHED");
  }

  @Test
  void findChallengeReturnsTheVersionWhenPresent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), id = UUID.randomUUID();
    when(jdbc.query(contains("f.scope = 'CHALLENGE' and v.id = ?"), any(RowMapper.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Overlay");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    stubDimensions(jdbc);

    Optional<RubricVersion> result = repository.findChallenge(course, challenge, id);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(id);
  }

  @Test
  void challengeCustomDimensionsMapsOverlayDimensions() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("from llm.rubric_custom_dimensions"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getString("dimension_key")).thenReturn("algoritmos");
          when(r.getString("label")).thenReturn("Algoritmos");
          when(r.getString("criterion")).thenReturn("Criterio");
          when(r.getString("anchors")).thenReturn(ANCHORS_JSON);
          when(r.getObject("weight", BigDecimal.class)).thenReturn(BigDecimal.valueOf(60));
        }));

    List<DimensionCustomInput> result = repository.challengeCustomDimensions(UUID.randomUUID());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("algoritmos");
    assertThat(result.get(0).weight()).isEqualByComparingTo(BigDecimal.valueOf(60));
  }

  @Test
  void baselineVersionIdOfReturnsTheBaseline() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID baseline = UUID.randomUUID();
    when(jdbc.query(contains("select baseline_version_id"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> when(r.getObject("baseline_version_id", UUID.class)).thenReturn(baseline)));

    assertThat(repository.baselineVersionIdOf(UUID.randomUUID())).isEqualTo(baseline);
  }

  @Test
  void createDraftForChallengeInsertsFamilyVersionAndReturnsTheOverlay() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    when(jdbc.query(contains("from llm.rubric_version_v2 v where v.id = ?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Overlay");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
          when(r.getObject("template_origin_version_id", UUID.class)).thenReturn(null);
        }));
    stubDimensions(jdbc);

    var result = repository.createDraftForChallenge(UUID.randomUUID(), UUID.randomUUID(), "Overlay",
        UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Overlay");
    assertThat(result.get().state()).isEqualTo("DRAFT");
    verify(jdbc, times(2)).update(anyString(), any(Object[].class));
  }

  @Test
  void advanceChallengeRevisionReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.advanceChallengeRevision(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2)).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.advanceChallengeRevision(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2)).isFalse();
  }

  @Test
  void updateChallengeVersionNameRenamesTheOverlay() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());

    repository.updateChallengeVersionName(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Nuevo");

    verify(jdbc).update(anyString(), any(Object[].class));
  }

  @Test
  void updateChallengePromptUpdatesTheUserPrompt() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());

    repository.updateChallengePrompt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Instrucciones");

    verify(jdbc).update(anyString(), any(Object[].class));
  }

  @Test
  void replaceChallengeCustomDimensionsDeletesAndInsertsEachDimension() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    repository.replaceChallengeCustomDimensions(UUID.randomUUID(), List.of());
    verify(jdbc, times(1)).update(anyString(), any(Object[].class));

    repository.replaceChallengeCustomDimensions(UUID.randomUUID(), List.of(customDimension(), customDimension()));
    verify(jdbc, times(4)).update(anyString(), any(Object[].class));
  }

  @Test
  void publishChallengeDraftReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.publishChallengeDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.publishChallengeDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())).isFalse();
  }

  @Test
  void createNextChallengeDraftReturnsEmptyWhenPublishedNotPresent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("f.scope = 'CHALLENGE' and v.id = ?"), any(RowMapper.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.createNextChallengeDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID())).isEmpty();
  }

  @Test
  void createNextChallengeDraftReturnsEmptyWhenPublishedNotPublished() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("f.scope = 'CHALLENGE' and v.id = ?"), any(RowMapper.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("name")).thenReturn("Overlay");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    stubDimensions(jdbc);

    assertThat(repository.createNextChallengeDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID())).isEmpty();
  }

  @Test
  void createNextChallengeDraftCreatesTheNextOverlayVersion() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    UUID family = UUID.randomUUID();
    when(jdbc.query(contains("f.scope = 'CHALLENGE' and v.id = ?"), any(RowMapper.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getInt("version_no")).thenReturn(2);
          when(r.getString("name")).thenReturn("Overlay");
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getLong("revision")).thenReturn(1L);
        }));
    stubDimensions(jdbc);
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(family))).thenReturn(3);
    when(jdbc.query(contains("select baseline_version_id"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> when(r.getObject("baseline_version_id", UUID.class)).thenReturn(UUID.randomUUID())));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    when(jdbc.query(contains("from llm.rubric_version_v2 v where v.id = ?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getInt("version_no")).thenReturn(3);
          when(r.getString("name")).thenReturn("Overlay");
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getLong("revision")).thenReturn(1L);
          when(r.getObject("template_origin_version_id", UUID.class)).thenReturn(null);
        }));

    var result = repository.createNextChallengeDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().version()).isEqualTo(3);
    assertThat(result.get().state()).isEqualTo("DRAFT");
    verify(jdbc, times(2)).update(anyString(), any(Object[].class));
  }

  @Test
  void rubricKindOfReturnsTheKindAndDefaultsToInstitutional() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("select rubric_kind"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> when(r.getString("rubric_kind")).thenReturn("MODULAR_CUSTOM")));
    assertThat(repository.rubricKindOf(UUID.randomUUID())).isEqualTo("MODULAR_CUSTOM");

    when(jdbc.query(contains("select rubric_kind"), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());
    assertThat(repository.rubricKindOf(UUID.randomUUID())).isEqualTo("DEFAULT_INSTITUTIONAL");
  }

  @Test
  void userPromptOfReturnsThePromptAndDefaultsToEmpty() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new RubricVersionRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("select user_prompt"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> when(r.getString("user_prompt")).thenReturn("Guía")));
    assertThat(repository.userPromptOf(UUID.randomUUID())).isEqualTo("Guía");

    when(jdbc.query(contains("select user_prompt"), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());
    assertThat(repository.userPromptOf(UUID.randomUUID())).isEmpty();
  }

  private static DimensionCustomInput customDimension() {
    return new DimensionCustomInput("algoritmos", "Algoritmos", "Criterio",
        new Anchors(new Anchor("bajo", 25, "ej"), new Anchor("medio", 60, "ej"), new Anchor("alto", 90, "ej")),
        BigDecimal.valueOf(50));
  }

  @FunctionalInterface
  private interface ResultSetConfigurer {
    void configure(ResultSet rs) throws SQLException;
  }

  @SafeVarargs
  private static Answer<List<?>> rowsOf(ResultSetConfigurer... configs) {
    return invocation -> {
      RowMapper<?> mapper = invocation.getArgument(1);
      List<Object> result = new ArrayList<>();
      for (int index = 0; index < configs.length; index++) {
        ResultSet rs = mock(ResultSet.class);
        configs[index].configure(rs);
        result.add(mapper.mapRow(rs, index));
      }
      return result;
    };
  }
}