package ar.edu.utn.frc.tup.piv.llm.provider.spi;

import java.time.Duration;

public record ProviderInvocation(String modelId, String prompt, InferenceSettings settings,
                                 Duration timeout) { }
