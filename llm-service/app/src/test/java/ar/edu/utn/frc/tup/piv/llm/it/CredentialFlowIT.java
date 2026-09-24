package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CredentialFlowIT extends AbstractIntegrationIT {
  static final String ADMIN = "/api/llm/admin";
  @Autowired ProviderCredentialRepository repository;

  private String credential(UUID c, String providerKey) throws Exception {
    String request = "{\"providerKey\":\"" + providerKey + "\",\"displayName\":\"" + providerKey
        + "\",\"secrets\":{\"apiKey\":\"sk-test-1234567890\"}}";
    return body(mvc.perform(asTeacher(post(ADMIN + "/provider-credentials"), c).content(request))
        .andExpect(status().isOk())).path("id").asText();
  }

  @Test
  void credentialToActiveEvaluatorModel() throws Exception {
    UUID c = UUID.randomUUID();
    String credentialId = credential(c, "anthropic");
    var listed = body(mvc.perform(asTeacher(get(ADMIN + "/provider-credentials"), c)).andExpect(status().isOk()));
    assertThat(listed.path("items").toString()).contains(credentialId).doesNotContain("sk-test-1234567890");

    var deployment = body(mvc.perform(asTeacher(post(ADMIN + "/provider-credentials/" + credentialId + "/deployments"), c)
        .content("{\"modelId\":\"claude-x\",\"slot\":2}")).andExpect(status().isOk()));
    String id = deployment.path("id").asText();
    assertThat(deployment.path("state").asText()).isEqualTo("CANDIDATE");
    mvc.perform(asTeacher(get(ADMIN + "/evaluator-models"), c)).andExpect(status().isOk());

    // Sin haber respondido en el chat de prueba, el candidato no se puede activar.
    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + id + "/activate"), c)).andExpect(status().isConflict());
    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + id + "/select-for-calibration"), c)).andExpect(status().isOk());

    repository.markChatVerified(UUID.fromString(id));
    repository.recordUsage(UUID.fromString(id), "ADMIN_TEST", 10, 20);
    repository.recordUsage(UUID.fromString(id), "EVALUATION", 1, 2);
    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + id + "/activate"), c)).andExpect(status().isOk());

    assertThat(body(mvc.perform(asTeacher(get(ADMIN + "/evaluator-models/active"), c)).andExpect(status().isOk())).path("id").asText()).isEqualTo(id);
    mvc.perform(asTeacher(get(ADMIN + "/evaluator-models/calibration-target"), c)).andExpect(status().isOk());
    var usage = body(mvc.perform(asTeacher(get(ADMIN + "/evaluator-models/" + id + "/usage"), c)).andExpect(status().isOk()));
    assertThat(usage.path("inputTokens").asLong()).isEqualTo(11);
    assertThat(usage.path("adminTests").asLong()).isEqualTo(1);
    assertThat(usage.path("evaluations").asLong()).isEqualTo(1);

    mvc.perform(asTeacher(get(ADMIN + "/model-adapters"), c)).andExpect(status().isOk());
    mvc.perform(asTeacher(get("/api/llm/courses/" + c + "/model-deployments"), c)).andExpect(status().isOk());
    mvc.perform(asTeacher(delete(ADMIN + "/evaluator-models/" + id), c)).andExpect(status().isOk());
    mvc.perform(asTeacher(delete(ADMIN + "/provider-credentials/" + credentialId), c)).andExpect(status().isOk());
    // Una credencial deshabilitada no se puede volver a deshabilitar ni usar.
    mvc.perform(asTeacher(delete(ADMIN + "/provider-credentials/" + credentialId), c)).andExpect(status().isConflict());
    mvc.perform(asTeacher(post(ADMIN + "/provider-credentials/" + credentialId + "/deployments"), c)
        .content("{\"modelId\":\"otro\"}")).andExpect(status().isConflict());
  }

  @Test
  void rejectsInvalidCredentialsAndUnknownModels() throws Exception {
    UUID c = UUID.randomUUID();
    // Sin apiKey el adaptador del SPI la rechaza (INVALID_CREDENTIAL).
    mvc.perform(asTeacher(post(ADMIN + "/provider-credentials"), c)
        .content("{\"providerKey\":\"gemini\",\"displayName\":\"g\",\"secrets\":{}}")).andExpect(status().isUnprocessableEntity());
    // Un proveedor que no está instalado tampoco se puede dar de alta.
    mvc.perform(asTeacher(post(ADMIN + "/provider-credentials"), c)
        .content("{\"providerKey\":\"desconocido\",\"displayName\":\"x\",\"secrets\":{\"apiKey\":\"sk-test-1234567890\"}}"))
        .andExpect(status().isUnprocessableEntity());
    // baseUrl http (no loopback) es inválida para el adaptador OpenAI compatible.
    mvc.perform(asTeacher(post(ADMIN + "/provider-credentials"), c)
        .content("{\"providerKey\":\"openai-compatible\",\"displayName\":\"o\",\"configuration\":{\"baseUrl\":\"http://inseguro\"},\"secrets\":{\"apiKey\":\"sk-test-1234567890\"}}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(asTeacher(post(ADMIN + "/provider-credentials"), c).content("{\"displayName\":\"sin proveedor\"}")).andExpect(status().isBadRequest());
    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + UUID.randomUUID() + "/chat"), c).content("{\"message\":\"hola\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + UUID.randomUUID() + "/activate"), c)).andExpect(status().isConflict());
    mvc.perform(asTeacher(get(ADMIN + "/evaluator-models/" + UUID.randomUUID() + "/usage"), c)).andExpect(status().is2xxSuccessful());
    mvc.perform(get(ADMIN + "/provider-credentials")).andExpect(status().isUnauthorized());
  }
}
