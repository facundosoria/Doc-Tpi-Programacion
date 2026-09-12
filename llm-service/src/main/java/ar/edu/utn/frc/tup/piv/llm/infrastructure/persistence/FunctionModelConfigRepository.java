package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** La tabla `función → proveedor + modelo` que pide H10·T5 (`GET`/`PUT
 * /api/llm/model-assignments/{function}` del contrato v1). Cambiar una fila no exige recompilar
 * ni re-desplegar código — H10·CA3. */
@Repository
public class FunctionModelConfigRepository {
  private final JdbcTemplate jdbc;

  public FunctionModelConfigRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Config> find(ModelFunction function) {
    return jdbc
        .query(
            "select provider, model_id, model_version, enabled from llm.function_model_config where function = ?",
            (rs, n) -> new Config(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)),
            key(function))
        .stream()
        .findFirst();
  }

  public void upsert(ModelFunction function, String provider, String modelId, String modelVersion, CallerIdentity actor) {
    jdbc.update(
        "insert into llm.function_model_config (function, provider, model_id, model_version, enabled, updated_at, updated_by_user_id) "
            + "values (?, ?, ?, ?, true, now(), ?) "
            + "on conflict (function) do update set provider = excluded.provider, model_id = excluded.model_id, "
            + "model_version = excluded.model_version, enabled = true, updated_at = now(), updated_by_user_id = excluded.updated_by_user_id",
        key(function), provider, modelId, modelVersion, actor.delegatedUserId());
  }

  private String key(ModelFunction function) {
    return function.name().toLowerCase(Locale.ROOT);
  }

  public record Config(String provider, String modelId, String modelVersion, boolean enabled) {}
}
