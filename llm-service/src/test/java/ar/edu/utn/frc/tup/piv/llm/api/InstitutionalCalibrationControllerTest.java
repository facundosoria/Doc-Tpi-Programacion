package ar.edu.utn.frc.tup.piv.llm.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway.Provider;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class InstitutionalCalibrationControllerTest {
  private final CalibrationRunRepository runs = mock(CalibrationRunRepository.class);
  private final ProviderCredentialRepository deployments = mock(ProviderCredentialRepository.class);
  private final GoldenSetAuthorization auth = mock(GoldenSetAuthorization.class);
  private final InstitutionalCalibrationController controller = new InstitutionalCalibrationController(runs, deployments, auth);
  private final HttpHeaders headers = new HttpHeaders();
  private final UUID actor = UUID.randomUUID();

  InstitutionalCalibrationControllerTest() {
    when(auth.require(any())).thenReturn(new CallerIdentity("svc", actor, "req", null));
  }

  @Test
  void profileReturnsConfiguredProfile() {
    var p = new CalibrationRunRepository.Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(runs.profile()).thenReturn(Optional.of(p));
    assertThat(controller.profile(headers)).isSameAs(p);
  }

  @Test
  void profileFailsWhenNotConfigured() {
    when(runs.profile()).thenReturn(Optional.empty());
    assertThatThrownBy(() -> controller.profile(headers)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("perfil institucional");
  }

  @Test
  void configureDelegatesWithActor() {
    UUID g = UUID.randomUUID(), r = UUID.randomUUID();
    controller.configure(new InstitutionalCalibrationController.ProfileRequest(g, r), headers);
    verify(runs).profile(g, r, actor);
  }

  @Test
  void listWrapsPlatformRuns() {
    var run = new CalibrationRunRepository.Run(UUID.randomUUID(), "QUEUED", 0);
    when(runs.listPlatform()).thenReturn(List.of(run));
    assertThat(controller.list(headers).items()).containsExactly(run);
  }

  @Test
  void createRequiresProfileAndTarget() {
    when(runs.profile()).thenReturn(Optional.empty());
    assertThatThrownBy(() -> controller.create(headers)).hasMessageContaining("Configure el perfil");
    when(runs.profile()).thenReturn(Optional.of(new CalibrationRunRepository.Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));
    when(deployments.calibrationTarget()).thenReturn(Optional.empty());
    assertThatThrownBy(() -> controller.create(headers)).hasMessageContaining("modelo candidato");
  }

  @Test
  void createReturnsAcceptedWithNewRun() {
    var profile = new CalibrationRunRepository.Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    var target = new ProviderCredentialRepository.Deployment(UUID.randomUUID(), UUID.randomUUID(), Provider.OPENAI_COMPATIBLE, "c", "m", "ACTIVE", Instant.now(), null, null);
    var run = new CalibrationRunRepository.Run(UUID.randomUUID(), "QUEUED", 0);
    when(runs.profile()).thenReturn(Optional.of(profile));
    when(deployments.calibrationTarget()).thenReturn(Optional.of(target));
    when(runs.createPlatform(profile.rubricVersionId(), profile.goldenSetVersionId(), target.id(), actor)).thenReturn(run);
    var response = controller.create(headers);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isSameAs(run);
  }
}
