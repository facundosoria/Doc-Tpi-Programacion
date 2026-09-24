package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderRegistry;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelDeploymentService;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ModelDeploymentControllerCoverageTest {

  @Test
  void listsAdapterCatalogWithTheirActiveDeployments() {
    var service = mock(ModelDeploymentService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var providers = mock(ProviderRegistry.class);
    var controller = new ModelDeploymentController(service, auth, courses, providers);
    HttpHeaders headers = new HttpHeaders();
    when(auth.require(headers)).thenReturn(new CallerIdentity("admin-service", UUID.randomUUID(), null, null));

    when(service.listEnabledDeployments()).thenReturn(List.of(
        new ModelDeploymentSummary(UUID.randomUUID(), "openai", "gpt-4o-mini", "2024-07-18", "ENABLED"),
        new ModelDeploymentSummary(UUID.randomUUID(), "anthropic", "claude-3-5-sonnet", "2025-01-01", "ENABLED")));
    when(providers.descriptors()).thenReturn(List.of(
        new ProviderDescriptor("openai", "OpenAI", "1.0", List.of(), caps()),
        new ProviderDescriptor("anthropic", "Anthropic", "1.0", List.of(), caps())));

    var page = controller.listAdapters(headers);

    assertThat(page.items()).hasSize(2);
    assertThat(page.items().get(0).deployments()).extracting(ModelDeploymentSummary::modelId).containsExactly("gpt-4o-mini");
    assertThat(page.items().get(1).deployments()).extracting(ModelDeploymentSummary::modelId).containsExactly("claude-3-5-sonnet");
    assertThat(page.items().get(0).capabilities()).isEqualTo(caps());
  }

  private static ProviderCapabilities caps() {
    return new ProviderCapabilities(true, true, true, false, false, false, false, false);
  }
}