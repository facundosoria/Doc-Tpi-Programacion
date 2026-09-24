package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.port.out.CourseMembershipPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CourseAuthorizationCoverageTest {

  private final CourseMembershipPort memberships = mock(CourseMembershipPort.class);

  @Test
  void rejectsANullCourse() {
    var authorization = new CourseAuthorization(memberships);

    assertForbidden(() -> authorization.requireTeacher(null,
        new CallerIdentity("x", UUID.randomUUID(), null, null), new HttpHeaders()));
    verify(memberships, never()).membership(any(), any(), any());
  }

  @Test
  void rejectsANullActor() {
    var authorization = new CourseAuthorization(memberships);

    assertForbidden(() -> authorization.requireTeacher(UUID.randomUUID(), null, new HttpHeaders()));
    verify(memberships, never()).membership(any(), any(), any());
  }

  @Test
  void rejectsAnActorWithoutDelegatedUser() {
    var authorization = new CourseAuthorization(memberships);

    assertForbidden(() -> authorization.requireTeacher(UUID.randomUUID(),
        new CallerIdentity("x", null, null, null), new HttpHeaders()));
    verify(memberships, never()).membership(any(), any(), any());
  }

  @Test
  void rejectsWhenMembershipIsNotAnActiveTeacher() {
    var authorization = new CourseAuthorization(memberships);
    when(memberships.membership(any(), any(), any()))
        .thenReturn(new CourseMembershipPort.Membership("STUDENT", "ACTIVE"));

    assertForbidden(() -> authorization.requireTeacher(UUID.randomUUID(),
        new CallerIdentity("x", UUID.randomUUID(), null, null), new HttpHeaders()));
  }

  private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable)
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }
}