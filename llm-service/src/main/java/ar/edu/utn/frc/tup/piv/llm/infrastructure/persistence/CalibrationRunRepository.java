package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Durable PAR-14 executions; provider traffic intentionally stays outside this adapter. */
@Repository
public class CalibrationRunRepository {
  private final JdbcTemplate jdbc; private final ObjectMapper json;
  public CalibrationRunRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
  /** Compatibility entry point retained for existing course callers. */
  public Run create(UUID course, UUID rubric, UUID golden, UUID deployment, UUID key, UUID actor) { return createCourse(course,rubric,golden,deployment,key,actor); }
  public Run createCourse(UUID course, UUID rubric, UUID golden, UUID deployment, UUID key, UUID actor) {
    var old=findKey(course,key); if(old.isPresent()) return old.get(); UUID id=UUID.randomUUID();
    int n=jdbc.update("insert into llm.calibration_runs(id,course_id,rubric_version_id,golden_set_version_id,model_deployment_id,reason,created_by_user_id,idempotency_key,stage) select ?,?,?,?,?, 'MANUAL',?,?,'COURSE' where exists(select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='COURSE' and f.course_id=?) and exists(select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='COURSE' and f.course_id=?)",id,course,rubric,golden,deployment,actor,key,rubric,course,golden,course);
    if(n==0) throw new IllegalStateException("Las versiones publicadas no pertenecen al curso"); return byId(id).orElseThrow();
  }
  public Run createPlatform(UUID rubric,UUID golden,UUID deployment,UUID actor) {
    UUID id=UUID.randomUUID(); int n=jdbc.update("insert into llm.calibration_runs(id,rubric_version_id,golden_set_version_id,model_deployment_id,reason,created_by_user_id,stage) select ?,?,?,?,'MANUAL',?,'PLATFORM' where exists(select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM') and exists(select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM')",id,rubric,golden,deployment,actor,rubric,golden);
    if(n==0) throw new IllegalStateException("El perfil institucional requiere versiones PLATFORM publicadas"); return byId(id).orElseThrow();
  }
  public Optional<Run> claimNextQueued() { return jdbc.query("with next as (select id from llm.calibration_runs where state='QUEUED' order by created_at for update skip locked limit 1) update llm.calibration_runs r set state='RUNNING',started_at=now() from next where r.id=next.id returning r.id,r.course_id,r.stage::text,r.state::text,r.progress,r.rubric_version_id,r.golden_set_version_id,r.model_deployment_id,r.mae_final,r.max_individual_error,r.reason,r.created_at,r.finished_at,r.failure_code,r.failure_detail",(rs,row)->row(rs)).stream().findFirst(); }
  public Optional<Run> find(UUID course,UUID id) { return jdbc.query(base()+" where r.course_id=? and r.id=?",(rs,row)->row(rs),course,id).stream().findFirst(); }
  public Optional<Run> byId(UUID id) { return jdbc.query(base()+" where r.id=?",(rs,row)->row(rs),id).stream().findFirst(); }
  public List<Run> list(UUID course) { return jdbc.query(base()+" where r.course_id=? order by r.created_at desc",(rs,row)->row(rs),course); }
  public List<Run> listPlatform() { return jdbc.query(base()+" where r.stage='PLATFORM' order by r.created_at desc",(rs,row)->row(rs)); }
  public Optional<Profile> profile() { return jdbc.query("select golden_set_version_id,rubric_version_id,configured_at from llm.institutional_calibration_profiles",(rs,row)->new Profile(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getTimestamp(3).toInstant())).stream().findFirst(); }
  public void profile(UUID golden,UUID rubric,UUID actor) {
    Integer valid=jdbc.queryForObject("select (exists(select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM') and exists(select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM'))::int",Integer.class,golden,rubric);
    if(valid==null||valid!=1) throw new IllegalStateException("El perfil requiere Golden Set y rúbrica institucionales publicados");
    jdbc.update("insert into llm.institutional_calibration_profiles(id,golden_set_version_id,rubric_version_id,configured_by_user_id) values(true,?,?,?) on conflict(id) do update set golden_set_version_id=excluded.golden_set_version_id,rubric_version_id=excluded.rubric_version_id,configured_by_user_id=excluded.configured_by_user_id,configured_at=now()",golden,rubric,actor);
  }
  public Execution execution(UUID id) {
    Run run=byId(id).filter(x->"RUNNING".equals(x.state())).orElseThrow(()->new IllegalStateException("La corrida no está disponible"));
    Deployment deployment=jdbc.query("select d.id,c.provider,c.base_url,c.encrypted_secret,c.secret_nonce,d.model_id from llm.model_deployments d join llm.provider_credentials c on c.id=d.credential_id where d.id=? and c.state='ACTIVE'",(rs,row)->new Deployment(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getBytes(4),rs.getBytes(5),rs.getString(6)),run.modelDeploymentId()).stream().findFirst().orElseThrow(()->new IllegalStateException("El candidato no tiene credencial activa"));
    Map<Dimension,Integer> weights=new EnumMap<>(Dimension.class); StringBuilder rubric=new StringBuilder(); jdbc.query("select dimension_key,label,criterion,anchors::text,weight from llm.rubric_dimension_v2 where rubric_version_id=? order by dimension_key",rs->{var dimension=Dimension.valueOf(rs.getString(1));weights.put(dimension,rs.getBigDecimal(5).intValueExact());rubric.append(dimension).append(" (peso ").append(rs.getBigDecimal(5)).append("): ").append(rs.getString(3)).append(" Anclas: ").append(rs.getString(4)).append('\n');},run.rubricVersionId());
    var cases=jdbc.query("select id,transcript::text,challenge_context::text,reference_scores::text from llm.golden_set_cases where golden_set_version_id=? order by case_order",(rs,row)->new Case(rs.getObject(1,UUID.class),parse(rs.getString(2)),parse(rs.getString(3)),scores(parse(rs.getString(4)))),run.goldenSetVersionId());
    if(cases.isEmpty())throw new IllegalStateException("El Golden Set publicado no contiene casos"); return new Execution(run,deployment,Map.copyOf(weights),rubric.toString(),List.copyOf(cases));
  }
  public void saveCase(UUID run,Case c,Map<Dimension,Integer> model,Map<Dimension,Integer> weights) { BigDecimal human=finalScore(c.humanScores(),weights), calculated=finalScore(model,weights); Map<Dimension,Integer> errors=new EnumMap<>(Dimension.class); for(var d:Dimension.values())errors.put(d,Math.abs(c.humanScores().get(d)-model.get(d))); jdbc.update("insert into llm.calibration_case_results(calibration_run_id,golden_set_case_id,model_scores,human_final_score,model_final_score,dimension_errors,final_error,output_artifact) values(?,?,cast(? as jsonb),?,?,cast(? as jsonb),?,cast(? as jsonb)) on conflict(calibration_run_id,golden_set_case_id) do nothing",run,c.id(),write(scoresNode(model)),human,calculated,write(scoresNode(errors)),human.subtract(calculated).abs(),write(scoresNode(model))); }
  public void progress(UUID id,int value){jdbc.update("update llm.calibration_runs set progress=? where id=? and state='RUNNING'",value,id);}
  public void finish(UUID id,boolean pass,BigDecimal mae,int max){jdbc.update("update llm.calibration_runs set state=cast(? as llm.calibration_state),progress=100,mae_final=?,max_individual_error=?,finished_at=now() where id=? and state='RUNNING'",pass?"PASSED":"FAILED",mae,max,id);}
  public void fail(UUID id,String code,String detail){jdbc.update("update llm.calibration_runs set state='FAILED',failure_code=?,failure_detail=?,finished_at=now() where id=? and state='RUNNING'",code,detail,id);}
  public boolean doubleEvidence(UUID deployment){return jdbc.queryForObject("select exists(select 1 from llm.calibration_runs where model_deployment_id=? and stage='PLATFORM' and state='PASSED') and exists(select 1 from llm.calibration_runs where model_deployment_id=? and stage='COURSE' and state='PASSED')",Boolean.class,deployment,deployment);}
  private String base(){return "select r.id,r.course_id,r.stage::text,r.state::text,r.progress,r.rubric_version_id,r.golden_set_version_id,r.model_deployment_id,r.mae_final,r.max_individual_error,r.reason,r.created_at,r.finished_at,r.failure_code,r.failure_detail from llm.calibration_runs r";}
  private Optional<Run> findKey(UUID course,UUID key){return jdbc.query(base()+" where r.course_id=? and r.idempotency_key=?",(rs,row)->row(rs),course,key).stream().findFirst();}
  private Run row(java.sql.ResultSet rs)throws java.sql.SQLException{return new Run(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getObject(6,UUID.class),rs.getObject(7,UUID.class),rs.getObject(8,UUID.class),rs.getBigDecimal(9),rs.getObject(10,Integer.class),rs.getString(11),rs.getTimestamp(12).toInstant(),rs.getTimestamp(13)==null?null:rs.getTimestamp(13).toInstant(),rs.getString(14),rs.getString(15));}
  private JsonNode parse(String s){try{return json.readTree(s);}catch(Exception e){throw new IllegalStateException("Datos de calibración inválidos",e);}}
  private Map<Dimension,Integer> scores(JsonNode node){Map<Dimension,Integer> result=new EnumMap<>(Dimension.class);for(var d:Dimension.values())result.put(d,node.path(d.name()).asInt(-1));return Map.copyOf(result);}
  private BigDecimal finalScore(Map<Dimension,Integer> scores,Map<Dimension,Integer> weights){BigDecimal result=BigDecimal.ZERO;for(var d:Dimension.values())result=result.add(BigDecimal.valueOf(scores.get(d)).multiply(BigDecimal.valueOf(weights.get(d))).movePointLeft(2));return result;}
  private JsonNode scoresNode(Map<Dimension,Integer> scores){var node=json.createObjectNode();scores.forEach((d,v)->node.put(d.name(),v));return node;}
  private String write(JsonNode n){try{return json.writeValueAsString(n);}catch(Exception e){throw new IllegalStateException(e);}}
  public record Run(UUID id,UUID courseId,String stage,String state,int progress,UUID rubricVersionId,UUID goldenSetVersionId,UUID modelDeploymentId,BigDecimal maeFinal,Integer maxIndividualError,String reason,java.time.Instant createdAt,java.time.Instant finishedAt,String failureCode,String failureDetail){public Run(UUID id,String state,int progress){this(id,null,"COURSE",state,progress,null,null,null,null,null,"MANUAL",java.time.Instant.now(),null,null,null);}}
  public record Profile(UUID goldenSetVersionId,UUID rubricVersionId,java.time.Instant configuredAt){}
  public record Deployment(UUID id,String provider,String baseUrl,byte[] encryptedSecret,byte[] nonce,String modelId){}
  public record Case(UUID id,JsonNode transcript,JsonNode challengeContext,Map<Dimension,Integer> humanScores){}
  public record Execution(Run run,Deployment deployment,Map<Dimension,Integer> weights,String rubric,List<Case> cases){}
}
