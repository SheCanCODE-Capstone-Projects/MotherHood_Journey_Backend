package com.motherhood.maternal.application.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PregnancyDTO{


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OpenRequest {
        private LocalDate lmpDate;
        private Integer   gravida;
        private Integer   para;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CloseRequest {
        private String status;
        private String outcomeNotes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignChwRequest {
        private UUID assignedChwId;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID        id;
        private UUID        motherId;
        private LocalDate   lmpDate;
        private LocalDate   edd;
        private String      status;
        private Integer     gravida;
        private Integer     para;
        private UUID        assignedChwId;
        private String      outcomeNotes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}