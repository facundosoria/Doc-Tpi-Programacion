package ar.edu.utn.frc.tup.piv.llm.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record CreateGoldenSetRequest(
    @NotBlank String rubricVersion, @Pattern(regexp = "es") String language) {}

record CreateGoldenSetEntryRequest(@NotNull JsonNode transcript, @NotNull JsonNode referenceScores) {}
