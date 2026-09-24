package ar.edu.utn.frc.tup.piv.llm.domain.rag;

/** Una imagen/diagrama de contenido genuino detectada dentro de un PDF (encabezados, pies y
 * barras decorativas repetitivas ya fueron filtrados por el adaptador). Portado de
 * `demoLLMSpringAi/.../rag/dto/ImageDetectionDto.java`. */
public record ImageDetection(int imageIndex, int pageNumber, int width, int height, String format, String base64Data, String titleHint) {}
