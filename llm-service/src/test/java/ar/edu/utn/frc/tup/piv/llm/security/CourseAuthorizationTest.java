package ar.edu.utn.frc.tup.piv.llm.security;

import java.util.Set;
import java.util.UUID;
import ar.edu.utn.frc.tup.piv.llm.configuration.WorkbenchDemoCatalog;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseAuthorizationTest {
  @Test void allowsAssignedTeacher() {
    UUID course = UUID.randomUUID();
    assertThatCode(() -> new CourseAuthorization(org.mockito.Mockito.mock(WorkbenchDemoCatalog.class)).requireTeacher(course, UUID.randomUUID(), Set.of("TEACHER"), Set.of(course))).doesNotThrowAnyException();
  }
  @Test void rejectsUnassignedCourse() {
    assertThatThrownBy(() -> new CourseAuthorization(org.mockito.Mockito.mock(WorkbenchDemoCatalog.class)).requireTeacher(UUID.randomUUID(), UUID.randomUUID(), Set.of("TEACHER"), Set.of())).isInstanceOf(RuntimeException.class);
  }
  @Test void rejectsNonTeacherRole() {
    UUID course = UUID.randomUUID();
    assertThatThrownBy(() -> new CourseAuthorization(org.mockito.Mockito.mock(WorkbenchDemoCatalog.class)).requireTeacher(course, UUID.randomUUID(), Set.of("STUDENT"), Set.of(course))).isInstanceOf(RuntimeException.class);
  }
}
