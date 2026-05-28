package com.motherhood.journey.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Simulates Rwanda MoH HMIS (DHIS2) report-push API for local development.
 * Activated only when mock.gov-apis.enabled=true (application-mock.yml).
 *
 * Accepts a DHIS2 dataValueSet payload built by Dhis2PayloadBuilder and
 * returns a DHIS2-compatible importSummary response.
 */
@RestController
@RequestMapping("/mock/hmis")
@ConditionalOnProperty(name = "mock.gov-apis.enabled", havingValue = "true")
@Slf4j
public class MockHmisController {

    private final AtomicLong reportCounter = new AtomicLong(5000);

    @PostMapping("/reports")
    public ResponseEntity<Map<String, Object>> pushReport(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        String reportId  = "RPT-" + reportCounter.getAndIncrement();
        String dataSet   = String.valueOf(body.getOrDefault("dataSet",  "UNKNOWN"));
        String period    = String.valueOf(body.getOrDefault("period",   "UNKNOWN"));
        String orgUnit   = String.valueOf(body.getOrDefault("orgUnit",  "UNKNOWN"));
        String extRef    = String.valueOf(body.getOrDefault("externalReferenceNo", ""));

        log.info("[MOCK-HMIS] report push: ref={} dataSet={} period={} orgUnit={} → reportId={}",
                extRef, dataSet, period, orgUnit, reportId);

        return ResponseEntity.ok(Map.of(
                "reportId",     reportId,
                "status",       "SUCCESS",
                "dataSet",      dataSet,
                "importCount",  Map.of("imported", 1, "updated", 0, "ignored", 0, "deleted", 0),
                "description",  "Import was successful.",
                "acceptedAt",   Instant.now().toString()
        ));
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable String reportId) {
        return ResponseEntity.ok(Map.of(
                "reportId", reportId,
                "status",   "PROCESSED"
        ));
    }
}
