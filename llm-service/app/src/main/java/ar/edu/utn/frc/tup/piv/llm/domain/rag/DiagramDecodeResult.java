package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.util.List;

/** Resultado de decodificar determinísticamente (sin IA) la estructura de una {@link ImageDetection}:
 * título, interpretación textual y su representación en Mermaid.js. Portado de
 * `demoLLMSpringAi/.../rag/dto/DiagramDecodedResultDto.java` — es la heurística de bounding
 * boxes + texto vectorial ya existente en la demo, **no** el pipeline de visión por computadora
 * (OpenCV/Tess4J) del spike `docs/31-spike-...md`; no presentarlo como "detección con IA". */
public record DiagramDecodeResult(
    int imageIndex,
    int pageNumber,
    String tituloDetectado,
    String tipoDiagrama,
    String interpretacion,
    String mermaidCode,
    List<String> elementosEncontrados) {

  public static final String TIPO_DESCONOCIDO = "DESCONOCIDO";

  public static DiagramDecodeResult vacio(int imageIndex) {
    return new DiagramDecodeResult(imageIndex, 1, "Figura no identificada", TIPO_DESCONOCIDO,
        "No se pudo extraer la interpretación de la figura solicitada.",
        "graph TD\n  A[\"Figura no identificada\"]", List.of());
  }
}
