package com.motherhood.facility.domain.model;

import com.motherhood.shared.audit.Auditable;
import jakarta.persistence.*;

@Entity
@Table(name = "facilities")
public class Facility extends Auditable {

    public enum Type { HOSPITAL, HEALTH_CENTER, CLINIC, DISPENSARY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String province;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    private String phoneNumber;
    private Double latitude;
    private Double longitude;

    protected Facility() {}

    public Facility(String name, String district, String province, Type type,
                    String phoneNumber, Double latitude, Double longitude) {
        this.name = name;
        this.district = district;
        this.province = province;
        this.type = type;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    public Type getType() { return type; }
    public String getPhoneNumber() { return phoneNumber; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
