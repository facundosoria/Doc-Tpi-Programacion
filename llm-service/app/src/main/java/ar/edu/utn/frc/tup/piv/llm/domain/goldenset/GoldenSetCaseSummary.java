package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import java.util.UUID;

public record GoldenSetCaseSummary(
    UUID id,
    int order,
    String author,
    String reviewState) {}
