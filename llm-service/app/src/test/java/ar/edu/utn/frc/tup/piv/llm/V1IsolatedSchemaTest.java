package ar.edu.utn.frc.tup.piv.llm;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "integration", matches = "true")
class V1IsolatedSchemaTest {

  @Test
  void testV1SchemaAndIntegrityRulesFulfillAllH04AcceptanceCriteria() throws Exception {
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))) {
      postgres.start();

      // CA1 & T2: Migración V1 desde base vacía
      var flyway = Flyway.configure()
          .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .schemas("llm")
          .defaultSchema("llm")
          .createSchemas(true)
          .target("1")
          .load();
      int applied = flyway.migrate().migrationsExecuted;
      assertThat(applied).isEqualTo(1);

      try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
           var statement = connection.createStatement()) {

        // CA1: Verificar que existen las 6 tablas principales del esquema inicial
        var tablesRs = statement.executeQuery(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'llm' ORDER BY table_name");
        var tableNames = new java.util.ArrayList<String>();
        while (tablesRs.next()) {
          tableNames.add(tablesRs.getString(1));
        }
        assertThat(tableNames).contains(
            "rubric_versions",
            "rubric_dimensions",
            "golden_sets",
            "golden_set_entries",
            "idempotency_requests",
            "audit_events"
        );

        // CA1: Verificar Rúbrica 1.0 y sus 5 dimensiones con pesos exactos (30, 25, 20, 15, 10)
        var rubricRs = statement.executeQuery(
            "SELECT id, version, language FROM llm.rubric_versions WHERE version = '1.0'");
        assertThat(rubricRs.next()).isTrue();
        String rubricVersionId = rubricRs.getString("id");
        assertThat(rubricRs.getString("language")).isEqualTo("es");

        var dimRs = statement.executeQuery(
            "SELECT code, weight FROM llm.rubric_dimensions WHERE rubric_version_id = '" + rubricVersionId + "'");
        Map<String, Integer> dimensions = new HashMap<>();
        while (dimRs.next()) {
          dimensions.put(dimRs.getString("code"), dimRs.getInt("weight"));
        }
        assertThat(dimensions).hasSize(5);
        assertThat(dimensions).containsEntry("autonomy", 30);
        assertThat(dimensions).containsEntry("clarity", 25);
        assertThat(dimensions).containsEntry("progression", 20);
        assertThat(dimensions).containsEntry("compliance", 15);
        assertThat(dimensions).containsEntry("efficiency", 10);

        // CA2: Repetir migración es determinístico
        int repeated = flyway.migrate().migrationsExecuted;
        assertThat(repeated).isZero();

        // CA3 (negativo): El dato académico es append-only (rechaza UPDATE y DELETE)
        assertThatThrownBy(() -> statement.executeUpdate(
            "UPDATE llm.rubric_versions SET version = '1.1' WHERE version = '1.0'"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("append-only table rubric_versions cannot be changed");

        assertThatThrownBy(() -> statement.executeUpdate(
            "DELETE FROM llm.rubric_dimensions WHERE rubric_version_id = '" + rubricVersionId + "'"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("append-only table rubric_dimensions cannot be changed");

        // Preparar un golden_set base para probar CA4
        UUID goldenSetId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        statement.executeUpdate(String.format(
            "INSERT INTO llm.golden_sets (id, rubric_version_id, language, created_by_user_id, created_by_service) " +
            "VALUES ('%s', '%s', 'es', '%s', 'test-service')",
            goldenSetId, rubricVersionId, userId));

        // CA4 (negativo): Puntuación con solo 4 dimensiones se rechaza
        String fourDimsScores = "{\"autonomy\": 80, \"clarity\": 70, \"progression\": 85, \"compliance\": 90}";
        assertThatThrownBy(() -> statement.executeUpdate(String.format(
            "INSERT INTO llm.golden_set_entries (golden_set_id, transcript, reference_scores, content_hash, created_by_user_id) " +
            "VALUES ('%s', '[{\"role\":\"user\",\"content\":\"hola\"}]'::jsonb, '%s'::jsonb, 'hash123', '%s')",
            goldenSetId, fourDimsScores, userId)))
            .isInstanceOf(SQLException.class);

        // CA4 (negativo): Puntuación con score fuera de rango (120) se rechaza
        String outOfRangeScores = "{\"autonomy\": 120, \"clarity\": 70, \"progression\": 85, \"compliance\": 90, \"efficiency\": 80}";
        assertThatThrownBy(() -> statement.executeUpdate(String.format(
            "INSERT INTO llm.golden_set_entries (golden_set_id, transcript, reference_scores, content_hash, created_by_user_id) " +
            "VALUES ('%s', '[{\"role\":\"user\",\"content\":\"hola\"}]'::jsonb, '%s'::jsonb, 'hash123', '%s')",
            goldenSetId, outOfRangeScores, userId)))
            .isInstanceOf(SQLException.class);

        // Entrada válida con 5 dimensiones y valores 0–100 debe persistir correctamente
        String validScores = "{\"autonomy\": 80, \"clarity\": 70, \"progression\": 85, \"compliance\": 90, \"efficiency\": 80}";
        statement.executeUpdate(String.format(
            "INSERT INTO llm.golden_set_entries (golden_set_id, transcript, reference_scores, content_hash, created_by_user_id) " +
            "VALUES ('%s', '[{\"role\":\"user\",\"content\":\"hola\"}]'::jsonb, '%s'::jsonb, 'hash123', '%s')",
            goldenSetId, validScores, userId));

        // CA5 (negativo): Control de idempotencia por clave única compuesta
        UUID idempotencyKey = UUID.randomUUID();
        statement.executeUpdate(String.format(
            "INSERT INTO llm.idempotency_requests (operation, caller_service, delegated_user_id, idempotency_key, request_hash) " +
            "VALUES ('EVALUATE', 'gateway-service', '%s', '%s', 'hash-abc')",
            userId, idempotencyKey));

        // Intentar registrar el mismo pedido duplicado debe fallar por restricción UNIQUE
        assertThatThrownBy(() -> statement.executeUpdate(String.format(
            "INSERT INTO llm.idempotency_requests (operation, caller_service, delegated_user_id, idempotency_key, request_hash) " +
            "VALUES ('EVALUATE', 'gateway-service', '%s', '%s', 'hash-abc')",
            userId, idempotencyKey)))
            .isInstanceOf(SQLException.class);

        // T6: Auditoría guarda trazabilidad (actor, recurso, traceparent, request_id) sin transcripción
        UUID auditId = UUID.randomUUID();
        statement.executeUpdate(String.format(
            "INSERT INTO llm.audit_events (id, action, actor_service, actor_user_id, resource_type, resource_id, request_id, traceparent) " +
            "VALUES ('%s', 'CREATE_GOLDEN_SET', 'test-service', '%s', 'GOLDEN_SET', '%s', 'req-123', '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01')",
            auditId, userId, goldenSetId));

        var auditRs = statement.executeQuery("SELECT action, request_id, traceparent FROM llm.audit_events WHERE id = '" + auditId + "'");
        assertThat(auditRs.next()).isTrue();
        assertThat(auditRs.getString("action")).isEqualTo("CREATE_GOLDEN_SET");
        assertThat(auditRs.getString("request_id")).isEqualTo("req-123");
        assertThat(auditRs.getString("traceparent")).isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
      }
    }
  }
}
