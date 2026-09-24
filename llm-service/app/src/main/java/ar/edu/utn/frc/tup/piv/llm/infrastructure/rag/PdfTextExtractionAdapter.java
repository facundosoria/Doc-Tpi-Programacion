package ar.edu.utn.frc.tup.piv.llm.infrastructure.rag;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPage;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPdf;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.InvalidPdfSourceException;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.PdfTextExtractionPort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Adaptador Apache PDFBox de {@link PdfTextExtractionPort}, portado tal cual de
 * `demoLLMSpringAi/.../rag/service/PdfTextExtractorService.java`. */
@Component
public class PdfTextExtractionAdapter implements PdfTextExtractionPort {

  @Override
  public ExtractedPdf extractTextWithPages(byte[] pdfBytes) throws IOException {
    if (pdfBytes == null || pdfBytes.length == 0) {
      throw new InvalidPdfSourceException("El archivo está vacío: no tiene contenido para indexar.");
    }

    List<ExtractedPage> extractedPages = new ArrayList<>();
    StringBuilder fullTextBuilder = new StringBuilder();

    try (PDDocument document = openOrReject(pdfBytes)) {
      // Un PDF con contraseña de usuario no lo abre PDFBox; uno con permisos restringidos sí, y
      // tampoco es material indexable: la cátedra no puede garantizar su lectura.
      if (document.isEncrypted()) {
        throw new InvalidPdfSourceException(
            "El archivo está protegido con contraseña. Subí una copia sin cifrar para poder indexarlo.");
      }
      int totalPages = document.getNumberOfPages();
      if (totalPages == 0) {
        throw new InvalidPdfSourceException("El archivo es un PDF sin páginas: no hay contenido para indexar.");
      }

      PDFTextStripper stripper = new PDFTextStripper();
      for (int page = 1; page <= totalPages; page++) {
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        String cleanedText = cleanText(stripper.getText(document));
        if (!cleanedText.isBlank()) {
          extractedPages.add(new ExtractedPage(page, cleanedText));
          fullTextBuilder.append(cleanedText).append("\n\n");
        }
      }
      return new ExtractedPdf(totalPages, extractedPages, fullTextBuilder.toString().trim());
    }
  }

  /** PDFBox distingue el PDF cifrado del simplemente ilegible; los dos se rechazan como fuente
   * inválida (#669), pero con mensajes distintos para que el docente sepa qué corregir. */
  private PDDocument openOrReject(byte[] pdfBytes) {
    try {
      return Loader.loadPDF(pdfBytes);
    } catch (InvalidPasswordException exception) {
      throw new InvalidPdfSourceException(
          "El archivo está protegido con contraseña. Subí una copia sin cifrar para poder indexarlo.");
    } catch (IOException exception) {
      throw new InvalidPdfSourceException(
          "El archivo no se pudo abrir como PDF: está dañado o no es realmente un PDF.");
    }
  }

  private String cleanText(String text) {
    if (text == null) return "";
    return text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replaceAll("[\\t\\x0B\\f]+", " ")
        .replaceAll(" +", " ")
        .replaceAll("\n{3,}", "\n\n")
        .trim();
  }
}
