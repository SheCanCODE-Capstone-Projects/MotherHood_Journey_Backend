package com.motherhood.geo.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "geo_locations",
        indexes = {
                @Index(
                        name = "idx_geo_prov_dist_sector",
                        columnList = "province, district, sector"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            updatable = false,
            nullable = false,
            columnDefinition = "uuid"  // PostgreSQL native uuid type
    )
    private UUID id;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String sector;

    @Column(nullable = false)
    private String cell;

    @Column(nullable = false)
    private String village;

    @Column(name = "postal_code")
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Runs automatically just before saving to database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}