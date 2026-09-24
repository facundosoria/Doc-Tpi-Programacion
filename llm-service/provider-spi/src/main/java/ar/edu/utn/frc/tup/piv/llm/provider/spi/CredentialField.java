package ar.edu.utn.frc.tup.piv.llm.provider.spi;

public record CredentialField(String key, String label, boolean secret, boolean required,
                              String hint) { }
