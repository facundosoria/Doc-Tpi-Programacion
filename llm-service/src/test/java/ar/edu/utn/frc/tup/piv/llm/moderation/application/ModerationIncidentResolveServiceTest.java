package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationResolutionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ResolveIncidentCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationEventPublisherPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationIncidentResolveServiceTest {

    private static final String REASON = "Motivo suficientemente largo para validar.";
    private static final String APPEAL_REASON = "Motivo de apelacion suficientemente largo.";

    private ModerationIncidentRepositoryPort incidents;
    private ModerationResolutionRepositoryPort resolutions;
    private ModerationAppealRepositoryPort appeals;
    private ModerationEventPublisherPort publisher;
    private ModerationIncidentResolveService service;
    private UUID incidentId;
    private ModerationIncident incident;

    @BeforeEach
    void setUp() {
        incidents = mock(ModerationIncidentRepositoryPort.class);
        resolutions = mock(ModerationResolutionRepositoryPort.class);
        appeals = mock(ModerationAppealRepositoryPort.class);
        publisher = mock(ModerationEventPublisherPort.class);
        service = new ModerationIncidentResolveService(incidents, resolutions, appeals, publisher);
        incidentId = UUID.randomUUID();
        incident = ModerationIncident.ofBlock(incidentId, "msg-1", "student-1", "course-1", "PROFANITY", "preview");
        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        when(appeals.findByIncidentId(incidentId)).thenReturn(Optional.empty());
    }

    private void expectStatus(ResolveIncidentCommand cmd, HttpStatus status) {
        assertThatThrownBy(() -> service.resolve(cmd))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(status));
        verify(resolutions, never()).save(any());
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.resolve(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsMissingIncidentIdWith400() {
        expectStatus(new ResolveIncidentCommand(null, "CONFIRMED", REASON, "doc"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsBlankResolverWith401() {
        expectStatus(new ResolveIncidentCommand(incidentId, "CONFIRMED", REASON, " "), HttpStatus.UNAUTHORIZED);
        expectStatus(new ResolveIncidentCommand(incidentId, "CONFIRMED", REASON, null), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidReasonLengthWith400() {
        expectStatus(new ResolveIncidentCommand(incidentId, "CONFIRMED", null, "doc"), HttpStatus.BAD_REQUEST);
        expectStatus(new ResolveIncidentCommand(incidentId, "CONFIRMED", "corto", "doc"), HttpStatus.BAD_REQUEST);
        expectStatus(new ResolveIncidentCommand(incidentId, "CONFIRMED", "x".repeat(501), "doc"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsMissingOrUnknownResolutionWith400() {
        expectStatus(new ResolveIncidentCommand(incidentId, null, REASON, "doc"), HttpStatus.BAD_REQUEST);
        expectStatus(new ResolveIncidentCommand(incidentId, "  ", REASON, "doc"), HttpStatus.BAD_REQUEST);
        expectStatus(new ResolveIncidentCommand(incidentId, "MAYBE", REASON, "doc"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownIncidentYields404() {
        UUID missing = UUID.randomUUID();
        when(incidents.findById(missing)).thenReturn(Optional.empty());
        expectStatus(new ResolveIncidentCommand(missing, "CONFIRMED", REASON, "doc"), HttpStatus.NOT_FOUND);
    }

    @Test
    void alreadyResolvedIncidentYields409() {
        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident.resolve("CONFIRMED")));
        expectStatus(new ResolveIncidentCommand(incidentId, "REVERSED", REASON, "doc"), HttpStatus.CONFLICT);
    }

    @Test
    void existingResolutionRecordYields409EvenIfIncidentStatusIsBlock() {
        when(resolutions.existsByIncidentId(incidentId)).thenReturn(true);
        expectStatus(new ResolveIncidentCommand(incidentId, "CONFIRMED", REASON, "doc"), HttpStatus.CONFLICT);
        verify(incidents, never()).save(any());
    }

    @Test
    void confirmedResolutionPersistsAndConfirmsAppealWithoutEvent() {
        ModerationAppeal appeal = ModerationAppeal.create(incidentId, "student-1", APPEAL_REASON);
        when(appeals.findByIncidentId(incidentId)).thenReturn(Optional.of(appeal));

        ModerationResolutionResult result = service.resolve(
                new ResolveIncidentCommand(incidentId, " confirmed ", "  " + REASON + " ", " doc "));

        assertThat(result.resolution()).isEqualTo("CONFIRMED");
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.resolvedBy()).isEqualTo("doc");
        assertThat(result.resolutionReason()).isEqualTo(REASON);
        assertThat(result.incidentId()).isEqualTo(incidentId);
        verify(resolutions).save(any(ModerationResolution.class));
        ArgumentCaptor<ModerationIncident> inc = ArgumentCaptor.forClass(ModerationIncident.class);
        verify(incidents).save(inc.capture());
        assertThat(inc.getValue().getStatus()).isEqualTo("CONFIRMED");
        ArgumentCaptor<ModerationAppeal> ap = ArgumentCaptor.forClass(ModerationAppeal.class);
        verify(appeals).save(ap.capture());
        assertThat(ap.getValue().getStatus()).isEqualTo(ModerationAppealStatus.CONFIRMED);
        verify(publisher, never()).publishMessageUnblocked(any());
    }

    @Test
    void reversedResolutionReversesAppealAndPublishesUnblockEvent() {
        ModerationAppeal appeal = ModerationAppeal.create(incidentId, "student-1", APPEAL_REASON);
        when(appeals.findByIncidentId(incidentId)).thenReturn(Optional.of(appeal));

        ModerationResolutionResult result = service.resolve(
                new ResolveIncidentCommand(incidentId, "REVERSED", REASON, "doc"));

        assertThat(result.status()).isEqualTo("REVERSED");
        ArgumentCaptor<ModerationAppeal> ap = ArgumentCaptor.forClass(ModerationAppeal.class);
        verify(appeals).save(ap.capture());
        assertThat(ap.getValue().getStatus()).isEqualTo(ModerationAppealStatus.REVERSED);
        ArgumentCaptor<ModerationResolutionDomainEvent> ev = ArgumentCaptor.forClass(ModerationResolutionDomainEvent.class);
        verify(publisher).publishMessageUnblocked(ev.capture());
        assertThat(ev.getValue()).isNotNull();
    }

    @Test
    void reversedWithoutAppealDoesNotTouchAppeals() {
        service.resolve(new ResolveIncidentCommand(incidentId, "REVERSED", REASON, "doc"));

        verify(appeals, never()).save(any());
        verify(publisher).publishMessageUnblocked(any());
    }
}
