package com.motherhood.journey.common.exception;

import com.motherhood.journey.common.dto.ErrorResponseDTO;
import com.motherhood.journey.common.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String traceId = newTraceId();
        log.warn("[{}] Resource not found – {} | path={}", traceId, ex.getMessage(), request.getRequestURI());
        auditService.log("RESOURCE_NOT_FOUND", ex.getMessage(), request.getRequestURI(), traceId);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(Instant.now())
                .traceId(traceId)
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {

        String traceId = newTraceId();
        log.warn("[{}] Authorisation denied – {} | path={}", traceId, ex.getMessage(), request.getRequestURI());
        auditService.log("AUTHORISATION_DENIED", ex.getMessage(), request.getRequestURI(), traceId);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(Instant.now())
                .traceId(traceId)
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = newTraceId();

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage(),
                        (first, second) -> first
                ));

        String firstMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");

        log.warn("[{}] Validation failed | path={} | errors={}", traceId, request.getRequestURI(), fieldErrors);
        auditService.log("VALIDATION_FAILED", firstMessage, request.getRequestURI(), traceId);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(Instant.now())
                .traceId(traceId)
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(firstMessage)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        String traceId = newTraceId();
        log.error("[{}] Unhandled exception | path={}", traceId, request.getRequestURI(), ex);
        auditService.log("INTERNAL_ERROR", ex.getClass().getSimpleName(), request.getRequestURI(), traceId);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(Instant.now())
                .traceId(traceId)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please contact support with traceId: " + traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
