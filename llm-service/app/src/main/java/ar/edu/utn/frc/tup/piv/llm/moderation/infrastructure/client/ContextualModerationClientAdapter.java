package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.client;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ContextualClassificationResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ContextualModerationPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptador de cliente HTTP para el clasificador contextual externo de moderación (T1 de LLM-S13-H01):
 * - Se conecta vía RestClient al endpoint compatible con OpenAI (omni-moderation-latest) o Groq (Llama Guard 3).
 * - Aplica timeout estricto de conexión (200 ms) y transporte (máximo 300 ms).
 * - Mapea categorías de toxicidad y scores (0.0 a 1.0).
 * - Permite inyección de RestClient personalizado para pruebas con WireMock.
 */
@Component
public class ContextualModerationClientAdapter implements ContextualModerationPort {

    private static final Logger log = LoggerFactory.getLogger(ContextualModerationClientAdapter.class);

    private final RestClient restClient;
    private final String model;

    @org.springframework.beans.factory.annotation.Autowired
    public ContextualModerationClientAdapter(
            @Value("${llm.moderation.contextual.base-url:https://api.openai.com}") String baseUrl,
            @Value("${llm.moderation.contextual.api-key:}") String apiKey,
            @Value("${llm.moderation.contextual.model:omni-moderation-latest}") String model,
            @Value("${llm.moderation.contextual.timeout-ms:300}") int timeoutMs) {
        this.model = (model != null && !model.isBlank()) ? model : "omni-moderation-latest";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(200, timeoutMs));
        factory.setReadTimeout(timeoutMs);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        this.restClient = builder.build();
    }

    public ContextualModerationClientAdapter(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = (model != null && !model.isBlank()) ? model : "omni-moderation-latest";
    }

    @Override
    public ContextualClassificationResult classify(String text) {
        long start = System.currentTimeMillis();
        if (text == null || text.isBlank()) {
            return ContextualClassificationResult.allow(0);
        }

        ModerationApiRequest request = new ModerationApiRequest(text, model);

        try {
            ModerationApiResponse response = restClient.post()
                    .uri("/v1/moderations")
                    .body(request)
                    .retrieve()
                    .body(ModerationApiResponse.class);

            long latency = Math.max(1, System.currentTimeMillis() - start);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                log.warn("Respuesta vacía o nula del clasificador contextual en {} ms", latency);
                return ContextualClassificationResult.allow(latency);
            }

            ModerationResultItem item = response.results().get(0);
            boolean flagged = Boolean.TRUE.equals(item.flagged());

            if (!flagged) {
                return ContextualClassificationResult.allow(latency);
            }

            // Identificar categoría con mayor score
            String topCategory = ModerationReasonCode.CONTEXTUAL_BLOCK;
            double topScore = 0.0;
            Map<String, Double> scores = item.categoryScores() != null ? item.categoryScores() : Map.of();

            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > topScore) {
                    topScore = entry.getValue();
                    topCategory = entry.getKey().toUpperCase();
                }
            }

            log.info("Mensaje clasificado como infractor por IA contextual: category='{}', score={}, latency={} ms",
                    topCategory, topScore, latency);

            return ContextualClassificationResult.block(topCategory, topScore, latency, scores);

        } catch (Exception e) {
            long latency = Math.max(1, System.currentTimeMillis() - start);
            log.error("Falla al invocar al clasificador contextual externo tras {} ms: {}", latency, e.getMessage());
            throw e;
        }
    }

    public String getModel() {
        return model;
    }

    public record ModerationApiRequest(
            @JsonProperty("input") String input,
            @JsonProperty("model") String model
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModerationApiResponse(
            @JsonProperty("id") String id,
            @JsonProperty("model") String model,
            @JsonProperty("results") List<ModerationResultItem> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModerationResultItem(
            @JsonProperty("flagged") Boolean flagged,
            @JsonProperty("categories") Map<String, Boolean> categories,
            @JsonProperty("category_scores") Map<String, Double> categoryScores
    ) {}
}
