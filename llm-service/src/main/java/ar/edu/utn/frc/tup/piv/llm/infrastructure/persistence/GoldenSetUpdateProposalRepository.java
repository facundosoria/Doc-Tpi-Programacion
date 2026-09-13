package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GoldenSetUpdateProposalRepository {
  private final JdbcTemplate jdbc;
  public GoldenSetUpdateProposalRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /** Creates one pending notification per eligible course, without modifying any course Golden Set. */
  public int createForPublishedBase(UUID baseVersionId) {
    return jdbc.update("""
        insert into llm.golden_set_update_proposals (course_id, course_family_id, base_version_id)
        select distinct course_family.course_id, course_family.id, base.id
        from llm.golden_set_versions base
        join llm.golden_set_families base_family on base_family.id = base.family_id
        join llm.golden_set_versions previous_base on previous_base.family_id = base_family.id and previous_base.id <> base.id
        join llm.golden_set_versions course_version on course_version.based_on_version_id = previous_base.id
        join llm.golden_set_families course_family on course_family.id = course_version.family_id
        where base.id = ? and base.state = 'PUBLISHED' and base_family.scope = 'PLATFORM'
          and course_family.scope = 'COURSE' and course_family.course_id is not null
          and not exists (select 1 from llm.golden_set_versions already_copied where already_copied.family_id = course_family.id and already_copied.based_on_version_id = base.id)
        on conflict (course_id, base_version_id) do nothing
        """, baseVersionId);
  }

  public List<GoldenSetUpdateProposal> pendingForCourse(UUID courseId) {
    return jdbc.query("""
        select p.id, p.course_id, p.course_family_id, p.base_version_id, base.version_no as base_version, count(c.id) as base_case_count, p.detected_at
        from llm.golden_set_update_proposals p
        join llm.golden_set_versions base on base.id = p.base_version_id
        left join llm.golden_set_cases c on c.golden_set_version_id = base.id
        where p.course_id = ? and p.state = 'PENDING'
        group by p.id, p.course_id, p.course_family_id, p.base_version_id, base.version_no, p.detected_at
        order by p.detected_at desc
        """, (rs, row) -> new GoldenSetUpdateProposal(rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
        rs.getObject("course_family_id", UUID.class), rs.getObject("base_version_id", UUID.class), rs.getInt("base_version"),
        rs.getInt("base_case_count"), rs.getObject("detected_at", OffsetDateTime.class)), courseId);
  }

  public record GoldenSetUpdateProposal(UUID id, UUID courseId, UUID courseFamilyId, UUID baseVersionId,
      int baseVersion, int baseCaseCount, OffsetDateTime detectedAt) {}
}
