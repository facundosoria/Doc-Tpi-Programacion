package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.CourseGoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

class CourseGoldenSetControllerTest {
  @Test void authorizesTheCourseBeforeCopyingTheBase() {
    var service = mock(CourseGoldenSetService.class); var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new CourseGoldenSetController(service, identity, courses); UUID course = UUID.randomUUID(), base = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders(); CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    CourseGoldenSetVersion copy = new CourseGoldenSetVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "DRAFT", base);
    when(identity.require(headers)).thenReturn(actor); when(service.copyFromPublishedBase(course, base, actor)).thenReturn(copy);

    var response = controller.copyFromBase(course, base, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    var order = org.mockito.Mockito.inOrder(identity, courses, service);
    order.verify(identity).require(headers); order.verify(courses).requireTeacher(course, actor, headers); order.verify(service).copyFromPublishedBase(course, base, actor);
  }

  @Test void authorizesTheCourseBeforeListingItsGoldenSets() {
    var service = mock(CourseGoldenSetService.class); var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new CourseGoldenSetController(service, identity, courses); UUID course = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders(); CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    when(identity.require(headers)).thenReturn(actor); when(service.list(course)).thenReturn(java.util.List.of());
    assertThat(controller.list(course, headers).items()).isEmpty(); verify(courses).requireTeacher(course, actor, headers);
  }

  @Test void authorizesAndPublishesVersion() {
    var service = mock(CourseGoldenSetService.class); var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new CourseGoldenSetController(service, identity, courses); UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders(); CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    when(identity.require(headers)).thenReturn(actor);

    var response = controller.publish(course, version, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(courses).requireTeacher(course, actor, headers);
    verify(service).publish(course, version, actor);
  }

  @Test void authorizesAndCreatesNextVersion() {
    var service = mock(CourseGoldenSetService.class); var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new CourseGoldenSetController(service, identity, courses); UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders(); CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    when(identity.require(headers)).thenReturn(actor);
    CourseGoldenSetVersion next = new CourseGoldenSetVersion(UUID.randomUUID(), UUID.randomUUID(), 2, "DRAFT", version);
    when(service.createNextVersion(course, version, actor)).thenReturn(next);

    var response = controller.createNextVersion(course, version, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(next);
    verify(courses).requireTeacher(course, actor, headers);
    verify(service).createNextVersion(course, version, actor);
  }
}
