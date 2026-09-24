package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDetectionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPdf;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ImageDetection;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.PdfTextExtractionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.PdfUploadValidator;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocumentNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.TextChunker;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Casos de uso de `/api/llm/rag/documents/**` (EP-09, alta/inspección de fuentes): sube y
 * procesa un PDF (extracción de texto, chunking, detección y auto-decodificación de diagramas,
 * embeddings) y lo indexa en {@link VectorStorePort}. Portado de
 * `demoLLMSpringAi/.../controller/RagController.java` (la parte de `processAndIndexPdf`), separando
 * la orquestación (acá) de la extracción/decodificación (puertos hacia `infrastructure`). */
@Service
public class RagIngestionService {
  private static final int PREVIEW_LENGTH = 250;
  private static final String OPERATION = "rag.document.upload";

  private final PdfTextExtractionPort textExtractor;
  private final DiagramDetectionPort diagramDetector;
  private final VectorStorePort vectorStore;
  private final RagDocumentRepository documents;
  private final EmbeddingInvocationService embeddings;
  private final IdempotencyRepository idempotency;
  private final ObjectMapper mapper;
  private final TextChunker chunker = new TextChunker();
  private final PdfUploadValidator uploadValidator;
  private final long maxUploadBytes;
  private final Duration embeddingTimeout;

  public RagIngestionService(PdfTextExtractionPort textExtractor, DiagramDetectionPort diagramDetector,
      VectorStorePort vectorStore, RagDocumentRepository documents, EmbeddingInvocationService embeddings,
      IdempotencyRepository idempotency, ObjectMapper mapper,
      @Value("${llm.rag.max-upload-bytes:26214400}") long maxUploadBytes,
      @Value("${llm.rag.embedding-timeout-ms:8000}") long embeddingTimeoutMs) {
    this.textExtractor = textExtractor;
    this.diagramDetector = diagramDetector;
    this.vectorStore = vectorStore;
    this.documents = documents;
    this.embeddings = embeddings;
    this.idempotency = idempotency;
    this.mapper = mapper;
    this.maxUploadBytes = maxUploadBytes;
    this.uploadValidator = new PdfUploadValidator(maxUploadBytes);
    this.embeddingTimeout = Duration.ofMillis(embeddingTimeoutMs);
  }

  @Transactional(rollbackFor = Exception.class)
  public RagDocument upload(UUID courseCohortId, String fileName, byte[] bytes,
      UUID idempotencyKey, CallerIdentity actor) throws IOException {
    if (courseCohortId == null) {
      throw new IllegalArgumentException("courseCohortId es obligatorio");
    }

    uploadValidator.validateContent(bytes, fileName);

    String hash = hash(courseCohortId, fileName, bytes);
    Optional<JsonNode> replay = idempotency.replay(OPERATION, actor, idempotencyKey, hash);
    if (replay.isPresent()) {
      return parse(replay.get());
    }

    UUID documentId = UUID.randomUUID();
    ExtractedPdf extracted;
    try {
      extracted = textExtractor.extractTextWithPages(bytes);
    } catch (IOException exception) {
      // Un archivo vacío, que no es PDF, cifrado o dañado se rechaza con 422 y no deja
      // ninguna fuente creada (CA3 de LLM-EP09-H01). Sin este mapeo la IOException de
      // PDFBox escapaba como 500.
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          "El archivo no es un PDF válido o está dañado", exception);
    }
    List<DocumentChunk> chunks = new ArrayList<>(chunker.createChunks(documentId, fileName, extracted.pages()));

    appendDiagramChunks(documentId, fileName, bytes, chunks);

    String preview = extracted.fullText().length() > PREVIEW_LENGTH
        ? extracted.fullText().substring(0, PREVIEW_LENGTH) + "..."
        : extracted.fullText();

    List<String> chunkTexts = chunks.stream().map(DocumentChunk::content).toList();
    List<EmbeddingResult> chunkEmbeddings = embeddings.embedBatch(chunkTexts, embeddingTimeout);

    RagDocument document = new RagDocument(documentId, courseCohortId, fileName, bytes.length,
        extracted.totalPages(), chunks.size(), OffsetDateTime.now(), preview, true);

