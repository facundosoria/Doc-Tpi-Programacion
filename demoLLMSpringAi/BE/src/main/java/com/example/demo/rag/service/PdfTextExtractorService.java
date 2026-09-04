package com.example.demo.rag.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractorService.class);

    public record ExtractedPage(int pageNumber, String text) {}

    public record ExtractedPdf(int totalPages, List<ExtractedPage> pages, String fullText) {}

    /**
     * Extrae el texto de un PDF preservando la paginación para citaciones RAG precisas.
     */
    public ExtractedPdf extractTextWithPages(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("El archivo PDF está vacío o no contiene bytes válidos.");
        }

        List<ExtractedPage> extractedPages = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("El archivo PDF está protegido con contraseña. Por favor sube un PDF sin cifrar.");
            }

            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                throw new IllegalArgumentException("El archivo PDF no contiene páginas.");
            }

            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String rawPageText = stripper.getText(document);

                // Normalización de espacios y saltos de línea superfluos
                String cleanedText = cleanText(rawPageText);

                if (!cleanedText.isBlank()) {
                    extractedPages.add(new ExtractedPage(page, cleanedText));
                    fullTextBuilder.append(cleanedText).append("\n\n");
                }
            }

            log.info("PDF extraído exitosamente: {} páginas totales, {} con contenido textual",
                    totalPages, extractedPages.size());

            return new ExtractedPdf(totalPages, extractedPages, fullTextBuilder.toString().trim());
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
