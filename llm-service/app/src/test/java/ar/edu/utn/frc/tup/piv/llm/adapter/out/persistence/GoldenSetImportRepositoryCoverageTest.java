package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.ImportBatch;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.ImportRow;
import com.fasterxml.jackson.databind.JsonNode;
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

class GoldenSetImportRepositoryCoverageTest {

  private final ObjectMapper json = new ObjectMapper();

  @Test
  void createInsertsBatchAndRowsAndReturnsTheBatch() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    UUID course = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    List<JsonNode> rows = List.of(json.readTree("{\"t\":1}"), json.readTree("{\"t\":2}"));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    Optional<ImportBatch> result = repository.create(course, version, "JSON", key, actor, rows);

    assertThat(result).isPresent();
    assertThat(result.get().goldenSetVersionId()).isEqualTo(version);
    assertThat(result.get().format()).isEqualTo("JSON");
    assertThat(result.get().state()).isEqualTo("DRAFT");
    assertThat(result.get().rows()).isEqualTo(2);
    verify(jdbc, times(3)).update(anyString(), any(Object[].class));
  }

  @Test
  void createDelegatesToExistingWhenNoBatchWasInserted() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    UUID course = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of(new ImportBatch(UUID.randomUUID(), version, "JSON", "FAILED", 0)));

    Optional<ImportBatch> result =
        repository.create(course, version, "JSON", UUID.randomUUID(), UUID.randomUUID(), List.of());

    assertThat(result).isPresent();
    assertThat(result.get().state()).isEqualTo("FAILED");
  }

  @Test
  void createReturnsEmptyWhenNoExistingBatchMatches() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class))).thenReturn(List.of());

    Optional<ImportBatch> result =
        repository.create(UUID.randomUUID(), UUID.randomUUID(), "JSON", UUID.randomUUID(), UUID.randomUUID(), List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void existingMapsTheFirstBatchRow() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    UUID batch = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(batch);
          when(r.getObject("golden_set_version_id", UUID.class)).thenReturn(version);
          when(r.getString("format")).thenReturn("CSV");
          when(r.getString("state")).thenReturn("READY");
        }));

    Optional<ImportBatch> result = repository.existing(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(batch);
    assertThat(result.get().state()).isEqualTo("READY");
  }

  @Test
  void rowsMapsRowNumberAndPayload() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class)))
        .thenAnswer(rowsOf(
            r -> {
              when(r.getInt(1)).thenReturn(1);
              when(r.getString(2)).thenReturn("{\"a\":\"uno\"}");
            },
            r -> {
              when(r.getInt(1)).thenReturn(2);
              when(r.getString(2)).thenReturn("{\"a\":\"dos\"}");
            }));

    List<ImportRow> result = repository.rows(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).rowNumber()).isEqualTo(1);
    assertThat(result.get(1).payload()).isEqualTo("{\"a\":\"dos\"}");
  }

  @Test
  void replaceRowReportsWhetherOneRowWasReplaced() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.replaceRow(UUID.randomUUID(), UUID.randomUUID(), 1, json.readTree("{}"))).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.replaceRow(UUID.randomUUID(), UUID.randomUUID(), 2, json.readTree("{}"))).isFalse();
  }

  @Test
  void beginValidationReportsWhetherTheBatchWasClaimed() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertThat(repository.beginValidation(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.beginValidation(UUID.randomUUID(), UUID.randomUUID())).isFalse();
  }

  @Test
  void rowResultFlagsValidAndInvalidRows() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    UUID batch = UUID.randomUUID();

    repository.rowResult(batch, 1, true, "[]");
    repository.rowResult(batch, 2, false, "[{\"code\":\"MISSING_FIELD\"}]");

    verify(jdbc, times(2)).update(anyString(), any(Object[].class));
  }

  @Test
  void finishValidationMarksReadyOrFailed() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    UUID batch = UUID.randomUUID();

    repository.finishValidation(batch, true);
    repository.finishValidation(batch, false);

    verify(jdbc, times(2)).update(anyString(), any(Object[].class));
  }

  @Test
  void claimReadyReturnsTheVersionWhenAvailableAndEmptyOtherwise() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);
    UUID version = UUID.randomUUID();
    UUID batch = UUID.randomUUID();
    UUID course = UUID.randomUUID();
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of(version))
        .thenReturn(List.of());

    Optional<UUID> first = repository.claimReady(course, batch);
    Optional<UUID> second = repository.claimReady(course, batch);

    assertThat(first).contains(version);
    assertThat(second).isEmpty();
  }

  @Test
  void insertCasesCopiesValidatedRowsIntoTheGoldenSet() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);

    repository.insertCases(UUID.randomUUID(), UUID.randomUUID());

    verify(jdbc).update(anyString(), any(Object[].class));
  }

  @Test
  void completeCommitsTheBatch() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetImportRepository(jdbc);

    repository.complete(UUID.randomUUID());

    verify(jdbc).update(anyString(), any(Object[].class));
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