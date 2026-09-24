package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.GoldenSetUpdateProposalRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetUpdateProposal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class GoldenSetUpdateProposalService {
  private final GoldenSetUpdateProposalRepository proposals;
  private final JdbcTemplate jdbc;
  private final CalibrationExpirationService expirations;

  public GoldenSetUpdateProposalService(GoldenSetUpdateProposalRepository proposals, JdbcTemplate jdbc, CalibrationExpirationService expirations) { 
    this.proposals = proposals; 
    this.jdbc = jdbc;
    this.expirations = expirations;
  }

  /** Called by the platform Golden Set publication workflow after it publishes a new base version. */
  @Transactional
  public int proposeForPublishedBase(UUID baseVersionId) { 
    UUID familyId = jdbc.queryForObject("select family_id from llm.golden_set_versions where id = ?", UUID.class, baseVersionId);
    expirations.expireByGoldenSet(familyId);
    return proposals.createForPublishedBase(baseVersionId); 
  }

  @Transactional(readOnly = true)
  public List<GoldenSetUpdateProposal> pendingForCourse(UUID courseId) { return proposals.pendingForCourse(courseId); }
}
