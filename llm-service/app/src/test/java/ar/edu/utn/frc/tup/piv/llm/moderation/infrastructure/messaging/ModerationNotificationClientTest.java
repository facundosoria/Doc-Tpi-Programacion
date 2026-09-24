package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.messaging;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ModerationNotificationClientTest {

    private static final String PATH = "/api/notifications/v1/moderation-events";

    private ModerationResolutionDomainEvent sampleEvent() {
        return ModerationResolutionDomainEvent.ofReversed(
                "msg-77", UUID.randomUUID(), "curso-42", "alumno-9", "prof-10",
                "Era contenido educativo, se revierte el bloqueo.");
    }

    @Test
    void postsEventToNotificationsServiceThroughGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(requestTo(PATH))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess());

        ModerationNotificationClient client = new ModerationNotificationClient(restClient, PATH);

        client.notifyMessageUnblocked(sampleEvent());

        server.verify();
    }

    @Test
    void doesNotThrowWhenNotificationsServiceFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(requestTo(PATH)).andRespond(withServerError());

        ModerationNotificationClient client = new ModerationNotificationClient(restClient, PATH);

        assertThatCode(() -> client.notifyMessageUnblocked(sampleEvent()))
                .doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void doesNotCallNotificationsServiceWhenDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        ModerationNotificationClient client = new ModerationNotificationClient(builder.build(), PATH, false);

        client.notifyMessageUnblocked(sampleEvent());

        server.verify();
    }
}
