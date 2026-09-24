package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayBudget;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Observabilidad del AI Gateway (EP-02): consumo, latencia, errores y presupuesto por función.
 * Los datos son en memoria (ver {@link GatewayUsageLog}); no expone prompts ni respuestas. */
@RestController
@RequestMapping("${app.api.private-path}/admin/gateway")
public class GatewayUsageController {
  private final GatewayUsageLog usageLog;
  private final GatewayBudget budget;
  private final GoldenSetAuthorization authorization;

  public GatewayUsageController(GatewayUsageLog usageLog, GatewayBudget budget, GoldenSetAuthorization authorization) {
    this.usageLog = usageLog;
    this.budget = budget;
    this.authorization = authorization;
  }

  @GetMapping("/usage")
  public UsageReport usage(@RequestHeader HttpHeaders headers) {
    authorization.require(headers);
    return new UsageReport(usageLog.summary(),
        Arrays.stream(ModelFunction.values()).map(budget::snapshot).toList());
  }

  @GetMapping("/calls")
  public List<GatewayUsageLog.CallRecord> calls(@RequestParam(defaultValue = "50") int limit,
      @RequestHeader HttpHeaders headers) {
    authorization.require(headers);
    return usageLog.recent(Math.min(Math.max(limit, 1), 200));
  }

  public record UsageReport(List<GatewayUsageLog.Summary> byFunction, List<GatewayBudget.Snapshot> budgets) {}
}
