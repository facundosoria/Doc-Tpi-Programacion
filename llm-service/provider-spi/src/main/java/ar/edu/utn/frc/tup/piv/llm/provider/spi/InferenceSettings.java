package ar.edu.utn.frc.tup.piv.llm.provider.spi;

public record InferenceSettings(String policyVersion, Double temperature, Double topP, Integer topK,
                                Long seed, boolean structuredJson, int maxOutputTokens) { }
