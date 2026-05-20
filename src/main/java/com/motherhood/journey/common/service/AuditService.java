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
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String resourceType, UUID resourceId) {
        resolveUser().ifPresent(user ->
            auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action.name())
                .resourceType(resourceType)
                .resourceId(resourceId)
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
                .failReason(reason != null && reason.length() > 128 ? reason.substring(0, 128) : reason)
                .build())
        );
    }

    private java.util.Optional<com.motherhood.journey.identity.entity.User> resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return java.util.Optional.empty();
        return userRepository.findByPhoneNumber(auth.getName());
    }
}
