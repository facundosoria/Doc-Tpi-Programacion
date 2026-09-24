package ar.edu.utn.frc.tup.piv.llm.domain.rag;

/** El texto de una página de PDF, ya limpiado de espacios/saltos superfluos. Portado de
 * `demoLLMSpringAi/.../PdfTextExtractorService.ExtractedPage`. */
public record ExtractedPage(int pageNumber, String text) {}
