package ar.edu.utn.frc.tup.piv.llm.shadow.infrastructure;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowSampleSource;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import java.util.List;
import org.springframework.stereotype.Component;

/** Reproduce los casos de una versión del golden set: traen nota humana, así que el shadow también
 * mide qué tan cerca de la nota humana queda cada rúbrica. */
@Component
public class GoldenSetShadowSource implements ShadowSampleSource {
  private final CourseGoldenSetRepository goldenSets;

  public GoldenSetShadowSource(CourseGoldenSetRepository goldenSets) {
    this.goldenSets = goldenSets;
  }

  @Override
  public ShadowRun.Source source() {
    return ShadowRun.Source.GOLDEN_SET;
  }

  @Override
  public List<Sample> load(ShadowRun run) {
    return goldenSets.casesOf(run.goldenSetVersionId()).stream().limit(run.sampleSize())
        .map(c -> new Sample(c.id().toString(), c.transcript(), c.challengeContext(), c.referenceScores()))
        .toList();
  }
}
