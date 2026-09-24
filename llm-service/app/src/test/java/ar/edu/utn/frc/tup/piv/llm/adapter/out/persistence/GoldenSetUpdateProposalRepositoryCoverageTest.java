package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetUpdateProposal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class GoldenSetUpdateProposalRepositoryCoverageTest {

  @Test
  void createForPublishedBaseReturnsTheInsertedRowCount() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetUpdateProposalRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(4);

    int inserted = repository.createForPublishedBase(UUID.randomUUID());

    assertThat(inserted).isEqualTo(4);
  }

  @Test
  void pendingForCourseMapsAllColumns() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetUpdateProposalRepository(jdbc);
    UUID id = UUID.randomUUID();
    UUID course = UUID.randomUUID();
    UUID family = UUID.randomUUID();
    UUID base = UUID.randomUUID();
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getObject("id", UUID.class)).thenReturn(id);
          when(r.getObject("course_id", UUID.class)).thenReturn(course);
          when(r.getObject("course_family_id", UUID.class)).thenReturn(family);
          when(r.getObject("base_version_id", UUID.class)).thenReturn(base);
          when(r.getInt("base_version")).thenReturn(7);
          when(r.getInt("base_case_count")).thenReturn(3);
          when(r.getObject("detected_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        }));

    List<GoldenSetUpdateProposal> result = repository.pendingForCourse(course);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(id);
    assertThat(result.get(0).courseId()).isEqualTo(course);
    assertThat(result.get(0).courseFamilyId()).isEqualTo(family);
    assertThat(result.get(0).baseVersionId()).isEqualTo(base);
    assertThat(result.get(0).baseVersion()).isEqualTo(7);
    assertThat(result.get(0).baseCaseCount()).isEqualTo(3);
    assertThat(result.get(0).detectedAt()).isNotNull();
  }

  @Test
  void pendingForCourseReturnsEmptyWhenThereAreNoProposals() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new GoldenSetUpdateProposalRepository(jdbc);
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());

    List<GoldenSetUpdateProposal> result = repository.pendingForCourse(UUID.randomUUID());

    assertThat(result).isEmpty();
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