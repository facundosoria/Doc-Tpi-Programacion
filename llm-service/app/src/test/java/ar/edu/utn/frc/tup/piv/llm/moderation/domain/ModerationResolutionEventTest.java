package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationIncidentResolveService;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationResolutionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ResolveIncidentCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationEventPublisherPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaEventProducer;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.messaging.ModerationEventPublisher;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.messaging.ModerationNotificationClient;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationResolutionEventTest {

    private ModerationIncidentRepositoryPort incidentRepository;
    private ModerationResolutionRepositoryPort resolutionRepository;
    private ModerationAppealRepositoryPort appealRepository;
    private ModerationEventPublisherPort eventPublisher;
    private ModerationIncidentResolveService resolveService;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(ModerationIncidentRepositoryPort.class);
        resolutionRepository = mock(ModerationResolutionRepositoryPort.class);
        appealRepository = mock(ModerationAppealRepositoryPort.class);
        eventPublisher = mock(ModerationEventPublisherPort.class);

        resolveService = new ModerationIncidentResolveService(
                incidentRepository,
                resolutionRepository,
                appealRepository,
                eventPublisher
        );
    }

    @Test
    void publishesUnblockEventWhenResolutionIsReversed() {
        UUID incidentId = UUID.randomUUID();
        String messageId = "msg-999";
        String courseId = "curso-42";
        String userId = "user-55";
        String teacherId = "prof-10";
        String reason = "El texto correspondía a un ejemplo legítimo de código base64, se revierte el bloqueo.";

        ModerationIncident incident = new ModerationIncident(
                incidentId, messageId, userId, courseId, "PENDING_REVIEW", "CODE_OBFUSCATION", "preview...", OffsetDateTime.now()
        );

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(resolutionRepository.existsByIncidentId(incidentId)).thenReturn(false);
        when(incidentRepository.save(any(ModerationIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveIncidentCommand command = new ResolveIncidentCommand(
                incidentId,
                "REVERSED",
                reason,
                teacherId
        );

        ModerationResolutionResult result = resolveService.resolve(command);

        assertThat(result.resolution()).isEqualTo("REVERSED");
        assertThat(result.status()).isEqualTo("REVERSED");

        ArgumentCaptor<ModerationResolutionDomainEvent> eventCaptor = ArgumentCaptor.forClass(ModerationResolutionDomainEvent.class);
        verify(eventPublisher).publishMessageUnblocked(eventCaptor.capture());

        ModerationResolutionDomainEvent event = eventCaptor.getValue();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getVersion()).isEqualTo("1.0");
        assertThat(event.getProducer()).isEqualTo("llm-service");
        assertThat(event.getEventType()).isEqualTo("MESSAGE_UNBLOCKED");
        assertThat(event.getMessageId()).isEqualTo(messageId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getCourseId()).isEqualTo(courseId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getResolvedBy()).isEqualTo(teacherId);
        assertThat(event.getResolution()).isEqualTo("REVERSED");
        assertThat(event.getResolutionReason()).isEqualTo(reason);
    }

    @Test
    void doesNotPublishUnblockEventWhenResolutionIsConfirmed() {
        UUID incidentId = UUID.randomUUID();
        String messageId = "msg-100";
        String courseId = "curso-42";
        String userId = "user-55";
        String teacherId = "prof-10";
        String reason = "El mensaje contenía ofensas explícitas hacia otros alumnos del curso.";

        ModerationIncident incident = new ModerationIncident(
                incidentId, messageId, userId, courseId, "PENDING_REVIEW", "OFFENSIVE", "preview...", OffsetDateTime.now()
        );

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(resolutionRepository.existsByIncidentId(incidentId)).thenReturn(false);
        when(incidentRepository.save(any(ModerationIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveIncidentCommand command = new ResolveIncidentCommand(
                incidentId,
                "CONFIRMED",
                reason,
                teacherId
        );

        ModerationResolutionResult result = resolveService.resolve(command);

        assertThat(result.resolution()).isEqualTo("CONFIRMED");
        verify(eventPublisher, never()).publishMessageUnblocked(any());
    }

    @Test
    void moderationEventPublisherDispatchesToSpringApplicationEventPublisher() {
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);
        ModerationNotificationClient notificationClient = mock(ModerationNotificationClient.class);
        KafkaEventProducer kafkaEventProducer = mock(KafkaEventProducer.class);
        ModerationEventPublisher publisher = new ModerationEventPublisher(springPublisher, notificationClient, kafkaEventProducer);

        ModerationResolutionDomainEvent event = ModerationResolutionDomainEvent.ofReversed(
                "msg-1", UUID.randomUUID(), "curso-42", "user-1", "prof-1", "Motivo de prueba con más de veinte caracteres"
        );

        publisher.publishMessageUnblocked(event);

        verify(springPublisher).publishEvent(event);
    }

    @Test
    void moderationEventPublisherAlsoNotifiesStudentThroughNotificationsService() {
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);
        ModerationNotificationClient notificationClient = mock(ModerationNotificationClient.class);
        KafkaEventProducer kafkaEventProducer = mock(KafkaEventProducer.class);
        ModerationEventPublisher publisher = new ModerationEventPublisher(springPublisher, notificationClient, kafkaEventProducer);

        ModerationResolutionDomainEvent event = ModerationResolutionDomainEvent.ofReversed(
                "msg-2", UUID.randomUUID(), "curso-42", "user-2", "prof-1", "Motivo de prueba con más de veinte caracteres"
        );

        publisher.publishMessageUnblocked(event);

        verify(notificationClient).notifyMessageUnblocked(event);
    }

    @Test
    void moderationEventPublisherEnqueuesKafkaEventOnModerationEventsTopicKeyedByCourse() {
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);
        ModerationNotificationClient notificationClient = mock(ModerationNotificationClient.class);
        KafkaEventProducer kafkaEventProducer = mock(KafkaEventProducer.class);
        ModerationEventPublisher publisher = new ModerationEventPublisher(springPublisher, notificationClient, kafkaEventProducer);

        ModerationResolutionDomainEvent event = ModerationResolutionDomainEvent.ofReversed(
                "msg-3", UUID.randomUUID(), "curso-42", "user-3", "prof-1", "Motivo de prueba con más de veinte caracteres"
        );

        publisher.publishMessageUnblocked(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaEventProducer).enqueue(
                org.mockito.ArgumentMatchers.eq(KafkaTopics.MODERATION_EVENTS),
                org.mockito.ArgumentMatchers.eq("curso-42"),
                org.mockito.ArgumentMatchers.eq("MESSAGE_UNBLOCKED"),
                payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("messageId")).isEqualTo("msg-3");
        assertThat(payload.get("courseId")).isEqualTo("curso-42");
        assertThat(payload.get("resolution")).isEqualTo("REVERSED");
    }
}
