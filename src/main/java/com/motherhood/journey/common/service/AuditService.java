package com.motherhood.journey.common.service;

import com.motherhood.journey.common.entity.AuditLog;
import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.repository.AuditLogRepository;
import com.motherhood.journey.identity.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Records a successful audit event in a separate transaction so the entry
     * is always persisted even if the caller's transaction rolls back.
     * Not async: SecurityContextHolder is thread-local and would be empty in a
     * new thread, causing resolveUser() to silently return empty.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String resourceType, UUID resourceId) {
        log(action, resourceType, resourceId, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String resourceType, UUID resourceId,
                    String ipAddress, String userAgent) {
        resolveUser().ifPresent(user ->
            auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action.name())
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent, 512))
                .success(true)
                .build())
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(AuditAction action, String resourceType, UUID resourceId, String reason) {
        resolveUser().ifPresent(user ->
            auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action.name())
                .resourceType(resourceType)
                .resourceId(resourceId)
                .success(false)
                .failReason(truncate(reason, 128))
                .build())
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private java.util.Optional<com.motherhood.journey.identity.entity.User> resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        return userRepository.findByPhoneNumber(auth.getName());
    }
}
