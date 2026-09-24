package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ChallengeCalibrationAssignmentRepositoryTest {

  @Test
  void assignActiveOnlySucceedsWhenExactlyOneRowIsUpdated() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ChallengeCalibrationAssignmentRepository(jdbc);
    UUID challenge = UUID.randomUUID();
    UUID course = UUID.randomUUID();
    when(jdbc.update(contains("challenge_calibration_assignments"), any(), any(), any())).thenReturn(1);

    assertThat(repository.assignActive(challenge, course)).isTrue();

    when(jdbc.update(contains("challenge_calibration_assignments"), any(), any(), any())).thenReturn(0);
    assertThat(repository.assignActive(challenge, course)).isFalse();
  }

  @Test
  void previewSplitsMigrableAndLockedChallenges() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ChallengeCalibrationAssignmentRepository(jdbc);
    UUID course = UUID.randomUUID();
    UUID migrable = UUID.randomUUID();
    UUID locked = UUID.randomUUID();
    UUID run = UUID.randomUUID();
    when(jdbc.query(contains("challenge_calibration_assignments where course_id=?"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(
            r -> {
              when(r.getObject(1, UUID.class)).thenReturn(migrable);
              when(r.getObject(2, UUID.class)).thenReturn(run);
              when(r.getObject(3, OffsetDateTime.class)).thenReturn(null);
            },
            r -> {
              when(r.getObject(1, UUID.class)).thenReturn(locked);
              when(r.getObject(2, UUID.class)).thenReturn(run);
              when(r.getObject(3, OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
            }));

    var preview = repository.preview(course, run);

    assertThat(preview.migrable()).containsExactly(migrable);
    assertThat(preview.locked()).containsExactly(locked);
  }

  @Test
  void migrateSkipsTheUpdateWhenThereAreNoChallenges() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ChallengeCalibrationAssignmentRepository(jdbc);

    assertThat(repository.migrate(UUID.randomUUID(), UUID.randomUUID(), Set.of())).isZero();
    verify(jdbc, never()).update(any(String.class), any(), any(), any());
  }

  @Test
  void migrateUpdatesUnlockedChallengesUsingAnArray() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ChallengeCalibrationAssignmentRepository(jdbc);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    java.sql.Array array = mock(java.sql.Array.class);
    when(jdbc.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createArrayOf(eq("uuid"), any())).thenReturn(array);
    UUID course = UUID.randomUUID();
    UUID run = UUID.randomUUID();
    UUID challenge = UUID.randomUUID();
    when(jdbc.update(contains("set calibration_run_id=?"), any(), any(), any())).thenReturn(2);

    int updated = repository.migrate(course, run, Set.of(challenge));

    assertThat(updated).isEqualTo(2);
    verify(jdbc).update(contains("set calibration_run_id=?"), eq(run), eq(course), eq(array));
  }

  @Test
  void lockOnFirstAttemptReportsTheOutcome() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ChallengeCalibrationAssignmentRepository(jdbc);
    UUID challenge = UUID.randomUUID();
    UUID attempt = UUID.randomUUID();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    assertThat(repository.lockOnFirstAttempt(challenge, attempt)).isTrue();

    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThat(repository.lockOnFirstAttempt(challenge, attempt)).isFalse();
  }

  @Test
  void belongsToCourseReportsMembership() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ChallengeCalibrationAssignmentRepository(jdbc);
    UUID challenge = UUID.randomUUID();
    UUID course = UUID.randomUUID();
    when(jdbc.queryForObject(contains("challenge_calibration_assignments where challenge_id = ? and course_id = ?"),
        eq(Integer.class), eq(challenge), eq(course))).thenReturn(1);
    assertThat(repository.belongsToCourse(challenge, course)).isTrue();

    when(jdbc.queryForObject(contains("challenge_calibration_assignments where challenge_id = ? and course_id = ?"),
        eq(Integer.class), eq(challenge), eq(course))).thenReturn(0);
    assertThat(repository.belongsToCourse(challenge, course)).isFalse();
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