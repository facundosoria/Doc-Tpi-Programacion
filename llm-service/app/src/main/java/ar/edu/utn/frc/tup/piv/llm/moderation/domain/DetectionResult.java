package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.util.Objects;

/**
 * Value Object inmutable que modela el resultado de la evaluación de un detector individual.
 * Cumple con CA5 y CA_negativo_1 al no almacenar expresiones regulares, palabras clave encontradas
 * ni detalles de reglas internas que pudieran filtrarse hacia el exterior.
 */
public final class DetectionResult {

    private final boolean passed;
    private final String reasonCode;
    private final String detectorName;
    private final double score;
    private final long latencyMs;

    public DetectionResult(boolean passed, String reasonCode, String detectorName, double score, long latencyMs) {
        this.passed = passed;
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode no puede ser nulo");
        this.detectorName = Objects.requireNonNull(detectorName, "detectorName no puede ser nulo");
        this.score = Math.max(0.0, Math.min(1.0, score));
        this.latencyMs = Math.max(0, latencyMs);
    }

    public static DetectionResult pass(String detectorName, long latencyMs) {
        return new DetectionResult(true, ModerationReasonCode.CLEAN, detectorName, 0.0, latencyMs);
    }

    public static DetectionResult fail(String detectorName, String reasonCode, double score, long latencyMs) {
        return new DetectionResult(false, reasonCode, detectorName, score, latencyMs);
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isFailed() {
        return !passed;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getDetectorName() {
        return detectorName;
    }

    public double getScore() {
        return score;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetectionResult that)) return false;
        return passed == that.passed
                && Double.compare(score, that.score) == 0
                && latencyMs == that.latencyMs
                && Objects.equals(reasonCode, that.reasonCode)
                && Objects.equals(detectorName, that.detectorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passed, reasonCode, detectorName, score, latencyMs);
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "passed=" + passed +
                ", reasonCode='" + reasonCode + '\'' +
                ", detectorName='" + detectorName + '\'' +
                ", score=" + score +
                ", latencyMs=" + latencyMs +
                '}';
    }
}
