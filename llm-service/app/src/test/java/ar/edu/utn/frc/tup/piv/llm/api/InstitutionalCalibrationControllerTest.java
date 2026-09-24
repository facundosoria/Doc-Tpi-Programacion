package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.InstitutionalCalibrationController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
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
    when(auth.requireInstitutionalManager(any())).thenReturn(new CallerIdentity("svc", actor, "req", null));
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
  void configureSavesProfile() {
    var req = new InstitutionalCalibrationController.ProfileRequest(UUID.randomUUID(), UUID.randomUUID());
    controller.configure(req, headers);
    verify(runs).profile(req.goldenSetVersionId(), req.rubricVersionId(), actor);
  }

  @Test
  void listReturnsPlatformRuns() {
    var run = new CalibrationRunRepository.Run(UUID.randomUUID(), "PASSED", 0);
    when(runs.listPlatform()).thenReturn(List.of(run));
    assertThat(controller.list(headers).items()).containsExactly(run);
  }

  @Test
  void createSavesPlatformRun() {
    var p = new CalibrationRunRepository.Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(runs.profile()).thenReturn(Optional.of(p));
    var d = new ProviderCredentialRepository.Deployment(UUID.randomUUID(), UUID.randomUUID(), "openai", "gpt", "gpt-4", "ACTIVE", Instant.now(), 1, Instant.now(), java.util.Map.of());
    when(deployments.calibrationTarget()).thenReturn(Optional.of(d));
    var run = new CalibrationRunRepository.Run(UUID.randomUUID(), "QUEUED", 0);
    UUID key = UUID.randomUUID();
    when(runs.createPlatform(p.rubricVersionId(), p.goldenSetVersionId(), d.id(), key, actor)).thenReturn(run);
    
    var res = controller.create(headers, key);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(res.getBody()).isSameAs(run);
  }

  @Test
  void createFailsWithoutProfile() {
    when(runs.profile()).thenReturn(Optional.empty());
    assertThatThrownBy(() -> controller.create(headers, UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void createFailsWithoutTarget() {
    var p = new CalibrationRunRepository.Profile(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(runs.profile()).thenReturn(Optional.of(p));
    when(deployments.calibrationTarget()).thenReturn(Optional.empty());
    assertThatThrownBy(() -> controller.create(headers, UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void getReturnsRunDetail() {
    UUID runId = UUID.randomUUID();
    var run = new CalibrationRunRepository.Run(runId, null, "PLATFORM", "PASSED", 100, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null, null, null, null);
    when(runs.byId(runId)).thenReturn(Optional.of(run));
    var errors = java.util.Map.of(ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.AUTONOMY, java.math.BigDecimal.TEN);
    when(runs.dimensionErrors(runId)).thenReturn(errors);
    var detail = controller.get(runId, headers);
    assertThat(detail.run()).isSameAs(run);
    assertThat(detail.dimensionErrors()).isEqualTo(errors);
  }
}
