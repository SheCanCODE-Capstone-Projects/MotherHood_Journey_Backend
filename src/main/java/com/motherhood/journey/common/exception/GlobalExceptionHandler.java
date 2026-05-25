package com.motherhood.journey.common.exception;

import com.motherhood.journey.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        // Field names and default messages come from annotations — safe to reflect
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation error: " + message));
    }

    /** Handles @Validated constraint violations on @RequestParam / @PathVariable */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        // Property paths and messages come from annotations — safe to reflect
        String message = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Constraint violation");
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation error: " + message));
    }

    /**
     * Handles invalid enum values passed as @RequestParam (e.g. unknown FacilityType).
     * ex.getValue() is user-supplied — sanitized before reflection to prevent log injection.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String safeValue = sanitize(String.valueOf(ex.getValue()));
        String safeName  = sanitize(ex.getName());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Invalid value '" + safeValue + "' for parameter '" + safeName + "'"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        // Log internally with a correlation ID — never expose ex.getMessage() to the client
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled exception [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("An unexpected error occurred. Reference: " + correlationId));
    }

    /**
     * Strips newline and carriage-return characters from user-supplied strings
     * before they are reflected into responses or logs, preventing log injection (CWE-117).
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\r\n]", "").trim();
    }
}
