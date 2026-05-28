package com.motherhood.journey.scheduler;

import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.repository.GovReportRepository;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import com.motherhood.journey.government.service.GovNidaApiClient;
import com.motherhood.journey.government.service.HmisApiClient;
import com.motherhood.journey.government.service.IremboApiClient;
import com.motherhood.journey.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transactional delegate for GovSyncOutboxProcessor.
 * Extracted into a separate bean so Spring's proxy correctly applies
 * @Transactional — self-calls within the same bean bypass the proxy.
 *
 * Routes each outbox entry to the correct external API based on targetSystem:
 *   IREMBO → IremboApiClient
 *   HMIS   → HmisApiClient  (also marks GovReport.hmisPushStatus = PUSHED on success)
 *   NIDA   → GovNidaApiClient
 */
@Component
public class GovSyncOutboxDelegate {

    private static final Logger log = LoggerFactory.getLogger(GovSyncOutboxDelegate.class);
    private static final int MAX_RETRIES = 5;

    private final GovSyncLogRepository govSyncLogRepository;
    private final GovReportRepository  govReportRepository;
    private final NotificationService  notificationService;
    private final IremboApiClient      iremboApiClient;
    private final HmisApiClient        hmisApiClient;
    private final GovNidaApiClient     govNidaApiClient;

    public GovSyncOutboxDelegate(GovSyncLogRepository govSyncLogRepository,
                                  GovReportRepository govReportRepository,
                                  NotificationService notificationService,
                                  IremboApiClient iremboApiClient,
                                  HmisApiClient hmisApiClient,
                                  GovNidaApiClient govNidaApiClient) {
        this.govSyncLogRepository = govSyncLogRepository;
        this.govReportRepository  = govReportRepository;
        this.notificationService  = notificationService;
        this.iremboApiClient      = iremboApiClient;
        this.hmisApiClient        = hmisApiClient;
        this.govNidaApiClient     = govNidaApiClient;
    }

    @Transactional
    public List<GovSyncLog> fetchAndMarkInProgress() {
        List<GovSyncLog> entries = govSyncLogRepository
                .findPendingForDispatch(SyncStatus.PENDING, OffsetDateTime.now());
        entries.forEach(e -> e.setStatus(SyncStatus.IN_FLIGHT));
        govSyncLogRepository.saveAll(entries);
        return entries;
    }

    @Transactional
    public void dispatch(GovSyncLog entry) {
        try {
            String result = switch (entry.getTargetSystem()) {
                case IREMBO -> dispatchToIrembo(entry);
                case HMIS   -> dispatchToHmis(entry);
                case NIDA   -> dispatchToNida(entry);
            };

            entry.setStatus(SyncStatus.SUCCEEDED);
            entry.setSyncedAt(LocalDateTime.now());
            entry.setErrorMessage(null);
            log.info("GovSyncOutboxDelegate: entry {} → {} SUCCEEDED ({})",
                    entry.getId(), entry.getTargetSystem(), result);

        } catch (Exception e) {
            handleFailure(entry, e);
        }
        govSyncLogRepository.save(entry);
    }

    // ── per-system dispatch ──────────────────────────────────────────────────

    private String dispatchToIrembo(GovSyncLog entry) {
        var sr   = entry.getServiceRequest();
        var body = new LinkedHashMap<String, Object>();

        if (entry.getPayload() != null) {
            body.putAll(entry.getPayload());
        }
        if (sr != null) {
            body.put("referenceNo",  sr.getReferenceNo());
            body.put("serviceType",  sr.getServiceType());
            body.put("submittedAt",  sr.getSubmittedAt());
            if (sr.getPayload() != null) {
                body.put("payload", sr.getPayload());
            }
        }
        body.put("syncType", entry.getSyncType());

        String ref = entry.getReferenceNo() != null ? entry.getReferenceNo() : entry.getIdempotencyKey();
        return iremboApiClient.submitServiceRequest(ref, body);
    }

    private String dispatchToHmis(GovSyncLog entry) {
        var payload = entry.getPayload() != null
                ? new LinkedHashMap<>(entry.getPayload())
                : new LinkedHashMap<String, Object>();

        String result = hmisApiClient.pushReport(entry.getIdempotencyKey(), payload);

        // Mark the linked GovReport as PUSHED (referenceNo stores the report UUID)
        if (entry.getReferenceNo() != null) {
            try {
                UUID reportId = UUID.fromString(entry.getReferenceNo());
                govReportRepository.findById(reportId).ifPresent(report -> {
                    report.setHmisPushStatus("PUSHED");
                    report.setPushedAt(LocalDateTime.now());
                    govReportRepository.save(report);
                });
            } catch (IllegalArgumentException ignored) {
                // referenceNo is not a UUID — no report to update
            }
        }
        return result;
    }

    private String dispatchToNida(GovSyncLog entry) {
        Map<String, Object> payload = entry.getPayload() != null ? entry.getPayload() : Map.of();
        String nationalId = String.valueOf(payload.getOrDefault("nationalId", ""));
        String firstName  = String.valueOf(payload.getOrDefault("firstName",  ""));
        String lastName   = String.valueOf(payload.getOrDefault("lastName",   ""));
        return govNidaApiClient.verifyIdentity(nationalId, firstName, lastName);
    }

    // ── failure handling ─────────────────────────────────────────────────────

    private void handleFailure(GovSyncLog entry, Exception e) {
        int retries = entry.getRetryCount() + 1;
        entry.setRetryCount(retries);
        entry.setErrorMessage(truncate(e.getMessage(), 500));

        if (retries >= MAX_RETRIES) {
            entry.setStatus(SyncStatus.DEAD_LETTER);
            entry.setDeadLetter(true);
            log.error("GovSyncOutboxDelegate: entry {} DEAD_LETTER after {} retries — {}",
                    entry.getId(), retries, e.getMessage(), e);
            try {
                notificationService.sendAdminAlert(String.format(
                        "GovSync DEAD_LETTER: entry %s (target=%s, retries=%d) — %s",
                        entry.getId(), entry.getTargetSystem(), retries, e.getMessage()));
            } catch (Exception alertEx) {
                log.error("Failed to enqueue DEAD_LETTER admin alert for {}: {}",
                        entry.getId(), alertEx.getMessage(), alertEx);
            }
        } else {
            entry.setStatus(SyncStatus.PENDING);
            entry.setNextRetryAt(OffsetDateTime.now().plusMinutes((long) Math.pow(2, retries)));
            log.warn("GovSyncOutboxDelegate: entry {} retry {}/{} scheduled at {}",
                    entry.getId(), retries, MAX_RETRIES, entry.getNextRetryAt());
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
