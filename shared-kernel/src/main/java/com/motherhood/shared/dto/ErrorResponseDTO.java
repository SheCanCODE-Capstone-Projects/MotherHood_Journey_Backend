package com.motherhood.shared.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponseDTO {

    private Instant timestamp;
    private String traceId;
    private int status;
    private String error;
    private String message;
}
