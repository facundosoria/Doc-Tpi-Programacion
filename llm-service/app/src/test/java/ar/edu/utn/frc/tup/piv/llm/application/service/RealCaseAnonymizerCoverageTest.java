package ar.edu.utn.frc.tup.piv.llm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealCaseAnonymizerCoverageTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test void sanitizesTextNodesInsideArraysAndPreservesScalars() {
    var array = mapper.createArrayNode();
    array.add("Chau juan@mail.com 550e8400-e29b-41d4-a716-446655440000");
    array.add(42);
    array.add(true);
    array.addNull();

    var result = RealCaseAnonymizer.anonymize(array);

    assertThat(result.get(0).asText()).contains("[REDACTED_EMAIL]").contains("[REDACTED_ID]");
    assertThat(result.get(1).asInt()).isEqualTo(42);
    assertThat(result.get(2).asBoolean()).isTrue();
    assertThat(result.get(3).isNull()).isTrue();
  }

  @Test void removesForbiddenKeysCaseInsensitivelyInsideNestedObjects() {
    ObjectNode nested = mapper.createObjectNode();
    nested.put("User_Id", "u-1");
    nested.put("EMAIL", "a@b.com");
    nested.put("displayName", "ok");
    ObjectNode root = mapper.createObjectNode();
    root.set("meta", nested);

    var result = RealCaseAnonymizer.anonymize(root);

    assertThat(result.path("meta").has("User_Id")).isFalse();
    assertThat(result.path("meta").has("EMAIL")).isFalse();
    assertThat(result.path("meta").path("displayName").asText()).isEqualTo("ok");
  }
}