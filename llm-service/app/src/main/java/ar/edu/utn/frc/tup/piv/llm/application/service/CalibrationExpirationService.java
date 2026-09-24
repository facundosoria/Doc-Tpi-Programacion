package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalibrationExpirationService {
  private final JdbcTemplate jdbc;
  private final CalibrationRunRepository runs;

  public CalibrationExpirationService(JdbcTemplate jdbc, CalibrationRunRepository runs) {
    this.jdbc = jdbc;
    this.runs = runs;
  }

  @Transactional
  public void expireByRubric(UUID rubricFamilyId) {
    jdbc.queryForList("""
        select r.id from llm.calibration_runs r
        join llm.active_calibrations a on a.calibration_run_id = r.id
        join llm.rubric_version_v2 v on v.id = r.rubric_version_id
        where v.family_id = ? and r.state = 'PASSED'
        union
        select r.id from llm.calibration_runs r
        join llm.rubric_version_v2 v on v.id = r.rubric_version_id
        where v.family_id = ? and r.stage = 'PLATFORM' and r.state = 'PASSED'
        """, UUID.class, rubricFamilyId, rubricFamilyId)
        .forEach(id -> runs.expire(id, "NEW_RUBRIC_VERSION"));
  }

  @Transactional
  public void expireByGoldenSet(UUID goldenSetFamilyId) {
    jdbc.queryForList("""
        select r.id from llm.calibration_runs r
        join llm.active_calibrations a on a.calibration_run_id = r.id
        join llm.golden_set_versions v on v.id = r.golden_set_version_id
        where v.family_id = ? and r.state = 'PASSED'
        union
        select r.id from llm.calibration_runs r
        join llm.golden_set_versions v on v.id = r.golden_set_version_id
        where v.family_id = ? and r.stage = 'PLATFORM' and r.state = 'PASSED'
        """, UUID.class, goldenSetFamilyId, goldenSetFamilyId)
        .forEach(id -> runs.expire(id, "NEW_GOLDEN_SET_VERSION"));
  }

  @Transactional
  public void expireByTimeLimit() {
    jdbc.queryForList("""
        select r.id from llm.calibration_runs r
        join llm.active_calibrations a on a.calibration_run_id = r.id
        left join llm.course_calibration_profiles cp on cp.course_id = a.course_id
        cross join llm.institutional_calibration_profiles ip
        where r.state = 'PASSED' and r.finished_at is not null
          and (
            (cp.expiration_days is not null and extract(day from now() - r.finished_at) >= cp.expiration_days)
            or (cp.expiration_days is null and ip.expiration_days is not null and extract(day from now() - r.finished_at) >= ip.expiration_days)
          )
        union
        select r.id from llm.calibration_runs r
        cross join llm.institutional_calibration_profiles ip
        where r.stage = 'PLATFORM' and r.state = 'PASSED' and r.finished_at is not null
          and ip.expiration_days is not null and extract(day from now() - r.finished_at) >= ip.expiration_days
        """, UUID.class)
        .forEach(id -> runs.expire(id, "TIME_LIMIT_EXCEEDED"));
  }
}
