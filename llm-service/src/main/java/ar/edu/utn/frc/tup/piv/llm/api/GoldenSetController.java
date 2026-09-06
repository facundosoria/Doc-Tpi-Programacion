package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.GoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.domain.GoldenSet;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/llm/golden-sets")
public class GoldenSetController {
  private final GoldenSetService service;
  private final GoldenSetAuthorization authorization;
  public GoldenSetController(GoldenSetService service, GoldenSetAuthorization authorization) { this.service = service; this.authorization = authorization; }
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateGoldenSetRequest request, @RequestHeader("Idempotency-Key") UUID key, @RequestHeader HttpHeaders headers) {
    var body = service.create(request.rubricVersion(), request.language(), key, authorization.require(headers));
    return ResponseEntity.created(URI.create("/api/llm/golden-sets/" + body.path("id").asText())).body(body);
  }
  @PostMapping("/{goldenSetId}/entries")
  public ResponseEntity<?> addEntry(@PathVariable UUID goldenSetId, @Valid @RequestBody CreateGoldenSetEntryRequest request, @RequestHeader("Idempotency-Key") UUID key, @RequestHeader HttpHeaders headers) {
    var body = service.addEntry(goldenSetId, request.transcript(), request.referenceScores(), key, authorization.require(headers));
    return ResponseEntity.created(URI.create("/api/llm/golden-sets/" + goldenSetId + "/entries/" + body.path("id").asText())).body(body);
  }
  @GetMapping
  public List<GoldenSet> list(@RequestParam(required = false) String rubricVersion, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @RequestHeader HttpHeaders headers) {
    authorization.require(headers); return service.list(rubricVersion, page, size);
  }
  @GetMapping("/{goldenSetId}")
  public GoldenSet get(@PathVariable UUID goldenSetId, @RequestHeader HttpHeaders headers) { authorization.require(headers); return service.get(goldenSetId); }
}
