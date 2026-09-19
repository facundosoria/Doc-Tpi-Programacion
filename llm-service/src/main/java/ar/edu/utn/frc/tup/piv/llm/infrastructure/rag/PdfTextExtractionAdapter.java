package ar.edu.utn.frc.tup.piv.llm.infrastructure.rag;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPage;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPdf;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.PdfTextExtractionPort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Adaptador Apache PDFBox de {@link PdfTextExtractionPort}, portado tal cual de
 * `demoLLMSpringAi/.../rag/service/PdfTextExtractorService.java`. */
@Component
public class PdfTextExtractionAdapter implements PdfTextExtractionPort {

  @Override
  public ExtractedPdf extractTextWithPages(byte[] pdfBytes) throws IOException {
    if (pdfBytes == null || pdfBytes.length == 0) {
      throw new IllegalArgumentException("El archivo PDF está vacío o no contiene bytes válidos.");
    }

    List<ExtractedPage> extractedPages = new ArrayList<>();
    StringBuilder fullTextBuilder = new StringBuilder();

    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      int totalPages = document.getNumberOfPages();
      if (totalPages == 0) {
        throw new IllegalArgumentException("El archivo PDF no contiene páginas.");
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
