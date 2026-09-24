package ar.edu.utn.frc.tup.piv.llm.provider.spi;

public class ProviderException extends RuntimeException {
  private final String code;
  public ProviderException(String code, String message) { super(message); this.code = code; }
  public ProviderException(String code, String message, Throwable cause) { super(message, cause); this.code = code; }
  public String code() { return code; }
}
