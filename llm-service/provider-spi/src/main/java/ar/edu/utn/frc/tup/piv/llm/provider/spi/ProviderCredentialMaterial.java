package ar.edu.utn.frc.tup.piv.llm.provider.spi;

import java.util.Map;

/** Public configuration and decrypted secrets are short-lived invocation material. */
public record ProviderCredentialMaterial(String providerKey, Map<String, String> configuration,
                                         Map<String, String> secrets) {
  public ProviderCredentialMaterial {
    configuration = Map.copyOf(configuration);
    secrets = Map.copyOf(secrets);
  }
  public String configuration(String key) { return configuration.get(key); }
  public String secret(String key) { return secrets.get(key); }
}
