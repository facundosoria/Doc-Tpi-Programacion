package ar.edu.utn.frc.tup.piv.llm.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GoldenSetEntry(UUID id, JsonNode transcript, JsonNode referenceScores, OffsetDateTime createdAt) {}
