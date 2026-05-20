package com.motherhood.journey.common.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuditLogService {

    private final JdbcTemplate jdbc;
    // called by GlobalExceptionHandler
    public void log(String action, String detail, String path, String traceId) {
        try {
            jdbc.update(
                    """
                    INSERT INTO audit_log (action, detail, path, trace_id, created_at)
                    VALUES (?, ?, ?, ?, NOW())
                    """,
                    action, detail, path, traceId
            );
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }
    // called by auth/security layer
    public void logFailure(UUID userId, String action, String resourceType,
                           String reason, String ip, String userAgent) {
        try {
            jdbc.update(
                    """
                    INSERT INTO audit_log (user_id, action, resource_type, detail, ip_address, user_agent, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW())
                    """,
                    userId != null ? userId.toString() : null,
                    action,
                    resourceType,
                    reason,
                    ip,
                    userAgent
            );
        } catch (Exception e) {
            log.error("Failed to write to audit_log: {}", e.getMessage());
        }
        log.warn("Auth failure [action={}, reason={}, ip={}]", action, reason, ip);
    }
}