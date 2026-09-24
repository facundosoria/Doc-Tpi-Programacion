package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.util.List;

/** Detección/decodificación determinística (sin IA) de imágenes y diagramas dentro de un PDF.
 * Aísla la heurística de bounding boxes + PDFBox de `application`, mismo criterio que
 * {@link PdfTextExtractionPort}. */
public interface DiagramDetectionPort {
  List<ImageDetection> detectImages(byte[] pdfBytes);

  DiagramDecodeResult decodeDiagram(byte[] pdfBytes, int imageIndex);
}
