package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test de contrato abstracto para ChallengeStatusPort (H03-T1).
 * Define las invariantes que cualquier adaptador de ChallengeStatusPort (Fake, REST o Kafka)
 * debe cumplir para preservar la semántica del dominio.
 */
public abstract class ChallengeStatusPortContractTest {

  protected abstract ChallengeStatusPort createPort();

  protected abstract void configureStatus(UUID challengeId, ChallengeStatus status);

  private ChallengeStatusPort port;

  @BeforeEach
  void setUpContract() {
    this.port = createPort();
  }

  @Test
  @DisplayName("Contrato: si challengeId es nulo, debe retornar NO_EXISTE de forma segura")
  void nullChallengeIdReturnsNoExiste() {
    ChallengeStatus status = port.consultar(null);
    assertThat(status).isEqualTo(ChallengeStatus.NO_EXISTE);
  }

  @Test
  @DisplayName("Contrato: desafío activo retorna ABIERTO")
  void openChallengeReturnsAbierto() {
    UUID challengeId = UUID.randomUUID();
    configureStatus(challengeId, ChallengeStatus.ABIERTO);

    ChallengeStatus status = port.consultar(challengeId);
    assertThat(status).isEqualTo(ChallengeStatus.ABIERTO);
  }

  @Test
  @DisplayName("Contrato: desafío finalizado retorna CERRADO")
  void closedChallengeReturnsCerrado() {
    UUID challengeId = UUID.randomUUID();
    configureStatus(challengeId, ChallengeStatus.CERRADO);

    ChallengeStatus status = port.consultar(challengeId);
    assertThat(status).isEqualTo(ChallengeStatus.CERRADO);
  }

  @Test
  @DisplayName("Contrato: desafío no existente retorna NO_EXISTE")
  void nonExistentChallengeReturnsNoExiste() {
    UUID challengeId = UUID.randomUUID();
    configureStatus(challengeId, ChallengeStatus.NO_EXISTE);

    ChallengeStatus status = port.consultar(challengeId);
    assertThat(status).isEqualTo(ChallengeStatus.NO_EXISTE);
  }

  @Test
  @DisplayName("Contrato: consultar un desafío no configurado explícitamente retorna un estado válido del enum")
  void unconfiguredChallengeReturnsValidStatus() {
    UUID challengeId = UUID.randomUUID();
    ChallengeStatus status = port.consultar(challengeId);
    assertThat(status).isNotNull().isIn(ChallengeStatus.ABIERTO, ChallengeStatus.CERRADO, ChallengeStatus.NO_EXISTE);
  }
}
