package ar.edu.utn.frc.tup.piv.llm.provider.spi;

import java.util.List;

public record ProviderDescriptor(String key, String displayName, String adapterVersion,
                                 List<CredentialField> credentialFields,
                                 ProviderCapabilities capabilities) {
  public ProviderDescriptor {
    credentialFields = List.copyOf(credentialFields);
  }
}
