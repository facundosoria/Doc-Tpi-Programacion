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
  @Test void migratesPostgresAndArchivesV1BeforeEnforcingV2IntegrityRules() throws Exception {
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
      postgres.start();
      Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .schemas("llm").defaultSchema("llm").createSchemas(true).load().migrate();
      try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
           var statement = connection.createStatement()) {
        var tables = statement.executeQuery("select count(*) from information_schema.tables where table_schema = 'llm'");
        tables.next();
        assertThat(tables.getInt(1)).isGreaterThanOrEqualTo(20);

        var legacyTables = statement.executeQuery("select count(*) from information_schema.tables where table_schema = 'legacy_v1' and table_name in ('golden_sets', 'golden_set_entries', 'rubric_versions', 'rubric_dimensions')");
        legacyTables.next();
        assertThat(legacyTables.getInt(1)).isEqualTo(4);
        var activeLegacyTables = statement.executeQuery("select count(*) from information_schema.tables where table_schema = 'llm' and table_name in ('golden_sets', 'golden_set_entries', 'rubric_versions', 'rubric_dimensions')");
        activeLegacyTables.next();
        assertThat(activeLegacyTables.getInt(1)).isZero();

        UUID courseId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID rubricFamilyId = UUID.randomUUID();
        UUID rubricVersionId = UUID.randomUUID();
        statement.executeUpdate("insert into llm.rubric_families (id, scope, course_id, name, created_by_user_id) values ('" + rubricFamilyId + "', 'COURSE', '" + courseId + "', 'Rúbrica curso', '" + actorId + "')");
        statement.executeUpdate("insert into llm.rubric_version_v2 (id, family_id, version_no, name, state, created_by_user_id, published_at) values ('" + rubricVersionId + "', '" + rubricFamilyId + "', 1, 'Rúbrica curso', 'PUBLISHED', '" + actorId + "', now())");
        assertThatThrownBy(() -> statement.executeUpdate("update llm.rubric_version_v2 set revision = 2 where id = '" + rubricVersionId + "'"))
            .isInstanceOf(SQLException.class);

        UUID goldenFamilyId = UUID.randomUUID();
        UUID goldenVersionId = UUID.randomUUID();
        statement.executeUpdate("insert into llm.golden_set_families (id, scope, course_id, name, created_by_user_id) values ('" + goldenFamilyId + "', 'COURSE', '" + courseId + "', 'Set curso', '" + actorId + "')");
        statement.executeUpdate("insert into llm.golden_set_versions (id, family_id, version_no, state, created_by_user_id, published_at) values ('" + goldenVersionId + "', '" + goldenFamilyId + "', 1, 'PUBLISHED', '" + actorId + "', now())");
        assertThatThrownBy(() -> statement.executeUpdate("insert into llm.golden_set_cases (golden_set_version_id, case_order, transcript, challenge_context, author, reference_scores) values ('" + goldenVersionId + "', 0, '[{\"role\":\"STUDENT\",\"content\":\"hola\"}]', '{\"statement\":\"x\"}', 'test', '{\"autonomy\":80}')"))
            .isInstanceOf(SQLException.class);

        UUID adapterId = UUID.randomUUID();
        UUID deploymentId = UUID.randomUUID();
        statement.executeUpdate("insert into llm.model_adapters (id, provider, created_by_user_id) values ('" + adapterId + "', 'test-provider', '" + actorId + "')");
        statement.executeUpdate("insert into llm.model_deployments (id, adapter_id, model_id, model_version) values ('" + deploymentId + "', '" + adapterId + "', 'test-model', '1')");
        UUID firstRunId = UUID.randomUUID();
        UUID secondRunId = UUID.randomUUID();
        statement.executeUpdate("insert into llm.calibration_runs (id, course_id, rubric_version_id, golden_set_version_id, model_deployment_id, reason, created_by_user_id) values ('" + firstRunId + "', '" + courseId + "', '" + rubricVersionId + "', '" + goldenVersionId + "', '" + deploymentId + "', 'MANUAL', '" + actorId + "')");
        statement.executeUpdate("insert into llm.calibration_runs (id, course_id, rubric_version_id, golden_set_version_id, model_deployment_id, reason, created_by_user_id) values ('" + secondRunId + "', '" + courseId + "', '" + rubricVersionId + "', '" + goldenVersionId + "', '" + deploymentId + "', 'MANUAL', '" + actorId + "')");
        statement.executeUpdate("insert into llm.active_calibrations (course_id, calibration_run_id, activated_by_user_id) values ('" + courseId + "', '" + firstRunId + "', '" + actorId + "')");
        assertThatThrownBy(() -> statement.executeUpdate("insert into llm.active_calibrations (course_id, calibration_run_id, activated_by_user_id) values ('" + courseId + "', '" + secondRunId + "', '" + actorId + "')"))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
