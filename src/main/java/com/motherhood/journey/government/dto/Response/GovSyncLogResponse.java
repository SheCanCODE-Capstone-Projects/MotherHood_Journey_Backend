package com.motherhood.journey.government.dto.Response;

import com.motherhood.journey.government.entity.GovSyncLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record GovSyncLogResponse(
        UUID id,
        UUID facilityId,
        String targetSystem,
        String syncType,
        String status,
        String idempotencyKey,
        String payloadHash,
        Integer retryCount,
        String errorMessage,
        LocalDateTime syncedAt,
        LocalDateTime nextRetryAt,
        LocalDateTime createdAt
) {
    public static GovSyncLogResponse from(GovSyncLog log) {
        return new GovSyncLogResponse(
                log.getId(),
                log.getFacility() != null ? log.getFacility().getId() : null,
                log.getTargetSystem(),
                log.getSyncType(),
                log.getStatus(),
                log.getIdempotencyKey(),
                log.getPayloadHash(),
                log.getRetryCount(),
                log.getErrorMessage(),
                log.getSyncedAt(),
                log.getNextRetryAt(),
                log.getCreatedAt()
        );
    }
}
