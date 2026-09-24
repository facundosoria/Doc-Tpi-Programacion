package ar.edu.utn.frc.tup.piv.llm.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.port.out.CourseMembershipPort.Membership;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

class GatewayCoursesMembershipClientTest {

  private static final UUID COURSE = UUID.randomUUID();
  private static final UUID USER = UUID.randomUUID();
  private static final CallerIdentity CALLER = new CallerIdentity("workbench", USER, "req-1", "tp-1");

  private static Object membershipResponse(String role, String status) throws Exception {
    Class<?> type = Class.forName("ar.edu.utn.frc.tup.piv.llm.adapter.out.http.GatewayCoursesMembershipClient$MembershipResponse");
    Constructor<?> constructor = type.getDeclaredConstructor(String.class, String.class);
    constructor.setAccessible(true);
    return constructor.newInstance(role, status);
  }

  private static Chain chain() {
    RestClient client = mock(RestClient.class);
    RestClient.Builder builder = mock(RestClient.Builder.class);
    @SuppressWarnings("rawtypes")
    RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    @SuppressWarnings("rawtypes")
    RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
    when(builder.baseUrl(anyString())).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    when(client.get()).thenReturn(uriSpec);
    when(uriSpec.uri(anyString(), any(), any())).thenReturn(headersSpec);
    when(headersSpec.headers(any())).thenReturn(headersSpec);
    when(headersSpec.accept(any(MediaType.class))).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    return new Chain(new GatewayCoursesMembershipClient(builder, "http://gateway", "service-token"),
        uriSpec, headersSpec, responseSpec);
  }

  private record Chain(GatewayCoursesMembershipClient client,
      RestClient.RequestHeadersUriSpec<?> uriSpec,
      RestClient.RequestHeadersSpec<?> headersSpec,
      RestClient.ResponseSpec responseSpec) {}

  @Test
  void membershipReturnsTheActiveTeacherStatus() throws Exception {
    Chain chain = chain();
    when(chain.responseSpec().body(any(Class.class))).thenAnswer(inv -> membershipResponse("TEACHER", "ACTIVE"));

    Membership membership = chain.client().membership(COURSE, USER, CALLER);

    assertThat(membership.isActiveTeacher()).isTrue();
    assertThat(membership.role()).isEqualTo("TEACHER");
    assertCorrelation(chain, "service-token", "req-1", "tp-1");
  }

  @Test
  void membershipPropagatesCorrelationAndDefaultRequestIdWhenMissing() throws Exception {
    Chain chain = chain();
    when(chain.responseSpec().body(any(Class.class))).thenAnswer(inv -> membershipResponse("TEACHER", "ACTIVE"));
    CallerIdentity noTrace = new CallerIdentity("workbench", USER, null, "   ");

    chain.client().membership(COURSE, USER, noTrace);

    ArgumentCaptor<Consumer<HttpHeaders>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(chain.headersSpec()).headers(captor.capture());
    HttpHeaders sent = new HttpHeaders();
    captor.getValue().accept(sent);
    assertThat(sent.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer service-token");
    assertThat(sent.getFirst("X-Request-Id")).isNotBlank();
    assertThat(sent.getFirst("traceparent")).isNull();
  }

  @Test
  void membershipPropagatesForANullCaller() throws Exception {
    Chain chain = chain();
    when(chain.responseSpec().body(any(Class.class))).thenAnswer(inv -> membershipResponse("TEACHER", "ACTIVE"));

    chain.client().membership(COURSE, USER, null);

    ArgumentCaptor<Consumer<HttpHeaders>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(chain.headersSpec()).headers(captor.capture());
    HttpHeaders sent = new HttpHeaders();
    captor.getValue().accept(sent);
    assertThat(sent.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer service-token");
    assertThat(sent.getFirst("X-Request-Id")).isNotBlank();
    assertThat(sent.getFirst("traceparent")).isNull();
  }

  @Test
  void membershipRejectsNullCourseOrUser() {
    Chain chain = chain();

    assertForbidden(() -> chain.client().membership(null, USER, CALLER));
    assertForbidden(() -> chain.client().membership(COURSE, null, CALLER));
  }

  @Test
  void membershipRejectsWhenTheBodyIsMissing() {
    Chain chain = chain();
    when(chain.responseSpec().body(any(Class.class))).thenReturn(null);

    assertForbidden(() -> chain.client().membership(COURSE, USER, CALLER));
  }

  @Test
  void membershipMaps4xxToForbidden() {
    Chain chain = chain();
    doThrow(new RestClientResponseException("denied", 403, "Forbidden", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8))
        .when(chain.responseSpec()).body(any(Class.class));

    assertForbidden(() -> chain.client().membership(COURSE, USER, CALLER));
  }

  @Test
  void membershipMaps5xxToServiceUnavailable() {
    Chain chain = chain();
    doThrow(new RestClientResponseException("down", 503, "Service Unavailable", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8))
        .when(chain.responseSpec()).body(any(Class.class));

    assertUnavailable(() -> chain.client().membership(COURSE, USER, CALLER));
  }

  @Test
  void membershipWrapsRuntimeFailuresAsUnavailable() {
    Chain chain = chain();
    doThrow(new RuntimeException("boom")).when(chain.responseSpec()).body(any(Class.class));

    assertUnavailable(() -> chain.client().membership(COURSE, USER, CALLER));
  }

  private static void assertCorrelation(Chain chain, String token, String requestId, String traceparent) {
    ArgumentCaptor<Consumer<HttpHeaders>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(chain.headersSpec()).headers(captor.capture());
    HttpHeaders sent = new HttpHeaders();
    captor.getValue().accept(sent);
    assertThat(sent.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
    assertThat(sent.getFirst("X-Request-Id")).isEqualTo(requestId);
    assertThat(sent.getFirst("traceparent")).isEqualTo(traceparent);
  }

  private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable).isInstanceOfSatisfying(ResponseStatusException.class,
        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  private static void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable).isInstanceOfSatisfying(ResponseStatusException.class,
        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }
}