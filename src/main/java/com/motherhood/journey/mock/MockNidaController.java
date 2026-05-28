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
import java.util.Set;

/**
 * Simulates the Rwanda NIDA identity-verification API.
 * Activated only when mock.gov-apis.enabled=true (application-mock.yml).
 *
 * ── Verification rules ──────────────────────────────────────────────────────
 *   FORCED_MATCH set        → always MATCH regardless of prefix
 *   FORCED_PARTIAL set      → always PARTIAL_MATCH
 *   ID starts with 1 or 2   → MATCH   (VERIFIED)
 *   ID starts with 3         → PARTIAL_MATCH  (MANUAL review)
 *   16 zeros                 → 503  (simulates NIDA service timeout)
 *   any other prefix         → NO_MATCH  (FAILED)
 *
 * ── Test data (seeded by V25 migration) ─────────────────────────────────────
 *   1500 mothers are seeded with NIDs matching:
 *     1{YYYY}{PP}{NNNNNNNNN}   (16 digits, starts with '1' → MATCH)
 *   Examples:
 *     1197501000000001  … 1197501000001500
 *   Phone numbers: +25078{0000001..0001500}
 *   Password for all test users: Test@1234
 */
@RestController
@RequestMapping("/mock/nida")
@ConditionalOnProperty(name = "mock.gov-apis.enabled", havingValue = "true")
@Slf4j
public class MockNidaController {

    // These specific NIDs always return MATCH (deterministic test cases)
    private static final Set<String> FORCED_MATCH = Set.of(
        "1198001000000001", "1198501000000002", "1990001000000003",
        "1197501000000004", "1198001000000005"
    );

    // These specific NIDs always return PARTIAL_MATCH (manual-review test cases)
    private static final Set<String> FORCED_PARTIAL = Set.of(
        "3198001000000001", "3198501000000002", "3199001000000003"
    );

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        String nationalId = body.getOrDefault("nationalId", "");
        String firstName  = body.getOrDefault("firstName", "");
        String lastName   = body.getOrDefault("lastName", "");

        log.info("[MOCK-NIDA] verify: nid={} firstName={} lastName={}", nationalId, firstName, lastName);

        // Simulate NIDA service unavailability
        if ("0000000000000000".equals(nationalId)) {
            log.warn("[MOCK-NIDA] simulating SERVICE_UNAVAILABLE for nid={}", nationalId);
            return ResponseEntity.status(503)
                    .body(Map.of(
                        "error",   "SERVICE_UNAVAILABLE",
                        "message", "NIDA service timeout (mock)"
                    ));
        }

        String status;
        if (FORCED_MATCH.contains(nationalId)) {
            status = "MATCH";
        } else if (FORCED_PARTIAL.contains(nationalId)) {
            status = "PARTIAL_MATCH";
        } else if (nationalId.startsWith("1") || nationalId.startsWith("2")) {
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
        return ResponseEntity.ok(Map.of(
            "status",  "UP",
            "service", "MOCK_NIDA",
            "note",    "Mock — not a real NIDA connection"
        ));
    }
}
