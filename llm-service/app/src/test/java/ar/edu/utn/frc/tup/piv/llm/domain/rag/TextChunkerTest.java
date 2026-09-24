package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TextChunkerTest {
  private final TextChunker chunker = new TextChunker();
  private final UUID documentId = UUID.randomUUID();

  private static final String PAGE_TEXT_OVER_MIN_SIZE =
      "Texto de una sola página, pero con la longitud suficiente (más de 100 caracteres) para "
          + "superar el tamaño mínimo de chunk exigido por TextChunker.";

  @Test
  void aShortPageBecomesASingleChunk() {
    var chunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(1, PAGE_TEXT_OVER_MIN_SIZE)));

    assertThat(chunks).hasSize(1);
    assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
    assertThat(chunks.get(0).documentId()).isEqualTo(documentId);
    assertThat(chunks.get(0).chunkIndex()).isZero();
  }

  @Test
  void aLongPageIsSplitWithOverlapAndKeepsPageNumbering() {
    String longText = "Oración número uno. ".repeat(120); // bien por encima de los 1000 caracteres
    var chunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(3, longText)));

    assertThat(chunks.size()).isGreaterThan(1);
    assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.pageNumber()).isEqualTo(3));
    // chunkIndex es global y creciente
    for (int i = 0; i < chunks.size(); i++) {
      assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
    }
  }

  @Test
  void blankPagesAreSkipped() {
    var chunks = chunker.createChunks(documentId, "doc.pdf", List.of(
        new ExtractedPage(1, "   "), new ExtractedPage(2, PAGE_TEXT_OVER_MIN_SIZE)));

    assertThat(chunks).hasSize(1);
    assertThat(chunks.get(0).pageNumber()).isEqualTo(2);
  }

  @Test
  void fragmentsShorterThanTheMinimumSizeAreDropped() {
    var chunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(1, "muy corto")));

    assertThat(chunks).isEmpty();
  }

  @Test
  void splitsAtParagraphAndQuestionBoundaries() {
    String textWithParagraphs = "Párrafo inicial con información relevante sobre el tema estudiado. ".repeat(15)
        + "\n\nSegundo párrafo que inicia aquí con más detalles necesarios para la prueba. ".repeat(15);
    var chunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(1, textWithParagraphs)));
    assertThat(chunks.size()).isGreaterThan(1);

    String textWithQuestions = "¿Cuál es la respuesta correcta a este problema planteado aquí? ".repeat(25);
    var questionChunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(2, textWithQuestions)));
    assertThat(questionChunks.size()).isGreaterThan(1);

    String textWithExclamations = "¡Atención a este paso fundamental en la arquitectura del sistema! ".repeat(25);
    var exclChunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(3, textWithExclamations)));
    assertThat(exclChunks.size()).isGreaterThan(1);

    String textWithLinebreaks = "Línea sin punto pero con salto de línea continuo para probar\n".repeat(30);
    var lbChunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(4, textWithLinebreaks)));
    assertThat(lbChunks.size()).isGreaterThan(1);
  }

  @Test
  void consecutivePiecesDoNotExceedTheChunkSizeOrOverlap() {
    String longText = "Oración número uno con contenido suficiente para el fragmento. ".repeat(60);
    List<DocumentChunk> chunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(1, longText)));

    assertThat(chunks.size()).isGreaterThan(1);
    for (DocumentChunk chunk : chunks) {
      assertThat(chunk.content().length()).isLessThanOrEqualTo(1000);
    }
    for (int i = 1; i < chunks.size(); i++) {
      int overlap = overlapBetween(chunks.get(i - 1).content(), chunks.get(i).content());
      assertThat(overlap).isLessThanOrEqualTo(200);
    }
  }

  @Test
  void overlappingPiecesKeepContinuousContext() {
    String longText = "Una oración con contexto que se repite hasta superar el tamaño del fragmento. ".repeat(50);
    List<DocumentChunk> chunks = chunker.createChunks(documentId, "doc.pdf", List.of(new ExtractedPage(1, longText)));

    assertThat(chunks.size()).isGreaterThan(1);
    for (int i = 1; i < chunks.size(); i++) {
      int overlap = overlapBetween(chunks.get(i - 1).content(), chunks.get(i).content());
      assertThat(overlap).isGreaterThan(0);
    }
  }

  private int overlapBetween(String previous, String current) {
    int maxOverlap = Math.min(Math.min(previous.length(), current.length()), 200);
    int overlap = 0;
    for (int size = 1; size <= maxOverlap; size++) {
      if (previous.endsWith(current.substring(0, size))) {
        overlap = size;
      }
    }
    return overlap;
  }
}
