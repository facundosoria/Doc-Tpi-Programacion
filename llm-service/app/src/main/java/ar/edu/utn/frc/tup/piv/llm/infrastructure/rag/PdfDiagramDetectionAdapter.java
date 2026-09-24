package ar.edu.utn.frc.tup.piv.llm.infrastructure.rag;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDetectionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ImageDetection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Detección/decodificación 100% determinística (sin IA) de diagramas dentro de un PDF, portada
 * tal cual de `demoLLMSpringAi/.../rag/service/PdfDiagramPositionService.java`: heurística de
 * bounding boxes + texto vectorial de la página (PDFBox), **no** el pipeline de visión por
 * computadora (OpenCV/Tess4J) del spike `docs/31-spike-...md` — ver
 * `docs/estado-implementacion/ep-09/` para esta limitación documentada explícitamente. */
@Component
public class PdfDiagramDetectionAdapter implements DiagramDetectionPort {
  private static final Logger log = LoggerFactory.getLogger(PdfDiagramDetectionAdapter.class);
  private static final Pattern TITLE_HINT_PATTERN = Pattern.compile(
      "(?i)(Figura\\s*\\d+[:\\.]?[^\\n\\r]+|Imagen\\s*\\d+[:\\.]?[^\\n\\r]+|Diagrama\\s*\\d+[:\\.]?[^\\n\\r]+|Gráfico\\s*\\d+[:\\.]?[^\\n\\r]+|Esquema\\s*\\d+[:\\.]?[^\\n\\r]+)");
  private static final Pattern CAPTION_REFERENCE_PATTERN =
      Pattern.compile("(?i)(figura|diagrama|esquema|gráfico|imagen|muestra|observa|arquitectura|componente|estructura)");

  @Override
  public List<ImageDetection> detectImages(byte[] pdfBytes) {
    List<ImageDetection> detected = new ArrayList<>();
    if (pdfBytes == null || pdfBytes.length == 0) {
      return detected;
    }

    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      int totalPages = document.getNumberOfPages();

      // PASO 1: mapear ocurrencias de cada imagen a lo largo de todo el documento. Encabezados/
      // pies institucionales se repiten en múltiples páginas con idéntica firma.
      Map<String, Set<Integer>> signaturePages = new HashMap<>();
      for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
        PDResources resources = document.getPage(pageIdx).getResources();
        if (resources == null) continue;
        int pageNum = pageIdx + 1;
        for (COSName name : resources.getXObjectNames()) {
          PDXObject xobj = resources.getXObject(name);
          if (xobj instanceof PDImageXObject img) {
            signaturePages.computeIfAbsent(computeImageSignature(img), k -> new HashSet<>()).add(pageNum);
          }
        }
      }

      // PASO 2: extraer únicamente figuras legítimas de contenido único.
      int imageCounter = 0;
      Set<String> processedInResult = new HashSet<>();
      for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
        PDPage page = document.getPage(pageIdx);
        int pageNumber = pageIdx + 1;
        PDResources resources = page.getResources();
        if (resources == null) continue;

