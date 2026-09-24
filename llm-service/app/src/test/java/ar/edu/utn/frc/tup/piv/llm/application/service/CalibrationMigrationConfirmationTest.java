package ar.edu.utn.frc.tup.piv.llm.application.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalibrationMigrationConfirmationTest {
  @Test void issuesSingleUseTokensForKeyedPreviews() {
    var confirmations = new CalibrationMigrationConfirmation();
    UUID course = UUID.randomUUID(), run = UUID.randomUUID(), challenge = UUID.randomUUID();

    String token = confirmations.issue(course, run, Set.of(challenge));

    assertThat(token).isNotBlank();
    assertThat(confirmations.consume(token, course, run, Set.of(challenge))).containsExactly(challenge);
    assertThatThrownBy(() -> confirmations.consume(token, course, run, Set.of(challenge)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void rejectsAnUnknownToken() {
    var confirmations = new CalibrationMigrationConfirmation();
    assertThatThrownBy(() -> confirmations.consume("missing", UUID.randomUUID(), UUID.randomUUID(), Set.of()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void rejectsSelectedChallengesOutsideTheApprovedPreview() {
    var confirmations = new CalibrationMigrationConfirmation();
    UUID course = UUID.randomUUID(), run = UUID.randomUUID();
    UUID approved = UUID.randomUUID(), requested = UUID.randomUUID();
    String token = confirmations.issue(course, run, Set.of(approved));
    assertThatThrownBy(() -> confirmations.consume(token, course, run, Set.of(requested)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void rejectsTokensBoundToAnotherCourseOrRun() {
    var confirmations = new CalibrationMigrationConfirmation();
    UUID course = UUID.randomUUID(), run = UUID.randomUUID(), challenge = UUID.randomUUID();
    String token = confirmations.issue(course, run, Set.of(challenge));
    assertThatThrownBy(() -> confirmations.consume(token, UUID.randomUUID(), run, Set.of(challenge)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> confirmations.consume(token, course, UUID.randomUUID(), Set.of(challenge)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void rejectsExpiredPreviews() throws Exception {
    var confirmations = new CalibrationMigrationConfirmation();
    UUID course = UUID.randomUUID(), run = UUID.randomUUID(), challenge = UUID.randomUUID();
    Constructor<?> previewConstructor = Class.forName(
            "ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationMigrationConfirmation$Preview")
        .getDeclaredConstructors()[0];
    previewConstructor.setAccessible(true);
    Object expired = previewConstructor.newInstance(course, run, Set.of(challenge), Instant.now().minusSeconds(60));
    Field previewsField = CalibrationMigrationConfirmation.class.getDeclaredField("previews");
    previewsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, Object> previews = (Map<String, Object>) previewsField.get(confirmations);
    previews.put("expired", expired);

    assertThatThrownBy(() -> confirmations.consume("expired", course, run, Set.of(challenge)))
        .isInstanceOf(IllegalStateException.class);
  }
}