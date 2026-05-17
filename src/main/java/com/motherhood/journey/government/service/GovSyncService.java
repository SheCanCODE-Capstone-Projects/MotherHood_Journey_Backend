package com.motherhood.journey.government.service;

import com.motherhood.journey.government.dto.Response.GovSyncLogResponse;
import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.notification.enums.NotificationType;
import com.motherhood.journey.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovSyncService {

    static final int MAX_RETRIES = 5;

    private final GovSyncLogRepository govSyncLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void recordFailure(UUID logId, String errorMessage) {
        GovSyncLog syncLog = findByIdOrThrow(logId);

        syncLog.setRetryCount(syncLog.getRetryCount() + 1);
        syncLog.setErrorMessage(errorMessage);

        if (syncLog.getRetryCount() >= MAX_RETRIES) {
            syncLog.setStatus(SyncStatus.DEAD_LETTER.name());
            syncLog.setNextRetryAt(null);
            govSyncLogRepository.save(syncLog);

            alertMohAdmins(syncLog);

            log.error("GovSyncLog {} marked DEAD_LETTER after {} retries — type={} system={}",
                    syncLog.getId(), syncLog.getRetryCount(),
                    syncLog.getSyncType(), syncLog.getTargetSystem());
        } else {
            syncLog.setStatus(SyncStatus.FAILED.name());
            syncLog.setNextRetryAt(LocalDateTime.now().plusMinutes(30L * syncLog.getRetryCount()));
            govSyncLogRepository.save(syncLog);
        }
    }

    @Transactional(readOnly = true)
    public List<GovSyncLogResponse> getAll(String status, String targetSystem) {
        List<GovSyncLog> results;

        if (status != null && targetSystem != null) {
            results = govSyncLogRepository.findByStatusAndTargetSystem(status, targetSystem);
        } else if (status != null) {
            results = govSyncLogRepository.findByStatus(status);
        } else if (targetSystem != null) {
            results = govSyncLogRepository.findByTargetSystem(targetSystem);
        } else {
            results = govSyncLogRepository.findAll();
        }

        return results.stream().map(GovSyncLogResponse::from).toList();
    }

    @Transactional
    public GovSyncLogResponse retry(UUID logId) {
        GovSyncLog syncLog = findByIdOrThrow(logId);

        if (!SyncStatus.DEAD_LETTER.name().equals(syncLog.getStatus())
                && !SyncStatus.FAILED.name().equals(syncLog.getStatus())) {
            throw new IllegalStateException(
                    "Only DEAD_LETTER or FAILED entries can be manually retried. " +
                            "Current status: " + syncLog.getStatus());
        }

        syncLog.setStatus(SyncStatus.PENDING.name());
        syncLog.setRetryCount(0);
        syncLog.setErrorMessage(null);
        syncLog.setNextRetryAt(null);

        GovSyncLog saved = govSyncLogRepository.save(syncLog);
        log.info("GovSyncLog {} manually retried by MOH_ADMIN", logId);

        return GovSyncLogResponse.from(saved);
    }

    // private helpers

    private void alertMohAdmins(GovSyncLog syncLog) {
        List<User> admins = userRepository.findByRole(UserRole.MOH_ADMIN);

        if (admins.isEmpty()) {
            log.warn("No MOH_ADMIN users found to alert for DEAD_LETTER sync log {}",
                    syncLog.getId());
            return;
        }

        String message = String.format(
                "ALERT: Sync DEAD_LETTER. Type: %s, System: %s, Key: %s. Manual retry required.",
                syncLog.getSyncType(),
                syncLog.getTargetSystem(),
                syncLog.getIdempotencyKey()
        );

        for (User admin : admins) {
            notificationService.enqueueRaw(admin, message, NotificationType.SERVICE_STATUS);
            log.info("DEAD_LETTER alert sent to MOH_ADMIN {} — sync log {}",
                    admin.getId(), syncLog.getId());
        }
    }

    private GovSyncLog findByIdOrThrow(UUID id) {
        return govSyncLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "GovSyncLog not found: " + id));
    }
}