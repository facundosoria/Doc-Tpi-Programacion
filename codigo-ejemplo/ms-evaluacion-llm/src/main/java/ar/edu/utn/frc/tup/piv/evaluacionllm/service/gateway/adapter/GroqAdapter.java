package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GroqAdapter {

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public GroqAdapter(
            @Value("${llm.gateway.groq.base-url:https://api.groq.com/openai}") String baseUrl,
            @Value("${llm.gateway.groq.api-key:mock}") String apiKey,
            @Value("${llm.gateway.groq.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String getModelName() {
        return model;
    }

    public String generar(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("mock") || apiKey.equals("mock-key")) {
            return "Respuesta simulada en entorno de desarrollo sin API key configurada.";
        }

        var payload = Map.of(
                "model",
                model,
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""),
                        Map.of("role", "user", "content", userPrompt != null ? userPrompt : "")),
                "temperature",
                0.6);

        try {
            ChatCompletionResponse response = restClient
                    .post()
                    .uri("v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response != null
                    && response.getChoices() != null
                    && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            }
            return "No se obtuvo contenido del modelo.";
        } catch (Exception e) {
            return "Modo degradado: Error de comunicacion con Groq (" + e.getMessage() + ")";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCompletionResponse {
        @JsonProperty("choices")
        private List<Choice> choices;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            @JsonProperty("message")
            private Message message;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            @JsonProperty("role")
            private String role;

            @JsonProperty("content")
            private String content;
        }
    }
}
