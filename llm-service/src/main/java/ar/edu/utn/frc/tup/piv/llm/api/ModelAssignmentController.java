package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** `GET`/`PUT /api/llm/model-assignments/{function}` — `docs/contracts/llm-service-v1.openapi.yaml`.
 * El catálogo `función → proveedor + modelo` que pide `LLM-S01-H10`·CA3 (RF-IA-11: "modelo por
 * función, editable por ADMIN, vía registro — no en el código"). Reusa
 * {@link GoldenSetAuthorization} (mismo `trusted-service` administrativo que el resto del
 * servicio) en vez de crear una tercera clase de autorización. */
@RestController
@RequestMapping("/api/llm/model-assignments")
public class ModelAssignmentController {
  private final FunctionModelConfigRepository configs;
  private final GoldenSetAuthorization authorization;

  public ModelAssignmentController(FunctionModelConfigRepository configs, GoldenSetAuthorization authorization) {
    this.configs = configs;
    this.authorization = authorization;
  }

  @GetMapping("/{function}")
  public Assignment get(@PathVariable String function, @RequestHeader HttpHeaders headers) {
    authorization.require(headers);
    var config = configs.find(parse(function))
        .orElseThrow(() -> new IllegalStateException("La función no tiene modelo asignado"));
    return new Assignment(config.provider(), config.modelId(), config.modelVersion());
  }

  @PutMapping("/{function}")
  public Assignment put(@PathVariable String function, @RequestBody Assignment body,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestHeader HttpHeaders headers) {
    // idempotencyKey: exigido por el contrato (parámetro obligatorio de putModelAssignment); el
    // upsert ya es naturalmente idempotente (misma fila, mismos valores), así que solo se valida
    // su presencia, no se persiste una réplica como en las operaciones de creación.
    var actor = authorization.require(headers);
    if (body.provider() == null || body.provider().isBlank() || body.modelId() == null || body.modelId().isBlank()) {
      throw new IllegalArgumentException("provider y modelId son obligatorios");
    }
    configs.upsert(parse(function), body.provider(), body.modelId(), body.modelVersion(), actor);
    return body;
  }

  private ModelFunction parse(String raw) {
    try {
      return ModelFunction.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Función desconocida: " + raw);
    }
  }

  /** Espejo del cuerpo `{provider, modelId, modelVersion}` de `putModelAssignment`. */
  public record Assignment(String provider, String modelId, String modelVersion) {}
}
