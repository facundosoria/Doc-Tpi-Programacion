package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.CreateModerationAppealCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.exception.AppealAlreadyExistsException;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationAppealServiceTest {

    private ModerationAppealRepositoryPort appealRepository;
    private ModerationIncidentRepositoryPort incidentRepository;
    private ModerationAppealService service;

    @BeforeEach
    void setUp() {
        appealRepository = mock(ModerationAppealRepositoryPort.class);
        incidentRepository = mock(ModerationIncidentRepositoryPort.class);
        service = new ModerationAppealService(appealRepository, incidentRepository);
    }

    @Test
    void createAppealSuccessWhenIncidentIsOwnedAndBlock() {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";
        ModerationIncident incident = ModerationIncident.ofBlock(incidentId, "msg-001", userId, "curso-42", "CODE_OBFUSCATION", "preview");

        when(appealRepository.findByIncidentId(incidentId)).thenReturn(Optional.empty());
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(appealRepository.save(any(ModerationAppeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateModerationAppealCommand command = new CreateModerationAppealCommand(incidentId, userId, reason);
        ModerationAppeal result = service.createAppeal(command);

        assertThat(result).isNotNull();
        assertThat(result.getIncidentId()).isEqualTo(incidentId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAppealReason()).isEqualTo(reason);
        assertThat(result.getStatus()).isEqualTo(ModerationAppealStatus.PENDING_REVIEW);
        verify(appealRepository).save(any(ModerationAppeal.class));
    }

    @Test
    void createAppealThrows409ConflictWhenAppealAlreadyExists() {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";
        ModerationAppeal existing = ModerationAppeal.create(incidentId, userId, reason);

        when(appealRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(existing));

        CreateModerationAppealCommand command = new CreateModerationAppealCommand(incidentId, userId, reason);
        assertThatThrownBy(() -> service.createAppeal(command))
                .isInstanceOf(AppealAlreadyExistsException.class)
                .satisfies(ex -> {
                    AppealAlreadyExistsException aee = (AppealAlreadyExistsException) ex;
                    assertThat(aee.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(aee.getExistingAppealId()).isEqualTo(existing.getId());
                });

        verify(incidentRepository, never()).findById(any());
        verify(appealRepository, never()).save(any());
    }

    @Test
    void createAppealThrows404NotFoundWhenIncidentDoesNotExist() {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";

        when(appealRepository.findByIncidentId(incidentId)).thenReturn(Optional.empty());
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        CreateModerationAppealCommand command = new CreateModerationAppealCommand(incidentId, userId, reason);
        assertThatThrownBy(() -> service.createAppeal(command))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(appealRepository, never()).save(any());
    }

    @Test
    void createAppealThrows422UnprocessableEntityWhenIncidentNotBlock() {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";
        ModerationIncident incident = new ModerationIncident(incidentId, "msg-001", userId, "curso-42", "ALLOW", "CLEAN", null, null);

        when(appealRepository.findByIncidentId(incidentId)).thenReturn(Optional.empty());
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        CreateModerationAppealCommand command = new CreateModerationAppealCommand(incidentId, userId, reason);
        assertThatThrownBy(() -> service.createAppeal(command))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(appealRepository, never()).save(any());
    }

    @Test
    void createAppealThrows403ForbiddenAntiIdorWhenIncidentBelongsToOtherUser() {
        UUID incidentId = UUID.randomUUID();
        String authenticatedUser = "user-55";
        String actualIncidentOwner = "user-99";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";
        ModerationIncident incident = ModerationIncident.ofBlock(incidentId, "msg-001", actualIncidentOwner, "curso-42", "CODE_OBFUSCATION", "preview");

        when(appealRepository.findByIncidentId(incidentId)).thenReturn(Optional.empty());
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        CreateModerationAppealCommand command = new CreateModerationAppealCommand(incidentId, authenticatedUser, reason);
        assertThatThrownBy(() -> service.createAppeal(command))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(appealRepository, never()).save(any());
    }

    @Test
    void createAppealThrows400BadRequestWhenReasonTooShort() {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String shortReason = "Demasiado corto";

        CreateModerationAppealCommand command = new CreateModerationAppealCommand(incidentId, userId, shortReason);
        assertThatThrownBy(() -> service.createAppeal(command))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(appealRepository, never()).save(any());
    }

    @Test
    void getAppealSuccessWhenOwnedByAuthenticatedUser() {
        UUID appealId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        ModerationAppeal appeal = ModerationAppeal.create(incidentId, userId, "Motivo válido de longitud suficiente.");

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

        ModerationAppeal result = service.getAppeal(appealId, userId);
        assertThat(result).isEqualTo(appeal);
    }

    @Test
    void getAppealThrows403ForbiddenWhenQueriedByOtherUser() {
        UUID appealId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        String owner = "user-55";
        String otherUser = "user-99";
        ModerationAppeal appeal = ModerationAppeal.create(incidentId, owner, "Motivo válido de longitud suficiente.");

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

        assertThatThrownBy(() -> service.getAppeal(appealId, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAppealThrows404NotFoundWhenAppealDoesNotExist() {
        UUID appealId = UUID.randomUUID();
        when(appealRepository.findById(appealId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAppeal(appealId, "user-55"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
