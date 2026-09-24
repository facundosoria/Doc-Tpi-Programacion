package ar.edu.utn.frc.tup.piv.llm.provider.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class OpenAiCompatibleAutoConfiguration {
  @Bean
  OpenAiCompatibleProviderAdapter openAiCompatibleProviderAdapter(ObjectMapper objectMapper) {
    return new OpenAiCompatibleProviderAdapter(objectMapper);
  }
}
