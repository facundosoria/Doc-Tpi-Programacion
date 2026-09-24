package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public record GoldenSetDetail(
    UUID id,
    UUID familyId,
    String name,
    int version,
    String state,
    UUID basedOnVersionId,
    List<CaseItem> cases) {

  public record CaseItem(UUID id, int order, JsonNode transcript, JsonNode referenceScores) {}
}