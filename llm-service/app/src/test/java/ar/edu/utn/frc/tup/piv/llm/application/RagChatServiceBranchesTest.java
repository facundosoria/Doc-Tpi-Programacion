package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EmbeddingInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagChatService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagQueryGuardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidEmbeddingException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.MessageRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RagChatServiceBranchesTest {
  private static final String Q = "¿qué es Docker y en qué se diferencia de una VM?";
  private final ObjectMapper mapper = new ObjectMapper();
  private final ModelInvocationService models = mock(ModelInvocationService.class);
  private final EmbeddingInvocationService embeddings = mock(EmbeddingInvocationService.class);
  private final VectorStorePort vectorStore = mock(VectorStorePort.class);
  private final RagDocumentRepository documents = mock(RagDocumentRepository.class);
  private final ConversationRepository conversations = mock(ConversationRepository.class);
  private final MessageRepository messages = mock(MessageRepository.class);
  private final AuditRepository audit = mock(AuditRepository.class);
  private final IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
  private RagChatService service;
  private UUID cohort = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(conversations.save(any())).thenAnswer(i -> i.getArgument(0));
    when(messages.findByConversationId(any())).thenReturn(List.of());
    when(embeddings.embed(anyString(), any())).thenReturn(new EmbeddingResult(new float[4], "fake", "m"));
    service = new RagChatService(models, embeddings, vectorStore, documents, conversations, messages,
        new RagQueryGuardrail(), audit, idempotency, mapper, 1000, 1000);
  }

  private CallerIdentity actor() { return new CallerIdentity("practice-service", UUID.randomUUID(), "r", null); }

  private RagDocument doc(UUID id, String name) {
    var d = new RagDocument(id, cohort, name, 10, 1, 1, OffsetDateTime.now(), "p", true);
    when(documents.findById(id)).thenReturn(Optional.of(d));
    return d;
  }

  private void activeDocs(RagDocument... ds) { when(documents.findActiveByCourse(cohort)).thenReturn(List.of(ds)); }

  private RagChatService.Request req(List<UUID> ids, UUID conv) {
    return new RagChatService.Request(cohort, UUID.randomUUID(), ids, Q, conv);
  }

  @Test
  void nullDocumentIdsAreTreatedAsNoSource() {
    var r = service.responder(req(null, null), UUID.randomUUID(), actor());
    assertThat(r.estado()).isEqualTo("BLOCKED_NO_SOURCE");
    assertThat(r.fuentes()).isEmpty();
    verify(idempotency).complete(eq("rag.chat"), any(), any(), any(), any());
  }

  @Test
  void embeddingTimeoutYieldsUnavailableAndNothingPersisted() {
    UUID id = UUID.randomUUID(); activeDocs(doc(id, "a.pdf"));
    when(embeddings.embed(anyString(), any())).thenThrow(new EmbeddingTimeoutException("t"));
    var r = service.responder(req(List.of(id), null), UUID.randomUUID(), actor());
    assertThat(r.estado()).isEqualTo("UNAVAILABLE");
    assertThat(r.mensajeValidacion()).isNull();
    verify(messages, never()).save(any());
    verify(models, never()).invoke(any(), anyString(), anyString(), any());
  }

  @Test
  void invalidEmbeddingYieldsUnavailable() {
    UUID id = UUID.randomUUID(); activeDocs(doc(id, "a.pdf"));
    when(embeddings.embed(anyString(), any())).thenThrow(new InvalidEmbeddingException("bad"));
    var r = service.responder(req(List.of(id), UUID.randomUUID()), UUID.randomUUID(), actor());
    assertThat(r.estado()).isEqualTo("UNAVAILABLE");
    assertThat(r.conversacionId()).isNotNull();
  }

  @Test
  void modelTimeoutYieldsUnavailableAnswerButStillPersistsBothMessages() {
    UUID id = UUID.randomUUID(); activeDocs(doc(id, "a.pdf"));
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(models.invoke(eq(ModelFunction.TUTOR), anyString(), anyString(), any())).thenThrow(new ModelTimeoutException("t"));
    var r = service.responder(req(List.of(id), null), UUID.randomUUID(), actor());
    assertThat(r.estado()).isEqualTo("UNAVAILABLE");
    assertThat(r.mensajeValidacion()).isNull();
    assertThat(r.fuentes()).isEmpty();
    verify(messages, org.mockito.Mockito.times(2)).save(any(Message.class));
    ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
    verify(audit).record(eq("rag.chat"), eq("rag-interaction"), any(), any(), details.capture());
    assertThat(details.getValue()).contains("\"estado\":\"UNAVAILABLE\"");
  }

  @Test
  void invalidModelResponseYieldsUnavailable() {
    UUID id = UUID.randomUUID(); activeDocs(doc(id, "a.pdf"));
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(models.invoke(any(), anyString(), anyString(), any())).thenThrow(new InvalidModelResponseException("x"));
    assertThat(service.responder(req(List.of(id), null), UUID.randomUUID(), actor()).estado()).isEqualTo("UNAVAILABLE");
  }

  @Test
  void multipleSourcesCreateAMultiSourceConversationAndDeduplicateIds() {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID(), foreign = UUID.randomUUID();
    activeDocs(doc(a, "a.pdf"), doc(b, "b.pdf"));
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("ok", "p", "m"));

    var r = service.responder(req(List.of(a, b, a, foreign), null), UUID.randomUUID(), actor());

    assertThat(r.estado()).isEqualTo("OK");
    assertThat(r.mensajeValidacion()).contains("2 fuente(s)");
    ArgumentCaptor<Conversation> conv = ArgumentCaptor.forClass(Conversation.class);
    verify(conversations).save(conv.capture());
    assertThat(conv.getValue().titulo()).isEqualTo("Tutoría Multi-Fuente (2 fuentes)");
    ArgumentCaptor<List<UUID>> searched = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).searchTopK(eq(cohort), searched.capture(), any(), eq(8));
    assertThat(searched.getValue()).containsExactly(a, b);
  }

  @Test
  void singleSourceConversationIsTitledWithTheFileName() {
    UUID a = UUID.randomUUID(); activeDocs(doc(a, "docker.pdf"));
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("ok", "p", "m"));
    service.responder(req(List.of(a), null), UUID.randomUUID(), actor());
    ArgumentCaptor<Conversation> conv = ArgumentCaptor.forClass(Conversation.class);
    verify(conversations).save(conv.capture());
    assertThat(conv.getValue().titulo()).isEqualTo("Tutoría: docker.pdf");
  }

  @Test
  void singleSourceWithoutLookupFallsBackToGenericTitle() {
    UUID a = UUID.randomUUID();
    var d = new RagDocument(a, cohort, "x.pdf", 1, 1, 1, OffsetDateTime.now(), "p", true);
    activeDocs(d);
    when(documents.findById(a)).thenReturn(Optional.empty());
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("ok", "p", "m"));
    service.responder(req(List.of(a), null), UUID.randomUUID(), actor());
    ArgumentCaptor<Conversation> conv = ArgumentCaptor.forClass(Conversation.class);
    verify(conversations).save(conv.capture());
    assertThat(conv.getValue().titulo()).isEqualTo("Tutoría RAG");
  }

  @Test
  void existingConversationIsReusedAndRecentHistoryIsLimitedToFourMessagesInThePrompt() {
    UUID a = UUID.randomUUID(); activeDocs(doc(a, "a.pdf"));
    UUID convId = UUID.randomUUID();
    var existing = new Conversation(convId, cohort, UUID.randomUUID(), null, "t", Conversation.ESTADO_ABIERTA, OffsetDateTime.now());
    when(conversations.findById(convId)).thenReturn(Optional.of(existing));
    List<Message> hist = new ArrayList<>();
    for (int i = 0; i < 6; i++) hist.add(Message.de(convId, i % 2 == 0 ? Message.ROL_ALUMNO : Message.ROL_TUTOR, "mensaje-" + i));
    when(messages.findByConversationId(convId)).thenReturn(hist);
    String longContent = "z".repeat(300);
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of(
        new DocumentChunk(UUID.randomUUID(), a, null, 3, 1, longContent, 0.5)));
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("resp", "p", "m"));

    var r = service.responder(req(List.of(a), convId), UUID.randomUUID(), actor());

    assertThat(r.conversacionId()).isEqualTo(convId);
    verify(conversations, never()).save(any());
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    verify(models).invoke(eq(ModelFunction.TUTOR), anyString(), prompt.capture(), any());
    assertThat(prompt.getValue()).doesNotContain("mensaje-0").doesNotContain("mensaje-1")
        .contains("mensaje-2").contains("mensaje-5").contains(Q).contains("Documento");
    assertThat(r.fuentes()).hasSize(1);
    assertThat(r.fuentes().get(0).documentName()).isEqualTo("Documento");
    assertThat(r.fuentes().get(0).textoExtracto()).hasSize(183).endsWith("...");
  }

  @Test
  void unknownConversationIdCreatesANewConversation() {
    UUID a = UUID.randomUUID(); activeDocs(doc(a, "a.pdf"));
    UUID missing = UUID.randomUUID();
    when(conversations.findById(missing)).thenReturn(Optional.empty());
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("ok", "p", "m"));
    var r = service.responder(req(List.of(a), missing), UUID.randomUUID(), actor());
    verify(conversations).save(any());
    assertThat(r.conversacionId()).isNotEqualTo(missing).isNotNull();
  }

  @Test
  void onlyTheTopFourChunksAreCitedButAllReachThePrompt() {
    UUID a = UUID.randomUUID(); activeDocs(doc(a, "a.pdf"));
    List<DocumentChunk> chunks = new ArrayList<>();
    for (int i = 0; i < 6; i++) chunks.add(new DocumentChunk(UUID.randomUUID(), a, "a.pdf", i + 1, i, "contenido" + i, 0.9 - i * 0.1));
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(chunks);
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("resp", "p", "m"));
    var r = service.responder(req(List.of(a), null), UUID.randomUUID(), actor());
    assertThat(r.fuentes()).hasSize(4);
    assertThat(r.tokensGastados()).isPositive();
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    verify(models).invoke(any(), anyString(), prompt.capture(), any());
    assertThat(prompt.getValue()).contains("contenido5");
  }

  @Test
  void nullChunkContentIsCitedAsEmptyExcerpt() {
    UUID a = UUID.randomUUID(); activeDocs(doc(a, "a.pdf"));
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of(
        new DocumentChunk(UUID.randomUUID(), a, "a.pdf", 1, 0, null, 0.1)));
    when(models.invoke(any(), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult("resp", "p", "m"));
    var r = service.responder(req(List.of(a), null), UUID.randomUUID(), actor());
    assertThat(r.fuentes().get(0).textoExtracto()).isEmpty();
  }

  @Test
  void unreadableReplayPayloadRaisesIllegalState() {
    ObjectNode bad = mapper.createObjectNode();
    bad.put("tokensGastados", "no-es-un-numero");
    when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.of(bad));
    assertThatThrownBy(() -> service.responder(req(List.of(UUID.randomUUID()), null), UUID.randomUUID(), actor()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("idempotente");
  }

  @Test
  void replayHashIgnoresDocumentOrderButNullDocumentsAreSupported() {
    // el request sin documentIds tiene hash estable; se completa y se devuelve BLOCKED_NO_SOURCE
    var r1 = service.responder(req(null, null), UUID.randomUUID(), actor());
    var r2 = service.responder(req(List.of(), null), UUID.randomUUID(), actor());
    assertThat(r1.respuesta()).isEqualTo(r2.respuesta());
  }
}
