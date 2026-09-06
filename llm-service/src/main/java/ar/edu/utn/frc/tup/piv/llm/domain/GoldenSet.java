package ar.edu.utn.frc.tup.piv.llm.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GoldenSet(UUID id, long version, String rubricVersion, String language, OffsetDateTime createdAt, List<GoldenSetEntry> entries) {}