        for (COSName name : resources.getXObjectNames()) {
          PDXObject xobj = resources.getXObject(name);
          if (!(xobj instanceof PDImageXObject img)) continue;

          String sig = computeImageSignature(img);
          Set<Integer> pagesAppeared = signaturePages.get(sig);
          if (pagesAppeared != null && pagesAppeared.size() >= 2) continue; // encabezado/pie repetido

          if (img.getWidth() < 80 || img.getHeight() < 60) continue; // ícono/viñeta diminuta

          double ratioWtoH = (double) img.getWidth() / img.getHeight();
          double ratioHtoW = (double) img.getHeight() / img.getWidth();
          if (ratioWtoH > 3.5 || ratioHtoW > 3.5) continue; // barra decorativa

          String uniquePageKey = pageNumber + "_" + sig;
          if (!processedInResult.add(uniquePageKey)) continue;

          String base64 = convertToBase64(img.getImage());
          String titleHint = findTitleHintOnPage(document, pageNumber);
          detected.add(new ImageDetection(imageCounter++, pageNumber, img.getWidth(), img.getHeight(),
              img.getSuffix() != null ? img.getSuffix().toLowerCase() : "png", base64, titleHint));
        }
      }
    } catch (Exception exception) {
      log.warn("Error inspeccionando imágenes en el PDF: {}", exception.getMessage());
    }
    return detected;
  }

  @Override
  public DiagramDecodeResult decodeDiagram(byte[] pdfBytes, int targetImageIndex) {
    List<ImageDetection> images = detectImages(pdfBytes);
    if (images.isEmpty() || targetImageIndex < 0 || targetImageIndex >= images.size()) {
      return DiagramDecodeResult.vacio(targetImageIndex);
    }

    ImageDetection target = images.get(targetImageIndex);
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      String titleHint = target.titleHint() != null ? target.titleHint() : ("Figura Pág. " + target.pageNumber());
      return analyzePageContentDeterministically(document, target.pageNumber(), targetImageIndex, titleHint, target.width(), target.height());
    } catch (Exception exception) {
      log.error("Error al decodificar imagen de la página {}: {}", target.pageNumber(), exception.getMessage());
      return DiagramDecodeResult.vacio(targetImageIndex);
    }
  }

  private record SpatialTextItem(String text, float x, float y, float width, float height) {}

  private static class SpatialTextCollector extends PDFTextStripper {
    private final List<SpatialTextItem> items = new ArrayList<>();

    SpatialTextCollector() throws IOException {
      super();
      setSortByPosition(true);
    }

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
      if (string != null && !string.trim().isEmpty() && textPositions != null && !textPositions.isEmpty()) {
        float x = textPositions.get(0).getXDirAdj();
        float y = textPositions.get(0).getYDirAdj();
        float height = textPositions.get(0).getHeightDir();
        float width = 0;
        for (TextPosition tp : textPositions) width += tp.getWidthDirAdj();
        items.add(new SpatialTextItem(string.trim(), x, y, width, height));
      }
      super.writeString(string, textPositions);
    }

    List<SpatialTextItem> getItems() {
      return items;
    }
  }

  private DiagramDecodeResult analyzePageContentDeterministically(
      PDDocument document, int pageNumber, int imageIndex, String titleHint, int imageWidth, int imageHeight) throws IOException {
    SpatialTextCollector collector = new SpatialTextCollector();
    collector.setStartPage(pageNumber);
    collector.setEndPage(pageNumber);
    collector.getText(document);

    List<SpatialTextItem> rawItems = collector.getItems();
    if (rawItems.isEmpty()) {
      return DiagramDecodeResult.vacio(imageIndex);
    }

    List<String> contextualParagraphs = new ArrayList<>();
    Set<String> keyEntities = new LinkedHashSet<>();

    for (SpatialTextItem item : rawItems) {
      String txt = item.text();
      if (txt.matches("^\\d+$") || txt.length() < 3 || item.y() < 25 || item.y() > 800) continue;

      if (txt.length() > 60 || CAPTION_REFERENCE_PATTERN.matcher(txt).find()) {
        if (!contextualParagraphs.contains(txt)) contextualParagraphs.add(txt);
      } else if (txt.length() >= 3 && txt.length() <= 50 && !txt.equalsIgnoreCase(titleHint)) {
        keyEntities.add(txt);
      }
    }

    StringBuilder interpretacion = new StringBuilder();
    interpretacion.append("### ").append(titleHint).append("\n\n");
    interpretacion.append("**Ubicación en el documento:** Página ").append(pageNumber)
        .append(" (Resolución original: ").append(imageWidth).append(" × ").append(imageHeight).append(" px)\n\n");

    interpretacion.append("#### Contexto y Explicación en el Documento:\n");
    if (!contextualParagraphs.isEmpty()) {
      int count = 0;
      for (String p : contextualParagraphs) {
        if (count++ >= 4) break;
        interpretacion.append("> ").append(p).append("\n\n");
      }
    } else {
      interpretacion.append("Figura de estudio identificada en la página ").append(pageNumber)
          .append(". Los conceptos clave asociados se detallan a continuación.\n\n");
    }

    List<String> entities = new ArrayList<>(keyEntities);
    if (!entities.isEmpty()) {
      interpretacion.append("#### Elementos y Conceptos Identificados:\n");
      int entityCount = 0;
      for (String entity : entities) {
        if (entityCount++ >= 8) break;
        interpretacion.append("- **").append(entity).append("**\n");
      }
      interpretacion.append("\n");
    }

    String mermaidCode = generateDeterministicMermaid(titleHint, entities, contextualParagraphs);
    return new DiagramDecodeResult(imageIndex, pageNumber, titleHint, "DIAGRAMA_DOCUMENTO",
        interpretacion.toString().trim(), mermaidCode, entities.stream().limit(10).collect(Collectors.toList()));
  }

  private String generateDeterministicMermaid(String title, List<String> entities, List<String> paragraphs) {
    StringBuilder sb = new StringBuilder();
    sb.append("graph TD\n");
    sb.append("  MAIN[\"📐 ").append(escapeMermaid(title)).append("\"]\n");

    if (!entities.isEmpty()) {
      int limit = Math.min(entities.size(), 6);
      for (int i = 0; i < limit; i++) {
        sb.append(String.format("  NODE_%d[\"%s\"]%n", i, escapeMermaid(entities.get(i))));
        sb.append(String.format("  MAIN --> NODE_%d%n", i));
      }
    } else if (!paragraphs.isEmpty()) {
      sb.append("  DESC[\"").append(escapeMermaid(truncate(paragraphs.get(0), 70))).append("\"]\n");
      sb.append("  MAIN --> DESC\n");
    } else {
      sb.append("  DESC[\"Figura técnica del documento\"]\n");
      sb.append("  MAIN --> DESC\n");
    }
    return sb.toString().trim();
  }

  private String computeImageSignature(PDImageXObject img) {
    try {
      byte[] sample = new byte[64];
      try (var is = img.createInputStream()) {
        is.read(sample);
        return img.getWidth() + "x" + img.getHeight() + "_" + Arrays.hashCode(sample);
      }
    } catch (Exception exception) {
      return img.getWidth() + "x" + img.getHeight();
    }
  }

  private String findTitleHintOnPage(PDDocument document, int pageNumber) {
    try {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      stripper.setStartPage(pageNumber);
      stripper.setEndPage(pageNumber);
      Matcher matcher = TITLE_HINT_PATTERN.matcher(stripper.getText(document));
      if (matcher.find()) return matcher.group(1).trim();
    } catch (Exception ignored) {
      // sin epígrafe detectable: se usa el fallback "Figura Pág. N"
    }
    return "Figura Pág. " + pageNumber;
  }

  private String convertToBase64(BufferedImage image) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", baos);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception exception) {
      log.warn("Error convirtiendo imagen a Base64: {}", exception.getMessage());
      return "";
    }
  }

  private String escapeMermaid(String text) {
    if (text == null) return "";
    return text.replace("\"", "'").replace("[", "(").replace("]", ")")
        .replace("{", "(").replace("}", ")").replace("\n", " ").trim();
  }

  private String truncate(String text, int maxLen) {
    if (text == null) return "";
    return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
  }
}
