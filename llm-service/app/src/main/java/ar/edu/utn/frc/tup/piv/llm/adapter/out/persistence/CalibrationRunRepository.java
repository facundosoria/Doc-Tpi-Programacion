package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

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
  public List<Run> createStability(UUID course, UUID rubric, UUID golden, UUID deployment, UUID key, UUID actor, List<Long> seeds) {
    var existing = jdbc.query("select id from llm.calibration_stability_groups where course_id=? and idempotency_key=?", (rs,row)->rs.getObject(1,UUID.class),course,key);
    UUID group = existing.isEmpty() ? UUID.randomUUID() : existing.getFirst();
    if (existing.isEmpty()) {
      jdbc.update("insert into llm.calibration_stability_groups(id,course_id,idempotency_key) values(?,?,?)", group,course,key);
      for (int i=0;i<seeds.size();i++) {
        UUID id=UUID.randomUUID(); int n=jdbc.update("insert into llm.calibration_runs(id,course_id,rubric_version_id,golden_set_version_id,model_deployment_id,reason,created_by_user_id,idempotency_key,stage,stability_group_id,stability_ordinal,calibration_seed) select ?,?,?,?,?, 'MANUAL',?,?,'COURSE',?,?,? where exists(select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='COURSE' and f.course_id=?) and exists(select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='COURSE' and f.course_id=?)",id,course,rubric,golden,deployment,actor,UUID.randomUUID(),group,i+1,seeds.get(i),rubric,course,golden,course);
        if(n==0) throw new IllegalStateException("Las versiones publicadas no pertenecen al curso");
      }
    }
    return jdbc.query(base()+" where r.stability_group_id=? order by r.stability_ordinal",(rs,row)->row(rs),group);
  }
  /** Compatibilidad: sin key de idempotencia (callers previos). */
  public Run createPlatform(UUID rubric,UUID golden,UUID deployment,UUID actor) {
    return createPlatform(rubric,golden,deployment,null,actor);
  }
  /** Corrida institucional deduplicada por Idempotency-Key (el índice único no cubre course_id NULL). */
  public Run createPlatform(UUID rubric,UUID golden,UUID deployment,UUID key,UUID actor) {
    if (key != null) {
      var existing=jdbc.query(base()+" where r.stage='PLATFORM' and r.course_id is null and r.idempotency_key=?",(rs,row)->row(rs),key).stream().findFirst();
      if (existing.isPresent()) return existing.get();
    }
    UUID id=UUID.randomUUID(); int n=jdbc.update("insert into llm.calibration_runs(id,rubric_version_id,golden_set_version_id,model_deployment_id,reason,created_by_user_id,idempotency_key,stage) select ?,?,?,?,'MANUAL',?,?,'PLATFORM' where exists(select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM') and exists(select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM')",id,rubric,golden,deployment,actor,key,rubric,golden);
    if(n==0) throw new IllegalStateException("El perfil institucional requiere versiones PLATFORM publicadas"); return byId(id).orElseThrow();
  }
  public Optional<Run> claimNextQueued() { return jdbc.query("with next as (select id from llm.calibration_runs where state='QUEUED' order by created_at for update skip locked limit 1) update llm.calibration_runs r set state='RUNNING',started_at=now() from next where r.id=next.id returning r.id,r.course_id,r.stage::text,r.state::text,r.progress,r.rubric_version_id,r.golden_set_version_id,r.model_deployment_id,r.mae_final,r.max_individual_error,r.reason,r.created_at,r.finished_at,r.failure_code,r.failure_detail,r.expiration_reason",(rs,row)->row(rs)).stream().findFirst(); }
  public Optional<Run> find(UUID course,UUID id) { return jdbc.query(base()+" where r.course_id=? and r.id=?",(rs,row)->row(rs),course,id).stream().findFirst(); }
  public Optional<Run> byId(UUID id) { return jdbc.query(base()+" where r.id=?",(rs,row)->row(rs),id).stream().findFirst(); }
  public List<Run> list(UUID course) { return jdbc.query(base()+" where r.course_id=? order by r.created_at desc",(rs,row)->row(rs),course); }
  public List<StabilityGroup> stabilityGroups(UUID course) { var groups=jdbc.query("select id,state,mae_spread,created_at,finished_at from llm.calibration_stability_groups where course_id=? order by created_at desc",(rs,row)->new StabilityGroup(rs.getObject(1,UUID.class),rs.getString(2),rs.getBigDecimal(3),rs.getTimestamp(4).toInstant(),rs.getTimestamp(5)==null?null:rs.getTimestamp(5).toInstant(),List.of()),course); return groups.stream().map(g->new StabilityGroup(g.id(),g.state(),g.maeSpread(),g.createdAt(),g.finishedAt(),jdbc.query(base()+" where r.stability_group_id=? order by r.stability_ordinal",(rs,row)->row(rs),g.id()))).toList(); }
  public List<Run> listPlatform() { return jdbc.query(base()+" where r.stage='PLATFORM' order by r.created_at desc",(rs,row)->row(rs)); }
  /** Promedio de error por dimensión de una corrida (claves del JSON en mayúscula o minúscula). */
  public Map<Dimension, BigDecimal> dimensionErrors(UUID runId) {
    var rows=jdbc.query("select dimension_errors::text from llm.calibration_case_results where calibration_run_id=? order by golden_set_case_id",(rs,row)->parse(rs.getString(1)),runId);
    if (rows.isEmpty()) return Map.of();
    Map<Dimension,BigDecimal> totals=new EnumMap<>(Dimension.class);
    for (var d:Dimension.values()) totals.put(d,BigDecimal.ZERO);
    for (var node:rows) {
      for (var d:Dimension.values()) {
        JsonNode value=node.get(d.name());
        if (value == null) value=node.get(d.name().toLowerCase());
        totals.put(d,totals.get(d).add(BigDecimal.valueOf(value == null ? 0 : value.asInt())));
      }
    }
    BigDecimal count=BigDecimal.valueOf(rows.size());
    Map<Dimension,BigDecimal> averages=new EnumMap<>(Dimension.class);
    for (var d:Dimension.values()) averages.put(d,totals.get(d).divide(count,4,java.math.RoundingMode.HALF_UP));
    return Map.copyOf(averages);
  }
  public Optional<Profile> profile() { return jdbc.query("select golden_set_version_id,rubric_version_id,configured_at from llm.institutional_calibration_profiles",(rs,row)->new Profile(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getTimestamp(3).toInstant())).stream().findFirst(); }
  public void profile(UUID golden,UUID rubric,UUID actor) {
    Integer valid=jdbc.queryForObject("select (exists(select 1 from llm.golden_set_versions v join llm.golden_set_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM') and exists(select 1 from llm.rubric_version_v2 v join llm.rubric_families f on f.id=v.family_id where v.id=? and v.state='PUBLISHED' and f.scope='PLATFORM'))::int",Integer.class,golden,rubric);
    if(valid==null||valid!=1) throw new IllegalStateException("El perfil requiere Golden Set y rúbrica institucionales publicados");
    jdbc.update("insert into llm.institutional_calibration_profiles(id,golden_set_version_id,rubric_version_id,configured_by_user_id) values(true,?,?,?) on conflict(id) do update set golden_set_version_id=excluded.golden_set_version_id,rubric_version_id=excluded.rubric_version_id,configured_by_user_id=excluded.configured_by_user_id,configured_at=now()",golden,rubric,actor);
  }
  public Execution execution(UUID id) {
    Run run=byId(id).filter(x->"RUNNING".equals(x.state())).orElseThrow(()->new IllegalStateException("La corrida no está disponible"));
    Deployment deployment=jdbc.query("select d.id,d.credential_id,c.provider_key,d.model_id from llm.model_deployments d join llm.provider_credentials c on c.id=d.credential_id where d.id=? and c.state='ACTIVE'",(rs,row)->new Deployment(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4)),run.modelDeploymentId()).stream().findFirst().orElseThrow(()->new IllegalStateException("El candidato no tiene credencial activa"));
    var rubricMeta=jdbc.query("select rubric_kind, user_prompt from llm.rubric_version_v2 where id=?",(rs,row)->Map.entry(
        rs.getString("rubric_kind")!=null?rs.getString("rubric_kind"):"DEFAULT_INSTITUTIONAL",
        rs.getString("user_prompt")!=null?rs.getString("user_prompt"):""
    ),run.rubricVersionId()).stream().findFirst().orElse(Map.entry("DEFAULT_INSTITUTIONAL",""));
    String rubricKind=rubricMeta.getKey(); String userPrompt=rubricMeta.getValue();
    Map<Dimension,Integer> weights=new EnumMap<>(Dimension.class);
    Map<String,Integer> dynamicWeights=new java.util.LinkedHashMap<>();
    List<String> dimensionKeys=new java.util.ArrayList<>();
    StringBuilder rubric=new StringBuilder();
    if("MODULAR_CUSTOM".equalsIgnoreCase(rubricKind)){
      jdbc.query("select dimension_key,label,criterion,anchors::text,weight from llm.rubric_custom_dimensions where rubric_version_id=? order by display_order, dimension_key",rs->{
        String key=rs.getString(1); int weight=rs.getBigDecimal(5).intValueExact();
        dynamicWeights.put(key,weight); dimensionKeys.add(key);
        rubric.append(key).append(" (").append(rs.getString(2)).append(", peso ").append(rs.getBigDecimal(5)).append("): ").append(rs.getString(3)).append(" Anclas: ").append(rs.getString(4)).append('\n');
      },run.rubricVersionId());
    } else {
      jdbc.query("select dimension_key,label,criterion,anchors::text,weight from llm.rubric_dimension_v2 where rubric_version_id=? order by dimension_key",rs->{
        var dimension=Dimension.valueOf(rs.getString(1));
        weights.put(dimension,rs.getBigDecimal(5).intValueExact());
        dynamicWeights.put(dimension.name(),rs.getBigDecimal(5).intValueExact());
        dimensionKeys.add(dimension.name());
        rubric.append(dimension).append(" (peso ").append(rs.getBigDecimal(5)).append("): ").append(rs.getString(3)).append(" Anclas: ").append(rs.getString(4)).append('\n');
      },run.rubricVersionId());
    }
    var cases=jdbc.query("select id,transcript::text,challenge_context::text,reference_scores::text from llm.golden_set_cases where golden_set_version_id=? order by case_order",(rs,row)->{
      JsonNode refScoresNode=parse(rs.getString(4));
      Map<String,Integer> dynScores=new java.util.LinkedHashMap<>();
      refScoresNode.fieldNames().forEachRemaining(fn->dynScores.put(fn,refScoresNode.path(fn).asInt(-1)));
      Map<Dimension,Integer> stdScores="MODULAR_CUSTOM".equalsIgnoreCase(rubricKind)?Map.of():scores(refScoresNode);
      return new Case(rs.getObject(1,UUID.class),parse(rs.getString(2)),parse(rs.getString(3)),stdScores,Map.copyOf(dynScores));
    },run.goldenSetVersionId());
    Long seed=jdbc.query("select calibration_seed from llm.calibration_runs where id=?",(rs,row)->rs.getObject(1,Long.class),id).stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
    if(cases.isEmpty())throw new IllegalStateException("El Golden Set publicado no contiene casos");
    return new Execution(run,deployment,Map.copyOf(weights),rubric.toString(),List.copyOf(cases),seed,rubricKind,userPrompt,List.copyOf(dimensionKeys),Map.copyOf(dynamicWeights));
  }
  public void saveCase(UUID run,Case c,Map<Dimension,Integer> model,Map<Dimension,Integer> weights) { BigDecimal human=finalScore(c.humanScores(),weights), calculated=finalScore(model,weights); Map<Dimension,Integer> errors=new EnumMap<>(Dimension.class); for(var d:Dimension.values())errors.put(d,Math.abs(c.humanScores().get(d)-model.get(d))); jdbc.update("insert into llm.calibration_case_results(calibration_run_id,golden_set_case_id,model_scores,human_final_score,model_final_score,dimension_errors,final_error,output_artifact) values(?,?,cast(? as jsonb),?,?,cast(? as jsonb),?,cast(? as jsonb)) on conflict(calibration_run_id,golden_set_case_id) do nothing",run,c.id(),write(scoresNode(model)),human,calculated,write(scoresNode(errors)),human.subtract(calculated).abs(),write(scoresNode(model))); }
  public void saveCaseModular(UUID run,Case c,Map<String,Integer> model,Map<String,Integer> weights) {
    BigDecimal human=finalScoreModular(c.dynamicHumanScores(),weights), calculated=finalScoreModular(model,weights);
    Map<String,Integer> errors=new java.util.LinkedHashMap<>();
    for(var k:weights.keySet()){
      int hVal=c.dynamicHumanScores().getOrDefault(k,0);
      int mVal=model.getOrDefault(k,0);
      errors.put(k,Math.abs(hVal-mVal));
    }
    var modelNode=json.createObjectNode(); model.forEach(modelNode::put);
    var errorsNode=json.createObjectNode(); errors.forEach(errorsNode::put);
    jdbc.update("insert into llm.calibration_case_results(calibration_run_id,golden_set_case_id,model_scores,human_final_score,model_final_score,dimension_errors,final_error,output_artifact) values(?,?,cast(? as jsonb),?,?,cast(? as jsonb),?,cast(? as jsonb)) on conflict(calibration_run_id,golden_set_case_id) do nothing",
        run,c.id(),write(modelNode),human,calculated,write(errorsNode),human.subtract(calculated).abs(),write(modelNode));
  }
  public void progress(UUID id,int value){jdbc.update("update llm.calibration_runs set progress=? where id=? and state='RUNNING'",value,id);}
  public void recordInference(UUID id, String policy, String fingerprint) { jdbc.update("update llm.calibration_runs set inference_policy=cast(? as jsonb),provider_fingerprint=? where id=?",policy,fingerprint,id); }
  public void finish(UUID id,boolean pass,BigDecimal mae,int max){jdbc.update("update llm.calibration_runs set state=cast(? as llm.calibration_state),progress=100,mae_final=?,max_individual_error=?,finished_at=now() where id=? and state='RUNNING'",pass?"PASSED":"FAILED",mae,max,id);}
  public void fail(UUID id,String code,String detail){jdbc.update("update llm.calibration_runs set state='FAILED',failure_code=?,failure_detail=?,finished_at=now() where id=? and state='RUNNING'",code,detail,id);}
  public void expire(UUID id, String reason) { jdbc.update("update llm.calibration_runs set state='EXPIRED', expiration_reason=? where id=?", reason, id); }
  public void refreshStability(UUID runId) { jdbc.update("update llm.calibration_stability_groups g set state=case when exists(select 1 from llm.calibration_runs r where r.stability_group_id=g.id and r.state='FAILED') then 'FAILED' when (select count(*) from llm.calibration_runs r where r.stability_group_id=g.id)=3 and not exists(select 1 from llm.calibration_runs r where r.stability_group_id=g.id and r.state not in ('PASSED','FAILED')) and (select max(mae_final)-min(mae_final) from llm.calibration_runs r where r.stability_group_id=g.id)<=1.0000 then 'STABLE_PASSED' else g.state end, mae_spread=(select max(mae_final)-min(mae_final) from llm.calibration_runs r where r.stability_group_id=g.id), finished_at=case when not exists(select 1 from llm.calibration_runs r where r.stability_group_id=g.id and r.state in ('QUEUED','RUNNING')) then now() else g.finished_at end where g.id=(select stability_group_id from llm.calibration_runs where id=?) and g.state='RUNNING'",runId); }
  public boolean doubleEvidence(UUID deployment){return jdbc.queryForObject("select exists(select 1 from llm.calibration_runs where model_deployment_id=? and stage='PLATFORM' and state='PASSED') and exists(select 1 from llm.calibration_runs where model_deployment_id=? and stage='COURSE' and state='PASSED')",Boolean.class,deployment,deployment);}
  private String base(){return "select r.id,r.course_id,r.stage::text,r.state::text,r.progress,r.rubric_version_id,r.golden_set_version_id,r.model_deployment_id,r.mae_final,r.max_individual_error,r.reason,r.created_at,r.finished_at,r.failure_code,r.failure_detail,r.expiration_reason from llm.calibration_runs r";}
  private Optional<Run> findKey(UUID course,UUID key){return jdbc.query(base()+" where r.course_id=? and r.idempotency_key=?",(rs,row)->row(rs),course,key).stream().findFirst();}
  private Run row(java.sql.ResultSet rs)throws java.sql.SQLException{return new Run(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getObject(6,UUID.class),rs.getObject(7,UUID.class),rs.getObject(8,UUID.class),rs.getBigDecimal(9),rs.getObject(10,Integer.class),rs.getString(11),rs.getTimestamp(12).toInstant(),rs.getTimestamp(13)==null?null:rs.getTimestamp(13).toInstant(),rs.getString(14),rs.getString(15),rs.getString(16));}
  private JsonNode parse(String s){try{return json.readTree(s);}catch(Exception e){throw new IllegalStateException("Datos de calibración inválidos",e);}}
  private Map<Dimension,Integer> scores(JsonNode node){Map<Dimension,Integer> result=new EnumMap<>(Dimension.class);for(var d:Dimension.values())result.put(d,node.path(d.name()).asInt(-1));return Map.copyOf(result);}
  private BigDecimal finalScore(Map<Dimension,Integer> scores,Map<Dimension,Integer> weights){BigDecimal result=BigDecimal.ZERO;for(var d:Dimension.values())result=result.add(BigDecimal.valueOf(scores.get(d)).multiply(BigDecimal.valueOf(weights.get(d))).movePointLeft(2));return result;}
  private BigDecimal finalScoreModular(Map<String,Integer> scores,Map<String,Integer> weights){BigDecimal result=BigDecimal.ZERO;if(weights==null||weights.isEmpty())return result;for(var entry:weights.entrySet()){int s=scores.getOrDefault(entry.getKey(),0);result=result.add(BigDecimal.valueOf(s).multiply(BigDecimal.valueOf(entry.getValue())).movePointLeft(2));}return result;}
  private JsonNode scoresNode(Map<Dimension,Integer> scores){var node=json.createObjectNode();scores.forEach((d,v)->node.put(d.name(),v));return node;}
  private String write(JsonNode n){try{return json.writeValueAsString(n);}catch(Exception e){throw new IllegalStateException(e);}}
  public record Run(UUID id,UUID courseId,String stage,String state,int progress,UUID rubricVersionId,UUID goldenSetVersionId,UUID modelDeploymentId,BigDecimal maeFinal,Integer maxIndividualError,String reason,java.time.Instant createdAt,java.time.Instant finishedAt,String failureCode,String failureDetail,String expirationReason){public Run(UUID id,String state,int progress){this(id,null,"COURSE",state,progress,null,null,null,null,null,"MANUAL",java.time.Instant.now(),null,null,null,null);}}
  public record Profile(UUID goldenSetVersionId,UUID rubricVersionId,java.time.Instant configuredAt){}
  public record StabilityGroup(UUID id,String state,BigDecimal maeSpread,java.time.Instant createdAt,java.time.Instant finishedAt,List<Run> runs){}
  public record Deployment(UUID id,UUID credentialId,String providerKey,String modelId){}
  public record Case(UUID id,JsonNode transcript,JsonNode challengeContext,Map<Dimension,Integer> humanScores,Map<String,Integer> dynamicHumanScores){public Case(UUID id,JsonNode transcript,JsonNode challengeContext,Map<Dimension,Integer> humanScores){this(id,transcript,challengeContext,humanScores,Map.of());}}
  public record Execution(Run run,Deployment deployment,Map<Dimension,Integer> weights,String rubric,List<Case> cases,Long seed,String rubricKind,String userPrompt,List<String> dimensionKeys,Map<String,Integer> dynamicWeights){public Execution(Run run,Deployment deployment,Map<Dimension,Integer> weights,String rubric,List<Case> cases,Long seed){this(run,deployment,weights,rubric,cases,seed,"DEFAULT_INSTITUTIONAL",null,List.of("AUTONOMY","CLARITY","PROGRESSION","COMPLIANCE","EFFICIENCY"),Map.of());}public UUID courseId(){return run!=null?run.courseId():null;}}

  /**
   * Devuelve la corrida como record de dominio; lo usan el runner de evaluación, el módulo shadow y
   * los tests de integración. {@link #byId(UUID)} sigue devolviendo la fila cruda del adaptador.
   */
  public Optional<ar.edu.utn.frc.tup.piv.llm.domain.calibration.CalibrationRun> findById(UUID id) {
    return byId(id).map(CalibrationRunRepository::toDomain);
  }

  private static ar.edu.utn.frc.tup.piv.llm.domain.calibration.CalibrationRun toDomain(Run r) {
    return new ar.edu.utn.frc.tup.piv.llm.domain.calibration.CalibrationRun(
        r.id(), r.state(), r.progress(), r.rubricVersionId(), r.goldenSetVersionId(), r.modelDeploymentId(),
        r.maeFinal() == null ? null : r.maeFinal().doubleValue(), r.maxIndividualError(), r.reason(),
        r.createdAt(), r.finishedAt());
  }

  /** Guarda progreso y métricas parciales de una corrida (usado por el runner de evaluación). */
  public void recordProgress(UUID id, int progress, Double maeFinal, Integer maxIndividualError) {
    jdbc.update("update llm.calibration_runs set progress=?, mae_final=?, max_individual_error=? where id=?",
        progress, maeFinal == null ? null : java.math.BigDecimal.valueOf(maeFinal), maxIndividualError, id);
  }
}
