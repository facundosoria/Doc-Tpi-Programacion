package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
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

class FunctionModelConfigRepositoryTest {

  @Test
  void findLoadsTheStoredConfig() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new FunctionModelConfigRepository(jdbc);
    UUID deploymentId = UUID.randomUUID();
    when(jdbc.query(org.mockito.ArgumentMatchers.contains("function_model_config"), any(RowMapper.class), anyString()))
        .thenAnswer(rows(r -> {
          when(r.getObject(1, UUID.class)).thenReturn(deploymentId);
          when(r.getBoolean(2)).thenReturn(true);
        }));

    Optional<FunctionModelConfigRepository.Config> config = repository.find(ModelFunction.TUTOR);

    assertThat(config).isPresent();
    assertThat(config.get().modelDeploymentId()).isEqualTo(deploymentId);
    assertThat(config.get().enabled()).isTrue();
  }

  @Test
  void findReturnsEmptyWhenAbsent() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new FunctionModelConfigRepository(jdbc);
    when(jdbc.query(org.mockito.ArgumentMatchers.contains("function_model_config"), any(RowMapper.class), anyString()))
        .thenReturn(List.of());

    assertThat(repository.find(ModelFunction.EVALUATOR)).isEmpty();
  }

  @Test
  void upsertPersistsWithTheLowerCasedFunctionKey() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new FunctionModelConfigRepository(jdbc);
    UUID deploymentId = UUID.randomUUID();
    var actor = new CallerIdentity("admin-service", UUID.randomUUID(), null, null);

    repository.upsert(ModelFunction.TUTOR, deploymentId, actor);

    verify(jdbc).update(org.mockito.ArgumentMatchers.contains("on conflict(function)"),
        eq("tutor"), eq(deploymentId), eq(actor.delegatedUserId()));
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