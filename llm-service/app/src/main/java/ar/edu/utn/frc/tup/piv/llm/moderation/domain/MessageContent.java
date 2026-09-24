package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Value Object inmutable que representa el texto a moderar y sus invariantes de longitud y contenido.
 */
public final class MessageContent {

    public static final int MAX_LENGTH = 4096;

    private final String text;
    private final String contentHash;

    public MessageContent(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El texto del mensaje no puede estar vacío ni ser nulo.");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El texto del mensaje no puede exceder los " + MAX_LENGTH + " caracteres.");
        }
        this.text = text;
        this.contentHash = computeSha256(text);
    }

    public String getText() {
        return text;
    }

    public String getContentHash() {
        return contentHash;
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la plataforma", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageContent that)) return false;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }

    @Override
    public String toString() {
        return "MessageContent[length=" + text.length() + ", hash=" + contentHash + "]";
    }
}
