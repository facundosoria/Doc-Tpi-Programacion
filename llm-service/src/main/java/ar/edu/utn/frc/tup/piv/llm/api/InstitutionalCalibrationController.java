package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrative institutional evidence. Course APIs remain unchanged. */
@RestController
@RequestMapping("/api/llm/admin/institutional-calibration")
public class InstitutionalCalibrationController {
  private final CalibrationRunRepository runs; private final ProviderCredentialRepository deployments; private final GoldenSetAuthorization authorization;
  public InstitutionalCalibrationController(CalibrationRunRepository runs, ProviderCredentialRepository deployments, GoldenSetAuthorization authorization) {this.runs=runs;this.deployments=deployments;this.authorization=authorization;}
  @GetMapping("/profile") public CalibrationRunRepository.Profile profile(@RequestHeader HttpHeaders headers) { authorization.require(headers); return runs.profile().orElseThrow(()->new IllegalStateException("No hay perfil institucional configurado")); }
  @PostMapping("/profile") public void configure(@RequestBody ProfileRequest request,@RequestHeader HttpHeaders headers) { CallerIdentity actor=authorization.require(headers); runs.profile(request.goldenSetVersionId(),request.rubricVersionId(),actor.delegatedUserId()); }
  @GetMapping("/runs") public Page list(@RequestHeader HttpHeaders headers) { authorization.require(headers); return new Page(runs.listPlatform()); }
  @PostMapping("/runs") public ResponseEntity<CalibrationRunRepository.Run> create(@RequestHeader HttpHeaders headers) { CallerIdentity actor=authorization.require(headers); var profile=runs.profile().orElseThrow(()->new IllegalStateException("Configure el perfil institucional antes de calibrar")); var target=deployments.calibrationTarget().orElseThrow(()->new IllegalStateException("Seleccione un modelo candidato para calibrar")); return ResponseEntity.accepted().body(runs.createPlatform(profile.rubricVersionId(),profile.goldenSetVersionId(),target.id(),actor.delegatedUserId())); }
  public record ProfileRequest(UUID goldenSetVersionId,UUID rubricVersionId) {}
  public record Page(List<CalibrationRunRepository.Run> items) {}
}
