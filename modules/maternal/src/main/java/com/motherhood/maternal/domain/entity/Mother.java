package com.motherhood.maternal.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "mothers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mother {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;


    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;


    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "geo_location_id", nullable = false)
    private UUID geoLocationId;


    @Column(name = "health_id", nullable = false, unique = true, length = 32)
    private String healthId;

    @Column(name = "nida_verified_status", nullable = false, length = 16)
    @Builder.Default
    private String nidaVerifiedStatus = "PENDING";

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;


    @Column(name = "education_level", length = 32)
    private String educationLevel;

    @Column(name = "registered_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();
}
