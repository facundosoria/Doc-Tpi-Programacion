package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCredentialMaterial;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderInvocation;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Core gateway: decrypts only at the boundary and delegates to the selected Strategy. */
@Component
public class ProviderInvocationGateway {
  private final ProviderRegistry registry;
  private final EncryptedSecretService secrets;
  private final ObjectMapper json;

  public ProviderInvocationGateway(ProviderRegistry registry, EncryptedSecretService secrets, ObjectMapper json) {
    this.registry = registry; this.secrets = secrets; this.json = json;
  }

  public ProviderReply invoke(ProviderCredentialRepository.Credential credential, String modelId,
      String prompt, InferenceSettings settings, Duration timeout) {
    var adapter = registry.required(credential.providerKey());
    var material = material(credential);
    adapter.validate(material);
    return adapter.invoke(material, new ProviderInvocation(modelId, prompt, settings, timeout));
  }

  public ProviderReply stream(ProviderCredentialRepository.Credential credential, String modelId,
      String prompt, InferenceSettings settings, Duration timeout, Consumer<String> onDelta) {
    var adapter = registry.required(credential.providerKey());
    var material = material(credential);
    adapter.validate(material);
    return adapter.stream(material, new ProviderInvocation(modelId, prompt, settings, timeout), onDelta);
  }

  public ProviderCredentialMaterial material(ProviderCredentialRepository.Credential credential) {
    try {
      Map<String, String> decrypted = json.readValue(secrets.decrypt(credential.encryptedSecrets(), credential.nonce()),
          new TypeReference<Map<String, String>>() { });
      return new ProviderCredentialMaterial(credential.providerKey(), credential.configuration(), decrypted);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo preparar la credencial del proveedor", exception);
    }
  }
}
