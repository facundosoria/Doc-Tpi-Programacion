package ar.edu.utn.frc.tup.piv.llm.domain.goldenset;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GoldenSetUpdateProposal(
    UUID id,
    UUID courseId,
    UUID courseFamilyId,
    UUID baseVersionId,
    int baseVersion,
    int baseCaseCount,
    OffsetDateTime detectedAt) {}