    // El documento se guarda primero: rag_chunks.document_id tiene FK contra rag_documents.
    RagDocument saved = documents.save(document, bytes);
    try {
      vectorStore.indexChunks(documentId, chunks, chunkEmbeddings);
    } catch (RuntimeException failure) {
      documents.deactivate(documentId); // no dejar un documento activo sin fragmentos
      throw failure;
    }
    idempotency.complete(OPERATION, actor, idempotencyKey, saved.id(), mapper.valueToTree(saved));
    return saved;
  }

  private RagDocument parse(JsonNode node) {
    try {
      return mapper.treeToValue(node, RagDocument.class);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo leer la respuesta idempotente de la fuente", exception);
    }
  }

  private String hash(UUID courseCohortId, String fileName, byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(courseCohortId.toString().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) '|');
      digest.update(fileName.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) '|');
      digest.update(bytes);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** Detecta imágenes/diagramas de contenido genuino y auto-indexa como chunks los que sí se
   * pudieron decodificar (mismo criterio que la demo: descarta los `DESCONOCIDO`). Un fallo acá
   * no aborta la subida del documento — se deja constancia (no se propaga la excepción), igual
   * que `RagController.processAndIndexPdf` en la demo. */
  private void appendDiagramChunks(UUID documentId, String fileName, byte[] bytes, List<DocumentChunk> chunks) {
    try {
      List<ImageDetection> images = diagramDetector.detectImages(bytes);
      for (ImageDetection image : images) {
        DiagramDecodeResult decoded = diagramDetector.decodeDiagram(bytes, image.imageIndex());
        if (!DiagramDecodeResult.TIPO_DESCONOCIDO.equalsIgnoreCase(decoded.tipoDiagrama())) {
          chunks.add(DocumentChunk.nuevo(documentId, fileName, decoded.pageNumber(), chunks.size(), diagramChunkContent(fileName, decoded)));
        }
      }
    } catch (Exception ignored) {
      // Aviso, no aborta la indexación del documento — mismo criterio que la demo.
    }
  }

  /** Tamaño declarado por el cliente, para cortar antes de cargar el archivo entero en memoria
   * (#669). El límite real lo vuelve a verificar {@link #upload} sobre los bytes ya leídos. */
  public void validateDeclaredUploadSize(long sizeInBytes, String fileName) {
    uploadValidator.validateDeclaredSize(sizeInBytes, fileName);
  }

  public List<RagDocument> list(UUID courseCohortId) {
    return documents.findActiveByCourse(courseCohortId);
  }

  public Optional<RagDocument> get(UUID id) {
    return documents.findById(id);
  }

  /** Retiro lógico de una fuente (#672, CA5 / Escenario BDD 3 de `h01.md`). Solo se puede retirar
   * una fuente de la propia cohorte: una de otra cohorte se comporta como inexistente
   * ({@link RagDocumentNotFoundException} → 404, no 403) para no filtrar existencia. Idempotente:
   * retirar una ya retirada devuelve el mismo resultado. Nunca borra la fila, sus chunks ni el PDF. */
  @Transactional
  public RagDocument retire(UUID id, UUID courseCohortId) {
    RagDocument document = documents.findById(id)
        .filter(found -> found.belongsTo(courseCohortId))
        .orElseThrow(() -> new RagDocumentNotFoundException(id));
    RagDocument retired = document.retire();
    if (document.active()) {
      documents.deactivate(id);
    }
    return retired;
  }

  public List<DocumentChunk> getChunks(UUID id) {
    return vectorStore.getChunks(id);
  }

  public Optional<byte[]> getPdfBytes(UUID id) {
    return documents.findById(id).flatMap(doc -> Optional.ofNullable(documents.getPdfBytes(id)));
  }

  public List<ImageDetection> detectImages(byte[] pdfBytes) {
    return diagramDetector.detectImages(pdfBytes);
  }

  public DiagramDecodeResult decodeImage(byte[] pdfBytes, int imageIndex) {
    return diagramDetector.decodeDiagram(pdfBytes, imageIndex);
  }

  /** Persiste manualmente un diagrama ya decodificado (ej. revisado por un docente) como chunk
   * semántico nuevo, calculando su embedding — portado de
   * `RagController.indexDiagramChunk` en la demo. */
  public void indexDiagram(UUID documentId, DiagramDecodeResult result) {
    RagDocument document = documents.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Fuente no encontrada: " + documentId));

    String content = diagramChunkContent(document.fileName(), result);
    DocumentChunk chunk = DocumentChunk.nuevo(documentId, document.fileName(), result.pageNumber(),
        document.chunkCount() + 1, content);
    EmbeddingResult embedding;
    try {
      embedding = embeddings.embed(content, embeddingTimeout);
    } catch (Exception exception) {
      embedding = null; // mismo criterio que la demo: un embedding fallido no bloquea el chunk
    }
    vectorStore.addChunk(chunk, embedding);
  }

  private String diagramChunkContent(String fileName, DiagramDecodeResult result) {
    return String.format(
        "[Fuente: \"%s\" | Pág. %d | Figura/Diagrama: %s]%nTipo: %s%n%nInterpretación:%n%s%n%nEstructura:%n```mermaid%n%s%n```",
        fileName, result.pageNumber(), result.tituloDetectado(), result.tipoDiagrama(),
        result.interpretacion() != null ? result.interpretacion() : "",
        result.mermaidCode() != null ? result.mermaidCode() : "");
  }

  /** Carga y auto-indexa el PDF de demostración PRD en la base de datos para la cohorte indicada.
   * Portado de `RagController.loadSamplePdf` de la demo. */
  public RagDocument uploadSample(UUID courseCohortId, UUID idempotencyKey, CallerIdentity actor) throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/fuentes/PRD-Plataforma-Gamificada-TP.pdf")) {
      if (is != null) {
        byte[] bytes = is.readAllBytes();
        return upload(courseCohortId, "PRD-Plataforma-Gamificada-TP.pdf", bytes, idempotencyKey, actor);
      }
    } catch (IOException ignored) {}

    Path[] candidates = new Path[] {
        Paths.get("src", "main", "resources", "fuentes", "PRD-Plataforma-Gamificada-TP.pdf"),
        Paths.get("docs", "fuentes", "PRD-Plataforma-Gamificada-TP.pdf"),
        Paths.get("llm-service", "docs", "fuentes", "PRD-Plataforma-Gamificada-TP.pdf"),
        Paths.get("..", "docs", "fuentes", "PRD-Plataforma-Gamificada-TP.pdf")
    };
    for (Path path : candidates) {
      if (Files.exists(path)) {
        try {
          byte[] bytes = Files.readAllBytes(path);
          return upload(courseCohortId, "PRD-Plataforma-Gamificada-TP.pdf", bytes, idempotencyKey, actor);
        } catch (IOException exception) {
          throw new IllegalStateException("Error al leer el archivo de muestra: " + path, exception);
        }
      }
    }
    throw new IllegalArgumentException("No se encontró el archivo de muestra 'PRD-Plataforma-Gamificada-TP.pdf'.");
  }
}
