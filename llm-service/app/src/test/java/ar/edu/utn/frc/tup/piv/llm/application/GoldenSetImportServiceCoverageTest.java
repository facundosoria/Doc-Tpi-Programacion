package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.GoldenSetImportRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.ImportBatch;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.ImportRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldenSetImportServiceCoverageTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test void createsBatchWithAnonymizedRows() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), key = UUID.randomUUID(), actor = UUID.randomUUID();
    ObjectNode row = mapper.createObjectNode().put("author", "Docente").put("studentId", "s-1");
    var batch = new ImportBatch(UUID.randomUUID(), version, "JSON", "DRAFT", 1);
    when(imports.create(eq(course), eq(version), eq("JSON"), eq(key), eq(actor), any())).thenReturn(Optional.of(batch));
    assertThat(service.create(course, version, "JSON", List.of(row), key, actor)).isEqualTo(batch);
    var rows = org.mockito.ArgumentCaptor.forClass(List.class);
    verify(imports).create(eq(course), eq(version), eq("JSON"), eq(key), eq(actor), rows.capture());
    assertThat(((JsonNode) ((List<?>) rows.getValue()).get(0)).has("studentId")).isFalse();
  }

  @Test void acceptsCsvFormat() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), key = UUID.randomUUID(), actor = UUID.randomUUID();
    when(imports.create(any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(new ImportBatch(UUID.randomUUID(), version, "CSV", "DRAFT", 1)));
    var result = service.create(course, version, "CSV", List.of(mapper.createObjectNode()), key, actor);
    assertThat(result.format()).isEqualTo("CSV");
  }

  @Test void rejectsWhenVersionIsNotACourseDraft() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), key = UUID.randomUUID(), actor = UUID.randomUUID();
    when(imports.create(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.create(course, version, "JSON", List.of(mapper.createObjectNode()), key, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("El Golden Set no es un borrador del curso");
  }

  @Test void rejectsNullRows() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), UUID.randomUUID(), "JSON", null, UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("El lote debe tener filas");
  }

  @Test void replacesRowForBatch() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID();
    var payload = mapper.createObjectNode().put("author", "Nuevo");
    when(imports.replaceRow(course, batch, 3, payload)).thenReturn(true);
    service.replaceRow(course, batch, 3, payload);
    verify(imports).replaceRow(course, batch, 3, payload);
  }

  @Test void rejectsRowReplacementWhenRowCannotBeEdited() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    when(imports.replaceRow(any(), any(), any(Integer.class), any())).thenReturn(false);
    assertThatThrownBy(() -> service.replaceRow(UUID.randomUUID(), UUID.randomUUID(), 1, mapper.createObjectNode()))
        .isInstanceOf(IllegalStateException.class).hasMessage("La fila no puede modificarse");
  }

  @Test void rejectsValidationWhenBatchIsTaken() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    when(imports.beginValidation(any(), any())).thenReturn(false);
    assertThatThrownBy(() -> service.validate(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class).hasMessage("El lote no puede validarse");
  }

  @Test void flagsMalformedJsonRows() {
    assertInvalid("not-json", "[\"invalid-json\"]");
  }

  @Test void flagsTranscriptWithoutArrayShape() {
    ObjectNode row = validRow().put("transcript", "texto");
    assertInvalid(row, "[\"transcript\"]");
  }

  @Test void flagsUnknownTranscriptRole() {
    ObjectNode row = validRow();
    ((ObjectNode) ((ArrayNode) row.get("transcript")).get(0)).put("role", "TEACHER");
    assertInvalid(row, "[\"transcript\"]");
  }

  @Test void flagsBlankTranscriptContent() {
    ObjectNode row = validRow();
    ((ObjectNode) ((ArrayNode) row.get("transcript")).get(0)).put("content", "");
    assertInvalid(row, "[\"transcript\"]");
  }

  @Test void flagsMissingChallengeContextObject() {
    ObjectNode row = validRow().put("challengeContext", "contexto");
    assertInvalid(row, "[\"context-or-author\"]");
  }

  @Test void flagsBlankStatement() {
    ObjectNode row = validRow();
    row.putObject("challengeContext").put("statement", "");
    assertInvalid(row, "[\"context-or-author\"]");
  }

  @Test void flagsBlankAuthor() {
    ObjectNode row = validRow().put("author", "");
    assertInvalid(row, "[\"context-or-author\"]");
  }

  @Test void flagsOutOfRangeReferenceScores() {
    ObjectNode row = validRow();
    ((ObjectNode) row.get("referenceScores")).put("AUTONOMY", 150);
    assertInvalid(row, "[\"referenceScores\"]");
  }

  @Test void flagsNonNumericReferenceScores() {
    ObjectNode row = validRow();
    ((ObjectNode) row.get("referenceScores")).put("AUTONOMY", "alto");
    assertInvalid(row, "[\"referenceScores\"]");
  }

  private void assertInvalid(ObjectNode payload, String errors) {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID();
    when(imports.beginValidation(course, batch)).thenReturn(true);
    when(imports.rows(course, batch)).thenReturn(List.of(new ImportRow(1, payload.toString())));
    var result = service.validate(course, batch);
    assertThat(result.state()).isEqualTo("FAILED");
    verify(imports).rowResult(eq(batch), eq(1), eq(false), eq(errors));
    verify(imports).finishValidation(batch, false);
  }

  private void assertInvalid(String payload, String errors) {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID();
    when(imports.beginValidation(course, batch)).thenReturn(true);
    when(imports.rows(course, batch)).thenReturn(List.of(new ImportRow(1, payload)));
    var result = service.validate(course, batch);
    assertThat(result.state()).isEqualTo("FAILED");
    verify(imports).rowResult(eq(batch), eq(1), eq(false), eq(errors));
    verify(imports).finishValidation(batch, false);
  }

  private ObjectNode validRow() {
    ObjectNode row = mapper.createObjectNode();
    ArrayNode transcript = row.putArray("transcript");
    transcript.addObject().put("role", "STUDENT").put("content", "Duda sobre arrays");
    transcript.addObject().put("role", "TUTOR").put("content", "Revisá los índices");
    row.putObject("challengeContext").put("statement", "Manejo de arrays");
    row.put("author", "Docente Gómez");
    ObjectNode scores = row.putObject("referenceScores");
    scores.put("AUTONOMY", 80).put("CLARITY", 85).put("PROGRESSION", 90).put("COMPLIANCE", 100).put("EFFICIENCY", 75);
    return row;
  }
}