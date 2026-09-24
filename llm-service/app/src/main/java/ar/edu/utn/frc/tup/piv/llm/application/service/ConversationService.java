package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeNotActiveException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatus;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeStatusPort;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationOwnershipException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.MessageRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Caso de uso de `POST/GET /api/llm/tutor/conversations` y
 * `GET /api/llm/tutor/conversations/{id}/messages` — el CRUD de conversaciones que
 * `docs/estado-implementacion/ep-05/interactions.md` había descartado y ahora se revisita (ver
 * `docs/estado-implementacion/ep-05/conversations.md`). {@link TutorInteractionService} resuelve
 * conversaciones por su cuenta al responder una interacción; este servicio es el que expone el
 * historial navegable por API, portado de `demoLLMSpringAi/.../controller/ChatController.java`. */
@Service
public class ConversationService {
  private static final String OPERATION = "tutor.conversation.create";

  private final ConversationRepository conversations;
  private final MessageRepository messages;
  private final IdempotencyRepository idempotency;
  private final AuditRepository audit;
  private final ObjectMapper mapper;
  private final ChallengeStatusPort challengeStatusPort;

  public ConversationService(ConversationRepository conversations, MessageRepository messages,
      IdempotencyRepository idempotency, AuditRepository audit, ObjectMapper mapper,
      ChallengeStatusPort challengeStatusPort) {
    this.conversations = conversations;
    this.messages = messages;
    this.idempotency = idempotency;
    this.audit = audit;
    this.mapper = mapper;
    this.challengeStatusPort = challengeStatusPort;
  }

  public ConversationService(ConversationRepository conversations, MessageRepository messages,
      IdempotencyRepository idempotency, AuditRepository audit, ObjectMapper mapper) {
    this(conversations, messages, idempotency, audit, mapper, null);
  }

  public Conversation create(UUID courseCohortId, UUID learnerId, UUID challengeId, String titulo,
      UUID idempotencyKey, CallerIdentity actor) {
    if (courseCohortId == null || learnerId == null) {
      throw new IllegalArgumentException("courseCohortId y learnerId son obligatorios");
    }

    if (challengeId != null && challengeStatusPort != null) {
      ChallengeStatus status = challengeStatusPort.consultar(challengeId);
      if (status != ChallengeStatus.ABIERTO) {
        throw new ChallengeNotActiveException(challengeId);
      }
    }

    String hash = hash(courseCohortId, learnerId, challengeId, titulo);
    var replay = idempotency.replay(OPERATION, actor, idempotencyKey, hash);
    if (replay.isPresent()) {
      return parse(replay.get());
    }

    Conversation conversation = conversations.save(Conversation.nueva(courseCohortId, learnerId, challengeId, titulo));
    audit.record(OPERATION, "conversation", conversation.id(), actor,
        "{\"courseCohortId\":\"" + courseCohortId + "\",\"learnerId\":\"" + learnerId + "\"}");
    idempotency.complete(OPERATION, actor, idempotencyKey, conversation.id(), mapper.valueToTree(conversation));
    return conversation;
  }

  public List<Conversation> list(UUID learnerId, UUID courseCohortId, UUID challengeId) {
    if (learnerId == null) {
      throw new IllegalArgumentException("learnerId es obligatorio");
    }
    return conversations.find(learnerId, courseCohortId, challengeId);
  }

  public List<Conversation> list(UUID learnerId, UUID courseCohortId) {
    return list(learnerId, courseCohortId, null);
  }

  public Message appendMessage(UUID conversationId, UUID requestingLearnerId, String contenido,
      UUID idempotencyKey, CallerIdentity actor) {
    if (contenido == null || contenido.isBlank()) {
      throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío");
    }
    Conversation conversation = conversations.findById(conversationId)
        .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada: " + conversationId));
    if (requestingLearnerId != null && !conversation.learnerId().equals(requestingLearnerId)) {
      throw new ConversationOwnershipException(conversationId);
    }
    if (conversation.challengeId() != null && challengeStatusPort != null) {
      ChallengeStatus status = challengeStatusPort.consultar(conversation.challengeId());
      if (status != ChallengeStatus.ABIERTO) {
        throw new ChallengeNotActiveException(conversation.challengeId());
      }
    }
    if (idempotencyKey != null && actor != null) {
      String hash = hashMessage(conversationId, contenido);
      var replay = idempotency.replay("tutor.conversation.message", actor, idempotencyKey, hash);
      if (replay.isPresent()) {
        return parseMessage(replay.get());
      }
      Message mensaje = conversation.crearMensajeAlumno(contenido.trim());
      Message saved = messages.save(mensaje);
      idempotency.complete("tutor.conversation.message", actor, idempotencyKey, saved.id(), mapper.valueToTree(saved));
      return saved;
    }
    Message mensaje = conversation.crearMensajeAlumno(contenido.trim());
    return messages.save(mensaje);
  }

  public Message appendMessage(UUID conversationId, UUID requestingLearnerId, String contenido) {
    return appendMessage(conversationId, requestingLearnerId, contenido, null, null);
  }

  public List<Message> messages(UUID conversationId, UUID requestingLearnerId) {
    Conversation conversation = conversations.findById(conversationId)
        .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada: " + conversationId));
    if (requestingLearnerId != null && !conversation.learnerId().equals(requestingLearnerId)) {
      throw new ConversationOwnershipException(conversationId);
    }
    return messages.findByConversationId(conversationId);
  }

  public List<Message> messages(UUID conversationId) {
    return messages(conversationId, null);
  }

  private Conversation parse(JsonNode node) {
    try {
      return mapper.treeToValue(node, Conversation.class);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo leer la respuesta idempotente de la conversación", exception);
    }
  }

  private String hash(UUID courseCohortId, UUID learnerId, UUID challengeId, String titulo) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      String canonical = courseCohortId + "|" + learnerId + "|" + challengeId + "|" + titulo;
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private Message parseMessage(JsonNode node) {
    try {
      return mapper.treeToValue(node, Message.class);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo leer la respuesta idempotente del mensaje", exception);
    }
  }

  private String hashMessage(UUID conversationId, String contenido) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      String canonical = conversationId + "|" + (contenido != null ? contenido.trim() : "");
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
