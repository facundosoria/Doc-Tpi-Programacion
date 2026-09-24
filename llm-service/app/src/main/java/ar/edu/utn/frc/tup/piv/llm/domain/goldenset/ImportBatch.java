package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import java.util.UUID;

public record ImportBatch(
    UUID id,
    UUID goldenSetVersionId,
    String format,
    String state,
    int rows) {}
