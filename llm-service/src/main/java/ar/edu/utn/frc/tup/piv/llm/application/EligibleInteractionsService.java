package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.RealCaseAnonymizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EligibleInteractionsService {
  private final ObjectMapper mapper;
  private static final UUID INT_1 = UUID.fromString("e1111111-1111-1111-1111-111111111111");
  private static final UUID INT_2 = UUID.fromString("e2222222-2222-2222-2222-222222222222");

  public EligibleInteractionsService(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public List<EligibleInteractionSummary> listEligible(UUID courseId) {
    return List.of(
        new EligibleInteractionSummary(INT_1, "Consulta de alumno sobre tope de pila: Lucas (l.gomez@alumno.frc.utn.edu.ar)", "ch-pila-01", "Pilas con arreglos"),
        new EligibleInteractionSummary(INT_2, "Consulta de alumna sobre inserción en ABB: Mariana (m.rodriguez@facultad.edu.ar)", "ch-tree-02", "Árboles binarios de búsqueda")
    );
  }

  public Optional<JsonNode> anonymizePreview(UUID courseId, UUID interactionId) {
    ObjectNode raw = sampleInteraction(interactionId);
    if (raw == null) return Optional.empty();
    JsonNode anonymized = RealCaseAnonymizer.anonymize(raw);
    return Optional.of(anonymized);
  }

  private ObjectNode sampleInteraction(UUID interactionId) {
    if (INT_1.equals(interactionId)) {
      ObjectNode root = mapper.createObjectNode();
      root.put("id", INT_1.toString());
      root.put("author", "Interacción real anonimizada");
      ObjectNode ctx = root.putObject("challengeContext");
      ctx.put("externalChallengeId", "ch-pila-01");
      ctx.put("statement", "Implementación de una Pila con array estático.");
      ArrayNode t = root.putArray("transcript");
      t.addObject().put("role", "STUDENT").put("content", "Hola tutor, soy Lucas (email l.gomez@alumno.frc.utn.edu.ar, cel 3514829102). En push(), ¿cómo valido si el array llegó al tope?").put("position", 0);
      t.addObject().put("role", "TUTOR").put("content", "Hola Lucas. Podés comparar si tope == arr.length - 1 antes de insertar para evitar desbordamiento.").put("position", 1);
      t.addObject().put("role", "STUDENT").put("content", "Perfecto, si tope == arr.length - 1 lanzo StackOverflowException. ¡Muchas gracias!").put("position", 2);
      root.putObject("metadata").put("source", "chat-student-interaction").put("student_id", "88321");
      return root;
    } else if (INT_2.equals(interactionId)) {
      ObjectNode root = mapper.createObjectNode();
      root.put("id", INT_2.toString());
      root.put("author", "Interacción real anonimizada");
      ObjectNode ctx = root.putObject("challengeContext");
      ctx.put("externalChallengeId", "ch-tree-02");
      ctx.put("statement", "Árbol binario de búsqueda: método insert(valor).");
      ArrayNode t = root.putArray("transcript");
      t.addObject().put("role", "STUDENT").put("content", "Hola, soy Mariana (m.rodriguez@facultad.edu.ar). Mi árbol no guarda el orden cuando agrego nodos menores.").put("position", 0);
      t.addObject().put("role", "TUTOR").put("content", "Revisá la comparación: si el valor nuevo es menor al nodo actual, la llamada recursiva debe ser hacia la izquierda.").put("position", 1);
      t.addObject().put("role", "STUDENT").put("content", "Tenías razón, estaba asignando a la derecha. Ahora sí pasa los tests.").put("position", 2);
      root.putObject("metadata").put("source", "chat-student-interaction").put("email", "m.rodriguez@facultad.edu.ar");
      return root;
    }
    return null;
  }

  public record EligibleInteractionSummary(UUID id, String preview, String externalChallengeId, String statement) {}
}
