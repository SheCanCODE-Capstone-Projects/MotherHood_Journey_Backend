package com.motherhood.journey.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for application-level errors
 * Used to throw business rule violations with appropriate HTTP status codes
 */
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public CustomException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

