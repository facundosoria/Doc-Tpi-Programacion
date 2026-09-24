package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ModelDeploymentRepositoryTest {

  @Test
  void listsEnabledDeployments() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ModelDeploymentRepository(jdbc);
    UUID id = UUID.randomUUID();
    when(jdbc.query(contains("where d.state = 'ENABLED'"), any(RowMapper.class))).thenAnswer(rows(r -> {
      when(r.getObject(1, UUID.class)).thenReturn(id);
      when(r.getString(2)).thenReturn("openai");
      when(r.getString(3)).thenReturn("gpt-4o-mini");
      when(r.getString(4)).thenReturn("2024-07-18");
      when(r.getString(5)).thenReturn("ENABLED");
    }));

    var items = repository.listEnabledDeployments();

    assertThat(items).hasSize(1);
    assertThat(items.get(0).id()).isEqualTo(id);
    assertThat(items.get(0).provider()).isEqualTo("openai");
    assertThat(items.get(0).state()).isEqualTo("ENABLED");
  }

  @Test
  void fallsBackToEmptyWhenNoRowsAreReturned() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ModelDeploymentRepository(jdbc);
    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

    assertThat(repository.listEnabledDeployments()).isEmpty();
  }

  @Test
  void fallsBackToEmptyWhenTheTableIsUnavailable() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ModelDeploymentRepository(jdbc);
    when(jdbc.query(anyString(), any(RowMapper.class))).thenThrow(new RuntimeException("not ready"));

    assertThat(repository.listEnabledDeployments()).isEmpty();
  }

  @FunctionalInterface
  private interface ResultSetConfigurer {
    void configure(ResultSet rs) throws SQLException;
  }

  private static Answer<List<?>> rows(ResultSetConfigurer config) {
    return invocation -> {
      RowMapper<?> mapper = invocation.getArgument(1);
      ResultSet rs = mock(ResultSet.class);
      config.configure(rs);
      List<Object> result = new ArrayList<>();
      result.add(mapper.mapRow(rs, 0));
      return result;
    };
  }
}