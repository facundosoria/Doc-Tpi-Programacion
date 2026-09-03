package ar.edu.utn.frc.tup.piv.evaluacionllm.exception;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-test-123");
    }

    @Test
    void manejarRecursoNoEncontrado() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("No existe");
        ResponseEntity<ErrorResponse> response = handler.manejarRecursoNoEncontrado(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("recurso_no_encontrado", response.getBody().getError());
        assertEquals("trace-test-123", response.getBody().getTraceId());
    }

    @Test
    void manejarArgumentoInvalido() {
        IllegalArgumentException ex = new IllegalArgumentException("Dato inválido");
        ResponseEntity<ErrorResponse> response = handler.manejarArgumentoInvalido(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("argumento_invalido", response.getBody().getError());
    }

    @Test
    void manejarErrorGenerico() {
        RuntimeException ex = new RuntimeException("Error inesperado");
        ResponseEntity<ErrorResponse> response = handler.manejarErrorGenerico(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("error_interno", response.getBody().getError());
    }
}
