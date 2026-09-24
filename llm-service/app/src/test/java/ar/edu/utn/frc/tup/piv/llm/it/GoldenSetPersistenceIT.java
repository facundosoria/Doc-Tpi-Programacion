package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.ImportBatch;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.GoldenSetImportRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.GoldenSetUpdateProposalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class GoldenSetPersistenceIT extends AbstractIntegrationIT {
  @Autowired CourseGoldenSetRepository golden;
  @Autowired GoldenSetImportRepository imports;
  @Autowired GoldenSetUpdateProposalRepository proposals;
  @Autowired JdbcTemplate jdbc;

  static final String SCORES = "{\"AUTONOMY\":80,\"CLARITY\":70,\"PROGRESSION\":60,\"COMPLIANCE\":90,\"EFFICIENCY\":50}";

  private JsonNode j(String s) throws Exception { return json.readTree(s); }

  private GoldenSetCaseInput input(String author) throws Exception {
    return new GoldenSetCaseInput(j("[{\"role\":\"STUDENT\",\"content\":\"hola\"}]"), j("{\"statement\":\"x\"}"),
        j("{\"k\":1}"), author, j(SCORES), j("{\"AUTONOMY\":\"ok\"}"));
  }

  /** Crea una familia PLATFORM con una versión PUBLISHED y n casos; devuelve el id de versión. */
  private UUID platformVersion(String name, int versionNo, UUID familyId, int cases) {
    if (versionNo == 1) {
      jdbc.update("insert into llm.golden_set_families (id, scope, name, next_version, created_by_user_id) values (?, 'PLATFORM', ?, 5, ?)",
          familyId, name, TEACHER);
    }
    UUID v = UUID.randomUUID();
    jdbc.update("insert into llm.golden_set_versions (id, family_id, version_no, state, created_by_user_id, published_at) values (?,?,?, 'PUBLISHED', ?, now())",
        v, familyId, versionNo, TEACHER);
    for (int i = 0; i < cases; i++) {
      jdbc.update("insert into llm.golden_set_cases (golden_set_version_id, case_order, review_state, transcript, challenge_context, author, reference_scores) values (?,?, 'REVIEWED', cast(? as jsonb), '{}'::jsonb, 'Docente', cast(? as jsonb))",
          v, i, "[{\"role\":\"STUDENT\",\"content\":\"c" + i + "\"}]", SCORES);
    }
    return v;
  }

  @Test
  void draftLifecycleCasesPublishNextDraftAndSoftDelete() throws Exception {
    UUID course = UUID.randomUUID();
    var draft = golden.createDraft(course, "Set A", TEACHER);
    assertThat(draft.state()).isEqualTo("DRAFT");
    assertThat(draft.version()).isEqualTo(1);
    assertThat(draft.basedOnVersionId()).isNull();
    assertThat(golden.countCases(draft.id())).isZero();

    var c0 = golden.addDraftCase(course, draft.id(), input("Ana")).orElseThrow();
    var c1 = golden.addDraftCase(course, draft.id(), input("Beto")).orElseThrow();
    assertThat(c0.order()).isZero();
    assertThat(c1.order()).isEqualTo(1);
    assertThat(golden.countCases(draft.id())).isEqualTo(2);
    // Curso ajeno o versión inexistente: no inserta.
    assertThat(golden.addDraftCase(UUID.randomUUID(), draft.id(), input("X"))).isEmpty();
    assertThat(golden.addDraftCase(course, UUID.randomUUID(), input("X"))).isEmpty();

    var view = golden.find(course, draft.id()).orElseThrow();
    assertThat(view.name()).isEqualTo("Set A");
    assertThat(view.cases()).extracting("author").containsExactly("Ana", "Beto");
    assertThat(golden.find(UUID.randomUUID(), draft.id())).isEmpty();

    var detail = golden.findDetail(course, draft.id()).orElseThrow();
    assertThat(detail.cases()).hasSize(2);
    assertThat(detail.cases().get(0).referenceScores().path("AUTONOMY").asInt()).isEqualTo(80);
    assertThat(detail.cases().get(0).transcript().get(0).path("content").asText()).isEqualTo("hola");
    assertThat(golden.findDetail(UUID.randomUUID(), draft.id())).isEmpty();

    // update: null challengeContext / metadata / justifications toman los defaults.
    var upd = new GoldenSetCaseInput(j("[{\"role\":\"STUDENT\",\"content\":\"nuevo\"}]"), null, null, "Carla", j(SCORES), null);
    var updated = golden.updateDraftCase(course, draft.id(), c0.id(), upd).orElseThrow();
    assertThat(updated.author()).isEqualTo("Carla");
    assertThat(jdbc.queryForObject("select challenge_context::text from llm.golden_set_cases where id=?", String.class, c0.id())).isEqualTo("null");
    assertThat(jdbc.queryForObject("select safe_metadata::text from llm.golden_set_cases where id=?", String.class, c0.id())).isEqualTo("{}");
    assertThat(golden.updateDraftCase(UUID.randomUUID(), draft.id(), c0.id(), upd)).isEmpty();

    assertThat(golden.list(course)).hasSize(1);
    assertThat(golden.list(course).get(0).cases()).hasSize(2);
    assertThat(golden.casesOf(draft.id())).hasSize(2);
    assertThat(golden.casesOf(draft.id()).get(0).transcript().get(0).path("content").asText()).isEqualTo("nuevo");

    // No se puede publicar un curso ajeno; sí el propio; una vez publicado ya no es DRAFT.
    assertThat(golden.publishDraft(UUID.randomUUID(), draft.id())).isFalse();
    assertThat(golden.publishDraft(course, draft.id())).isTrue();
    assertThat(golden.publishDraft(course, draft.id())).isFalse();
    assertThat(golden.addDraftCase(course, draft.id(), input("tarde"))).isEmpty();
    assertThat(golden.updateDraftCase(course, draft.id(), c0.id(), upd)).isEmpty();
    assertThat(golden.softDeleteUnusedDraft(course, draft.id(), TEACHER)).isFalse();

    var next = golden.createNextDraft(course, draft.id(), TEACHER).orElseThrow();
    assertThat(next.version()).isEqualTo(2);
    assertThat(next.familyId()).isEqualTo(draft.familyId());
    assertThat(next.basedOnVersionId()).isEqualTo(draft.id());
    assertThat(golden.countCases(next.id())).isEqualTo(2);
    assertThat(golden.createNextDraft(course, next.id(), TEACHER)).as("solo desde PUBLISHED").isEmpty();
    assertThat(golden.createNextDraft(UUID.randomUUID(), draft.id(), TEACHER)).isEmpty();

    assertThat(golden.list(course)).extracting("version").containsExactly(2, 1);
    assertThat(golden.softDeleteUnusedDraft(UUID.randomUUID(), next.id(), TEACHER)).isFalse();
    assertThat(golden.softDeleteUnusedDraft(course, next.id(), TEACHER)).isTrue();
    assertThat(golden.softDeleteUnusedDraft(course, next.id(), TEACHER)).isFalse();
    assertThat(golden.list(course)).extracting("version").containsExactly(1);
    assertThat(golden.find(course, next.id())).isEmpty();
    assertThat(golden.findDetail(course, next.id())).isEmpty();
  }

  @Test
  void copyOfPublishedPlatformVersionClonesCasesAndRejectsOtherVersions() throws Exception {
    UUID base = platformVersion("Institucional " + UUID.randomUUID(), 1, UUID.randomUUID(), 2);
    UUID course = UUID.randomUUID();
    var copy = golden.copyPublishedPlatformVersion(course, base, TEACHER).orElseThrow();
    assertThat(copy.state()).isEqualTo("DRAFT");
    assertThat(copy.basedOnVersionId()).isEqualTo(base);
    assertThat(golden.countCases(copy.id())).isEqualTo(2);
    assertThat(golden.find(course, copy.id()).orElseThrow().cases()).extracting("reviewState").containsOnly("REVIEWED");

    assertThat(golden.copyPublishedPlatformVersion(course, UUID.randomUUID(), TEACHER)).isEmpty();
    // una versión de curso (no PLATFORM) no se puede copiar
    assertThat(golden.copyPublishedPlatformVersion(course, copy.id(), TEACHER)).isEmpty();
  }

  @Test
  void importBatchFlowFromDraftToCommittedCases() throws Exception {
    UUID course = UUID.randomUUID();
    var draft = golden.createDraft(course, "Import", TEACHER);
    golden.addDraftCase(course, draft.id(), input("Existente")).orElseThrow();
    UUID key = UUID.randomUUID();
    JsonNode row = j("{\"transcript\":[{\"role\":\"STUDENT\",\"content\":\"a\"}],\"challengeContext\":{\"s\":1},\"author\":\"Imp\",\"referenceScores\":" + SCORES + "}");
    JsonNode row2 = j("{\"transcript\":[{\"role\":\"STUDENT\",\"content\":\"b\"}],\"challengeContext\":{\"s\":2},\"author\":\"Imp2\",\"metadata\":{\"m\":true},\"referenceScores\":" + SCORES + ",\"scoreJustifications\":{\"AUTONOMY\":\"j\"}}");

    ImportBatch batch = imports.create(course, draft.id(), "JSON", key, TEACHER, List.of(row, row2)).orElseThrow();
    assertThat(batch.state()).isEqualTo("DRAFT");
    assertThat(batch.rows()).isEqualTo(2);
    // la clave repetida en un lote vigente viola la unicidad; el servicio consulta existing() antes
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> imports.create(course, draft.id(), "JSON", key, TEACHER, List.of(row)))
        .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    assertThat(imports.existing(course, key).orElseThrow().id()).isEqualTo(batch.id());
    assertThat(imports.existing(course, key).orElseThrow().rows()).isZero();
    assertThat(imports.existing(course, key)).isPresent();
    assertThat(imports.existing(course, UUID.randomUUID())).isEmpty();
    // versión de otro curso o inexistente: sin lote
    assertThat(imports.create(UUID.randomUUID(), draft.id(), "JSON", UUID.randomUUID(), TEACHER, List.of(row))).isEmpty();
    // con clave ya usada y precondición fallida devuelve el lote existente
    assertThat(imports.create(course, UUID.randomUUID(), "JSON", key, TEACHER, List.of(row)).orElseThrow().id()).isEqualTo(batch.id());

    assertThat(imports.rows(course, batch.id())).extracting("rowNumber").containsExactly(1, 2);
    assertThat(imports.rows(UUID.randomUUID(), batch.id())).isEmpty();
    assertThat(imports.claimReady(course, batch.id())).as("no está READY").isEmpty();

    JsonNode fixed = j("{\"transcript\":[{\"role\":\"STUDENT\",\"content\":\"c\"}],\"challengeContext\":{},\"author\":\"Fix\",\"referenceScores\":" + SCORES + "}");
    assertThat(imports.replaceRow(course, batch.id(), 1, fixed)).isTrue();
    assertThat(imports.replaceRow(course, batch.id(), 99, fixed)).isFalse();
    assertThat(imports.replaceRow(UUID.randomUUID(), batch.id(), 1, fixed)).isFalse();
    assertThat(imports.rows(course, batch.id()).get(0).payload()).contains("Fix");

    assertThat(imports.beginValidation(UUID.randomUUID(), batch.id())).isFalse();
    assertThat(imports.beginValidation(course, batch.id())).isTrue();
    assertThat(imports.beginValidation(course, batch.id())).as("ya VALIDATING").isFalse();
    assertThat(imports.replaceRow(course, batch.id(), 1, fixed)).as("no editable mientras valida").isFalse();
    imports.rowResult(batch.id(), 1, false, "[\"malo\"]");
    imports.rowResult(batch.id(), 2, true, "[]");
    assertThat(jdbc.queryForList("select state from llm.import_rows where batch_id=? order by row_number", String.class, batch.id()))
        .containsExactly("INVALID", "VALID");
    imports.finishValidation(batch.id(), false);
    assertThat(imports.existing(course, key).orElseThrow().state()).isEqualTo("FAILED");

    // reintento: FAILED se puede revalidar y corregir
    assertThat(imports.beginValidation(course, batch.id())).isTrue();
    imports.rowResult(batch.id(), 1, true, "[]");
    imports.finishValidation(batch.id(), true);
    assertThat(imports.existing(course, key).orElseThrow().state()).isEqualTo("READY");
    imports.finishValidation(batch.id(), false); // no-op: ya no está VALIDATING
    assertThat(imports.existing(course, key).orElseThrow().state()).isEqualTo("READY");

    assertThat(imports.claimReady(UUID.randomUUID(), batch.id())).isEmpty();
    assertThat(imports.claimReady(course, batch.id())).contains(draft.id());
    imports.insertCases(batch.id(), draft.id());
    var cases = golden.find(course, draft.id()).orElseThrow().cases();
    assertThat(cases).extracting("author").containsExactly("Existente", "Fix", "Imp2");
    assertThat(cases).extracting("order").containsExactly(0, 1, 2);
    assertThat(cases).extracting("reviewState").containsExactly("DRAFT", "REVIEWED", "REVIEWED");
    imports.complete(batch.id());
    assertThat(imports.existing(course, key).orElseThrow().state()).isEqualTo("COMMITTED");
  }

  @Test
  void claimReadyRefusesBatchesWithInvalidRows() throws Exception {
    UUID course = UUID.randomUUID();
    var draft = golden.createDraft(course, "Import2", TEACHER);
    UUID key = UUID.randomUUID();
    JsonNode row = j("{\"a\":1}");
    var batch = imports.create(course, draft.id(), "CSV", key, TEACHER, List.of(row)).orElseThrow();
    assertThat(batch.format()).isEqualTo("CSV");
    imports.beginValidation(course, batch.id());
    imports.finishValidation(batch.id(), true); // fila sigue INVALID: READY pero no reclamable
    assertThat(imports.claimReady(course, batch.id())).isEmpty();
  }

  @Test
  void updateProposalsAreCreatedOncePerCourseAndListedWithBaseCaseCount() throws Exception {
    UUID family = UUID.randomUUID();
    UUID v1 = platformVersion("Base " + family, 1, family, 1);
    UUID courseA = UUID.randomUUID();
    UUID courseB = UUID.randomUUID();
    golden.copyPublishedPlatformVersion(courseA, v1, TEACHER).orElseThrow();
    golden.copyPublishedPlatformVersion(courseB, v1, TEACHER).orElseThrow();

    assertThat(proposals.pendingForCourse(courseA)).isEmpty();
    UUID v2 = platformVersion("ignored", 2, family, 3);

    assertThat(proposals.createForPublishedBase(v2)).isEqualTo(2);
    assertThat(proposals.createForPublishedBase(v2)).as("idempotente").isZero();
    assertThat(proposals.createForPublishedBase(UUID.randomUUID())).isZero();
    // v1 no tiene versión previa: no genera nada
    assertThat(proposals.createForPublishedBase(v1)).isZero();

    var pending = proposals.pendingForCourse(courseA);
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).baseVersionId()).isEqualTo(v2);
    assertThat(pending.get(0).baseVersion()).isEqualTo(2);
    assertThat(pending.get(0).baseCaseCount()).isEqualTo(3);
    assertThat(pending.get(0).courseId()).isEqualTo(courseA);
    assertThat(proposals.pendingForCourse(UUID.randomUUID())).isEmpty();

    // Una vez que el curso ya copió la base v2, no se propone de nuevo.
    UUID courseC = UUID.randomUUID();
    golden.copyPublishedPlatformVersion(courseC, v2, TEACHER).orElseThrow();
    UUID v3 = platformVersion("ignored", 3, family, 0);
    assertThat(proposals.createForPublishedBase(v3)).as("A y B (basadas en v1) + C (basada en v2)").isEqualTo(3);
    assertThat(proposals.pendingForCourse(courseA)).extracting("baseVersion").containsExactlyInAnyOrder(2, 3);
  }
}
