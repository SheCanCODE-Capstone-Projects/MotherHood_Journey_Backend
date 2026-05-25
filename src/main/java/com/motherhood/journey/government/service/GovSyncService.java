package com.motherhood.journey.government.service;

import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.enums.TargetSystem;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import com.motherhood.journey.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GovSyncService {

    private static final int MAX_RETRIES = 5;

    private final GovSyncLogRepository syncLogRepository;
    private final GovNidaApiClient nidaApiClient;
    private final IremboApiClient iremboApiClient;
    private final HmisApiClient hmisApiClient;
    private final NotificationService notificationService;

    /**
     * Called by the scheduler every 2 minutes.
     * Picks up PENDING rows whose next_retry_at has passed and processes them.
     */
    @Transactional
    public void processPendingEntries() {
        List<GovSyncLog> pending = syncLogRepository
                .findByStatusAndNextRetryAtLessThanEqualAndDeadLetterFalse(
                        SyncStatus.PENDING, OffsetDateTime.now());

        log.info("GovSyncService: found {} pending entries to process", pending.size());

        for (GovSyncLog entry : pending) {
            processEntry(entry);
        }
    }

    private void processEntry(GovSyncLog entry) {
        // Mark as in-flight so parallel runs don't pick up the same row
        entry.setStatus(SyncStatus.IN_FLIGHT);
        syncLogRepository.save(entry);

        try {
            dispatchToTargetSystem(entry);
            entry.setStatus(SyncStatus.SUCCEEDED);
            entry.setLastError(null);
            log.info("GovSync SUCCEEDED: id={} target={} ref={}",
                    entry.getId(), entry.getTargetSystem(), entry.getReferenceNo());

        } catch (Exception e) {
            handleFailure(entry, e);
        }

        syncLogRepository.save(entry);
    }

    private void dispatchToTargetSystem(GovSyncLog entry) {
        switch (entry.getTargetSystem()) {
            case NIDA -> {
                Map<String, Object> p = entry.getPayload();
                nidaApiClient.verifyIdentity(
                        (String) p.get("nationalId"),
                        (String) p.get("firstName"),
                        (String) p.get("lastName")
                );
            }
            case IREMBO -> {
                iremboApiClient.submitServiceRequest(
                        entry.getReferenceNo(),
                        entry.getPayload()
                );
            }
            case HMIS -> {
                hmisApiClient.pushReport(
                        entry.getReferenceNo(),
                        entry.getPayload()
                );
            }
        }
    }

    private void handleFailure(GovSyncLog entry, Exception e) {
        int retries = entry.getRetryCount() + 1;
        entry.setRetryCount(retries);
        entry.setLastError(e.getMessage());

        if (retries >= MAX_RETRIES) {
            entry.setStatus(SyncStatus.DEAD_LETTER);
            entry.setDeadLetter(true);
            log.error("GovSync DEAD_LETTER: id={} target={} ref={} after {} retries",
                    entry.getId(), entry.getTargetSystem(), entry.getReferenceNo(), retries);
            sendDeadLetterAlert(entry);
        } else {
            // Exponential backoff: 2^retryCount minutes
            long backoffMinutes = (long) Math.pow(2, retries);
            entry.setStatus(SyncStatus.PENDING);
            entry.setNextRetryAt(OffsetDateTime.now().plusMinutes(backoffMinutes));
            log.warn("GovSync FAILED (retry {}/{}): id={} next_retry_at=+{}min error={}",
                    retries, MAX_RETRIES, entry.getId(), backoffMinutes, e.getMessage());
        }
    }

    /**
     * Creates a new outbox entry. Always call this BEFORE making any external API call.
     * The scheduler will pick it up and execute the actual call.
     */
    @Transactional
    public GovSyncLog enqueue(TargetSystem targetSystem,
                              String syncType,
                              String referenceNo,
                              String idempotencyKey,
                              Map<String, Object> payload) {

        // Idempotency guard: if already enqueued, return existing entry
        return syncLogRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    GovSyncLog entry = GovSyncLog.builder()
                            .idempotencyKey(idempotencyKey)
                            .targetSystem(targetSystem)
                            .syncType(syncType)
                            .referenceNo(referenceNo)
                            .payload(payload)
                            .status(SyncStatus.PENDING)
                            .nextRetryAt(OffsetDateTime.now()) // process immediately
                            .build();
                    return syncLogRepository.save(entry);
                });
    }

    private void sendDeadLetterAlert(GovSyncLog entry) {
        String message = String.format(
                "[ALERT] Gov sync DEAD_LETTER: system=%s type=%s ref=%s id=%s",
                entry.getTargetSystem(), entry.getSyncType(),
                entry.getReferenceNo(), entry.getId()
        );
        // Enqueue SMS to all MOH_ADMIN users — implementation delegates to NotificationService
        notificationService.sendAdminAlert(message);
    }
}
