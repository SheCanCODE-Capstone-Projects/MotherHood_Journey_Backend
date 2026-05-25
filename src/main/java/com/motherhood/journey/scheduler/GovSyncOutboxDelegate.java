package com.motherhood.journey.scheduler;

import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Transactional delegate for GovSyncOutboxProcessor.
 * Extracted into a separate bean so Spring's proxy correctly applies
 * @Transactional on fetchPending() and dispatch() — self-calls within
 * the same bean bypass the proxy and lose transaction isolation.
 */
@Component
public class GovSyncOutboxDelegate {

    private static final Logger log = LoggerFactory.getLogger(GovSyncOutboxDelegate.class);
    private static final int MAX_RETRIES = 5;

    private final GovSyncLogRepository govSyncLogRepository;
    private final RestClient restClient;

    public GovSyncOutboxDelegate(GovSyncLogRepository govSyncLogRepository,
                                  RestClient.Builder restClientBuilder) {
        this.govSyncLogRepository = govSyncLogRepository;
        this.restClient = restClientBuilder.build();
    }

    @Transactional
    public List<GovSyncLog> fetchAndMarkInProgress() {
        List<GovSyncLog> entries = govSyncLogRepository
            .findByStatusAndNextRetryAtLessThanEqualAndDeadLetterFalse(
                SyncStatus.PENDING, OffsetDateTime.now());
        entries.forEach(e -> e.setStatus(SyncStatus.IN_FLIGHT));
        govSyncLogRepository.saveAll(entries);
        return entries;
    }

    @Transactional
    public void dispatch(GovSyncLog entry, String iremboBaseUrl, String iremboApiKey) {
        try {
            var sr = entry.getServiceRequest();
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("idempotencyKey", entry.getIdempotencyKey());
            body.put("syncType", entry.getSyncType());
            if (sr != null) {
                body.put("referenceNo", sr.getReferenceNo());
                body.put("serviceType", sr.getServiceType());
                body.put("requesterId", sr.getRequester() != null ? sr.getRequester().getId() : null);
                body.put("facilityId", sr.getFacility() != null ? sr.getFacility().getId() : null);
                body.put("submittedAt", sr.getSubmittedAt());
                if (sr.getPayload() != null) body.put("payload", sr.getPayload());
            }

            restClient.post()
                .uri(iremboBaseUrl + "/api/service-requests")
                .header("X-API-Key", iremboApiKey)
                .header("X-Idempotency-Key", entry.getIdempotencyKey())
                .body(body)
                .retrieve()
                .toBodilessEntity();

            entry.setStatus(SyncStatus.SUCCEEDED);
            entry.setSyncedAt(LocalDateTime.now());
            entry.setErrorMessage(null);
            log.info("GovSyncOutboxDelegate: entry {} dispatched to Irembo", entry.getId());

        } catch (RestClientException e) {
            int retries = entry.getRetryCount() + 1;
            entry.setRetryCount(retries);
            entry.setErrorMessage(truncate(e.getMessage(), 500));

            if (retries >= MAX_RETRIES) {
                entry.setStatus(SyncStatus.DEAD_LETTER);
                entry.setDeadLetter(true);
                log.error("GovSyncOutboxDelegate: entry {} FAILED after {} retries — {}",
                    entry.getId(), retries, e.getMessage(), e);
            } else {
                entry.setStatus(SyncStatus.PENDING);
                entry.setNextRetryAt(OffsetDateTime.now().plusMinutes((long) Math.pow(2, retries)));
                log.warn("GovSyncOutboxDelegate: entry {} retry {}/{} scheduled at {}",
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