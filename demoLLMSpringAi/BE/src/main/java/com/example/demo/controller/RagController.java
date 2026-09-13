package com.example.demo.controller;

import com.example.demo.rag.dto.DiagramDecodedResultDto;
import com.example.demo.rag.dto.ImageDetectionDto;
import com.example.demo.rag.dto.RagChatRequest;
import com.example.demo.rag.dto.RagChatResponse;
import com.example.demo.rag.model.DocumentChunk;
import com.example.demo.rag.model.RagDocumentInfo;
import com.example.demo.rag.service.EmbeddingService;
import com.example.demo.rag.service.FileStorageService;
import com.example.demo.rag.service.PdfDiagramPositionService;
import com.example.demo.rag.service.PdfTextExtractorService;
import com.example.demo.rag.service.PgVectorStoreService;
import com.example.demo.rag.service.TextChunkerService;
import com.example.demo.rag.service.TutorRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG Tutor Pedagógico", description = "Endpoints para carga de PDFs, gestión de fuentes estilo NotebookLM e indexación pgvector")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

    private final PdfTextExtractorService pdfExtractor;
    private final TextChunkerService textChunker;
    private final PgVectorStoreService vectorStore;
    private final EmbeddingService embeddingService;
    private final TutorRagService tutorRagService;
    private final PdfDiagramPositionService diagramPositionService;
    private final FileStorageService fileStorageService;

    public RagController(PdfTextExtractorService pdfExtractor,
                          TextChunkerService textChunker,
                          PgVectorStoreService vectorStore,
                          EmbeddingService embeddingService,
                          TutorRagService tutorRagService,
                          PdfDiagramPositionService diagramPositionService,
                          FileStorageService fileStorageService) {
        this.pdfExtractor = pdfExtractor;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.tutorRagService = tutorRagService;
        this.diagramPositionService = diagramPositionService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/documentos")
    @Operation(summary = "Listar todas las fuentes disponibles en la biblioteca (NotebookLM)")
    public ResponseEntity<List<RagDocumentInfo>> getAllDocuments() {
        return ResponseEntity.ok(vectorStore.getAllDocuments());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir e indexar un documento PDF en pgvector")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo PDF está vacío."));
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo supera el tamaño máximo permitido de 25MB."));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Solo se admiten documentos en formato PDF (.pdf)."));
            }

            byte[] bytes = file.getBytes();
            RagDocumentInfo info = processAndIndexPdf(originalFilename, bytes);

            return ResponseEntity.status(HttpStatus.CREATED).body(info);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al procesar archivo PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocurrió un error al procesar el archivo PDF: " + e.getMessage()));
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "Preguntar al tutor sobre las fuentes seleccionadas (NotebookLM)")
    public ResponseEntity<RagChatResponse> chatWithTutor(
            @Valid @RequestBody RagChatRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = servletRequest.getRemoteAddr();
        RagChatResponse response = tutorRagService.responderConsultaRag(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documento/{id}")
    @Operation(summary = "Obtener información de un documento indexado")
    public ResponseEntity<?> getDocumentInfo(@PathVariable String id) {
        return vectorStore.getDocument(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/documento/{id}")
    @Operation(summary = "Eliminar una fuente de la biblioteca y sus chunks de pgvector")
    public ResponseEntity<?> deleteDocument(@PathVariable String id) {
        vectorStore.deleteDocument(id);
        fileStorageService.deletePdf(id);
        return ResponseEntity.ok(Map.of("message", "Documento eliminado correctamente.", "documentId", id));
    }

    @GetMapping("/documento/{id}/chunks")
    @Operation(summary = "Listar los fragmentos vectorizados del documento")
    public ResponseEntity<List<DocumentChunk>> getDocumentChunks(@PathVariable String id) {
        List<DocumentChunk> chunks = vectorStore.getAllChunks(id);
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/documento/{id}/imagenes")
    @Operation(summary = "Detectar e inspeccionar imágenes incrustadas dentro del PDF")
    public ResponseEntity<?> getDocumentImages(@PathVariable String id) {
        byte[] bytes = resolvePdfBytes(id);
        if (bytes == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "El archivo PDF de este documento no está disponible en el servidor (fue registrado antes de habilitar la persistencia). Por favor vuelve a arrastrar o subir el archivo para inspeccionar sus imágenes."));
        }
        List<ImageDetectionDto> images = diagramPositionService.detectImages(bytes);
        return ResponseEntity.ok(images);
    }

    @PostMapping("/documento/{id}/decodificar-imagen/{imageIndex}")
    @Operation(summary = "Decodificar determinísticamente la estructura de una imagen del PDF sin IA")
    public ResponseEntity<?> decodeImage(@PathVariable String id, @PathVariable int imageIndex) {
        byte[] bytes = resolvePdfBytes(id);
        if (bytes == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se encontró el archivo PDF para decodificar. Por favor vuelve a subir el documento."));
        }
        DiagramDecodedResultDto result = diagramPositionService.decodeDiagram(bytes, imageIndex);
        return ResponseEntity.ok(result);
    }

    private byte[] resolvePdfBytes(String documentId) {
        byte[] bytes = fileStorageService.loadPdf(documentId);
        if (bytes == null) {
            bytes = vectorStore.getDocumentPdfBytes(documentId);
            if (bytes != null) {
                try {
                    fileStorageService.savePdf(documentId, bytes);
                } catch (Exception ignored) {}
            }
        }
        return bytes;
    }

    @PostMapping("/documento/{id}/indexar-diagrama")
    @Operation(summary = "Persistir manualmente un diagrama decodificado como chunk semántico en pgvector")
    public ResponseEntity<?> indexDiagramChunk(@PathVariable String id, @RequestBody DiagramDecodedResultDto result) {
        var docOpt = vectorStore.getDocument(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var doc = docOpt.get();

        String chunkId = UUID.randomUUID().toString();
        String content = String.format(
                "[Fuente: \"%s\" | Pág. %d | 📐 Figura/Diagrama: %s]\nTipo: %s\n\nInterpretación:\n%s\n\nEstructura:\n```mermaid\n%s\n```",
                doc.getFileName(),
                result.getPageNumber(),
                result.getTituloDetectado(),
                result.getTipoDiagrama(),
                result.getInterpretacion() != null ? result.getInterpretacion() : "",
                result.getMermaidCode() != null ? result.getMermaidCode() : ""
        );

        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .documentId(id)
                .documentName(doc.getFileName())
                .pageNumber(result.getPageNumber())
                .chunkIndex(doc.getChunkCount() + 1)
                .content(content)
                .similarityScore(1.0)
                .build();

        float[] embedding = null;
        try {
            List<float[]> embs = embeddingService.computeEmbeddings(List.of(content));
            if (embs != null && !embs.isEmpty()) {
                embedding = embs.get(0);
            }
        } catch (Exception e) {
            log.warn("No se pudo generar vector para el diagrama: {}", e.getMessage());
        }

        vectorStore.addSingleChunkWithVector(chunk, embedding);
        return ResponseEntity.ok(Map.of("message", "Diagrama indexado exitosamente como chunk en pgvector.", "chunkId", chunkId));
    }

    @PostMapping("/sample-pdf")
    @Operation(summary = "Carga y auto-indexa el PDF de demostración en la base de datos")
    public ResponseEntity<?> loadSamplePdf() {
        try {
            Path samplePath = Paths.get("PRD-Plataforma-Gamificada-TP.pdf");
            if (!Files.exists(samplePath)) {
                samplePath = Paths.get("BE", "PRD-Plataforma-Gamificada-TP.pdf");
            }
            if (!Files.exists(samplePath)) {
                samplePath = Paths.get("..", "BE", "PRD-Plataforma-Gamificada-TP.pdf");
            }

            if (!Files.exists(samplePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No se encontró el PDF de muestra 'PRD-Plataforma-Gamificada-TP.pdf'."));
            }

            byte[] bytes = Files.readAllBytes(samplePath);
            RagDocumentInfo info = processAndIndexPdf("PRD-Plataforma-Gamificada-TP.pdf", bytes);
            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Error al cargar PDF de muestra: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error cargando PDF de muestra: " + e.getMessage()));
        }
    }

    private RagDocumentInfo processAndIndexPdf(String fileName, byte[] bytes) throws IOException {
        // 1. Extraer texto por páginas con Apache PDFBox
        var extractedPdf = pdfExtractor.extractTextWithPages(bytes);

        // 2. Generar identificador único para la fuente
        String docId = UUID.randomUUID().toString();

        // 3. Persistir bytes en almacenamiento local para no perderlos tras reinicios
        fileStorageService.savePdf(docId, bytes);

        // 4. Segmentación en fragmentos semánticos con solapamiento
        List<DocumentChunk> chunks = textChunker.createChunks(docId, fileName, extractedPdf.pages());

        // 5. Detectar y auto-decodificar diagramas sin IA
        try {
            List<ImageDetectionDto> images = diagramPositionService.detectImages(bytes);
            for (ImageDetectionDto img : images) {
                DiagramDecodedResultDto decoded = diagramPositionService.decodeDiagram(bytes, img.getImageIndex());
                if (decoded != null && !"DESCONOCIDO".equalsIgnoreCase(decoded.getTipoDiagrama())) {
                    String diagChunkId = UUID.randomUUID().toString();
                    String diagContent = String.format(
                            "[Fuente: \"%s\" | Pág. %d | 📐 Figura/Diagrama: %s]\nTipo: %s\n\nInterpretación:\n%s\n\nEstructura:\n```mermaid\n%s\n```",
                            fileName,
                            decoded.getPageNumber(),
                            decoded.getTituloDetectado(),
                            decoded.getTipoDiagrama(),
                            decoded.getInterpretacion() != null ? decoded.getInterpretacion() : "",
                            decoded.getMermaidCode() != null ? decoded.getMermaidCode() : ""
                    );

                    DocumentChunk diagChunk = DocumentChunk.builder()
                            .id(diagChunkId)
                            .documentId(docId)
                            .documentName(fileName)
                            .pageNumber(decoded.getPageNumber())
                            .chunkIndex(chunks.size() + img.getImageIndex() + 1)
                            .content(diagContent)
                            .similarityScore(1.0)
                            .build();

                    chunks.add(diagChunk);
                }
            }
        } catch (Exception e) {
            log.warn("Aviso al auto-decodificar diagramas: {}", e.getMessage());
        }

        // 6. Vista previa de texto
        String preview = extractedPdf.fullText().length() > 250
                ? extractedPdf.fullText().substring(0, 250) + "..."
                : extractedPdf.fullText();

        RagDocumentInfo docInfo = RagDocumentInfo.builder()
                .documentId(docId)
                .fileName(fileName)
                .fileSizeBytes(bytes.length)
                .pageCount(extractedPdf.totalPages())
                .chunkCount(chunks.size())
                .uploadedAt(LocalDateTime.now())
                .previewText(preview)
                .build();

        // 7. Cálculo de embeddings densos con Google AI Studio (text-embedding-004)
        List<String> chunkTexts = chunks.stream().map(DocumentChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.computeEmbeddings(chunkTexts);

        // 8. Indexación en PostgreSQL pgvector (con réplica en memoria y persistencia de PDF)
        vectorStore.indexDocumentWithVectors(docInfo, chunks, embeddings, bytes);

        return docInfo;
    }
}
