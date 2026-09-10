package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetImportRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetImportRepository.ImportBatch;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetImportRepository.ImportRow;
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

class GoldenSetImportServiceTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test void rejectsInvalidFormat() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), key = UUID.randomUUID(), actor = UUID.randomUUID();

    assertThatThrownBy(() -> service.create(course, version, "XML", List.of(mapper.createObjectNode()), key, actor))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Formato de importación inválido");
  }

  @Test void rejectsEmptyRows() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), key = UUID.randomUUID(), actor = UUID.randomUUID();

    assertThatThrownBy(() -> service.create(course, version, "JSON", List.of(), key, actor))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("El lote debe tener filas");
  }

  @Test void validatesBatchAndSetsReadyWhenAllRowsAreValid() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID();

    ObjectNode row = mapper.createObjectNode();
    ArrayNode transcript = row.putArray("transcript");
    transcript.addObject().put("role", "STUDENT").put("content", "Duda sobre arrays");
    transcript.addObject().put("role", "TUTOR").put("content", "Revisá los índices");
    row.putObject("challengeContext").put("statement", "Manejo de arrays");
    row.put("author", "Docente Gómez");
    ObjectNode scores = row.putObject("referenceScores");
    scores.put("AUTONOMY", 80).put("CLARITY", 85).put("PROGRESSION", 90).put("COMPLIANCE", 100).put("EFFICIENCY", 75);

    when(imports.beginValidation(course, batch)).thenReturn(true);
    when(imports.rows(course, batch)).thenReturn(List.of(new ImportRow(1, row.toString())));

    var result = service.validate(course, batch);

    assertThat(result.state()).isEqualTo("READY");
    verify(imports).rowResult(batch, 1, true, "[]");
    verify(imports).finishValidation(batch, true);
  }

  @Test void validatesBatchAndSetsFailedWhenRowHasErrors() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID();

    ObjectNode row = mapper.createObjectNode();
    row.putArray("transcript"); // vacío -> error
    row.putObject("challengeContext").put("statement", "Manejo de arrays");
    row.put("author", "Docente Gómez");

    when(imports.beginValidation(course, batch)).thenReturn(true);
    when(imports.rows(course, batch)).thenReturn(List.of(new ImportRow(1, row.toString())));

    var result = service.validate(course, batch);

    assertThat(result.state()).isEqualTo("FAILED");
    verify(imports).rowResult(eq(batch), eq(1), eq(false), any());
    verify(imports).finishValidation(batch, false);
  }

  @Test void commitsReadyBatchAtomically() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID(), version = UUID.randomUUID();

    when(imports.claimReady(course, batch)).thenReturn(Optional.of(version));

    service.commit(course, batch);

    verify(imports).insertCases(batch, version);
    verify(imports).complete(batch);
  }

  @Test void refusesCommitWhenBatchIsNotReady() {
    var imports = mock(GoldenSetImportRepository.class);
    var service = new GoldenSetImportService(imports, mapper);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID();

    when(imports.claimReady(course, batch)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.commit(course, batch))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("El lote debe estar READY");

    verify(imports, org.mockito.Mockito.never()).insertCases(any(), any());
    verify(imports, org.mockito.Mockito.never()).complete(any());
  }
}
