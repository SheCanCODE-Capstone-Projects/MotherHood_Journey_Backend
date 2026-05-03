package com.motherhood.journey.child.entity;


import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;

import com.motherhood.journey.maternal.entity.Mother;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "children", indexes = {
        @Index(name = "idx_child_mother", columnList = "mother_id"),
        @Index(name = "idx_child_facility", columnList = "facility_id"),
        @Index(name = "idx_child_birth_cert", columnList = "birth_certificate_no", unique = true),
        @Index(name = "idx_child_health_status", columnList = "health_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mother_id", nullable = false)
    private Mother mother;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "birth_certificate_no", unique = true, length = 64)
    private String birthCertificateNo;

    @Column(name = "first_name", length = 64)
    private String firstName;

    @Column(length = 8)
    private String gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "birth_weight_kg")
    private Double birthWeightKg;

    @Column(name = "delivery_type", length = 16)
    private String deliveryType;

    @Column(name = "health_status", length = 16)
    @Builder.Default
    private String healthStatus = "HEALTHY";

    @Column(name = "registered_at", updatable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();
}