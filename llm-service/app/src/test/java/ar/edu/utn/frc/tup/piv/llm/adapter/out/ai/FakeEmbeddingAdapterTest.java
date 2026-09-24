package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FakeEmbeddingAdapterTest {
  private final FakeEmbeddingAdapter adapter = new FakeEmbeddingAdapter();

  @Test
  void alwaysReturns768Dimensions() {
    var result = adapter.embed("¿cómo funciona un árbol binario de búsqueda?");

    assertThat(result.vector()).hasSize(768);
    assertThat(result.provider()).isEqualTo("fake");
    assertThat(result.model()).isEqualTo("fake-embedding-768");
  }

  @Test
  void theSameTextAlwaysProducesTheSameVector() {
    var first = adapter.embed("misma pregunta");
    var second = adapter.embed("misma pregunta");

    assertThat(first.vector()).containsExactly(second.vector());
  }

  @Test
  void differentTextsProduceDifferentVectors() {
    var first = adapter.embed("texto A");
    var second = adapter.embed("texto B, bien distinto");

    assertThat(first.vector()).isNotEqualTo(second.vector());
  }

  @Test
  void blankTextYieldsANullVectorInsteadOfFailing() {
    var result = adapter.embed("   ");

    assertThat(result.vector()).isNull();
  }

  @Test
  void embedBatchPreservesOrderAndSize() {
    var results = adapter.embedBatch(List.of("uno", "dos", "tres"));

    assertThat(results).hasSize(3);
    assertThat(results.get(0).vector()).isNotEqualTo(results.get(1).vector());
  }
}
