package ar.edu.utn.frc.tup.piv.llm.shadow.infrastructure;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ConversationRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.MessageRepository;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowSampleSource;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import org.springframework.stereotype.Component;

/** Reproduce las conversaciones reales del tutor del curso (las más recientes primero). No tienen nota
 * humana. El {@code course_cohort_id} de la conversación se toma como el curso de la corrida.
 * Las conversaciones con menos de dos mensajes se omiten: no hay intercambio que evaluar. */
@Component
public class TutorConversationShadowSource implements ShadowSampleSource {
  private final ConversationRepository conversations;
  private final MessageRepository messages;
  private final ObjectMapper mapper;

  public TutorConversationShadowSource(ConversationRepository conversations, MessageRepository messages, ObjectMapper mapper) {
    this.conversations = conversations;
    this.messages = messages;
    this.mapper = mapper;
  }

  @Override
  public ShadowRun.Source source() {
    return ShadowRun.Source.TUTOR_CONVERSATIONS;
  }

  @Override
  public List<Sample> load(ShadowRun run) {
    return conversations.find(null, run.courseId()).stream()
        .map(conversation -> {
          var history = messages.findByConversationId(conversation.id());
          if (history.size() < 2) {
            return null;
          }
          ArrayNode transcript = mapper.createArrayNode();
          int position = 0;
          for (var message : history) {
            transcript.addObject().put("role", "alumno".equals(message.rol()) ? "STUDENT" : "TUTOR")
                .put("content", message.contenido()).put("position", position++);
          }
          var context = mapper.createObjectNode();
          context.put("statement", conversation.challengeId() == null ? conversation.titulo()
              : "Desafío " + conversation.challengeId());
          return new Sample(conversation.id().toString(), transcript, context, null);
        })
        .filter(java.util.Objects::nonNull)
        .limit(run.sampleSize())
        .toList();
  }
}
