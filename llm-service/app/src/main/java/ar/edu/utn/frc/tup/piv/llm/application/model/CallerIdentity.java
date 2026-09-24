package ar.edu.utn.frc.tup.piv.llm.application.model;

import java.util.UUID;

public record CallerIdentity(String serviceId, UUID delegatedUserId, String requestId, String traceparent) {}
