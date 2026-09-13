package com.example.demo.rag;

import com.example.demo.rag.dto.DiagramDecodedResultDto;
import com.example.demo.rag.dto.ImageDetectionDto;
import com.example.demo.rag.service.PdfDiagramPositionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfDiagramPositionServiceTest {

    private PdfDiagramPositionService service;

    @BeforeEach
    void setUp() {
        service = new PdfDiagramPositionService();
    }

    @Test
    void testDetectImages_EmptyOrNull() {
        List<ImageDetectionDto> nullResult = service.detectImages(null);
        assertTrue(nullResult.isEmpty());

        List<ImageDetectionDto> emptyResult = service.detectImages(new byte[0]);
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void testDecodeDiagram_InvalidIndex() {
        DiagramDecodedResultDto result = service.decodeDiagram(new byte[0], 99);
        assertNotNull(result);
        assertEquals("DESCONOCIDO", result.getTipoDiagrama());
    }

    @Test
    void testDetectAndDecode_DockerLayersPdf() throws IOException {
        byte[] pdfBytes = createSamplePdfWithImageAndText();

        // 1. Detectar imágenes
        List<ImageDetectionDto> images = service.detectImages(pdfBytes);
        assertFalse(images.isEmpty(), "Debe detectar la imagen incrustada de prueba");
        assertEquals(1, images.size());
        ImageDetectionDto img = images.get(0);
        assertTrue(img.getWidth() >= 60);
        assertTrue(img.getHeight() >= 60);
        assertTrue(img.getBase64Data().startsWith("data:image/png;base64,"));

        // 2. Decodificar diagrama determinísticamente
        DiagramDecodedResultDto decoded = service.decodeDiagram(pdfBytes, 0);
        assertNotNull(decoded);
        assertEquals("DIAGRAMA_DOCUMENTO", decoded.getTipoDiagrama());
        assertNotNull(decoded.getInterpretacion());
        assertFalse(decoded.getInterpretacion().isBlank());
        assertTrue(decoded.getTituloDetectado().contains("Arquitectura Docker"));
        assertTrue(decoded.getMermaidCode().contains("graph TD"));
        assertTrue(decoded.getMermaidCode().contains("Docker Engine") || decoded.getMermaidCode().contains("Hardware"));
    }

    @Test
    void testDetectAndDecode_GenericNetworkLayers_NoHardcodedWords() throws IOException {
        byte[] pdfBytes = createSamplePdfWithNetworkDiagram();

        List<ImageDetectionDto> images = service.detectImages(pdfBytes);
        assertFalse(images.isEmpty());

        DiagramDecodedResultDto decoded = service.decodeDiagram(pdfBytes, 0);
        assertNotNull(decoded);
        assertEquals("DIAGRAMA_DOCUMENTO", decoded.getTipoDiagrama());
        assertNotNull(decoded.getInterpretacion());

        // Verificar que detectó el epígrafe y elementos dinámicamente sin que existan palabras fijas en el código
        assertTrue(decoded.getTituloDetectado().contains("Pila de Protocolos de Red"));
        assertTrue(decoded.getMermaidCode().contains("graph TD"));
        assertTrue(decoded.getInterpretacion().contains("Protocolos de Red") || decoded.getInterpretacion().contains("Capa"));
    }

    @Test
    void testFilterOut_RepetitiveHeaderFooterImages() throws IOException {
        byte[] pdfBytes = createPdfWithRepetitiveHeadersAndOneRealFigure();

        List<ImageDetectionDto> images = service.detectImages(pdfBytes);
        // Debe descartar las barras repetitivas de cabecera en las 3 páginas y dejar solo la figura real
        assertEquals(1, images.size(), "Debe filtrar las cabeceras repetitivas y dejar exactamente 1 imagen de contenido");
        ImageDetectionDto figure = images.get(0);
        assertEquals(2, figure.getPageNumber(), "La figura real está en la página 2");
        assertEquals(150, figure.getWidth());
        assertEquals(120, figure.getHeight());
    }

    private byte[] createPdfWithRepetitiveHeadersAndOneRealFigure() throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Cabecera institucional idéntica repetida en 3 páginas (ratio alargado 400x30 = 13.3)
            BufferedImage headerImg = new BufferedImage(400, 30, BufferedImage.TYPE_INT_RGB);
            Graphics2D gH = headerImg.createGraphics();
            gH.setColor(Color.DARK_GRAY);
            gH.fillRect(0, 0, 400, 30);
            gH.dispose();
            PDImageXObject pdHeader = LosslessFactory.createFromImage(document, headerImg);

            // Figura de contenido genuino en la página 2 (150x120)
            BufferedImage figureImg = new BufferedImage(150, 120, BufferedImage.TYPE_INT_RGB);
            Graphics2D gF = figureImg.createGraphics();
            gF.setColor(Color.ORANGE);
            gF.fillRect(0, 0, 150, 120);
            gF.dispose();
            PDImageXObject pdFigure = LosslessFactory.createFromImage(document, figureImg);

            for (int p = 1; p <= 3; p++) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    // Dibujar cabecera repetitiva en el borde superior de cada página
                    cs.drawImage(pdHeader, 50, 750, 400, 30);

                    if (p == 2) {
                        // Dibujar figura legítima en el cuerpo de la página 2
                        cs.drawImage(pdFigure, 100, 400, 150, 120);
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                        cs.newLineAtOffset(50, 350);
                        cs.showText("Figura 2: Esquema del Flujo de Datos");
                        cs.endText();
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createSamplePdfWithNetworkDiagram() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            BufferedImage bImg = new BufferedImage(120, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bImg.createGraphics();
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, 120, 100);
            g.dispose();

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bImg);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.drawImage(pdImage, 100, 400, 120, 100);

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Figura 3: Pila de Protocolos de Red");
                cs.newLineAtOffset(0, -30);
                cs.showText("Capa de Aplicacion (HTTP y DNS)");
                cs.newLineAtOffset(0, -25);
                cs.showText("Capa de Transporte (Protocolo TCP)");
                cs.newLineAtOffset(0, -25);
                cs.showText("Capa de Red (Protocolo IP)");
                cs.newLineAtOffset(0, -25);
                cs.showText("Capa de Enlace (Ethernet)");
                cs.newLineAtOffset(0, -25);
                cs.showText("Capa Fisica (Cables y Senales)");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createSamplePdfWithImageAndText() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            BufferedImage bImg = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bImg.createGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 120, 120);
            g.setColor(Color.WHITE);
            g.drawString("Docker", 20, 60);
            g.dispose();

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bImg);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.drawImage(pdImage, 100, 400, 120, 120);

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Imagen 1: Elaboracion propia - Arquitectura Docker");
                cs.newLineAtOffset(0, -30);
                cs.showText("Hardware");
                cs.newLineAtOffset(0, -20);
                cs.showText("Sistema Operativo Anfitrion");
                cs.newLineAtOffset(0, -20);
                cs.showText("Docker Engine");
                cs.newLineAtOffset(0, -20);
                cs.showText("Contenedor #1 APP #1");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
