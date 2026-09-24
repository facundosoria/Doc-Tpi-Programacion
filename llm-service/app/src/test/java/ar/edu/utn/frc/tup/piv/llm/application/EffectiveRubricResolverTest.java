package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EffectiveRubricResolver;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchor;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchors;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionCustomInput;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EffectiveRubricResolverTest {
  private final RubricVersionRepository rubrics = mock(RubricVersionRepository.class);
  private final EffectiveRubricResolver resolver = new EffectiveRubricResolver(rubrics);

  @Test void resolvesOverlayDimensionsAsTheEffectiveProfile() {
    UUID baseline = UUID.randomUUID(), overlay = UUID.randomUUID();
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(List.of(
        new DimensionCustomInput("algoritmos", "Algoritmos", "Criterio", anchors(), BigDecimal.valueOf(60)),
        new DimensionCustomInput("pruebas", "Pruebas", "Criterio", anchors(), BigDecimal.valueOf(40))));

    var effective = resolver.resolve(baseline, overlay);

    assertThat(effective).hasSize(2);
    assertThat(effective.get(0).key()).isEqualTo("algoritmos");
    assertThat(effective.get(0).origin()).isEqualTo(EffectiveRubricResolver.EffectiveDimension.Origin.OVERLAY);
    assertThat(effective.stream().map(EffectiveRubricResolver.EffectiveDimension::weight).reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(BigDecimal.valueOf(100));
  }

  private Anchors anchors() {
    return new Anchors(new Anchor("bajo", 25, "ej"), new Anchor("medio", 60, "ej"), new Anchor("alto", 90, "ej"));
  }
}