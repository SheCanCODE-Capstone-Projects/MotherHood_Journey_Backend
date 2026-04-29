package com.motherhood.shared.audit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    // Simple 4-arg version (used by JwtAuthFilter)
    @Async
    public void log(String action, String detail, String path, String traceId) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .detail(detail)
                    .path(path)
                    .traceId(traceId)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log entry: action={} traceId={}", action, traceId, e);
        }
    }

    // Full 9-arg version (used by AuditAspect)
    @Async
    public void log(String action, String resourceType, String resourceId,
                    String performedBy, String facilityId,
                    String clientIp, String userAgent,
                    String path, String traceId) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .performedBy(performedBy)
                    .facilityId(facilityId)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .path(path)
                    .traceId(traceId)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: action={} resource={}/{} by={}",
                    action, resourceType, resourceId, performedBy, e);
        }
    }
}