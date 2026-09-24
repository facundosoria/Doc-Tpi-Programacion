package ar.edu.utn.frc.tup.piv.llm.provider.spi;

public record ProviderCapabilities(boolean modelDiscovery, boolean streaming, boolean structuredJson,
                                   boolean seed, boolean temperature, boolean topP, boolean topK,
                                   boolean providerFingerprint) { }
