package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("${app.api.private-path}/model-assignments")
public class ModelAssignmentController {
  private final FunctionModelConfigRepository configs; private final GoldenSetAuthorization authorization;
  public ModelAssignmentController(FunctionModelConfigRepository configs,GoldenSetAuthorization authorization){this.configs=configs;this.authorization=authorization;}
  @GetMapping("/{function}") public Assignment get(@PathVariable String function,@RequestHeader HttpHeaders headers){authorization.require(headers);var config=configs.find(parse(function)).orElseThrow(()->new IllegalStateException("La función no tiene modelo asignado"));return new Assignment(config.modelDeploymentId());}
  @PutMapping("/{function}") public Assignment put(@PathVariable String function,@RequestBody Assignment body,@RequestHeader("Idempotency-Key") UUID idempotencyKey,@RequestHeader HttpHeaders headers){var actor=authorization.require(headers);if(body.modelDeploymentId()==null)throw new IllegalArgumentException("modelDeploymentId es obligatorio");configs.upsert(parse(function),body.modelDeploymentId(),actor);return body;}
  private ModelFunction parse(String raw){try{return ModelFunction.valueOf(raw.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException exception){throw new IllegalArgumentException("Función desconocida: "+raw);}}
  public record Assignment(UUID modelDeploymentId){}
}
