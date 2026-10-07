package com.proyecto.pedidos.exception;

import com.proyecto.pedidos.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            HttpServletRequest request) {

        return construir(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(
            ConflictoException ex,
            HttpServletRequest request) {

        return construir(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> manejarNegocio(
            NegocioException ex,
            HttpServletRequest request) {

        return construir(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ServicioIaException.class)
    public ResponseEntity<ErrorResponse> manejarServicioIa(
            ServicioIaException ex,
            HttpServletRequest request) {

        return construir(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ServicioInstagramException.class)
    public ResponseEntity<ErrorResponse> manejarInstagram(
            ServicioInstagramException ex,
            HttpServletRequest request) {

        return construir(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                ex.getMessage(),
                request
        );
    }

    private ResponseEntity<ErrorResponse> construir(
            HttpStatus status,
            String error,
            String mensaje,
            HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                mensaje,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }
}
