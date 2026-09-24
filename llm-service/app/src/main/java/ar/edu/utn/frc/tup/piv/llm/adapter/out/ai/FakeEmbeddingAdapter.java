package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Mismo espíritu que {@link FakeModelAdapter} para el puerto de embeddings (EP-09): nunca llama
 * a un proveedor real. Determinístico (hash SHA-256 del texto, repetido hasta llenar 768
 * dimensiones y normalizado) para que dos textos iguales den siempre el mismo vector — útil para
 * probar la búsqueda por similitud sin depender de una API externa. El adaptador real (Google
 * `text-embedding-004` u otro, vía langchain4j) es trabajo futuro, igual que el proveedor real de
 * {@link FakeModelAdapter}. */
@Component
public class FakeEmbeddingAdapter implements EmbeddingPort {
  private static final String PROVIDER = "fake";
  private static final String MODEL = "fake-embedding-768";
  private static final int DIMENSIONS = 768;

  @Override
  public EmbeddingResult embed(String text) {
    return new EmbeddingResult(vectorize(text), PROVIDER, MODEL);
  }

  @Override
  public List<EmbeddingResult> embedBatch(List<String> texts) {
    if (texts == null) return List.of();
    return texts.stream().map(this::embed).collect(Collectors.toList());
  }

  private float[] vectorize(String text) {
    if (text == null || text.isBlank()) return null;
    byte[] seed = sha256(text);
    float[] vector = new float[DIMENSIONS];
    double sumSquares = 0;
    for (int i = 0; i < DIMENSIONS; i++) {
      int seedByte = seed[i % seed.length] & 0xFF;
      float value = ((seedByte / 255f) * 2f) - 1f;
      vector[i] = value;
      sumSquares += (double) value * value;
    }
    float norm = (float) Math.sqrt(sumSquares);
    if (norm > 0) {
      for (int i = 0; i < DIMENSIONS; i++) {
        vector[i] /= norm;
      }
    }
    return vector;
  }

  private byte[] sha256(String text) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String model() {
    return MODEL;
  }
}
