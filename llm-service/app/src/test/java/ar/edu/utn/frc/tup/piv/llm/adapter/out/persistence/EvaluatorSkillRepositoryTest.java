package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.CourseSkillView;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.EvaluatorSkill;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class EvaluatorSkillRepositoryTest {

  private JdbcTemplate jdbc;
  private EvaluatorSkillRepository repository;

  @BeforeEach
  void setUp() {
    jdbc = mock(JdbcTemplate.class);
    repository = new EvaluatorSkillRepository(jdbc);
  }

  @Test
  void listCatalog_returnsSkillsAndMapsRowsCorrectly() throws SQLException {
    EvaluatorSkill expected = new EvaluatorSkill(
        "rubric-alignment",
        "Alineación con Rúbrica",
        "Verifica criterios",
        "SYSTEM_PROMPT",
        "Instrucción de sistema",
        true);

    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(expected));

    List<EvaluatorSkill> result = repository.listCatalog();

    assertThat(result).containsExactly(expected);

    // Verify row mapper
    ArgumentCaptor<RowMapper<EvaluatorSkill>> captor = ArgumentCaptor.forClass(RowMapper.class);
    verify(jdbc).query(anyString(), captor.capture());

    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("skill_key")).thenReturn("rubric-alignment");
    when(rs.getString("name")).thenReturn("Alineación con Rúbrica");
    when(rs.getString("description")).thenReturn("Verifica criterios");
    when(rs.getString("tool_type")).thenReturn("SYSTEM_PROMPT");
    when(rs.getString("system_instruction")).thenReturn("Instrucción de sistema");
    when(rs.getBoolean("enabled_by_default")).thenReturn(true);

    EvaluatorSkill mapped = captor.getValue().mapRow(rs, 1);
    assertThat(mapped).isEqualTo(expected);
  }

  @Test
  void findCourseSkills_returnsViewWithCourseActiveState() throws SQLException {
    UUID courseId = UUID.randomUUID();
    CourseSkillView view = new CourseSkillView(
        "rag-context-check",
        "RAG Check",
        "Contexto",
        "SYSTEM_PROMPT",
        "Prompt",
        false);

    when(jdbc.query(anyString(), any(RowMapper.class), eq(courseId))).thenReturn(List.of(view));

    List<CourseSkillView> result = repository.findCourseSkills(courseId);

    assertThat(result).containsExactly(view);

    ArgumentCaptor<RowMapper<CourseSkillView>> captor = ArgumentCaptor.forClass(RowMapper.class);
    verify(jdbc).query(anyString(), captor.capture(), eq(courseId));

    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("skill_key")).thenReturn("rag-context-check");
    when(rs.getString("name")).thenReturn("RAG Check");
    when(rs.getString("description")).thenReturn("Contexto");
    when(rs.getString("tool_type")).thenReturn("SYSTEM_PROMPT");
    when(rs.getString("system_instruction")).thenReturn("Prompt");
    when(rs.getBoolean("is_active")).thenReturn(false);

    CourseSkillView mapped = captor.getValue().mapRow(rs, 1);
    assertThat(mapped).isEqualTo(view);
  }

  @Test
  void findActiveByCourse_and_findActiveSkillKeysByCourse() {
    UUID courseId = UUID.randomUUID();
    EvaluatorSkill s1 = new EvaluatorSkill("skill-1", "Skill 1", "Desc", "TOOL", "Inst", true);
    EvaluatorSkill s2 = new EvaluatorSkill("skill-2", "Skill 2", "Desc", "TOOL", "Inst", true);

    when(jdbc.query(anyString(), any(RowMapper.class), eq(courseId))).thenReturn(List.of(s1, s2));

    List<EvaluatorSkill> skills = repository.findActiveByCourse(courseId);
    assertThat(skills).containsExactly(s1, s2);

    List<String> keys = repository.findActiveSkillKeysByCourse(courseId);
    assertThat(keys).containsExactly("skill-1", "skill-2");
  }

  @Test
  void updateCourseSkills_updatesStateForEachCatalogSkill() {
    UUID courseId = UUID.randomUUID();
    EvaluatorSkill s1 = new EvaluatorSkill("skill-1", "Skill 1", "Desc", "TOOL", "Inst", true);
    EvaluatorSkill s2 = new EvaluatorSkill("skill-2", "Skill 2", "Desc", "TOOL", "Inst", true);

    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(s1, s2));

    repository.updateCourseSkills(courseId, Set.of("skill-1"));

    // s1 should be active (true), s2 should be inactive (false)
    verify(jdbc).update(anyString(), eq(courseId), eq("skill-1"), eq(true));
    verify(jdbc).update(anyString(), eq(courseId), eq("skill-2"), eq(false));
  }

  @Test
  void findActiveByCourse_mapsRowCorrectly() throws SQLException {
    UUID courseId = UUID.randomUUID();
    EvaluatorSkill s1 = new EvaluatorSkill("skill-1", "Skill 1", "Desc", "TOOL", "Inst", true);
    when(jdbc.query(anyString(), any(RowMapper.class), eq(courseId))).thenReturn(List.of(s1));

    repository.findActiveByCourse(courseId);

    ArgumentCaptor<RowMapper<EvaluatorSkill>> captor = ArgumentCaptor.forClass(RowMapper.class);
    verify(jdbc).query(anyString(), captor.capture(), eq(courseId));

    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("skill_key")).thenReturn("skill-1");
    when(rs.getString("name")).thenReturn("Skill 1");
    when(rs.getString("description")).thenReturn("Desc");
    when(rs.getString("tool_type")).thenReturn("TOOL");
    when(rs.getString("system_instruction")).thenReturn("Inst");
    when(rs.getBoolean("enabled_by_default")).thenReturn(true);

    EvaluatorSkill mapped = captor.getValue().mapRow(rs, 1);
    assertThat(mapped).isEqualTo(s1);
  }

  @Test
  void updateCourseSkills_handlesNullActiveKeys() {
    UUID courseId = UUID.randomUUID();
    EvaluatorSkill s1 = new EvaluatorSkill("skill-1", "Skill 1", "Desc", "TOOL", "Inst", true);
    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(s1));

    repository.updateCourseSkills(courseId, null);

    verify(jdbc).update(anyString(), eq(courseId), eq("skill-1"), eq(false));
  }

  @Test
  void allKeysExist_validatesAgainstCatalog() {
    EvaluatorSkill s1 = new EvaluatorSkill("k1", "N1", "D", "T", "I", true);
    EvaluatorSkill s2 = new EvaluatorSkill("k2", "N2", "D", "T", "I", false);

    when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(s1, s2));

    assertThat(repository.allKeysExist(null)).isTrue();
    assertThat(repository.allKeysExist(List.of())).isTrue();
    assertThat(repository.allKeysExist(List.of("k1"))).isTrue();
    assertThat(repository.allKeysExist(List.of("k1", "k2"))).isTrue();
    assertThat(repository.allKeysExist(List.of("k1", "k_unknown"))).isFalse();
  }
}
