package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ModelAssignmentControllerCoverageTest {

  @Test
  void getThrowsWhenTheFunctionHasNoAssignedModel() {
    var configs = mock(FunctionModelConfigRepository.class);
    var authorization = mock(GoldenSetAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("admin-service", UUID.randomUUID(), null, null));
    when(configs.find(ModelFunction.EVALUATOR)).thenReturn(Optional.empty());
    var controller = new ModelAssignmentController(configs, authorization);

    assertThatThrownBy(() -> controller.get("evaluator", headers))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no tiene modelo asignado");
  }

  @Test
  void getIsCaseInsensitiveOnTheFunctionName() {
    var configs = mock(FunctionModelConfigRepository.class);
    var authorization = mock(GoldenSetAuthorization.class);
    var headers = new HttpHeaders();
    UUID deploymentId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("admin-service", UUID.randomUUID(), null, null));
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.of(new FunctionModelConfigRepository.Config(deploymentId, true)));
    var controller = new ModelAssignmentController(configs, authorization);

    assertThat(controller.get("TUTOR", headers).modelDeploymentId()).isEqualTo(deploymentId);
  }

  @Test
  void putRejectsAMissingModelDeploymentId() {
    var configs = mock(FunctionModelConfigRepository.class);
    var authorization = mock(GoldenSetAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("admin-service", UUID.randomUUID(), null, null));
    var controller = new ModelAssignmentController(configs, authorization);

    assertThatThrownBy(() -> controller.put("tutor", new ModelAssignmentController.Assignment(null), UUID.randomUUID(), headers))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("modelDeploymentId es obligatorio");
  }

  @Test
  void putRejectsAnUnknownFunction() {
    var configs = mock(FunctionModelConfigRepository.class);
    var authorization = mock(GoldenSetAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(new CallerIdentity("admin-service", UUID.randomUUID(), null, null));
    var controller = new ModelAssignmentController(configs, authorization);

    assertThatThrownBy(() -> controller.put("unknown", new ModelAssignmentController.Assignment(UUID.randomUUID()), UUID.randomUUID(), headers))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Función desconocida");
  }
}