package com.motherhood.journey.security;

import java.util.List;
import java.util.UUID;

/**
 * Immutable value object attached to the Authentication.details field by JwtFilter.
 * Carries the facility + geo scope claims extracted from the JWT. Per-request and
 * never shared across threads — safe to read from anywhere during a request's
 * lifecycle without races.
 */
public record FacilityAuthDetails(UUID facilityId, List<UUID> geoScopeIds) {

    public FacilityAuthDetails(UUID facilityId, List<UUID> geoScopeIds) {
        this.facilityId = facilityId;
        this.geoScopeIds = geoScopeIds == null ? List.of() : List.copyOf(geoScopeIds);
    }

    /** Convenience constructor — defaults geoScopeIds to an empty list. */
    public FacilityAuthDetails(UUID facilityId) {
        this(facilityId, List.of());
    }
}
