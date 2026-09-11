package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CourseGoldenSetRepositoryTest {

  @Test
  void publishDraft_updatesOnlyTheDraftVersionBecausePublishedVersionsAreImmutable() {
    RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
    CourseGoldenSetRepository repository = new CourseGoldenSetRepository(jdbc);
    UUID courseId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();

    boolean published = repository.publishDraft(courseId, versionId);

    assertThat(published).isTrue();
    assertThat(jdbc.sql).contains("set state = 'PUBLISHED'");
    assertThat(jdbc.sql).doesNotContain("set state = 'SUPERSEDED'");
    assertThat(jdbc.arguments).containsExactly(versionId, courseId);
  }

  private static final class RecordingJdbcTemplate extends JdbcTemplate {
    private String sql;
    private Object[] arguments;

    @Override
    public int update(String sql, Object... args) {
      this.sql = sql;
      this.arguments = args;
      return 1;
    }
  }
}
