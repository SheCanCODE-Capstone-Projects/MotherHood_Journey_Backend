package com.motherhood.journey.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulates the Irembo citizen-services API for local development.
 * Activated only when mock.gov-apis.enabled=true (application-mock.yml).
 *
 * Endpoints:
 *   POST /mock/irembo/service-requests       – used by IremboApiClient (direct submit)
 *   POST /mock/irembo/api/service-requests   – used by GovSyncOutboxDelegate (outbox dispatch)
 *   GET  /mock/irembo/service-requests/{id}  – ticket status lookup
 */
@RestController
@RequestMapping("/mock/irembo")
@ConditionalOnProperty(name = "mock.gov-apis.enabled", havingValue = "true")
@Slf4j
public class MockIremboController {

    private final AtomicLong ticketCounter = new AtomicLong(1000);

    /** Used by IremboApiClient.submitServiceRequest() — expects {"ticketId": …} in response. */
    @PostMapping("/service-requests")
    public ResponseEntity<Map<String, Object>> createServiceRequest(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        String ticketId = "IRB-" + ticketCounter.getAndIncrement();
        log.info("[MOCK-IREMBO] service-request received: ref={} serviceType={} → ticketId={}",
                body.get("externalReferenceNo"), body.get("serviceType"), ticketId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ticketId",   ticketId,
                "status",     "RECEIVED",
                "createdAt",  Instant.now().toString()
        ));
    }

    /** Used by GovSyncOutboxDelegate — idempotency-key forwarded as header. */
    @PostMapping("/api/service-requests")
    public ResponseEntity<Void> syncServiceRequest(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        log.info("[MOCK-IREMBO] outbox sync received: idempotencyKey={} syncType={}",
                idempotencyKey, body.get("syncType"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/service-requests/{ticketId}")
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(Map.of(
                "ticketId",  ticketId,
                "status",    "PROCESSING",
                "updatedAt", Instant.now().toString()
        ));
    }
}
