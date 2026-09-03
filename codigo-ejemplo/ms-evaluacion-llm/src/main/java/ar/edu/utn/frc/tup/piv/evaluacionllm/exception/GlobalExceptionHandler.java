package ar.edu.utn.frc.tup.piv.evaluacionllm.exception;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        ErrorResponse error = ErrorResponse.builder()
                .error("recurso_no_encontrado")
                .mensaje(ex.getMessage())
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .error("validacion_fallida")
                .mensaje("Los datos de entrada no cumplen con las validaciones requeridas.")
                .detalles(detalles)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        ErrorResponse error = ErrorResponse.builder()
                .error("argumento_invalido")
                .mensaje(ex.getMessage())
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGenerico(Exception ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        log.error("Error no controlado procesando petición (traceId: {}): ", traceId, ex);
        ErrorResponse error = ErrorResponse.builder()
                .error("error_interno")
                .mensaje("Ocurrió un error inesperado al procesar la solicitud.")
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
