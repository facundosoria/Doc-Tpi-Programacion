package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Credential;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.AiProviderAdapter;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCredentialMaterial;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderInvocation;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ProviderInvocationGatewayTest {

  private static Credential credential() {
    return new Credential(UUID.randomUUID(), "openai", "API", Map.of("baseUrl", "x"),
        new byte[] {1}, new byte[] {2}, "mask", "ACTIVE", Instant.now());
  }

  @Test
  void invokeDecryptsValidatesAndDelegatesToTheAdapter() {
    var registry = mock(ProviderRegistry.class);
    var secrets = mock(EncryptedSecretService.class);
    var adapter = mock(AiProviderAdapter.class);
    Credential credential = credential();
    when(registry.required("openai")).thenReturn(adapter);
    when(secrets.decrypt(credential.encryptedSecrets(), credential.nonce())).thenReturn("{\"apiKey\":\"k\"}");
    when(adapter.invoke(any(), any())).thenReturn(new ProviderReply("hola", 1, 2, "fp"));
    var gateway = new ProviderInvocationGateway(registry, secrets, new ObjectMapper());

    var reply = gateway.invoke(credential, "gpt", "prompt",
        new InferenceSettings("v2", null, null, null, null, false, 512), Duration.ofSeconds(30));

    assertThat(reply.text()).isEqualTo("hola");
    verify(adapter).validate(any(ProviderCredentialMaterial.class));
    verify(adapter).invoke(any(ProviderCredentialMaterial.class),
        org.mockito.ArgumentMatchers.argThat(invocation -> "gpt".equals(((ProviderInvocation) invocation).modelId())));
  }

  @Test
  void streamForwardsTheDeltaConsumerToTheAdapter() {
    var registry = mock(ProviderRegistry.class);
    var secrets = mock(EncryptedSecretService.class);
    var adapter = mock(AiProviderAdapter.class);
    Credential credential = credential();
    when(registry.required("openai")).thenReturn(adapter);
    when(secrets.decrypt(credential.encryptedSecrets(), credential.nonce())).thenReturn("{\"apiKey\":\"k\"}");
    when(adapter.stream(any(), any(), any())).thenReturn(new ProviderReply("hola", 1, 2, "fp"));
    var gateway = new ProviderInvocationGateway(registry, secrets, new ObjectMapper());

    var reply = gateway.stream(credential, "gpt", "prompt",
        new InferenceSettings("v2", null, null, null, null, false, 512), Duration.ofSeconds(30), mock(Consumer.class));

    assertThat(reply.text()).isEqualTo("hola");
    verify(adapter).stream(any(ProviderCredentialMaterial.class), any(ProviderInvocation.class), any(Consumer.class));
  }

  @Test
  void materialBuildsDecryptedShortLivedCredentials() {
    var registry = mock(ProviderRegistry.class);
    var secrets = mock(EncryptedSecretService.class);
    Credential credential = credential();
    when(secrets.decrypt(credential.encryptedSecrets(), credential.nonce())).thenReturn("{\"apiKey\":\"k\"}");
    var gateway = new ProviderInvocationGateway(registry, secrets, new ObjectMapper());

    var material = gateway.material(credential);

    assertThat(material.providerKey()).isEqualTo("openai");
    assertThat(material.secret("apiKey")).isEqualTo("k");
    assertThat(material.configuration("baseUrl")).isEqualTo("x");
  }

  @Test
  void materialFailsWhenTheDecryptedSecretsAreNotValidJson() {
    var registry = mock(ProviderRegistry.class);
    var secrets = mock(EncryptedSecretService.class);
    Credential credential = credential();
    when(secrets.decrypt(credential.encryptedSecrets(), credential.nonce())).thenReturn("not-json");
    var gateway = new ProviderInvocationGateway(registry, secrets, new ObjectMapper());

    assertThatThrownBy(() -> gateway.material(credential))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No se pudo preparar la credencial");
  }

  @Test
  void materialFailsWhenDecryptionThrows() {
    var registry = mock(ProviderRegistry.class);
    var secrets = mock(EncryptedSecretService.class);
    Credential credential = credential();
    when(secrets.decrypt(credential.encryptedSecrets(), credential.nonce()))
        .thenThrow(new IllegalStateException("boom"));
    var gateway = new ProviderInvocationGateway(registry, secrets, new ObjectMapper());

    assertThatThrownBy(() -> gateway.material(credential))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No se pudo preparar la credencial");
  }
}