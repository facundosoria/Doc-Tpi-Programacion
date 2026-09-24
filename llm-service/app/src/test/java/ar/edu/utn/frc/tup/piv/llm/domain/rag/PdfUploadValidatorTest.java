package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** #669 — cada rechazo tiene su propio motivo: el docente tiene que saber qué corregir. */
class PdfUploadValidatorTest {
  private static final long MAX = 25L * 1024 * 1024;
  private final PdfUploadValidator validator = new PdfUploadValidator(MAX);

  private static byte[] pdfBytes(String body) {
    return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void aRealPdfIsAccepted() {
    assertThatCode(() -> validator.validateContent(pdfBytes("contenido"), "apunte.pdf")).doesNotThrowAnyException();
  }

  @Test
  void anEmptyFileIsRejectedSayingItIsEmpty() {
    assertThatThrownBy(() -> validator.validateContent(new byte[0], "vacio.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("vacio.pdf")
        .hasMessageContaining("vacío");
  }

  @Test
  void aNullContentIsRejectedAsEmpty() {
    assertThatThrownBy(() -> validator.validateContent(null, "nulo.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("vacío");
  }

  /** El caso que motiva validar por firma binaria: la extensión y el Content-Type los elige quien sube. */
  @Test
  void aDocxRenamedToPdfIsRejectedByItsSignature() {
    // Firma real de un .docx (ZIP): "PK\003\004".
    byte[] docx = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};

    assertThatThrownBy(() -> validator.validateContent(docx, "trabajo.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("no es un PDF")
        .hasMessageContaining("%PDF-");
  }

  @Test
  void aFileShorterThanTheSignatureIsRejected() {
    assertThatThrownBy(() -> validator.validateContent(new byte[] {'%', 'P'}, "cortito.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("no es un PDF");
  }

  @Test
  void aFileOverTheLimitIsRejectedSayingItsSize() {
    assertThatThrownBy(() -> validator.validateDeclaredSize(MAX + 1, "enorme.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("enorme.pdf")
        .hasMessageContaining("supera el máximo permitido")
        .hasMessageContaining("25");
  }

  @Test
  void aFileExactlyAtTheLimitIsAccepted() {
    assertThatCode(() -> validator.validateDeclaredSize(MAX, "justo.pdf")).doesNotThrowAnyException();
  }

  @Test
  void theDeclaredSizeIsCheckedBeforeReadingTheFile() {
    assertThatThrownBy(() -> validator.validateDeclaredSize(0, "vacio.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("vacío");
  }

  @Test
  void eachRejectionHasItsOwnMessage() {
    String vacio = messageOf(() -> validator.validateContent(new byte[0], "x.pdf"));
    String noPdf = messageOf(() -> validator.validateContent(new byte[] {0x50, 0x4B, 0x03, 0x04, 0, 0}, "x.pdf"));
    String grande = messageOf(() -> validator.validateDeclaredSize(MAX + 1, "x.pdf"));

    assertThat(List.of(vacio, noPdf, grande)).doesNotHaveDuplicates();
  }

  private String messageOf(Runnable rejection) {
    try {
      rejection.run();
      throw new AssertionError("se esperaba un rechazo");
    } catch (InvalidPdfSourceException exception) {
      return exception.getMessage();
    }
  }
}
