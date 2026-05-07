package com.motherhood.journey.geo.entity;

import com.motherhood.journey.geo.enums.FacilityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "facilities", indexes = {
        @Index(name = "idx_facility_code", columnList = "facility_code", unique = true),
        @Index(name = "idx_facility_geo", columnList = "geo_location_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(name = "facility_code", nullable = false, length = 32, unique = true)
    private String facilityCode;

    // Changed from String to FacilityType enum
    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 32)
    private FacilityType facilityType;

    @Column(nullable = false, length = 64)
    private String district;

    @Column(length = 20)
    private String phone;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}