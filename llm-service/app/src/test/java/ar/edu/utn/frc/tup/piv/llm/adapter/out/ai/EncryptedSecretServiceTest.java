package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptedSecretServiceTest {

  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void encryptsAndDecryptsRoundTrip() {
    var service = new EncryptedSecretService(KEY);

    var encrypted = service.encrypt("mi-secreto");

    assertThat(encrypted.value()).isNotEmpty();
    assertThat(encrypted.nonce()).hasSize(12);
    assertThat(service.decrypt(encrypted.value(), encrypted.nonce())).isEqualTo("mi-secreto");
  }

  @Test
  void encryptsUnicodeSecrets() {
    var service = new EncryptedSecretService(KEY);

    var encrypted = service.encrypt("clave ñoña con acentos áéíóú");

    assertThat(service.decrypt(encrypted.value(), encrypted.nonce())).isEqualTo("clave ñoña con acentos áéíóú");
  }

  @Test
  void requiresAMasterKey() {
    assertThatThrownBy(() -> new EncryptedSecretService(""))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("LLM_CREDENTIALS_MASTER_KEY es obligatoria");
    assertThatThrownBy(() -> new EncryptedSecretService("   "))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("LLM_CREDENTIALS_MASTER_KEY es obligatoria");
  }

  @Test
  void requiresA32ByteKey() {
    assertThatThrownBy(() -> new EncryptedSecretService(Base64.getEncoder().encodeToString(new byte[16])))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("base64 de 32 bytes");
  }

  @Test
  void decryptRejectsAnInvalidNonce() {
    var service = new EncryptedSecretService(KEY);

    assertThatThrownBy(() -> service.decrypt(new byte[] {1, 2, 3}, new byte[11]))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No se pudo descifrar");
  }

  @Test
  void decryptRejectsTamperedCiphertext() {
    var service = new EncryptedSecretService(KEY);
    var encrypted = service.encrypt("secreto");
    byte[] tampered = encrypted.value().clone();
    tampered[tampered.length - 1] ^= 0x01;

    assertThatThrownBy(() -> service.decrypt(tampered, encrypted.nonce()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No se pudo descifrar");
  }

  @Test
  void decryptHandlesByteArraysFromStorage() {
    var service = new EncryptedSecretService(KEY);
    var encrypted = service.encrypt("almacenado");

    byte[] value = encrypted.value().clone();
    byte[] nonce = encrypted.nonce().clone();

    assertThat(service.decrypt(value, nonce)).isEqualTo("almacenado");
    assertThat(new String(value, StandardCharsets.UTF_8)).isNotEqualTo("almacenado");
  }
}