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
public class ConsentRequest {

    // Which mother is giving consent
    private UUID motherId;

    // What type of consent
    private ConsentType consentType;
    private Boolean granted;

    // Who is recording this consent
    private String grantedByRole;

    // Legal basis for processing
    private String legalBasis;
    private LocalDateTime expiresAt;
}