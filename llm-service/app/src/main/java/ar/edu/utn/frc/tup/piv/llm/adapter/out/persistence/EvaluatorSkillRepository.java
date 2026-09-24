package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EvaluatorSkillRepository {
  private final JdbcTemplate jdbc;

  public EvaluatorSkillRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<EvaluatorSkill> listCatalog() {
    return jdbc.query("""
        select skill_key, name, description, tool_type, system_instruction, enabled_by_default
        from llm.evaluator_skills
        order by skill_key
        """,
        (rs, row) -> new EvaluatorSkill(
            rs.getString("skill_key"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("tool_type"),
            rs.getString("system_instruction"),
            rs.getBoolean("enabled_by_default")));
  }

  public List<CourseSkillView> findCourseSkills(UUID courseId) {
    return jdbc.query("""
        select s.skill_key, s.name, s.description, s.tool_type, s.system_instruction,
               coalesce(c.is_active, s.enabled_by_default) as is_active
        from llm.evaluator_skills s
        left join llm.course_evaluator_skills c on c.skill_key = s.skill_key and c.course_id = ?
        order by s.skill_key
        """,
        (rs, row) -> new CourseSkillView(
            rs.getString("skill_key"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("tool_type"),
            rs.getString("system_instruction"),
            rs.getBoolean("is_active")),
        courseId);
  }

  public List<EvaluatorSkill> findActiveByCourse(UUID courseId) {
    return jdbc.query("""
        select s.skill_key, s.name, s.description, s.tool_type, s.system_instruction, s.enabled_by_default
        from llm.evaluator_skills s
        left join llm.course_evaluator_skills c on c.skill_key = s.skill_key and c.course_id = ?
        where coalesce(c.is_active, s.enabled_by_default) = true
        order by s.skill_key
        """,
        (rs, row) -> new EvaluatorSkill(
            rs.getString("skill_key"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("tool_type"),
            rs.getString("system_instruction"),
            rs.getBoolean("enabled_by_default")),
        courseId);
  }

  public List<String> findActiveSkillKeysByCourse(UUID courseId) {
    return findActiveByCourse(courseId).stream()
        .map(EvaluatorSkill::skillKey)
        .toList();
  }

  @Transactional
  public void updateCourseSkills(UUID courseId, Collection<String> activeKeys) {
    var catalog = listCatalog();
    Set<String> activeSet = activeKeys != null ? new HashSet<>(activeKeys) : Set.of();
    for (var skill : catalog) {
      boolean active = activeSet.contains(skill.skillKey());
      jdbc.update("""
          insert into llm.course_evaluator_skills (course_id, skill_key, is_active, updated_at)
          values (?, ?, ?, now())
          on conflict (course_id, skill_key) do update set is_active = excluded.is_active, updated_at = now()
          """, courseId, skill.skillKey(), active);
    }
  }

  public boolean allKeysExist(Collection<String> keys) {
    if (keys == null || keys.isEmpty()) return true;
    var catalogKeys = listCatalog().stream()
        .map(EvaluatorSkill::skillKey)
        .collect(Collectors.toSet());
    return catalogKeys.containsAll(keys);
  }

  public record EvaluatorSkill(
      String skillKey,
      String name,
      String description,
      String toolType,
      String systemInstruction,
      boolean enabledByDefault) {}

  public record CourseSkillView(
      String skillKey,
      String name,
      String description,
      String toolType,
      String systemInstruction,
      boolean isActive) {}
}
