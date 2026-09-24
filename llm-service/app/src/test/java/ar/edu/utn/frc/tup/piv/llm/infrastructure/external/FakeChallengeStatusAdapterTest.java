package ar.edu.utn.frc.tup.piv.llm.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatus;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatusPort;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatusPortContractTest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FakeChallengeStatusAdapterTest extends ChallengeStatusPortContractTest {

  private FakeChallengeStatusAdapter adapter;
  private SimpleMeterRegistry meterRegistry;

  @Override
  protected ChallengeStatusPort createPort() {
    meterRegistry = new SimpleMeterRegistry();
    adapter = new FakeChallengeStatusAdapter(meterRegistry);
    return adapter;
  }

  @Override
  protected void configureStatus(UUID challengeId, ChallengeStatus status) {
    adapter.setStatus(challengeId, status);
  }

  @Test
  @DisplayName("Verifica que las consultas incrementen la métrica challenge.status.queries")
  void recordsMetricsWhenConsulted() {
    UUID challengeId = UUID.randomUUID();
    adapter.setStatus(challengeId, ChallengeStatus.ABIERTO);

    adapter.consultar(challengeId);
    adapter.consultar(challengeId);

    double count = meterRegistry.counter("challenge.status.queries", "status", "ABIERTO").count();
    assertThat(count).isEqualTo(2.0);
  }

  @Test
  @DisplayName("Verifica que setDefaultStatus configure el estado por defecto")
  void defaultStatusCanBeConfigured() {
    adapter.setDefaultStatus(ChallengeStatus.CERRADO);
    UUID randomId = UUID.randomUUID();
    assertThat(adapter.consultar(randomId)).isEqualTo(ChallengeStatus.CERRADO);
  }

  @Test
  @DisplayName("Verifica que clear restablezca los overrides y el estado por defecto")
  void clearRestoresInitialState() {
    UUID challengeId = UUID.randomUUID();
    adapter.setStatus(challengeId, ChallengeStatus.CERRADO);
    adapter.clear();

    assertThat(adapter.consultar(challengeId)).isEqualTo(ChallengeStatus.ABIERTO);
  }
}
