package ar.edu.utn.frc.tup.piv.llm;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "integration", matches = "true")
class FlywaySchemaTest {
  @Test void migratesPostgresAndEnforcesGoldenSetIntegrityRules() throws Exception {
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
      postgres.start();
      Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .schemas("llm").defaultSchema("llm").createSchemas(true).load().migrate();
      try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
           var statement = connection.createStatement()) {
        var tables = statement.executeQuery("select count(*) from information_schema.tables where table_schema = 'llm'");
        tables.next();
        assertThat(tables.getInt(1)).isGreaterThanOrEqualTo(6);

        UUID goldenSetId = UUID.randomUUID();
        statement.executeUpdate("insert into llm.golden_sets (id, rubric_version_id, language, created_by_user_id, created_by_service) values ('" + goldenSetId + "', '00000000-0000-0000-0000-000000000001', 'es', '11111111-1111-1111-1111-111111111111', 'test')");
        assertThatThrownBy(() -> statement.executeUpdate("insert into llm.golden_set_entries (golden_set_id, transcript, reference_scores, content_hash, created_by_user_id) values ('" + goldenSetId + "', '[{\"role\":\"learner\"}]', '{\"autonomy\":80}', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111')"))
            .isInstanceOf(SQLException.class);
        statement.executeUpdate("insert into llm.golden_set_entries (golden_set_id, transcript, reference_scores, content_hash, created_by_user_id) values ('" + goldenSetId + "', '[{\"role\":\"learner\"}]', '{\"autonomy\":80,\"clarity\":70,\"progression\":60,\"compliance\":100,\"efficiency\":90}', 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111')");
        assertThatThrownBy(() -> statement.executeUpdate("update llm.golden_sets set language = 'es' where id = '" + goldenSetId + "'"))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
