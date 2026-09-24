package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository.Config;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Credential;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Deployment;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LangChain4jModelAdapterTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final ModelInvocationRequest REQUEST =
      new ModelInvocationRequest(ModelFunction.TUTOR, "system", "user", TIMEOUT);

  private static Deployment deployment(UUID deploymentId, UUID credentialId, String state) {
    return new Deployment(deploymentId, credentialId, "openai", "API", "gpt-4o-mini", state,
        Instant.now(), null, null, Map.of());
  }

  @Test
  void invokeResolvesTheConfiguredDeploymentAndCredential() {
    var deployments = mock(ProviderCredentialRepository.class);
    var configs = mock(FunctionModelConfigRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    UUID deploymentId = UUID.randomUUID();
    UUID credentialId = UUID.randomUUID();
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.of(new Config(deploymentId, true)));
    when(deployments.forId(deploymentId)).thenReturn(Optional.of(deployment(deploymentId, credentialId, "ACTIVE")));
    when(deployments.get(credentialId)).thenReturn(Optional.of(new Credential(credentialId, "openai", "API",
        Map.of(), new byte[] {}, new byte[] {}, "mask", "ACTIVE", Instant.now())));
    when(gateway.invoke(any(), eq("gpt-4o-mini"), any(), any(), eq(TIMEOUT)))
        .thenReturn(new ProviderReply("respuesta", 5, 7, "fp"));
    var adapter = new LangChain4jModelAdapter(deployments, configs, gateway);

    ModelInvocationResult result = adapter.invoke(REQUEST);

    assertThat(result.text()).isEqualTo("respuesta");
    assertThat(result.provider()).isEqualTo("openai");
    assertThat(result.model()).isEqualTo("gpt-4o-mini");
    org.mockito.Mockito.verify(gateway).invoke(any(), eq("gpt-4o-mini"),
        argThat(prompt -> "system\n\nuser".equals(prompt)), any(), eq(TIMEOUT));
  }

  @Test
  void invokeRequiresAnEnabledFunctionConfig() {
    var deployments = mock(ProviderCredentialRepository.class);
    var configs = mock(FunctionModelConfigRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.empty());
    var adapter = new LangChain4jModelAdapter(deployments, configs, gateway);

    assertThatThrownBy(() -> adapter.invoke(REQUEST))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("modelo habilitado");
  }

  @Test
  void invokeIgnoresDisabledFunctionConfigs() {
    var deployments = mock(ProviderCredentialRepository.class);
    var configs = mock(FunctionModelConfigRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.of(new Config(UUID.randomUUID(), false)));
    var adapter = new LangChain4jModelAdapter(deployments, configs, gateway);

    assertThatThrownBy(() -> adapter.invoke(REQUEST))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("modelo habilitado");
  }

  @Test
  void invokeRequiresTheDeploymentToBeAvailable() {
    var deployments = mock(ProviderCredentialRepository.class);
    var configs = mock(FunctionModelConfigRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    UUID deploymentId = UUID.randomUUID();
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.of(new Config(deploymentId, true)));
    when(deployments.forId(deploymentId)).thenReturn(Optional.empty());
    var adapter = new LangChain4jModelAdapter(deployments, configs, gateway);

    assertThatThrownBy(() -> adapter.invoke(REQUEST))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("deployment asignado");
  }

  @Test
  void invokeRequiresAnActiveCredentialForTheDeployment() {
    var deployments = mock(ProviderCredentialRepository.class);
    var configs = mock(FunctionModelConfigRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    UUID deploymentId = UUID.randomUUID();
    UUID credentialId = UUID.randomUUID();
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.of(new Config(deploymentId, true)));
    when(deployments.forId(deploymentId)).thenReturn(Optional.of(deployment(deploymentId, credentialId, "ACTIVE")));
    when(deployments.get(credentialId)).thenReturn(Optional.of(new Credential(credentialId, "openai", "API",
        Map.of(), new byte[] {}, new byte[] {}, "mask", "DISABLED", Instant.now())));
    var adapter = new LangChain4jModelAdapter(deployments, configs, gateway);

    assertThatThrownBy(() -> adapter.invoke(REQUEST))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no está activa");
  }

  @Test
  void exposesTheDeploymentSelectedIdentity() {
    var adapter = new LangChain4jModelAdapter(mock(ProviderCredentialRepository.class),
        mock(FunctionModelConfigRepository.class), mock(ProviderInvocationGateway.class));

    assertThat(adapter.provider()).isEqualTo("deployment-selected");
    assertThat(adapter.model()).isEqualTo("deployment-selected");
  }
}