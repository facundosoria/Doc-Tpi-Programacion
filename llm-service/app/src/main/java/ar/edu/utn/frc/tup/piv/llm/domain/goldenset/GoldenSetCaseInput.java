package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import com.fasterxml.jackson.databind.JsonNode;

public record GoldenSetCaseInput(
    JsonNode transcript,
    JsonNode challengeContext,
    JsonNode metadata,
    String author,
    JsonNode referenceScores,
    JsonNode scoreJustifications) {}
