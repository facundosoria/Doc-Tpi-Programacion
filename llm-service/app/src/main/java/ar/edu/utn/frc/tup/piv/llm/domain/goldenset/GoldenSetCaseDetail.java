package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record GoldenSetCaseDetail(
    UUID id,
    JsonNode transcript,
    JsonNode challengeContext,
    JsonNode referenceScores) {}
