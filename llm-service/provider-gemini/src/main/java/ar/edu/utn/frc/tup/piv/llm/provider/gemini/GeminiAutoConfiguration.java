package ar.edu.utn.frc.tup.piv.llm.provider.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class GeminiAutoConfiguration {
  @Bean GeminiProviderAdapter geminiProviderAdapter(ObjectMapper objectMapper) { return new GeminiProviderAdapter(objectMapper); }
}
