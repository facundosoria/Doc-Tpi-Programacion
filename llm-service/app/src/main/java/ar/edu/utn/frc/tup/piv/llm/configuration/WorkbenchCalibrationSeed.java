package ar.edu.utn.frc.tup.piv.llm.configuration;

import ar.edu.utn.frc.tup.piv.llm.application.service.CourseGoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricPublicationService;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseInput;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-only, idempotent data for exercising Golden Set composition and calibration.
 */
@Component
@Profile("workbench")
public class WorkbenchCalibrationSeed implements ApplicationRunner {
    private static final UUID COURSE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private final RubricDraftService rubrics;
    private final RubricPublicationService publication;
    private final CourseGoldenSetService goldenSets;
    private final ObjectMapper json;

    public WorkbenchCalibrationSeed(RubricDraftService rubrics, RubricPublicationService publication, CourseGoldenSetService goldenSets, ObjectMapper json) {
        this.rubrics = rubrics;
        this.publication = publication;
        this.goldenSets = goldenSets;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) {
        var actor = new CallerIdentity("llm-service", ACTOR, "workbench-seed", null);
        for (String name : List.of("Exigente", "Normal", "Permisiva"))
            if (rubrics.list(COURSE).stream().noneMatch(r -> r.name().equals(name))) {
                var draft = rubrics.createFromTemplate(COURSE, TEMPLATE, name, actor);
                publication.publish(COURSE, draft.id(), actor);
            }
        seedSet(actor, "Golden Set · Predominio excelente", List.of("excellent", "excellent", "excellent", "regular", "poor"));
        seedSet(actor, "Golden Set · Distribución equilibrada", List.of("excellent", "excellent", "regular", "regular", "poor"));
        seedSet(actor, "Golden Set · Predominio regular", List.of("excellent", "regular", "regular", "regular", "poor"));
    }

    private void seedSet(CallerIdentity actor, String name, List<String> profiles) {
        if (goldenSets.list(COURSE).stream().anyMatch(set -> set.name().equals(name))) return;
        var draft = goldenSets.createDraft(COURSE, name, actor);
        for (int index = 0; index < profiles.size(); index++)
            goldenSets.addCase(COURSE, draft.id(), caseFor(profiles.get(index), index + 1), actor);
        goldenSets.publish(COURSE, draft.id(), actor);
    }

    private GoldenSetCaseInput caseFor(String profile, int number) {
        int score = switch (profile) {
            case "excellent" -> 90;
            case "regular" -> 60;
            default -> 25;
        };
        String text = switch (profile) {
            case "excellent" ->
                    "Presento mi hipótesis, el error observado y una prueba de borde. ¿Me orientás para validarla?";
            case "regular" -> "Probé una alternativa y el resultado mejoró parcialmente. ¿Qué debería revisar ahora?";
            default -> "No funciona, pasame la solución completa.";
        };
        var transcript = json.valueToTree(List.of(Map.of("role", "student", "content", text), Map.of("role", "assistant", "content", "Ofrezco una pista conceptual sin resolver el ejercicio.")));
        var scores = json.valueToTree(Map.of("AUTONOMY", score, "CLARITY", score, "PROGRESSION", score, "COMPLIANCE", score, "EFFICIENCY", score));
        return new GoldenSetCaseInput(transcript, json.valueToTree(Map.of("challenge", "Caso demo " + number)), json.createObjectNode(), "workbench-seed", scores, json.createObjectNode());
    }
}
