package com.motherhood.journey.scheduler;

import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox processor for GovSyncLog entries targeting Irembo.
 *
 * Runs every 30 seconds. Picks up to 50 PENDING entries whose
 * nextRetryAt has passed. On success marks SENT. On failure applies
 * exponential backoff (2^retryCount minutes) and marks FAILED after
 * MAX_RETRIES attempts. External HTTP call is isolated from the DB
 * transaction — DB is committed first, then HTTP is attempted.
 */
@Component
public class GovSyncOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(GovSyncOutboxProcessor.class);
    private static final int MAX_RETRIES = 5;

    private final GovSyncLogRepository govSyncLogRepository;
    private final RestClient restClient;

    @Value("${irembo.base-url:}")
    private String iremboBaseUrl;

    @Value("${irembo.api-key:}")
    private String iremboApiKey;

    public GovSyncOutboxProcessor(GovSyncLogRepository govSyncLogRepository,
                                   RestClient.Builder restClientBuilder) {
        this.govSyncLogRepository = govSyncLogRepository;
        this.restClient = restClientBuilder.build();
    }

    @Scheduled(fixedDelay = 30_000)
    public void process() {
        if (iremboBaseUrl == null || iremboBaseUrl.isBlank()) {
            log.debug("GovSyncOutboxProcessor: IREMBO_BASE_URL not configured — skipping");
            return;
        }

        List<GovSyncLog> pending = fetchPending();
        if (pending.isEmpty()) return;

        log.info("GovSyncOutboxProcessor: processing {} pending entry(ies)", pending.size());

        for (GovSyncLog entry : pending) {
            dispatch(entry);
        }
    }

    // Fetch and immediately mark IN_PROGRESS in its own transaction
    // to prevent concurrent processor instances from double-processing.
    @Transactional
    public List<GovSyncLog> fetchPending() {
        List<GovSyncLog> entries = govSyncLogRepository.findPendingForRetry(LocalDateTime.now());
        entries.forEach(e -> e.setStatus("IN_PROGRESS"));
        govSyncLogRepository.saveAll(entries);
        return entries;
    }

    // HTTP dispatch is intentionally outside the DB transaction.
    // DB commit happens in fetchPending(); HTTP failure only affects this entry's status.
    @Transactional
    public void dispatch(GovSyncLog entry) {
        try {
            // Build full payload from the linked ServiceRequest
            var sr = entry.getServiceRequest();
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("idempotencyKey", entry.getIdempotencyKey());
            body.put("syncType",       entry.getSyncType());
            if (sr != null) {
                body.put("referenceNo",  sr.getReferenceNo());
                body.put("serviceType",  sr.getServiceType());
                body.put("requesterId",  sr.getRequester() != null ? sr.getRequester().getId() : null);
                body.put("facilityId",   sr.getFacility()  != null ? sr.getFacility().getId()  : null);
                body.put("submittedAt",  sr.getSubmittedAt());
                if (sr.getPayload() != null) body.put("payload", sr.getPayload());
            }

            restClient.post()
                .uri(iremboBaseUrl + "/api/service-requests")
                .header("X-API-Key", iremboApiKey)
                .header("X-Idempotency-Key", entry.getIdempotencyKey())
                .body(body)
                .retrieve()
                .toBodilessEntity();

            entry.setStatus("SENT");
            entry.setSyncedAt(LocalDateTime.now());
            entry.setErrorMessage(null);
            log.info("GovSyncOutboxProcessor: entry {} dispatched to Irembo", entry.getId());

        } catch (Exception e) {
            int retries = entry.getRetryCount() + 1;
            entry.setRetryCount(retries);
            entry.setErrorMessage(truncate(e.getMessage(), 500));

            if (retries >= MAX_RETRIES) {
                entry.setStatus("FAILED");
                log.error("GovSyncOutboxProcessor: entry {} FAILED after {} retries — {}",
                    entry.getId(), retries, e.getMessage());
            } else {
                // Exponential backoff: 2, 4, 8, 16 minutes
                entry.setStatus("PENDING");
                entry.setNextRetryAt(LocalDateTime.now().plusMinutes((long) Math.pow(2, retries)));
                log.warn("GovSyncOutboxProcessor: entry {} retry {}/{} scheduled at {}",
                    entry.getId(), retries, MAX_RETRIES, entry.getNextRetryAt());
            }
        }
        govSyncLogRepository.save(entry);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
