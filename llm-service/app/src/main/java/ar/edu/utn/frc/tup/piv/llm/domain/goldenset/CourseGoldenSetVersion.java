package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import java.util.UUID;

public record CourseGoldenSetVersion(
    UUID id,
    UUID familyId,
    int version,
    String state,
    UUID basedOnVersionId) {}
