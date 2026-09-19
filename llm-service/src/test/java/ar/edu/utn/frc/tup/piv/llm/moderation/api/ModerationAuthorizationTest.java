package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationAuthorizationTest {

    private ModerationCourseAuthorization authorization;

    @BeforeEach
    void setUp() {
        authorization = new ModerationCourseAuthorization();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String createTeacherJwt(String sub, String courseId) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"roles\":[\"TEACHER\"],\"courses\":[\"%s\"]}", sub, courseId).getBytes(StandardCharsets.UTF_8)
        );
        return header + "." + payload + ".mock-sig";
    }

    @Test
    void allowsTeacherWithAssignedCourseViaHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IdentityHeaders.USER_ID, "prof-10");
        headers.set(IdentityHeaders.USER_ROLES, "TEACHER");
        headers.set("X-Teacher-Course-Ids", "curso-42,curso-100");

        authorization.requireTeacherCourse("curso-42", headers);
        assertThat(authorization.resolveUserId(headers)).isEqualTo("prof-10");
    }

    @Test
    void allowsTeacherWithAssignedCourseViaUnsignedJwtOnlyWhenTrustFlagIsOn() {
        authorization.setTrustUnsignedBearer(true);
        String jwt = createTeacherJwt("prof-10", "curso-42");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

        authorization.requireTeacherCourse("curso-42", headers);
        assertThat(authorization.resolveUserId(headers)).isEqualTo("prof-10");
    }

    @Test
    void ignoresUnsignedJwtWhenTrustFlagIsOff() {
        String jwt = createTeacherJwt("prof-10", "curso-42");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

        assertThat(authorization.resolveUserId(headers)).isNull();
        assertThatThrownBy(() -> authorization.requireTeacherCourse("curso-42", headers))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsWith403WhenTeacherDoesNotBelongToCourse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IdentityHeaders.USER_ID, "prof-20");
        headers.set(IdentityHeaders.USER_ROLES, "TEACHER");
        headers.set("X-Teacher-Course-Ids", "curso-99");

        assertThatThrownBy(() -> authorization.requireTeacherCourse("curso-42", headers))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).isEqualTo("No sos docente del curso curso-42");
                });
    }

    @Test
    void rejectsWith403WhenUserIsNotTeacher() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IdentityHeaders.USER_ID, "student-01");
        headers.set(IdentityHeaders.USER_ROLES, "STUDENT");
        headers.set("X-Teacher-Course-Ids", "curso-42");

        assertThatThrownBy(() -> authorization.requireTeacherCourse("curso-42", headers))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).isEqualTo("No sos docente del curso curso-42");
                });
    }

    @Test
    void rejectsWith401WhenNotAuthenticated() {
        HttpHeaders headers = new HttpHeaders();

        assertThatThrownBy(() -> authorization.requireTeacherCourse("curso-42", headers))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }
}
