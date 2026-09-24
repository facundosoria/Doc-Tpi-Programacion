package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ConversationRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integración real (Postgres + Flyway) del repositorio JDBC de conversaciones (EP-05/EP-09). */
class ConversationRepositoryIT extends AbstractIntegrationIT {
  @Autowired ConversationRepository repository;

  private Conversation conversation(UUID cohort, UUID learner, String titulo, OffsetDateTime createdAt) {
    return new Conversation(UUID.randomUUID(), cohort, learner, null, titulo,
        Conversation.ESTADO_ABIERTA, createdAt);
  }

  @Test
  void savesAndReadsBackAConversation() {
    Conversation conversation = conversation(UUID.randomUUID(), UUID.randomUUID(), "Duda de Docker", OffsetDateTime.now());

    repository.save(conversation);

    var found = repository.findById(conversation.id());
    assertThat(found).isPresent();
    assertThat(found.get().titulo()).isEqualTo("Duda de Docker");
    assertThat(found.get().estado()).isEqualTo(Conversation.ESTADO_ABIERTA);
    assertThat(found.get().courseCohortId()).isEqualTo(conversation.courseCohortId());
    assertThat(found.get().learnerId()).isEqualTo(conversation.learnerId());
  }

  @Test
  void findByIdIsEmptyForAnUnknownConversation() {
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void findFiltersByLearner() {
    UUID cohort = UUID.randomUUID();
    UUID learner = UUID.randomUUID();
    UUID otherLearner = UUID.randomUUID();
    repository.save(conversation(cohort, learner, "L1-a", OffsetDateTime.now()));
    repository.save(conversation(cohort, learner, "L1-b", OffsetDateTime.now()));
    repository.save(conversation(cohort, otherLearner, "L2", OffsetDateTime.now()));

    assertThat(repository.find(learner, null)).hasSize(2);
    assertThat(repository.find(otherLearner, null)).hasSize(1);
  }

  @Test
  void findFiltersByCourse() {
    UUID learner = UUID.randomUUID();
    UUID cohortA = UUID.randomUUID();
    UUID cohortB = UUID.randomUUID();
    repository.save(conversation(cohortA, learner, "A", OffsetDateTime.now()));
    repository.save(conversation(cohortB, learner, "B", OffsetDateTime.now()));

    assertThat(repository.find(null, cohortA)).extracting(Conversation::titulo).containsExactly("A");
    assertThat(repository.find(null, cohortB)).extracting(Conversation::titulo).containsExactly("B");
  }

  @Test
  void findFiltersByLearnerAndCourseAtTheSameTime() {
    UUID learner = UUID.randomUUID();
    UUID cohortA = UUID.randomUUID();
    UUID cohortB = UUID.randomUUID();
    repository.save(conversation(cohortA, learner, "A", OffsetDateTime.now()));
    repository.save(conversation(cohortB, learner, "B", OffsetDateTime.now()));

    assertThat(repository.find(learner, cohortA)).extracting(Conversation::titulo).containsExactly("A");
  }

  @Test
  void findWithoutFiltersReturnsTheStoredConversations() {
    Conversation conversation = conversation(UUID.randomUUID(), UUID.randomUUID(), "Sin filtro", OffsetDateTime.now());
    repository.save(conversation);

    assertThat(repository.find(null, null)).extracting(Conversation::id).contains(conversation.id());
  }

  @Test
  void findOrdersByCreatedAtDescending() {
    UUID cohort = UUID.randomUUID();
    UUID learner = UUID.randomUUID();
    OffsetDateTime base = OffsetDateTime.now();
    repository.save(conversation(cohort, learner, "Vieja", base.minusMinutes(10)));
    repository.save(conversation(cohort, learner, "Nueva", base));

    assertThat(repository.find(learner, cohort)).extracting(Conversation::titulo).containsExactly("Nueva", "Vieja");
  }
}
