package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Function assignment references a deployment, never a provider/model string pair. */
@Repository
public class FunctionModelConfigRepository {
  private final JdbcTemplate jdbc;
  public FunctionModelConfigRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  public Optional<Config> find(ModelFunction function){return jdbc.query("select model_deployment_id,enabled from llm.function_model_config where function=?",(rs,n)->new Config(rs.getObject(1,UUID.class),rs.getBoolean(2)),key(function)).stream().findFirst();}
  public void upsert(ModelFunction function,UUID deploymentId,CallerIdentity actor){jdbc.update("insert into llm.function_model_config(function,model_deployment_id,enabled,updated_at,updated_by_user_id) values(?,?,true,now(),?) on conflict(function) do update set model_deployment_id=excluded.model_deployment_id,enabled=true,updated_at=now(),updated_by_user_id=excluded.updated_by_user_id",key(function),deploymentId,actor.delegatedUserId());}
  private String key(ModelFunction function){return function.name().toLowerCase(Locale.ROOT);}
  public record Config(UUID modelDeploymentId,boolean enabled){}
}
