package com.example.demo.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int BATCH_SIZE = 16;

    private final RestClient restClient;
    private final String model;
    private final boolean isConfigured;

    public EmbeddingService(
            @Value("${spring.ai.openai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}") String baseUrl,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${gemini.embedding.model:text-embedding-004}") String model) {

        this.model = model;
        this.isConfigured = apiKey != null && !apiKey.isBlank();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public boolean isAvailable() {
        return isConfigured;
    }

    /**
     * Calcula los vectores densos para una lista de textos mediante lotes.
     */
    public List<float[]> computeEmbeddings(List<String> texts) {
        if (!isConfigured || texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<float[]> allEmbeddings = new ArrayList<>(texts.size());

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            try {
                EmbeddingRequest req = new EmbeddingRequest(model, batch);
                EmbeddingResponse res = restClient.post()
                        .uri("/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(req)
                        .retrieve()
                        .body(EmbeddingResponse.class);

                if (res != null && res.data != null) {
                    res.data.sort(Comparator.comparingInt(d -> d.index));
                    for (EmbeddingData item : res.data) {
                        float[] vec = new float[item.embedding.length];
                        for (int j = 0; j < item.embedding.length; j++) {
                            vec[j] = (float) item.embedding[j];
                        }
                        allEmbeddings.add(vec);
                    }
                }
            } catch (Exception e) {
                log.warn("Error generando embeddings para lote {}-{}: {}", i, end, e.getMessage());
                // Rellenar con nulls para mantener el orden si falla
                for (int j = i; j < end; j++) {
                    allEmbeddings.add(null);
                }
            }
        }

        return allEmbeddings;
    }

    /**
     * Calcula el vector denso para un único texto.
     */
    public float[] computeEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<float[]> list = computeEmbeddings(List.of(text));
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public record EmbeddingRequest(String model, List<String> input) {}

    public static class EmbeddingResponse {
        public List<EmbeddingData> data;
    }

    public static class EmbeddingData {
        public int index;
        public double[] embedding;
    }
}
