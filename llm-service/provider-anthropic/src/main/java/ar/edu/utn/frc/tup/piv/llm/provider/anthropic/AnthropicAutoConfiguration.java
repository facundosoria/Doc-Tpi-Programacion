package ar.edu.utn.frc.tup.piv.llm.provider.anthropic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AnthropicAutoConfiguration {
  @Bean AnthropicProviderAdapter anthropicProviderAdapter(ObjectMapper objectMapper) {
    return new AnthropicProviderAdapter(objectMapper);
  }
}
