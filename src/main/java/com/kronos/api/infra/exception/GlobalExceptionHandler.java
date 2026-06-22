package com.kronos.api.infra.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura erros de recursos não encontrados (Ex: Buscar um grupo ou tarefa que não existe)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CustomErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(404);
        CustomErrorResponse error = new CustomErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Captura erros de credenciais incorretas (Ex: Senha ou e-mail errados no login)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CustomErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(401);
        CustomErrorResponse error = new CustomErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "Unauthorized",
                "Invalid email or password",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Captura violações de integridade do banco (Ex: Tentar cadastrar um e-mail que já existe)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(400);
        CustomErrorResponse error = new CustomErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "Database Conflict",
                "This resource already exists or violates a database constraint",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Captura erros de validação de campos (Ex: Deixar o título da tarefa em branco ou e-mail inválido)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CustomErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(403);
        CustomErrorResponse error = new CustomErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(422);

        // Junta todas as mensagens de erro dos campos em uma única String legível
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        CustomErrorResponse error = new CustomErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "Validation Error",
                errors,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Captura qualquer outro erro genérico e inesperado do sistema (Fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(500);
        CustomErrorResponse error = new CustomErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
