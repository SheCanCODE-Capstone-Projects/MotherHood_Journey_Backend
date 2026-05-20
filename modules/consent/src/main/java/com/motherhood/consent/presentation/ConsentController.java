package com.motherhood.consent.presentation;

import com.motherhood.consent.application.dto.ConsentRequest;
import com.motherhood.consent.application.dto.ConsentResponse;
import com.motherhood.consent.application.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService service;

    // Records a new consent from a mother
    // Called when health worker or mother fills the consent form
    @PostMapping
    public ResponseEntity<ConsentResponse> recordConsent(
            @RequestBody ConsentRequest request) {
        ConsentResponse response = service.recordConsent(request);
        // Return 201 Created a new record was created
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Revokes an existing consent
    // Record stays in database only revoked_at is set
    @DeleteMapping("/{id}")
    public ResponseEntity<ConsentResponse> revokeConsent(
            @PathVariable UUID id) {
        ConsentResponse response = service.revokeConsent(id);
        // Return 200 OK revocation was recorded successfully
        return ResponseEntity.ok(response);
    }
    // Returns full consent history for a mother
    // Used by admin or health worker to see what mother has agreed to
    @GetMapping("/mother/{motherId}")
    public ResponseEntity<List<ConsentResponse>> getConsentsByMother(
            @PathVariable UUID motherId) {
        return ResponseEntity.ok(service.getConsentsByMother(motherId));
    }
}