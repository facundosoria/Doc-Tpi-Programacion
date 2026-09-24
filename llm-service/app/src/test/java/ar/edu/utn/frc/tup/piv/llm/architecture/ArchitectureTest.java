package ar.edu.utn.frc.tup.piv.llm.architecture;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CalibrationActivationController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CalibrationRunController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CourseEvaluationStatusController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.CourseGoldenSetController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.GoldenSetImportController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.GoldenSetUpdateProposalController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.InstitutionalCalibrationController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ModelAssignmentController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ModelDeploymentController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ProviderCredentialController;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationCaseResultRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.JdbcCalibrationWorkflowStore;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationActivationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hace cumplir ADR-001/ADR-003 (docs/00-gobierno-y-evolucion/adr) y reglas estrictas de Clean Architecture / DDD:
 * - domain es el núcleo: no depende de Spring, Kafka, JDBC/JPA, SDKs de modelos, infrastructure, application ni api.
 * - application no depende de la capa de presentación (api).
 * - adapter.in.web no depende directamente de adapter.out.persistence (orquestado vía application).
 * Jackson en domain es una excepción documentada y queda explícitamente permitido.
 */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("ar.edu.utn.frc.tup.piv.llm");
  }

  @Test
  void domainDoesNotDependOnSpring() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnKafka() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.apache.kafka..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnProviderSdks() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "dev.langchain4j..",
            "com.openai..",
            "com.anthropic..",
            "com.google.genai..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnJdbcOrJpa() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.persistence..", "jakarta.persistence..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnInfrastructure() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnAdapters() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..adapter..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnApplication() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..application..");
    rule.check(classes);
  }

  @Test
  void domainDoesNotDependOnApi() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..api..");
    rule.check(classes);
  }

  @Test
  void applicationDoesNotDependOnApi() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..api..");
    rule.check(classes);
  }

  @Test
  void webAdapterDoesNotDependOnPersistenceAdapter() {
    // ADR-003 §3: deuda conocida. Tras la integración main↔dev (2026-09-21) estos diez controllers
    // siguen tomando tipos de `adapter.out.persistence` (records anidados de los repositories) en
    // vez de pasar por `application`. La lista solo puede achicarse: no agregar controllers acá.
    ArchRule rule = noClasses()
        .that().resideInAPackage("..adapter.in.web..")
        .and().haveNameNotMatching(".*(ProviderCredentialController|InstitutionalCalibrationController"
            + "|CalibrationActivationController|CalibrationRunController|CourseEvaluationStatusController"
            + "|CourseGoldenSetController|GoldenSetImportController|GoldenSetUpdateProposalController"
            + "|ModelAssignmentController|ModelDeploymentController|EvaluatorSkillsController).*")
        .should().dependOnClassesThat().resideInAnyPackage("..adapter.out.persistence..");
    rule.check(classes);
  }

  /**
   * E-31: el shadow descarta su salida por construcción. Si el módulo pudiera tocar el outbox/Kafka o
   * las tablas de calibración y evaluaciones reales, una evaluación en sombra podría emitir un score.
   */
  @Test
  void shadowCannotEmitEventsNorTouchRealCalibrationOrEvaluationState() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("ar.edu.utn.frc.tup.piv.llm.shadow..")
        .should().dependOnClassesThat().resideInAnyPackage("..messaging..", "org.apache.kafka..", "org.springframework.kafka..")
        .orShould().dependOnClassesThat(com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf(
            ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository.class,
            ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationCaseResultRepository.class,
            ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.JdbcCalibrationWorkflowStore.class,
            ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService.class,
            ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationActivationService.class));
    rule.check(classes);
  }
}
