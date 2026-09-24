package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resuelve el perfil efectivo de un desafío. En el Slice 1 el overlay de desafío es el
 * conjunto efectivo completo (N dimensiones que suman 100); el baseline del curso queda
 * como referencia de origen, sin combinarse.
 */
@Component
public class EffectiveRubricResolver {
  private final RubricVersionRepository rubrics;

  public EffectiveRubricResolver(RubricVersionRepository rubrics) {
    this.rubrics = rubrics;
  }

  /**
   * Devuelve el perfil efectivo de un overlay de desafío.
   *
   * @param baselineVersionId ID del baseline del curso (referencia de origen)
   * @param overlayVersionId  ID de la versión del overlay de desafío (conjunto efectivo)
   * @return Lista de dimensiones efectivas del overlay
   */
  public List<EffectiveDimension> resolve(UUID baselineVersionId, UUID overlayVersionId) {
    return rubrics.challengeCustomDimensions(overlayVersionId).stream()
        .map(d -> new EffectiveDimension(
            d.key(),
            d.label(),
            d.criterion(),
            d.anchors(),
            d.weight(),
            EffectiveDimension.Origin.OVERLAY))
        .toList();
  }

  public record EffectiveDimension(
      String key,
      String label,
      String criterion,
      RubricDraftService.Anchors anchors,
      BigDecimal weight,
      Origin origin
  ) {
    public enum Origin {
      OVERLAY
    }
  }
}