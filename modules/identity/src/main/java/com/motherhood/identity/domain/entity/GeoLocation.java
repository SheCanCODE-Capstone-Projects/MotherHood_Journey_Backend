package com.motherhood.identity.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geo_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GeoLocation {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "province", nullable = false, length = 64)
    private String province;

    @Column(name = "district", nullable = false, length = 64)
    private String district;

    @Column(name = "sector", nullable = false, length = 64)
    private String sector;

    @Column(name = "cell", nullable = false, length = 64)
    private String cell;

    @Column(name = "village", nullable = false, length = 64)
    private String village;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
