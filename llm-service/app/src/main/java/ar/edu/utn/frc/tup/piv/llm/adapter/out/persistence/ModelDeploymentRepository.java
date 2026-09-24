package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModelDeploymentRepository {
  private final JdbcTemplate jdbc;

  public ModelDeploymentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<ModelDeploymentSummary> listEnabledDeployments() {
    try {
      List<ModelDeploymentSummary> list = jdbc.query(
          "select d.id, c.provider_key, d.model_id, d.model_version, d.state::text "
              + "from llm.model_deployments d "
              + "join llm.provider_credentials c on c.id = d.credential_id "
              + "where d.state = 'ENABLED' order by c.provider_key, d.model_id",
          (rs, rowNum) -> new ModelDeploymentSummary(
              rs.getObject(1, UUID.class),
              rs.getString(2),
              rs.getString(3),
              rs.getString(4),
              rs.getString(5)
          )
      );
      if (!list.isEmpty()) {
        return list;
      }
    } catch (Exception ignored) {
      // Fallback if table not ready or in mock mode
    }

    return List.of();
  }

  /**
   * Resuelve un despliegue por id. Desde la V26 (provider_spi_modular_architecture) la asignación
   * función→modelo guarda {@code model_deployment_id}, así que el proveedor y el modelo concreto
   * se leen desde acá y ya no desde {@code function_model_config}.
   */
  public Optional<ModelDeploymentSummary> byId(UUID deploymentId) {
    return jdbc.query(
        "select d.id, c.provider_key, d.model_id, d.model_version, d.state::text "
            + "from llm.model_deployments d "
            + "join llm.provider_credentials c on c.id = d.credential_id "
            + "where d.id = ?",
        (rs, rowNum) -> new ModelDeploymentSummary(
            rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
            rs.getString(4), rs.getString(5)),
        deploymentId).stream().findFirst();
  }

}
