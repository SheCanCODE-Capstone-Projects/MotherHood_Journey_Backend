package com.motherhood.identity.domain.entity;

import com.motherhood.identity.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_nid",      columnList = "national_id",  unique = true),
                @Index(name = "idx_users_phone",    columnList = "phone_number", unique = true),
                @Index(name = "idx_users_role",     columnList = "role"),
                @Index(name = "idx_users_facility", columnList = "facility_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "national_id", nullable = false, unique = true, length = 32)
    private String nationalId;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 250)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Column(name = "first_name", nullable = false, length = 64)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 64)
    private String lastName;

    @Column(name = "preferred_language", nullable = false, length = 8)
    @Builder.Default
    private String preferredLanguage = "rw";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Transient
    private List<UUID> scopedGeoIds;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.toGrantedAuthority()));
    }

    @Override public String  getPassword()             { return passwordHash; }
    @Override public String  getUsername()             { return phoneNumber;  }
    @Override public boolean isAccountNonExpired()     { return true;         }
    @Override public boolean isAccountNonLocked()      { return active;       }
    @Override public boolean isCredentialsNonExpired() { return true;         }
    @Override public boolean isEnabled()               { return active;       }


    public String getFullName() {
        return firstName + " " + lastName;
    }

    public UUID getFacilityId() {
        return facility != null ? facility.getId() : null;
    }


    public UUID getGeoLocationId() {
        return geoLocation != null ? geoLocation.getId() : null;
    }
}