package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.messaging;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente saliente hacia {@code notifications-service} (LLM-S12-H02 / T4 / CA5).
 * Reutiliza el {@link RestClient} de Gateway ya configurado en {@code HttpClientConfig}
 * (mismo patrón M2M que el resto de las llamadas salientes de este servicio: nunca se llama
 * directo a otro microservicio, todo pasa por el Gateway).
 */
@Component
public class ModerationNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(ModerationNotificationClient.class);

    private final RestClient gatewayRestClient;
    private final String moderationEventsPath;
    private final boolean enabled;

    public ModerationNotificationClient(RestClient gatewayRestClient, String moderationEventsPath) {
        this(gatewayRestClient, moderationEventsPath, true);
    }

    @Autowired
    public ModerationNotificationClient(
            RestClient gatewayRestClient,
            @Value("${app.notifications.moderation-events-path:/api/notifications/v1/moderation-events}")
            String moderationEventsPath,
            @Value("${app.notifications.enabled:true}") boolean enabled) {
        this.gatewayRestClient = gatewayRestClient;
        this.moderationEventsPath = moderationEventsPath;
        this.enabled = enabled;
    }

    /**
     * Notifica al alumno el desbloqueo de su mensaje. Falla en modo "best effort": un error de
     * notificaciones no debe revertir ni bloquear la resolución del docente ya persistida, pero
     * sí queda registrado en el log para su seguimiento/alerta operativa.
     */
    public void notifyMessageUnblocked(ModerationResolutionDomainEvent event) {
        if (!enabled) {
            log.info("[MOCK] app.notifications.enabled=false: notificación NO enviada "
                            + "[eventId={}, messageId={}, userId={}, resolution={}]",
                    event.getEventId(), event.getMessageId(), event.getUserId(), event.getResolution());
            return;
        }
        Map<String, Object> payload = Map.ofEntries(
                Map.entry("eventId", event.getEventId()),
                Map.entry("eventType", event.getEventType()),
                Map.entry("version", event.getVersion()),
                Map.entry("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt() : OffsetDateTime.now()),
                Map.entry("producer", event.getProducer()),
                Map.entry("messageId", event.getMessageId()),
                Map.entry("incidentId", event.getIncidentId()),
                Map.entry("courseId", event.getCourseId()),
                Map.entry("userId", event.getUserId()),
                Map.entry("resolvedBy", event.getResolvedBy()),
                Map.entry("resolution", event.getResolution()),
                Map.entry("resolutionReason", event.getResolutionReason() == null ? "" : event.getResolutionReason())
        );

        try {
            gatewayRestClient.post()
                    .uri(moderationEventsPath)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notificación de desbloqueo enviada a notifications-service [eventId={}, messageId={}, userId={}]",
                    event.getEventId(), event.getMessageId(), event.getUserId());
        } catch (RestClientException e) {
            log.warn("No se pudo notificar a notifications-service el desbloqueo del mensaje "
                            + "[eventId={}, messageId={}, userId={}]. Se continúa (best effort): {}",
                    event.getEventId(), event.getMessageId(), event.getUserId(), e.getMessage());
        }
    }
}
