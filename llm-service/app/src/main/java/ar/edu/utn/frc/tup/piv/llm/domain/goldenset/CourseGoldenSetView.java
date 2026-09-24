package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import java.util.List;
import java.util.UUID;

public record CourseGoldenSetView(
    UUID id,
    UUID familyId,
    String name,
    int version,
    String state,
    UUID basedOnVersionId,
    List<GoldenSetCaseSummary> cases) {}
