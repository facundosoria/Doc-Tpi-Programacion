package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ContextLoadsIT extends AbstractIntegrationIT {
  @Autowired JdbcTemplate jdbc;

  @Test
  void migrationsRunFromScratch() {
    Integer n = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='llm'", Integer.class);
    assertThat(n).isGreaterThan(20);
  }
}
