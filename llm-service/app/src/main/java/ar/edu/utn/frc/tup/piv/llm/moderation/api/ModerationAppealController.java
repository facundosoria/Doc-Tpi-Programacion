package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.CreateModerationAppealRequest;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationAppealResponse;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.mapper.ModerationAppealApiMapper;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.CreateModerationAppealCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationAppealUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para el flujo de apelación de moderación por parte de los alumnos (LLM-S12-H01).
 * Expone POST /moderation/v1/appeals y GET /moderation/v1/appeals/{appealId}.
 */
@RestController
public class ModerationAppealController {

    private final ModerationAppealUseCase useCase;

    public ModerationAppealController(ModerationAppealUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping({"/moderation/v1/appeals", "${app.api.private-path:/api/llm}/moderation/v1/appeals"})
    public ResponseEntity<ModerationAppealResponse> createAppeal(
            @Valid @RequestBody CreateModerationAppealRequest request,
            @RequestHeader(required = false) HttpHeaders headers) {

        String userId = resolveUserId(headers);
        CreateModerationAppealCommand command = ModerationAppealApiMapper.toCommand(request, userId);
        ModerationAppeal created = useCase.createAppeal(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ModerationAppealApiMapper.toResponse(created));
    }

    @GetMapping({"/moderation/v1/appeals/{appealId}", "${app.api.private-path:/api/llm}/moderation/v1/appeals/{appealId}"})
    public ResponseEntity<ModerationAppealResponse> getAppeal(
            @PathVariable UUID appealId,
            @RequestHeader(required = false) HttpHeaders headers) {

        String userId = resolveUserId(headers);
        ModerationAppeal appeal = useCase.getAppeal(appealId, userId);

        return ResponseEntity.ok(ModerationAppealApiMapper.toResponse(appeal));
    }

    private String resolveUserId(HttpHeaders headers) {
        if (headers != null) {
            String headerUserId = headers.getFirst(IdentityHeaders.USER_ID);
            if (headerUserId != null && !headerUserId.isBlank()) {
                return headerUserId.trim();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getName() != null
                && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            return auth.getName().trim();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
    }
}
