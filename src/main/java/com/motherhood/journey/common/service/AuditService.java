package com.motherhood.journey.common.service;

import com.motherhood.journey.common.Repository.AuditLogRepository;
import com.motherhood.journey.common.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    /**
     * Basic audit log (used in filters like JwtAuthFilter)
     */
    @Async
    public void logBasic(String action, String detail, String path, String traceId) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    // store the human-readable detail in failReason
                    .failReason(detail)
                    .ipAddress(null)
                    .user(null)
                    .build();

            auditLogRepository.save(entry);

        } catch (Exception e) {
            logger.error("Failed to write basic audit log: action={}, traceId={}", action, traceId, e);
        }
    }

    /**
     * Full audit log (used in aspects like AuditAspect)
     */
    @Async
    public void logFull(String action,
                        String resourceType,
                        String resourceId,
                        String performedBy,
                        String facilityId,
                        String clientIp,
                        String userAgent,
                        String path,
                        String traceId) {
        try {
            java.util.UUID resUuid = null;
            if (resourceId != null) {
                try {
                    resUuid = java.util.UUID.fromString(resourceId);
                } catch (IllegalArgumentException ignored) {
                }
            }

            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resUuid)
                    .ipAddress(clientIp)
                    .userAgent(userAgent)
                    .user(null)
                    .build();

            auditLogRepository.save(entry);

        } catch (Exception e) {
            logger.error(
                    "Failed to write full audit log: action={}, resource={}/{}, performedBy={}",
                    action, resourceType, resourceId, performedBy, e
            );
        }
    }

    // Backwards-compatible log overload used across the codebase
    @Async
    public void log(String action, String detail, String path, String traceId) {
        logBasic(action, detail, path, traceId);
    }

    // Full log overload matching older call sites (keeps the async behaviour)
    @Async
    public void log(String action,
                    String resourceType,
                    String resourceId,
                    String performedBy,
                    String facilityId,
                    String clientIp,
                    String userAgent,
                    String path,
                    String traceId) {
        logFull(action, resourceType, resourceId, performedBy, facilityId, clientIp, userAgent, path, traceId);
    }
}