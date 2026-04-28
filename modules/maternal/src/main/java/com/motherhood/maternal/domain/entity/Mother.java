package com.motherhood.maternal.domain.entity;

import com.motherhood.identity.domain.entity.Facility;
import com.motherhood.identity.domain.entity.GeoLocation;
import com.motherhood.maternal.domain.enums.EducationLevel;
import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mothers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mother {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "health_id", nullable = false, unique = true, length = 32)
    private String healthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nida_verified_status", nullable = false, length = 16)
    @Builder.Default
    private NidaVerifiedStatus nidaVerifiedStatus = NidaVerifiedStatus.PENDING;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 32)
    private EducationLevel educationLevel;

    @Column(name = "registered_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();
}
