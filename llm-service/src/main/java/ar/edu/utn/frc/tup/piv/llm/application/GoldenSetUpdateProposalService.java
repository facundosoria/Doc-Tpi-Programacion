package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetUpdateProposalRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetUpdateProposalRepository.GoldenSetUpdateProposal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoldenSetUpdateProposalService {
  private final GoldenSetUpdateProposalRepository proposals;
  public GoldenSetUpdateProposalService(GoldenSetUpdateProposalRepository proposals) { this.proposals = proposals; }

  /** Called by the platform Golden Set publication workflow after it publishes a new base version. */
  @Transactional
  public int proposeForPublishedBase(UUID baseVersionId) { return proposals.createForPublishedBase(baseVersionId); }

  @Transactional(readOnly = true)
  public List<GoldenSetUpdateProposal> pendingForCourse(UUID courseId) { return proposals.pendingForCourse(courseId); }
}
