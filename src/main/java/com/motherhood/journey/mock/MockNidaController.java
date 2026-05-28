package com.motherhood.journey.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Simulates the Rwanda NIDA identity-verification API for local development.
 * Activated only when mock.gov-apis.enabled=true (application-mock.yml).
 *
 * Verification logic (based on national-ID prefix):
 *   1… or 2…  →  MATCH       (VERIFIED)
 *   3…         →  PARTIAL_MATCH (MANUAL review)
 *   any other  →  NO_MATCH   (FAILED)
 *   0000…0000  →  503         (simulates service timeout)
 */
@RestController
@RequestMapping("/mock/nida")
@ConditionalOnProperty(name = "mock.gov-apis.enabled", havingValue = "true")
@Slf4j
public class MockNidaController {

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        String nationalId = body.getOrDefault("nationalId", "");
        String firstName  = body.getOrDefault("firstName", "");
        String lastName   = body.getOrDefault("lastName", "");

        log.info("[MOCK-NIDA] verify: nid={} firstName={} lastName={}", nationalId, firstName, lastName);

        if ("0000000000000000".equals(nationalId)) {
            log.warn("[MOCK-NIDA] simulating SERVICE_UNAVAILABLE for nid={}", nationalId);
            return ResponseEntity.status(503)
                    .body(Map.of("error", "SERVICE_UNAVAILABLE", "message", "NIDA service timeout (mock)"));
        }

        String status;
        if (nationalId.startsWith("1") || nationalId.startsWith("2")) {
            status = "MATCH";
        } else if (nationalId.startsWith("3")) {
            status = "PARTIAL_MATCH";
        } else {
            status = "NO_MATCH";
        }

        log.info("[MOCK-NIDA] nid={} → status={}", nationalId, status);
        return ResponseEntity.ok(Map.of(
                "nationalId",  nationalId,
                "status",      status,
                "verifiedAt",  Instant.now().toString(),
                "source",      "MOCK_NIDA"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "MOCK_NIDA"));
    }
}
