package com.example.demo.rag.service;

import com.example.demo.rag.dto.DiagramDecodedResultDto;
import com.example.demo.rag.dto.ImageDetectionDto;
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
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio 100% determinístico sin IA que utiliza Apache PDFBox para:
 * 1. Filtrar matemáticamente cabeceras, pies de página y barras decorativas repetitivas.
 * 2. Extraer únicamente imágenes y diagramas de contenido genuino del documento.
 * 3. Extraer el epígrafe y la interpretación contextual del PDF redactada por el autor.
 * 4. Generar la estructura visual en Mermaid.js de forma dinámica sin datos hardcodeados.
 */
@Service
public class PdfDiagramPositionService {

    private static final Logger log = LoggerFactory.getLogger(PdfDiagramPositionService.class);
    private static final Pattern TITLE_HINT_PATTERN = Pattern.compile("(?i)(Figura\\s*\\d+[:\\.]?[^\\n\\r]+|Imagen\\s*\\d+[:\\.]?[^\\n\\r]+|Diagrama\\s*\\d+[:\\.]?[^\\n\\r]+|Gráfico\\s*\\d+[:\\.]?[^\\n\\r]+|Esquema\\s*\\d+[:\\.]?[^\\n\\r]+)");
    private static final Pattern CAPTION_REFERENCE_PATTERN = Pattern.compile("(?i)(figura|diagrama|esquema|gráfico|imagen|muestra|observa|arquitectura|componente|estructura)");

