package ar.edu.utn.frc.tup.piv.llm.infrastructure.rag;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ImageDetection;
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

class PdfDiagramDetectionAdapterTest {
  private final PdfDiagramDetectionAdapter adapter = new PdfDiagramDetectionAdapter();

  @Test
  void aPdfWithoutImagesHasNoDetections() throws Exception {
    byte[] pdf = pdfWithTextOnly("Solo texto, sin figuras.");

    assertThat(adapter.detectImages(pdf)).isEmpty();
  }

  @Test
  void detectsAGenuineContentImageAboveTheMinimumSize() throws Exception {
    byte[] pdf = pdfWithImageAndCaption("Figura 1: Arquitectura en capas de Docker.");

    var images = adapter.detectImages(pdf);

    assertThat(images).hasSize(1);
    assertThat(images.get(0).pageNumber()).isEqualTo(1);
    assertThat(images.get(0).width()).isGreaterThanOrEqualTo(100);
    assertThat(images.get(0).titleHint()).contains("Figura 1");
  }

  @Test
  void aTinyImageBelowTheMinimumSizeIsDiscardedAsAnIcon() throws Exception {
    byte[] pdf = pdfWithImage(30, 30);

    assertThat(adapter.detectImages(pdf)).isEmpty();
  }

  @Test
  void decodeDiagramOnAnOutOfRangeIndexReturnsTheEmptyFallback() throws Exception {
    byte[] pdf = pdfWithTextOnly("Sin figuras.");

    DiagramDecodeResult result = adapter.decodeDiagram(pdf, 0);

    assertThat(result.tipoDiagrama()).isEqualTo(DiagramDecodeResult.TIPO_DESCONOCIDO);
  }

  @Test
  void decodeDiagramOnADetectedImageReturnsAStructuredResult() throws Exception {
    byte[] pdf = pdfWithImageAndCaption("Figura 1: Arquitectura en capas de Docker.");

    DiagramDecodeResult result = adapter.decodeDiagram(pdf, 0);

    assertThat(result.pageNumber()).isEqualTo(1);
    assertThat(result.mermaidCode()).startsWith("graph TD");
    assertThat(result.tituloDetectado()).contains("Figura 1");
  }

  @Test
  void aRepeatedImageSignatureAcrossPagesIsFilteredAsAHeaderOrFooter() throws Exception {
    byte[] pdf = pdfWithSameImageOnTwoPages(200, 150);

    List<ImageDetection> images = adapter.detectImages(pdf);

    assertThat(images).isEmpty();
  }

  @Test
  void anImageWithExtremeAspectRatioIsDiscardedAsADecorativeBar() throws Exception {
    byte[] pdf = pdfWithImage(400, 60);

    List<ImageDetection> images = adapter.detectImages(pdf);

    assertThat(images).isEmpty();
  }

  @Test
  void decodeDiagramFallsBackToAPageNumberWhenNoCaptionIsDetected() throws Exception {
    byte[] pdf = pdfWithImageAndText(200, 150, "Contenido técnico sin epígrafe identificable.");

    DiagramDecodeResult result = adapter.decodeDiagram(pdf, 0);

    assertThat(result.tipoDiagrama()).isEqualTo("DIAGRAMA_DOCUMENTO");
    assertThat(result.tituloDetectado()).isEqualTo("Figura Pág. 1");
  }

  private byte[] pdfWithTextOnly(String text) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        stream.newLineAtOffset(50, 700);
        stream.showText(text);
        stream.endText();
      }
      return toBytes(document);
    }
  }

  private byte[] pdfWithSameImageOnTwoPages(int width, int height) throws IOException {
    try (PDDocument document = new PDDocument()) {
      BufferedImage image = solidImage(width, height);
      PDImageXObject xObject = LosslessFactory.createFromImage(document, image);
      for (int pageIndex = 0; pageIndex < 2; pageIndex++) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
          stream.drawImage(xObject, 50, 500, width, height);
        }
      }
      return toBytes(document);
    }
  }

  private byte[] pdfWithImage(int width, int height) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      BufferedImage image = solidImage(width, height);
      PDImageXObject xObject = LosslessFactory.createFromImage(document, image);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.drawImage(xObject, 50, 500, width, height);
      }
      return toBytes(document);
    }
  }

  private byte[] pdfWithImageAndCaption(String caption) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      BufferedImage image = solidImage(200, 150);
      PDImageXObject xObject = LosslessFactory.createFromImage(document, image);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.drawImage(xObject, 50, 500, 200, 150);
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        stream.newLineAtOffset(50, 480);
        stream.showText(caption);
        stream.endText();
      }
      return toBytes(document);
    }
  }

  private byte[] pdfWithImageAndText(int width, int height, String text) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      BufferedImage image = solidImage(width, height);
      PDImageXObject xObject = LosslessFactory.createFromImage(document, image);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.drawImage(xObject, 50, 500, width, height);
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        stream.newLineAtOffset(50, 480);
        stream.showText(text);
        stream.endText();
      }
      return toBytes(document);
    }
  }

  private BufferedImage solidImage(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    return image;
  }

  private byte[] toBytes(PDDocument document) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    document.save(out);
    return out.toByteArray();
  }
}
