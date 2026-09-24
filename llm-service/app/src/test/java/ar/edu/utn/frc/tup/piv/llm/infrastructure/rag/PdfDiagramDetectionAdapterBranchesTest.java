package ar.edu.utn.frc.tup.piv.llm.infrastructure.rag;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

class PdfDiagramDetectionAdapterBranchesTest {
  private final PdfDiagramDetectionAdapter adapter = new PdfDiagramDetectionAdapter();

  private static BufferedImage image(int w, int h, Color c) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    var g = img.createGraphics();
    g.setColor(c);
    g.fillRect(0, 0, w, h);
    g.setColor(Color.WHITE);
    g.fillOval(w / 4, h / 4, w / 2, h / 2);
    g.dispose();
    return img;
  }

  private static void text(PDPageContentStream s, float x, float y, String t) throws IOException {
    s.beginText();
    s.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
    s.newLineAtOffset(x, y);
    s.showText(t);
    s.endText();
  }

  private static byte[] bytes(PDDocument d) throws IOException {
    var out = new ByteArrayOutputStream();
    d.save(out);
    d.close();
    return out.toByteArray();
  }

  @Test
  void nullOrEmptyBytesYieldNoDetections() {
    assertThat(adapter.detectImages(null)).isEmpty();
    assertThat(adapter.detectImages(new byte[0])).isEmpty();
  }

  @Test
  void corruptPdfIsSwallowedAndDecodeReturnsEmptyFallback() {
    byte[] junk = "esto no es un pdf".getBytes();
    assertThat(adapter.detectImages(junk)).isEmpty();
    assertThat(adapter.decodeDiagram(junk, 0).tipoDiagrama()).isEqualTo(DiagramDecodeResult.TIPO_DESCONOCIDO);
  }

  @Test
  void negativeIndexReturnsEmptyFallback() throws Exception {
    byte[] pdf = pdfWithImage(200, 150, "Figura 1: algo");
    assertThat(adapter.decodeDiagram(pdf, -1).tipoDiagrama()).isEqualTo(DiagramDecodeResult.TIPO_DESCONOCIDO);
  }

  @Test
  void imageRepeatedAcrossPagesIsTreatedAsHeaderAndDiscarded() throws Exception {
    PDDocument doc = new PDDocument();
    PDImageXObject x = LosslessFactory.createFromImage(doc, image(200, 150, Color.RED));
    for (int i = 0; i < 2; i++) {
      PDPage p = new PDPage();
      doc.addPage(p);
      try (var s = new PDPageContentStream(doc, p)) {
        s.drawImage(x, 50, 500, 200, 150);
      }
    }
    assertThat(adapter.detectImages(bytes(doc))).isEmpty();
  }

  @Test
  void decorativeBarsAreDiscardedByAspectRatio() throws Exception {
    assertThat(adapter.detectImages(pdfWithImage(600, 100, null))).isEmpty();
    assertThat(adapter.detectImages(pdfWithImage(100, 600, null))).isEmpty();
  }

  @Test
  void imageWithoutCaptionUsesPageFallbackTitleAndPngDataUri() throws Exception {
    var images = adapter.detectImages(pdfWithImage(200, 150, null));
    assertThat(images).hasSize(1);
    assertThat(images.get(0).titleHint()).isEqualTo("Figura Pág. 1");
    assertThat(images.get(0).base64Data()).startsWith("data:image/png;base64,");
  }

  @Test
  void twoDistinctImagesGetConsecutiveIndexes() throws Exception {
    PDDocument doc = new PDDocument();
    PDPage p = new PDPage();
    doc.addPage(p);
    PDImageXObject a = LosslessFactory.createFromImage(doc, image(200, 150, Color.RED));
    PDImageXObject b = LosslessFactory.createFromImage(doc, image(180, 140, Color.GREEN));
    try (var s = new PDPageContentStream(doc, p)) {
      s.drawImage(a, 50, 600, 200, 150);
      s.drawImage(b, 300, 600, 180, 140);
    }
    var images = adapter.detectImages(bytes(doc));
    assertThat(images).extracting(i -> i.imageIndex()).containsExactly(0, 1);
  }

  @Test
  void decodeBuildsMermaidWithEntitiesAndContextParagraphs() throws Exception {
    PDDocument doc = new PDDocument();
    PDPage p = new PDPage();
    doc.addPage(p);
    PDImageXObject a = LosslessFactory.createFromImage(doc, image(200, 150, Color.RED));
    try (var s = new PDPageContentStream(doc, p)) {
      s.drawImage(a, 50, 500, 200, 150);
      text(s, 50, 480, "Figura 1: Arquitectura general");
      text(s, 50, 460, "Controlador");
      text(s, 50, 445, "Servicio");
      text(s, 50, 430, "Repositorio");
      text(s, 50, 415, "La figura muestra como interactuan los componentes principales del sistema");
    }
    DiagramDecodeResult r = adapter.decodeDiagram(bytes(doc), 0);
    assertThat(r.tipoDiagrama()).isEqualTo("DIAGRAMA_DOCUMENTO");
    assertThat(r.mermaidCode()).startsWith("graph TD").contains("NODE_0").contains("MAIN --> NODE_0");
    assertThat(r.interpretacion()).contains("Elementos y Conceptos Identificados").contains("Controlador");
    assertThat(r.interpretacion()).contains("> ");
  }

  @Test
  void decodeWithOnlyParagraphsUsesDescNode() throws Exception {
    PDDocument doc = new PDDocument();
    PDPage p = new PDPage();
    doc.addPage(p);
    PDImageXObject a = LosslessFactory.createFromImage(doc, image(200, 150, Color.RED));
    try (var s = new PDPageContentStream(doc, p)) {
      s.drawImage(a, 50, 500, 200, 150);
      text(s, 50, 460, "La figura muestra una estructura de datos con varios elementos enlazados entre si");
    }
    DiagramDecodeResult r = adapter.decodeDiagram(bytes(doc), 0);
    assertThat(r.mermaidCode()).contains("DESC[").contains("MAIN --> DESC");
  }

  @Test
  void decodeOfImageOnlyPageReturnsEmptyFallbackBecauseNoText() throws Exception {
    DiagramDecodeResult r = adapter.decodeDiagram(pdfWithImage(200, 150, null), 0);
    assertThat(r.tipoDiagrama()).isEqualTo(DiagramDecodeResult.TIPO_DESCONOCIDO);
  }

  private byte[] pdfWithImage(int w, int h, String caption) throws IOException {
    PDDocument doc = new PDDocument();
    PDPage p = new PDPage();
    doc.addPage(p);
    PDImageXObject x = LosslessFactory.createFromImage(doc, image(w, h, Color.BLUE));
    try (var s = new PDPageContentStream(doc, p)) {
      s.drawImage(x, 20, 100, Math.min(w, 500), Math.min(h, 500));
      if (caption != null) text(s, 50, 80, caption);
    }
    return bytes(doc);
  }
}
