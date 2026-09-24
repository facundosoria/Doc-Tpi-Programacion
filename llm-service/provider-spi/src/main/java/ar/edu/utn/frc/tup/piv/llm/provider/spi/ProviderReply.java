package ar.edu.utn.frc.tup.piv.llm.provider.spi;

public record ProviderReply(String text, int inputTokens, int outputTokens,
                            String providerFingerprint) { }
