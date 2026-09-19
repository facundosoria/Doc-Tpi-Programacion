package ar.edu.utn.frc.tup.piv.llm.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WorkbenchCorsConfigurationTest {
  @Test
  @SuppressWarnings("unchecked")
  void registersPrivatePathMappingWithWorkbenchOriginsAndMethods() throws Exception {
    var registry = new CorsRegistry();
    new WorkbenchCorsConfiguration("/api/private").addCorsMappings(registry);
    Method m = CorsRegistry.class.getDeclaredMethod("getCorsConfigurations");
    m.setAccessible(true);
    Map<String, CorsConfiguration> configs = (Map<String, CorsConfiguration>) m.invoke(registry);

    assertThat(configs).containsKey("/api/private/**");
    CorsConfiguration cfg = configs.get("/api/private/**");
    assertThat(cfg.getAllowedOrigins()).contains("http://localhost:4200");
    assertThat(cfg.getAllowedMethods()).contains("DELETE", "PATCH", "GET", "POST", "OPTIONS");
    assertThat(cfg.getAllowedHeaders()).contains("Idempotency-Key", "traceparent");
  }
}
