package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidEmbeddingException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.MessageRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Implementa `POST /api/llm/rag/chat` — `docs/contracts/llm-service-v1.openapi.yaml`
 * (`RagChatRequest`/`Response`). Orquesta: validación de fuentes (partición obligatoria por
 * `courseCohortId`, `AGENTS.md` §2) → {@link RagQueryGuardrail} → caché → embedding de la
 * pregunta ({@link EmbeddingInvocationService}) → búsqueda semántica ({@link VectorStorePort}) →
 * prompt con citas de fuente/página (`prompts/tutor-rag/{system,user}-v1.txt`) →
 * {@link ModelInvocationService} → histórico multi-turno → auditoría.
 *
 * <p>Portado de `demoLLMSpringAi/.../rag/service/TutorRagService.java`, con dos cambios de
 * diseño respecto de la demo: (1) valida que cada `documentId` pedido pertenezca a un
 * {@link RagDocument} activo del `courseCohortId` del request — nunca se mezcla material de otra
 * cohorte (regla dura de `AGENTS.md`), cosa que la demo no podía garantizar por ser
 * single-tenant; (2) no reintenta con un "modelo liviano" ante un error — el modelo activo es una
 * fila de `function_model_config`, cambiarla es una operación administrativa, no un fallback en
 * código (mismo criterio que `LLM-S01-H10`). */
@Service
public class RagChatService {
  private static final String OPERATION = "rag.chat";
  private static final String DEFAULT_TUTOR_ROLE = "Profesor Tutor Pedagógico";
  private static final int CONTEXT_TOP_K = 8;
  private static final int CITATION_TOP_K = 4;
  private static final int HISTORY_WINDOW = 4;

  private final ModelInvocationService models;
  private final EmbeddingInvocationService embeddings;
  private final VectorStorePort vectorStore;
  private final RagDocumentRepository documents;
  private final ConversationRepository conversations;
  private final MessageRepository messages;
  private final RagQueryGuardrail guardrail;
  private final AuditRepository audit;
  private final IdempotencyRepository idempotency;
  private final ObjectMapper mapper;
  private final Duration modelTimeout;
  private final Duration embeddingTimeout;
  private final String systemPromptTemplate;
  private final String userPromptTemplate;

  public RagChatService(ModelInvocationService models, EmbeddingInvocationService embeddings,
      VectorStorePort vectorStore, RagDocumentRepository documents, ConversationRepository conversations,
      MessageRepository messages, RagQueryGuardrail guardrail, AuditRepository audit,
      IdempotencyRepository idempotency, ObjectMapper mapper,
      @Value("${llm.tutor.invocation-timeout-ms:8000}") long modelTimeoutMs,
      @Value("${llm.rag.embedding-timeout-ms:8000}") long embeddingTimeoutMs) {
    this.models = models;
    this.embeddings = embeddings;
    this.vectorStore = vectorStore;
    this.documents = documents;
    this.conversations = conversations;
    this.messages = messages;
    this.guardrail = guardrail;
    this.audit = audit;
    this.idempotency = idempotency;
    this.mapper = mapper;
    this.modelTimeout = Duration.ofMillis(modelTimeoutMs);
    this.embeddingTimeout = Duration.ofMillis(embeddingTimeoutMs);
    this.systemPromptTemplate = readPrompt("system-v1.txt");
    this.userPromptTemplate = readPrompt("user-v1.txt");
  }

