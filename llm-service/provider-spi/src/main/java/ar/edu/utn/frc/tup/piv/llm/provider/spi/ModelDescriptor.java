package ar.edu.utn.frc.tup.piv.llm.provider.spi;

import java.util.Map;

public record ModelDescriptor(String modelId, String displayName, String revision,
                              ProviderCapabilities capabilities, Map<String, Object> metadata) {
  public ModelDescriptor { metadata = Map.copyOf(metadata); }
}
