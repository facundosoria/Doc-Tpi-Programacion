package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.util.List;

/** Resultado de extraer el texto de un PDF conservando la paginación (citas RAG precisas).
 * Portado de `demoLLMSpringAi/.../PdfTextExtractorService.ExtractedPdf`. */
public record ExtractedPdf(int totalPages, List<ExtractedPage> pages, String fullText) {}
