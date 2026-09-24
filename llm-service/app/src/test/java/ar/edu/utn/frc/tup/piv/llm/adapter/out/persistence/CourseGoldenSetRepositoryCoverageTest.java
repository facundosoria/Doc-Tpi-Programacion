package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetView;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class CourseGoldenSetRepositoryCoverageTest {

  private final ObjectMapper json = new ObjectMapper();

  @Test
  void copyPublishedPlatformVersionCopiesWhenSourceExists() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    UUID course = UUID.randomUUID();
    UUID base = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    when(jdbc.query(contains("v.state = 'PUBLISHED' and f.scope = 'PLATFORM'"), any(RowMapper.class),
        any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(base);
          when(r.getString("name")).thenReturn("Golden base");
        }));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    Optional<CourseGoldenSetVersion> result = repository.copyPublishedPlatformVersion(course, base, actor);

    assertThat(result).isPresent();
    assertThat(result.get().basedOnVersionId()).isEqualTo(base);
    assertThat(result.get().version()).isEqualTo(1);
    assertThat(result.get().state()).isEqualTo("DRAFT");
    assertThat(result.get().familyId()).isNotNull();
    verify(jdbc, org.mockito.Mockito.times(3)).update(anyString(), any(Object[].class));
  }

  @Test
  void copyPublishedPlatformVersionReturnsEmptyWhenSourceMissing() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.query(contains("v.state = 'PUBLISHED' and f.scope = 'PLATFORM'"), any(RowMapper.class),
        any(UUID.class)))
        .thenReturn(List.of());

    Optional<CourseGoldenSetVersion> result =
        repository.copyPublishedPlatformVersion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isEmpty();
    verify(jdbc, org.mockito.Mockito.never()).update(anyString(), any(Object[].class));
  }

  @Test
  void createDraftInsertsFamilyAndVersion() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    CourseGoldenSetVersion draft = repository.createDraft(UUID.randomUUID(), "Borrador", UUID.randomUUID());

    assertThat(draft.state()).isEqualTo("DRAFT");
    assertThat(draft.version()).isEqualTo(1);
    assertThat(draft.basedOnVersionId()).isNull();
  }

  @Test
  void listMapsViewsWithTheirCases() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    UUID id = UUID.randomUUID();
    UUID family = UUID.randomUUID();
    UUID base = UUID.randomUUID();
    when(jdbc.query(contains("order by f.name, v.version_no desc"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getString("name")).thenReturn("Golden");
          when(r.getInt("version_no")).thenReturn(3);
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getObject("based_on_version_id", UUID.class)).thenReturn(base);
        }));
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("case_order")).thenReturn(1);
          when(r.getString("author")).thenReturn("alumno");
          when(r.getString("review_state")).thenReturn("DRAFT");
        }));

    List<CourseGoldenSetView> result = repository.list(UUID.randomUUID());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(id);
    assertThat(result.get(0).version()).isEqualTo(3);
    assertThat(result.get(0).basedOnVersionId()).isEqualTo(base);
    assertThat(result.get(0).cases()).hasSize(1);
    assertThat(result.get(0).cases().get(0).author()).isEqualTo("alumno");
  }

  @Test
  void countCasesUsesTheCountAndDefaultsToZeroWhenNull() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    UUID version = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(version))).thenReturn(5);
    assertThat(repository.countCases(version)).isEqualTo(5);
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(version))).thenReturn(null);
    assertThat(repository.countCases(version)).isZero();
  }

  @Test
  void findReturnsTheVersionWhenPresent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    UUID id = UUID.randomUUID();
    when(jdbc.query(contains("where v.id = ? and f.scope = 'COURSE' and f.course_id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getString("name")).thenReturn("Golden");
          when(r.getInt("version_no")).thenReturn(2);
          when(r.getString("state")).thenReturn("PUBLISHED");
          when(r.getObject("based_on_version_id", UUID.class)).thenReturn(UUID.randomUUID());
        }));
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());

    Optional<CourseGoldenSetView> result = repository.find(UUID.randomUUID(), id);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(id);
    assertThat(result.get().cases()).isEmpty();
  }

  @Test
  void findReturnsEmptyWhenNotPresent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.query(contains("where v.id = ? and f.scope = 'COURSE' and f.course_id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.find(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
  }

  @Test
  void findDetailMapsCaseDetails() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.query(contains("where v.id = ? and f.scope = 'COURSE' and f.course_id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getString("name")).thenReturn("Golden");
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("state")).thenReturn("DRAFT");
          when(r.getObject("based_on_version_id", UUID.class)).thenReturn(null);
        }));
    when(jdbc.query(contains("select id, case_order, transcript, reference_scores"), any(RowMapper.class),
        any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("case_order")).thenReturn(1);
          when(r.getString("transcript")).thenReturn("{\"role\":\"TUTOR\"}");
          when(r.getString("reference_scores")).thenReturn("{\"AUTONOMY\":80}");
        }));

    Optional<GoldenSetDetail> result = repository.findDetail(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().cases()).hasSize(1);
    assertThat(result.get().cases().get(0).transcript().get("role").asText()).isEqualTo("TUTOR");
    assertThat(result.get().cases().get(0).referenceScores().get("AUTONOMY").asInt()).isEqualTo(80);
  }

  @Test
  void findDetailFailsOnCorruptedTranscript() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.query(contains("where v.id = ? and f.scope = 'COURSE' and f.course_id = ?"),
        any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getObject("family_id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getString("name")).thenReturn("Golden");
          when(r.getInt("version_no")).thenReturn(1);
          when(r.getString("state")).thenReturn("DRAFT");
        }));
    when(jdbc.query(contains("select id, case_order, transcript, reference_scores"), any(RowMapper.class),
        any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("case_order")).thenReturn(1);
          when(r.getString("transcript")).thenReturn("{ no valido");
          when(r.getString("reference_scores")).thenReturn("{}");
        }));

    assertThatThrownBy(() -> repository.findDetail(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void addDraftCaseReturnsSummaryWhenInserted() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    GoldenSetCaseInput input = new GoldenSetCaseInput(
        json.readTree("{\"role\":\"TUTOR\"}"),
        json.readTree("{\"context\":\"x\"}"),
        json.readTree("{\"meta\":\"y\"}"),
        "alumno",
        json.readTree("{\"AUTONOMY\":80}"),
        json.readTree("{\"AUTONOMY\":\"bien\"}"));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(
            r -> {
              when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
              when(r.getInt("case_order")).thenReturn(0);
              when(r.getString("author")).thenReturn("a");
              when(r.getString("review_state")).thenReturn("DRAFT");
            },
            r -> {
              when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
              when(r.getInt("case_order")).thenReturn(1);
              when(r.getString("author")).thenReturn("b");
              when(r.getString("review_state")).thenReturn("DRAFT");
            }));

    var result = repository.addDraftCase(UUID.randomUUID(), UUID.randomUUID(), input);

    assertThat(result).isPresent();
    assertThat(result.get().order()).isEqualTo(1);
    assertThat(result.get().author()).isEqualTo("alumno");
    assertThat(result.get().reviewState()).isEqualTo("DRAFT");
  }

  @Test
  void addDraftCaseReturnsEmptyWhenNoDraftMatches() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    GoldenSetCaseInput input = new GoldenSetCaseInput(
        json.readTree("{\"role\":\"TUTOR\"}"), json.readTree("{}"), json.readTree("{}"), "alumno",
        json.readTree("{}"), json.readTree("{}"));

    assertThat(repository.addDraftCase(UUID.randomUUID(), UUID.randomUUID(), input)).isEmpty();
  }

  @Test
  void addDraftCaseToleratesNullMetadataAndJustifications() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    when(jdbc.query(contains("from llm.golden_set_cases"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
          when(r.getInt("case_order")).thenReturn(0);
          when(r.getString("author")).thenReturn("x");
          when(r.getString("review_state")).thenReturn("DRAFT");
        }));
    GoldenSetCaseInput input = new GoldenSetCaseInput(
        json.readTree("{\"role\":\"TUTOR\"}"), null, null, "x", json.readTree("{}"), null);

    var result = repository.addDraftCase(UUID.randomUUID(), UUID.randomUUID(), input);

    assertThat(result).isPresent();
  }

  @Test
  void updateDraftCaseReturnsUpdatedSummary() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    UUID caseId = UUID.randomUUID();
    GoldenSetCaseInput input = new GoldenSetCaseInput(
        json.readTree("{\"role\":\"TUTOR\"}"), json.readTree("{}"), null, "alumno",
        json.readTree("{}"), null);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    when(jdbc.query(contains("where id = ?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(caseId);
          when(r.getInt("case_order")).thenReturn(2);
          when(r.getString("author")).thenReturn("alumno");
          when(r.getString("review_state")).thenReturn("DRAFT");
        }));

    var result = repository.updateDraftCase(UUID.randomUUID(), UUID.randomUUID(), caseId, input);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(caseId);
    assertThat(result.get().order()).isEqualTo(2);
  }

  @Test
  void updateDraftCaseReturnsEmptyWhenNothingMatched() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    GoldenSetCaseInput input = new GoldenSetCaseInput(
        json.readTree("{}"), json.readTree("{}"), json.readTree("{}"), "x", json.readTree("{}"),
        json.readTree("{}"));

    assertThat(repository.updateDraftCase(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), input))
        .isEmpty();
  }

  @Test
  void publishDraftReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.publishDraft(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.publishDraft(UUID.randomUUID(), UUID.randomUUID())).isFalse();
  }

  @Test
  void createNextDraftReturnsEmptyWithoutPublishedSource() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.query(contains("f.id as family_id"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of());

    assertThat(repository.createNextDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        .isEmpty();
  }

  @Test
  void createNextDraftCreatesTheFollowingVersion() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    UUID family = UUID.randomUUID();
    when(jdbc.query(contains("f.id as family_id"), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("family_id", UUID.class)).thenReturn(family);
          when(r.getString("name")).thenReturn("Golden");
        }));
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(family))).thenReturn(5);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    var result = repository.createNextDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().version()).isEqualTo(5);
    assertThat(result.get().state()).isEqualTo("DRAFT");
  }

  @Test
  void softDeleteUnusedDraftReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new CourseGoldenSetRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.softDeleteUnusedDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        .isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.softDeleteUnusedDraft(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        .isFalse();
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