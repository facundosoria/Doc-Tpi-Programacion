package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persiste `calibration_case_results` — hasta esta historia, ningún código escribía ahí
 * (confirmado: `grep calibration_case_results src/main/java` daba 0 resultados). Una fila por
 * caso del golden set evaluado en un run de calibración. */
@Repository
public class CalibrationCaseResultRepository {
  private final JdbcTemplate jdbc;

  public CalibrationCaseResultRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void record(UUID runId, UUID caseId, String modelScoresJson, BigDecimal humanFinalScore,
      BigDecimal modelFinalScore, String dimensionErrorsJson, BigDecimal finalError) {
    jdbc.update("""
        insert into llm.calibration_case_results
          (calibration_run_id, golden_set_case_id, model_scores, human_final_score, model_final_score, dimension_errors, final_error)
        values (?, ?, cast(? as jsonb), ?, ?, cast(? as jsonb), ?)
        """, runId, caseId, modelScoresJson, humanFinalScore, modelFinalScore, dimensionErrorsJson, finalError);
  }
}
