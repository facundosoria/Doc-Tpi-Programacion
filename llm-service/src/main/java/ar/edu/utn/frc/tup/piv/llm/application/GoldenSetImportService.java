package ar.edu.utn.frc.tup.piv.llm.application;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetImportRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.RealCaseAnonymizer;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetImportRepository.ImportBatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List; import java.util.UUID;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class GoldenSetImportService {
 private final GoldenSetImportRepository imports; private final ObjectMapper mapper;
 public GoldenSetImportService(GoldenSetImportRepository imports,ObjectMapper mapper){this.imports=imports;this.mapper=mapper;}
 @Transactional public ImportBatch create(UUID course,UUID version,String format,List<JsonNode> rows,UUID key,UUID actor){if(!"JSON".equals(format)&&!"CSV".equals(format))throw new IllegalArgumentException("Formato de importación inválido");if(rows==null||rows.isEmpty())throw new IllegalArgumentException("El lote debe tener filas");return imports.create(course,version,format,key,actor,rows.stream().map(RealCaseAnonymizer::anonymize).toList()).orElseThrow(()->new IllegalStateException("El Golden Set no es un borrador del curso"));}
 @Transactional public void replaceRow(UUID course,UUID batch,int row,JsonNode payload){if(!imports.replaceRow(course,batch,row,RealCaseAnonymizer.anonymize(payload)))throw new IllegalStateException("La fila no puede modificarse");}
 @Transactional public ImportBatch validate(UUID course,UUID batch){if(!imports.beginValidation(course,batch))throw new IllegalStateException("El lote no puede validarse");boolean all=true;for(var row:imports.rows(course,batch)){String errors;try{errors=errors(mapper.readTree(row.payload()));}catch(Exception e){errors="[\"invalid-json\"]";}boolean valid="[]".equals(errors);imports.rowResult(batch,row.rowNumber(),valid,errors);all&=valid;}imports.finishValidation(batch,all);return new ImportBatch(batch,null,null,all?"READY":"FAILED",0);}
 @Transactional public void commit(UUID course,UUID batch){UUID version=imports.claimReady(course,batch).orElseThrow(()->new IllegalStateException("El lote debe estar READY y todas sus filas deben ser válidas"));imports.insertCases(batch,version);imports.complete(batch);}
 private String errors(JsonNode p){try{if(!p.path("transcript").isArray()||p.path("transcript").isEmpty())return "[\"transcript\"]";for(JsonNode m:p.path("transcript"))if(!("STUDENT".equals(m.path("role").asText())||"TUTOR".equals(m.path("role").asText()))||m.path("content").asText().isBlank())return "[\"transcript\"]";if(!p.path("challengeContext").isObject()||p.path("challengeContext").path("statement").asText().isBlank()||p.path("author").asText().isBlank())return "[\"context-or-author\"]";JsonNode s=p.path("referenceScores");String[] keys={"AUTONOMY","CLARITY","PROGRESSION","COMPLIANCE","EFFICIENCY"};for(String k:keys)if(!s.path(k).canConvertToInt()||s.path(k).asInt()<0||s.path(k).asInt()>100)return "[\"referenceScores\"]";return "[]";}catch(Exception e){return "[\"invalid-json\"]";}}
}
