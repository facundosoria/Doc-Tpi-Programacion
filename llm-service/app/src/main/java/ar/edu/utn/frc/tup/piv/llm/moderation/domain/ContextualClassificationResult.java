package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.util.Map;

/**
 * Resultado inmutable de la clasificación contextual externa (OpenAI / Groq / Perspective).
 * Modela el veredicto del modelo de lenguaje o API de toxicidad externa.
 */
public record ContextualClassificationResult(
        boolean flagged,
        String category,
        double score,
        long latencyMs,
        Map<String, Double> categoryScores
) {
    public ContextualClassificationResult {
        category = category != null ? category : ModerationReasonCode.CLEAN;
        score = Math.max(0.0, Math.min(1.0, score));
        latencyMs = Math.max(0, latencyMs);
        categoryScores = categoryScores != null ? Map.copyOf(categoryScores) : Map.of();
    }

    public static ContextualClassificationResult allow(long latencyMs) {
        return new ContextualClassificationResult(false, ModerationReasonCode.CLEAN, 0.0, latencyMs, Map.of());
    }

    public static ContextualClassificationResult block(String category, double score, long latencyMs) {
        return new ContextualClassificationResult(true, category, score, latencyMs, Map.of());
    }

    public static ContextualClassificationResult block(String category, double score, long latencyMs, Map<String, Double> categoryScores) {
        return new ContextualClassificationResult(true, category, score, latencyMs, categoryScores);
    }
}
