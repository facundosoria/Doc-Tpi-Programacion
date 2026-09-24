package ar.edu.utn.frc.tup.piv.llm.infrastructure.external;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatus;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatusPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador fake para consultar el estado de un desafío hasta que practice-service
 * confirme el contrato definitivo (REST o Kafka).
 */
// TODO: reemplazar por adapter real cuando practice-service confirme el contrato (ver T1 en tareas/ep-05/h03.md)
@Component
@Profile({"default", "dev", "test"})
public class FakeChallengeStatusAdapter implements ChallengeStatusPort {

  private static final Logger log = LoggerFactory.getLogger(FakeChallengeStatusAdapter.class);

  private final Map<UUID, ChallengeStatus> overrides = new ConcurrentHashMap<>();
  private volatile ChallengeStatus defaultStatus = ChallengeStatus.ABIERTO;
  private final MeterRegistry meterRegistry;

  public FakeChallengeStatusAdapter(@Autowired(required = false) MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public FakeChallengeStatusAdapter() {
    this(null);
  }

  @Override
  public ChallengeStatus consultar(UUID challengeId) {
    ChallengeStatus result;
    if (challengeId == null) {
      result = ChallengeStatus.NO_EXISTE;
    } else {
      result = overrides.getOrDefault(challengeId, defaultStatus);
    }
    log.debug("ChallengeStatusPort.consultar challengeId={} -> status={}", challengeId, result);
    if (meterRegistry != null) {
      meterRegistry.counter("challenge.status.queries", "status", result.name()).increment();
    }
    return result;
  }

  public void setStatus(UUID challengeId, ChallengeStatus status) {
    overrides.put(challengeId, status);
  }

  public void setDefaultStatus(ChallengeStatus defaultStatus) {
    this.defaultStatus = defaultStatus;
  }

  public void clear() {
    overrides.clear();
    this.defaultStatus = ChallengeStatus.ABIERTO;
  }
}
