package ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EncryptedSecretService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final SecretKeySpec key;

  public EncryptedSecretService(@Value("${llm.credentials.master-key:}") String encodedKey) {
    if (encodedKey.isBlank()) throw new IllegalStateException("LLM_CREDENTIALS_MASTER_KEY es obligatoria");
    byte[] raw = Base64.getDecoder().decode(encodedKey);
    if (raw.length != 32) throw new IllegalStateException("LLM_CREDENTIALS_MASTER_KEY debe ser base64 de 32 bytes");
    key = new SecretKeySpec(raw, "AES");
  }

  public EncryptedSecret encrypt(String secret) {
    try {
      byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      return new EncryptedSecret(cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8)), nonce);
    } catch (Exception exception) { throw new IllegalStateException("No se pudo cifrar la credencial", exception); }
  }

  public String decrypt(byte[] encrypted, byte[] nonce) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) { throw new IllegalStateException("No se pudo descifrar la credencial", exception); }
  }
  public record EncryptedSecret(byte[] value, byte[] nonce) {}
}
