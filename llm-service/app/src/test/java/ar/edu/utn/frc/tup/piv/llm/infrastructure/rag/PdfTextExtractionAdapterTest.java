package ar.edu.utn.frc.tup.piv.llm.infrastructure.rag;

import ar.edu.utn.frc.tup.piv.llm.application.service.RagIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.InvalidPdfSourceException;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfTextExtractionAdapterTest {
  private final PdfTextExtractionAdapter adapter = new PdfTextExtractionAdapter();

  @Test
  void extractsTheTextOfEachPageWithItsPageNumber() throws Exception {
    byte[] pdf = pdfWithText("Docker comparte el kernel del sistema anfitrión.");

    var extracted = adapter.extractTextWithPages(pdf);

    assertThat(extracted.totalPages()).isEqualTo(1);
    assertThat(extracted.pages()).hasSize(1);
    assertThat(extracted.pages().get(0).pageNumber()).isEqualTo(1);
    assertThat(extracted.pages().get(0).text()).contains("Docker comparte el kernel");
    assertThat(extracted.fullText()).contains("Docker comparte el kernel");
  }

  @Test
  void rejectsNullOrEmptyBytes() {
    // #669: pasó de IllegalArgumentException a un rechazo de fuente con motivo propio; ambos
    // terminan en 422, pero ahora el mensaje dice cuál de los cinco casos se rechazó.
    assertThatThrownBy(() -> adapter.extractTextWithPages(null))
        .isInstanceOf(InvalidPdfSourceException.class).hasMessageContaining("vacío");
    assertThatThrownBy(() -> adapter.extractTextWithPages(new byte[0]))
        .isInstanceOf(InvalidPdfSourceException.class).hasMessageContaining("vacío");
  }

  /** #669 — `Loader.loadPDF` lanza `InvalidPasswordException` para un PDF con contraseña real
   * ANTES de que el adaptador pueda chequear `document.isEncrypted()`. Antes esa IOException
   * escapaba sin mapear (500); ahora se traduce a un rechazo con el motivo exacto. */
  @Test
  void aPasswordProtectedPdfIsRejectedAsEncrypted() throws Exception {
    byte[] pdf = encryptedPdf();

    assertThatThrownBy(() -> adapter.extractTextWithPages(pdf))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("contraseña");
  }

  /** #669 — el caso que documentaba `TutorRagIT` como pendiente: un archivo que no es PDF hacía
   * escapar la IOException de PDFBox como 500. */
  @Test
  void aFileThatIsNotAPdfIsRejectedAsUnreadable() {
    byte[] docx = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00, 0x08, 0x00};

    assertThatThrownBy(() -> adapter.extractTextWithPages(docx))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("no se pudo abrir como PDF");
  }

  @Test
  void aTruncatedPdfIsRejectedAsUnreadable() throws Exception {
    byte[] valido = pdfWithText("contenido");
    byte[] truncado = java.util.Arrays.copyOf(valido, valido.length / 2);

    assertThatThrownBy(() -> adapter.extractTextWithPages(truncado))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("dañado");
  }

  private byte[] pdfWithText(String text) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        stream.newLineAtOffset(50, 700);
        stream.showText(text);
        stream.endText();
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      return out.toByteArray();
    }
  }

  private byte[] encryptedPdf() throws IOException {
    try (PDDocument document = new PDDocument()) {
      document.addPage(new PDPage());
      AccessPermission permission = new AccessPermission();
      StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-secret", "user-secret", permission);
      document.protect(policy);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      return out.toByteArray();
    }
  }
}
