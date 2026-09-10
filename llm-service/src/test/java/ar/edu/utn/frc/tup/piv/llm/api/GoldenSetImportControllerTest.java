package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.GoldenSetImportService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetImportRepository.ImportBatch;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldenSetImportControllerTest {
  @Test void authorizesAndCreatesImportBatch() {
    var service = mock(GoldenSetImportService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new GoldenSetImportController(service, identity, courses);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), key = UUID.randomUUID(), actorId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("gateway", actorId, null, null);
    when(identity.require(headers)).thenReturn(actor);
    var row = JsonNodeFactory.instance.objectNode().put("author", "Docente");
    var request = new GoldenSetImportController.ImportRequest(version, "JSON", List.of(row));
    var expected = new ImportBatch(UUID.randomUUID(), version, "JSON", "DRAFT", 1);
    when(service.create(course, version, "JSON", List.of(row), key, actorId)).thenReturn(expected);

    var response = controller.create(course, request, key, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expected);
    verify(identity).require(headers);
    verify(courses).requireTeacher(course, actor, headers);
  }

  @Test void authorizesAndValidatesBatch() {
    var service = mock(GoldenSetImportService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new GoldenSetImportController(service, identity, courses);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID(), actorId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identity.require(headers)).thenReturn(new CallerIdentity("gateway", actorId, null, null));
    var expected = new ImportBatch(batch, null, null, "READY", 0);
    when(service.validate(course, batch)).thenReturn(expected);

    var result = controller.validate(course, batch, headers);

    assertThat(result.state()).isEqualTo("READY");
    verify(service).validate(course, batch);
  }

  @Test void authorizesAndReplacesRow() {
    var service = mock(GoldenSetImportService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new GoldenSetImportController(service, identity, courses);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID(), actorId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identity.require(headers)).thenReturn(new CallerIdentity("gateway", actorId, null, null));
    var payload = JsonNodeFactory.instance.objectNode().put("author", "Nuevo autor");

    controller.replace(course, batch, 1, payload, headers);

    verify(service).replaceRow(course, batch, 1, payload);
  }

  @Test void authorizesAndCommitsBatch() {
    var service = mock(GoldenSetImportService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new GoldenSetImportController(service, identity, courses);
    UUID course = UUID.randomUUID(), batch = UUID.randomUUID(), actorId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identity.require(headers)).thenReturn(new CallerIdentity("gateway", actorId, null, null));

    var response = controller.commit(course, batch, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    verify(service).commit(course, batch);
  }
}
