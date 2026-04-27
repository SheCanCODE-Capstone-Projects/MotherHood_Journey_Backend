package com.motherhood.identity.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "facilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facility {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "facility_code", nullable = false, unique = true, length = 32)
    private String facilityCode;

    @Column(name = "facility_type", nullable = false, length = 32)
    private String facilityType;

    @Column(name = "district", nullable = false, length = 64)
    private String district;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
