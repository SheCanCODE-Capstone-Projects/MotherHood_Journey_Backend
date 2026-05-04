package com.motherhood.journey.facility.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "facilities")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Setters
    @Setter
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String province;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityType type;

    private String phoneNumber;
    private Double latitude;
    private Double longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Facility() {}

    public Facility(String name, String district, String province, FacilityType type,
                    String phoneNumber, Double latitude, Double longitude) {
        this.name = name;
        this.district = district;
        this.province = province;
        this.type = type;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    public FacilityType getType() { return type; }
    public String getPhoneNumber() { return phoneNumber; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setDistrict(String district) { this.district = district; }
    public void setProvince(String province) { this.province = province; }
    public void setType(FacilityType type) { this.type = type; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

