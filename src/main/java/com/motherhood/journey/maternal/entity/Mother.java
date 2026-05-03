package com.motherhood.journey.maternal.entity;


import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mothers", indexes = {
        @Index(name = "idx_mother_health_id", columnList = "health_id", unique = true),
        @Index(name = "idx_mother_facility", columnList = "facility_id"),
        @Index(name = "idx_mother_geo", columnList = "geo_location_id"),
        @Index(name = "idx_mother_nida_status", columnList = "nida_verified_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mother {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "health_id", nullable = false, unique = true, length = 32)
    private String healthId;

    @Column(name = "nida_verified_status", length = 16)
    @Builder.Default
    private String nidaVerifiedStatus = "PENDING";

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "education_level", length = 32)
    private String educationLevel;

    @Column(name = "registered_at", updatable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();
}