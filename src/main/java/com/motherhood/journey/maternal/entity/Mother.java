package com.motherhood.journey.maternal.entity;

import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.geo.entity.GeoLocation;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "mothers")
public class Mother {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "health_id", unique = true, nullable = false)
    private String healthId;

    // Getters and Setters
}
