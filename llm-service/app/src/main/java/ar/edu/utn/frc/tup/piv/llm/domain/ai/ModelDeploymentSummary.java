package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.util.UUID;

/** Resumen de despliegue de modelo disponible en la plataforma. */
public record ModelDeploymentSummary(
    UUID id,
    String provider,
    String modelId,
    String modelVersion,
    String state) {}
