package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integración real (Postgres + Flyway) del repositorio JDBC de fuentes RAG (EP-09). */
class RagDocumentRepositoryIT extends AbstractIntegrationIT {
  @Autowired RagDocumentRepository repository;

  private RagDocument document(UUID cohort, String fileName, boolean active) {
    return new RagDocument(UUID.randomUUID(), cohort, fileName, 100, 3, 7, OffsetDateTime.now(), "preview", active);
  }

  @Test
  void savesAndReadsBackAllTheDocumentFields() {
    UUID cohort = UUID.randomUUID();
    RagDocument document = document(cohort, "docker.pdf", true);

    repository.save(document, "pdf-bytes".getBytes());
    Optional<RagDocument> found = repository.findById(document.id());

    assertThat(found).isPresent();
    assertThat(found.get().courseCohortId()).isEqualTo(cohort);
    assertThat(found.get().fileName()).isEqualTo("docker.pdf");
    assertThat(found.get().fileSizeBytes()).isEqualTo(100);
    assertThat(found.get().pageCount()).isEqualTo(3);
    assertThat(found.get().chunkCount()).isEqualTo(7);
    assertThat(found.get().previewText()).isEqualTo("preview");
    assertThat(found.get().active()).isTrue();
  }

  @Test
  void findActiveByCourseReturnsOnlyTheActiveDocumentsOfThatCohort() {
    UUID cohort = UUID.randomUUID();
    RagDocument active = document(cohort, "active.pdf", true);
    RagDocument inactive = document(cohort, "inactive.pdf", false);
    repository.save(active, "pdf".getBytes());
    repository.save(inactive, "pdf".getBytes());

    var result = repository.findActiveByCourse(cohort);

    assertThat(result).extracting(RagDocument::id).containsExactly(active.id());
  }

  @Test
  void findActiveByCourseNeverLeaksDocumentsFromAnotherCohort() {
    UUID cohortA = UUID.randomUUID();
    UUID cohortB = UUID.randomUUID();
    RagDocument docA = document(cohortA, "a.pdf", true);
    RagDocument docB = document(cohortB, "b.pdf", true);
    repository.save(docA, "pdf".getBytes());
    repository.save(docB, "pdf".getBytes());

    assertThat(repository.findActiveByCourse(cohortA)).extracting(RagDocument::fileName).containsExactly("a.pdf");
    assertThat(repository.findActiveByCourse(cohortB)).extracting(RagDocument::fileName).containsExactly("b.pdf");
  }

  @Test
  void deactivateIsLogicalAndKeepsTheAuditRow() {
    UUID cohort = UUID.randomUUID();
    RagDocument document = document(cohort, "doc.pdf", true);
    repository.save(document, "pdf".getBytes());

    repository.deactivate(document.id());

    assertThat(repository.findActiveByCourse(cohort)).isEmpty();
    Optional<RagDocument> stillThere = repository.findById(document.id());
    assertThat(stillThere).isPresent();
    assertThat(stillThere.get().active()).isFalse();
  }

  @Test
  void getPdfBytesReturnsTheExactBytesThatWereStored() {
    UUID id = UUID.randomUUID();
    byte[] original = "contenido-binario-del-pdf".getBytes();
    repository.save(new RagDocument(id, UUID.randomUUID(), "doc.pdf", original.length, 1, 1,
        OffsetDateTime.now(), "preview", true), original);

    assertThat(repository.getPdfBytes(id)).isEqualTo(original);
  }

  @Test
  void getPdfBytesIsNullForAnUnknownDocument() {
    assertThat(repository.getPdfBytes(UUID.randomUUID())).isNull();
  }

  @Test
  void findByIdIsEmptyForAnUnknownDocument() {
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }
}