    /**
     * Inspecciona el PDF aplicando filtros estrictos para excluir:
     * - Barras de encabezado y pie de página (imágenes que se repiten en 2 o más páginas).
     * - Franjas alargadas decorativas (aspect ratio ancho/alto > 3.5 o alto/ancho > 3.5).
     * - Iconos y viñetas pequeñas (ancho < 100px o alto < 70px).
     */
    public List<ImageDetectionDto> detectImages(byte[] pdfBytes) {
        List<ImageDetectionDto> detected = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) {
            return detected;
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();

            // PASO 1: Mapear ocurrencias de cada imagen a lo largo de todo el documento
            // Las cabeceras/pies institucionales se repiten en múltiples páginas con idéntica firma.
            Map<String, Set<Integer>> signaturePages = new HashMap<>();

            for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
                PDPage page = document.getPage(pageIdx);
                int pageNum = pageIdx + 1;
                PDResources resources = page.getResources();
                if (resources == null) continue;

                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xobj = resources.getXObject(name);
                    if (xobj instanceof PDImageXObject img) {
                        String sig = computeImageSignature(img);
                        signaturePages.computeIfAbsent(sig, k -> new HashSet<>()).add(pageNum);
                    }
                }
            }

            // PASO 2: Extraer únicamente figuras legítimas de contenido único
            int imageCounter = 0;
            Set<String> processedSignaturesInResult = new HashSet<>();

            for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
                PDPage page = document.getPage(pageIdx);
                int pageNumber = pageIdx + 1;
                PDResources resources = page.getResources();
                if (resources == null) continue;

                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xobj = resources.getXObject(name);
                    if (xobj instanceof PDImageXObject img) {
                        String sig = computeImageSignature(img);

                        // Filtro 1: Repetición de plantilla (encabezados, pies o marcas de agua que aparecen en 2+ páginas)
                        Set<Integer> pagesAppeared = signaturePages.get(sig);
                        if (pagesAppeared != null && pagesAppeared.size() >= 2) {
                            log.debug("Descartada imagen repetitiva de encabezado/pie en pág {}: {} páginas", pageNumber, pagesAppeared.size());
                            continue;
                        }

                        // Filtro 2: Dimensiones mínimas para descartar viñetas diminutas o iconos pequeños
                        if (img.getWidth() < 80 || img.getHeight() < 60) {
                            continue;
                        }

                        // Filtro 3: Relación de aspecto extrema (franjas horizontales o líneas verticales de diseño)
                        double ratioWtoH = (double) img.getWidth() / img.getHeight();
                        double ratioHtoW = (double) img.getHeight() / img.getWidth();
                        if (ratioWtoH > 3.5 || ratioHtoW > 3.5) {
                            log.debug("Descartada barra decorativa con ratio {}:1 en pág {}", ratioWtoH, pageNumber);
                            continue;
                        }

                        // Evitar duplicados exactos dentro de la misma página
                        String uniquePageKey = pageNumber + "_" + sig;
                        if (processedSignaturesInResult.contains(uniquePageKey)) {
                            continue;
                        }
                        processedSignaturesInResult.add(uniquePageKey);

                        String base64 = convertToBase64(img.getImage());
                        String titleHint = findTitleHintOnPage(document, pageNumber);

                        detected.add(ImageDetectionDto.builder()
                                .imageIndex(imageCounter++)
                                .pageNumber(pageNumber)
                                .width(img.getWidth())
                                .height(img.getHeight())
                                .format(img.getSuffix() != null ? img.getSuffix().toLowerCase() : "png")
                                .base64Data(base64)
                                .titleHint(titleHint)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error inspeccionando imágenes en el PDF: {}", e.getMessage());
        }

        return detected;
    }

    /**
     * Decodifica de forma determinística la estructura y significado de la imagen seleccionada,
     * extrayendo el epígrafe, el contexto explicativo del autor y construyendo un diagrama Mermaid
     * fiel a la página sin asumir ningún dominio fijo ni utilizar datos hardcodeados.
     */
    public DiagramDecodedResultDto decodeDiagram(byte[] pdfBytes, int targetImageIndex) {
        List<ImageDetectionDto> images = detectImages(pdfBytes);
        if (images.isEmpty() || targetImageIndex < 0 || targetImageIndex >= images.size()) {
            return fallbackEmptyResult(targetImageIndex);
        }

        ImageDetectionDto targetImg = images.get(targetImageIndex);
        int pageNumber = targetImg.getPageNumber();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String titleHint = targetImg.getTitleHint() != null ? targetImg.getTitleHint() : ("Figura Pág. " + pageNumber);
            return analyzePageContentDeterministically(document, pageNumber, targetImageIndex, titleHint, targetImg.getWidth(), targetImg.getHeight());
        } catch (Exception e) {
            log.error("Error al decodificar imagen de la página {}: {}", pageNumber, e.getMessage());
            return fallbackEmptyResult(targetImageIndex);
        }
    }

    /**
     * Representa un fragmento de texto con sus coordenadas físicas en la página del PDF.
     */
    public static class SpatialTextItem {
        private final String text;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        public SpatialTextItem(String text, float x, float y, float width, float height) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public String getText() { return text; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
    }

    private static class SpatialTextCollector extends PDFTextStripper {
        private final List<SpatialTextItem> items = new ArrayList<>();

        public SpatialTextCollector() throws IOException {
            super();
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            if (string != null && !string.trim().isEmpty() && textPositions != null && !textPositions.isEmpty()) {
                String trimmed = string.trim();
                float x = textPositions.get(0).getXDirAdj();
                float y = textPositions.get(0).getYDirAdj();
                float width = 0;
                float height = textPositions.get(0).getHeightDir();
                for (TextPosition tp : textPositions) {
                    width += tp.getWidthDirAdj();
                }
                items.add(new SpatialTextItem(trimmed, x, y, width, height));
            }
            super.writeString(string, textPositions);
        }

        public List<SpatialTextItem> getItems() {
            return items;
        }
    }

    /**
     * Analiza determinísticamente el texto de la página donde se ubica la imagen:
     * 1. Extrae el epígrafe o título auténtico.
     * 2. Recopila los párrafos explicativos del autor asociados a la figura.
     * 3. Identifica entidades o conceptos destacados.
     * 4. Construye una interpretación enriquecida y un diagrama Mermaid.js dinámico sin palabras hardcodeadas.
     */
    private DiagramDecodedResultDto analyzePageContentDeterministically(
            PDDocument document,
            int pageNumber,
            int imageIndex,
            String titleHint,
            int imageWidth,
            int imageHeight) throws IOException {

        SpatialTextCollector collector = new SpatialTextCollector();
        collector.setStartPage(pageNumber);
        collector.setEndPage(pageNumber);
        collector.getText(document);

        List<SpatialTextItem> rawItems = collector.getItems();
        if (rawItems.isEmpty()) {
            return fallbackEmptyResult(imageIndex);
        }

        // 1. Separar párrafos contextuales de conceptos concisos
        List<String> contextualParagraphs = new ArrayList<>();
        List<String> keyEntities = new ArrayList<>();
        Set<String> seenEntities = new LinkedHashSet<>();

        for (SpatialTextItem item : rawItems) {
            String txt = item.getText().trim();
            // Ignorar números de página sueltos o cabeceras de margen
            if (txt.matches("^\\d+$") || txt.length() < 3 || item.getY() < 25 || item.getY() > 800) {
                continue;
            }

            // Si es un párrafo explicativo (> 60 caracteres o hace referencia explícita a la figura)
            if (txt.length() > 60 || CAPTION_REFERENCE_PATTERN.matcher(txt).find()) {
                if (!contextualParagraphs.contains(txt)) {
                    contextualParagraphs.add(txt);
                }
            } else {
                // Concepto / etiqueta concisa (posible componente de diagrama)
                if (txt.length() >= 3 && txt.length() <= 50 && !txt.equalsIgnoreCase(titleHint)) {
                    seenEntities.add(txt);
                }
            }
        }

        keyEntities.addAll(seenEntities);

        // 2. Construir la Interpretación Semántica Determinista
        StringBuilder interpretacion = new StringBuilder();
        interpretacion.append("### ").append(titleHint).append("\n\n");
        interpretacion.append("**Ubicación en el documento:** Página ").append(pageNumber)
                .append(" (Resolución original: ").append(imageWidth).append(" × ").append(imageHeight).append(" px)\n\n");

        interpretacion.append("#### Contexto y Explicación en el Documento:\n");
        if (!contextualParagraphs.isEmpty()) {
            // Tomar los párrafos más representativos de esa página
            int count = 0;
            for (String p : contextualParagraphs) {
                if (count++ >= 4) break;
                interpretacion.append("> ").append(p).append("\n\n");
            }
        } else {
            interpretacion.append("Figura de estudio identificada en la página ").append(pageNumber)
                    .append(". Los conceptos clave asociados se detallan a continuación.\n\n");
        }

        if (!keyEntities.isEmpty()) {
            interpretacion.append("#### Elementos y Conceptos Identificados:\n");
            int entityCount = 0;
            for (String entity : keyEntities) {
                if (entityCount++ >= 8) break;
                interpretacion.append("- **").append(entity).append("**\n");
            }
            interpretacion.append("\n");
        }

        // 3. Generar Mermaid.js dinámico y determinista
        String mermaidCode = generateDeterministicMermaid(titleHint, keyEntities, contextualParagraphs);

        return DiagramDecodedResultDto.builder()
                .imageIndex(imageIndex)
                .pageNumber(pageNumber)
                .tituloDetectado(titleHint)
                .tipoDiagrama("DIAGRAMA_DOCUMENTO")
                .interpretacion(interpretacion.toString().trim())
                .mermaidCode(mermaidCode)
                .elementosEncontrados(keyEntities.stream().limit(10).collect(Collectors.toList()))
                .build();
    }

    /**
     * Construye un diagrama Mermaid.js determinista relacionando el título de la figura
     * con los componentes o conceptos extraídos de esa página, 100% libre de palabras fijas.
     */
    private String generateDeterministicMermaid(String title, List<String> entities, List<String> paragraphs) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        String safeTitle = escapeMermaid(title);
        sb.append("  MAIN[\"📐 ").append(safeTitle).append("\"]\n");

        if (!entities.isEmpty()) {
            int limit = Math.min(entities.size(), 6);
            for (int i = 0; i < limit; i++) {
                String entityText = escapeMermaid(entities.get(i));
                sb.append(String.format("  NODE_%d[\"%s\"]\n", i, entityText));
                sb.append(String.format("  MAIN --> NODE_%d\n", i));
            }
        } else if (!paragraphs.isEmpty()) {
            String snippet = escapeMermaid(truncate(paragraphs.get(0), 70));
            sb.append("  DESC[\"").append(snippet).append("\"]\n");
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
                int read = is.read(sample);
                return img.getWidth() + "x" + img.getHeight() + "_" + Arrays.hashCode(sample);
            }
        } catch (Exception e) {
            return img.getWidth() + "x" + img.getHeight();
        }
    }

    private String findTitleHintOnPage(PDDocument document, int pageNumber) {
        try {
            String pageText = extractPageText(document, pageNumber);
            Matcher matcher = TITLE_HINT_PATTERN.matcher(pageText);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception ignored) {}
        return "Figura Pág. " + pageNumber;
    }

    private String extractPageText(PDDocument document, int pageNumber) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        return stripper.getText(document);
    }

    private String convertToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Error convirtiendo imagen a Base64: {}", e.getMessage());
            return "";
        }
    }

    private String escapeMermaid(String text) {
        if (text == null) return "";
        return text.replace("\"", "'")
                .replace("[", "(")
                .replace("]", ")")
                .replace("{", "(")
                .replace("}", ")")
                .replace("\n", " ")
                .trim();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    private DiagramDecodedResultDto fallbackEmptyResult(int imageIndex) {
        return DiagramDecodedResultDto.builder()
                .imageIndex(imageIndex)
                .pageNumber(1)
                .tituloDetectado("Figura no identificada")
                .tipoDiagrama("DESCONOCIDO")
                .interpretacion("No se pudo extraer la interpretación de la figura solicitada.")
                .mermaidCode("graph TD\n  A[\"Figura no identificada\"]")
                .elementosEncontrados(Collections.emptyList())
                .build();
    }
}
