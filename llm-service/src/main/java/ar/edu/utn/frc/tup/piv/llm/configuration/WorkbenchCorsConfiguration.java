package ar.edu.utn.frc.tup.piv.llm.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("workbench")
public class WorkbenchCorsConfiguration implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/llm/**")
        .allowedOrigins("http://localhost:4200", "http://192.168.0.250:4200", "http://100.68.49.115:4200")
        // Angular's proxy preserves Origin when the workbench is opened from a LAN device.
        // DELETE is required for the logical draft deletion endpoint.
        .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", "Idempotency-Key", "X-Request-Id", "traceparent");
  }
}
