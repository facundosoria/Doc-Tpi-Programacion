package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.io.IOException;

/** Extraer texto de un PDF es un detalle de librería (Apache PDFBox), no una regla de negocio —
 * se aísla detrás de un puerto para que `application` no dependa de PDFBox directamente, mismo
 * principio de puertos/adaptadores que ya usa el AI Gateway (`AGENTS.md` §4.B.5). */
public interface PdfTextExtractionPort {
  ExtractedPdf extractTextWithPages(byte[] pdfBytes) throws IOException;
}
