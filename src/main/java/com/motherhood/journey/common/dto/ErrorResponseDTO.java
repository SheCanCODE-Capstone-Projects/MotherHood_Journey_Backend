package com.motherhood.journey.common.dto;

<<<<<<< Updated upstream
public class ErrorResponseDTO {
=======
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class ErrorResponseDTO {
    private Instant timestamp;
    private String traceId;
    private int status;
    private String error;
    private String message;
>>>>>>> Stashed changes
}
