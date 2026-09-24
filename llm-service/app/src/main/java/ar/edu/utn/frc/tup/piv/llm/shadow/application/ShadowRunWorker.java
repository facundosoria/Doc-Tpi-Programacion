package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Toma corridas de shadow de a una y las ejecuta. Corre en el scheduler, nunca en un request, y se
 * apaga con {@code llm.shadow.enabled=false} (kill switch: el shadow duplica llamadas al proveedor). */
@Component
@ConditionalOnProperty(prefix = "llm.shadow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ShadowRunWorker {
  private final ShadowRunStore store;
  private final ShadowEvaluationRunner runner;
  private final int retentionDays;

  public ShadowRunWorker(ShadowRunStore store, ShadowEvaluationRunner runner,
      @Value("${llm.shadow.retention-days:30}") int retentionDays) {
    this.store = store;
    this.runner = runner;
    this.retentionDays = retentionDays;
  }

  @Scheduled(fixedDelayString = "${llm.shadow.dispatch-delay-ms:2000}")
  public void dispatch() {
    store.claimNextQueued().ifPresent(run -> runner.execute(run.id()));
  }

  /** Los resultados son datos derivados de conversaciones reales: se borran pasado el plazo. */
  @Scheduled(cron = "${llm.shadow.purge-cron:0 30 3 * * *}")
  public void purge() {
    store.purgeFinishedBefore(retentionDays);
  }
}
