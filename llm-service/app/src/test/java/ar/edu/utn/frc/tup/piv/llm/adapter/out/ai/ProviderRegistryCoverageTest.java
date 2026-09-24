package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piv.llm.provider.spi.AiProviderAdapter;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCredentialMaterial;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderInvocation;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderRegistryCoverageTest {

  @Test
  void requiredRejectsUninstalledProviders() {
    var registry = new ProviderRegistry(List.of(new StubAdapter("openai")));

    assertThatThrownBy(() -> registry.required("anthropic"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no está instalado");
  }

  @Test
  void requiredRejectsInvalidKeys() {
    var registry = new ProviderRegistry(List.of(new StubAdapter("openai")));

    assertThatThrownBy(() -> registry.required(null))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("providerKey inválido");
    assertThatThrownBy(() -> registry.required("Bad Key"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("providerKey inválido");
  }

  @Test
  void constructionRejectsInvalidAdapterKeys() {
    assertThatThrownBy(() -> new ProviderRegistry(List.of(new StubAdapter("UPPER"))))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("providerKey inválido");
  }

  @Test
  void descriptorsAreSortedByKey() {
    var registry = new ProviderRegistry(List.of(new StubAdapter("zeta"), new StubAdapter("alpha")));

    assertThat(registry.descriptors()).extracting(ProviderDescriptor::key)
        .containsExactly("alpha", "zeta");
  }

  private static final class StubAdapter implements AiProviderAdapter {
    private final ProviderDescriptor descriptor;

    private StubAdapter(String key) {
      descriptor = new ProviderDescriptor(key, key, "test", List.of(),
          new ProviderCapabilities(false, false, false, false, false, false, false, false));
    }

    @Override public ProviderDescriptor descriptor() { return descriptor; }
    @Override public void validate(ProviderCredentialMaterial credential) { }
    @Override public List<ModelDescriptor> discoverModels(ProviderCredentialMaterial credential) { return List.of(); }
    @Override public ProviderReply invoke(ProviderCredentialMaterial credential, ProviderInvocation invocation) { return new ProviderReply("", 0, 0, null); }
  }
}