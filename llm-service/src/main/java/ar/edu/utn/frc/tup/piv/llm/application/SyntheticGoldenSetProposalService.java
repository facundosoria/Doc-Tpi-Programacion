package ar.edu.utn.frc.tup.piv.llm.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SyntheticGoldenSetProposalService {
  private final ObjectMapper mapper;

  public SyntheticGoldenSetProposalService(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public SyntheticProposalResult propose(UUID courseId, int count) {
    int safeCount = Math.max(1, Math.min(count, 10));
    List<SyntheticCaseProposal> items = new ArrayList<>();

    for (int i = 0; i < safeCount; i++) {
      items.add(createSampleProposal(i));
    }

    return new SyntheticProposalResult(UUID.randomUUID(), "COMPLETED", items);
  }

  private SyntheticCaseProposal createSampleProposal(int index) {
    UUID caseId = UUID.randomUUID();
    int sampleIndex = index % 3;

    String challengeId;
    String statement;
    ArrayNode transcript = mapper.createArrayNode();

    if (sampleIndex == 0) {
      challengeId = "ch-rec-01";
      statement = "Algoritmo recursivo: cálculo de potencia con exponente entero.";
      transcript.addObject().put("role", "STUDENT").put("content", "¿Por qué mi función recursiva entra en bucle infinito si le paso exponente 0?").put("position", 0);
      transcript.addObject().put("role", "TUTOR").put("content", "Revisá el caso base. Para exponente 0, cualquier número no nulo elevado a 0 debe retornar 1 inmediatamente sin volver a llamarse.").put("position", 1);
      transcript.addObject().put("role", "STUDENT").put("content", "Entendido, tenía la condición `n < 0` en lugar de `n == 0`. Ahora funciona correctamente.").put("position", 2);
    } else if (sampleIndex == 1) {
      challengeId = "ch-sort-02";
      statement = "Implementación de ordenamiento MergeSort sobre listas enlazadas.";
      transcript.addObject().put("role", "STUDENT").put("content", "¿Cómo divido la lista enlazada a la mitad de forma eficiente en MergeSort?").put("position", 0);
      transcript.addObject().put("role", "TUTOR").put("content", "Podés usar la técnica de punteros rápido y lento (tortuga y liebre). Cuando el rápido llega al final, el lento está justo en el punto medio.").put("position", 1);
      transcript.addObject().put("role", "STUDENT").put("content", "Excelente idea, con eso evito recorrer dos veces la lista para contar los nodos.").put("position", 2);
    } else {
      challengeId = "ch-hash-03";
      statement = "Manejo de colisiones en Tabla Hash mediante direccionamiento abierto.";
      transcript.addObject().put("role", "STUDENT").put("content", "Al eliminar un elemento en direccionamiento abierto con exploración lineal, ¿por qué no puedo dejar la celda como nula?").put("position", 0);
      transcript.addObject().put("role", "TUTOR").put("content", "Si dejás la celda nula, la búsqueda de elementos posteriores con el mismo hash se detendrá prematuramente creyendo que no existen. Se debe marcar con una bandera 'BORRADO'.").put("position", 1);
      transcript.addObject().put("role", "STUDENT").put("content", "Claro, la búsqueda lineal corta al encontrar null. Usando la marca de borrado sigue buscando.").put("position", 2);
    }

    ObjectNode challengeCtx = mapper.createObjectNode();
    challengeCtx.put("externalChallengeId", challengeId);
    challengeCtx.put("statement", statement);

    return new SyntheticCaseProposal(caseId, transcript, challengeCtx, "Generador Asistido / LLM", "DRAFT");
  }

  public record SyntheticProposalResult(UUID id, String state, List<SyntheticCaseProposal> items) {}
  public record SyntheticCaseProposal(UUID id, JsonNode transcript, JsonNode challengeContext, String author, String reviewState) {}
}
