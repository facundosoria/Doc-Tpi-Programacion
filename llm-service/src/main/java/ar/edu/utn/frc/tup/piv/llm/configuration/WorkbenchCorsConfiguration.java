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
        .allowedOrigins("http://localhost:4200")
        .allowedMethods("GET", "POST")
        .allowedHeaders("Content-Type", "Idempotency-Key", "X-Request-Id");
  }
}
