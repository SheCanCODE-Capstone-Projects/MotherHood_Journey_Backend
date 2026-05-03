package com.motherhood.journey.consent.entity;

import com.motherhood.journey.maternal.entity.Mother;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_records", indexes = {
        @Index(name = "idx_consent_mother", columnList = "mother_id"),
        @Index(name = "idx_consent_type", columnList = "mother_id, consent_type"),
        @Index(name = "idx_consent_expiry", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mother_id", nullable = false)
    private Mother mother;

    @Column(name = "consent_type", nullable = false, length = 32)
    private String consentType;

    @Column(nullable = false)
    private Boolean granted;

    @Column(name = "granted_by_role", length = 32)
    private String grantedByRole;

    @Column(name = "consented_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime consentedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "legal_basis", length = 32)
    private String legalBasis;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}