  private String readPrompt(String file) {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("prompts/tutor-rag/" + file)) {
      if (in == null) return "";
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      return "";
    }
  }

  public Response responder(Request request, UUID idempotencyKey, CallerIdentity actor) {
    String hash = hash(request);
    var replay = idempotency.replay(OPERATION, actor, idempotencyKey, hash);
    if (replay.isPresent()) {
      return parse(replay.get());
    }

    Response response = doRespond(request, actor);
    idempotency.complete(OPERATION, actor, idempotencyKey, UUID.randomUUID(), mapper.valueToTree(response));
    return response;
  }

  private Response doRespond(Request request, CallerIdentity actor) {
    List<UUID> requested = request.documentIds() == null ? List.of() : request.documentIds();
    if (requested.isEmpty()) {
      return sinFuente(request);
    }

    Set<UUID> activeIds = documents.findActiveByCourse(request.courseCohortId()).stream()
        .map(RagDocument::id).collect(Collectors.toCollection(HashSet::new));
    List<UUID> authorizedDocIds = requested.stream().distinct().filter(activeIds::contains).toList();
    if (authorizedDocIds.isEmpty()) {
      return sinFuente(request);
    }

    // Clave de caché (#679): cohorte + conjunto de fuentes AUTORIZADAS (ordenado y deduplicado, para
    // que el mismo conjunto en otro orden pegue en el mismo slot). Que se arme con las autorizadas
    // y no con las pedidas es lo que invalida el caché al retirar una fuente (#672): retirada, o la
    // selección queda vacía (y se abstiene antes de llegar acá) o el docKey cambia y falla el hit.
    String docKey = request.courseCohortId() + "|"
        + authorizedDocIds.stream().map(UUID::toString).sorted().collect(Collectors.joining(";"));

    var validation = guardrail.validate(request.pregunta(), actor.delegatedUserId().toString());
    if (!validation.valid()) {
      return new Response(validation.userMessage(), validation.status(),
          "Consulta interceptada por medidas de seguridad y validación de uso.", 0, false,
          DEFAULT_TUTOR_ROLE, List.of(), request.conversacionId());
    }

    var cached = guardrail.getCachedResponse(docKey, request.pregunta());
    if (cached.isPresent()) {
      Response hit = cached.get();
      return new Response(hit.respuesta(), hit.estado(), "Respuesta servida desde caché en memoria (0 tokens gastados).",
          0, true, hit.rolTutor(), hit.fuentes(), hit.conversacionId());
    }

    EmbeddingResult queryEmbedding;
    try {
      queryEmbedding = embeddings.embed(request.pregunta(), embeddingTimeout);
    } catch (EmbeddingTimeoutException | InvalidEmbeddingException exception) {
      return new Response(
          "El tutor no está disponible en este momento. Podés reintentar tu consulta en unos segundos.",
          "UNAVAILABLE", null, 0, false, DEFAULT_TUTOR_ROLE, List.of(), request.conversacionId());
    }

    List<DocumentChunk> contextChunks = vectorStore.searchTopK(request.courseCohortId(), authorizedDocIds, queryEmbedding.vector(), CONTEXT_TOP_K);
    List<DocumentChunk> citationChunks = contextChunks.subList(0, Math.min(CITATION_TOP_K, contextChunks.size()));
    List<SourceCitation> fuentes = citationChunks.stream().map(chunk -> new SourceCitation(
        chunk.documentId(), chunk.documentName() != null ? chunk.documentName() : "Documento",
        chunk.pageNumber(), chunk.chunkIndex(), chunk.similarityScore(), truncate(chunk.content(), 180)))
        .collect(Collectors.toList());

    Conversation conversation = resolveConversation(request, authorizedDocIds);
    List<Message> historicoReciente = recentHistory(conversation.id());
    messages.save(Message.de(conversation.id(), Message.ROL_ALUMNO, request.pregunta()));

    String systemPrompt = systemPromptTemplate.replace("{totalFuentes}", String.valueOf(authorizedDocIds.size()));
    String userPrompt = buildUserPrompt(contextChunks, historicoReciente, request.pregunta());

    String respuestaTexto;
    String estado;
    try {
      var result = models.invoke(ModelFunction.TUTOR, systemPrompt, userPrompt, modelTimeout);
      respuestaTexto = result.text();
      estado = "OK";
    } catch (ModelTimeoutException | InvalidModelResponseException exception) {
      respuestaTexto = "El tutor no está disponible en este momento. Podés reintentar tu consulta en unos segundos.";
      estado = "UNAVAILABLE";
    }

    messages.save(Message.de(conversation.id(), Message.ROL_TUTOR, respuestaTexto));

    int tokensEstimados = ((systemPrompt.length() + userPrompt.length()) / 4) + (respuestaTexto.length() / 4);
    Response response = new Response(respuestaTexto, estado,
        "OK".equals(estado) ? String.format("Respuesta generada a partir de %d fuente(s) seleccionada(s).", authorizedDocIds.size()) : null,
        tokensEstimados, false, DEFAULT_TUTOR_ROLE, fuentes, conversation.id());

    audit.record(OPERATION, "rag-interaction", UUID.randomUUID(), actor,
        "{\"courseCohortId\":\"" + request.courseCohortId() + "\",\"documentIds\":" + authorizedDocIds.size()
            + ",\"conversacionId\":\"" + conversation.id() + "\",\"estado\":\"" + estado + "\"}");
    guardrail.cacheResponse(docKey, request.pregunta(), response);
    return response;
  }

  private Response sinFuente(Request request) {
    return new Response(
        "Debes seleccionar al menos una fuente o documento en el panel lateral para formular tu consulta.",
        "BLOCKED_NO_SOURCE", "No se ha seleccionado ninguna fuente autorizada para la consulta.", 0, false,
        DEFAULT_TUTOR_ROLE, List.of(), request.conversacionId());
  }

  private Conversation resolveConversation(Request request, List<UUID> docIds) {
    if (request.conversacionId() != null) {
      Optional<Conversation> existing = conversations.findById(request.conversacionId());
      if (existing.isPresent()) {
        Conversation conversation = existing.get();
        // Aislamiento entre cohortes: reutilizar una conversación de otro curso filtraria
        // historial de otra cohorte al prompt del tutor.
        if (!request.courseCohortId().equals(conversation.courseCohortId())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN,
              "La conversación no pertenece a este curso");
        }
        return conversation;
      }
    }
    return conversations.save(nuevaConversacion(request, docIds));
  }

  private Conversation nuevaConversacion(Request request, List<UUID> docIds) {
    String titulo = docIds.size() == 1
        ? documents.findById(docIds.get(0)).map(d -> "Tutoría: " + d.fileName()).orElse("Tutoría RAG")
        : "Tutoría Multi-Fuente (" + docIds.size() + " fuentes)";
    return Conversation.nueva(request.courseCohortId(), request.learnerId(), null, titulo);
  }

  private List<Message> recentHistory(UUID conversationId) {
    List<Message> all = messages.findByConversationId(conversationId);
    return all.subList(Math.max(0, all.size() - HISTORY_WINDOW), all.size());
  }

  private String buildUserPrompt(List<DocumentChunk> chunks, List<Message> historico, String pregunta) {
    StringBuilder contexto = new StringBuilder();
    for (DocumentChunk chunk : chunks) {
      String docLabel = chunk.documentName() != null ? chunk.documentName() : "Documento";
      contexto.append(String.format("[Fuente: \"%s\" | Página %d]: %s%n%n", docLabel, chunk.pageNumber(), chunk.content()));
    }
    StringBuilder hist = new StringBuilder();
    for (Message m : historico) {
      hist.append(String.format("%s: %s%n", m.rol(), m.contenido()));
    }
    String template = userPromptTemplate.isBlank()
        ? "<contexto_fuentes>\n{contexto}\n</contexto_fuentes>\n<historial_reciente>\n{historico}\n</historial_reciente>\n<pregunta_estudiante>\n{pregunta}\n</pregunta_estudiante>"
        : userPromptTemplate;
    return template
        .replace("{contexto}", contexto.toString().trim())
        .replace("{historico}", hist.toString().trim())
        .replace("{pregunta}", pregunta.trim());
  }

  private String truncate(String text, int maxLength) {
    if (text == null) return "";
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
  }

  private Response parse(com.fasterxml.jackson.databind.JsonNode node) {
    try {
      return mapper.treeToValue(node, Response.class);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo leer la respuesta idempotente del chat RAG", exception);
    }
  }

  private String hash(Request request) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      String docIds = request.documentIds() == null ? "" : request.documentIds().stream()
          .map(UUID::toString).sorted().collect(Collectors.joining(","));
      String canonical = request.courseCohortId() + "|" + request.learnerId() + "|" + docIds + "|"
          + request.pregunta() + "|" + request.conversacionId();
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** Espejo de `RagChatRequest` del contrato v1. */
  public record Request(UUID courseCohortId, UUID learnerId, List<UUID> documentIds, String pregunta, UUID conversacionId) {}

  /** Espejo de `RagChatResponse` del contrato v1. */
  public record Response(String respuesta, String estado, String mensajeValidacion, int tokensGastados,
      boolean cached, String rolTutor, List<SourceCitation> fuentes, UUID conversacionId) {}

  /** Espejo de `RagSourceCitation` del contrato v1. */
  public record SourceCitation(UUID documentId, String documentName, int pageNumber, int chunkIndex, double score, String textoExtracto) {}
}
