package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService.ChallengeOverlayVersion;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService.OverlayInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.EffectiveRubricResolver;
import ar.edu.utn.frc.tup.piv.llm.application.service.EffectiveRubricResolver.EffectiveDimension;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeRubricOverlayControllerTest {
  private final ChallengeRubricOverlayService overlay = mock(ChallengeRubricOverlayService.class);
  private final GoldenSetAuthorization identityAuthorization = mock(GoldenSetAuthorization.class);
  private final CourseAuthorization courseAuthorization = mock(CourseAuthorization.class);
  private final ChallengeRubricOverlayController controller =
      new ChallengeRubricOverlayController(overlay, identityAuthorization, courseAuthorization);

  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), "request", null);

  @Test void listAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    when(overlay.listByChallenge(course, challenge)).thenReturn(List.of());

    var page = controller.list(course, challenge, headers);

    assertThat(page.items()).isEmpty();
    verify(courseAuthorization).requireTeacher(course, actor, headers);
  }

  @Test void getAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    var ov = new ChallengeOverlayVersion(version, UUID.randomUUID(), 1, "Overlay", "DRAFT", 1,
        "MODULAR_CUSTOM", "Guía", List.of(), UUID.randomUUID());
    when(overlay.get(course, challenge, version)).thenReturn(ov);

    var result = controller.get(course, challenge, version, headers);

    assertThat(result.id()).isEqualTo(version);
  }

  @Test void createAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), baseline = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    var created = new ChallengeOverlayVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "Overlay", "DRAFT", 1,
        "MODULAR_CUSTOM", "", List.of(), baseline);
    when(overlay.createDraft(course, challenge, "Overlay", baseline, actor)).thenReturn(created);

    var response = controller.create(course, challenge,
        new ChallengeRubricOverlayController.CreateOverlayRequest("Overlay", baseline), headers);

    assertThat(response.getBody().name()).isEqualTo("Overlay");
    verify(courseAuthorization).requireTeacher(course, actor, headers);
  }

  @Test void autosaveAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    var input = new OverlayInput("Overlay", "Guía", List.of());
    var saved = new ChallengeOverlayVersion(version, UUID.randomUUID(), 1, "Overlay", "DRAFT", 2,
        "MODULAR_CUSTOM", "Guía", List.of(), UUID.randomUUID());
    when(overlay.autosave(course, challenge, version, 5, input, actor)).thenReturn(saved);

    var result = controller.autosave(course, challenge, version, 5, input, headers);

    assertThat(result.revision()).isEqualTo(2);
  }

  @Test void publishAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);

    var response = controller.publish(course, challenge, version, headers);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    verify(overlay).publish(course, challenge, version, actor);
  }

  @Test void createNextVersionAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    var next = new ChallengeOverlayVersion(UUID.randomUUID(), UUID.randomUUID(), 2, "Overlay v2", "DRAFT", 1,
        "MODULAR_CUSTOM", "", List.of(), UUID.randomUUID());
    when(overlay.createNextVersion(course, challenge, version, actor)).thenReturn(next);

    var response = controller.createNextVersion(course, challenge, version, headers);

    assertThat(response.getBody().version()).isEqualTo(2);
  }

  @Test void effectiveAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    var dim = new EffectiveDimension("a", "A", "c", null, BigDecimal.valueOf(100),
        EffectiveDimension.Origin.OVERLAY);
    when(overlay.getEffectiveProfile(course, challenge, version)).thenReturn(List.of(dim));

    var profile = controller.effective(course, challenge, version, headers);

    assertThat(profile.dimensions()).hasSize(1);
  }
}