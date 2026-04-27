package com.motherhood.consent.application.dto;

import com.motherhood.consent.domain.model.ConsentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponse {

    private UUID id;
    private UUID motherId;
    private ConsentType consentType;
    private Boolean granted;
    private String grantedByRole;
    private String legalBasis;
    private LocalDateTime consentedAt;
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;
    private Boolean active;
}