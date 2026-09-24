package ar.edu.utn.frc.tup.piv.llm.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetView;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.application.service.CourseGoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricPublicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkbenchCalibrationSeedTest {

  private static final UUID COURSE = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TEMPLATE = UUID.fromString("10000000-0000-0000-0000-000000000002");

  @Test
  void seedsRubricsAndGoldenSetsOnlyWhenMissing() {
    var rubrics = mock(RubricDraftService.class);
    var publication = mock(RubricPublicationService.class);
    var goldenSets = mock(CourseGoldenSetService.class);
    var seed = new WorkbenchCalibrationSeed(rubrics, publication, goldenSets, new ObjectMapper());
    when(rubrics.list(COURSE)).thenReturn(List.of());
    when(rubrics.createFromTemplate(eq(COURSE), eq(TEMPLATE), anyString(), any()))
        .thenAnswer(inv -> new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1,
            inv.getArgument(2), "DRAFT", 0L, TEMPLATE, List.of()));
    when(goldenSets.list(COURSE)).thenReturn(List.of());
    when(goldenSets.createDraft(eq(COURSE), anyString(), any()))
        .thenReturn(new CourseGoldenSetVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "DRAFT", null));

    seed.run(null);

    verify(rubrics, times(3)).createFromTemplate(eq(COURSE), eq(TEMPLATE),
        argThat(name -> List.of("Exigente", "Normal", "Permisiva").contains(name)), any());
    verify(publication, times(3)).publish(eq(COURSE), any(), any());
    verify(goldenSets, times(3)).createDraft(eq(COURSE), anyString(), any());
    verify(goldenSets, times(15)).addCase(eq(COURSE), any(), any(), any());
    verify(goldenSets, times(3)).publish(eq(COURSE), any(), any());
  }

  @Test
  void skipsEverythingWhenDataAlreadyExists() {
    var rubrics = mock(RubricDraftService.class);
    var publication = mock(RubricPublicationService.class);
    var goldenSets = mock(CourseGoldenSetService.class);
    var seed = new WorkbenchCalibrationSeed(rubrics, publication, goldenSets, new ObjectMapper());
    when(rubrics.list(COURSE)).thenReturn(List.of(
        versionOf("Exigente"), versionOf("Normal"), versionOf("Permisiva")));
    when(goldenSets.list(COURSE)).thenReturn(List.of(
        viewOf("Golden Set · Predominio excelente"),
        viewOf("Golden Set · Distribución equilibrada"),
        viewOf("Golden Set · Predominio regular")));

    seed.run(null);

    verify(rubrics, never()).createFromTemplate(any(), any(), anyString(), any());
    verify(publication, never()).publish(any(), any(), any());
    verify(goldenSets, never()).createDraft(any(), anyString(), any());
    verify(goldenSets, never()).addCase(any(), any(), any(), any());
    verify(goldenSets, never()).publish(any(), any(), any());
  }

  private static RubricVersion versionOf(String name) {
    return new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, name, "PUBLISHED", 1L, TEMPLATE, List.of());
  }

  private static CourseGoldenSetView viewOf(String name) {
    return new CourseGoldenSetView(UUID.randomUUID(), UUID.randomUUID(), name, 1, "PUBLISHED", null, List.of());
  }
}