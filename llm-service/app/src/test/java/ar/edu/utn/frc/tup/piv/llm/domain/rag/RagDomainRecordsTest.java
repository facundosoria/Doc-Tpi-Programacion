package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagDomainRecordsTest {

  @Test
  void diagramDecodeResultVacioProducesDefaultValues() {
    var vacio = DiagramDecodeResult.vacio(3);
    assertThat(vacio.imageIndex()).isEqualTo(3);
    assertThat(vacio.pageNumber()).isEqualTo(1);
    assertThat(vacio.tipoDiagrama()).isEqualTo(DiagramDecodeResult.TIPO_DESCONOCIDO);
    assertThat(vacio.tituloDetectado()).contains("no identificada");
    assertThat(vacio.mermaidCode()).contains("graph TD");
    assertThat(vacio.elementosEncontrados()).isEmpty();
  }

  @Test
  void ragDocumentRecordHoldsAttributesCorrectly() {
    UUID docId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    var doc = new RagDocument(docId, courseId, "manual.pdf", 1024L, 5, 10, now, "preview...", true);

    assertThat(doc.id()).isEqualTo(docId);
    assertThat(doc.courseCohortId()).isEqualTo(courseId);
    assertThat(doc.fileName()).isEqualTo("manual.pdf");
    assertThat(doc.fileSizeBytes()).isEqualTo(1024L);
    assertThat(doc.pageCount()).isEqualTo(5);
    assertThat(doc.chunkCount()).isEqualTo(10);
    assertThat(doc.uploadedAt()).isEqualTo(now);
    assertThat(doc.previewText()).isEqualTo("preview...");
    assertThat(doc.active()).isTrue();
  }

  @Test
  void retireMarksTheDocumentInactiveKeepingEverythingElse() {
    var doc = new RagDocument(UUID.randomUUID(), UUID.randomUUID(), "manual.pdf", 1024L, 5, 10, OffsetDateTime.now(), "preview...", true);

    RagDocument retired = doc.retire();

    assertThat(retired.active()).isFalse();
    assertThat(retired.id()).isEqualTo(doc.id());
    assertThat(retired.courseCohortId()).isEqualTo(doc.courseCohortId());
    assertThat(retired.fileName()).isEqualTo(doc.fileName());
    assertThat(retired.chunkCount()).isEqualTo(doc.chunkCount());
    assertThat(retired.uploadedAt()).isEqualTo(doc.uploadedAt());
    assertThat(doc.active()).as("el record original es inmutable").isTrue();
  }

  @Test
  void retireIsIdempotent() {
    var doc = new RagDocument(UUID.randomUUID(), UUID.randomUUID(), "manual.pdf", 1024L, 5, 10, OffsetDateTime.now(), "preview...", true);

    RagDocument once = doc.retire();
    RagDocument twice = once.retire();

    assertThat(twice).isEqualTo(once);
    assertThat(twice.active()).isFalse();
  }

  @Test
  void belongsToOnlyMatchesItsOwnCohort() {
    UUID cohort = UUID.randomUUID();
    var doc = new RagDocument(UUID.randomUUID(), cohort, "manual.pdf", 1024L, 5, 10, OffsetDateTime.now(), "preview...", true);

    assertThat(doc.belongsTo(cohort)).isTrue();
    assertThat(doc.belongsTo(UUID.randomUUID())).isFalse();
    assertThat(doc.belongsTo(null)).isFalse();
  }

  @Test
  void extractedPdfAndChunkHoldValues() {
    var page = new ExtractedPage(1, "Texto extraído");
    var pdf = new ExtractedPdf(1, List.of(page), "Texto extraído");
    assertThat(pdf.totalPages()).isEqualTo(1);
    assertThat(pdf.pages()).hasSize(1);
    assertThat(pdf.fullText()).isEqualTo("Texto extraído");

    UUID chunkDocId = UUID.randomUUID();
    var chunk = DocumentChunk.nuevo(chunkDocId, "doc.pdf", 1, 0, "Contenido chunk");
    assertThat(chunk.documentId()).isEqualTo(chunkDocId);
    assertThat(chunk.documentName()).isEqualTo("doc.pdf");
    assertThat(chunk.chunkIndex()).isZero();
    assertThat(chunk.pageNumber()).isEqualTo(1);
    assertThat(chunk.content()).isEqualTo("Contenido chunk");
    assertThat(chunk.similarityScore()).isZero();

    var scoredChunk = chunk.conSimilitud(0.92);
    assertThat(scoredChunk.similarityScore()).isEqualTo(0.92);

    var img = new ImageDetection(1, 2, 800, 600, "PNG", "base64...", "Hint");
    assertThat(img.imageIndex()).isEqualTo(1);
    assertThat(img.pageNumber()).isEqualTo(2);
    assertThat(img.width()).isEqualTo(800);
    assertThat(img.height()).isEqualTo(600);
    assertThat(img.format()).isEqualTo("PNG");
    assertThat(img.titleHint()).isEqualTo("Hint");
  }
}
