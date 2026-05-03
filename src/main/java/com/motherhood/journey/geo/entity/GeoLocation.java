package com.motherhood.journey.geo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geo_locations", indexes = {
        @Index(name = "idx_geo_pds", columnList = "province, district, sector"),
        @Index(name = "idx_geo_sector", columnList = "sector")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocation {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 64)
    private String province;

    @Column(nullable = false, length = 64)
    private String district;

    @Column(nullable = false, length = 64)
    private String sector;

    @Column(nullable = false, length = 64)
    private String cell;

    @Column(nullable = false, length = 64)
    private String village;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}